Compiling and using libqi java bindings
=======================================


Compiling
----------

* Install `qibuild <http://doc.aldebaran.com/qibuild/>`_

* Get a toolchain (See `here <https://github.com/aldebaran/toolchains/tree/master/feeds>`_
  for a list of supported architectures) ::

    qitoolchain create --feed-name <feed-name> <toolchain-name> git://github.com/aldebaran/toolchains.git
<toolchain-name> : name give to tool chain

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
    qibuild configure -c <config-name>
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


Android compilation
+++++++++++++++++++

For Android arm, you have to choose android-arm configuration.
The first time create the configuration
    cd ~/work/aldebaran/sdk/libqi-java
    qitoolchain create --feed-name android-arm android-arm git://github.com/aldebaran/toolchains.git

On each compilation
    cd ~/work/aldebaran/sdk/libqi-java/jni
    qibuild configure --release -c android-arm
    qibuild make -j4 -c android-arm
    mkdir -p ../qimessaging/lib/armeabi-v7a
    mkdir -p /tmp/libqi-java/android-arm/
    qibuild install --runtime -c android-arm
    
    cd /tmp/libqi-java/android-arm/
    
    for file in $(find -name *.so)
    do
       cp $file ~/work/aldebaran/sdk/libqi-java/qimessaging/lib/armeabi-v7a
    done

    cd ~/work/aldebaran/sdk/libqi-java
    rm -rf /tmp/libqi-java/android-arm/
    cp -f jni/build-android-arm/sdk/lib/libgnustl_shared.so qimessaging/lib/armeabi-v7a
    cd qimessaging
    mvn install -DskipTests=true # use -f pom-android.xml
    version=$(cat pom-android.xml | grep -oPm1 "(?<=<version>)[^<]+")
    zip -r target/libqi-java-$version.jar lib/

