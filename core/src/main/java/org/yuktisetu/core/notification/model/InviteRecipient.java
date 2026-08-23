package org.yuktisetu.core.notification.model;

public class InviteRecipient {

    private final String email;
    private final String name;
    private final String token;

    public InviteRecipient(String email, String name, String token) {
        this.email = email;
        this.name = name;
        this.token = token;
    }

    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getToken() { return token; }
}
