package idea.bios.browser.chrome;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import idea.bios.datasource.mongodb.MongoDb;
import idea.bios.util.selenium.script.ChromeDriverBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.bson.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.FileReader;
import java.util.*;

/**
 * Chrome引擎测试
 * @author 86153
 */
@Slf4j
public class TestChrome {
    private static final MongoCollection<Document> COLLECTION = new MongoDb()
            .getCrawlerDataCollection("com.reddit.comment");

    public static void main(String[] args) throws InterruptedException {
        try (var sc = new Scanner(new FileReader("C:/crawler/reditt.txt"))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (!line.isEmpty()) {
                    run(line);
                }
            }
        } catch (Exception ignored) {
            log.warn("Exception", ignored);
        }
    }

    private static void run(String url) throws InterruptedException {
        log.warn("________________URL:{}", url);
        var result = new LinkedHashMap<String, Object>();
        ChromeDriver driver = null;
        // driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        List<WebElement> dataParentElement = new ArrayList<>();
        while (dataParentElement.isEmpty()) {
            if (driver != null) {
                driver.quit();
                driver.close();
            }
            driver = ChromeDriverBuilder.buildScriptChromeDriver();
            driver.get(url);
            Thread.sleep(10000);
            dataParentElement = driver.findElements(By.xpath(
                    "//div[@data-scroller-first]/parent::div"));
        }
        // 先获取标题等基础信息
        WebElement postContent = driver.findElement(By.cssSelector(
                "div > div > div[data-test-id='post-content']"));
        result.put("title", postContent.findElement(By.cssSelector("div > div > h1")).getText());
        List<WebElement> content = postContent.findElements(By.cssSelector(" div > h3"));
        if (content.isEmpty()) {
            content = postContent.findElements(By.cssSelector(" div[data-click-id='text']"));
        }
        if (!content.isEmpty()) {
            result.put("content", content.get(0).getText().trim());
        }
        // 评论数
        WebElement commentNum = postContent.findElement(By.xpath(
                "//button[@aria-label='upvote']/following-sibling::div[1]"));
        result.put("commentNum", commentNum.getText());
        // comments
        List<WebElement> dataElement = dataParentElement.get(0)
                .findElements(By.xpath("./div"));
        // 点击所有expand
//        List<WebElement> expandList = dataParentElement.findElements(By.cssSelector(
//                "i.icon-expand"));
//        expandList.forEach(item -> {
//            try {
//                item.click();
//            } catch (Exception ignored){}
//        });
        // 遍历获取内容
        var contentList = new ArrayList<DataDao>();
        var level3Map = new HashMap<DataDao, String>();
        dataElement.forEach(element -> {
            String text = element.getText();
            if (text.trim().isEmpty()) {
                return;
            }
            // 第一层级
            if (text.contains("level 1")) {
                DataDao dataDao = buildTextDataDao(element);
                if (dataDao != null) {
                    contentList.add(dataDao);
                }
            } else if (text.contains("Continue this thread")) {
                // 处理链接
                List<WebElement> commentLink = element.findElements(By.tagName("a"));
                if (commentLink.isEmpty()) {
                    return;
                }
                // 先将链接存放起来，最后统一处理
                DataDao pDao = contentList.get(contentList.size() - 1);
                if (pDao.getComments().isEmpty()) {
                    return;
                }
                DataDao curDao = pDao.getComments().get(pDao.getComments().size() - 1);
                level3Map.put(curDao, commentLink.get(0).getAttribute("href"));
            } else if (text.contains("level 2")) {
                DataDao dataDao = buildTextDataDao(element);
                DataDao firstDao = contentList.get(contentList.size() - 1);
                if (dataDao != null) {
                    firstDao.getComments().add(dataDao);
                }
            }
        });
        // 处理level3
        ChromeDriver finalDriver = driver;
        level3Map.forEach((curDao, level3Url) -> {
            try {
                finalDriver.get(level3Url);
                Thread.sleep(5000);
                // 总体是level 3，最多3层级
                List<WebElement> parent = finalDriver.findElements(By.xpath(
                        "//div[@data-scroller-first]/parent::div/div"));
                parent.stream().filter(item -> item.getText().contains("level 2"))
                        .forEach(item -> {
                            DataDao dataDao = buildTextDataDao(item);
                            if (dataDao != null) {
                                curDao.getComments().add(dataDao);
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        result.put("commentList", new Gson().toJson(contentList));
        result.put("site", url);
        System.out.println("---------------------------------------------");
        System.out.println(new Gson().toJson(result));
        // 落库
        var doc = new Document();
        result.forEach(doc::append);
        COLLECTION.insertOne(doc);
    }


    private static DataDao buildTextDataDao(WebElement element) {
        List<WebElement> contentElement = element.findElements(By.cssSelector(
                "div[data-testid='comment']"));
        if (contentElement.isEmpty()) {
            return null;
        }
        WebElement textElement = contentElement.get(0);
        List<WebElement> thumbUpElement = textElement.findElements(By.xpath(
                "./following-sibling::div[1]/div[1]/div[1]"));

        return DataDao.builder()
                .text(textElement.getText()
                        .replaceAll("\u0027", "'"))
                .comments(new ArrayList<>())
                .thumbUp(thumbUpElement.isEmpty() ? "" : thumbUpElement.get(0).getText().trim())
                .build();
    }

    /**
     * data
     */
    @Builder
    @Data
    static class DataDao {
        private String text;
        private List<DataDao> comments;
        private String thumbUp;
    }
}
