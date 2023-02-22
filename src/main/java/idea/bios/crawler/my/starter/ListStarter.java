package idea.bios.crawler.my.starter;

import idea.bios.crawler.CrawlConfig;
import idea.bios.crawler.CrawlController;
import idea.bios.crawler.WebCrawler;
import idea.bios.crawler.my.CommonController;
import idea.bios.crawler.my.Tools;
import idea.bios.crawler.my.sites.ListCrawlerEnum;
import idea.bios.fetcher.PageFetcher;
import idea.bios.robotstxt.RobotsTxtConfig;
import idea.bios.robotstxt.RobotsTxtServer;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 启动类
 * @author 86153
 */
@Slf4j
public class ListStarter {

    private static final int NUMBER_OF_CRAWLERS = 8;

    private final CrawlConfig config;
    private final RobotsTxtConfig robotsTxtConfig;
    private CommonController myListController;

    private static final String CRAW_STORAGE_FOLDER = "./crawl/root";

    public ListStarter(CrawlConfig config, RobotsTxtConfig robotsTxtConfig) {
        this.config = config;
        this.robotsTxtConfig = robotsTxtConfig;
    }

    public ListStarter(CrawlConfig config) {
        this.config = config;
        this.robotsTxtConfig = Tools.defaultRobotsBuilder();
    }

    public ListStarter() {
        this.config = Tools.listConfigBuilder(1000);
        this.robotsTxtConfig = Tools.defaultRobotsBuilder();
    }

    /**
     * 在执行的过程中，有需要写入队列的
     * @param pageUrls  urls
     */
    public void setQueue(List<String> pageUrls) {
        if (myListController == null || pageUrls == null || pageUrls.isEmpty()) {
            return;
        }
        try {
            myListController.addUrlsToQueue(pageUrls);
        } catch (UnsupportedEncodingException e) {
            log.warn("Exception occurs.", e);
        }
    }

    /**
     *  处理list类型的爬虫
     *
     */
    public void run(ListCrawlerEnum crawlerEnum, URLSourceBuilder urlSourceBuilder,
                    int step, final int start, final int end) throws Exception {
        config.setCrawlStorageFolder(CRAW_STORAGE_FOLDER);
        config.setRespectNoIndex(false);

        // 判断参数
        if (step <= 0 || start < 0 || start > end) {
            log.warn("param error!");
            return;
        }
        // 先启动一个空队列的Controller
        var pageFetcher = new PageFetcher(config);
        var robotsTxtServer = new RobotsTxtServer(robotsTxtConfig, pageFetcher);
        // controller.start是阻塞的，按循环次序进行
        myListController = new CommonController(config, pageFetcher, robotsTxtServer);
        CrawlController.WebCrawlerFactory<WebCrawler> factory = crawlerEnum
                .getCrawlerClass()::newInstance;
        // 非阻塞
        myListController.start(factory, NUMBER_OF_CRAWLERS);

        int s = start;
        while (s <= end) {
            // 分页获取web site
            List<String> webSites = null;
            try {
                webSites = urlSourceBuilder.batchGetUrls(s, step);
                if (webSites == null || webSites.isEmpty()) {
                    log.warn("url empty!");
                    continue;
                }
                // 入队列
                // 此时控制入队列速度，网页处理间隔 * 批处理条数 / 2;
                myListController.addUrlsToQueue(webSites);
                log.info("url list enter queue. size:{}", webSites.size());
                s += step;
                Thread.sleep((long) config.getPolitenessDelay() * step / 2);
            } catch (Exception e) {
                log.warn("Exception:", e);
            }
        }
        // 完成 停顿10秒，等待后续是否有动态加入队列的
        Thread.sleep(10000);
        myListController.putQueueFinish();
    }

    public boolean getQueueFinish() {
        return myListController.isSchedulePutQueueFinish();
    }
}
