package idea.bios.jobs.example.net.miaoshou;

import com.google.gson.Gson;
import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.controller.ControllerFacade;
import idea.bios.url.WebURL;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 妙手医生对话
 * https://www.miaoshou.net/consult/3gy9Qq2E8nKdWjeX.html
 * @author 86153
 */
@Slf4j
public class MiaoshouDialogCrawler extends AbsCommonCrawler {
    public MiaoshouDialogCrawler(ControllerFacade controllerFacade) {
        super(controllerFacade);
    }

    @Override
    public void visit(Page page) {
        super.commonHtmlPageVisit(page, html -> {
            var result = new HashMap<String, Object>();
            Document doc = Jsoup.parseBodyFragment(html);
            // title
            Element titleElement = doc.selectFirst(
                    "#main > div.card.question-card > div.question-card__head > h1.question-card__title > span");
            result.put("title", titleElement.text());
            // desc
            Element descElement = doc.selectFirst(
                    "#main > div.card.question-card > div.question-card__body > div.question-content");
            result.put("desc", descElement.text());
            // 医生信息
            Element author = doc.selectFirst(
                    "#main > div.card.chat-card > div > div > a.author-info-link");
            if (author == null) {
                log.warn("no author info.");
                return null;
            }
            var authorInfoMap = new HashMap<String, Object>();
            authorInfoMap.put("name", author.selectFirst("div> p.main-row > span.name").text());
            authorInfoMap.put("jobTitle", author.selectFirst("div> p.main-row > span.title").text());
            authorInfoMap.put("department", author.selectFirst("div > p.vice-row > span.department").text());
            authorInfoMap.put("hospital", author.selectFirst("div > p.vice-row > span.hospital").text());
            result.put("authorInfo", new Gson().toJson(authorInfoMap));
            // 对话
            Elements dialogElements = doc.select(
                    "#main > div.card.chat-card > div.chat-card__body > div.message-item > div.message-content");
            if (dialogElements == null) {
                log.warn("no dialog info.");
                return null;
            }
            var dialogList = new ArrayList<String>();
            dialogElements.forEach(c -> {
                if (c.parent().is(".message-left")) {
                    dialogList.add("A:" + c.text());
                } else if (c.parent().is(".message-right")) {
                    dialogList.add("Q:" + c.text());
                }
            });
            result.put("dialog", dialogList);
            return result;
        });
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
//                Pattern.matches("https://www.miaoshou.net/[a-z]/[0-9A-Za-z]+.html",
//                        url.getURL()) &&
                url.getURL().startsWith("https://www.miaoshou.net");
    }

    @Override
    protected boolean shouldParse(WebURL url) {
        return url.getURL().startsWith("https://www.miaoshou.net/consult") &&
                !url.getURL().contains("list");
    }

    @Override
    public void prepareToRun() {
        controllerFacade.addUrlsToQueue(seedFetcher.getSeedsPlain(
                "https://www.miaoshou.net/consult/list.html"));
    }
}
