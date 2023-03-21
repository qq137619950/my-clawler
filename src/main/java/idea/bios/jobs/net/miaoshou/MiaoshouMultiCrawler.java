package idea.bios.jobs.net.miaoshou;

import com.google.gson.Gson;
import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.controller.ControllerFacade;
import idea.bios.url.WebURL;
import idea.bios.util.Schedule;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 妙手医生综合
 * https://www.miaoshou.net/article/
 * @author 86153
 */
public class MiaoshouMultiCrawler extends AbsCommonCrawler {
    private static final Pattern MP4_PATTERN = Pattern.compile("https://([\\s\\S]*).mp4\",");
    public MiaoshouMultiCrawler(ControllerFacade controllerFacade) {
        super(controllerFacade);
    }

    @Override
    public void visit(Page page) {
        // Pattern
        final String pattern1 = "https://www.miaoshou.net/article/[0-9A-Za-z]+.html";
        final String pattern2 = "https://www.miaoshou.net/voice/[0-9A-Za-z]+.html";
        final String pattern3 = "https://www.miaoshou.net/video/[0-9A-Za-z]+.html";
        final String pattern4 = "https://www.miaoshou.net/question/[0-9A-Za-z]+.html";

        // 构建map
        var chMap = new HashMap<String, Function<String, Map<String, ?>>>();
        // https://www.miaoshou.net/article/268375.html
        chMap.put("net.miaoshou.article", (html) -> {
            var result = new HashMap<String, Object>();
            Document doc = Jsoup.parseBodyFragment(html);
            result.put("title", Objects.requireNonNull(
                    doc.select("#main > div.card.content-card > div > h2 > span").first()).text());
            result.put("time", Objects.requireNonNull(
                    doc.select("#main > div.card.content-card > div.content-card__head > div > p").first()).text().substring(0, 10));

            // 内容
            Elements contentList = doc.select("#main > div.card.content-card > div.content-card__body > div > div > div.article-section");
            if (contentList == null || contentList.isEmpty()) {
                return null;
            }
            result.put("content", contentList.stream().map(Element::text)
                    .collect(Collectors.joining("\n")));
            // 作者
            Element author = doc.select("#main > div.card.content-card > div > div > a").first();
            if (author != null) {
                var authorInfoMap = new HashMap<String, Object>();
                authorInfoMap.put("name", author.selectFirst(
                        "div.fl > p.main-row > span.name").text());
                authorInfoMap.put("jobTitle", author.selectFirst(
                        "div.fl > p.main-row > span.title").text());
                authorInfoMap.put("department", author.selectFirst(
                        "div.fl > p.vice-row > span.department").text());
                authorInfoMap.put("hospital", author.selectFirst(
                        "div.fl > p.vice-row > span.hospital").text());
                authorInfoMap.put("img", author.selectFirst(
                        "div.fl.avatar-link > img").attr("src"));
                result.put("authorInfo", new Gson().toJson(authorInfoMap));
            }
            return result;
        });
//        // https://www.miaoshou.net/voice/77002.html
//        chMap.put("net.miaoshou.voice", (html) -> {
//            var result = new HashMap<String, Object>();
//            Document doc = Jsoup.parseBodyFragment(html);
//            result.put("title", Objects.requireNonNull(
//                    doc.select("#main > div.card.content-card > div.content-card__head > h2 > span.fl").first()).text());
//            result.put("time", Objects.requireNonNull(
//                    doc.select("#main > div.card.content-card > div.content-card__head > div > p.views-count").first()).text().substring(0, 10));
//            Element content = doc.select("#main > div.card.content-card > div.content-card__body > div > div > div.article-content").first();
//            result.put("content", JsoupUtils.getBeautifulText(content));
//            String voice = doc.select("div.container > div.fl > div.fr > div.audio-wrapper > audio > source")
//                    .first().absUrl("src");
//            result.put("voice", voice);
//            Element docInfo = doc.select("#wz_box_left > div.doc_info").first();
//            result.put("docInfo", docInfo.text());
//            return result;
//        });
//        // https://www.miaoshou.net/video/my3Xvn4OP4Jg129w.html
//        chMap.put("net.miaoshou.video", (html) -> {
//            var result = new HashMap<String, Object>();
//            Document doc = Jsoup.parseBodyFragment(html);
//            result.put("title", Objects.requireNonNull(
//                    doc.select("div.container > div.fl > div.fr > h2 > span").first()).text());
//            result.put("time", Objects.requireNonNull(
//                    doc.select("div.container > div.fl > div.fr > div.tit_info").first()).text().substring(0, 10));
//            Element content = doc.select("body > div.container > div.fl > div.fr > div > div.doc_note").first();
//            result.put("content", JsoupUtils.getBeautifulText(content));
//            Element docInfo = doc.select("#wz_box_left > div.doc_info").first();
//            result.put("docInfo", docInfo.text());
//            Elements elements = doc.select("body > div.container > div.fl > div.fr > div.pr > script");
//            // 从script中获取video url
//            for (Element e : elements) {
//                String scriptText = e.html();
//                if (scriptText.contains("mp4")) {
//                    Matcher matcher = MP4_PATTERN.matcher(scriptText);
//                    if (matcher.find()) {
//                        String t =  matcher.group(0);
//                        result.put("video", t.substring(0, t.length() - 2));
//                        break;
//                    }
//                }
//            }
//            // 封面
//            String poster = doc.select(
//                            "div.container > div.fl > div.fr > div.pr > div > div.paperPic > img")
//                    .first().absUrl("src");
//            result.put("poster", poster);
//            return result;
//        });

        // https://www.miaoshou.net/question/P9p8VgM5y8pgxNko.html
        chMap.put("net.miaoshou.question", (html) -> {
            var result = new HashMap<String, Object>();
            Document doc = Jsoup.parseBodyFragment(html);
            result.put("question", Objects.requireNonNull(
                    doc.select("#main > div.card.question-card > div.question-card__head > h1 > span").first().text()));
            result.put("time", Objects.requireNonNull(
                    doc.select("#main > div.card.question-card > div.question-card__head > div > p.question-card__question-date > span").first()).text());
            // content
            Elements contentList = doc.select("#main > div.card.answer-card > div.answer-item > div.answer-item__body > div.answer-item__section");
            if (contentList == null || contentList.isEmpty()) {
                return null;
            }
            result.put("content", contentList.stream().map(Element::text)
                    .collect(Collectors.joining("\n")));
            // 作者
            Element author = doc.select("#main > div.card.answer-card > div.answer-item > div.answer-item__meta.clearfix > a").first();
            if (author != null) {
                var authorInfoMap = new HashMap<String, Object>();
                authorInfoMap.put("name", author.selectFirst(
                        "div.fl > p.main-row > span.name").text());
                authorInfoMap.put("jobTitle", author.selectFirst(
                        "div.fl > p.main-row > span.title").text());
                authorInfoMap.put("department", author.selectFirst(
                        "div.fl > p.vice-row > span.department").text());
                authorInfoMap.put("hospital", author.selectFirst(
                        "div.fl > p.vice-row > span.hospital").text());
                authorInfoMap.put("img", author.selectFirst(
                        "div.fl.avatar-link > img").attr("src"));
                result.put("authorInfo", new Gson().toJson(authorInfoMap));
            }
            return result;
        });

        super.multiPageVisit(page, (curUrl) -> {
            if (Pattern.matches(pattern1, curUrl)) {
                return "net.miaoshou.article";
//            } else if (Pattern.matches(pattern2, curUrl)) {
//                return "net.miaoshou.voice";
//            } else if (Pattern.matches(pattern3, curUrl)) {
//                return "net.miaoshou.video";
            } else if (Pattern.matches(pattern4, curUrl)) {
                return "net.miaoshou.question";
            }
            return null;
        }, chMap);
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://www.miaoshou.net");
    }

    @Override
    public void prepareToRun() {

        Schedule.crawlerScheduleAtFixedRate(()-> {
            controllerFacade.addUrlsToQueue(seedFetcher.getSeedsPlain("https://www.miaoshou.net/voice/77002.html",
                    "https://www.miaoshou.net/article/266640.html",
                    "https://www.miaoshou.net/question/RlG2KOwAQGn3vLnV.html"));
            var seeds = IntStream.range(0, 100).mapToObj(
                            i -> "https://www.miaoshou.net/article/" + (INT_FLAG.get() + i) + ".html")
                    .collect(Collectors.toCollection(ArrayList::new));
            var seeds2 = IntStream.range(0, 100).mapToObj(
                            i -> "https://www.miaoshou.net/voice/" + (INT_FLAG.get() + i) + ".html")
                    .collect(Collectors.toCollection(ArrayList::new));
            controllerFacade.addUrlsToQueue(seeds);
            INT_FLAG.addAndGet(100);
        }, 2);
    }
}
