package com.oracle.weblogicx.imagebuilder.builder.api.model;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.util.Objects;

public class UserSession {

    private User user;
    private HttpClient oraClient;
    private boolean isUserValidated = false;

    public UserSession(String email, String password) {
        this(User.newUser(email, password));
    }

    public UserSession(User user) {
        Objects.requireNonNull(user);
        this.user = user;
        oraClient = createOraClient();
    }

    public UserSession(User user, HttpClient oraClient) {
        this.user = user;
        this.oraClient = oraClient;
    }

    public UserSession(UserSession userSession) {
        this.user = userSession.getUser();
        this.oraClient = userSession.getOraClient();
    }

    private HttpClient createOraClient() {
        RequestConfig.Builder config = RequestConfig.custom();
        config.setCircularRedirectsAllowed(true);

        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cc = new BasicClientCookie("oraclelicense", "a");
        cc.setDomain("edelivery.oracle.com");
        cookieStore.addCookie(cc);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                user.getEmail(), String.valueOf(user.getPassword())));

        return HttpClientBuilder.create().setDefaultRequestConfig(config.build())
                .setDefaultCookieStore(cookieStore).useSystemProperties()
                .setDefaultCredentialsProvider(credentialsProvider).build();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null) {
            if (obj == this || UserSession.class.isAssignableFrom(obj.getClass())) {
                return this.getUser().equals(((UserSession) obj).getUser());
            }
        }
        return false;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public HttpClient getOraClient() {
        return oraClient;
    }

    public void setOraClient(HttpClient oraClient) {
        this.oraClient = oraClient;
    }

    public boolean isUserValidated() {
        return isUserValidated;
    }

    public void setUserValidated(boolean userValidated) {
        isUserValidated = userValidated;
    }
}
