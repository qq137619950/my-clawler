package idea.bios.jobs.example.com.iiyi;

import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.controller.ControllerFacade;
import idea.bios.url.WebURL;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * 病例中心
 * <a href="https://bingli.iiyi.com/show/62218-1.html">...</a>
 * @author 86153
 */
public class IiyiBingchengDiseaseCrawler extends AbsCommonCrawler  {
    public IiyiBingchengDiseaseCrawler(ControllerFacade controllerFacade) {
        super(controllerFacade);
    }

    @Override
    public void visit(Page page) {
        super.commonHtmlPageVisit(page, html -> {
            var result = new HashMap<String, Object>();
            Document doc = Jsoup.parseBodyFragment(html);
            // index
            // body > div.t20.s_wz
            Elements index = doc.select("div.t20.s_wz > a");
            var indexList = new ArrayList<String>();
            index.forEach(i -> indexList.add(i.text()));
            result.put("menu", indexList);
            // title
            // div.fl > div > h1
            String title = Objects.requireNonNull(
                    doc.selectFirst("div.fl > div > h1").text());
            result.put("title", title);
            // info
            // div.fl > div > div.s_p2:
            Elements info = doc.select("div.fl > div > div.s_p2");
            var infoList = new ArrayList<String>();
            info.forEach(i -> infoList.add(i.text()));
            result.put("info", infoList);
            // content
            // div.fl > div > div > div.s_floorbox > div
            var contentMap = new LinkedHashMap<String, String>();
            Elements content = doc.select("div.fl > div > div > div.s_floorbox > div > p");
            content.forEach(c -> {
                String key = c.selectFirst("b").text()
                        .replaceAll("【", "")
                        .replaceAll("】", "");
                String value = c.selectFirst("span").text();
                contentMap.put(key, value);

            });
            result.put("content", contentMap);
            return result;
        });
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://bingli.iiyi.com/show");
    }

    @Override
    protected boolean shouldParse(WebURL url) {
        // shouldVisit即可
        return true;
    }

    @Override
    public void prepareToRun() {
        var urlList = new ArrayList<String>();
        IntStream.rangeClosed(200, 100000).forEach(i ->
                urlList.add("https://bingli.iiyi.com/show/" + i + "-1.html"));

        controllerFacade.addUrlsToQueue(urlList);
    }
}
