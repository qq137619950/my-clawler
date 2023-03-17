package idea.bios.browser.phantomjs;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.openqa.selenium.By;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
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
        //设置必要参数
        var dcaps = new DesiredCapabilities();
        //ssl证书支持
        dcaps.setCapability("acceptSslCerts", true);
        //css搜索支持
        dcaps.setCapability("cssSelectorsEnabled", true);
        //js支持
        // dcaps.setJavascriptEnabled(true);
        dcaps.setAcceptInsecureCerts(true);
        dcaps.setPlatform(Platform.WIN11);
        //驱动支持（第二参数表明的是你的phantomjs引擎所在的路径，使用whereis phantomjs可以查看）
        dcaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                "./driver/phantomjs/phantomjs-win.exe");

        //创建无界面浏览器对象
        var driver = new PhantomJSDriver(dcaps);
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
