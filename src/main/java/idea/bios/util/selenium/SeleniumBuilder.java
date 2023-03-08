package idea.bios.util.selenium;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
// import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 获取浏览器驱动实例
 * @author 86153
 */
@Slf4j
public class SeleniumBuilder {
    private static final String DESKTOP_CHROME_PATH =
            "C:/Program Files/Google/Chrome/Application/chromedriver.exe";
    private static final String CHROME_PATH =
            "C:/Users/19106/AppData/Local/Google/Chrome/Application/chromedriver.exe";
    private static final String CHROME_PATH_HYQ =
            "C:/Users/IDEA/AppData/Local/Google/Chrome/Application/chromedriver.exe";
    /**
     * 最大chrome驱动进程，10个为上限，否则系统相当卡顿
     */
    private static final int MAX_CHROME_DRIVER_PROCESS = 10;
    /**
     * 当前已经创建的驱动进程数
     */
    private static final AtomicInteger CUR_PROCESS_COUNT = new AtomicInteger(0);
    /**
     * 清除windows中滞留进程
     */
    private static final String WINDOWS_KILL_CMD = "taskkill /f /im chromedriver.exe";

    public static ChromeDriver getChromeDriver() {
        if (CUR_PROCESS_COUNT.get() >= MAX_CHROME_DRIVER_PROCESS) {
            log.warn("Chrome driver limit.");
            return null;
        }
        ChromeDriver driver;
        try {
            driver = buildChromeDriver();
        } catch (Exception e) {
            log.warn("exception occurs.", e);
            return null;
        }
        CUR_PROCESS_COUNT.getAndIncrement();
        return driver;
    }

    public static void shutdownDriver(ChromeDriver driver) {
        if (driver == null) {
            return;
        }
        driver.close();
        CUR_PROCESS_COUNT.decrementAndGet();
    }

    private static ChromeDriver buildChromeDriver() {
        System.getProperties().setProperty("webdriver.chrome.driver", DESKTOP_CHROME_PATH);
        var chromeOptions = new ChromeOptions();
        // chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        // 配置参数优化
        // 无头模式
        chromeOptions.addArguments("--headless");
        // 禁用GPU和缓存
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-gpu-program-cache");
        chromeOptions.addArguments("--disable-software-rasterizer");
        // 配置不加载图片
        chromeOptions.addArguments("--blink-settings=imagesEnabled=false");
        // 禁用插件加载
        chromeOptions.addArguments("--disable-extensions");
        // 设置浏览器窗口大小
        chromeOptions.addArguments("--window-size=1920,1080");
        // 不使用沙箱
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--ignore-certificate-errors");
        chromeOptions.addArguments("--allow-running-insecure-content");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        return new ChromeDriver(chromeOptions);
    }
}
