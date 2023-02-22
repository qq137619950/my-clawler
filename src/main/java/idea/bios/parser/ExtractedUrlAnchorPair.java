package idea.bios.parser;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 86153
 */
public class ExtractedUrlAnchorPair {
    @Getter @Setter
    private String href;
    @Getter @Setter
    private String anchor;
    @Getter @Setter
    private String tag;
    @Getter @Setter
    private Map<String, String> attributes = new HashMap<>();

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, String val) {
        attributes.put(name, val);
    }
}