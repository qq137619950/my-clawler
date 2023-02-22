package idea.bios.crawler.my.sites;


import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.jobs.com.bh.BaiduBhListCrawler;

/**
 * @author 86153
 */

public enum ListCrawlerEnum {

    baidu_bh_list(BaiduBhListCrawler.class);

    private Class<? extends AbsCommonCrawler> crawlerClass;

    ListCrawlerEnum(Class<? extends AbsCommonCrawler> crawlerClass) {
        this.crawlerClass = crawlerClass;
    }

    public Class<? extends AbsCommonCrawler> getCrawlerClass() {
        return crawlerClass;
    }

    public void setCrawlerClass(Class<? extends AbsCommonCrawler> crawlerClass) {
        this.crawlerClass = crawlerClass;
    }
}
