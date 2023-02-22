package idea.bios.parser;


import idea.bios.crawler.Page;
import idea.bios.crawler.exceptions.ParseException;

/**
 * @author 86153
 */
public interface HtmlParser {
    /**
     * parse
     * @param page                  page
     * @param contextURL            contextURL
     * @return                      HtmlParseData
     * @throws ParseException       ParseException
     */
    HtmlParseData parse(Page page, String contextURL) throws ParseException;

}
