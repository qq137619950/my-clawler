package idea.bios.util.selenium;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;


/**
 * Selenium的工具
 * TODO 定一个一个接口实现方法在crawler中实现，或者定义为page parser的一部分
 * @author 86153
 */
@Slf4j
public class SeleniumUtils {
    private static final double PASS_RATIO = 0.5;
    private static final String DESKTOP_CHROME_PATH =
            "C:/Program Files/Google/Chrome/Application/chromedriver.exe";
    private static final String CHROME_PATH =
            "C:/Users/19106/AppData/Local/Google/Chrome/Application/chromedriver.exe";
    private static final String CHROME_PATH_HYQ =
            "C:/Users/IDEA/AppData/Local/Google/Chrome/Application/chromedriver.exe";

    private static final Semaphore SEMAPHORE = new Semaphore(1);

    public static List<String> getLinks(String url, ChromeDriver driver) {
        // 随机返回空
//        var random = new Random();
//        if (random.nextInt(10) > PASS_RATIO * 10) {
//            return new ArrayList<>();
//        }
        try {
            if (SEMAPHORE.tryAcquire()) {
                List<String> res = getBaiduBhLinks(url, driver);
                SEMAPHORE.release();
                return res;
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.warn("Exception", e);
            SEMAPHORE.release();
            return new ArrayList<>();
        }
    }

    public static String getContentWait(String url,
                                        String cssCondition,
                                        int waitSecond,
                                        ChromeDriver driver) {
        var content = "";
        if (url == null || url.isEmpty()) {
            log.warn("url empty, then return null.");
            return null;
        }
        // 等待xx元素出现
        if (waitSecond < 0) {
            waitSecond = 0;
        }
        var wait = new WebDriverWait(driver, Duration.ofSeconds(waitSecond));
        driver.get(url);
        // 自定义等待事件
        if (cssCondition == null || cssCondition.isEmpty()) {
            cssCondition = "body";
        }
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssCondition)));
        return driver.getPageSource();
    }

    /**
     * 具体化的一个，作为参考
     * @param url   site
     * @return      link的地址
     */
    public static List<String> getBaiduBhLinks(String url, ChromeDriver driver) {
        var links = new ArrayList<String>();
        if (url == null || url.isEmpty()) {
            return links;
        }
        // 等待xx元素出现
        var wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(url);
        // 自定义等待事件
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("health-carea")));
        WebElement element = wait.until((ExpectedCondition<WebElement>)
                d -> {
                    if (d != null) {
                        return d.findElement(By.cssSelector("div.health-carea > div:nth-child(3)"));
                    }
                    return null;
                });
        if (element != null) {
            List<WebElement> nextDivList = element.findElements(By.cssSelector("div.swan-ad-fc-feed"));
            Optional.ofNullable(nextDivList)
                    .orElse(new ArrayList<>()).forEach(e -> {
                        try {
                            WebElement inner = e.findElement(By.cssSelector("div.swan-ad-fc-recommend"));
                            if (inner != null) {
                                String dataSrc = inner.getAttribute("data-source");
                                String[] dsa = dataSrc.split("\\?");
                                links.add(dsa[0]);
                            }
                        } catch (Exception ex) {
                            return;
                        }
                    });
        }
        // CHROME_DRIVER.quit();
        SEMAPHORE.release();
        return links;
    }
}
