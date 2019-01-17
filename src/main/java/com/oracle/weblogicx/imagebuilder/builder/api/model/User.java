package com.oracle.weblogicx.imagebuilder.builder.api.model;

import javax.validation.constraints.Email;

public class User {

    @Email(message = "A valid email id is required")
    private String email;

    private char[] password;

    public static User newUser(String email, String password) {
        return new User().setEmail(email).setPassword(password.toCharArray());
    }

    private User() {
        //restrict access
    }

    public String getEmail() {
        return email;
    }

    private User setEmail(String email) {
        this.email = email;
        return this;
    }

    public char[] getPassword() {
        return password;
    }

    private User setPassword(char[] password) {
        this.password = password;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null) {
            if (obj == this || User.class.isAssignableFrom(obj.getClass())) {
                return email.equalsIgnoreCase(((User) obj).getEmail());
            }
        }
        return false;
    }
}
