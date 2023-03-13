package idea.bios.util.selenium.script;


import com.mongodb.client.MongoCollection;
import idea.bios.datasource.mongodb.MongoDb;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.bson.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static idea.bios.util.selenium.script.ChromeDriverBuilder.buildScriptChromeDriver;

/**
 * https://www.khanacademy.org
 * @author 86153
 */
@Slf4j
public class KhanAcademyCrawlerScript {
    private static final String MENU_SITE = "https://www.khanacademy.org/humanities";
    /**
     * 最终数据存放在mongodb中
     */
    private static final MongoCollection<Document> COLLECTION = new MongoDb().getCrawlerDataCollection(
            "org.khanacademy.article");

    public static void main(String[] args) {
        try {
            run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void run() throws InterruptedException {
        ChromeDriver driver = buildScriptChromeDriver();
        // 菜单页面，获取课程
        var wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        driver.get(MENU_SITE);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(
                "#app-shell-root > div > main")));
        WebElement root = wait.until((ExpectedCondition<WebElement>)
                d -> {
                    if (d != null) {
                        return d.findElement(By.cssSelector("#app-shell-root"));
                    }
                    return null;
                });
        List<String> courseUrl = root.findElements(By.cssSelector(
                        "div > main > div > div > div > div > div > div > div > h2 > a")).stream()
                .map(e -> e.getAttribute("href"))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        log.info("获取课程：{}", courseUrl);
        // 遍历课程，进入详情页
        courseUrl.forEach(url -> {
            driver.get(url);
            // 获取Unit
            WebElement unit = wait.until((ExpectedCondition<WebElement>)
                    d -> {
                        if (d != null) {
                            return d.findElement(By.cssSelector(
                                    "#topic-progress > span > div > div > div[data-slug='topic-progress']"));
                        }
                        return null;
                    });
            if (unit == null) {
                log.warn("找不到课程单元");
                return;
            }
            // 遍历unit
            List<String> unitList = unit.findElements(By.cssSelector(
                    "div > a[data-test-id='unit-header']")).stream()
                    .map(e -> e.getAttribute("href"))
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
            log.info("unit list:{}", unitList);
            if (unitList.isEmpty()) {
                log.warn("找不到任何单元");
            }
            // 遍历每个unit，将最终的URL放进去
            unitList.forEach(u -> {
                var finalList = new ArrayList<String>();
                driver.get(u);
                // 获取这个单元所有的文本链接
                WebElement links = wait.until((ExpectedCondition<WebElement>)
                        d -> {
                            if (d != null) {
                                return d.findElement(By.cssSelector("div[data-slug='topic-progress']"));
                            }
                            return null;
                        });
                if (links == null) {
                    log.warn("找不到课程链接");
                    return;
                }
                List<WebElement> courseLinks = links.findElements(By.cssSelector(
                        "div > div > ul[role='list']"));
                courseLinks.forEach(c -> {
                    List<String> finalUrls = c.findElements(By.cssSelector("li > div > div > a")).stream()
                            .map(e -> e.getAttribute("href"))
                            .filter(s -> s.contains("/a/"))
                            .distinct()
                            .collect(Collectors.toCollection(ArrayList::new));
                    log.info("课程url:{}", finalUrls);
                    finalList.addAll(finalUrls);
                });
                log.info("final list: {}", finalList);
                // 遍历 list，处理数据
                finalList.forEach(f -> {
                    driver.get(f);
                    // 限制频率 1s以上
                    WebElement body = wait.until((ExpectedCondition<WebElement>)
                            d -> {
                                if (d != null) {
                                    return d.findElement(By.cssSelector(
                                            "div[data-test-id='article-renderer-scroll-container']"));
                                }
                                return null;
                            });
                    if (body == null) {
                        log.warn("找不到内容, url:{}", f);
                        return;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    WebElement title = body.findElement(By.cssSelector("header"));
                    List<WebElement> contents = body.findElements(By.cssSelector(
                            "div > div > div.clearfix"));
                    if (title == null || contents == null || contents.isEmpty()) {
                        log.warn("找不到内容, url:{}", f);
                        return;
                    }
                    try {
                        COLLECTION.insertOne(new Document("title", title.getText())
                                .append("content", contents.stream().map(WebElement::getText)
                                        .collect(Collectors.joining("\n")))
                                .append("site", f));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        });
    }
}
