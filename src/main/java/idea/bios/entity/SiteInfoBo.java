package idea.bios.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网址
 * @author 86153
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteInfoBo {
    private String sourceId;
    private String className;
    private int minPolitenessDelay;
    private boolean chromeDriver;
    private boolean phantomJsDriver;
    private boolean isDynParse;
    private boolean run;
}
