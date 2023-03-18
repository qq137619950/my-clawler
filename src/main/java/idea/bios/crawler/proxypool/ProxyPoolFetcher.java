package idea.bios.crawler.proxypool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 从ProxyPool中获取代理地址
 * TODO 检测到不通自动移除
 * @author 86153
 */
public class ProxyPoolFetcher {
    private static final AtomicInteger COUNT = new AtomicInteger(0);

    public static String simpleGetHostAndPort() {
        return ProxyPoolEnum.getProxyHostAndPortList()
                .get(COUNT.incrementAndGet() % ProxyPoolEnum.values().length);
    }
}
