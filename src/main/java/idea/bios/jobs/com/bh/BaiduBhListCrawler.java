package idea.bios.jobs.com.bh;

import com.google.gson.Gson;
import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.sites.ListCrawlerEnum;
import idea.bios.crawler.my.starter.ListStarter;
import idea.bios.url.WebURL;
import idea.bios.util.JsoupUtils;
import idea.bios.util.selenium.SeleniumUtils;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;

import static idea.bios.crawler.my.Tools.configBuilder;

/**
 * https://m.baidu.com/bh/m/detail/ar_1151125392613938133
 * @author 86153
 */
public class BaiduBhListCrawler extends AbsCommonCrawler {
    /**
     * 启动器
     */
    private static ListStarter listStarter;
    @Override
    protected Map<String, ?> getSingleHtmlInfo(String html) {
        var result = new HashMap<String, Object>();
        Document doc = Jsoup.parseBodyFragment(html);
        // title不同的情况
        Element title = doc.select(
                "div.health-article-container > div.health-article-title > div > div").first();
        if (title != null) {
            // 文章
            result.put("title", title.text());
            Element content = doc.select(
                            "div.health-article-container > div.health-article-content-small")
                    .first();
            if (content == null) {
                return null;
            }
            String contentText = Objects.requireNonNull(JsoupUtils.getBeautifulText(content));
            result.put("content", contentText);
            result.put("imgs", JsoupUtils.getElementAllImgSrc(content));
            result.put("type", "article");
        } else {
            // 问答
            title = doc.select(
                    "div.health-detail-body-ask > div.health-detail-question-title").first();
            if (title == null) {
                return null;
            }
            result.put("title", title.text());
            Element content = doc.select(
                            "div.health-answer-content > div.health-answer-content-analy > div.J-summary")
                    .first();
            if (content == null) {
                return null;
            }
            result.put("content", content.text());
            result.put("imgs", JsoupUtils.getElementAllImgSrc(content));
            result.put("type", "qa");
        }
        // 作者
        Element authorInfo = doc.selectFirst(
                "div.health-article-title> div> div > div.health-doctor-info");

        if (authorInfo == null) {
            authorInfo = doc.selectFirst(" div > div.health-doctor-info");
        }
        if (authorInfo != null) {
            var authorInfoMap = new LinkedHashMap<String, Object>();
            Element img = authorInfo.selectFirst("div > img");
            if (img != null) {
                authorInfoMap.put("img", img.attr("src"));
            }
            Element name = authorInfo.selectFirst(
                    "div.health-doctor-info-doc > p.health-doctor-name > .health-doctor-name-span");
            if (name != null) {
                authorInfoMap.put("name", name.text());
            }
            Element jobTitle = authorInfo.selectFirst(
                    "div.health-doctor-info-doc > p.health-doctor-name > .health-doctor-info-job-title");
            if (jobTitle != null) {
                authorInfoMap.put("jobTitle", jobTitle.text());
            }
            Element department = authorInfo.selectFirst(
                    "div.health-doctor-info-doc > p.health-doctor-name > .health-doctor-info-department");
            if (department != null) {
                authorInfoMap.put("department", department.text());
            }
            Element hospital = authorInfo.selectFirst(
                    "div > span > .health-doctor-info-text");
            if (hospital != null) {
                authorInfoMap.put("hospital", hospital.text());
            }
            Element hospitalLabel = authorInfo.selectFirst(
                    "div.health-doctor-location-info > p > span.health-doctor-info-tags-tag > span");
            if (hospitalLabel != null) {
                authorInfoMap.put("hospitalLabel", hospitalLabel.text());
            }
            result.put("authorInfo", new Gson().toJson(authorInfoMap));
        }
        // 时间
        Element createTime = doc.selectFirst("div.health-answer-wrapped-content > div > div.health-question-tip-info");
        if (createTime != null) {
            result.put("createTime", createTime.text());
        }
        return result;
    }

    @Override
    public void visit(Page page) {
        // 解析网页得到link url
        String url = page.getUrl().getURL();
        super.commonPageVisit(page, "com.baidu.bh.article.qa");
        listStarter.setQueue(SeleniumUtils.getLinks(url));
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://m.baidu.com/bh/m/detail/");
    }

    @Override
    public boolean shouldFollowLinksIn(WebURL url) {
        // 有特定的链接生成方式
        return false;
    }

    @Override
    public void runner() throws Exception {
        var startList = new ArrayList<String>(){{
            add("https://m.baidu.com/bh/m/detail/ar_1151125392613938133");
            add("https://m.baidu.com/bh/m/detail/ar_6442070064922690834");
            add("https://m.baidu.com/bh/m/detail/ar_9439497617102990576");
            add("https://m.baidu.com/bh/m/detail/ar_3640179929473294485");
            add("https://m.baidu.com/bh/m/detail/ar_8083185686713864556");
            add("https://m.baidu.com/bh/m/detail/ar_9615903856324255196");
            add("https://m.baidu.com/bh/m/detail/ar_7788970030282471763");
            add("https://m.baidu.com/bh/m/detail/ar_7026564090491937961");

            add("https://m.baidu.com/bh/m/detail/ar_7922028990731072213");
            add("https://m.baidu.com/bh/m/detail/ar_10858666168882250311");
            add("https://m.baidu.com/bh/m/detail/ar_5539072192622654549");
            add("https://m.baidu.com/bh/m/detail/ar_1954794905110302825");
            add("https://m.baidu.com/bh/m/detail/ar_8302957808524277268");
            add("https://m.baidu.com/bh/m/detail/ar_10070257364496974024");
            add("https://m.baidu.com/bh/m/detail/vc_3000816748248986681");
            add("https://m.baidu.com/bh/m/detail/qr_12274882085697843187");
            add("https://m.baidu.com/bh/m/detail/ar_16626261557995470980");
            add("https://m.baidu.com/bh/m/detail/ar_7217544763780178215");
            add("https://m.baidu.com/bh/m/detail/qr_8082268841242361251");
            add("https://m.baidu.com/bh/m/detail/ar_6850790536306110484");
            add("https://m.baidu.com/bh/m/detail/ar_10487979156498197426");
            add("https://m.baidu.com/bh/m/detail/ar_6788309259092883319");
            add("https://m.baidu.com/bh/m/detail/ar_9344947297443864269");
            add("https://m.baidu.com/bh/m/detail/ar_8887407635695244126");
            add("https://m.baidu.com/bh/m/detail/ar_11563770150590832949");
            add("https://m.baidu.com/bh/m/detail/qr_12255470671776017147");
            add("https://m.baidu.com/bh/m/detail/ar_6829471551307117734");
            add("https://m.baidu.com/bh/m/detail/ar_7291371047636713556");
            add("https://m.baidu.com/bh/m/detail/ar_7996760970748216313");
            add("https://m.baidu.com/bh/m/detail/ar_7058428578494431975");
            add("https://m.baidu.com/bh/m/detail/qr_11723972364665847864");
            add("https://m.baidu.com/bh/m/detail/qr_12697154049055531484");

        }};
        listStarter = new ListStarter(configBuilder(-1, 300));
        listStarter.run(ListCrawlerEnum.baidu_bh_list, (offset, limit) ->
                startList, startList.size(), 1, startList.size());
    }

    public static void main(String[] args) throws IOException {
        new BaiduBhListCrawler().testGetHtmlInfo(
                "https://m.baidu.com/bh/m/detail/qr_18071052435650200215");
    }
}
