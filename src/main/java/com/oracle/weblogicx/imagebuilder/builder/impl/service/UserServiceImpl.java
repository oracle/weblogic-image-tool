package com.oracle.weblogicx.imagebuilder.builder.impl.service;

import com.oracle.weblogicx.imagebuilder.builder.api.model.User;
import com.oracle.weblogicx.imagebuilder.builder.api.model.UserSession;
import com.oracle.weblogicx.imagebuilder.builder.api.service.UserService;
import com.oracle.weblogicx.imagebuilder.builder.util.ARUUtil;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public enum UserServiceImpl implements UserService {

    USER_SERVICE;

    private static final Map<User, UserSession> userSessionMap = new ConcurrentHashMap<>();

    @Override
    public UserSession getUserSession(User user) {
        Objects.requireNonNull(user, "User parameter cannot be null");
        UserSession userSession = userSessionMap.get(user);
        if (userSession == null) {
            userSession = validateUser(user);
            if (userSession != null) {
                addUserSession(user, userSession);
            }
        }
        return userSession;
    }

    @Override
    public boolean addUserSession(UserSession userSession) {
        Objects.requireNonNull(userSession, "UserSession parameter cannot be null");
        if (!userSession.isUserValidated()) {
            if (!ARUUtil.checkCredentials(userSession)) {
                return false;
            }
            userSession.setUserValidated(true);
        }
        return addUserSession(userSession.getUser(), userSession);
    }

    @Override
    public boolean deleteUserSession(UserSession userSession) {
        //String userEmail = userSession.getUser().getEmail();
        User user = userSession.getUser();
        if (userSession.equals(userSessionMap.get(user))) {
            //synchronized (userSessionMap) {
            userSessionMap.remove(user, userSession);
            //}
            return true;
        }
        return false;
    }

    private boolean addUserSession(User user, UserSession userSession) {
        userSessionMap.put(user, userSession);
        return true;
    }

    private UserSession validateUser(@NotNull User user) {
        UserSession userSession = new UserSession(user);
        if (ARUUtil.checkCredentials(userSession)) {
            userSession.setUserValidated(true);
        }
        return userSession;
    }

    //    public Executor getOraHttpExecutor(String username, String password) {
//        RequestConfig.Builder config = RequestConfig.custom();
//        config.setCircularRedirectsAllowed(true);
//
//        CookieStore cookieStore = new BasicCookieStore();
//        BasicClientCookie cc = new BasicClientCookie("oraclelicense", "a");
//        cc.setDomain("edelivery.oracle.com");
//        cookieStore.addCookie(cc);
//
//        CloseableHttpClient client =
//                HttpClientBuilder.create().setDefaultRequestConfig(config.build()).useSystemProperties().build();
//
//        Executor httpExecutor = Executor.newInstance(client).auth(username, password);
//        httpExecutor.use(cookieStore);
//        return httpExecutor;
//    }
}
