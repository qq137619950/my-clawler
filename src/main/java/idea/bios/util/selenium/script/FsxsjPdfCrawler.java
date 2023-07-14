package idea.bios.util.selenium.script;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
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
 * 中国肿瘤生物治疗杂志 http://www.biother.cn/zgzlswzlzz/ch/reader/issue_list.aspx?year_id=2008&quarter_id=1
 * 医用生物力学 http://www.mechanobiology.cn/yyswlx/ch/reader/issue_list.aspx?year_id=2008&quarter_id=1
 * 药学实践与服务 http://yxsjzz.smmu.edu.cn/ch/reader/issue_list.aspx?year_id=2008&quarter_id=1
 * 中国介入影像与治疗学  http://www.cjiit.com/cjiit/ch/reader/issue_list.aspx?year_id=2008&quarter_id=1
 * 江苏中医药  http://www.jstcm.cn/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 中西医结合研究 http://ritcwm.com//ch/reader/issue_list.aspx?year_id=2009&quarter_id=1
     * 中国骨伤  http://www.zggszz.com/zggszzcn/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 医学研究杂志  http://www.yxyjzz.cn/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 中华医学教育探索杂志  http://yxjyts.cnjournals.com/yxjyts/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 现代药物与临床  https://www.tiprpress.com/xdywlc/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 中国脊柱脊髓杂志  http://www.cspine.org.cn/zgjzjszz/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 中国骨质疏松杂志  http://www.chinacjo.com/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 中国药师  http://zgyszz.cnjournals.org/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 浙江中西医结合杂志  http://zjzxy.alljournal.com.cn/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 中国肿瘤  http://www.chinaoncology.cn/zgzl8/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 * 天津中医药大学学报  http://tjzhongyiyao.ijournals.cn/tjzyydxxb/ch/reader/issue_list.aspx?year_id=2019&quarter_id=1
 *
 * @author 86153
 */
public class FsxsjPdfCrawler {
    private static final String MENU_SITE_PRE =
            "http://tjzhongyiyao.ijournals.cn/tjzyydxxb/ch/reader/issue_list.aspx?year_id=";

    public static void main(String[] args) throws InterruptedException {
        run();
    }

    private static void run() throws InterruptedException {
        ChromeDriver driver = buildScriptChromeDriver();
        driver.get("chrome://settings/content/pdfDocuments");
        Thread.sleep(5000);
        IntStream.rangeClosed(1990, 2023).forEach(year -> IntStream.rangeClosed(1, 12)
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
                            List<String> windows = new ArrayList<>(driver.getWindowHandles());
                            if (windows.size() > 1) {
                                WebDriver.TargetLocator targetLocator = driver.switchTo();
                                targetLocator.window(windows.get(windows.size() - 1));
                                driver.close();
                                targetLocator.window(windows.get(0));
                            }
                        } catch (Exception ignore) {}
                    });
        }));
    }
}
