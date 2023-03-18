package idea.bios.crawler.my.sites;


import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.jobs.com.ahospital.HospitalBaikeCrawler;
import idea.bios.jobs.com.bh.BaiduBhListCrawler;
import idea.bios.jobs.com.chunyuyisheng.CyysDialogCrawler;
import idea.bios.jobs.com.dxy.DxyDialogCrawler;
import idea.bios.jobs.com.haodf.HaodfBingchengDiseaseProCrawler;
import idea.bios.jobs.com.mfk.MkfQaCrawler;
import idea.bios.jobs.com.yixue.YixueCrawler;
import idea.bios.jobs.net.health120.Health120DialogCrawler;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 原网站信息
 * @author 86153
 */
@AllArgsConstructor
public enum CrawlerSiteEnum {
    /**
     * 百度博禾医生
     */
    baidu_bh_list(BaiduBhListCrawler.class, "com.baidu.bh.article.qa", 1000),
    /**
     * 丁香园 对话
     */
    dxy_dialog(DxyDialogCrawler.class, "com.dxy.dialog", 1000),
    /**
     * a hospital
     */
    a_hospital(HospitalBaikeCrawler.class, "com.a.hospital.baike", 3000),
    /**
     * 民福康 问答
     */
    mfk_qa(MkfQaCrawler.class, "com.mfk.qa", 200),
    /**
     * 医学百科
     * https://www.yixue.com/
     */
    yixue_baike(YixueCrawler.class, "com.yixue.baike", 1000),
    /**
     * 120健康网
     * https://www.120.net/post/9163705.html
     */
    health120_dialog(Health120DialogCrawler.class, "health120.dialog", 300),
    /**
     * 春雨医生 对话
     */
    cyys_dialog(CyysDialogCrawler.class, "com.cyys.dialog", 500),
    /**
     * 好大夫 病程
     */
    haodf_disease_process(HaodfBingchengDiseaseProCrawler.class, "com.haodf.disease.process.v2",
            200);
    /**
     * job中的Class文件
     */
    @Getter
    private final Class<? extends AbsCommonCrawler> crawlerClass;

    /**
     * 资源ID
     */
    @Getter
    private final String sourceId;

    /**
     * 访问网页最小间隔
     */
    @Getter
    private final int minPolitenessDelay;

    /**
     * 通过class找enum
     * @param clazz     class
     * @return          CrawlerSiteEnum
     */
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
