package idea.bios.crawler.my.controller;

import java.util.List;

/**
 * 在crawler中操作接口
 * @author 86153
 */
public interface CrawlerController {
    /**
     * 添加url到队列
     * @param pageUrls      url list
     */
    void addUrlsToQueue(List<String> pageUrls);

    /**
     * 停止推送数据开关
     */
    @Deprecated
    void putQueueFinish();
}
