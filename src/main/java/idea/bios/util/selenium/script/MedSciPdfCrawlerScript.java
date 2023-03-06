package idea.bios.util.selenium.script;

import com.mongodb.client.MongoCollection;
import idea.bios.datasource.mongodb.MongoDb;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.bson.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.*;

/**
 * 梅斯 https://www.medsci.cn/guideline/search?s_id=2
 * @author 86153
 */
@Slf4j
public class MedSciPdfCrawlerScript {
    private static final String LOGIN_URL = "https://login.medsci.cn";
    private static final String NICK_NAME = "18871432492";
    private static final String PASSWORD = "Sunxiaowu#12";

    private static final String NICK_NAME_2 = "15327783808";
    private static final String PASSWORD_2 = "Idea@123123";

    private static final String NICK_NAME_3 = "13160612048";
    private static final String PASSWORD_3 = "Idea@2022";



    public static void main(String[] args) {
        try {
            run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void login(ChromeDriver driver) throws InterruptedException {
        driver.get(LOGIN_URL);
        Thread.sleep(2000);
        log.info("打开登录页面,地址是{}", LOGIN_URL);
        WebElement tab = driver.findElement(By.cssSelector("#registerTabs > a"));
        tab.click();
        Thread.sleep(1000);
        // 找到账号的输入框，并模拟输入账号
        WebElement nickname = driver.findElement(By.id("nickname"));
        nickname.sendKeys(NICK_NAME_2);
        WebElement password = driver.findElement(By.id("password"));
        password.sendKeys(PASSWORD_2);
        WebElement check = driver.findElement(By.cssSelector("#loginForm > div > form > div.ms-checkbox.check-link > label"));
        check.click();
        WebElement login = driver.findElement(By.id("submit"));
        login.click();
        Thread.sleep(2000);
    }

    private static void run() throws InterruptedException {
        ChromeDriver driver = buildScriptChromeDriver();
        MongoCollection<Document> collection = new MongoDb().getCrawlerDataCollection("cn.medsci.pdf.zn");
        int start = (int) collection.countDocuments();
        // 先登录
        login(driver);
        for (int type = 1; type <= 100; type++) {
            // 循环100页，如果没有数据，则break
            for (int i = 1; i <= 100; i++) {
                var wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                driver.get("https://www.medsci.cn/guideline/search?page=" + i
                        + "&s_id=" + type + "&tenant=100");
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.journal-list-items")));
                WebElement element = wait.until((ExpectedCondition<WebElement>)
                        d -> {
                            if (d != null) {
                                return d.findElement(By.cssSelector("div.journal-list-items"));
                            }
                            return null;
                        });
                List<WebElement> journalList = element.findElements(By.cssSelector("#journalList > div.journal-item"));
                if (journalList == null || journalList.isEmpty()) {
                    log.info("no data found. pageNum{}", i);
                    break;
                }
                // title
                WebElement listTitleElement = driver.findElement(By.cssSelector("div.journal-index-title"));
                if (listTitleElement == null || listTitleElement.getText() == null || listTitleElement.getText().isEmpty()) {
                    log.info("no title found. pageNum{}", i);
                    break;
                }
                var urls = new ArrayList<String>();
                journalList.forEach(e -> {
                    // 过滤掉非中文
                    WebElement tagElement = e.findElement(By.cssSelector("div > div > strong > span.item-label"));
                    if (tagElement == null || !tagElement.getText().contains("CN")) {
                        return;
                    }
                    // 获取链接
                    WebElement linkElement = e.findElement(By.cssSelector("div > div > strong > a"));
                    if (linkElement == null) {
                        log.warn("cannot find link");
                        return;
                    }
                    // 如果mongo中已经存在，则过滤
                    String site = linkElement.getAttribute("href");
                    long uc = collection.countDocuments(new Document("site", site));
                    if (uc == 0) {
                        urls.add(linkElement.getAttribute("href"));
                    }
                });
                // 处理每一条记录，写入mongodb
                urls.forEach(url -> {
                    driver.get(url);
                    var insertDoc = new Document();
                    WebElement typeElement = driver.findElement(By.cssSelector("#articleBox > div.wrapper > div > div > div > div > h2"));
                    if (typeElement == null) {
                        log.warn("type element empty.");
                        return;
                    }
                    insertDoc.append("type", typeElement.getText());
                    WebElement titleElement = driver.findElement(By.cssSelector("#headTitle"));
                    if (titleElement == null) {
                        log.warn("title element empty.");
                        return;
                    }
                    insertDoc.append("title", titleElement.getText());
                    WebElement infoElement = driver.findElement(By.cssSelector("#detail > div.shortcode-content"));
                    if (infoElement != null) {
                        insertDoc.append("info", infoElement.getText());
                    }
                    insertDoc.append("site", url);

                    // 点击下载
                    WebElement download = null;
                    try {
                        download = driver.findElement(By.cssSelector("#fileDownload"));
                    } catch (Exception e) {
                        log.warn("error!", e);
                        return;
                    }
                    if (download == null) {
                        return;
                    }
                    download.click();
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        log.error("InterruptedException", ex);
                    }
                    // 查找文件夹中新加入文件, 循环遍历3次
                    String fileName = null;
                    for (int k = 0; k < 3; k++) {
                        fileName = getFileName(typeElement.getText());
                        if (fileName == null) {
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            break;
                        }
                    }
                    if (fileName != null) {
                        insertDoc.append("fileName", fileName);
                    } else {
                        log.warn("error.");
                        return;
                    }
                    collection.insertOne(insertDoc);
                });
            }
        }
        // driver.quit();
        // driver.close();
    }

    private static final String DESKTOP_CHROME_PATH =
            "C:/Program Files/Google/Chrome/Application/chromedriver.exe";
    private static ChromeDriver buildScriptChromeDriver() {
        System.getProperties().setProperty("webdriver.chrome.driver", DESKTOP_CHROME_PATH);
        var chromeOptions = new ChromeOptions();
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        // 配置参数优化
        // 无头模式
        // chromeOptions.addArguments("--headless");
        // 禁用GPU和缓存
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-gpu-program-cache");
        chromeOptions.addArguments("--disable-software-rasterizer");
        // 配置不加载图片
        // chromeOptions.addArguments("--blink-settings=imagesEnabled=false");
        // 禁用插件加载
        // chromeOptions.addArguments("--disable-extensions");
        // 设置浏览器窗口大小
        chromeOptions.addArguments("--window-size=1920,1080");
        // 不使用沙箱
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--allow-running-insecure-content");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        return new ChromeDriver(chromeOptions);
    }


    private static String getFileName(String title){
        var file = new File("C:/Users/86153/Downloads");
        //判断文件或目录是否存在
        if(!file.exists()){
            log.info("【"+ "C:/Users/86153/Downloads" + " not exists】");
        }
        //获取该文件夹下所有的文件
        File[] fileArray= file.listFiles();
        File curFile;
        // 如果有两个以上的未命名文件，全部删除
        List<File> old = new ArrayList<>();
        assert fileArray != null;
        for (File f : fileArray) {
            if (!f.isDirectory() && !f.getName().startsWith("medsci-")) {
                old.add(f);
            }
        }
        if (old.size() == 0) {
            return null;
        }
        if (old.size() > 1) {
            log.warn("old too many!");
            return null;
        }
        // 改名
        File oldFile = old.get(0);
        String newName = "medsci-" + title + "-" + oldFile.getName();
        String newPath = oldFile.getPath().replace(oldFile.getName(), newName);
        boolean res = oldFile.renameTo(new File(newPath));
        if (!res) {
            log.warn("rename error! old:{}, newPath:{}", oldFile.getName(), newPath);
            return null;
        }
        return newName;
    }
}




