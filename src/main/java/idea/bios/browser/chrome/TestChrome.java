package idea.bios.browser.chrome;

import idea.bios.util.selenium.SeleniumBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chrome引擎测试
 * @author 86153
 */
@Slf4j
public class TestChrome {
    private static final String URL =
            "https://www.haodf.com/bingcheng/8890858862.html";

    public static void main(String[] args) {
        ChromeDriver driver = SeleniumBuilder.getChromeSeleniumBo().getChromeDriver();
        // 等待xx元素出现
        assert driver != null;
        var wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(URL);
        WebElement recommend = wait.until((ExpectedCondition<WebElement>)
                d -> {
                    if (d != null) {
                        return d.findElement(By.cssSelector("body > main > section.left-heart"));
                    }
                    return null;
                });
        if (recommend == null) {
            log.warn("找不到内容, url:{}", URL);
            return;
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
//        controllerFacade.addUrlsToQueue(links.stream().filter(u -> u.contains("bingcheng"))
//                .distinct()
//                .collect(Collectors.toList()));
        System.out.println(links);

        // 病例信息和问诊建议
        var result = new HashMap<String, Object>();
        List<WebElement> diseaseInfo = driver.findElements(By.cssSelector("p.diseaseinfo > span"));
        if (diseaseInfo == null || diseaseInfo.isEmpty()) {
            log.warn("diseaseInfo is null");
            return;
        }
        List<WebElement> suggestions = driver.findElements(By.cssSelector(
                "section.suggestions > div.suggestions-text"));
        if (suggestions == null) {
            log.warn("suggestions is null");
            return;
        }
        // 病例信息
        result.put("diseaseInfo", diseaseInfo.stream().map(WebElement::getText)
                .map(t -> t.endsWith("）") || t.endsWith(")") ? t.substring(0, t.length() - 14) : t)
                .collect(Collectors.joining("\n")));
        // 问诊建议
        result.put("suggestions", suggestions.stream().map(WebElement::getText)
                .collect(Collectors.joining("\n")));
        // 问诊过程
        while (true) {
            WebElement more = driver.findElement(By.cssSelector("div.msg-more"));
            if (more.getText().contains("没有更多")) {
                break;
            }
            WebElement click = more.findElement(By.cssSelector("div.msg-more-link > span"));
            click.click();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.warn("InterruptedException:", e);
            }
        }
        var conversation = new ArrayList<String>();
        List<WebElement> diseaseProcess = driver.findElements(By.cssSelector(
                "#msgboard > div.chunk > div.msg-item > div.msg-block"));
        for (WebElement item : diseaseProcess) {
            String name;
            String content;
            if (item.getText().contains("本次问诊已到期结束")) {
                break;
            }
            try {
                // 称呼
                name = item.findElement(By.cssSelector("p > span.content-name")).getText();
                if ("小牛医助".equals(name)) {
                    continue;
                }
                // 内容
                content = item.findElement(By.cssSelector("p > span.content-him")).getText();
            } catch (Exception e) {
                log.info("Exception: item:{}", item.getText());
                continue;
            }
            conversation.add(name + ":" + content);
        }
        result.put("conversation", conversation);
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

        System.out.println(result);
    }
}
