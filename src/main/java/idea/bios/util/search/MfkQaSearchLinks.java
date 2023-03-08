package idea.bios.util.search;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 86153
 */
@Slf4j
public class MfkQaSearchLinks implements SearchLinks {
    @Override
    public List<String> getLinks(String url) {
        var list = new ArrayList<String>();
        Connection connection = Jsoup.connect(url);
        Connection.Response res;
        try {
            res = connection.execute().charset("utf-8");
        } catch (IOException e) {
            log.warn("Exception occurs.", e);
            return list;
        }
        if (res.statusCode() == 200) {
            String html = res.body();
            // 解析
            Document doc = Jsoup.parseBodyFragment(html);
            Element body = doc.body();
            Elements box = body.select("div.ArticleSearchContent > div > div.questionsBoxData");
            if (box != null && !box.isEmpty()) {
                box.forEach(b -> list.add(b.selectFirst("div.boxList > a")
                        .attr("href")));
            }
        }
        return list;
    }

    public static void main(String[] args) {
        System.out.println(new MfkQaSearchLinks().getLinks(
                "https://www.mfk.com/search/?q=%E5%8D%92%E4%B8%AD&page=2"));
    }
}
