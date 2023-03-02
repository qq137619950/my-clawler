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

import java.util.Set;

/**
 * @author 86153
 */
public interface ParseData {
    /**
     * 获取链接集合
     * @return  链接
     */
    Set<WebURL> getOutgoingUrls();

    /**
     * 设置链接集合
     * @param outgoingUrls  链接
     */
    void setOutgoingUrls(Set<WebURL> outgoingUrls);

    /**
     * toString
     * @return  String
     */
    @Override
    String toString();
}