package idea.bios.util;


import lombok.extern.slf4j.Slf4j;


import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 单机简单定时器
 * @author 86153
 */
@Slf4j
public class Schedule {
    /**
     * 创建任务队列
     */
    private static final ScheduledExecutorService CRAWLER_SCHEDULE_EXECUTOR =
            Executors.newScheduledThreadPool(10);

    private static final ScheduledExecutorService CONTROLLER_SCHEDULE_EXECUTOR =
            Executors.newScheduledThreadPool(10);

    public static void crawlerScheduleAtFixedRate(Runnable command, long period) {
        CRAWLER_SCHEDULE_EXECUTOR.scheduleWithFixedDelay(command, period, period,
                TimeUnit.SECONDS);
    }

    public static void crawlerScheduleAtFixedRateMi(Runnable command, long period) {
        CRAWLER_SCHEDULE_EXECUTOR.scheduleWithFixedDelay(command, period, period,
                TimeUnit.MILLISECONDS);
    }

    public static void controllerScheduleAtFixedRate(Runnable command, long period) {
        CONTROLLER_SCHEDULE_EXECUTOR.scheduleWithFixedDelay(command, period, 60,
                TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        // 1s 后开始执行，每 3s 执行一次
        CRAWLER_SCHEDULE_EXECUTOR.scheduleAtFixedRate(() ->
                System.out.println("打印当前时间：" + new Date()),
                1, 3, TimeUnit.SECONDS);
    }

}
