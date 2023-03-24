package idea.bios.jobs.example.com.chunyuyisheng;

import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.controller.ControllerFacade;
import idea.bios.url.WebURL;
import idea.bios.util.JsoupUtils;
import idea.bios.util.Schedule;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * 春雨医生文章
 * https://www.chunyuyisheng.com/pc/article/107395/
 * @author 86153
 */
@Slf4j
public class CyysArticleCrawler extends AbsCommonCrawler {
    public CyysArticleCrawler(ControllerFacade controllerFacade) {
        super(controllerFacade);
    }

    @Override
    public void visit(Page page) {
        super.commonHtmlPageVisit(page, html -> {
            var result = new HashMap<String, Object>();
            Document doc = Jsoup.parseBodyFragment(html);
            String title = Objects.requireNonNull(
                    doc.select("div.clearfix > div.main-wrap > h1")
                            .first()).text();
            result.put("title", title);
            String time = Objects.requireNonNull(
                    doc.select("div.clearfix > div.main-wrap > p.time")
                            .first()).text();
            result.put("time", time);
            String desc = Objects.requireNonNull(
                    doc.select("div.clearfix > div.main-wrap > p.desc")
                            .first()).text();
            result.put("desc", desc);
            Element content = doc.select("div.clearfix > div.main-wrap > div").first();
            if (content == null) {
                return null;
            }
            result.put("content", JsoupUtils.getBeautifulText(content));
            result.put("imgs", JsoupUtils.getElementAllImgSrc(content));
            // result.put("rawContent", content.html());
            return result;
        });
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://www.chunyuyisheng.com");
    }

    @Override
    protected boolean shouldParse(WebURL url) {
        return url.getURL().startsWith("https://www.chunyuyisheng.com/pc/article");
    }

    @Override
    public void prepareToRun() {
        Schedule.crawlerScheduleAtFixedRate(()-> {
            var seeds = new ArrayList<String>();
            seeds.add("https://www.chunyuyisheng.com/pc/article/" + INT_FLAG.incrementAndGet());
            controllerFacade.addUrlsToQueue(seeds);
        }, 1);
    }
}
