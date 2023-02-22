package idea.bios.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import idea.bios.crawler.CrawlConfig;
import idea.bios.url.TLDList;
import idea.bios.url.WebURL;
import lombok.var;


/**
 *
 * @author Avi Hayun
 * @date 9/22/2014
 * Net related Utils
 */
public class Net {

    private TLDList tldList;

    private final Function<Url, WebURL> urlMapper = url -> {
        WebURL webUrl = new WebURL();
        webUrl.setTldList(tldList);
        webUrl.setURL(url.getFullUrl());
        return webUrl;
    };

    private final CrawlConfig config;

    public Net(CrawlConfig config, TLDList tldList) {
        this.config = config;
        this.tldList = tldList;
    }

    public Set<WebURL> extractUrls(String input) {
        if (input == null) {
            return Collections.emptySet();
        } else {
            var detector = new UrlDetector(input, getOptions());
            List<Url> urls = detector.detect();
            return urls.stream().map(urlMapper).collect(Collectors.toSet());
        }
    }

    private UrlDetectorOptions getOptions() {
        if (config.isAllowSingleLevelDomain()) {
            return UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN;
        } else {
            return UrlDetectorOptions.Default;
        }
    }

}