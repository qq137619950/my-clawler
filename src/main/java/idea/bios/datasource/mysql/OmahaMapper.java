package idea.bios.datasource.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * mysql mapper
 * @author 86153
 */
@Mapper
public interface OmahaMapper {
    /**
     * 从omaha_term_only_chi表中批量获取数据
     * @param offset        offset
     * @param limit         limit
     * @return              List<String>
     */
    List<String> batchGetChiTerm(@Param("offset") int offset,
                                 @Param("limit") int limit);

    /**
     * 从bios_ch_term_short表中批量获取数据
     * @param offset
     * @param limit
     * @return
     */
    List<String> batchGetBiosChiShortTerm(@Param("offset") int offset,
                                          @Param("limit") int limit);
}
