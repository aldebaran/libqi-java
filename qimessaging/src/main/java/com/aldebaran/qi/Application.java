package com.aldebaran.qi;

public class Application
{

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
  private static native long qiApplicationCreate(String[] args, String defaultUrl, boolean listen);
  private static native long qiApplicationGetSession(long pApp);
  private static native void qiApplicationStart(long pApp);
  private static native void qiApplicationRun(long pApp);
  private static native void qiApplicationStop(long pApp);
  private static native void qiApplicationDestroy(long pApplication);

  /**
  * Crude interface to native log system
  */
  public static native void setLogCategory(String category, long verbosity);

  // Members
  private long _application;
  private Session _session;

  /**
   * Application constructor.
   * @param args Arguments given to main() function.
   * @param defaultUrl Default url to connect to if none was provided in the
   * program arguments
   * @param listen If no argument was provided, will start a listening session
   * with a ServiceDirectory (this argument is ignored for the moment)
   */
  public Application(String[] args, String defaultUrl, boolean listen)
  {
    if (args == null)
      throw new NullPointerException("Creating application with null args");
    if (defaultUrl == null)
      throw new NullPointerException("Creating application with null defaultUrl");
    _application = Application.qiApplicationCreate(args, defaultUrl, listen);
    _session = new Session(Application.qiApplicationGetSession(_application));
  }

  /**
   * Application constructor.
   * @param args Arguments given to main() function.
   */
  public Application(String[] args)
  {
    if (args == null)
      throw new RuntimeException("Creating application with null args");
    _application = Application.qiApplicationCreate(args, null, false);
    _session = new Session(Application.qiApplicationGetSession(_application));
  }

  /**
   * Start Application eventloops and connects the Session
   */
  public void start()
  {
    Application.qiApplicationStart(_application);
  }

  public Session session()
  {
    return _session;
  }

  /**
   * Stop Application eventloops and calls atStop() callbacks.
   * @since 1.20
   */
  public void stop()
  {
    Application.qiApplicationStop(_application);
  }

  /**
   * Blocking function. Application.run() join eventloop thread.
   * Return when :
   * - Eventloop is stopped.
   * - Application.stop() is called
   * @since 1.20
   */
  public void run()
  {
    Application.qiApplicationRun(_application);
  }

}
