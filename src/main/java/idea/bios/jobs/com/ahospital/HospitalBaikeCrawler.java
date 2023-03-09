package idea.bios.jobs.com.ahospital;

import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.sites.CrawlerSiteEnum;
import idea.bios.crawler.my.starter.CommonCrawlerStarter;
import idea.bios.url.WebURL;
import idea.bios.util.Schedule;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static idea.bios.crawler.my.Tools.configBuilder;

/**
 * http://www.a-hospital.com/w/医学电子书
 * @author 86153
 */
@Slf4j
public class HospitalBaikeCrawler extends AbsCommonCrawler {
    private static final AtomicInteger START_INT = new AtomicInteger(0);

    @Override
    protected Map<String, ?> getSingleHtmlInfo(String html) {
        var result = new HashMap<String, Object>();
        Document doc = Jsoup.parseBodyFragment(html);
        // 标题
        String title = doc.selectFirst("#firstHeading").text();
        result.put("title", title);
        // 层级标签
        Element nav = doc.selectFirst(
                "#bodyContent > table.hierarchy-breadcrumb > tbody > tr > td > small");
        if (nav == null) {
            nav = doc.selectFirst("#bodyContent > table.nav > tbody > tr > td");
        }
        if (nav != null) {
            var tagList = new ArrayList<String>();
            for(String t : nav.text().split(">>")) {
                if (t.contains("A+医学百科")) {
                    continue;
                }
                tagList.add(t.replaceAll(" +",""));
            }
            result.put("tag", tagList);
        }

        // 正文
        Element bodyContent = doc.selectFirst("#bodyContent");
        var contentList = new ArrayList<String>();
        for (Element e : bodyContent.children()) {
            if (e.text() != null && e.text().contains("参看")) {
                break;
            }
            if (e.is("h2") || e.is("h3") || e.is("p")
                || e.is("ul")) {
                contentList.add(e.text());
            }
        }
        result.put("content", String.join("\n", contentList));
        // 来源
        Element info = doc.selectFirst("#footer-info-credits");
        if (info != null) {
            result.put("info", info.text());
        }
        return result;
    }

    @Override
    public void visit(Page page) {
        super.commonPageVisit(page, crawlerSiteEnum.getSourceId());
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("http://www.a-hospital.com/w/") &&
                !url.getURL().contains("中医");
    }

    @Override
    public void runner() throws Exception {
        crawlerSiteEnum = CrawlerSiteEnum.findCrawlerSiteEnumByClass(this.getClass());
        listStarter = new CommonCrawlerStarter(configBuilder(
                -1, 1000, false));
        Schedule.scheduleAtFixedRate(()-> {
            List<String> sUrls = listStarter.getSeedFetcher().getSeedsFromDb(
                    START_INT.getAndIncrement(),
                    1,
                    term -> "http://www.a-hospital.com/w/" + term);
            if (!sUrls.isEmpty()) {
                listStarter.addUrlsToQueue(sUrls);
            }}, 5);
        var seeds = new ArrayList<String>();
        seeds.add("http://www.a-hospital.com/w/%E8%A7%A3%E5%89%96%E5%AD%A6/%E5%BF%83%E8%A1%80%E7%AE%A1");
        listStarter.run(crawlerSiteEnum, seeds);
    }

    public static void main(String[] args) throws IOException {
        new HospitalBaikeCrawler().testGetHtmlInfo(
                "http://www.a-hospital.com/w/%E5%8F%A3%E8%85%94");
    }
}
