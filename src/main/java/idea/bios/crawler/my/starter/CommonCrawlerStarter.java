package idea.bios.crawler.my.starter;

import idea.bios.config.SiteConfig;
import idea.bios.crawler.CrawlConfig;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.controller.CommonController;
import idea.bios.crawler.my.ConfigBuilder;

import idea.bios.crawler.my.controller.ControllerFacade;
import idea.bios.crawler.proxypool.ProxyPoolFetcher;
import idea.bios.fetcher.PageFetcher;
import idea.bios.robotstxt.RobotsTxtConfig;
import idea.bios.robotstxt.RobotsTxtServer;

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
    private CommonController controller;

    /**
     * 持久化数据目录，一般不需要改变
     */
    private static final String CRAW_STORAGE_FOLDER = "./crawl/root";

    public CommonCrawlerStarter() {
        this.config = ConfigBuilder.defaultConfigBuilder();
        this.robotsTxtConfig = ConfigBuilder.defaultRobotsBuilder();
    }

    /**
     * 启动一个没有初始seed的crawler
     * @throws Exception    Exception
     */
    public void run() throws Exception {
        this.run((offset, limit)-> null, 0, 0, 0);
    }

    /**
     * 直接加载seed的crawler
     * @param originalUrls  初始的seeds
     * @throws Exception    Exception
     */
    public void run(List<String> originalUrls) throws Exception {
        this.run((offset, limit)-> originalUrls,
                originalUrls.size(), 0, originalUrls.size());
    }

    /**
     * 爬虫crawler启动类（支持分页）
     * @param urlSourceBuilder      urlSourceBuilder
     */
    public void run(URLSourceBuilder urlSourceBuilder,
                    // 分页参数
                    int step, final int start, final int end) throws Exception {

        // 不关闭进程，而是从其他途径不断加入seed
        config.setContinuousPutSeeds(true);
        // 先启动一个空队列的Controller
        // 将pageFetcher放在crawler线程中
        var robotsTxtServer = new RobotsTxtServer(robotsTxtConfig, new PageFetcher(config));
        // controller.start是阻塞的，按循环次序进行
        controller = new CommonController(config, robotsTxtServer,
                SiteConfig.getCurSite().getSourceId());
        // 阻塞
        if (!config.isContinuousPutSeeds()) {
            controller.putQueueFinish();
        }
        // 执行特定函数
        Class<? extends AbsCommonCrawler> crawlerClass = (Class<? extends AbsCommonCrawler>)
                Class.forName(SiteConfig.getJobRoot() + "." + SiteConfig.getCurSite().getClassName());
        AbsCommonCrawler crawlerTemp = crawlerClass.getDeclaredConstructor(ControllerFacade.class)
                .newInstance(this.controller);
        crawlerTemp.prepareToRun();
        // 开启
        controller.start(crawlerClass, ProxyPoolFetcher.getCurProxyPoolSize());

        // 判断参数 遗留逻辑
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
                controller.addUrlsToQueue(webSites);
                log.info("url list enter queue. size:{}", webSites.size());
                s += step;
                Thread.sleep((long) config.getPolitenessDelay() * step / 2);
            } catch (Exception e) {
                log.warn("Exception:", e);
            }
        }
    }
}
