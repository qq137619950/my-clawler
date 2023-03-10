package idea.bios.jobs.com.yixue;

import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.starter.CommonCrawlerStarter;
import idea.bios.url.WebURL;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * https://www.yixue.com/
 * @author 86153
 */
@Slf4j
public class YixueCrawler extends AbsCommonCrawler {
    @Override
    protected Map<String, ?> getSingleHtmlInfo(String html) {
        return null;
    }

    @Override
    public void visit(Page page) {

    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return false;
    }

    @Override
    public void prepareToRun(CommonCrawlerStarter listStarter) {

    }


    public static void main(String[] args) {

    }
}
