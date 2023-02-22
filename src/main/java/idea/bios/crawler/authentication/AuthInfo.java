package idea.bios.crawler.authentication;

import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.text.html.FormSubmitEvent.MethodType;

/**
 *
 * @author Avi Hayun
 * @date 11/23/2014
 *
 * Abstract class containing authentication information needed to login into a user/password
 * protected site<br>
 * This class should be extended by specific authentication types like form authentication and
 * basic authentication
 * etc<br>
 * <br>
 * This class contains all of the mutual authentication data for all authentication types
 */
public abstract class AuthInfo {
    public enum AuthenticationType {
        BASIC_AUTHENTICATION,
        FORM_AUTHENTICATION,
        NT_AUTHENTICATION
    }
    @Getter @Setter
    protected AuthenticationType authenticationType;
    @Getter @Setter
    protected MethodType httpMethod;
    @Getter @Setter
    protected String protocol;
    @Getter @Setter
    protected String host;
    @Getter @Setter
    protected String loginTarget;
    @Getter @Setter
    protected int port;
    @Getter @Setter
    protected String username;
    @Getter @Setter
    protected String password;

    /**
     * This constructor should only be used by extending classes
     *
     * @param authenticationType Pick the one which matches your authentication
     * @param httpMethod Choose POST / GET
     * @param loginUrl Full URL of the login page
     * @param username Username for Authentication
     * @param password Password for Authentication
     *
     * @throws MalformedURLException Make sure your URL is valid
     */
    protected AuthInfo(AuthenticationType authenticationType, MethodType httpMethod,
                       String loginUrl, String username, String password)
            throws MalformedURLException {
        this.authenticationType = authenticationType;
        this.httpMethod = httpMethod;
        URL url = new URL(loginUrl);
        this.protocol = url.getProtocol();
        this.host = url.getHost();
        this.port =
                url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
        this.loginTarget = url.getFile();

        this.username = username;
        this.password = password;
    }
}