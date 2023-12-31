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
import idea.bios.util.Util;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.OperationStatus;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 文档ID Server
 * TODO 使用roaring bitmap进行过滤，减少IO
 * @author 86153
 */

@Slf4j
public class DocIDServer {
    private final Database docIDsDB;
    private static final String DATABASE_NAME = "DocIDs";

    // private final Object mutex = new Object();
    protected Lock mutexLock = new ReentrantLock(false);

    private final CrawlConfig config;
    private int lastDocId;

    public DocIDServer(Environment env, CrawlConfig config) {
        this.config = config;
        var dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(config.isResumableCrawling());
        dbConfig.setDeferredWrite(!config.isResumableCrawling());
        lastDocId = 0;
        docIDsDB = env.openDatabase(null, DATABASE_NAME, dbConfig);
        if (config.isResumableCrawling()) {
            int docCount = getDocCount();
            if (docCount > 0) {
                log.info("Loaded {} URLs that had been detected in previous crawl.",
                        docCount);
                lastDocId = docCount;
            }
        }
    }

    /**
     * Returns the docId of an already seen url.
     *
     * @param url the URL for which the docId is returned.
     * @return the docId of the url if it is seen before. Otherwise -1 is returned.
     */
    public int getDocId(String url) {
        mutexLock.lock();
        try {
            OperationStatus result;
            var value = new DatabaseEntry();
            try {
                var key = new DatabaseEntry(url.getBytes());
                result = docIDsDB.get(null, key, value, null);
            } catch (RuntimeException e) {
                if (config.isHaltOnError()) {
                    throw e;
                } else {
                    log.error("Exception thrown while getting DocID", e);
                    return -1;
                }
            }

            if ((result == OperationStatus.SUCCESS) && (value.getData().length > 0)) {
                return Util.byteArray2Int(value.getData());
            }
            return -1;
        } finally {
            mutexLock.unlock();
        }
    }

    public int getNewDocId(String url) {
        mutexLock.lock();
        try {
            try {
                // Make sure that we have not already assigned a docId for this URL
                int docId = getDocId(url);
                if (docId > 0) {
                    return docId;
                }
                ++lastDocId;
                docIDsDB.put(null, new DatabaseEntry(url.getBytes()),
                             new DatabaseEntry(Util.int2ByteArray(lastDocId)));
                return lastDocId;
            } catch (RuntimeException e) {
                if (config.isHaltOnError()) {
                    throw e;
                } else {
                    log.error("Exception thrown while getting new DocID", e);
                    return -1;
                }
            }
        } finally {
            mutexLock.unlock();
        }
    }

    public void addUrlAndDocId(String url, int docId) {
        mutexLock.lock();
        try {
            if (docId <= lastDocId) {
                throw new IllegalArgumentException(
                    "Requested doc id: " + docId + " is not larger than: " + lastDocId);
            }
            // Make sure that we have not already assigned a docId for this URL
            int prevDocId = getDocId(url);
            if (prevDocId > 0) {
                if (prevDocId == docId) {
                    return;
                }
                throw new IllegalArgumentException("Doc id: " + prevDocId + " is already assigned to URL: " + url);
            }

            docIDsDB.put(null, new DatabaseEntry(url.getBytes()),
                         new DatabaseEntry(Util.int2ByteArray(docId)));
            lastDocId = docId;
        } finally {
            mutexLock.unlock();
        }
    }

    public boolean isSeenBefore(String url) {
        return getDocId(url) != -1;
    }

    public final int getDocCount() {
        try {
            return (int) docIDsDB.count();
        } catch (DatabaseException e) {
            log.error("Exception thrown while getting DOC Count", e);
            return -1;
        }
    }

    public void close() {
        try {
            docIDsDB.close();
        } catch (DatabaseException e) {
            log.error("Exception thrown while closing DocIDServer", e);
        }
    }
}