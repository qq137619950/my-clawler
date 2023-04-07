package idea.bios.util.selenium.script;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static idea.bios.util.selenium.script.ChromeDriverBuilder.buildScriptChromeDriver;

/**
 * http://www.fsxsj.net/ch/reader/issue_list.aspx?year_id=2023&quarter_id=01
 * http://www.spinejournal.net/ch/reader/issue_list.aspx?year_id=2022&quarter_id=1
 * @author 86153
 */
public class FsxsjPdfCrawler {
    private static final String MENU_SITE_PRE =
            "http://www.spinejournal.net/ch/reader/issue_list.aspx?year_id=";

    public static void main(String[] args) {
        run();
    }

    private static void run() {
        ChromeDriver driver = buildScriptChromeDriver();
        IntStream.rangeClosed(2003, 2023).forEach(year -> IntStream.rangeClosed(1, 6)
                .forEach(month -> {
                    String curUrl = MENU_SITE_PRE + year + "&quarter_id="
                        + (month < 10 ? "0" + month : month);
                    driver.get(curUrl);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    List<WebElement> needToClick = driver.findElements(By.cssSelector(
                                    "tbody > tr > td > a")).stream().filter(e -> e.getText().contains("PDF"))
                            .collect(Collectors.toList());
                    needToClick.forEach(element -> {
                        try {
                            element.click();
                            Thread.sleep(3000);
                        } catch (Exception ignore) {}
                    });
        }));
    }
}
