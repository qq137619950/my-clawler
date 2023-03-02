package idea.bios.jobs.com.bh;

import com.google.gson.Gson;
import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.sites.ListCrawlerEnum;
import idea.bios.crawler.my.starter.CommonCrawlerStarter;
import idea.bios.url.WebURL;
import idea.bios.util.JsoupUtils;
import idea.bios.util.Schedule;
import idea.bios.util.search.BaiduSfSearchLinks;
import idea.bios.util.selenium.SeleniumUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static idea.bios.crawler.my.Tools.configBuilder;

/**
 * https://m.baidu.com/bh/m/detail/ar_1151125392613938133
 * @author 86153
 */
@Slf4j
public class BaiduBhListCrawler extends AbsCommonCrawler {

    private static final AtomicInteger START_INT = new AtomicInteger(0);

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
        listStarter.addUrlsToQueue(SeleniumUtils.getLinks(url, this.getChromeDriver()));
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
        listStarter = new CommonCrawlerStarter(configBuilder(
                -1, 300, true));
        var searchLinks = new BaiduSfSearchLinks();
        // 创建一个定时任务，10s从数据库拿1条数据
        Schedule.scheduleAtFixedRate(()-> {
            List<String> sUrls = listStarter.getSeedFetcher().getSeedsFromDb(
                    START_INT.getAndIncrement(),
                    1,
                    term -> BaiduSfSearchLinks.URL_PREFIX + term);
            // 解析其中的url
            log.info("count:{}, urls:{}", START_INT.get(), sUrls);
            if (sUrls == null || sUrls.isEmpty()) {
                return;
            }
            List<String> links = searchLinks.getLinks(sUrls.get(0));
            listStarter.addUrlsToQueue(links);
        }, 10);
        var seeds = new ArrayList<String>();
        seeds.add("https://m.baidu.com/bh/m/detail/qr_12116861696193512074");
        seeds.add("https://m.baidu.com/bh/m/detail/ar_12703356293423056141");
        seeds.add("https://m.baidu.com/bh/m/detail/ar_8883886229467987604");
        listStarter.run(ListCrawlerEnum.baidu_bh_list, seeds);
    }

    public static void main(String[] args) throws IOException {
//        new BaiduBhListCrawler().testGetHtmlInfo(
//                "https://m.baidu.com/bh/m/detail/ar_17806348835341762212");
        UUID uuid = UUID.nameUUIDFromBytes(
                "https://baijiahao.baidu.com/s?id=1696989908880011257".getBytes(StandardCharsets.UTF_8));
        System.out.println(uuid);
    }
}
