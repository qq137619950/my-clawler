package idea.bios.jobs.com.dxy;

import com.google.gson.Gson;
import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.sites.CrawlerSiteEnum;
import idea.bios.crawler.my.starter.CommonCrawlerStarter;
import idea.bios.url.WebURL;
import idea.bios.util.Schedule;
import idea.bios.util.search.DxyDialogSearchLinks;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static idea.bios.crawler.my.Tools.configBuilder;

/**
 * 丁香园多轮问答  https://dxy.com/question/102688528
 * @author 86153
 */
@Slf4j
public class DxyDialogCrawler extends AbsCommonCrawler {
    private static final AtomicInteger START_INT = new AtomicInteger(0);

    @Override
    protected Map<String, ?> getSingleHtmlInfo(String html) {
        // 只有医生信息和多轮问答
        var result = new HashMap<String, Object>();
        Document doc = Jsoup.parseBodyFragment(html);
        // 医生信息
        Element author = doc.selectFirst("div.doctor-info > div.doctor-detail");
        if (author == null) {
            log.warn("no author info.");
            return null;
        }
        var authorInfoMap = new HashMap<String, Object>();
        authorInfoMap.put("name", author.selectFirst("div.doctor-header > div").text());
        authorInfoMap.put("jobTitle", author.selectFirst("div.doctor-body > div:nth-child(1)").text());
        authorInfoMap.put("department", author.selectFirst("div.doctor-body > div:nth-child(2)").text());
        authorInfoMap.put("hospital", author.selectFirst("div.doctor-body > div:nth-child(3)").text());
        result.put("authorInfo", new Gson().toJson(authorInfoMap));
        // 对话信息
        Element dialog = doc.selectFirst("div.question-detail-dialogs > div.dialogs");
        if (dialog == null) {
            log.warn("no dialog info.");
            return null;
        }
        Elements contents = dialog.select(".dialog-content");
        var dialogList = new ArrayList<String>();
        if (contents == null) {
            log.warn("no dialog info.");
            return null;
        }
        contents.forEach(c -> {
            if (c.parent().is(".theme-white")) {
                dialogList.add("A:" + c.text());
            } else if (c.parent().is(".theme-dark")) {
                dialogList.add("Q:" + c.text());
            }
        });
        result.put("dialog", dialogList);
        return result;
    }

    @Override
    public void visit(Page page) {
        super.commonPageVisit(page);
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://dxy.com/question/");
    }

    @Override
    public void runner() throws Exception {
        crawlerSiteEnum = CrawlerSiteEnum.findCrawlerSiteEnumByClass(this.getClass());
        listStarter = new CommonCrawlerStarter(configBuilder(
                -1, 2000, false));
        var searchLinks = new DxyDialogSearchLinks();
        Schedule.scheduleAtFixedRate(()-> {
            List<String> sUrls = seedFetcher.getSeedsFromDb(
                    START_INT.getAndIncrement(),
                    1,
                    DxyDialogCrawler::getSearchUrlPrefix);
            if (!sUrls.isEmpty()) {
                listStarter.addUrlsToQueue(searchLinks.getAllLinks(sUrls.get(0)));
            }}, 30);
        listStarter.run(crawlerSiteEnum, seedFetcher.getSeedsPlain(
                "https://dxy.com/question/50314296"));
    }

    public static void main(String[] args) throws IOException {
        new DxyDialogCrawler().testGetHtmlInfo(
                "https://dxy.com/question/54875205");
    }

    private static String getSearchUrlPrefix(String term) {
        return "https://dxy.com/search/questions/" + term + "?page_index=";
    }
}
