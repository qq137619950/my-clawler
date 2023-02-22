package idea.bios.crawler.exceptions;

/**
 *
 * @author Avi Hayun
 * @date 12/8/2014
 *
 * Thrown when there is a problem with the parsing of the content - this is a tagging exception
 */
public class ParseException extends Exception {

    public ParseException() {
        super();
    }

    public ParseException(Throwable cause) {
        super(cause);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

}