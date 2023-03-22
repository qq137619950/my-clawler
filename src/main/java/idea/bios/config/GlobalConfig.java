package idea.bios.config;

import idea.bios.crawler.my.starter.CommonCrawlerStarter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 全局配置
 * @author 86153
 */
@Slf4j
public class GlobalConfig {
    private static Map<String, Object> globalConfig = null;
    @Getter
    private static String wxKey;

    static {
        // 读取配置
        try (InputStream in = CommonCrawlerStarter.class.getClassLoader()
                .getResourceAsStream("yaml/global.yaml")) {
            // 加载 YAML 文件
            globalConfig = new Yaml().loadAs(in, Map.class);
            wxKey = (String) globalConfig.get("wxKey");
        } catch (Exception e) {
            log.warn("Exception", e);
        }
    }

    public static String getChromeDriverPath() {
        List<Map<String, String>> config = (List<Map<String, String>>) globalConfig.get("driver");
        for(Map<String, String> map : config) {
            if (map.containsKey("chrome")) {
                return map.get("chrome");
            }
        }
        return null;
    }

    public static String getPhantomJsDriverPath() {
        List<Map<String, String>> config = (List<Map<String, String>>) globalConfig.get("driver");
        for(Map<String, String> map : config) {
            if (map.containsKey("phantomJs")) {
                return map.get("phantomJs");
            }
        }
        return null;
    }

}
