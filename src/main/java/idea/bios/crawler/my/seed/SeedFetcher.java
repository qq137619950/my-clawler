package idea.bios.crawler.my.seed;

import java.util.List;
import java.util.function.Function;

/**
 * seeds的获取接口
 * @author 86153
 */
public interface SeedFetcher {
    /**
     * 获取seeds
     * @param urls urls
     * @return  seeds
     */
    List<String> getSeedsPlain(String... urls);

    /**
     * 从数据库中获取url列表，数据库固定
     * @param sql               sql语句
     * @param offset            offset
     * @param limit             limit
     * @param urlConvert        url处理函数，比如拼装成url
     * @return                  seeds
     */
    List<String> getSeedsFromDb(int offset,
                                int limit,
                                Function<String, String> urlConvert);

    /**
     * 从文件中读取url，一个url为一行
     * @param filePath  文件路径
     * @return          seed
     */
    List<String> getSeedsFromTxt(String filePath);
}
