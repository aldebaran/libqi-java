package com.aldebaran.qi;

import com.aldebaran.qi.ClientAuthenticator;

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
