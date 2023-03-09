package idea.bios.crawler.my.sites;


import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.jobs.com.ahospital.HospitalBaikeCrawler;
import idea.bios.jobs.com.bh.BaiduBhListCrawler;
import idea.bios.jobs.com.chunyuyisheng.CyysDialogCrawler;
import idea.bios.jobs.com.dxy.DxyDialogCrawler;
import idea.bios.jobs.com.mfk.MkfQaCrawler;

/**
 * @author 86153
 */

public enum CrawlerSiteEnum {
    /**
     * 百度博禾医生
     */
    baidu_bh_list(BaiduBhListCrawler.class, "com.baidu.bh.article.qa"),
    /**
     * 丁香园 对话
     */
    dxy_dialog(DxyDialogCrawler.class, ""),
    /**
     * a hospital
     */
    a_hospital(HospitalBaikeCrawler.class, "com.a.hospital.baike"),
    /**
     * 民福康 问答
     */
    mfk_qa(MkfQaCrawler.class, "com.mfk.qa"),
    /**
     * 春雨医生 对话
     */
    cyys_dialog(CyysDialogCrawler.class, "");



    private Class<? extends AbsCommonCrawler> crawlerClass;

    public String getSourceId() {
        return sourceId;
    }

    CrawlerSiteEnum(Class<? extends AbsCommonCrawler> crawlerClass, String sourceId) {
        this.crawlerClass = crawlerClass;
        this.sourceId = sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    private String sourceId;



    public Class<? extends AbsCommonCrawler> getCrawlerClass() {
        return crawlerClass;
    }

    public void setCrawlerClass(Class<? extends AbsCommonCrawler> crawlerClass) {
        this.crawlerClass = crawlerClass;
    }

    public static CrawlerSiteEnum findCrawlerSiteEnumByClass(Class<? extends AbsCommonCrawler> clazz) {
        if (clazz == null) {
            return null;
        }
        for(CrawlerSiteEnum cse : CrawlerSiteEnum.values()) {
            if (cse.getCrawlerClass().equals(clazz)) {
                return cse;
            }
        }
        return null;
    }
}
