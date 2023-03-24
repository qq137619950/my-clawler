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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import idea.bios.frontier.DocIDServer;
import idea.bios.frontier.Frontier;
import idea.bios.parser.Parser;
import idea.bios.robotstxt.RobotsTxtServer;
import idea.bios.url.TLDList;
import idea.bios.url.URLCanonicalizer;
import idea.bios.url.WebURL;
import idea.bios.util.IO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import lombok.var;


/**
 * The controller that manages a crawling session. This class creates the
 * crawler threads and monitors their progress.
 * 废弃
 * @author Yasser Ganjisaffar
 */
@Slf4j
public class CrawlController {
    @Getter
    protected final CrawlConfig config;

    private static final Executor MONITOR_THREAD_EXECUTOR = Executors.newFixedThreadPool(5);
    /**
     * The 'customData' object can be used for passing custom crawl-related
     * configurations to different components of the crawler.
     */
    protected Object customData;

    /**
     * Once the crawling session finishes the controller collects the local data
     * of the crawler threads and stores them in this List.
     */
    protected List<Object> crawlersLocalData = new ArrayList<>();

    /**
     * Is the crawling of this session finished?
     */
    @Getter
    protected boolean finished;

    private Throwable error;

    /**
     * Is the crawling session set to 'shutdown'. Crawler threads monitor this
     * flag and when it is set they will no longer process new pages.
     */
    @Getter
    protected boolean shuttingDown;
    @Getter @Setter
    protected RobotsTxtServer robotstxtServer;
    @Getter @Setter
    protected Frontier frontier;
    @Getter @Setter
    protected DocIDServer docIdServer;
    @Getter
    protected TLDList tldList;

    protected final Object waitingLock = new Object();
    protected final Environment env;

    protected Parser parser;

    public CrawlController(CrawlConfig config, RobotsTxtServer robotstxtServer) throws Exception {
        this(config, null, robotstxtServer, null);
    }

    public CrawlController(CrawlConfig config, RobotsTxtServer robotstxtServer,
                           TLDList tldList) throws Exception {
        this(config, null, robotstxtServer, tldList);
    }

    public CrawlController(CrawlConfig config, Parser parser,
                           RobotsTxtServer robotstxtServer, TLDList tldList) throws Exception {
        config.validate();
        this.config = config;
        var folder = new File(config.getCrawlStorageFolder());
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                log.debug("Created folder: {}", folder.getAbsolutePath());
            } else {
                throw new Exception(
                    "couldn't create the storage folder: " + folder.getAbsolutePath() +
                    " does it already exist ?");
            }
        }

        this.tldList = tldList == null ? new TLDList(config) : tldList;
        URLCanonicalizer.setHaltOnError(config.isHaltOnError());

        boolean resumable = config.isResumableCrawling();

        // 配置队列数据库
        EnvironmentConfig envConfig = new EnvironmentConfig()
                .setAllowCreate(true)
                .setTransactional(resumable)
                .setLocking(resumable)
                .setLockTimeout(config.getDbLockTimeout(), TimeUnit.MILLISECONDS);

        var envHome = new File(config.getCrawlStorageFolder() + "/frontier");
        if (!envHome.exists()) {
            if (envHome.mkdir()) {
                log.debug("Created folder: {}", envHome.getAbsolutePath());
            } else {
                throw new Exception(
                    "Failed creating the frontier folder: " + envHome.getAbsolutePath());
            }
        }

        if (!resumable) {
            IO.deleteFolderContents(envHome);
            log.info("Deleted contents of: {}  ( as you have configured resumable crawling to false )", envHome);
        }

        env = new Environment(envHome, envConfig);
        docIdServer = new DocIDServer(env, config);
        frontier = new Frontier(env, config);

        this.parser = parser == null ? new Parser(config, tldList) : parser;
        this.robotstxtServer = robotstxtServer;
        finished = false;
        shuttingDown = false;

        robotstxtServer.setCrawlConfig(config);
    }

    public Parser getParser() {
        return parser;
    }

    @FunctionalInterface
    public interface WebCrawlerFactory<T extends WebCrawler> {
        /**
         * 返回一个实例
         * @return              T类型的实例
         * @throws Exception    Exception
         */
        T newInstance() throws Exception;
    }

    private static class SingleInstanceFactory<T extends WebCrawler>
        implements WebCrawlerFactory<T> {

        final T instance;

        SingleInstanceFactory(T instance) {
            this.instance = instance;
        }

        @Override
        public T newInstance() {
            return this.instance;
        }
    }

    private static class DefaultWebCrawlerFactory<T extends WebCrawler>
        implements WebCrawlerFactory<T> {
        final Class<T> clazz;

        DefaultWebCrawlerFactory(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T newInstance() throws Exception {
            try {
                return clazz.newInstance();
            } catch (ReflectiveOperationException e) {
                throw e;
            }
        }
    }

    /**
     * Start the crawling session and wait for it to finish.
     * This method utilizes default crawler factory that creates new crawler using Java reflection
     *
     * @param clazz
     *            the class that implements the logic for crawler threads
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void start(Class<T> clazz, int numberOfCrawlers) {
        this.start(new DefaultWebCrawlerFactory<>(clazz), numberOfCrawlers, true);
    }

    /**
     * Start the crawling session and wait for it to finish.
     * This method depends on a single instance of a crawler. Only that instance will be used for crawling.
     *
     * @param instance
     *            the instance of a class that implements the logic for crawler threads
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void start(T instance) {
        this.start(new SingleInstanceFactory<>(instance), 1, true);
    }

    /**
     * Start the crawling session and wait for it to finish.
     *
     * @param crawlerFactory
     *            factory to create crawlers on demand for each thread
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void start(WebCrawlerFactory<T> crawlerFactory,
                                             int numberOfCrawlers) {
        this.start(crawlerFactory, numberOfCrawlers, true);
    }

    /**
     * Start the crawling session and return immediately.
     *
     * @param crawlerFactory
     *            factory to create crawlers on demand for each thread
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void startNonBlocking(WebCrawlerFactory<T> crawlerFactory,
                                                        final int numberOfCrawlers) {
        this.start(crawlerFactory, numberOfCrawlers, false);
    }

    /**
     * Start the crawling session and return immediately.
     * This method utilizes default crawler factory that creates new crawler using Java reflection
     *
     * @param clazz
     *            the class that implements the logic for crawler threads
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void startNonBlocking(Class<T> clazz, int numberOfCrawlers) {
        start(new DefaultWebCrawlerFactory<>(clazz), numberOfCrawlers, false);
    }

    @Deprecated
    protected <T extends WebCrawler> void start(final WebCrawlerFactory<T> crawlerFactory,
                                                final int numberOfCrawlers, boolean isBlocking) {
        try {
            finished = false;
            setError(null);
            crawlersLocalData.clear();
            final var threads = new ArrayList<Thread>();
            final var crawlers = new ArrayList<T>();

            for (int i = 1; i <= numberOfCrawlers; i++) {
                T crawler = crawlerFactory.newInstance();
                var thread = new Thread(crawler, "Crawler " + i);
                crawler.setMyThread(thread);
                crawler.init(i, this);
                thread.start();
                crawlers.add(crawler);
                threads.add(thread);
                log.info("Crawler {} started", i);
            }
            final CrawlController controller = this;
            MONITOR_THREAD_EXECUTOR.execute(() -> {
                try {
                    synchronized (waitingLock) {
                        while (true) {
                            sleep(config.getThreadMonitoringDelaySeconds());
                            boolean someoneIsWorking = false;
                            for (int i = 0; i < threads.size(); i++) {
                                Thread thread = threads.get(i);
                                if (!thread.isAlive()) {
                                    if (!shuttingDown && !config.isHaltOnError()) {
                                        log.info("Thread {} was dead, I'll recreate it", i);
                                        T crawler = crawlerFactory.newInstance();
                                        thread = new Thread(crawler, "Crawler " + (i + 1));
                                        threads.remove(i);
                                        threads.add(i, thread);
                                        crawler.setMyThread(thread);
                                        crawler.init(i + 1, controller);
                                        thread.start();
                                        crawlers.remove(i);
                                        crawlers.add(i, crawler);
                                    }
                                } else if (!crawlers.get(i).isWaitingForNewURLs()) {
                                    someoneIsWorking = true;
                                }
                                Throwable t = crawlers.get(i).getError();
                                if (t != null && config.isHaltOnError()) {
                                    throw new RuntimeException(
                                            "error on thread [" + threads.get(i).getName() + "]", t);
                                }
                            }
                            boolean shutOnEmpty = config.isShutdownOnEmptyQueue();
                            if (!someoneIsWorking && shutOnEmpty) {
                                // Make sure again that none of the threads
                                // are
                                // alive.
                                log.info(
                                    "It looks like no thread is working, waiting for {} seconds to make sure...",
                                     config.getThreadShutdownDelaySeconds());
                                sleep(config.getThreadShutdownDelaySeconds());
                                someoneIsWorking = false;
                                for (int i = 0; i < threads.size(); i++) {
                                    Thread thread = threads.get(i);
                                    if (thread.isAlive() && !crawlers.get(i).isWaitingForNewURLs()) {
                                        someoneIsWorking = true;
                                    }
                                }
                                if (!someoneIsWorking) {
                                    if (!shuttingDown) {
                                        long queueLength = frontier.getQueueLength();
                                        if (queueLength > 0) {
                                            continue;
                                        }
                                        log.info(
                                            "No thread is working and no more URLs are in " +
                                            "queue waiting for another {}  seconds to make sure...", +
                                            config.getThreadShutdownDelaySeconds());
                                        sleep(config.getThreadShutdownDelaySeconds());
                                        queueLength = frontier.getQueueLength();
                                        if (queueLength > 0) {
                                            continue;
                                        }
                                    }
                                    log.info(
                                        "All of the crawlers are stopped. Finishing the " +
                                        "process...");
                                    // At this step, frontier notifies the threads that were
                                    // waiting for new URLs and they should stop
                                    frontier.finish();
                                    for (T crawler : crawlers) {
                                        crawler.onBeforeExit();
                                        crawlersLocalData.add(crawler.getMyLocalData());
                                        crawler.getPageFetcher().shutDown();
                                    }
                                    log.info("Waiting for {} seconds before final clean up...",
                                            config.getCleanupDelaySeconds());
                                    sleep(config.getCleanupDelaySeconds());
                                    frontier.close();
                                    docIdServer.close();
                                    finished = true;
                                    waitingLock.notifyAll();
                                    env.close();
                                    return;
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    if (config.isHaltOnError()) {
                        setError(e);
                        synchronized (waitingLock) {
                            frontier.finish();
                            frontier.close();
                            docIdServer.close();
                            waitingLock.notifyAll();
                            env.close();
                        }
                    } else {
                        log.error("Unexpected Error", e);
                    }
                }
            });
            if (isBlocking) {
                waitUntilFinish();
            }
        } catch (Exception e) {
            if (config.isHaltOnError()) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("error running the monitor thread", e);
                }
            } else {
                log.error("Error happened", e);
            }
        }
    }

    /**
     * Wait until this crawling session finishes.
     */
    public void waitUntilFinish() {
        while (!finished) {
            synchronized (waitingLock) {
                if (config.isHaltOnError()) {
                    Throwable t = getError();
                    if (t != null && config.isHaltOnError()) {
                        if (t instanceof RuntimeException) {
                            throw (RuntimeException)t;
                        } else if (t instanceof Error) {
                            throw (Error)t;
                        } else {
                            throw new RuntimeException("error on monitor thread", t);
                        }
                    }
                }
                if (finished) {
                    return;
                }
                try {
                    waitingLock.wait();
                } catch (InterruptedException e) {
                    log.error("Error occurred", e);
                }
            }
        }
    }

    /**
     * Once the crawling session finishes the controller collects the local data of the crawler
     * threads and stores them
     * in a List.
     * This function returns the reference to this list.
     *
     * @return List of Objects which are your local data
     */
    public List<Object> getCrawlersLocalData() {
        return crawlersLocalData;
    }

    protected static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ignored) {
            // Do nothing
        }
    }

    /**
     * Adds a new seed URL. A seed URL is a URL that is fetched by the crawler
     * to extract new URLs in it and follow them for crawling.
     *
     * @param pageUrl
     *            the URL of the seed
     *
     */
    public void addSeed(String pageUrl) throws IOException, InterruptedException {
        addSeed(pageUrl, -1);
    }

    /**
     * Adds a new seed URL. A seed URL is a URL that is fetched by the crawler
     * to extract new URLs in it and follow them for crawling. You can also
     * specify a specific document id to be assigned to this seed URL. This
     * document id needs to be unique. Also, note that if you add three seeds
     * with document ids 1,2, and 7. Then the next URL that is found during the
     * crawl will get a doc id of 8. Also you need to ensure to add seeds in
     * increasing order of document ids.
     *
     * Specifying doc ids is mainly useful when you have had a previous crawl
     * and have stored the results and want to start a new crawl with seeds
     * which get the same document ids as the previous crawl.
     *
     * @param pageUrl
     *            the URL of the seed
     * @param docId
     *            the document id that you want to be assigned to this seed URL.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void addSeed(String pageUrl, int docId) throws IOException, InterruptedException {
        String canonicalUrl = URLCanonicalizer.getCanonicalURL(pageUrl);
        if (canonicalUrl == null) {
            log.error("Invalid seed URL: {}", pageUrl);
        } else {
            if (docId < 0) {
                docId = docIdServer.getDocId(canonicalUrl);
                if (docId > 0) {
                    log.trace("This URL is already seen.");
                    return;
                }
                docId = docIdServer.getNewDocId(canonicalUrl);
            } else {
                try {
                    docIdServer.addUrlAndDocId(canonicalUrl, docId);
                } catch (RuntimeException e) {
                    if (config.isHaltOnError()) {
                        throw e;
                    } else {
                        log.error("Could not add seed: {}", e.getMessage());
                    }
                }
            }

            var webUrl = new WebURL();
            webUrl.setTldList(tldList);
            webUrl.setURL(canonicalUrl);
            webUrl.setDocid(docId);
            webUrl.setDepth((short) 0);
            if (robotstxtServer.allows(webUrl)) {
                frontier.schedule(webUrl);
            } else {
                // using the WARN level here, as the user specifically asked to add this seed
                log.warn("Robots.txt does not allow this seed: {}", pageUrl);
            }
        }
    }

    /**
     * This function can called to assign a specific document id to a url. This
     * feature is useful when you have had a previous crawl and have stored the
     * Urls and their associated document ids and want to have a new crawl which
     * is aware of the previously seen Urls and won't re-crawl them.
     *
     * Note that if you add three seen Urls with document ids 1,2, and 7. Then
     * the next URL that is found during the crawl will get a doc id of 8. Also
     * you need to ensure to add seen Urls in increasing order of document ids.
     *
     * @param url
     *            the URL of the page
     * @param docId
     *            the document id that you want to be assigned to this URL.
     * @throws UnsupportedEncodingException
     *
     */
    public void addSeenUrl(String url, int docId) throws UnsupportedEncodingException {
        String canonicalUrl = URLCanonicalizer.getCanonicalURL(url);
        if (canonicalUrl == null) {
            log.error("Invalid Url: {} (can't cannonicalize it!)", url);
        } else {
            try {
                docIdServer.addUrlAndDocId(canonicalUrl, docId);
            } catch (RuntimeException e) {
                if (config.isHaltOnError()) {
                    throw e;
                } else {
                    log.error("Could not add seen url: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * @deprecated implements a factory {@link WebCrawlerFactory} and inject your cutom data as
     * shown <a href="https://github.com/yasserg/crawler4j#using-a-factory">here</a> .
     */
    @Deprecated
    public Object getCustomData() {
        return customData;
    }

    /**
     * @deprecated implements a factory {@link WebCrawlerFactory} and inject your cutom data as
     * shown <a href="https://github.com/yasserg/crawler4j#using-a-factory">here</a> .
     */

    @Deprecated
    public void setCustomData(Object customData) {
        this.customData = customData;
    }

    /**
     * Set the current crawling session set to 'shutdown'. Crawler threads
     * monitor the shutdown flag and when it is set to true, they will no longer
     * process new pages.
     */
    public void shutdown() {
        log.info("Shutting down...");
        this.shuttingDown = true;
        frontier.finish();
    }
    protected synchronized Throwable getError() {
        return error;
    }

    private synchronized void setError(Throwable e) {
        this.error = e;
    }
}
