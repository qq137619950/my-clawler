package idea.bios.util.search;

import java.util.List;

/**
 * 通过一些搜索引擎获取链接
 * @author 86153
 */
@FunctionalInterface
public interface SearchLinks {
    /**
     * 在搜索引擎中获取links
     * @param url   url
     * @return      List<String>
     */
    List<String> getLinks(String url);
}
