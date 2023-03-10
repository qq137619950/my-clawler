package idea.bios.jobs.com.yixue;

import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.starter.CommonCrawlerStarter;
import idea.bios.url.WebURL;
import idea.bios.util.Schedule;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * https://www.yixue.com/
 * @author 86153
 */
@Slf4j
public class YixueCrawler extends AbsCommonCrawler {
    private static final AtomicInteger START_INT = new AtomicInteger(0);
    @Override
    protected Map<String, ?> getSingleHtmlInfo(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        var result = new HashMap<String, Object>();
        // title
        result.put("title", Objects.requireNonNull(doc.selectFirst(
                "#firstHeading")).text());
        // content
        Element content = doc.selectFirst("#mw-content-text > div.mw-parser-output");
        if (content == null) {
            return null;
        }
        var contentList = new ArrayList<String>();
        content.children().forEach(c -> {
            if ((c.is("p") || c.is("h2")) && !"参看".equals(c.text())) {
                contentList.add(c.text());
            }
        });
        result.put("content", String.join("\n", contentList).trim());
        return result;
    }

    @Override
    public void visit(Page page) {
        super.commonPageVisit(page);
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://www.yixue.com/") &&
                !url.getURL().contains("#");
    }

    @Override
    public boolean shouldAddLinkQueue(WebURL url) {
        return url.getURL().startsWith("https://www.yixue.com/") &&
                !url.getURL().contains("#");
    }

    @Override
    public void prepareToRun(CommonCrawlerStarter listStarter) {
        Schedule.scheduleAtFixedRate(()-> {
            List<String> sUrls = seedFetcher.getSeedsFromDb(
                    START_INT.getAndIncrement(),
                    10,
                    term -> "https://www.yixue.com/" + term);
            if (!sUrls.isEmpty()) {
                listStarter.addUrlsToQueue(sUrls);
            }}, 10);
    }

    public static void main(String[] args) throws IOException {
        new YixueCrawler().testGetHtmlInfo("https://www.yixue.com/%E4%BD%8E%E7%83%AD");
    }
}
