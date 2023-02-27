package idea.bios.crawler.my.starter;

import idea.bios.crawler.CrawlConfig;
import idea.bios.crawler.CrawlController;
import idea.bios.crawler.WebCrawler;
import idea.bios.crawler.my.controller.CommonController;
import idea.bios.crawler.my.Tools;
import idea.bios.crawler.my.seed.SeedFetcher;
import idea.bios.crawler.my.seed.SeedFetcherImpl;
import idea.bios.crawler.my.sites.ListCrawlerEnum;
import idea.bios.fetcher.PageFetcher;
import idea.bios.robotstxt.RobotsTxtConfig;
import idea.bios.robotstxt.RobotsTxtServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.util.List;

/**
 * 启动类，crawler启动入口
 * @author 86153
 */
@Slf4j
public class CommonCrawlerStarter {
    /**
     * 并行线程数
     */
    private static final int NUMBER_OF_CRAWLERS = 5;
    /**
     * 爬虫配置
     */
    private final CrawlConfig config;
    /**
     * Robots.txt配置
     */
    private final RobotsTxtConfig robotsTxtConfig;
    /**
     * 控制器
     */
    private CommonController myListController;
    /**
     * seed的获取类，常用于持续性加入队列
     */
    @Getter
    private final SeedFetcher seedFetcher;
    /**
     * 持久化数据目录，一般不需要改变
     */
    private static final String CRAW_STORAGE_FOLDER = "./crawl/root";

    public CommonCrawlerStarter(CrawlConfig config) {
        this.config = config;
        this.robotsTxtConfig = Tools.defaultRobotsBuilder();
        seedFetcher = new SeedFetcherImpl();
    }

    public CommonCrawlerStarter() {
        this.config = Tools.listConfigBuilder(1000);
        this.robotsTxtConfig = Tools.defaultRobotsBuilder();
        seedFetcher = new SeedFetcherImpl();
    }

    /**
     * 在执行的过程中，有需要写入队列的
     * @param pageUrls  urls
     */
    public void addUrlsToQueue(List<String> pageUrls) {
        if (myListController == null || pageUrls == null || pageUrls.isEmpty()) {
            log.warn("add nothing to queue.");
            return;
        }
        myListController.addUrlsToQueue(pageUrls);
    }

    /**
     * 启动一个没有初始seed的crawler
     * @param crawlerEnum   crawlerEnum
     * @throws Exception    Exception
     */
    public void run(ListCrawlerEnum crawlerEnum) throws Exception {
        this.run(crawlerEnum, (offset, limit)-> null, 0, 0, 0);
    }

    /**
     * 直接加载seed的crawler
     * @param crawlerEnum   crawlerEnum
     * @param originalUrls  初始的seeds
     * @throws Exception    Exception
     */
    public void run(ListCrawlerEnum crawlerEnum, List<String> originalUrls) throws Exception {
        this.run(crawlerEnum, (offset, limit)-> originalUrls,
                originalUrls.size(), 0, originalUrls.size());
    }

    /**
     * 爬虫crawler启动类（支持分页）
     * @param crawlerEnum           crawler枚举
     * @param urlSourceBuilder      urlSourceBuilder
     */
    public void run(ListCrawlerEnum crawlerEnum, URLSourceBuilder urlSourceBuilder,
                    // 分页参数
                    int step, final int start, final int end) throws Exception {
        config.setCrawlStorageFolder(CRAW_STORAGE_FOLDER);
        config.setRespectNoIndex(false);
        // 不关闭进程，而是从其他途径不断加入seed
        config.setContinuousPutSeeds(true);
        // 先启动一个空队列的Controller
        var pageFetcher = new PageFetcher(config);
        var robotsTxtServer = new RobotsTxtServer(robotsTxtConfig, pageFetcher);
        // controller.start是阻塞的，按循环次序进行
        myListController = new CommonController(config, pageFetcher, robotsTxtServer);
        CrawlController.WebCrawlerFactory<WebCrawler> factory = crawlerEnum
                .getCrawlerClass()::newInstance;
        // 阻塞
        if (!config.isContinuousPutSeeds()) {
            myListController.putQueueFinish();
        }
        myListController.start(factory, NUMBER_OF_CRAWLERS);

        // 判断参数
        if (step <= 0 || start < 0 || start > end) {
            return;
        }
        int s = start;
        while (s <= end) {
            // 分页获取web site
            List<String> webSites;
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
    }
}
