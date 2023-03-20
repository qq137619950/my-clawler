package idea.bios.browser.phantomjs;

import idea.bios.util.selenium.SeleniumBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试PhantomJs
 * @author 86153
 */
@Slf4j
public class TestPhantomJs {
    public static void main(String[] args) {
        PhantomJSDriver driver = SeleniumBuilder.getPhantomJsSeleniumBo().getPhantomJSDriver();
        final String url =
                "https://www.haodf.com/bingcheng/8890840370.html";
        // 等待xx元素出现
        var wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(url);
        WebElement recommend = wait.until((ExpectedCondition<WebElement>)
                d -> {
                    if (d != null) {
                        return d.findElement(By.cssSelector("body > main > section.left-heart"));
                    }
                    return null;
                });
        if (recommend == null) {
            log.warn("找不到内容, url:{}", url);
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

        log.info(String.valueOf(links.stream().filter(u -> u.contains("bingcheng"))
                        .distinct()
                        .collect(Collectors.toList())));

        // 病例信息和问诊建议
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
        log.info(diseaseInfo.stream().map(WebElement::getText)
                        .map(t -> t.endsWith("）") || t.endsWith(")") ? t.substring(0, t.length() - 14) : t)
                    .collect(Collectors.joining("\n")));
        log.info(suggestions.stream().map(WebElement::getText)
                .collect(Collectors.joining("\n")));

        // 多轮对话，先模拟点击"查看更多"
//        WebElement moreDialog = driver.findElement(By.cssSelector(
//                "div.msg-more-link > span.msg-more-link-text"));
//        log.info(moreDialog.getText());
//        moreDialog.click();
//        List<WebElement> dialogList = driver.findElements(By.cssSelector("#msgboard > div.chunk"));
    }
}
