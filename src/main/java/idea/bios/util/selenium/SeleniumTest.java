package idea.bios.util.selenium;

import lombok.var;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;
import java.util.TreeSet;

/**
 * @author 86153
 */
public class SeleniumTest {
    private static final String DESKTOP_CHROME_PATH =
            "C:/Program Files/Google/Chrome/Application/chromedriver.exe";

    public static ChromeDriver getChromeDriver() {
        System.getProperties().setProperty("webdriver.chrome.driver", DESKTOP_CHROME_PATH);
        var chromeOptions = new ChromeOptions();
        return new ChromeDriver(chromeOptions);
    }

    public static void main(String[] args) throws InterruptedException {
        ChromeDriver driver = getChromeDriver();
        driver.get("http://www.news.cn/health/");
        var urlList = new TreeSet<String>();
        for (int i = 0; i < 10000; i++) {
            WebElement look = driver.findElement(By.className("look"));
            if ("暂无更多".equals(look.getText())) {
                break;
            }
            look.click();
            List<WebElement> webElementList = driver.findElements(By.cssSelector("#content-list > div.item"));
            int size = webElementList.size();
            // 每90个输出一次
            if (size % 10 == 0 && size > 100) {
                webElementList = webElementList.subList(size - 100, size);
                webElementList.forEach(item -> {
                    WebElement element = null;
                    try {
                        element = item.findElement(By.cssSelector(".img > a"));
                        if (element != null) {
                            urlList.add(element.getAttribute("href"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                System.out.println(urlList);
            }
            Thread.sleep(1000);
        }
        System.out.println("ALL:" + urlList.size());
        driver.quit();
    }

}
