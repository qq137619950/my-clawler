package idea.bios.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 86153
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostAndPort {
    String host;
    String port;

    @Override
    public String toString() {
        return host + ":" + port;
    }
}


