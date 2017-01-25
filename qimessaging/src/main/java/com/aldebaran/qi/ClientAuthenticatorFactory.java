package com.aldebaran.qi;

/**
 * Abstract class that represents a factory for creating
 * {@link ClientAuthenticator}s.
 */
public abstract class ClientAuthenticatorFactory {
    abstract public ClientAuthenticator newAuthenticator();

    public int authVersionMajor() {
        return 1;
    }

    public int authVersionMinor() {
        return 0;
    }
}
