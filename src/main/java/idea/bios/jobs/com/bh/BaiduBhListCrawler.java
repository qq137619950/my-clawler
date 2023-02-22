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
    // 启动器
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
        listStarter.setQueue(SeleniumUtils.getBaiduBhLinks(url));
        super.commonPageVisit(page, "com.baidu.bh.article.qa");
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://m.baidu.com/bh/m/detail/");
    }

    @Override
    public void runner() throws Exception {
        var startList = new ArrayList<String>(){{
            add("https://m.baidu.com/bh/m/detail/ar_9757274631162417881");
            add("https://m.baidu.com/bh/m/detail/ar_17307994535258803987");
            add("https://m.baidu.com/bh/m/detail/ar_12665210231907902229");
            add("https://m.baidu.com/bh/m/detail/ar_4085299825249287360");
            add("https://m.baidu.com/bh/m/detail/vc_16593335641951410586");
            add("https://m.baidu.com/bh/m/detail/vc_12527290574691625828");
            add("https://m.baidu.com/bh/m/detail/ar_14672916287349354402");
            add("https://m.baidu.com/bh/m/detail/ar_2500694198211796944");
        }};
        listStarter = new ListStarter(configBuilder(-1, 1000));
        listStarter.run(ListCrawlerEnum.baidu_bh_list, (offset, limit) ->
                startList, startList.size(), 1, startList.size());
    }

    public static void main(String[] args) throws IOException {
        new BaiduBhListCrawler().testGetHtmlInfo(
                "https://m.baidu.com/bh/m/detail/qr_18071052435650200215");
    }
}
