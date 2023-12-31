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

package idea.bios.frontier;

import idea.bios.crawler.CrawlConfig;
import idea.bios.url.WebURL;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Yasser Ganjisaffar
 */
@Slf4j
public class Frontier {
    private static final String DATABASE_NAME = "PendingURLsDB";
    private static final int IN_PROCESS_RESCHEDULE_BATCH_SIZE = 100;
    private final CrawlConfig config;

    protected WorkQueues workQueues;
    protected InProcessPagesDB inProcessPages;

    // protected final Object mutex = new Object();
    // protected final Object waitingList = new Object();
    protected Lock mutexLock = new ReentrantLock(false);

    protected Lock waitingListLock = new ReentrantLock(true);
    protected Condition waitingLockCondition = waitingListLock.newCondition();

    protected boolean isFinished = false;

    protected long scheduledPages;

    protected Counters counters;

    public Frontier(Environment env, CrawlConfig config) {
        this.config = config;
        this.counters = new Counters(env, config);
        try {
            workQueues = new WorkQueues(env, DATABASE_NAME, config.isResumableCrawling());
            if (config.isResumableCrawling()) {
                scheduledPages = counters.getValue(Counters.ReservedCounterNames.SCHEDULED_PAGES);
                inProcessPages = new InProcessPagesDB(env);
                long numPreviouslyInProcessPages = inProcessPages.getLength();
                if (numPreviouslyInProcessPages > 0) {
                    log.info("Rescheduling {} URLs from previous crawl.",
                                numPreviouslyInProcessPages);
                    scheduledPages -= numPreviouslyInProcessPages;

                    List<WebURL> urls = inProcessPages.get(IN_PROCESS_RESCHEDULE_BATCH_SIZE);
                    while (!urls.isEmpty()) {
                        scheduleAll(urls);
                        inProcessPages.delete(urls.size());
                        urls = inProcessPages.get(IN_PROCESS_RESCHEDULE_BATCH_SIZE);
                    }
                }
            } else {
                inProcessPages = null;
                scheduledPages = 0;
            }
        } catch (DatabaseException e) {
            log.error("Error while initializing the Frontier", e);
            workQueues = null;
        }
    }

    /**
     * 批量插入
     * @param urls  urls
     */
    public void scheduleAll(List<WebURL> urls) {
        // Maximum number of pages to fetch For unlimited number of pages
        int maxPagesToFetch = config.getMaxPagesToFetch();
        mutexLock.lock();
        try {
            int newScheduledPage = 0;
            for (WebURL url : urls) {
                if ((maxPagesToFetch > 0) &&
                    ((scheduledPages + newScheduledPage) >= maxPagesToFetch)) {
                    log.warn("大于最大配置 maxPage:{}", maxPagesToFetch);
                    break;
                }
                try {
                    workQueues.put(url);
                    newScheduledPage++;
                } catch (DatabaseException e) {
                    log.error("Error while putting the url in the work queue", e);
                }
            }
            if (newScheduledPage > 0) {
                scheduledPages += newScheduledPage;
                counters.increment(Counters.ReservedCounterNames.SCHEDULED_PAGES, newScheduledPage);
            }
            waitingListLock.lock();
            try {
                waitingLockCondition.signalAll();
            } finally {
                waitingListLock.unlock();
            }
//            synchronized (waitingList) {
//                waitingList.notifyAll();
//            }
        } finally {
            mutexLock.unlock();
        }
    }

    /**
     * 单个插入
     * @param url   url
     */
    public void schedule(WebURL url) {
        int maxPagesToFetch = config.getMaxPagesToFetch();
        mutexLock.lock();
        try {
            if (maxPagesToFetch < 0 || scheduledPages < maxPagesToFetch) {
                workQueues.put(url);
                scheduledPages++;
                counters.increment(Counters.ReservedCounterNames.SCHEDULED_PAGES);
            }
        } catch (DatabaseException e) {
            log.error("Error while putting the url in the work queue", e);
        } finally {
            mutexLock.unlock();
        }
    }

    /**
     * 获取下一个url
     * @param max       number of pages to fetch/process from the database in a single read
     *                  默认 50
     * @param result    result
     */
    public void getNextURLs(int max, List<WebURL> result) {
        while (true) {
            mutexLock.lock();
            try {
                if (isFinished) {
                    return;
                }
                try {
                    List<WebURL> curResults = workQueues.get(max);
                    workQueues.delete(curResults.size());
                    if (inProcessPages != null) {
                        curResults.forEach(res -> inProcessPages.put(res));
                    }
                    result.addAll(curResults);
                } catch (DatabaseException e) {
                    log.error("Error while getting next urls", e);
                }
                if (result.size() > 0) {
                    return;
                }
            } finally {
                mutexLock.unlock();
            }
            waitingListLock.lock();
            try {
                // 等待 scheduleAll 进行完毕
                waitingLockCondition.signalAll();
//                synchronized (waitingList) {
//                    waitingList.wait();
//                }
            } finally {
                waitingListLock.unlock();
            }
            if (isFinished) {
                return;
            }
        }
    }

    public void setProcessed(WebURL webURL) {
        counters.increment(Counters.ReservedCounterNames.PROCESSED_PAGES);
        if (inProcessPages != null) {
            if (!inProcessPages.removeURL(webURL)) {
                log.warn("Could not remove: {} from list of processed pages.", webURL.getURL());
            }
        }
    }

    /**
     * 需要扫描，比较耗时
     * @return  length
     */
    public long getQueueLength() {
        return workQueues.getLength();
    }

    public long getNumberOfAssignedPages() {
        if (inProcessPages != null) {
            return inProcessPages.getLength();
        } else {
            return 0;
        }
    }

    public long getNumberOfProcessedPages() {
        return counters.getValue(Counters.ReservedCounterNames.PROCESSED_PAGES);
    }

    public long getNumberOfScheduledPages() {
        return counters.getValue(Counters.ReservedCounterNames.SCHEDULED_PAGES);
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void close() {
        workQueues.close();
        counters.close();
        if (inProcessPages != null) {
            inProcessPages.close();
        }
    }

    public void finish() {
        isFinished = true;
        waitingListLock.lock();
        try {
            waitingLockCondition.signalAll();
        } finally {
            waitingListLock.unlock();
        }
//        synchronized (waitingList) {
//            waitingList.notifyAll();
//        }
    }
}
