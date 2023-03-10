package idea.bios.jobs.com.chunyuyisheng;

import com.google.gson.Gson;
import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.sites.CrawlerSiteEnum;
import idea.bios.crawler.my.starter.CommonCrawlerStarter;
import idea.bios.url.WebURL;
import idea.bios.util.Schedule;
import idea.bios.util.search.BaiduSfSearchLinks;
import idea.bios.util.search.CyysDialogSearchLinks;
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

import static idea.bios.crawler.my.Config.configBuilder;

/**
 * 春雨医生对话
 * @author 86153
 */
@Slf4j
public class CyysDialogCrawler extends AbsCommonCrawler {
    private static final AtomicInteger START_INT = new AtomicInteger(0);

    @Override
    protected Map<String, ?> getSingleHtmlInfo(String html) {
        var result = new HashMap<String, Object>();
        Document doc = Jsoup.parseBodyFragment(html);
        String title = doc.selectFirst("div > span.title").text();
        result.put("title", title);
        // 医生信息
        Elements docInfoElements = doc.select("div.bread-crumb-top > a.crumb-top-link");
        String docSimpleInfo = null;
        for (Element de : docInfoElements) {
            docSimpleInfo = de.text();
        }
        var authorInfoMap = new HashMap<String, Object>();
        String[] strings = docSimpleInfo.split(" ");
        authorInfoMap.put("name", strings[1]);
        authorInfoMap.put("hospital", strings[0]);
        result.put("authorInfo", new Gson().toJson(authorInfoMap));
        // 对话
        Element dialog = doc.selectFirst("div.main-wrap > div.problem-detail-wrap");
        if (dialog == null) {
            return null;
        }
        Elements ds = dialog.getElementsByClass("block-line");
        var dialogList = new ArrayList<String>();
        ds.forEach(d -> {
            Elements pa = d.select("div > div > p.blue");
            if (pa != null && !pa.isEmpty()) {
                // 患者
                dialogList.add("Q:" + pa.text());
            } else {
                pa = d.select("div > div > p");
                // 医生
                dialogList.add("A:" + pa.text());
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
                url.getURL().startsWith("https://www.chunyuyisheng.com/pc/qa/");
    }

    @Override
    public void prepareToRun(CommonCrawlerStarter listStarter) {
        var searchLinks = new CyysDialogSearchLinks();
        Schedule.scheduleAtFixedRate(()-> {
            List<String> sUrls = seedFetcher.getSeedsFromDb(
                    START_INT.getAndIncrement(),
                    1,
                    term ->CyysDialogSearchLinks.CYYS_DIALOG_PREFIX + term);
            if (!sUrls.isEmpty()) {
                listStarter.addUrlsToQueue(searchLinks.getLinks(sUrls.get(0)));
            }}, 5);
    }

    public static void main(String[] args) throws IOException {
        new CyysDialogCrawler().testGetHtmlInfo(
                "https://www.chunyuyisheng.com/pc/qa/ZqwVbxyV62DKlViFMHtP8g/");
    }
}
