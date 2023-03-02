package idea.bios.crawler.my.sites;


import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.jobs.com.bh.BaiduBhListCrawler;
import idea.bios.jobs.com.chunyuyisheng.CyysDialogCrawler;
import idea.bios.jobs.com.dxy.DxyDialogCrawler;

/**
 * @author 86153
 */

public enum ListCrawlerEnum {

    baidu_bh_list(BaiduBhListCrawler.class),

    dxy_dialog(DxyDialogCrawler.class),

    cyys_dialog(CyysDialogCrawler.class);

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
