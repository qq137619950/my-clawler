package idea.bios.crawler.my.sites;

import idea.bios.crawler.WebCrawler;
import lombok.Getter;
import lombok.Setter;

/**
 * 爬虫任务配置在这里
 */
public enum SeedCrawlerEnum {
//    baikemyDisease("https://www.baikemy.com/disease/detail/841",
//            BaikemyDiseaseCrawler.class),
//    baikemyMedicine("https://www.baikemy.com/medicine/detail/44672583836584",
//            BaikemyMedicineCrawler.class),
//    tsuTw("https://www.tsu.tw/edu/14227.html",
//            TsuTwCrawlerMy.class),
//    yixue("https://www.yixue.com/%E8%83%B0%E5%B2%9B",
//            YixueCrawler.class),
//    xywy("http://dxb.xywy.com/zhinan/arc621194.html," +
//            "http://www.xywy.com/yspd/jksd/200908/12-527928.html," +
//            "http://tnb.xywy.com/yufangbaojian/663716.html",
//            XywyCrawler.class),
//
//    baidu_baike("https://baike.baidu.com/item/%E8%84%91%E8%A1%80%E7%AE%A1%E7%96%BE%E7%97%85",
//            BaiduBaikeCrawlerSeed.class),
//
//    weixin("https://mp.weixin.qq.com/s?__biz=Mzg3ODgwNjUyOA==&mid=2247483848&idx=1&sn=c989f59e0d2c32a3ac5ff29c9a1a2faa&chksm=cf0f52a7f878dbb19efe12885a603923cc424475f6b0247d6f6ac8b37b7221ec5f04568e64ab&scene=132#wechat_redirect",
//            StrokeLinkCrawler.class),
//
//    youlai("https://www.youlai.cn/dise/1845.html,https://www.youlai.cn/dise/292.html", YouLaiBaikeCrawler.class),
//
//    youlai_article("https://www.youlai.cn/yyk/article/299240.html," +
//            "https://www.youlai.cn/yyk/article/290052.html," +
//            "https://www.youlai.cn/yyk/article/342029.html," +
//            "https://www.youlai.cn/yyk/article/353623.html," +
//            "https://www.youlai.cn/yyk/article/354302.html", YouLaiArticleCrawler.class),
//
//    youlai_qa("https://www.youlai.cn/ask/1767333.html," +
//            "https://www.youlai.cn/ask/195658.html," +
//            "https://www.youlai.cn/ask/165148l5xJF.html", YouLaiQACrawler.class),
//
//    wiki_baidu_baike("https://zh.iwiki.icu/wiki/%E4%B8%AD%E9%A2%A8", WikiBaikeCrawler.class),
//
//    idea_text_all("https://idea.edu.cn/", IdeaCrawler.class),
//
//    jbk39_baike_all("https://jbk.39.net/yg/jbzs", JBK39Crawler.class),
//
//    mfk_all("https://www.mfk.com/ask/yinpin/2210871.shtml," +
//            "https://www.mfk.com/shipin/2221463.shtml," +
//            "https://www.mfk.com/ask/11160281.shtml", MfkSeedCrawler.class),
//
//    miaoshou_all(
//            "https://www.miaoshou.net/article/8xOeynwqMaz26Zdp.html," +
//            "https://www.miaoshou.net/voice/4q3R1eqDPgKowWDK.html," +
//            "https://www.miaoshou.net/voice/KlPyz6jMD3DeExXw.html," +
//            "https://www.miaoshou.net/question/v0wyROqjbW7ExNln.html" , MiaoShouCrawlerNew.class),
//
//    familydoctor_article("https://www.familydoctor.com.cn/",
//            FamilyDoctorCrawler.class),
//
//    haodf_article("https://www.haodf.com/neirong/wenzhang/8186325828.html",
//            HaodfCrawler.class),
//    bioon_article("https://news.bioon.com/article/c4cce217471a.html",
//            BioonArticleSeedCrawler.class),
//    chunyuyisheng_article("https://www.chunyuyisheng.com",
//            CYYSCrawler.class),
//    medsci_article("https://www.medsci.cn/department/details?s_id=2",
//            MedsciSeedCrawler.class),
//    baidu_bh_article("https://m.baidu.com/bh/m/detail/ar_6442070064922690834",
//            BaiduBhCrawlerSeed.class),
//    jkb_article("https://www.jkb.com.cn/news/technology/",
//            JKBSeedCrawler.class),
//    health_peaple_article("http://health.people.com.cn/n1/2021/0806/c14739-32183529.html",
//            HealthPeopleSeedCrawler.class),
//    cn_news_article("http://www.news.cn/health/",
//            CNNewsSeedCrawler.class),
//    healthcare_article("https://www.cn-healthcare.com/articlewm/20220813/content-1417719.html",
//            HealthcareSeedCrawler.class);
    ;

    private String url;
    private Class<? extends WebCrawler> crawlerClass;

    SeedCrawlerEnum(String url, Class<? extends WebCrawler> crawlerClass) {
        this.url = url;
        this.crawlerClass = crawlerClass;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Class<? extends WebCrawler> getCrawlerClass() {
        return crawlerClass;
    }

    public void setCrawlerClass(Class<? extends WebCrawler> crawlerClass) {
        this.crawlerClass = crawlerClass;
    }
}

