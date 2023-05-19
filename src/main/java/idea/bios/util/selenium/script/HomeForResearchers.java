package idea.bios.util.selenium.script;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import idea.bios.datasource.mongodb.MongoDb;
import lombok.var;
import org.bson.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Map;
import java.util.stream.IntStream;

import static idea.bios.util.selenium.script.ChromeDriverBuilder.buildScriptChromeDriver;

/**
 * 医考真题汇
 * <a href="https://question.home-for-researchers.com/">...</a>
 * @author 86153
 */
public class HomeForResearchers {
    private static final MongoCollection<Document> COLLECTION = new MongoDb().getCrawlerDataCollection(
            "com.home-for-researchers");
    public static void main(String[] args) throws InterruptedException {
        ChromeDriver driver = buildScriptChromeDriver();
        // 设置cookie
//        Cookie c1 = new Cookie("Hm_lvt_a00e0672d163bf6e6278fa093592d492", "1683880442");
//        Cookie c2 = new Cookie("Hm_lvt_2309b3a60fa801f73807c5ef901439c8", "1683880450");
//        Cookie c3 = new Cookie("Hm_lpvt_2309b3a60fa801f73807c5ef901439c8", "1683880450");
//        Cookie c4 = new Cookie("satoken", "da0e9659-90c0-4ee7-9089-c7f01e312501");
//        Cookie c5 = new Cookie("Hm_lpvt_a00e0672d163bf6e6278fa093592d492", "1684465821");
//        driver.manage().addCookie(c1);
//        driver.manage().addCookie(c2);
//        driver.manage().addCookie(c3);
//        driver.manage().addCookie(c4);
//        driver.manage().addCookie(c5);
//        Thread.sleep(3000);
        run(driver);
    }

    private static void run(ChromeDriver driver) throws InterruptedException {
        var wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // driver.get("https://question.home-for-researchers.com/pages/qbms/practice?chapterId=1");
        driver.get("https://www.home-for-researchers.com/static/index.html#");
        driver.navigate().refresh();
        // 等待10s使用微信登录
        Thread.sleep(2000);
        // 开始点题
        IntStream.rangeClosed(1, 3000).forEach(chapter -> {
//            driver.get("https://question.home-for-researchers.com/pages/qbms/practice?chapterId=" + chapter);
//            // 寻找标题列表
//            // body > uni-app > uni-page > uni-page-wrapper > uni-page-body > uni-view > uni-view.bg-white.padding-lr-lg > div > div.padding-left-xl.padding-top-lg.el-col.el-col-20 > div > div.el-col.el-col-8 > uni-view > uni-view > uni-view.grid
//            WebElement listElement = wait.until((ExpectedCondition<WebElement>)
//                    d -> {
//                        if (d != null) {
//                            return d.findElement(By.cssSelector(
//                                    " div > div.el-col > uni-view > uni-view.grid"));
//                        }
//                        return null;
//                    });
//            if (listElement == null) {
//                return;
//            }
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            List<WebElement> unitList = listElement.findElements(By.cssSelector(
//                    "uni-view.padding-xs"));
//            if (unitList == null || unitList.isEmpty()) {
//                return;
//            }
//            unitList.forEach(unit -> {
//                unit.click();
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                // 都选A
//                WebElement chooseA = driver.findElement(By.cssSelector(
//                        "uni-view > uni-view > uni-view.padding-sm"));
//                chooseA.click();
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                try {
//                    WebElement submit = driver.findElement(By.cssSelector(
//                            "uni-view.text-center> button"));
//                    submit.click();
//                    Thread.sleep(1000);
//                } catch (Exception ignored) {
//                }
//            });

            // 调用api获取json
            // https://question.home-for-researchers.com/api/client/qbms/question/list?pageNum=1&chapterId=2
            driver.get(
                    "https://question.home-for-researchers.com/api/client/qbms/question/list?pageNum=1&chapterId=" + chapter);
            try {
                WebElement jsonElementTemp = driver.findElement(By.tagName("body"));
                if (jsonElementTemp != null) {
                    String jsonStrTemp = jsonElementTemp.getText();
                    // 查找总数
                    Map<String, Object> mapTemp = new Gson().fromJson(jsonStrTemp, Map.class);
                    Double total = (Double) mapTemp.get("total");
                    if (total == null || total <= 0) {
                        return;
                    }
                    // 遍历total次
                    IntStream.rangeClosed(1, total.intValue()).forEach(i -> {
                        driver.get("https://question.home-for-researchers.com/api/client/qbms/question/list?pageNum="
                            + i + "&chapterId=" + chapter);
                        try {
                            // 限制一下频率
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        WebElement jsonElement = driver.findElement(By.tagName("body"));
                        if (jsonElement == null) {
                            return;
                        }
                        String jsonStr = jsonElementTemp.getText();
                        // 查找总数
                        Map<String, Object> map = new Gson().fromJson(jsonStr, Map.class);
                        Map<String, Object> data = (Map<String, Object>) map.get("rows");
                        if (data == null) {
                            return;
                        }
                        // 落库
                        var doc = new Document();
                        data.forEach(doc::append);
                        try {
                            COLLECTION.insertOne(doc);
                        } catch (Exception ignored) {
                        }
                    });
                }
            } catch (Exception ignored) {
            }

        });
    }
}
