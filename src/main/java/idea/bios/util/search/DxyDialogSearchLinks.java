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
public class DxyDialogSearchLinks implements SearchLinks {

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
            Elements contentList = body.select(
                    "div.content-info > div.content-list > a");
            if (contentList == null || contentList.isEmpty()) {
                return list;
            }
            contentList.forEach(c -> list.add(c.absUrl("href")));
        }
        return list;
    }

    public List<String> getAllLinks(String prefix) {
        var res = new ArrayList<String>();
        for(int i = 1; i <= 10; i++) {
            res.addAll(getLinks(prefix + i));
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                log.warn("InterruptedException", e);
                break;
            }
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(new DxyDialogSearchLinks().getLinks(
                "https://dxy.com/search/questions/%E8%84%91%E5%8D%92%E4%B8%AD?page_index=10"));
    }
}
