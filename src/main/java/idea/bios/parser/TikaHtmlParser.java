package idea.bios.parser;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import idea.bios.crawler.CrawlConfig;
import idea.bios.crawler.Page;
import idea.bios.crawler.exceptions.ParseException;
import idea.bios.url.TLDList;
import idea.bios.url.URLCanonicalizer;
import idea.bios.url.WebURL;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.html.HtmlParser;

/**
 * @author 86153
 */
@Slf4j
public class TikaHtmlParser implements idea.bios.parser.HtmlParser {
    private final CrawlConfig config;
    private final TLDList tldList;

    private final HtmlParser htmlParser;
    private final ParseContext parseContext;

    public TikaHtmlParser(CrawlConfig config, TLDList tldList) throws InstantiationException, IllegalAccessException {
        this.config = config;
        this.tldList = tldList;
        htmlParser = new HtmlParser();
        parseContext = new ParseContext();
        parseContext.set(HtmlMapper.class, AllTagMapper.class.newInstance());
    }

    @Override
    public HtmlParseData parse(Page page, String contextURL) throws ParseException {
        var parsedData = new HtmlParseData();
        var contentHandler = new HtmlContentHandler();
        var metadata = new Metadata();

        if (page.getContentType() != null) {
            metadata.add(Metadata.CONTENT_TYPE, page.getContentType());
        }

        try (var inputStream = new ByteArrayInputStream(page.getContentData())) {
            htmlParser.parse(inputStream, contentHandler, metadata, parseContext);
        } catch (Exception e) {
            log.error("{}, while parsing: {}", e.getMessage(), page.getUrl().getURL());
            throw new ParseException("could not parse [" + page.getUrl().getURL() + "]", e);
        }

        String contentCharset = chooseEncoding(page, metadata);
        parsedData.setContentCharset(contentCharset);

        parsedData.setText(contentHandler.getBodyText().trim());
        parsedData.setTitle(metadata.get(DublinCore.TITLE));
        parsedData.setMetaTags(contentHandler.getMetaTags());

        try {
            Set<WebURL> outgoingUrls = getOutgoingUrls(contextURL, contentHandler, contentCharset);
            parsedData.setOutgoingUrls(outgoingUrls);
            if (page.getContentCharset() == null) {
                parsedData.setHtml(new String(page.getContentData()));
            } else {
                parsedData.setHtml(new String(page.getContentData(), page.getContentCharset()));
            }
            return parsedData;
        } catch (UnsupportedEncodingException e) {
            log.error("error parsing the html: {}", page.getUrl().getURL(), e);
            throw new ParseException("could not parse [" + page.getUrl().getURL() + "]", e);
        }

    }

    private Set<WebURL> getOutgoingUrls(String contextURL, HtmlContentHandler contentHandler, String contentCharset)
            throws UnsupportedEncodingException {
        Set<WebURL> outgoingUrls = new HashSet<>();

        String baseURL = contentHandler.getBaseUrl();
        if (baseURL != null) {
            contextURL = baseURL;
        }

        int urlCount = 0;
        for (ExtractedUrlAnchorPair urlAnchorPair : contentHandler.getOutgoingUrls()) {

            String href = urlAnchorPair.getHref();
            if ((href == null) || href.trim().isEmpty()) {
                continue;
            }

            String hrefLoweredCase = href.trim().toLowerCase();
            if (!hrefLoweredCase.contains("javascript:") &&
                    !hrefLoweredCase.contains("mailto:") && !hrefLoweredCase.contains("@")) {
                // Prefer page's content charset to encode href url
                Charset hrefCharset = ((contentCharset == null) || contentCharset.isEmpty()) ?
                        StandardCharsets.UTF_8 : Charset.forName(contentCharset);
                String url = URLCanonicalizer.getCanonicalURL(href, contextURL, hrefCharset);
                if (url != null) {
                    var webURL = new WebURL();
                    webURL.setTldList(tldList);
                    webURL.setURL(url);
                    webURL.setTag(urlAnchorPair.getTag());
                    webURL.setAnchor(urlAnchorPair.getAnchor());
                    webURL.setAttributes(urlAnchorPair.getAttributes());
                    outgoingUrls.add(webURL);
                    urlCount++;
                    if (urlCount > config.getMaxOutgoingLinksToFollow()) {
                        break;
                    }
                }
            }
        }
        return outgoingUrls;
    }

    private String chooseEncoding(Page page, Metadata metadata) {
        String pageCharset = page.getContentCharset();
        if (pageCharset == null || pageCharset.isEmpty()) {
            return metadata.get("Content-Encoding");
        }
        return pageCharset;
    }
}
