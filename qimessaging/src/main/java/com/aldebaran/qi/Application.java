/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

/**
 * Class responsible for initializing the qi framework.
 * <p>
 * When started, it creates a {@link Session} by default connected
 * to <i>tcp://127.0.0.1:9559</i> and listening to <i>tcp://0.0.0.0:0</i>
 */
public class Application {

    static {
        // Loading native C++ libraries.
        if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY) {
            EmbeddedTools loader = new EmbeddedTools();
            loader.loadEmbeddedLibraries();
        }
    }

    // Native function
    private native long qiApplicationCreate(String[] args, String defaultUrl, boolean listen);

    private native long qiApplicationGetSession(long pApp);

    private native void qiApplicationStart(long pApp);

    private native void qiApplicationRun(long pApp);

    private native void qiApplicationStop(long pApp);

    private native void qiApplicationDestroy(long pApplication);

    /**
     * Crude interface to native log system
     */
    public static native void setLogCategory(String category, long verbosity);

    // Members
    private long _application;
    private Session _session;

    /**
     * Application constructor.
     *
     * @param args       Arguments given to main() function.
     * @param defaultUrl Default url to connect to if none was provided in the
     *                   program arguments
     */
    public Application(String[] args, String defaultUrl) {
        if (args == null)
            throw new NullPointerException("Creating application with null args");
        if (defaultUrl == null)
            throw new NullPointerException("Creating application with null defaultUrl");
        init(args, defaultUrl, false);
    }

    /**
     * Application constructor.
     *
     * @param args Arguments given to main() function.
     */
    public Application(String[] args) {
        if (args == null)
            throw new RuntimeException("Creating application with null args");
        init(args, null, false);
    }

    private void init(String[] args, String defaultUrl, boolean listen) {
        _application = qiApplicationCreate(args, defaultUrl, listen);
        _session = new Session(qiApplicationGetSession(_application));
    }

    /**
     * Start Application eventloops and connects the Session
     */
    public void start() {
        qiApplicationStart(_application);
    }

    public Session session() {
        return _session;
    }

    /**
     * Stop Application eventloops and calls atStop() callbacks.
     *
     * @since 1.20
     */
    public void stop() {
        qiApplicationStop(_application);
    }

    /**
     * Blocking function. Application.run() join eventloop thread.
     * Return when :
     * - Eventloop is stopped.
     * - Application.stop() is called
     *
     * @since 1.20
     */
    public void run() {
        qiApplicationRun(_application);
    }

    /**
     * Called by garbage collector when object destroy.<br>
     * Override to free the reference in JNI.
     *
     * @throws Throwable On destruction issue.
     */
    @Override
    protected void finalize() throws Throwable {
        this.qiApplicationDestroy(this._application);
        super.finalize();
    }
}
