package com.aldebaran.qi;

import java.util.Map;

public interface ClientAuthenticator {
  public Map<String, Object> initialAuthData();

  public Map<String, Object> _processAuth(Map<String, Object> authData);
}
