package idea.bios.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * controller的info
 * @author 86153
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControllerInfoBo {
    private String name;
    private int remainItem;
}
