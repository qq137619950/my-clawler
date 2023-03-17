package idea.bios.jobs.com.haodf;

import idea.bios.crawler.Page;
import idea.bios.crawler.my.AbsCommonCrawler;
import idea.bios.crawler.my.controller.ControllerFacade;
import idea.bios.url.WebURL;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好大夫病程
 * https://www.haodf.com/bingcheng/8889010821.html
 * @author 86153
 */
@Slf4j
public class HaodfBingchengDiseaseProCrawler extends AbsCommonCrawler {
    public HaodfBingchengDiseaseProCrawler(ControllerFacade controllerFacade) {
        super(controllerFacade);
    }

    @Override
    protected Map<String, ?> getSingleHtmlInfo(String html) {
        // 不需要这个函数
        return null;
    }

    @Override
    public void visit(Page page) {
        super.driverPageVisit(page.getUrl(), () -> {
            PhantomJSDriver driver = getPhantomJsDriver();
            // 等待xx元素出现
            var wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            driver.get(page.getUrl().getURL());
            WebElement recommend = wait.until((ExpectedCondition<WebElement>)
                    d -> {
                        if (d != null) {
                            return d.findElement(By.cssSelector("body > main > section.left-heart"));
                        }
                        return null;
                    });
            if (recommend == null) {
                log.warn("找不到内容, url:{}", page.getUrl().getURL());
                return null;
            }
            var links = new ArrayList<String>();
            // 个人推荐
            List<WebElement> myRecommendList = recommend.findElements(By.cssSelector(
                    "#my-recommend > div.involved-recommend-list > a"));
            myRecommendList.forEach(webElement -> links.add(webElement.getAttribute("href")));
            // 更多推荐
            List<WebElement> otherRecommendList = recommend.findElements(By.cssSelector(
                    "#involved-recommend > p.involved-recommend-list > a"));
            otherRecommendList.forEach(webElement -> links.add(webElement.getAttribute("href")));
            // 加入队列
            controllerFacade.addUrlsToQueue(links.stream().filter(u -> u.contains("bingcheng"))
                    .distinct()
                    .collect(Collectors.toList()));

            // 病例信息和问诊建议
            var result = new HashMap<String, Object>();
            List<WebElement> diseaseInfo = driver.findElements(By.cssSelector("p.diseaseinfo > span"));
            if (diseaseInfo == null || diseaseInfo.isEmpty()) {
                log.warn("diseaseInfo is null");
                return null;
            }
            List<WebElement> suggestions = driver.findElements(By.cssSelector(
                    "section.suggestions > div.suggestions-text"));
            if (suggestions == null) {
                log.warn("suggestions is null");
                return null;
            }
            // 病例信息
            result.put("diseaseInfo", diseaseInfo.stream().map(WebElement::getText)
                    .map(t -> t.endsWith("）") || t.endsWith(")") ? t.substring(0, t.length() - 14) : t)
                    .collect(Collectors.joining("\n")));
            // 问诊建议
            result.put("suggestions", suggestions.stream().map(WebElement::getText)
                    .collect(Collectors.joining("\n")));
            // 医生信息
            WebElement authorInfo = driver.findElement(By.cssSelector(
                    "#doctor-card > div.doctor-card-wrap > div.doctor-card-info"));
            String authorInfoUrl = authorInfo.findElement(By.cssSelector("a")).getAttribute("href");
            result.put("authorInfo", authorInfo.getText());
            result.put("authorInfoUrl", authorInfoUrl);
            List<WebElement> authorExt = driver.findElements(By.cssSelector(
                    "#doctor-card > div.doctor-card-wrap > div.doctor-card-service > div.service-item"));
            if (!authorExt.isEmpty()) {
                result.put("authorExt", authorExt.stream().map(WebElement::getText)
                                .map(t -> t.replaceAll("\n", ":"))
                        .collect(Collectors.toList()));
            }
            return result;
        });
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return !COMMON_FILTERS.matcher(url.getURL()).matches() &&
                url.getURL().startsWith("https://www.haodf.com/bingcheng/");
    }

    @Override
    public void prepareToRun() {
        // 放几个即可
        controllerFacade.addUrlsToQueue(seedFetcher.getSeedsPlain(
                "https://www.haodf.com/bingcheng/8890840370.html",
                "https://www.haodf.com/bingcheng/8890819561.html"));
    }

}
