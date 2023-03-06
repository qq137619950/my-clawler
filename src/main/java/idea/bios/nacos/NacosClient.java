package idea.bios.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.util.Properties;

/**
 * Nacos的客户端
 * @author 86153
 */
@Slf4j
public class NacosClient {
    public static String getConfig() {
        try {
            String serverAddr = "192.168.76.11:30148";
            String dataId = "stroke-recommend.yaml";
            String group = "DEFAULT_GROUP";
            var properties = new Properties();
            properties.put("serverAddr", serverAddr);
            ConfigService configService = NacosFactory.createConfigService(properties);
            String content = configService.getConfig(dataId, group, 5000);
            log.info("fetch config from nacos.");
            return content;
        } catch (NacosException e) {
            log.warn("NacosException:", e);
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println(NacosClient.getConfig());
    }
}
