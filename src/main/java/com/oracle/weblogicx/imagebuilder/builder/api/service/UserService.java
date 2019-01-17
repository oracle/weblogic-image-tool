package com.oracle.weblogicx.imagebuilder.builder.api.service;

import com.oracle.weblogicx.imagebuilder.builder.api.model.User;
import com.oracle.weblogicx.imagebuilder.builder.api.model.UserSession;
import org.apache.http.client.HttpClient;

import java.io.IOException;

public interface UserService {

    /**
     * Gets a UserSession with http client for this user with all the required plumbing done
     * to handle Oracle download cookies.
     * @param user should have email and password set
     * @return a UserSession or null if validation fails
     */
    UserSession getUserSession(User user) throws IOException;

    boolean addUserSession(UserSession userSession);

    boolean deleteUserSession(UserSession userSession);

}
