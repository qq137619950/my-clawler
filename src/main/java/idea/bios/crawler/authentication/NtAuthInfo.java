package idea.bios.crawler.authentication;

import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;

import javax.swing.text.html.FormSubmitEvent.MethodType;

/**
 * Authentication information for Microsoft Active Directory
 * @author 86153
 */
public class NtAuthInfo extends AuthInfo {
    @Getter @Setter
    private String domain;

    public NtAuthInfo(String username, String password, String loginUrl, String domain)
        throws MalformedURLException {
        super(AuthenticationType.NT_AUTHENTICATION, MethodType.GET, loginUrl, username, password);
        this.domain = domain;
    }
}