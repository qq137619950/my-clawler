package idea.bios.util.selenium;

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


/**
 * @author 86153
 */
public class SeleniumUtils {
    private static final String DESKTOP_CHROME_PATH =
            "C:/Program Files/Google/Chrome/Application/chromedriver.exe";
    private static final String CHROME_PATH = "C:/Users/19106/AppData/Local/Google/Chrome/Application/chromedriver.exe";

    private static ChromeDriver getChromeDriver() {
        System.getProperties().setProperty("webdriver.chrome.driver", DESKTOP_CHROME_PATH);
        var chromeOptions = new ChromeOptions();
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        // 配置参数优化
        // 没有界面的模型，可以节省很多的内存和cpu占用，而且禁用了cpu渲染，提高加载速度
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-software-rasterizer");
        // 配置不加载图片
        chromeOptions.addArguments("--blink-settings=imagesEnabled=false");
        // 禁用插件加载
        chromeOptions.addArguments("--disable-extensions");
        // 其他
        chromeOptions.addArguments("--window-size=1920,1080");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--allow-running-insecure-content");
        chromeOptions.addArguments("blink-settings=imagesEnabled=false");
        return new ChromeDriver(chromeOptions);
    }

    public static List<String> getBaiduBhLinks(String url) {
        var links = new ArrayList<String>();
        if (url == null || url.isEmpty()) {
            return links;
        }
        ChromeDriver driver = getChromeDriver();
        //等待xx元素出现
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
        driver.quit();
        return links;
    }

    public static void main(String[] args) throws InterruptedException {
        // https://m.baidu.com/bh/m/detail/ar_15210660146895383766
        System.out.println("urls:" + getBaiduBhLinks(
                "https://m.baidu.com/bh/m/detail/ar_15210660146895383766"));
    }
}
