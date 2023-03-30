package idea.bios.util.selenium.script;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static idea.bios.util.selenium.script.ChromeDriverBuilder.buildScriptChromeDriver;

/**
 * https://lchc.hebmu.edu.cn/CN/volumn/volumn_446.shtml#1
 * @author 86153
 */
public class LchcHebmuPdfCrawler {
    private static final String MENU_SITE_PRE =
            "https://lchc.hebmu.edu.cn/CN/volumn/volumn_";

    public static void main(String[] args) {
        run();
    }

    private static void run() {
        ChromeDriver driver = buildScriptChromeDriver();
        IntStream.rangeClosed(89, 446).forEach(index -> {
            String curUrl = MENU_SITE_PRE + index + ".shtml";
            driver.get(curUrl);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<WebElement> needToClick = driver.findElements(By.cssSelector(
                    "a.txt_zhaiyao1")).stream().filter(e -> e.getText().contains("PDF"))
                    .collect(Collectors.toList());
            needToClick.forEach(element -> {
                        try {
                            element.click();
                            Thread.sleep(1000);
                        } catch (Exception ignore) {

                        }
                    });
        });
    }
}
