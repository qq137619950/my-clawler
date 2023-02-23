package idea.bios.crawler.my;


import com.drew.lang.annotations.NotNull;
import com.mongodb.client.MongoCollection;
import idea.bios.crawler.Page;
import idea.bios.crawler.WebCrawler;
import idea.bios.datasource.mongodb.MongoDb;
import idea.bios.parser.HtmlParseData;
import idea.bios.url.WebURL;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.bson.Document;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 通用Crawler抽象类
 * @author 86153
 */
@Slf4j
public abstract class AbsCommonCrawler extends WebCrawler {
    protected final static Pattern COMMON_FILTERS = Pattern.compile(
            ".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf|rm|smil|wmv|swf|wma|zip|rar|gz|bmp|gif|jpeg|png|))$");

    /**
     * 最终的处理HTML页面函数
     * 此时网页最终解析成了HTML格式
     * @param html   html网页代码
     */
    protected abstract Map<String, ?> getSingleHtmlInfo(String html);

    /**
     * 处理 page，可以封装成统一的逻辑，比如落库等
     * This function is called when a page is fetched and ready
     * @param page  page
     */
    @Override
    public abstract void visit(Page page);

    /**
     * 是否加入队列
     * @param referringPage page
     * @param url           url
     */
    @Override
    public abstract boolean shouldVisit(Page referringPage, WebURL url);

    /**
     * Determine whether links found at the given URL should be added to the queue for crawling.
     * @param url the URL of the page under consideration
     * @return boolean
     */
    @Override
    protected boolean shouldFollowLinksIn(WebURL url) {
        return true;
    }

    /**
     * 启动器
     * @throws Exception 异常
     */
    public abstract void runner() throws Exception;

    /**
     * 页面是否需要Parse
     * 一些网页需要Fetch但是不需要解析
     * @param url url
     */
    protected boolean shouldParse(WebURL url) {
        return true;
    }

    /**
     * 处理page的通用方法
     * @param page  page
     * @param cName collection name
     */
    protected void commonPageVisit(@NotNull Page page, String cName) {
        String url = page.getUrl().getURL();
        if (!shouldParse(page.getUrl())) {
            log.info("this page should not be parse: {}", url);
            return;
        }
        log.info("visit URL: {}", url);
        // 此处处理HTML格式的data
        if (page.getParseData() instanceof HtmlParseData) {
            var htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();
            // 解析html
            Map<String, ?> result = this.getSingleHtmlInfo(html);
            // 写数据库
            if (result == null) {
                log.warn("get nothing from html.");
                return;
            }
            // 将格式化的数据写入MongoDb
            MongoCollection<Document> collection = new MongoDb()
                    .getCrawlerDataCollection(cName);
            var insertDoc = new Document();;
            result.forEach(insertDoc::append);
            // 页面url也记录下
            insertDoc.append("site", url);
            // 根据site生成唯一的docId
            UUID uuid = UUID.nameUUIDFromBytes(url.getBytes());
            insertDoc.append("docId", uuid.toString());
            for (Object o : result.values()) {
                if (o == null || "".equals(o)) {
                    log.warn("some content empty.");
                    return;
                }
            }
            collection.insertOne(insertDoc);
        }
    }

    /**
     * 特殊处理page的通用方法
     * @param page  page
     * @param cName collection name
     */
    protected void specialPageVisit(@NotNull Page page, String cName, String charset) {
        String url = page.getUrl().getURL();
        if (!shouldParse(page.getUrl())) {
            log.info("this page should not be parse: {}", url);
            return;
        }
        log.info("special visit URL: {}", url);
        // 重新获取
        Connection connection = Jsoup.connect(url);
        Connection.Response res;
        try {
            res = connection.execute().charset(charset);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (res.statusCode() == 200) {
            String html = res.body();
//            try {
//                byte[] bytes = html.getBytes(charset);
//                html = new String(bytes, StandardCharsets.UTF_8);
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
            // 解析html
            Map<String, ?> result = this.getSingleHtmlInfo(html);
            // 写数据库
            if (result == null) {
                log.warn("get nothing from html.");
                return;
            }
            MongoCollection<Document> collection = new MongoDb()
                    .getCrawlerDataCollection(cName);
            var insertDoc = new Document();;
            result.forEach(insertDoc::append);
            // url也记录下
            insertDoc.append("src", url);
            for (Object o : result.values()) {
                if (o == null || "".equals(o)) {
                    log.warn("some content empty.");
                    return;
                }
            }
            collection.insertOne(insertDoc);
        }
    }

    /**
     * 适合一个网站获取多种结构化数据的场景
     * @param page      page
     * @param switchByUrlRule  通过规则选择对应的db college
     * @param chMap     K: db collection name  V: 对应的方法，参考 this.getHtmlInfo(html)
     */
    protected void multiPageVisit(@NotNull Page page,
                                  Function<String, String> switchByUrlRule,
                                  Map<String, Function<String, Map<String, ?>>> chMap) {
        String url = page.getUrl().getURL();
        log.info("visit URL: {}", url);
        // 通过规则判断爬取类型
        String cName = switchByUrlRule.apply(url);
        if (cName == null || cName.isEmpty()) {
            log.warn("no type defined in switchByUrlRule.");
            return;
        }
        // 获取html处理方法
        if (page.getParseData() instanceof HtmlParseData) {
            Function<String, Map<String, ?>> function = chMap.get(cName);
            if (function == null) {
                log.warn("no function defined in chMap.");
                return;
            }
            var htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();
            Map<String, ?> result = function.apply(html);
            // 写数据库
            if (result == null) {
                log.warn("get nothing from html.");
                return;
            }
            MongoCollection<Document> collection = new MongoDb()
                    .getCrawlerDataCollection(cName);
            var insertDoc = new Document();
            result.forEach(insertDoc::append);
            // url也记录下
            insertDoc.append("src", url);
            for (Object o : result.values()) {
                if (o == null || "".equals(o)) {
                    log.warn("some content empty.");
                    return;
                }
            }
            collection.insertOne(insertDoc);
        }
    }

    /**
     * 带过滤的page处理
     * @param page      page
     * @param cName     collection name
     * @param func      filter function
     */
    protected void filteredPageVisit(@NotNull Page page, String cName,
                                     Function<Map<String, ?>, Boolean> func) {
        String url = page.getUrl().getURL();
        if (!shouldParse(page.getUrl())) {
            log.info("this page should not be parse: {}", url);
            return;
        }
        log.info("visit URL: {}", url);
        if (page.getParseData() instanceof HtmlParseData) {
            var htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();
            // 解析html
            Map<String, ?> result = this.getSingleHtmlInfo(html);
            // 写数据库
            if (result == null) {
                log.warn("get nothing from html.");
                return;
            }
            // 不符合欧过滤规则
            if (!func.apply(result)) {
                log.warn("not applying filter rule.");
                return;
            }
            MongoCollection<Document> collection = new MongoDb()
                    .getCrawlerDataCollection(cName);
            var insertDoc = new Document();
            result.forEach(insertDoc::append);
            // url也记录下
            insertDoc.append("src", url);
            for (Object o : result.values()) {
                if (o == null || "".equals(o)) {
                    log.warn("some content empty.");
                    return;
                }
            }
            collection.insertOne(insertDoc);
        }

    }

    /**
     * 构建知识图谱的数据
     * 爬取的对象必须是有效的三元组
     * @param page  page
     * @param cName collection name
     */
    protected void graphPageVisit(Page page, String cName) {
        String url = page.getUrl().getURL();
        if (!shouldParse(page.getUrl())) {
            log.info("this page should not be parse: {}", url);
            return;
        }
    }

    /**
     * 校验url解析
     * @param url   url
     */
    public void testGetHtmlInfo(String url) throws IOException {
        if (url == null || !url.startsWith("http")) {
            log.warn("url format error！ url:{}", url);
            return;
        }
        // Connection connection = HttpConnection.connect(url);
        Connection connection = Jsoup.connect(url);
        // connection.validateTLSCertificates(false);
        Connection.Response res = connection.execute();
        if (res.statusCode() != 200) {
            log.warn("res error！ code:{}", res.statusCode());
            return;
        }
        // 输出
        log.info(this.getSingleHtmlInfo(res.body()).toString());
    }

//    public void testGetHtmlInfoWithJs(String url) throws IOException, InterruptedException {
//        // 输出
//        log.info(this.getSingleHtmlInfo(HtmlUtils.getDocumentWithJs(url)).toString());
//    }

}
