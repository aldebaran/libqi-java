package com.aldebaran.qi;

import java.util.HashMap;
import java.util.Map;

/**
 * Specific {@link ClientAuthenticator} that authenticates a user using a token.
 */
public class UserTokenAuthenticator implements ClientAuthenticator {
    private final String user;
    private final String token;

    /**
     * Constructs an authenticator with the specified user and token.
     */
    public UserTokenAuthenticator(String user, String token) {
        this.user = Objects.requireNonNull(user);
        this.token = Objects.requireNonNull(token);
    }

    /**
     * Constructs an authenticator with the specified user and an empty token.
     *
     * @deprecated since 3.1.1, use the constructor that takes both user and token arguments
     */
    @Deprecated
    public UserTokenAuthenticator(String user) {
        this(user, "");
    }

    @Override
    public Map<String, Object> initialAuthData() {
        return new HashMap<String, Object>(){{
            put("user", user);
            put("token", token);
        }};
    }

    @Override
    public Map<String, Object> _processAuth(Map<String, Object> authData) {
        return new HashMap();
    }

    /**
     * Indicates whether a new token has been retrieved during the last
     * authentication.
     *
     * @return {@code false} all the time.
     * @deprecated since 3.1.1, became useless since a new token cannot be retrieved with _processAuth.
     */
    @Deprecated
    public boolean hasNewToken() {
        return false;
    }

    public String getToken() {
        return token;
    }
}