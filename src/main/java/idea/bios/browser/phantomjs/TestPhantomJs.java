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
                "https://www.khanacademy.org/math/get-ready-for-algebra-ii/x6e4201668896ef07:get-ready-for-equations/x6e4201668896ef07:solving-systems-of-equations-with-substitution/a/substitution-method-review-systems-of-equations";
        // 等待xx元素出现
        var wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(url);
        WebElement body = wait.until((ExpectedCondition<WebElement>)
                d -> {
            if (d != null) {
                return d.findElement(By.cssSelector(
                        "div[data-test-id='article-renderer-scroll-container']"));
            }
            return null;
        });
        if (body == null) {
            log.warn("找不到内容, url:{}", url);
            return;
        }
        WebElement title = body.findElement(By.cssSelector("header"));
        List<WebElement> contents = body.findElements(By.cssSelector(
                "div > div > div.clearfix"));
        if (title == null || contents == null || contents.isEmpty()) {
            log.warn("找不到内容, url:{}", url);
            return;
        }
        log.info("title:{}", title.getText());
        log.info("content:{}",  contents.stream().map(WebElement::getText)
                .collect(Collectors.joining("\n")));
    }
}
