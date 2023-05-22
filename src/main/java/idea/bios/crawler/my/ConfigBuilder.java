package idea.bios.crawler.my;

import idea.bios.config.SiteConfig;
import idea.bios.crawler.CrawlConfig;
import idea.bios.robotstxt.RobotsTxtConfig;
import lombok.var;
import org.apache.http.message.BasicHeader;

import java.util.HashSet;

/**
 * @author 86153
 */
public class ConfigBuilder {
    private static final String CRAW_STORAGE_FOLDER = "./data/crawl/root";

    public static RobotsTxtConfig defaultRobotsBuilder() {
        var robotsTxtConfig = new RobotsTxtConfig();
        // robotsTxtConfig.setEnabled(false);
        robotsTxtConfig.setUserAgentName("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/603.3.8 (KHTML, like Gecko) Version/10.1.2 Safari/603.3.8");
        return robotsTxtConfig;
    }

    public static CrawlConfig configBuilder(int maxDepthOfCrawling,
                                            int politenessDelay) {
        CrawlConfig config = ConfigBuilder.defaultConfigBuilder();
        config.setMaxDepthOfCrawling(maxDepthOfCrawling);
        config.setPolitenessDelay(politenessDelay);
        return config;
    }

    public static CrawlConfig configBuilder(int maxDepthOfCrawling,
                                            int politenessDelay,
                                            boolean chromeDriver,
                                            boolean phantomJsDriver,
                                            boolean isDynParse) {
        CrawlConfig config = ConfigBuilder.defaultConfigBuilder();
        config.setMaxDepthOfCrawling(maxDepthOfCrawling);
        config.setPolitenessDelay(politenessDelay);
        config.setChromeDriver(chromeDriver);
        config.setPhantomJsDriver(phantomJsDriver);
        config.setDynParse(isDynParse);
        return config;
    }

    public static CrawlConfig configBuilder(int maxDepthOfCrawling,
                                            int politenessDelay,
                                            int connectionTimeout) {
        CrawlConfig config = ConfigBuilder.defaultConfigBuilder();
        config.setMaxDepthOfCrawling(maxDepthOfCrawling);
        config.setConnectionTimeout(connectionTimeout);
        config.setPolitenessDelay(politenessDelay);
        return config;
    }


    public static CrawlConfig defaultConfigBuilder() {
        var config = new CrawlConfig();
        config.setUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/603.3.8 (KHTML, like Gecko) Version/10.1.2 Safari/603.3.8");
        config.setCrawlStorageFolder(CRAW_STORAGE_FOLDER);
        config.setRespectNoIndex(false);
        config.setResumableCrawling(false);
        // 爬虫深度   -1 代表无限
        config.setMaxDepthOfCrawling(-1);
        // 加载配置
        config.setPolitenessDelay(SiteConfig.getCurSite().getMinPolitenessDelay());
        config.setChromeDriver(SiteConfig.getCurSite().isChromeDriver());
        config.setPhantomJsDriver(SiteConfig.getCurSite().isPhantomJsDriver());
        config.setDynParse(SiteConfig.getCurSite().isDynParse());

        // Header
        var collections = new HashSet<BasicHeader>();
        collections.add(new BasicHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"));
        collections.add(new BasicHeader("Accept-Encoding", "gzip,deflate,sdch"));
        collections.add(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6"));
        collections.add(new BasicHeader("Content-Type","application/x-www-form-urlencoded;charset=UTF-8"));
        collections.add(new BasicHeader("Connection", "keep-alive"));
        // temp
        collections.add(new BasicHeader("Host", "bingli.iiyi.com"));
        collections.add(new BasicHeader("Cookie", "__jsluid_s=3c218313081942f87adee298effb1b7c; UM_distinctid=18842507e6cd8a-0e5d693b494b9-26031a51-e1000-18842507e6daa4; Hm_lvt_941d74201fa27a7df77f1985ac333ff7=1684739752; cnaday_auth_visit=6ae52165976d8f3af9ecc5984373e51a; CNZZDATA30080017=cnzz_eid%3D714144329-1684737258-%26ntime%3D1684744460; CNZZDATA30079332=cnzz_eid%3D752632641-1684737421-%26ntime%3D1684745093; CNZZDATA1281110805=296794688-1684736782-%7C1684745067; cnaday_uuid=3def9xOdzjVchoA0IrFDQkVre1Ym3GZrVt4mEDdPx5ntJ10LGASuD2AQ2P8lHIUGJGJna4FS4McP%2Bu5Pmo9tf; Hm_lpvt_941d74201fa27a7df77f1985ac333ff7=1684746358"));

        config.setDefaultHeaders(collections);
        config.setIncludeBinaryContentInCrawling(false);
        return config;
    }


}
