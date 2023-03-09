package idea.bios;

import idea.bios.crawler.my.sites.CrawlerSiteEnum;
import idea.bios.jobs.com.ahospital.HospitalBaikeCrawler;
import idea.bios.jobs.com.bh.BaiduBhListCrawler;
import idea.bios.jobs.com.chunyuyisheng.CyysDialogCrawler;
import idea.bios.jobs.com.dxy.DxyDialogCrawler;
import idea.bios.jobs.com.mfk.MkfQaCrawler;
import idea.bios.util.search.MfkQaSearchLinks;

/**
 * 启动类
 * @author 86153
 */
public class Main {
    public static void main(String[] args) throws Exception {
        CrawlerSiteEnum cur = CrawlerSiteEnum.a_hospital;
        cur.getCrawlerClass().newInstance().runner();
    }
}
