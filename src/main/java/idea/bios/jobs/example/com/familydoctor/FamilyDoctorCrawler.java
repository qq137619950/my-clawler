package idea.bios.jobs.example.com.familydoctor;

import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.controller.ControllerFacade;
import idea.bios.url.WebURL;
import idea.bios.util.JsoupUtils;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 家庭医生在线
 * https://www.familydoctor.com.cn/mouth/a/201906/2556918.html
 * @author 86153
 */
public class FamilyDoctorCrawler extends AbsCommonCrawler {
    public FamilyDoctorCrawler(ControllerFacade controllerFacade) {
        super(controllerFacade);
    }

    @Override
    public void visit(Page page) {
        super.commonHtmlPageVisit(page, html -> {
            var result = new HashMap<String, Object>();
            Document doc = Jsoup.parseBodyFragment(html);
            // 标题
            String title = Objects.requireNonNull(
                    doc.select("div.main > div > div.article-titile > h1")
                            .first()).text();
            result.put("title", title);
            // time
            String time = Objects.requireNonNull(
                    doc.select("div.main > div > div.article-titile > div > div.left")
                            .first()).text().substring(0, 19);
            result.put("time", time);
            // 内容
            Element content = doc.select("#viewContent").first();
            if (content == null) {
                return null;
            }
            result.put("content", JsoupUtils.getBeautifulText(content));
            if (!JsoupUtils.getElementAllImgSrc(content).isEmpty()) {
                result.put("imgs", JsoupUtils.getElementAllImgSrc(content));
            }
            // result.put("raw_content", content.html());
            return result;
        });
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://www.familydoctor.com.cn");
    }

    @Override
    protected boolean shouldParse(WebURL url) {
        final String pattern =
                "https://www.familydoctor.com.cn/[a-z]+/a/[a-z0-9|/]+.html";
        return Pattern.matches(pattern, url.getURL());

    }

    @Override
    public void prepareToRun() {
        controllerFacade.addUrlsToQueue(
            seedFetcher.getSeedsPlain("https://www.familydoctor.com.cn/yx/a/201209/4805279101052.html",
                "https://www.familydoctor.com.cn/changdao/a/202206/2812550.html"));
    }
}
