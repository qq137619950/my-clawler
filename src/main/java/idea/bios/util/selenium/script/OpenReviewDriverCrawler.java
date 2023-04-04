package idea.bios.util.selenium.script;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import idea.bios.datasource.mongodb.MongoDb;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static idea.bios.util.selenium.script.ChromeDriverBuilder.buildScriptChromeDriver;

/**
 * https://openreview.net/
 * @author 86153
 */
@Slf4j
public class OpenReviewDriverCrawler {
    private static final MongoCollection<Document> COLLECTION = new MongoDb()
            .getCrawlerDataCollection("net.openreview.pdf.json");

    public static void main(String[] args) throws InterruptedException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        // 查询所有的parentGroup
        ChromeDriver driver = buildScriptChromeDriver();
        driver.get("https://openreview.net/");
        Thread.sleep(5000);
        WebElement webElement = driver.findElement(By.cssSelector(
                "#all-venues > div > ul.list-inline"));
        List<WebElement> list = webElement.findElements(By.cssSelector(
                "li > h2 > a"));
        Pattern pattern = Pattern.compile("id=[\\s\\S]*&");

        List<String> groupIds = list.stream()
                .map(item -> {
                    String link = item.getAttribute("href");
                    Matcher matcher = pattern.matcher(link);
                    if (matcher.find()) {
                        String m = matcher.group(0);
                        return m.substring(3, m.length() - 1);
                    }
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
        driver.close();

        // 查询所有groupId
        groupIds.forEach(groupId -> {
            var httpGet = new HttpGet(
                    "https://api.openreview.net/groups?parent=" + groupId);
            httpGet.setHeader("Content-Type", "application/json; charset=utf-8");
            CloseableHttpResponse response = null;
            try {
                response = client.execute(httpGet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String res = null;
            try {
                res = EntityUtils.toString(response.getEntity());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Map<String, Object> resMap = new Gson().fromJson(res, Map.class);
            List<Map<String, Object>> notes = (List<Map<String, Object>>) resMap.get("groups");
            // 抽出groupId
            var groupIdList = new ArrayList<String>();
            notes.forEach(item -> {
                String id = (String) item.get("id");
                String web = (String) item.get("web");
                if (web == null) {
                    return;
                }
                Pattern pattern2 = Pattern.compile(id + "/[a-zA-z0-9]*'");
                Matcher matcher = pattern2.matcher(web);
                while (matcher.find()) {
                    String str = matcher.group(0);
                    groupIdList.add(str.replaceAll("'", ""));
                }
            });
            groupIdList.forEach(group -> {
                try {
                    run(group);
                    Thread.sleep(3000);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });

    }

    private static void run(String groupId) throws IOException {
        final int step = 1000;
        int start = 0;
        // 调接口获取json
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        var proxy = new HttpHost("192.168.218.37", 13128);
        clientBuilder.setProxy(proxy);
        CloseableHttpClient client = clientBuilder.build();
        var arr = new ArrayList<Map<String, Object>>();
        while (true) {
            var httpGet = new HttpGet(
                    "https://api.openreview.net/notes?offset=" + start + "&limit=" + step +
                            "&invitation="
                            + groupId + "/-/Blind_Submission");
            // 设置请求头信息
            httpGet.setHeader("Content-Type", "application/json; charset=utf-8");
            CloseableHttpResponse response = client.execute(httpGet);
            String res = EntityUtils.toString(response.getEntity());
            Map<String, Object> resMap = new Gson().fromJson(res, Map.class);
            List<Map<String, Object>> notes = (List<Map<String, Object>>) resMap.get("notes");
            if (notes.isEmpty()) {
                break;
            } else {
                arr.addAll(notes);
                start += step;
            }
        }
        // arr处理
        arr.forEach(a -> {
            var finalItem = new LinkedHashMap<String, Object>();
            // id
            String dataId = (String) a.get("id");
            finalItem.put("id", dataId);
            // signatures
            finalItem.put("signatures", a.get("signatures"));
            // content
            Map<String, Object> content = (Map<String, Object>) a.get("content");
            if (content == null) {
                log.warn("no content");
                return;
            }
            finalItem.put("title", content.get("title"));
            finalItem.put("authors", content.get("authors"));
            finalItem.put("keywords", content.get("keywords"));
            finalItem.put("abstract", content.get("abstract"));
            finalItem.put("pdf", content.get("pdf"));
            // 查找详情页
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            var httpGet = new HttpGet(
                    "https://api.openreview.net/notes?forum=" + finalItem.get("id") + "&limit=1000&offset=0");
            // 设置请求头信息
            httpGet.setHeader("Content-Type", "application/json; charset=utf-8");
            CloseableHttpResponse response;
            var finalNoteList = new ArrayList<Map<String, Object>>();
            try {
                response = client.execute(httpGet);
                String res = EntityUtils.toString(response.getEntity());
                Map<String, Object> resMap = new Gson().fromJson(res, Map.class);
                List<Map<String, Object>> notes = (List<Map<String, Object>>) resMap.get("notes");
                notes.forEach(note -> {
                    var map = new LinkedHashMap<String, Object>();
                    String id = (String) note.get("id");
                    if (id.equals(dataId)) {
                        return;
                    }
                    map.put("id", note.get("id"));
                    map.put("parentId", note.get("replyto"));
                    String invitation = (String) note.get("invitation");
                    if (invitation != null) {
                        String[] strings = invitation.split("/");
                        map.put("type", strings[strings.length - 1]);
                    }
                    map.put("writers", note.get("writers"));
                    map.put("content", note.get("content"));
                    // 时间
                    var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Double createTimeDb = (Double) note.get("cdate");
                    Double modifyTimeDb = (Double) note.get("mdate");
                    if (createTimeDb != null) {
                        map.put("createTime", simpleDateFormat.format(new Date(createTimeDb.longValue())));
                    }
                    if (modifyTimeDb != null) {
                        map.put("modifyTime", simpleDateFormat.format(new Date(modifyTimeDb.longValue())));
                    }
                    finalNoteList.add(map);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            finalItem.put("replies", finalNoteList);
            try {
                var document = new Document();
                finalItem.forEach(document::append);
                COLLECTION.insertOne(document);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
