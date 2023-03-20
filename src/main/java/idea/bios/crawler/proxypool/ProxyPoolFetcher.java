package idea.bios.crawler.proxypool;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 从ProxyPool中获取代理地址
 * TODO 检测到不通自动移除
 * @author 86153
 */
public class ProxyPoolFetcher {
    private static final AtomicInteger COUNT = new AtomicInteger(0);

    private static final List<String> CUR_HOST_AND_PORT = ProxyPoolEnum.getProxyHostAndPortList();

    private static final Map<String, AtomicInteger> STATICS_MAP = new ConcurrentHashMap<>();
    static {
        CUR_HOST_AND_PORT.forEach(hp -> STATICS_MAP.put(hp, new AtomicInteger(0)));
    }

    public static String simpleGetHostAndPort() {
        String hp = CUR_HOST_AND_PORT.get(
                COUNT.incrementAndGet() % CUR_HOST_AND_PORT.size());
        STATICS_MAP.get(hp).incrementAndGet();
        return hp;
    }

    public static String robbinGetHostAndPort() {
        return null;
    }
}
