package idea.bios.crawler.my.controller;

import idea.bios.crawler.CrawlConfig;

import java.util.List;

/**
 * 在crawler中controller暴露出的接口
 * @author 86153
 */
public interface ControllerFacade {
    /**
     * 添加url到队列
     * @param pageUrls      url list
     */
    void addUrlsToQueue(List<String> pageUrls);

    /**
     * 停止推送数据开关
     */
    void putQueueFinish();

    /**
     * 获取config
     * @return  CrawlConfig
     */
    CrawlConfig getCrawlConfig();
}
