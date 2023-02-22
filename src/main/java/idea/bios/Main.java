package idea.bios;

import idea.bios.jobs.com.bh.BaiduBhListCrawler;

/**
 * 启动类
 * @author 86153
 */
public class Main {
    public static void main(String[] args) throws Exception {
        new BaiduBhListCrawler().runner();
    }
}
