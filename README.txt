Compiling and using qimessaging java bindings
==============================================

Requirements
-------------

The default CMake is broken for android, you should install
cmake 2.8.10 (or better) etheir by extracting the pre-compiled version here:
http://cmake.org/files/v2.8/, or by compiling a version yourself

Android SDK will work on linux64 if you install some 32bits libraries::

  sudo apt-get install gcc-multilib lib32z1

* Compile qimessaging-jni as usual::

  qibuild configure qimessaging-jni
  qibuild make qimessaging-jni

For android, get the NDK and the ATD bundle on ftp://kiwi/thirdparty/android/
and export the following environment variables:

 ANDROID_HOME       /path/to/adt-bundle-linux/sdk  # don't forget the 'sdk'
 ANDROID_NDK_HOME   /path/to/android-ndk-r8e

Get the toolchain on ftp://kiwi/qi/toolchains/feeds/master/linux32-android.xml

  qitoolchain create android ftp://kiwi/qi/toolchains/feeds/master/linux32-android.xml
  qibuild configure qimessaging-jni -c android
  qibuild make qimessaging-jni -c android

If you get link errors about boost::locale, this is yet another CMake bug,
and you should run::

  qibuild configure -DWITH_BOOST_LOCALE=OFF

* Copy (or create symlinks) so that the qimessaging native libs are treated as resources

  cd qimessaging/
  export PYTHONPATH=/path/to/qibuild/python:$PYTHONPATH # if qibuild is not properly installed
  python copy-jni-resources.py  # use -c android if relevant

* To use the qimessaging Java bindings in an other maven project,
  run:

    mvn install -DskipTests=true   # use -f pom-android.xml if relevant
