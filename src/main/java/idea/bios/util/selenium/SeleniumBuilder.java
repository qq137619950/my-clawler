package idea.bios.util.selenium;

import idea.bios.crawler.proxypool.ProxyPoolFetcher;
import idea.bios.util.selenium.entity.SeleniumDriverBo;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
// import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

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

    private static final int MAX_PHANTOM_JS_DRIVER_PROCESS = 10;
    /**
     * 当前已经创建的驱动进程数
     */
    private static final AtomicInteger CUR_CHROME_PROCESS_COUNT = new AtomicInteger(0);

    private static final AtomicInteger CUR_PHANTOM_JS_PROCESS_COUNT = new AtomicInteger(0);
    /**
     * 清除windows中滞留进程
     */
    private static final String WINDOWS_KILL_CMD = "taskkill /f /im chromedriver.exe";

    /**
     * 谷歌浏览器的Driver
     * @return  ChromeDriver
     */
    public static SeleniumDriverBo getChromeSeleniumBo() {
        if (CUR_CHROME_PROCESS_COUNT.get() >= MAX_CHROME_DRIVER_PROCESS) {
            log.warn("Chrome driver limit.");
            return new SeleniumDriverBo();
        }
        SeleniumDriverBo driver;
        try {
            driver = buildChromeDriver();
        } catch (Exception e) {
            log.warn("exception occurs.", e);
            return new SeleniumDriverBo();
        }
        CUR_CHROME_PROCESS_COUNT.getAndIncrement();
        return driver;
    }

    /**
     * 关闭一个谷歌浏览器driver
     * @param driver    chrome driver
     */
    public static void shutdownChromeDriver(ChromeDriver driver) {
        if (driver == null) {
            return;
        }
        driver.close();
        CUR_CHROME_PROCESS_COUNT.decrementAndGet();
    }

    /**
     * 获取一个Phantom Js Driver
     * @return PhantomJSDriver
     */
    public static SeleniumDriverBo getPhantomJsSeleniumBo() {
        if (CUR_PHANTOM_JS_PROCESS_COUNT.get() >= MAX_PHANTOM_JS_DRIVER_PROCESS) {
            log.warn("PHANTOM JS driver limit.");
            return new SeleniumDriverBo();
        }
        SeleniumDriverBo driver;
        try {
            driver = buildPhantomJsDriver();
        } catch (Exception e) {
            log.warn("exception occurs.", e);
            return new SeleniumDriverBo();
        }
        CUR_PHANTOM_JS_PROCESS_COUNT.getAndIncrement();
        return driver;
    }

    /**
     * 关闭一个PHANTOM_JS浏览器driver
     * @param driver    PHANTOM JS driver
     */
    public static void shutdownPhantomJsDriver(PhantomJSDriver driver) {
        if (driver == null) {
            return;
        }
        driver.close();
        CUR_PHANTOM_JS_PROCESS_COUNT.decrementAndGet();
    }

    private static SeleniumDriverBo buildPhantomJsDriver() {
        // TODO 将执行文件打包起来
        final String absoluteExePath = "C:/crawler/phantomjs-win.exe";
        //设置必要参数
        var dcaps = new DesiredCapabilities();
        //ssl证书支持
        dcaps.setCapability("acceptSslCerts", true);
        //css搜索支持
        dcaps.setCapability("cssSelectorsEnabled", false);
        //js支持
        // dcaps.setJavascriptEnabled(true);
        // 从代理池中获取代理
        String proxyIpAndPort = ProxyPoolFetcher.simpleGetHostAndPort();
        if (!proxyIpAndPort.startsWith("localhost")) {
            var proxy = new Proxy();
            proxy.setHttpProxy(proxyIpAndPort).setFtpProxy(proxyIpAndPort).setSslProxy(proxyIpAndPort);
            dcaps.setCapability(CapabilityType.PROXY, proxy);
        }
        dcaps.setAcceptInsecureCerts(true);
        dcaps.setPlatform(Platform.WIN11);
        //驱动支持（第二参数表明的是你的phantomjs引擎所在的路径，使用whereis phantomjs可以查看）
        dcaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                absoluteExePath);
        //创建无界面浏览器对象
        return SeleniumDriverBo.builder()
                .phantomJSDriver(new PhantomJSDriver(dcaps))
                .proxyHostAndPort(proxyIpAndPort)
                .build();
    }

    private static SeleniumDriverBo buildChromeDriver() {
        System.getProperties().setProperty("webdriver.chrome.driver",
                DESKTOP_CHROME_PATH);
        var chromeOptions = new ChromeOptions();
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        // 配置参数优化
        // 无头模式
        // chromeOptions.addArguments("--headless");
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
        // 从代理池中获取代理
        String proxyIpAndPort = ProxyPoolFetcher.simpleGetHostAndPort();
        if (!proxyIpAndPort.startsWith("localhost")) {
            var proxy = new Proxy();
            proxy.setHttpProxy(proxyIpAndPort).setFtpProxy(proxyIpAndPort).setSslProxy(proxyIpAndPort);
            chromeOptions.setProxy(proxy);
        }
        return SeleniumDriverBo.builder()
                .chromeDriver(new ChromeDriver(chromeOptions))
                .proxyHostAndPort(proxyIpAndPort)
                .build();
    }
}
