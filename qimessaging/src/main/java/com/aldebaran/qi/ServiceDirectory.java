/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

public class ServiceDirectory {

  static
  {
    // Loading native C++ libraries.
    if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY)
    {
      EmbeddedTools loader = new EmbeddedTools();
      loader.loadEmbeddedLibraries();
    }
  }

  // Native function
  private native long qiTestSDCreate();
  private native void qiTestSDDestroy(long pServiceDirectory);
  private native String qiListenUrl(long pServiceDirectory);
  private native void qiTestSDClose(long pServiceDirectory);

  // Members
  private long _sd;

  public ServiceDirectory()
  {
    _sd = qiTestSDCreate();
  }

  public String listenUrl()
  {
    return qiListenUrl(_sd);
  }

  @Override
  protected void finalize()
  {
    qiTestSDDestroy(_sd);
  }

  public void close()
  {
    qiTestSDClose(_sd);
  }
}
