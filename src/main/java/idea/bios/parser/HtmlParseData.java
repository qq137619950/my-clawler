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

package idea.bios.parser;

import idea.bios.url.WebURL;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/**
 * @author 86153
 */
public class HtmlParseData implements ParseData {
    @Getter @Setter
    private String html;
    @Getter @Setter
    private String text;
    @Getter @Setter
    private String title;
    @Getter @Setter
    private Map<String, String> metaTags;
    private Set<WebURL> outgoingUrls;
    @Getter @Setter
    private String contentCharset;

    public String getMetaTagValue(String metaTag) {
        return metaTags.getOrDefault(metaTag, "");
    }

    @Override
    public Set<WebURL> getOutgoingUrls() {
        return outgoingUrls;
    }

    @Override
    public void setOutgoingUrls(Set<WebURL> outgoingUrls) {
        this.outgoingUrls = outgoingUrls;
    }

    @Override
    public String toString() {
        return text;
    }
}