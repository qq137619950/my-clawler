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

import java.io.IOException;
import java.net.SocketTimeoutException;

import idea.bios.crawler.Page;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

/**
 * @author Yasser Ganjisaffar
 */
@Slf4j
public class PageFetchResult {
    private final boolean haltOnError;
    @Getter @Setter
    protected int statusCode;
    @Getter @Setter
    protected HttpEntity entity = null;
    @Getter @Setter
    protected Header[] responseHeaders = null;
    @Getter @Setter
    protected String fetchedUrl = null;
    @Getter @Setter
    protected String movedToUrl = null;

    public PageFetchResult(boolean haltOnError) {
        this.haltOnError = haltOnError;
    }

    public boolean fetchContent(Page page, int maxBytes) throws IOException {
        try {
            page.setFetchResponseHeaders(responseHeaders);
            page.load(entity, maxBytes);
            return true;
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            if (haltOnError) {
                throw e;
            } else {
                log.info("Exception while fetching content for: {} [{}]", page.getUrl().getURL(),
                            e.getMessage());
            }
        }
        return false;
    }

    public void discardContentIfNotConsumed() {
        try {
            if (entity != null) {
                EntityUtils.consume(entity);
            }
        } catch (IOException ignored) {
            // We can EOFException (extends IOException) exception. It can happen on compressed
            // streams which are not
            // repeatable
            // We can ignore this exception. It can happen if the stream is closed.
        } catch (RuntimeException e) {
            if (haltOnError) {
                throw e;
            } else {
                log.warn("Unexpected error occurred while trying to discard content", e);
            }
        }
    }
}
