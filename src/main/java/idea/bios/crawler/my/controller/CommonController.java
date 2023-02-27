package idea.bios.crawler.my.controller;

import idea.bios.crawler.CrawlConfig;
import idea.bios.crawler.CrawlController;
import idea.bios.crawler.WebCrawler;
import idea.bios.fetcher.PageFetcher;
import idea.bios.robotstxt.RobotsTxtServer;
import idea.bios.url.URLCanonicalizer;
import idea.bios.url.WebURL;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 重写Controller方法
 * 需要操作队列用此Controller
 * @author 86153
 */
@Slf4j
public class CommonController extends CrawlController {
    private static final Executor MONITOR_THREAD_EXECUTOR = Executors.newFixedThreadPool(5);
    /**
     * 计划中的url是否插入完毕
     */
    private boolean isSchedulePutQueueFinish = false;

    public CommonController(CrawlConfig config, PageFetcher pageFetcher,
                            RobotsTxtServer robotsTxtServer) throws Exception {
        super(config, pageFetcher, robotsTxtServer);
    }

    public boolean isSchedulePutQueueFinish() {
        return isSchedulePutQueueFinish;
    }

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
     * @param pageUrls   List
     */
    public void addUrlsToQueue(List<String> pageUrls) {
        if (pageUrls == null || pageUrls.isEmpty()) {
            log.warn("pageUrls empty!");
            return;
        }
        // 标准化
        List<String> canonicalUrls = new ArrayList<>();
        for (String pageUrl : pageUrls) {
            try {
                URLCanonicalizer.getCanonicalURL(pageUrl);
            } catch (UnsupportedEncodingException e) {
                log.warn("Exception occurs. pageUrls={}", pageUrls, e);
                continue;
            }
            if (pageUrl != null) {
                canonicalUrls.add(pageUrl);
            }
        }
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
                log.trace("This URL is already seen.");
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
            log.warn("all url illegal!");
            return;
        }
        frontier.scheduleAll(urls);
    }

    /**
     * 重写启动方法，不退出程序，持续性放入新的数据
     * @param crawlerFactory        爬虫构造方法
     * @param numberOfCrawlers      爬虫线程池个数
     * @param <T>                   T
     */
    @Override
    public <T extends WebCrawler> void start(final WebCrawlerFactory<T> crawlerFactory,
                                             final int numberOfCrawlers) {
        final CrawlConfig config = super.getConfig();
        try {
            finished = false;
            crawlersLocalData.clear();
            // 线程列表
            final var threads = new ArrayList<Thread>();
            // 爬虫队列
            final var crawlers = new ArrayList<T>();
            for (int i = 1; i <= numberOfCrawlers; i++) {
                // 创建一个crawler
                T crawler = crawlerFactory.newInstance();
                var thread = new Thread(crawler, "Crawler " + i);
                crawler.setMyThread(thread);
                // 初始化crawler
                crawler.init(i, this);
                thread.start();
                crawlers.add(crawler);
                threads.add(thread);
                log.info("Crawler {} started", i);
            }
            // 监控时钟
            final CrawlController controller = this;
            MONITOR_THREAD_EXECUTOR.execute(() -> {
                try {
                    synchronized (waitingLock) {
                        while (true) {
                            // Wait this long before checking the status of the worker threads.
                            sleep(config.getThreadMonitoringDelaySeconds());
                            boolean someoneIsWorking = false;
                            for (int i = 0; i < threads.size(); i++) {
                                Thread thread = threads.get(i);
                                // 线程不存活
                                if (!thread.isAlive()) {
                                    if (!shuttingDown) {
                                        // 重新拉起一个线程
                                        T crawler = crawlerFactory.newInstance();
                                        thread = new Thread(crawler, "Crawler " + (i + 1));
                                        Thread droppedThread = threads.remove(i);
                                        log.warn("Thread {} is droped. then new one:{}", droppedThread.getName(), thread.getName());
                                        threads.add(i, thread);
                                        crawler.setMyThread(thread);
                                        crawler.init(i + 1, controller);
                                        thread.start();
                                        // 更新crawler list
                                        T droppedCrawler = crawlers.remove(i);
                                        crawlers.add(i, crawler);
                                    }
                                } else if (!crawlers.get(i).isWaitingForNewURLs()){
                                    someoneIsWorking = true;
                                }
                            }
                            // 如果没有在执行，而且所有数据已经进入队列，则关闭程序
                            if (!someoneIsWorking && isSchedulePutQueueFinish) {
                                log.info("任务可能已经完成，等待几秒再进行第一次判断");
                                sleep(config.getThreadShutdownDelaySeconds());
                                // sleep几秒，再次做一次检测
                                // 再检测一下队列，如果还有数据，则不退出
                                boolean firstCheck = false;
                                for (int i = 0; i < threads.size(); i++) {
                                    Thread thread = threads.get(i);
                                    if (thread.isAlive() &&
                                            !crawlers.get(i).isWaitingForNewURLs()) {
                                        firstCheck = true;
                                        break;
                                    }
                                }
                                if (firstCheck) {
                                    continue;
                                }
                                log.info("任务可能已经完成，等待几秒再进行第二次判断");
                                sleep(config.getThreadShutdownDelaySeconds());
                                if (frontier.getQueueLength() > 0) {
                                    continue;
                                }
                                // 此时确定已经完毕，关闭系统
                                log.info("任务可能已经完成，系统关闭");
                                frontier.finish();
                                for (T crawler : crawlers) {
                                    crawler.onBeforeExit();
                                    crawlersLocalData.add(crawler.getMyLocalData());
                                }
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
                    }
                } catch (Exception e) {
                    log.error("Unexpected Error", e);
                }
            });
            // waitUntilFinish();
        } catch (Exception e) {
            log.error("Error happened", e);
        }
    }
}
