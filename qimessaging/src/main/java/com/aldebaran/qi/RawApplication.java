/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

/**
 * Class responsible for initializing the qi framework, but without creating a
 * {@link Session}.
 *
 * @see Application
 */
public class RawApplication {

    static {
        // Loading native C++ libraries.
        if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY) {
            EmbeddedTools loader = new EmbeddedTools();
            loader.loadEmbeddedLibraries();
        }
    }

    // Native function
    private native long qiApplicationCreate(String[] args);

    private native void qiApplicationRun(long pApp);

    private native void qiApplicationStop(long pApp);

    private native void qiApplicationDestroy(long pApplication);

    // Members
    private long _application;

    /**
     * RawApplication constructor.
     *
     * @param args Arguments given to main() function.
     */
    public RawApplication(String[] args) {
        if (args == null)
            throw new NullPointerException("Creating application with null args");
        _application = qiApplicationCreate(args);
    }

    /**
     * Stop RawApplication eventloops and calls atStop() callbacks.
     */
    public void stop() {
        qiApplicationStop(_application);
    }

    /**
     * Blocking function. RawApplication.run() join eventloop thread.
     * Return when :
     * - Eventloop is stopped.
     * - RawApplication.stop() is called
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
