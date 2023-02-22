package idea.bios.util;
import lombok.var;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 86153
 */
public class JsoupUtils {

    /**
     * 获取Element下所有图片链接
     * @param element   element
     */
    public static List<String> getElementAllImgSrc(Element element) {
        if (element == null) {
            return new ArrayList<>();
        }
        Elements imgUrlElements = element.getElementsByTag("img");
        return imgUrlElements.stream()
                .map(item -> item.absUrl("src")).distinct().collect(Collectors.toList());
    }


    public static String getBeautifulText(Element element) {
        if (element == null) {
            return "";
        }
        var texts = new ArrayList<String>();
        Elements all = element.getAllElements();
        for (Element e : all) {
            // 如果是list，则直接pass
            boolean isPrintAllElement = false;
            for (Element parent : e.parents()) {
                if (parent.is("li") || parent.is("ul") ||
                        parent.is("p")) {
                    isPrintAllElement = true;
                    break;
                }
            }
            if (isPrintAllElement) {
                continue;
            }
            if (e.is("ul")) {
                // 表格的输出方式
                texts.add(e.getElementsByTag("li").stream().map(Element::text)
                                .peek(item -> item.replace('\n', '\t'))
                                .collect(Collectors.joining("\n")));
                continue;
            } else if (e.is("p")) {
                // TODO 注意p下面有ul
                texts.add(e.text());
                continue;
            }
            // 如果子节点质包含a，就输出
            if (isPutTextElement(e)) {
                // 输出text
                texts.add(e.text());
            }
        }
        return String.join("\n",
                texts.stream().filter(item -> !item.isEmpty()).collect(Collectors.toList()));
    }

    private static boolean isPutTextElement(Element element) {
        if (element == null || element.is("a")) {
            return false;
        }
        int cCount = 0;
        for(Element e : element.children()) {
            if (!e.is("a")) {
                cCount++;
            }
        }
        return cCount == 0;
    }
}
