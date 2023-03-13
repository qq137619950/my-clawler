package idea.bios.util.selenium.script;

import lombok.var;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * 浏览器驱动
 * @author 86153
 */
public class ChromeDriverBuilder {
    private static final String DESKTOP_CHROME_PATH =
            "C:/Program Files/Google/Chrome/Application/chromedriver.exe";
    private static final String CHROME_PATH =
            "C:/Users/19106/AppData/Local/Google/Chrome/Application/chromedriver.exe";
    public static ChromeDriver buildScriptChromeDriver() {
        System.getProperties().setProperty("webdriver.chrome.driver", DESKTOP_CHROME_PATH);
        var chromeOptions = new ChromeOptions();
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        // 配置参数优化
        // 无头模式
        // chromeOptions.addArguments("--headless");
        // 禁用GPU和缓存
        // chromeOptions.addArguments("--disable-gpu");
        // chromeOptions.addArguments("--disable-gpu-program-cache");
        // chromeOptions.addArguments("--disable-software-rasterizer");
        // 配置不加载图片
        // chromeOptions.addArguments("--blink-settings=imagesEnabled=false");
        // 禁用插件加载
        // chromeOptions.addArguments("--disable-extensions");
        // 设置浏览器窗口大小
        chromeOptions.addArguments("--window-size=1920,1080");
        // 不使用沙箱
        // chromeOptions.addArguments("--no-sandbox");
        // chromeOptions.addArguments("--ignore-certificate-errors");
        // chromeOptions.addArguments("--allow-running-insecure-content");
        // chromeOptions.addArguments("--disable-dev-shm-usage");
        return new ChromeDriver(chromeOptions);
    }
}
