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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import idea.bios.crawler.CrawlConfig;
import idea.bios.crawler.authentication.AuthInfo;
import idea.bios.crawler.authentication.BasicAuthInfo;
import idea.bios.crawler.authentication.FormAuthInfo;
import idea.bios.crawler.authentication.NtAuthInfo;
import idea.bios.crawler.exceptions.PageBiggerThanMaxSizeException;
import idea.bios.url.URLCanonicalizer;
import idea.bios.url.WebURL;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
/**
 * @author Yasser Ganjisaffar
 */
@Slf4j
public class PageFetcher {
    protected final Object mutex = new Object();
    /**
     * This field is protected for retro compatibility. Please use the getter method: getConfig() to
     * read this field;
     */
    protected final CrawlConfig config;
    protected PoolingHttpClientConnectionManager connectionManager;
    protected CloseableHttpClient httpClient;
    protected long lastFetchTime = 0;
    protected IdleConnectionMonitorThread connectionMonitorThread = null;

    public PageFetcher(CrawlConfig config) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        this.config = config;
        RequestConfig requestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(false)
                .setCookieSpec(config.getCookiePolicy())
                .setRedirectsEnabled(false)
                .setSocketTimeout(config.getSocketTimeout())
                .setConnectTimeout(config.getConnectionTimeout())
                .build();

        RegistryBuilder<ConnectionSocketFactory> connRegistryBuilder = RegistryBuilder.create();
        connRegistryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);
        if (config.isIncludeHttpsPages()) {
            try { // Fixing: https://code.google.com/p/crawler4j/issues/detail?id=174
                // By always trusting the ssl certificate
                SSLContext sslContext =
                        SSLContexts.custom().loadTrustMaterial(null,
                                (TrustStrategy) (chain, authType) -> true).build();
                SSLConnectionSocketFactory sslsf =
                        new SniSSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
                connRegistryBuilder.register("https", sslsf);
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | RuntimeException e) {
                if (config.isHaltOnError()) {
                    throw e;
                } else {
                    log.warn("Exception thrown while trying to register https");
                    log.debug("Stacktrace", e);
                }
            }
        }

        Registry<ConnectionSocketFactory> connRegistry = connRegistryBuilder.build();
        connectionManager =
                new SniPoolingHttpClientConnectionManager(connRegistry, config.getDnsResolver());
        connectionManager.setMaxTotal(config.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.getMaxConnectionsPerHost());

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        if (config.getCookieStore() != null) {
            clientBuilder.setDefaultCookieStore(config.getCookieStore());
        }
        clientBuilder.setDefaultRequestConfig(requestConfig);
        clientBuilder.setConnectionManager(connectionManager);
        clientBuilder.setUserAgent(config.getUserAgentString());
        clientBuilder.setDefaultHeaders(config.getDefaultHeaders());

        var credentialsMap = new HashMap<AuthScope, Credentials>();
        if (config.getProxyHost() != null) {
            if (config.getProxyUsername() != null) {
                var authScope = new AuthScope(config.getProxyHost(), config.getProxyPort());
                var credentials = new UsernamePasswordCredentials(config.getProxyUsername(),
                        config.getProxyPassword());
                credentialsMap.put(authScope, credentials);
            }
            var proxy = new HttpHost(config.getProxyHost(), config.getProxyPort());
            clientBuilder.setProxy(proxy);
            log.debug("Working through Proxy: {}", proxy.getHostName());
        }
        List<AuthInfo> authInfos = config.getAuthInfos();
        if (authInfos != null) {
            for (AuthInfo authInfo : authInfos) {
                if (AuthInfo.AuthenticationType.BASIC_AUTHENTICATION.equals(authInfo.getAuthenticationType())) {
                    addBasicCredentials((BasicAuthInfo) authInfo, credentialsMap);
                } else if (AuthInfo.AuthenticationType.NT_AUTHENTICATION.equals(authInfo.getAuthenticationType())) {
                    addNtCredentials((NtAuthInfo) authInfo, credentialsMap);
                }
            }
            if (!credentialsMap.isEmpty()) {
                var credentialsProvider = new BasicCredentialsProvider();
                credentialsMap.forEach(credentialsProvider::setCredentials);
                clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                clientBuilder.addInterceptorFirst(new BasicAuthHttpRequestInterceptor());
            }
            httpClient = clientBuilder.build();
            authInfos.stream()
                    .filter(info ->
                            AuthInfo.AuthenticationType.FORM_AUTHENTICATION.equals(info.getAuthenticationType()))
                    .map(FormAuthInfo.class::cast)
                    .forEach(this::doFormLogin);
        } else {
            httpClient = clientBuilder.build();
        }
        if (connectionMonitorThread == null) {
            connectionMonitorThread = new IdleConnectionMonitorThread(connectionManager);
        }
        connectionMonitorThread.start();
    }

    /**
     * BASIC authentication<br/>
     * Official Example: https://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org
     * /apache/http/examples/client/ClientAuthentication.java
     */
    private void addBasicCredentials(BasicAuthInfo authInfo,
                                     Map<AuthScope, Credentials> credentialsMap) {
        log.info("BASIC authentication for: {}", authInfo.getLoginTarget());
        var credentials = new UsernamePasswordCredentials(authInfo.getUsername(),
                authInfo.getPassword());
        credentialsMap.put(new AuthScope(authInfo.getHost(), authInfo.getPort()), credentials);
    }

    /**
     * Do NT auth for Microsoft AD sites.
     */
    private void addNtCredentials(NtAuthInfo authInfo, Map<AuthScope, Credentials> credentialsMap) {
        log.info("NT authentication for: {}", authInfo.getLoginTarget());
        try {
            var credentials = new NTCredentials(authInfo.getUsername(),
                    authInfo.getPassword(), InetAddress.getLocalHost().getHostName(),
                    authInfo.getDomain());
            credentialsMap.put(new AuthScope(authInfo.getHost(), authInfo.getPort()), credentials);
        } catch (UnknownHostException e) {
            log.error("Error creating NT credentials", e);
        }
    }

    /**
     * FORM authentication<br/>
     * Official Example: https://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org
     * /apache/http/examples/client/ClientFormLogin.java
     */
    private void doFormLogin(FormAuthInfo authInfo) {
        log.info("FORM authentication for: {}", authInfo.getLoginTarget());
        String fullUri =
                authInfo.getProtocol() + "://" + authInfo.getHost() + ":" + authInfo.getPort() +
                        authInfo.getLoginTarget();
        var httpPost = new HttpPost(fullUri);
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(
                new BasicNameValuePair(authInfo.getUsernameFormStr(), authInfo.getUsername()));
        formParams.add(
                new BasicNameValuePair(authInfo.getPasswordFormStr(), authInfo.getPassword()));
        var entity = new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);
        try {
            httpClient.execute(httpPost);
            log.debug("Successfully request to login in with user: {} to: {}", authInfo.getUsername(),
                    authInfo.getHost());
        } catch (ClientProtocolException e) {
            log.error("While trying to login to: {} - Client protocol not supported",
                    authInfo.getHost(), e);
        } catch (IOException e) {
            log.error("While trying to login to: {} - Error making request", authInfo.getHost(),
                    e);
        }
    }

    public PageFetchResult fetchPage(WebURL webUrl)
            throws InterruptedException, IOException, PageBiggerThanMaxSizeException {
        // Getting URL, setting headers & content
        var fetchResult = new PageFetchResult(config.isHaltOnError());
        String toFetchURL = webUrl.getURL();
        HttpUriRequest request = null;
        try {
            request = newHttpUriRequest(toFetchURL);
            if (config.getPolitenessDelay() > 0) {
                // Applying Politeness delay
                synchronized (mutex) {
                    long now = (new Date()).getTime();
                    if ((now - lastFetchTime) < config.getPolitenessDelay()) {
                        Thread.sleep(config.getPolitenessDelay() - (now - lastFetchTime));
                    }
                    lastFetchTime = (new Date()).getTime();
                }
            }

            CloseableHttpResponse response = httpClient.execute(request);
            fetchResult.setEntity(response.getEntity());
            fetchResult.setResponseHeaders(response.getAllHeaders());

            // Setting HttpStatus
            int statusCode = response.getStatusLine().getStatusCode();

            // If Redirect ( 3xx )
            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
                    statusCode == HttpStatus.SC_MOVED_TEMPORARILY ||
                    statusCode == HttpStatus.SC_MULTIPLE_CHOICES ||
                    statusCode == HttpStatus.SC_SEE_OTHER ||
                    statusCode == HttpStatus.SC_TEMPORARY_REDIRECT ||
                    statusCode == 308) { // todo follow
                // https://issues.apache.org/jira/browse/HTTPCORE-389

                Header header = response.getFirstHeader(HttpHeaders.LOCATION);
                if (header != null) {
                    String movedToUrl =
                            URLCanonicalizer.getCanonicalURL(header.getValue(), toFetchURL);
                    fetchResult.setMovedToUrl(movedToUrl);
                }
            } else if (statusCode >= 200 && statusCode <= 299) {
                // is 2XX, everything looks ok
                fetchResult.setFetchedUrl(toFetchURL);
                String uri = request.getURI().toString();
                if (!uri.equals(toFetchURL)) {
                    if (!URLCanonicalizer.getCanonicalURL(uri).equals(toFetchURL)) {
                        fetchResult.setFetchedUrl(uri);
                    }
                }
                // Checking maximum size
                if (fetchResult.getEntity() != null) {
                    long size = fetchResult.getEntity().getContentLength();
                    if (size == -1) {
                        Header length = response.getLastHeader(HttpHeaders.CONTENT_LENGTH);
                        if (length == null) {
                            length = response.getLastHeader("Content-length");
                        }
                        if (length != null) {
                            size = Integer.parseInt(length.getValue());
                        }
                    }
                    if (size > config.getMaxDownloadSize()) {
                        //fix issue #52 - consume entity
                        response.close();
                        throw new PageBiggerThanMaxSizeException(size);
                    }
                }
            }
            fetchResult.setStatusCode(statusCode);
            return fetchResult;
        } finally { // occurs also with thrown exceptions
            if ((fetchResult.getEntity() == null) && (request != null)) {
                request.abort();
            }
        }
    }

    public synchronized void shutDown() {
        if (connectionMonitorThread != null) {
            connectionManager.shutdown();
            connectionMonitorThread.shutdown();
        }
    }

    /**
     * Creates a new HttpUriRequest for the given url. The default is to create a HttpGet without
     * any further configuration. Subclasses may override this method and provide their own logic.
     *
     * @param url the url to be fetched
     * @return the HttpUriRequest for the given url
     */
    protected HttpUriRequest newHttpUriRequest(String url) {
        return new HttpGet(url);
    }

    protected CrawlConfig getConfig() {
        return config;
    }
}
