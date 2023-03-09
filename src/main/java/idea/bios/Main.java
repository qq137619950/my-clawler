package idea.bios;

import idea.bios.crawler.my.sites.CrawlerSiteEnum;


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
