package idea.bios.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 86153
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyPoolYamlBo {
    List<HostAndPort> proxy;
}
