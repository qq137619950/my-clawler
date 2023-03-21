package idea.bios.config;

import idea.bios.crawler.my.starter.CommonCrawlerStarter;
import idea.bios.entity.HostAndPort;
import idea.bios.entity.ProxyPoolYamlBo;
import idea.bios.entity.SiteConfigYamlBo;
import idea.bios.entity.SiteInfoBo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;

/**
 * 代理池
 * @author 86153
 */
@Slf4j
public class ProxyPoolConfig {
    @Getter
    private static List<HostAndPort> proxyPoolList;
    static {
        // 读取配置
        try (InputStream in = CommonCrawlerStarter.class.getClassLoader()
                .getResourceAsStream("proxy.yaml")) {
            // 加载 YAML 文件
            ProxyPoolYamlBo config = new Yaml().loadAs(in, ProxyPoolYamlBo.class);
            proxyPoolList = config.getProxy();
        } catch (Exception e) {
            log.warn("Exception", e);
        }
    }

}
