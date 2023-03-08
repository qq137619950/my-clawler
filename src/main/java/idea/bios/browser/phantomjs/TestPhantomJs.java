package idea.bios.browser.phantomjs;

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
import java.util.Optional;

/**
 * 测试PhantomJs
 * @author 86153
 */
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
                "https://www.medsci.cn/guideline/search?s_id=37";
        var links = new ArrayList<String>();
        // 等待xx元素出现
        var wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(url);
//        // 自定义等待事件
//        wait.until((ExpectedCondition<Boolean>) d -> d != null &&
//                d.findElement(By.cssSelector(
//                        "div.health-carea")) != null);
//
//        WebElement element = driver.findElement(By.cssSelector(
//                "div.health-carea"));
//
//        System.out.println("Element:" + driver.findElement(By.name("body")).getText());
//        if (element != null) {
//            List<WebElement> nextDivList = element.findElements(By.cssSelector("div.swan-ad-fc-feed"));
//            Optional.ofNullable(nextDivList)
//                    .orElse(new ArrayList<>()).forEach(e -> {
//                        try {
//                            WebElement inner = e.findElement(By.cssSelector("div.swan-ad-fc-recommend"));
//                            if (inner != null) {
//                                String dataSrc = inner.getAttribute("data-source");
//                                String[] dsa = dataSrc.split("\\?");
//                                links.add(dsa[0]);
//                            }
//                        } catch (Exception ignored) {
//                        }
//                    });
//        }
        System.out.println(links);
    }
}
