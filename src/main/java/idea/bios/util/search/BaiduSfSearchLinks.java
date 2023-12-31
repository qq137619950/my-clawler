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
import java.util.UUID;

/**
 * @author 86153
 */
@Slf4j
public class BaiduSfSearchLinks implements SearchLinks {
    public static final String URL_PREFIX =
            "https://www.baidu.com/sf?openapi=1&dspName=iphone&from_sf=1&pd=wenda_kg&resource_id=5243&&dsp=iphone&aptstamp=1678266350&top=%7B%22sfhs%22%3A11%7D&alr=1&fromSite=pc&total_res_num=812&ms=1&frsrcid=5242&frorder=3&lid=11342033447821599347&pcEqid=9d66ff54000162730000000664084fed&word=";
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
            Element e = body.selectFirst(
                    "#super-frame > div > b-superframe-body > div > div.sfa-results > div > div > article > section > section > div.c-infinite-scroll");
            if (e == null) {
                log.warn("cannot find element. url:{}", url);
                return list;
            }
            Elements ce = e.select("div");
            ce.forEach(d -> {
                String u = d.select("a").attr("data-url");
                if (u != null && !u.isEmpty()) {
                    list.add(d.select("a").attr("data-url"));
                }
            });
        }
        return list;
    }

    public static void main(String[] args) {
        System.out.println(
                new BaiduSfSearchLinks().getLinks(URL_PREFIX + "阿尔兹海默症"));
//        UUID uuid = UUID.nameUUIDFromBytes("".getBytes());
//        System.out.println(uuid.toString());
    }
}
