package idea.bios.util.selenium.script;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import static idea.bios.util.selenium.script.ChromeDriverBuilder.buildScriptChromeDriver;

/**
 * @author 86153
 */
public class OpenAiChatGPT4Crawler {
    private static final String LOGIN_URL = "https://auth0.openai.com/u/login/identifier?state=hKFo2SB5TDRYczVvMTZaN01YZTgzbnVDQm1zaDQ5MjloczBuaqFur3VuaXZlcnNhbC1sb2dpbqN0aWTZIGZPX1Brd0FnN1BNNzZkT0pvbWJwc2tkMlRsMzhMVktyo2NpZNkgVGRKSWNiZTE2V29USHROOTVueXl3aDVFNHlPbzZJdEc";
    private static final String GPT_URL = "https://chat.openai.com/?model=gpt-4";

    private static void login(ChromeDriver driver) throws InterruptedException {
        driver.get(LOGIN_URL);
        Thread.sleep(1000);
        WebElement nickname = driver.findElement(By.cssSelector("#username"));
        nickname.sendKeys("jyren422@gmail.com");
        WebElement submit = driver.findElement(By.cssSelector("form > div > button"));
        submit.click();
        Thread.sleep(2000);
        WebElement password = driver.findElement(By.cssSelector("#password"));
        password.sendKeys("idea@2022");
        WebElement submit2 = driver.findElement(By.cssSelector("form > div > button"));
        submit2.click();
    }


    public static void main(String[] args) throws InterruptedException {
        ChromeDriver driver = buildScriptChromeDriver();
        login(driver);
        driver.get("https://chat.openai.com/auth/login");
    }
}
