package com.aldebaran.qi;

import com.aldebaran.qi.ClientAuthenticator;

public abstract class ClientAuthenticatorFactory {
  abstract public ClientAuthenticator newAuthenticator();

  public int authVersionMajor() {
    return 1;
  }

  public int authVersionMinor() {
    return 0;
  }
}
