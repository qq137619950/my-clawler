package idea.bios.crawler.exceptions;

import lombok.Getter;

/**
 *
 * @author Avi Hayun
 * @date 12/8/2014
 * Thrown when trying to fetch a page which is bigger than allowed size
 */
public class PageBiggerThanMaxSizeException extends Exception {
    @Getter
    long pageSize;

    public PageBiggerThanMaxSizeException(long pageSize) {
        super("Aborted fetching of this URL as it's size ( " + pageSize +
              " ) exceeds the maximum size");
        this.pageSize = pageSize;
    }
}