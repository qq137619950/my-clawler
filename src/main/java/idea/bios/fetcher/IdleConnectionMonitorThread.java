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

package idea.bios.fetcher;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * @author 86153
 */
public class IdleConnectionMonitorThread extends Thread {

    private final PoolingHttpClientConnectionManager connMgr;
    private volatile boolean shutdown;

    private final Lock mutexLock = new ReentrantLock(false);
    private final Condition mutexCondition = mutexLock.newCondition();

    public IdleConnectionMonitorThread(PoolingHttpClientConnectionManager connMgr) {
        super("Connection Manager");
        this.connMgr = connMgr;
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                mutexLock.lock();
                try {
                    // wait(5000);
                    mutexCondition.await(5, TimeUnit.SECONDS);
                    // Close expired connections
                    connMgr.closeExpiredConnections();
                    // Optionally, close connections that have been idle longer than 30 sec
                    connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                } finally {
                    mutexLock.unlock();
                }
            }
        } catch (InterruptedException ignored) {
            // terminate
        }
    }

    public void shutdown() {
        shutdown = true;
        mutexLock.lock();
        try {
            mutexCondition.signalAll();
        } finally {
            mutexLock.unlock();
        }
//        synchronized (this) {
//            notifyAll();
//        }
    }
}