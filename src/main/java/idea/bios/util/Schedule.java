package idea.bios.util;

import idea.bios.crawler.my.seed.SeedFetcher;
import idea.bios.crawler.my.seed.SeedFetcherImpl;
import idea.bios.util.search.BaiduSfSearchLinks;
import idea.bios.util.search.SearchLinks;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单机简单定时器
 * @author 86153
 */
@Slf4j
public class Schedule {
    /**
     * 创建任务队列
     */
    private static final ScheduledExecutorService scheduledExecutorService =
            Executors.newScheduledThreadPool(5);

    public static void scheduleAtFixedRate(Runnable command, long period) {
        scheduledExecutorService.scheduleAtFixedRate(command, period, period,
                TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        // 1s 后开始执行，每 3s 执行一次
        scheduledExecutorService.scheduleAtFixedRate(() ->
                System.out.println("打印当前时间：" + new Date()),
                1, 3, TimeUnit.SECONDS);
    }

}
