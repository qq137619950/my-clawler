package idea.bios.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * crawler统计信息  线程安全的类
 * @author 86153
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlerStaticsBo {
    private int lastVisitPageNum;
    private int curVisitPageNum;

    private int lastTimeStamp;
    private int curTimeStamp;

    public void increaseVisitNum() {
        curVisitPageNum++;
    }
}
