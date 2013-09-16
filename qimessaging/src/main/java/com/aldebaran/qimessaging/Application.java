package com.aldebaran.qimessaging;

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
  private static native long qiApplicationCreate();
  private static native void qiApplicationRun(long pApp);
  private static native void qiApplicationStop(long pApp);
  private static native void qiApplicationDestroy(long pApplication);

  // Members
  long _application;

  /**
   * Application constructor.
   */
  public Application() {
    _application = Application.qiApplicationCreate();
  }

  /**
   * Application constructor.
   * @param args Arguments given to main() function. (unused !)
   * @since 1.20
   */
  public Application(String[] args)
  {
    _application = Application.qiApplicationCreate();
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

  /**
   * Called by garbage collector
   * Finalize is overridden manually in order to delete C++ data
   */
  @Override
  protected void finalize() throws Throwable
  {
    super.finalize();
  }

}
