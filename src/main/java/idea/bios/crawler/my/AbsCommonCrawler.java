package idea.bios.crawler.my;


import com.mongodb.client.MongoCollection;
import idea.bios.config.SiteConfig;
import idea.bios.crawler.Page;
import idea.bios.crawler.WebCrawler;
import idea.bios.crawler.my.controller.ControllerFacade;
import idea.bios.crawler.my.seed.SeedFetcher;
import idea.bios.crawler.my.seed.SeedFetcherImpl;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 通用Crawler抽象类
 * @author 86153
 */
@Slf4j
public abstract class AbsCommonCrawler extends WebCrawler {
    /**
     * 每一个Crawler操作controller的接口
     */
    protected ControllerFacade controllerFacade;
    /**
     * 一个计数标记位
     */
    protected static final AtomicInteger INT_FLAG = new AtomicInteger(0);
    /**
     * seed获取接口
     */
    protected final SeedFetcher seedFetcher = new SeedFetcherImpl();

    protected final static Pattern COMMON_FILTERS = Pattern.compile(
            ".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf|rm|smil|wmv|swf|wma|zip|rar|gz|bmp|gif|jpeg|png|))$");

    public AbsCommonCrawler(ControllerFacade controllerFacade) {
        this.controllerFacade = controllerFacade;
    }

    /**
     * 处理 page，可以封装成统一的逻辑，比如落库、使用PhantomJs Driver对网页处理等
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
     * 这个方法用来控制是否需要加入link，但是此时已经耗时。
     * 可以用shouldAddLinkQueue替代
     * @see #shouldAddLinkQueue
     * @param url the URL of the page under consideration
     * @return boolean
     */
    @Override
    @Deprecated
    protected boolean shouldFollowLinksIn(WebURL url) {
        return true;
    }

    /**
     * 这个函数是过滤是否加入link队列
     * 注意和上面的函数区分
     * @param url   url
     * @return      boolean
     */
    @Override
    protected boolean shouldAddLinkQueue(WebURL url) {
        return true;
    }

    /**
     * 页面是否需要Parse
     * 一些网页需要Fetch但是不需要解析
     * @param url url
     * @return should parse
     */
    protected abstract boolean shouldParse(WebURL url);

    /**
     * 启动器准备
     * public abstract void runner() throws Exception;
     */
    public abstract void prepareToRun();


    protected void driverPageVisit(WebURL url, Supplier<Map<String, Object>> mapSupplier) {
        if (url == null || !shouldParse(url)) {
            log.info("this page should not be parse: {}", url);
            return;
        }
        // 写mongodb
        MongoCollection<Document> collection = new MongoDb()
                .getCrawlerDataCollection(SiteConfig.getCurSite().getSourceId());
        Map<String, Object> res;
        try {
            res = mapSupplier.get();
        } catch (Exception e) {
            log.warn("Exception:", e);
            return;
        }
        // 增加页面获取统计
        staticsBo.increaseVisitNum();
        // TODO 是否允许空值
        for (Object o : res.values()) {
            if (o == null || "".equals(o)) {
                log.warn("some content empty.");
                return;
            }
        }
        var insertDoc = new Document();
        res.forEach(insertDoc::append);
        // 页面url也记录下
        insertDoc.append("site", url.getURL());
        // 根据site生成唯一的docId
        UUID uuid = UUID.nameUUIDFromBytes(url.getURL().getBytes());
        insertDoc.append("docId", uuid.toString());
        collection.insertOne(insertDoc);
    }

    /**
     * 处理page的通用方法，处理已经解析好的html文本
     * @param page  page
     */
    protected void commonHtmlPageVisit(Page page, Function<String, Map<String, Object>> mapSupplier) {
        String url = page.getUrl().getURL();
        if (!shouldParse(page.getUrl())) {
            log.info("this page should not be parse: {}", url);
            return;
        }
        log.info("common html page visit URL: {}", url);
        // 此处处理HTML格式的data
        if (page.getParseData() instanceof HtmlParseData) {
            var htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();
            // 解析html
            Map<String, ?> result = mapSupplier.apply(html);
            // 增加页面获取统计
            staticsBo.increaseVisitNum();
            // 写数据库
            if (result == null) {
                log.warn("get nothing from html.");
                return;
            }
            // 将格式化的数据写入MongoDb
            MongoCollection<Document> collection = new MongoDb()
                    .getCrawlerDataCollection(SiteConfig.getCurSite().getSourceId());
            var insertDoc = new Document();
            result.forEach(insertDoc::append);
            // 页面url也记录下
            insertDoc.append("site", url);
            // 根据site生成唯一的docId
            UUID uuid = UUID.nameUUIDFromBytes(url.getBytes());
            insertDoc.append("docId", uuid.toString());
            // TODO 是否允许空值
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
    protected void multiPageVisit(Page page,
                                  Function<String, String> switchByUrlRule,
                                  Map<String, Function<String, Map<String, ?>>> chMap) {
        if (!shouldParse(page.getUrl())) {
            log.info("this page should not be parse: {}", page.getUrl());
            return;
        }
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
            // 增加页面获取统计
            staticsBo.increaseVisitNum();
            // 写数据库
            if (result == null) {
                log.warn("get nothing from html.");
                return;
            }
            MongoCollection<Document> collection = new MongoDb()
                    .getCrawlerDataCollection(cName);
            var insertDoc = new Document();
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
        // log.info(this.getSingleHtmlInfo(res.body()).toString());
    }

//    public void testGetHtmlInfoWithJs(String url) throws IOException, InterruptedException {
//        // 输出
//        log.info(this.getSingleHtmlInfo(HtmlUtils.getDocumentWithJs(url)).toString());
//    }

}
