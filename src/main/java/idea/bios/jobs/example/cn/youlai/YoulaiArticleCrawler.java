package idea.bios.jobs.example.cn.youlai;

import com.google.gson.Gson;
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
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 有来医生 - 文章
 * https://www.youlai.cn/yyk/article/720589.html
 * @author 86153
 */
@Slf4j
public class YoulaiArticleCrawler extends AbsCommonCrawler {
    public YoulaiArticleCrawler(ControllerFacade controllerFacade) {
        super(controllerFacade);
    }

    @Override
    public void visit(Page page) {
        super.commonHtmlPageVisit(page, html -> {
            // 只有医生信息和多轮问答
            var result = new HashMap<String, Object>();
            Document doc = Jsoup.parseBodyFragment(html);
            String title = Objects.requireNonNull(
                    doc.select("div.clearfix > div.fl_right > h3.v_title").first()).text();
            if (title.isEmpty()) {
                return null;
            }
            result.put("title", title);
            String time = Objects.requireNonNull(
                    doc.select("div > div > div.clearfix > span.time").first()).text();
            result.put("time", time);
            // 医生信息
            Element author = doc.select("div > dl.doc_pic_box").first();
            if (author != null) {
                var authorInfoMap = new HashMap<String, Object>();
                authorInfoMap.put("name", author.selectFirst(
                        "dd > a > ul > li:nth-child(1)").text());
                authorInfoMap.put("jobTitle", author.selectFirst(
                        "dd > a > ul > li:nth-child(2)").text());
                authorInfoMap.put("department", author.selectFirst(
                        "dd > a > p:nth-child(3)").text());
                authorInfoMap.put("hospital", author.selectFirst(
                        "dd > a > p:nth-child(2)").text());
                authorInfoMap.put("img", author.selectFirst(
                        "dt > a > img").attr("src"));
                result.put("authorInfo", new Gson().toJson(authorInfoMap));
            }
            // 正文
            Element content = doc.select("div > div.text").first();
            if (content == null) {
                return null;
            }
            result.put("content", JsoupUtils.getBeautifulText(content));
            if (!getSpecialImgSrc(content).isEmpty()) {
                result.put("imgs", getSpecialImgSrc(content));
            }
            // result.put("raw_content", content.html());
            return result;
        });
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://www.youlai.cn/yyk/article/");
    }

    @Override
    protected boolean shouldParse(WebURL url) {
        return true;
    }

    @Override
    public void prepareToRun() {
        Schedule.crawlerScheduleAtFixedRate(()-> {
            // 直接拼接
            var seeds = IntStream.range(0, 100).mapToObj(
                    i -> "https://www.youlai.cn/yyk/article/" + (INT_FLAG.get() + i) + ".html")
                    .collect(Collectors.toCollection(ArrayList::new));
            controllerFacade.addUrlsToQueue(seeds);
            INT_FLAG.addAndGet(100);
        }, 2);
    }

    private static List<String> getSpecialImgSrc(Element element) {
        if (element == null) {
            return new ArrayList<>();
        }
        Elements imgUrlElements = element.getElementsByTag("img");
        return imgUrlElements.stream()
                .map(item -> item.attr("src")).distinct().collect(Collectors.toList());
    }
}
