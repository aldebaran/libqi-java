package com.aldebaran.qi;

import java.util.Map;

/**
 * Interface used to create an authenticator for connections to a given
 * {@link Session}.
 */
public interface ClientAuthenticator {
    public Map<String, Object> initialAuthData();

    public Map<String, Object> _processAuth(Map<String, Object> authData);
}
