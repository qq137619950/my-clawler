package idea.bios.crawler.my.starter;

import java.io.IOException;
import java.util.List;

/**
 * @author 86153
 */
@FunctionalInterface
public interface URLSourceBuilder {
    /**
     * 批量获取URL接口
     * @param offset        offset
     * @param limit         limit
     * @return              结果列表
     * @throws IOException  IOException
     */
    List<String> batchGetUrls(int offset, int limit) throws IOException;
}
