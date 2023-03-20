package idea.bios.util.selenium.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

/**
 * Selenium Driver
 * @author 86153
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeleniumDriverBo {
    private PhantomJSDriver phantomJSDriver;
    private ChromeDriver chromeDriver;
    private String proxyHostAndPort;
}
