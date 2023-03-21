package idea.bios.crawler.proxypool;

import idea.bios.config.ProxyPoolConfig;
import idea.bios.entity.HostAndPort;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 从ProxyPool中获取代理地址
 * TODO 检测到不通自动移除
 * @author 86153
 */
public class ProxyPoolFetcher {
    private static final AtomicInteger COUNT = new AtomicInteger(0);
    private static final List<String> CUR_HOST_AND_PORT_LIST;
    private static final Map<String, AtomicInteger> STATICS_MAP = new ConcurrentHashMap<>();
    static {
        CUR_HOST_AND_PORT_LIST = ProxyPoolConfig.getProxyPoolList().stream()
                .map(HostAndPort::toString)
                        .collect(Collectors.toList());
        ProxyPoolConfig.getProxyPoolList().forEach(
                hp -> STATICS_MAP.put(hp.toString(), new AtomicInteger(0)));
    }

    public static int getCurProxyPoolSize() {
        return CUR_HOST_AND_PORT_LIST.size();
    }

    public static String simpleGetHostAndPort() {
        String hp = CUR_HOST_AND_PORT_LIST.get(
                COUNT.incrementAndGet() % CUR_HOST_AND_PORT_LIST.size());
        STATICS_MAP.get(hp).incrementAndGet();
        return hp;
    }

    public static String robbinGetHostAndPort() {
        return null;
    }
}
