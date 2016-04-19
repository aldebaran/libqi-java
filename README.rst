Compiling and using libqi java bindings
=======================================


If you are from Aldebaran
-------------------------

Go read the instructions on the internal documentation, this README is
for external contributors only.

Compiling
----------

* Install `qibuild <http://doc.aldebaran.com/qibuild/>`_

* Get a toolchain (See `here <https://github.com/aldebaran/toolchains/tree/master/feeds>`_
  for a list of supported architectures) ::

    qitoolchain create <toolchain-name> --feed-name <feed-name> git://github.com/aldebaran/toolchains.git

  (Where ``feed-name`` is the basename of one of the feeds in ``feeds/``, without the
  ``.xml`` extension)

* If you want to build for android, you should install the NDK and set the
  following environment variables::

    ANDROID_HOME       /path/to/adt-bundle-linux/sdk  # don't forget the 'sdk'
    ANDROID_NDK_HOME   /path/to/android-ndk-r8e


* Create a build config matching this toolchain::

    qibuild add-config <config-name> -t <toolchain-name>

* Fetch the sources::

    mkdir -p ~/work/aldebaran
    cd ~/work/aldebaran
    qisrc init git@github.com:aldebaran/manifest.git

* Configure and build ``libqi-java`` bindings::

    cd sdk/libqi-java/jni
    qibuild confiure -c <config-name>
    qibuild make -c <config-name>

* Copy (or create symlinks) so that the qimessaging native libs are treated as resources

  For instance, on linux, you should copy ``libqi.so`` and ``libqimessagingjni.so`` to
  ``libqi-java/qimessaging/native/linux64``

* If you are not building for android, you  can now run the tests with
  `maven <https://maven.apache.org/>`_ ::

    mvn test

Using libqi-java in an other project
++++++++++++++++++++++++++++++++++++


To use the libqi Java bindings in an other maven project, run::

    mvn install -DskipTests=true   # use -f pom-android.xml if relevant
