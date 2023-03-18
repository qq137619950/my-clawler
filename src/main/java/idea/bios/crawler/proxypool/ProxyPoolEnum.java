package idea.bios.crawler.proxypool;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代理服务器Enum
 * @author 86153
 */
@AllArgsConstructor
public enum ProxyPoolEnum {
    /**
     * 本地
     */
    local(0, "localhost", 0),
    /**
     * 阿里云上的http代理
     */
    proxy_1(1, "47.106.191.148", 43128),
    /**
     * 代理2
     */
    proxy_2(2, "192.168.218.26", 3128),
    /**
     * 代理3
     */
    proxy_3(3, "192.168.218.37", 13128),

    ;

    /**
     * 代理的ID
     */
    @Getter
    private final int id;
    /**
     * 代理的HOST
     */
    @Getter
    private final String host;
    /**
     * 代理的PORT
     */
    @Getter
    private final int port;

    public static List<String> getProxyHostAndPortList() {
        return Arrays.stream(ProxyPoolEnum.values())
                .map(item -> item.host + ":" + item.port)
                .collect(Collectors.toList());
    }
}
