package idea.bios.jobs.net.health120;

import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.controller.ControllerFacade;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * https://www.120.net/post/1499768.html
 * @author 86153
 */
@Slf4j
public class Health120DialogCrawler extends AbsCommonCrawler {
    private static final AtomicInteger START_INT = new AtomicInteger(1000);
    public Health120DialogCrawler(ControllerFacade controllerFacade) {
        super(controllerFacade);
    }

    @Override
    public void visit(Page page) {
        super.commonHtmlPageVisit(page, html -> {
            Document doc = Jsoup.parseBodyFragment(html);
            var result = new HashMap<String, Object>();
            // title
            Element titleInfo = doc.selectFirst("body > div > div > div.p_lbox1");
            if (titleInfo == null) {
                return null;
            }
            result.put("title", titleInfo.selectFirst("span > h1[itemprop=name]").text());
            result.put("patientInfo", titleInfo.selectFirst("div > p.p_answer_usr").text());
            result.put("desc", titleInfo.selectFirst("div > p.p_artcont").text());
            // content
            Element contentInfo = doc.selectFirst("body > div > div > div.p_lbox2");
            if (contentInfo == null) {
                return null;
            }
            result.put("authorInfo", contentInfo.selectFirst("div > dl > dd.d1").text());
            // 处理content
            result.put("content", contentInfo.selectFirst("div > p.answer_p").text()
                    .replaceAll("追问：", "\n追问：")
                    .replaceAll("回复：", "\n回复："));
            return result;
        });
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://www.120.net/post/");
    }

    @Override
    public void prepareToRun() {
        Schedule.scheduleAtFixedRateMi(()-> {
            var seeds = new ArrayList<String>();
            seeds.add("https://www.120.net/post/" + START_INT.incrementAndGet() + ".html");
            controllerFacade.addUrlsToQueue(seeds);
            }, 200);
    }

    public static void main(String[] args) throws IOException {
        new Health120DialogCrawler(null).testGetHtmlInfo(
                "https://www.120.net/post/11427987.html");
    }
}
