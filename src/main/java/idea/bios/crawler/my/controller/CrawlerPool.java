package idea.bios.crawler.my.controller;

import idea.bios.crawler.WebCrawler;
import idea.bios.util.selenium.SeleniumBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 爬虫线程管理
 * @author 86153
 */
@Slf4j
public class CrawlerPool<T extends WebCrawler> {
    /**
     * 线程列表
     */
    @Getter
    private final List<Thread> THREADS = new ArrayList<>();
    /**
     * 爬虫队列
     */
    @Getter
    private final List<T> CRAWLERS = new ArrayList<>();
    /**
     * 统计失败个数
     * 失败个数过多则移除Crawler
     */
    @Getter
    private final List<Integer> FAILS = new ArrayList<>();

    /**
     * 初始化crawler池
     * @param numberOfCrawlers  crawler个数
     * @param clazz             crawler类
     * @param controller        controller注入crawler
     */
    public void init(int numberOfCrawlers, Class<T> clazz, CommonController controller) {
        // 构造Crawler
        IntStream.rangeClosed(1, numberOfCrawlers).forEach(i -> {
            try {
                T crawler = clazz.getDeclaredConstructor(ControllerFacade.class)
                        .newInstance(controller);
                var thread = new Thread(crawler, crawlerThreadBuilder(i));
                crawler.setMyThread(thread);
                // 初始化crawler
                crawler.init(i, controller);
                thread.start();
                CRAWLERS.add(crawler);
                THREADS.add(thread);
                log.info("Crawler {} started", i);
            } catch (Exception e) {
                log.warn("Exception occurs.", e);
            }
        });
    }

    /**
     * 更新crawler
     * @param clazz         crawler类
     * @param controller    controller注入crawler
     * @param index         索引
     */
    public void rebuildCrawler(Class<T> clazz, CommonController controller, int index) {
        try {
            // 创建新的crawler
            T crawler = clazz.getDeclaredConstructor(ControllerFacade.class)
                    .newInstance(controller);
            T droppedCrawler = CRAWLERS.remove(index);
            Thread thread = new Thread(crawler, crawlerThreadBuilder(index + 1));
            Thread droppedThread = THREADS.remove(index);
            log.warn("Thread {} is dropped. then new one:{}",
                    droppedThread.getName(), thread.getName());
            THREADS.add(index, thread);
            crawler.setMyThread(thread);
            // crawler.init(index + 1, controller);
            // init 继承上个线程的driver和fetcher
            crawler.init(index + 1, controller,
                    droppedCrawler.getChromeDriver(), droppedCrawler.getPhantomJsDriver());
            thread.start();
            CRAWLERS.add(index, crawler);
        } catch (Exception e) {
            log.warn("rebuild crawler exception occurs.", e);
        }
    }

    private static String crawlerThreadBuilder(int index) {
        return "Crawler " + index;
    }
}
