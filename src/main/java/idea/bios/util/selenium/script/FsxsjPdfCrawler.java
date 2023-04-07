package idea.bios.util.selenium.script;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static idea.bios.util.selenium.script.ChromeDriverBuilder.buildScriptChromeDriver;

/**
 * 列表：http://www.e-tiller.com/et/customer/list
 * http://www.fsxsj.net/ch/reader/issue_list.aspx?year_id=2023&quarter_id=01
 * http://www.spinejournal.net/ch/reader/issue_list.aspx?year_id=2022&quarter_id=1
 * 医学信息学杂志 http://www.yxxxx.ac.cn/ch/reader/issue_list.aspx?year_id=1997&quarter_id=1
 * 中国卫生政策研究 http://journal.healthpolicy.cn/ch/reader/issue_list.aspx?year_id=2008&quarter_id=1
 *
 * @author 86153
 */
public class FsxsjPdfCrawler {
    private static final String MENU_SITE_PRE =
            "http://journal.healthpolicy.cn/ch/reader/issue_list.aspx?year_id=";

    public static void main(String[] args) throws InterruptedException {
        run();
    }

    private static void run() throws InterruptedException {
        ChromeDriver driver = buildScriptChromeDriver();
        driver.get("chrome://settings/content/pdfDocuments");
        Thread.sleep(5000);
        IntStream.rangeClosed(2022, 2023).forEach(year -> IntStream.rangeClosed(1, 12)
                .forEach(month -> {
                    String curUrl = MENU_SITE_PRE + year + "&quarter_id=" + month;
                        // + (month < 10 ? "0" + month : month);
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
