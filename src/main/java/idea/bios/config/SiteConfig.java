package idea.bios.config;

import idea.bios.crawler.my.starter.CommonCrawlerStarter;
import idea.bios.entity.SiteConfigYamlBo;
import idea.bios.entity.SiteInfoBo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;


/**
 * Site的config配置
 * @author 86153
 */
@Slf4j
public class SiteConfig {
    @Getter
    private static SiteInfoBo curSite = new SiteInfoBo();

    @Getter
    private static String jobRoot = "";

    static {
        // 读取配置
        try (InputStream in = CommonCrawlerStarter.class.getClassLoader()
                .getResourceAsStream("yaml/sites.yaml")) {
            // 加载 YAML 文件
            SiteConfigYamlBo configYamlBo = new Yaml().loadAs(in, SiteConfigYamlBo.class);
            // 找到run的网站，开始任务
            curSite = configYamlBo.getSites().stream()
                    .filter(SiteInfoBo::isRun).findFirst().get();
            jobRoot = configYamlBo.getJobRoot();
        } catch (Exception e) {
            log.warn("Exception", e);
        }
    }
}
