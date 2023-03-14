package idea.bios.jobs.com.mfk;

import com.google.gson.Gson;
import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.controller.ControllerFacade;
import idea.bios.crawler.my.sites.CrawlerSiteEnum;
import idea.bios.url.WebURL;
import idea.bios.util.Schedule;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 民福康问答
 * https://www.mfk.com/ask/999767.shtml
 * @author 86153
 */
public class MkfQaCrawler extends AbsCommonCrawler {
    private static final AtomicInteger START_INT = new AtomicInteger(0);

    public MkfQaCrawler(ControllerFacade controllerFacade) {
        super(controllerFacade);
    }

    @Override
    protected Map<String, ?> getSingleHtmlInfo(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        var result = new HashMap<String, Object>();
        // title
        result.put("title", Objects.requireNonNull(doc.selectFirst(
                "div.detailsBox > div.detailsTop > h1")).text());
        // 时间
        result.put("time", Objects.requireNonNull(doc.selectFirst(
                "div.detailsBox > div.detailsTop > p")).text().substring(0, 10));
        // 问题描述
        result.put("question", Objects.requireNonNull(doc.selectFirst(
                "div.askDetails_info > div > p")).text());
        // 医生回答是一个list
        Elements list = doc.select("div > div.selectedAskGuo > ul > li");
        var answerList = new ArrayList<Map<String, String>>();
        list.forEach(item -> {
            var m = new LinkedHashMap<String, String>();
            // 作者
            Element authorInfo = item.selectFirst("a > dl > dd > p.docotrP1");
            if (authorInfo != null) {
                var authorInfoMap = new LinkedHashMap<String, Object>();
                Element img = item.selectFirst("a > dl > dt > img");
                if (img != null) {
                    authorInfoMap.put("img", img.attr("src"));
                }
                Element name = authorInfo.selectFirst("b");
                if (name != null) {
                    authorInfoMap.put("name", name.text());
                }
                Element title = authorInfo.selectFirst("span");
                if (name != null) {
                    authorInfoMap.put("jobTitle", title.text());
                }
                Elements others = authorInfo.select("em");
                if (others != null && others.size() == 2) {
                    authorInfoMap.put("hospital", others.first().text());
                    authorInfoMap.put("department", others.last().text());
                }
                m.put("authorInfo", new Gson().toJson(authorInfoMap));
            }
            // 问答
            Elements answer = item.select("div.contJsUnfold > p");
            if (answer == null || answer.isEmpty()) {
                return;
            }
            m.put("answer", answer.stream().map(Element::text)
                    .map(text -> text.replaceAll(" +",""))
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining("\n")));
            answerList.add(m);
        });
        result.put("answerList", answerList);
        return result;
    }

    @Override
    public void visit(Page page) {
        super.commonHtmlPageVisit(page);
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        final String pattern = "https://www.mfk.com/ask/[0-9]+.shtml";
        return Pattern.matches(pattern, url.getURL());
    }

    @Override
    public void prepareToRun() {
        Schedule.scheduleAtFixedRateMi(()-> {
            // 直接拼接
            var seeds = new ArrayList<String>();
            seeds.add("https://www.mfk.com/ask/" + START_INT.incrementAndGet() + ".shtml");
            controllerFacade.addUrlsToQueue(seeds);
        }, 100);
//        Schedule.scheduleAtFixedRate(()-> {
//            var searchLinks = new MfkQaSearchLinks();
//            List<String> sUrls = seedFetcher.getSeedsFromDb(
//                    START_INT.getAndIncrement(),
//                    1,
//                    term -> "https://www.mfk.com/search/?q=" + term + "page=");
//            if (!sUrls.isEmpty()) {
//                // 每隔1s检索一次，共5次
//                for (int i = 1; i <= 5; i++) {
//                    listStarter.addUrlsToQueue(searchLinks.getLinks(sUrls.get(0) + i));
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException ignored) {
//                    }
//                }
//            }}, 6);
        // 半小时一次，拉取seed
        Schedule.scheduleAtFixedRate(()-> {
            // TODO 分布式锁
            controllerFacade.addUrlsToQueue(seedFetcher.getSeedsFromPool(
                    CrawlerSiteEnum.findCrawlerSiteEnumByClass(
                            this.getClass()).getSourceId()));
        }, 1800);
    }

    public static void main(String[] args) throws IOException {
        new MkfQaCrawler(null).testGetHtmlInfo(
                "https://www.mfk.com/ask/11683454.shtml");
    }
}
