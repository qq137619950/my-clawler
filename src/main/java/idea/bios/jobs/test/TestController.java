package idea.bios.jobs.test;

import idea.bios.crawler.CrawlConfig;
import idea.bios.crawler.CrawlController;
import idea.bios.fetcher.PageFetcher;
import idea.bios.robotstxt.RobotsTxtConfig;
import idea.bios.robotstxt.RobotsTxtServer;
import lombok.var;

/**
 * 测试工程是否跑通
 * @author 86153
 */
public class TestController {
    public static void main(String[] args) throws Exception {
        String crawlStorageFolder = "/data/crawl/root";
        int numberOfCrawlers = 3;

        var config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        // 测试代理
        config.setProxyHost("47.106.191.148");
        config.setProxyPort(443);

        // Instantiate the controller for this crawl.
        var pageFetcher = new PageFetcher(config);
        var robotsTxtConfig = new RobotsTxtConfig();
        var robotsTxtServer = new RobotsTxtServer(robotsTxtConfig, pageFetcher);
        var controller = new CrawlController(config, pageFetcher, robotsTxtServer);

        // For each crawl, you need to add some seed urls. These are the first
        // URLs that are fetched and then the crawler starts following links
        // which are found in these pages
        controller.addSeed("https://www.chunyuyisheng.com/pc/qa/Flul6ZGrH9G269aZq484UA/");

        // The factory which creates instances of crawlers.
        CrawlController.WebCrawlerFactory<TestCrawler> factory = TestCrawler::new;

        // Start the crawl. This is a blocking operation, meaning that your code
        // will reach the line after this only when crawling is finished.
        controller.start(factory, numberOfCrawlers);
    }
}
