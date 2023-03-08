package idea.bios;

import idea.bios.jobs.com.ahospital.HospitalBaikeCrawler;
import idea.bios.jobs.com.chunyuyisheng.CyysDialogCrawler;
import idea.bios.jobs.com.dxy.DxyDialogCrawler;

/**
 * 启动类
 * @author 86153
 */
public class Main {
    public static void main(String[] args) throws Exception {
        new HospitalBaikeCrawler().runner();
    }
}
