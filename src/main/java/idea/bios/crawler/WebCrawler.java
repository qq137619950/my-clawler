/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package idea.bios.crawler;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.stream.Collectors;

import idea.bios.entity.CrawlerStaticsBo;
import idea.bios.crawler.exceptions.ContentFetchException;
import idea.bios.crawler.exceptions.PageBiggerThanMaxSizeException;
import idea.bios.crawler.exceptions.ParseException;
import idea.bios.fetcher.PageFetchResult;
import idea.bios.fetcher.PageFetcher;
import idea.bios.frontier.DocIDServer;
import idea.bios.frontier.Frontier;
import idea.bios.parser.HtmlParseData;
import idea.bios.parser.NotAllowedContentException;
import idea.bios.parser.ParseData;
import idea.bios.parser.Parser;
import idea.bios.robotstxt.RobotsTxtServer;
import idea.bios.url.WebURL;
import idea.bios.util.selenium.SeleniumBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.http.HttpStatus;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import javax.net.ssl.SSLException;


/**
 * WebCrawler class in the Runnable class that is executed by each crawler thread.
 * 复写此类，定制一些操作
 * @author Yasser Ganjisaffar
 */
@Slf4j
public class WebCrawler implements Runnable {
    /**
     * The id associated to the crawler thread running this instance
     */
    @Getter
    protected int myId;

    /**
     * The controller instance that has created this crawler thread. This
     * reference to the controller can be used for getting configurations of the
     * current crawl or adding new seeds during runtime.
     */
    private CrawlController myController;

    /**
     * The thread within which this crawler instance is running.
     */
    @Getter @Setter
    private Thread myThread;

    /**
     * The parser that is used by this crawler instance to parse the content of the fetched pages.
     */
    private Parser parser;

    /**
     * The fetcher that is used by this crawler instance to fetch the content of pages from the web.
     */
    @Getter
    protected PageFetcher pageFetcher;

    /**
     * The RobotsTxtServer instance that is used by this crawler instance to
     * determine whether the crawler is allowed to crawl the content of each page.
     */
    private RobotsTxtServer robotstxtServer;

    /**
     * The DocIDServer that is used by this crawler instance to map each URL to a unique docid.
     */
    private DocIDServer docIdServer;

    /**
     * The Frontier object that manages the crawl queue.
     */
    private Frontier frontier;

    /**
     * Is the current crawler instance waiting for new URLs? This field is
     * mainly used by the controller to detect whether all the crawler
     * instances are waiting for new URLs and therefore there is no more work
     * and crawling can be stopped.
     */
    @Getter
    private boolean isWaitingForNewURLs;

    private Throwable error;

    private int batchReadSize;

    /**
     * 谷歌浏览器驱动和轻量的PhantomJs驱动
     */
    @Getter
    private ChromeDriver chromeDriver;

    @Getter
    private PhantomJSDriver phantomJsDriver;

    /**
     * 数据统计
     */
    @Getter
    protected CrawlerStaticsBo staticsBo;

    @Getter
    protected String proxyInfo;

    /**
     * Initializes the current instance of the crawler
     *
     * @param id
     *            the id of this crawler instance
     * @param crawlController
     *            the controller that manages this crawling session
     */
    public void init(int id, CrawlController crawlController) {
        this.init(id, crawlController, null, null);
        if (myController.getConfig().isChromeDriver()) {
            this.chromeDriver = SeleniumBuilder.getChromeSeleniumBo().getChromeDriver();
            this.proxyInfo = SeleniumBuilder.getChromeSeleniumBo().getProxyHostAndPort();
        }
        if (myController.getConfig().isPhantomJsDriver()) {
            this.phantomJsDriver = SeleniumBuilder.getPhantomJsSeleniumBo().getPhantomJSDriver();
            this.proxyInfo = SeleniumBuilder.getChromeSeleniumBo().getProxyHostAndPort();
        }
    }

    /**
     *  线程挂了创建新的线程，能够继承一些属性
     */
    public void init(int id, CrawlController crawlController,
                     ChromeDriver chromeDriver, PhantomJSDriver phantomJsDriver) {
        this.myId = id;
        // 改为每个crawler都有一个pageFetcher
        this.pageFetcher = new PageFetcher(crawlController.getConfig());
        this.robotstxtServer = crawlController.getRobotstxtServer();
        this.docIdServer = crawlController.getDocIdServer();
        this.frontier = crawlController.getFrontier();
        this.parser = crawlController.getParser();
        this.myController = crawlController;
        this.isWaitingForNewURLs = false;
        this.batchReadSize = crawlController.getConfig().getBatchReadSize();
        this.chromeDriver = chromeDriver;
        this.phantomJsDriver = phantomJsDriver;
        this.staticsBo = new CrawlerStaticsBo();
        this.proxyInfo = this.pageFetcher.getProxyHost();
    }

    /**
     * This function is called just before starting the crawl by this crawler
     * instance. It can be used for setting up the data structures or
     * initializations needed by this crawler instance.
     * crawler线程启动函数
     */
    public void onStart() {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called just before the termination of the current
     * crawler instance. It can be used for persisting in-memory data or other
     * finalization tasks.
     */
    public void onBeforeExit() {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
        // 关闭绑定的Driver
        if(myController.getConfig().isChromeDriver()) {
            SeleniumBuilder.shutdownChromeDriver(chromeDriver);
        }
        if(myController.getConfig().isPhantomJsDriver()) {
            SeleniumBuilder.shutdownPhantomJsDriver(phantomJsDriver);
        }
    }

    /**
     * This function is called once the header of a page is fetched. It can be
     * overridden by sub-classes to perform custom logic for different status
     * codes. For example, 404 pages can be logged, etc.
     *
     * @param webUrl WebUrl containing the statusCode
     * @param statusCode Html Status Code number
     * @param statusDescription Html Status COde description
     */
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called before processing of the page's URL
     * It can be overridden by subclasses for tweaking of the url before processing it.
     * For example, http://abc.com/def?a=123 - http://abc.com/def
     *
     * @param curURL current URL which can be tweaked before processing
     * @return tweaked WebURL
     */
    protected WebURL handleUrlBeforeProcess(WebURL curURL) {
        return curURL;
    }

    /**
     * This function is called if the content of a url is bigger than allowed size.
     * @param urlStr - The URL which it's content is bigger than allowed size
     */
    protected void onPageBiggerThanMaxSize(String urlStr, long pageSize) {
        log.warn("Skipping a URL: {} which was bigger ( {} ) than max allowed size", urlStr,
                    pageSize);
    }

    /**
     * This function is called if the crawler encounters a page with a 3xx status code
     *
     * @param page Partial page object
     */
    protected void onRedirectedStatusCode(Page page) {
        //Subclasses can override this to add their custom functionality
    }

    /**
     * Emitted when the crawler is redirected to an invalid Location.
     * @param page page
     */
    protected void onRedirectedToInvalidUrl(Page page) {
        log.warn("Unexpected error, URL: {} is redirected to NOTHING",
            page.url.getURL());
    }

    /**
     * This function is called if the crawler encountered an unexpected http status code ( a
     * status code other than 3xx)
     *
     * @param urlStr URL in which an unexpected error was encountered while crawling
     * @param statusCode Html StatusCode
     * @param contentType Type of Content
     * @param description Error Description
     */
    protected void onUnexpectedStatusCode(String urlStr, int statusCode, String contentType,
                                          String description) {
        log.warn("Skipping URL: {}, StatusCode: {}, {}, {}", urlStr, statusCode, contentType,
                    description);
        // Do nothing by default (except basic logging)
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called if the content of a url could not be fetched.
     *
     * @param webUrl URL which content failed to be fetched
     *
     * @deprecated use {@link #onContentFetchError(Page)}
     */
    @Deprecated
    protected void onContentFetchError(WebURL webUrl) {
        log.warn("Can't fetch content of: {}", webUrl.getURL());
        // Do nothing by default (except basic logging)
        // Sub-classed can override this to add their custom functionality
        // 挂起一下
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.warn("InterruptedException: ", e);
        }
    }

    /**
     * This function is called if the content of a url could not be fetched.
     *
     * @param page Partial page object
     */
    protected void onContentFetchError(Page page) {
        log.warn("Can't fetch content of: {}", page.getUrl().getURL());
        // Do nothing by default (except basic logging)
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called when a unhandled exception was encountered during fetching
     *
     * @param webUrl URL where a unhandled exception occured
     */
    protected void onUnhandledException(WebURL webUrl, Throwable e) {
        if (myController.getConfig().isHaltOnError() && !(e instanceof IOException)) {
            throw new RuntimeException("unhandled exception", e);
        } else {
            String urlStr = (webUrl == null ? "NULL" : webUrl.getURL());
            log.warn("Unhandled exception while fetching {}: {}", urlStr, e.getMessage());
            log.info("Stacktrace: ", e);
            // Do nothing by default (except basic logging)
            // Sub-classed can override this to add their custom functionality
        }
        // 挂起
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            log.warn("InterruptedException: ", ex);
        }
    }

    /**
     * This function is called if there has been an error in parsing the content.
     *
     * @param webUrl URL which failed on parsing
     */
    protected void onParseError(WebURL webUrl, ParseException e) throws ParseException {
        log.error("ParseException occur.", e);
        onParseError(webUrl);
    }

    /**
     * This function is called if there has been an error in parsing the content.
     *
     * @param webUrl URL which failed on parsing
     */
    @Deprecated
    protected void onParseError(WebURL webUrl) {
        log.warn("Parsing error of: {}", webUrl.getURL());
        // Do nothing by default (Except logging)
        // Sub-classed can override this to add their custom functionality
        // 挂起
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.warn("InterruptedException: ", e);
        }
    }

    /**
     * The CrawlController instance that has created this crawler instance will
     * call this function just before terminating this crawler thread. Classes
     * that extend WebCrawler can override this function to pass their local
     * data to their controller. The controller then puts these local data in a
     * List that can then be used for processing the local data of crawlers (if needed).
     *
     * @return currently NULL
     */
    public Object getMyLocalData() {
        return null;
    }

    @Override
    public void run() {
        try {
            onStart();
            setError(null);
            boolean halt = false;
            while (!halt) {
                var assignedURLs = new ArrayList<WebURL>(batchReadSize);
                isWaitingForNewURLs = true;
                frontier.getNextURLs(batchReadSize, assignedURLs);
                isWaitingForNewURLs = false;
                if (assignedURLs.isEmpty()) {
                    if (frontier.isFinished()) {
                        // 队列中没有数据
                        return;
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        log.error("Error occurred", e);
                    }
                } else {
                    for (WebURL curURL : assignedURLs) {
                        if (myController.isShuttingDown()) {
                            log.info("Exiting because of controller shutdown.");
                            return;
                        }
                        if (curURL != null) {
                            curURL = handleUrlBeforeProcess(curURL);
                            // 处理page的函数，加入动态页面处理
                            if (myController.getConfig().isDynParse()) {
                                // attention 此时需要复写processDynPage方法
                                processDynPage(curURL);
                            } else {
                                processCommonPage(curURL);
                            }
                            frontier.setProcessed(curURL);
                        }
                    }
                }
                if (myController.getConfig().isHaltOnError() && myController.getError() != null) {
                    halt = true;
                    log.info("halting because an error has occurred on another thread");
                }
            }
        } catch (Throwable t) {
            setError(t);
        }
    }

    /**
     * Classes that extends WebCrawler should overwrite this function to tell the
     * crawler whether the given url should be crawled or not. The following
     * default implementation indicates that all urls should be included in the crawl
     * except those with a nofollow flag.
     *
     * @param url
     *            the url which we are interested to know whether it should be
     *            included in the crawl or not.
     * @param referringPage
     *           The Page in which this url was found.
     * @return if the url should be included in the crawl it returns true,
     *         otherwise false is returned.
     */
    protected boolean shouldVisit(Page referringPage, WebURL url) {
        if (myController.getConfig().isRespectNoFollow()) {
            return !((referringPage != null &&
                    referringPage.getContentType() != null &&
                    referringPage.getContentType().contains("html") &&
                    ((HtmlParseData)referringPage.getParseData())
                        .getMetaTagValue("robots")
                        .contains("nofollow")) ||
                    url.getAttribute("rel").contains("nofollow"));
        }
        return true;
    }

    /**
     * Determine whether links found at the given URL should be added to the queue for crawling.
     * By default this method returns true always, but classes that extend WebCrawler can
     * override it in order to implement particular policies about which pages should be
     * mined for outgoing links and which should not.
     *
     * If links from the URL are not being followed, then we are not operating as
     * a web crawler and need not check robots.txt before fetching the single URL.
     * (see definition at http://www.robotstxt.org/faq/what.html).  Thus URLs that
     * return false from this method will not be subject to robots.txt filtering.
     *
     * @param url the URL of the page under consideration
     * @return true if outgoing links from this page should be added to the queue.
     */
    protected boolean shouldFollowLinksIn(WebURL url) {
        return true;
    }

    /**
     * 这个函数是过滤是否加入link队列
     * 注意和上面的函数区分
     * @param url   url
     * @return      boolean
     */
    protected boolean shouldAddLinkQueue(WebURL url) {
        return true;
    }

    /**
     * Classes that extends WebCrawler should overwrite this function to process
     * the content of the fetched and parsed page.
     *
     * @param page
     *            the page object that is just fetched and parsed.
     */
    protected void visit(Page page) {
        // Do nothing by default
        // Sub-classed should override this to add their custom functionality
    }

    /**
     * 实现动态网页获取
     * @param curURL    curURL
     */
    protected void processDynPage(WebURL curURL) throws InterruptedException {
        curURL.setURL(curURL.getURL());
        curURL.setDocid(docIdServer.getNewDocId(curURL.getURL()));
        // 不在此处理links
        if (myController.getConfig().getPolitenessDelay() > 0) {
            pageFetcher.fetchNothing(curURL);
        }
        visit(new Page(curURL));
    }

    /**
     * 处理普通网页
     * @param curURL            页面URL
     * @throws ParseException   ParseException
     */
    private void processCommonPage(WebURL curURL) throws ParseException {
        PageFetchResult fetchResult = null;
        var page = new Page(curURL);
        try {
            if (curURL == null) {
                return;
            }
            // Fetch阶段控制频率
            fetchResult = pageFetcher.fetchPage(curURL);
            int statusCode = fetchResult.getStatusCode();
            // 此处处理http返回信息
            handlePageStatusCode(curURL, statusCode,
                                 EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode,
                                                                               Locale.ENGLISH));
            // Finds the status reason for all known statuses
            page.setFetchResponseHeaders(fetchResult.getResponseHeaders());
            page.setStatusCode(statusCode);
            if (statusCode < 200 || statusCode > 299) {
                // Not 2XX: 2XX status codes indicate success
                if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
                    statusCode == HttpStatus.SC_MOVED_TEMPORARILY ||
                    statusCode == HttpStatus.SC_MULTIPLE_CHOICES ||
                    statusCode == HttpStatus.SC_SEE_OTHER ||
                    statusCode == HttpStatus.SC_TEMPORARY_REDIRECT ||
                    statusCode == 308) { // is 3xx  todo
                    // follow https://issues.apache.org/jira/browse/HTTPCORE-389
                    page.setRedirect(true);

                    String movedToUrl = fetchResult.getMovedToUrl();
                    if (movedToUrl == null) {
                        onRedirectedToInvalidUrl(page);
                        return;
                    }
                    page.setRedirectedToUrl(movedToUrl);
                    onRedirectedStatusCode(page);

                    if (myController.getConfig().isFollowRedirects()) {
                        int newDocId = docIdServer.getDocId(movedToUrl);
                        if (newDocId > 0) {
                            log.debug("Redirect page: {} is already seen", curURL);
                            return;
                        }
                        var webURL = new WebURL();
                        webURL.setTldList(myController.getTldList());
                        webURL.setURL(movedToUrl);
                        webURL.setParentDocid(curURL.getParentDocid());
                        webURL.setParentUrl(curURL.getParentUrl());
                        webURL.setDepth(curURL.getDepth());
                        webURL.setDocid(-1);
                        webURL.setAnchor(curURL.getAnchor());
                        if (shouldVisit(page, webURL)) {
                            if (!shouldFollowLinksIn(webURL) || robotstxtServer.allows(webURL)) {
                                webURL.setDocid(docIdServer.getNewDocId(movedToUrl));
                                frontier.schedule(webURL);
                            } else {
                                log.debug(
                                    "Not visiting: {} as per the server's \"robots.txt\" policy",
                                    webURL.getURL());
                            }
                        } else {
                            log.debug("Not visiting: {} as per your \"shouldVisit\" policy",
                                         webURL.getURL());
                        }
                    }
                } else { // All other http codes other than 3xx & 200
                    String description =
                        EnglishReasonPhraseCatalog.INSTANCE.getReason(fetchResult.getStatusCode(),
                                                                      Locale.ENGLISH);
                    // Finds the status reason for all known statuses
                    String contentType = fetchResult.getEntity() == null ? "" :
                                         fetchResult.getEntity().getContentType() == null ? "" :
                                         fetchResult.getEntity().getContentType().getValue();
                    onUnexpectedStatusCode(curURL.getURL(), fetchResult.getStatusCode(),
                                           contentType, description);
                }
                // if status code is 200
            } else {
                if (!curURL.getURL().equals(fetchResult.getFetchedUrl())) {
                    if (docIdServer.isSeenBefore(fetchResult.getFetchedUrl())) {
                        log.debug("Redirect page: {} has already been seen", curURL);
                        return;
                    }
                    curURL.setURL(fetchResult.getFetchedUrl());
                    curURL.setDocid(docIdServer.getNewDocId(fetchResult.getFetchedUrl()));
                }
                if (!fetchResult.fetchContent(page, myController.getConfig().getMaxDownloadSize())) {
                    throw new ContentFetchException();
                }
                if (page.isTruncated()) {
                    log.warn(
                        "Warning: unknown page size exceeded max-download-size, truncated to: " +
                        "({}), at URL: {}",
                        myController.getConfig().getMaxDownloadSize(), curURL.getURL());
                }
                parser.parse(page, curURL.getURL());
                if (shouldFollowLinksIn(page.getUrl())) {
                    ParseData parseData = page.getParseData();
                    var toSchedule = new ArrayList<WebURL>();
                    int maxCrawlDepth = myController.getConfig().getMaxDepthOfCrawling();
                    /*
                     * 为了防止过多无用的url进入队列，影响效率，加入过滤过程
                     * 原因是，在这个函数上限制频率
                     */
                    Set<WebURL> links = Optional.of(parseData.getOutgoingUrls()).orElse(new HashSet<>())
                            .stream().filter(this::shouldAddLinkQueue).collect(Collectors.toSet());
                    for (WebURL webURL : links) {
                        webURL.setParentDocid(curURL.getDocid());
                        webURL.setParentUrl(curURL.getURL());
                        int newDocId = docIdServer.getDocId(webURL.getURL());
                        if (newDocId > 0) {
                            // This is not the first time that this Url is visited. So, we set the
                            // depth to a negative number.
                            webURL.setDepth((short) -1);
                            webURL.setDocid(newDocId);
                        } else {
                            webURL.setDocid(-1);
                            webURL.setDepth((short) (curURL.getDepth() + 1));
                            if ((maxCrawlDepth == -1) || (curURL.getDepth() < maxCrawlDepth)) {
                                if (shouldVisit(page, webURL)) {
                                    if (robotstxtServer.allows(webURL)) {
                                        webURL.setDocid(docIdServer.getNewDocId(webURL.getURL()));
                                        toSchedule.add(webURL);
                                    } else {
                                        log.debug(
                                            "Not visiting: {} as per the server's \"robots.txt\" " +
                                            "policy", webURL.getURL());
                                    }
                                } else {
                                    log.debug(
                                        "Not visiting: {} as per your \"shouldVisit\" policy",
                                        webURL.getURL());
                                }
                            }
                        }
                    }
                    frontier.scheduleAll(toSchedule);
                } else {
                    log.debug("Not looking for links in page {}, "
                                 + "as per your \"shouldFollowLinksInPage\" policy",
                                 page.getUrl().getURL());
                }
                boolean noIndex = myController.getConfig().isRespectNoIndex() &&
                    page.getContentType() != null &&
                    page.getContentType().contains("html") &&
                    ((HtmlParseData)page.getParseData())
                        .getMetaTagValue("robots").
                        contains("noindex");
                if (!noIndex) {
                    visit(page);
                }
            }
        } catch (PageBiggerThanMaxSizeException e) {
            onPageBiggerThanMaxSize(curURL.getURL(), e.getPageSize());
        } catch (ParseException pe) {
            onParseError(curURL, pe);
        } catch (ContentFetchException | SocketException | SSLException cfe) {
            onContentFetchError(curURL);
            onContentFetchError(page);
        } catch (NotAllowedContentException nace) {
            log.debug(
                "Skipping: {} as it contains binary content which you configured not to crawl",
                curURL.getURL());
        } catch (IOException | InterruptedException | RuntimeException e) {
            onUnhandledException(curURL, e);
        } finally {
            if (fetchResult != null) {
                fetchResult.discardContentIfNotConsumed();
            }
        }
    }

    protected synchronized Throwable getError() {
        return error;
    }

    private synchronized void setError(Throwable error) {
        this.error = error;
    }
}
