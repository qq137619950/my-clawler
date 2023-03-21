package idea.bios.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 从yaml中读取配置
 * @author 86153
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteConfigYamlBo {
    List<SiteInfoBo> sites;
}
