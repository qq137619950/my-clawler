package idea.bios;

import idea.bios.crawler.my.sites.CrawlerSiteEnum;
import idea.bios.crawler.my.starter.CommonCrawlerStarter;

import static idea.bios.crawler.my.Config.configBuilder;


/**
 * 启动类
 * @author 86153
 */
public class Main {
    public static void main(String[] args) throws Exception {
        new CommonCrawlerStarter(configBuilder(-1,
                2000, false)).run(CrawlerSiteEnum.a_hospital);
    }
}
