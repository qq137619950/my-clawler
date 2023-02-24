package idea.bios.util.selenium;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 86153
 */
public class SeleniumHystrix extends HystrixCommand<List<String>> {

    private final String url;

    public SeleniumHystrix(HystrixCommandGroupKey group, String url) {
        super(group);
        this.url = url;
    }

    @Override
    protected List<String> run() throws Exception {
        return SeleniumUtils.getLinks(url);
    }

    @Override
    protected List<String> getFallback() {
        return new ArrayList<>();
    }
}
