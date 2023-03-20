package idea.bios.crawler.my.controller;

import com.google.gson.Gson;
import idea.bios.crawler.CrawlConfig;
import idea.bios.crawler.CrawlController;
import idea.bios.crawler.WebCrawler;
import idea.bios.crawler.entity.CrawlerStaticsBo;
import idea.bios.crawler.my.sites.CrawlerSiteEnum;
import idea.bios.datasource.mongodb.MongoDb;
import idea.bios.fetcher.PageFetcher;
import idea.bios.robotstxt.RobotsTxtServer;
import idea.bios.url.URLCanonicalizer;
import idea.bios.url.WebURL;
import idea.bios.util.Schedule;
import idea.bios.util.selenium.SeleniumBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 重写Controller方法
 * 需要操作队列用此Controller
 * @author 86153
 */
@Slf4j
public class CommonController extends CrawlController implements ControllerFacade {
    /**
     * 计划中的url是否插入完毕
     */
    private boolean isSchedulePutQueueFinish = false;

    @Getter
    private final String name;

    public CommonController(CrawlConfig config, PageFetcher pageFetcher,
                            RobotsTxtServer robotsTxtServer, String name) throws Exception {
        super(config, pageFetcher, robotsTxtServer);
        this.name = name;
    }

    public boolean isSchedulePutQueueFinish() {
        return isSchedulePutQueueFinish;
    }

    @Override
    public void putQueueFinish() {
       if (isSchedulePutQueueFinish) {
           log.warn("put queue already finished!");
           return;
       }
        log.info("put queue finished!");
        isSchedulePutQueueFinish = true;
    }

    /**
     * 在task存续期增加url进队列
     * 持续加入队列接口
     * @param pageUrls   List
     */
    @Override
    public void addUrlsToQueue(List<String> pageUrls) {
        if (pageUrls == null || pageUrls.isEmpty()) {
            log.warn("pageUrls empty!");
            return;
        }
        List<String> canonicalUrls = pageUrls.stream()
                .filter(Objects::nonNull).filter(pageUrl -> {
            try {
                URLCanonicalizer.getCanonicalURL(pageUrl);
                return true;
            } catch (UnsupportedEncodingException e) {
                log.warn("Exception occurs. pageUrl={}", pageUrl, e);
                return false;
            }
        }).collect(Collectors.toList());

        if (canonicalUrls.isEmpty()) {
            log.warn("seed URL empty: {}", pageUrls);
            return;
        }
        // 构造URL List
        var urls = new ArrayList<WebURL>(canonicalUrls.size());
        canonicalUrls.forEach(url -> {
            if (url == null) {
                log.warn("illegal canonicalUrls:{}", canonicalUrls);
                return;
            }
            // docIdServer需要保持开启
            var webUrl = new WebURL();
            int docId = docIdServer.getDocId(url);
            if (docId > 0) {
                log.info("This URL is already seen. url:{}", url);
                return;
            } else {
                // 创建一个新的
                docId = docIdServer.getNewDocId(url);
            }
            // 组装
            webUrl.setURL(url);
            webUrl.setDocid(docId);
            // 是否合规
            try {
                if (robotstxtServer.allows(webUrl)) {
                    urls.add(webUrl);
                } else {
                    // using the WARN level here
                    // as the user specifically asked to add this seed
                    log.warn("Robots.txt does not allow this seed: {}", webUrl);
                }
            } catch (IOException | InterruptedException e) {
                log.warn("Exception occurs.", e);
            }
        });
        if (urls.isEmpty()) {
            log.warn("all url illegal, adding nothing!");
            return;
        }
        frontier.scheduleAll(urls);
    }


    /**
     * 重写启动方法
     * @param clazz                 类型
     * @param numberOfCrawlers      爬虫线程池个数
     * @param <T>                   T
     */
    @Override
    public <T extends WebCrawler> void start(Class<T> clazz, final int numberOfCrawlers) {
        final CrawlConfig config = super.getConfig();
        // 初始化crawler池
        var crawlerPool = new CrawlerPool<T>();
        crawlerPool.init(numberOfCrawlers, clazz, this);
        try {
            finished = false;
            crawlersLocalData.clear();
            // 监控时钟
            Schedule.scheduleAtFixedRate(()-> {
                try {
                    synchronized (waitingLock) {
                        AtomicBoolean someoneIsWorking = new AtomicBoolean(false);
                        IntStream.range(0, crawlerPool.getTHREADS().size()).forEach(i -> {
                            // 线程不存活
                            if (!crawlerPool.getTHREADS().get(i).isAlive()) {
                                if (!shuttingDown) {
                                    crawlerPool.rebuildCrawler(clazz, this, i);
                                } else if (!crawlerPool.getCRAWLERS().get(i).isWaitingForNewURLs()){
                                    someoneIsWorking.set(true);
                                }
                            }
                        });
                        // 如果没有在执行，而且所有数据已经进入队列，则关闭程序
                        if (!someoneIsWorking.get() && isSchedulePutQueueFinish) {
                            log.info("任务可能已经完成，等待几秒再进行第一次判断");
                            sleep(config.getThreadShutdownDelaySeconds());
                            // 再检测一下队列，如果还有数据，则不退出
                            var firstCheck = new AtomicBoolean(false);
                            IntStream.range(0, crawlerPool.getTHREADS().size()).forEach(i -> {
                                Thread thread = crawlerPool.getTHREADS().get(i);
                                if (thread.isAlive() && !crawlerPool.getCRAWLERS().get(i).isWaitingForNewURLs()) {
                                    firstCheck.set(true);
                                }
                            });
                            if (firstCheck.get()) {
                                return;
                            }
                            log.info("任务可能已经完成，等待几秒再进行第二次判断");
                            sleep(config.getThreadShutdownDelaySeconds());
                            if (frontier.getQueueLength() > 0) {
                                return;
                            }
                            // 此时确定已经完毕，关闭系统
                            log.info("任务可能已经完成，系统即将关闭");
                            frontier.finish();
                            crawlerPool.getCRAWLERS().forEach(crawler -> {
                                crawler.onBeforeExit();
                                crawlersLocalData.add(crawler.getMyLocalData());
                                SeleniumBuilder.shutdownChromeDriver(crawler.getChromeDriver());
                                SeleniumBuilder.shutdownPhantomJsDriver(crawler.getPhantomJsDriver());
                            });
                            log.info("Waiting for {} seconds before final clean up...",
                                    config.getCleanupDelaySeconds());
                            sleep(config.getCleanupDelaySeconds());
                            frontier.close();
                            docIdServer.close();
                            pageFetcher.shutDown();
                            finished = true;
                            waitingLock.notifyAll();
                            env.close();
                        }
                    }
                } catch (Exception e) {
                    log.error("Unexpected Error", e);
                }
            }, config.getThreadMonitoringDelaySeconds());
            // waitUntilFinish();
        } catch (Exception e) {
            log.error("Error happened", e);
        }
        // 开启一个频率为30 min的schedule，用来观测和处理各个线程的运行情况
        final int staticRate = 1800;
        Schedule.scheduleAtFixedRate(()-> {
            var msgAllList = new ArrayList<String>();
            msgAllList.add("【INFOS】");
            msgAllList.add("name: " + this.name);
            msgAllList.add("work path: " + System.getProperty("user.dir"));
            msgAllList.add("work queue length: " + this.frontier.getQueueLength());
            msgAllList.add("collected length: " + new MongoDb()
                            .getCrawlerDataCollection(this.name).countDocuments());
            msgAllList.add("\n【CRAWLERS】");
            crawlerPool.getCRAWLERS().forEach(crawler -> {
                var msgList = new ArrayList<String>();
                msgList.add("----------------------------");
                CrawlerStaticsBo staticsBo = crawler.getStaticsBo();
                msgList.add("\t[name: " + crawler.getMyThread().getName() + "]");
                msgList.add("\t-proxy: " + crawler.getProxyInfo());
                msgList.add("\t-num of visited: " + staticsBo.getCurVisitPageNum());
                msgList.add("\t-rate: " +
                        (staticsBo.getCurVisitPageNum() - staticsBo.getLastVisitPageNum()) * 60
                                / (staticRate) + "/ min");
                staticsBo.setLastVisitPageNum(staticsBo.getCurVisitPageNum());
                msgAllList.add(String.join("\n", msgList));
            });
            // 发送
            final String wxUrl = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=58cce18d-9306-449c-b9f9-eab09d459597";
            // wxUrl += "&debug=1";
            var reqMap = new HashMap<String, Object>();
            var textMap = new HashMap<String, String>();
            textMap.put("content", String.join("\n", msgAllList));
            reqMap.put("msgtype", "text");
            reqMap.put("text", textMap);
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(wxUrl);
            // 设置请求头信息
            httpPost.setHeader("Content-Type", "application/json; charset=utf-8");
            // 设置请求体参数
            CloseableHttpResponse response = null;
            try {
                StringEntity entity = new StringEntity(
                        new Gson().toJson(reqMap), ContentType.APPLICATION_JSON);
                httpPost.setEntity(entity);
                response = client.execute(httpPost);
            } catch (Exception e) {
                log.warn("Exception", e);
            } finally {
                try {
                    response.close();
                } catch (IOException e) {
                    log.warn("IOException", e);
                }
            }
        }, staticRate);
    }
}
