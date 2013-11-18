********************************
QiMessaging Java bindings
********************************

Introduction
============

QiMessaging provides Java bindings to call remote services,
create and create new services.

They required Java 1.6 or higher, and are available on the following platforms:

 * Windows 32 bits (no 64bits support)
 * Linux 32 and 64 bits (tested with Ubuntu 12.04 LTS, but should work fine on
   any recent distribution)
 * The NAO v4
 * Android 4.0 (experimental)

.. warning:: The API presented here is still under development and may change
             in the future.


How to use
===========

There is one ``.jar`` per platform, because the implementation uses
a C++ dynamic library using ``JNI``

Just use the ``.jar`` as you would do for any 3rd party Java library:

In eclipse
-----------

Just add the ``qimessaging.jar`` as an external jar in the properties of your project

From the command line
---------------------

.. code-block:: sh

  javac -cp /path/to/qimessaging.jar YourClass.java

  java -cp /path/to/qimessaging.jar:. YourClass

Examples
=========

Call a remote service
----------------------

The list of the available services and methods is available
in the ``NAOqi`` API documentation.

.. literalinclude:: /samples/SayHello/SayHello.java
   :language: java


Create a new service
---------------------

.. literalinclude:: /samples/HelloService/App.java
   :language: java

Were ``HelloService`` implementation looks like this:

.. literalinclude:: /samples/HelloService/HelloService.java
   :language: java


In order for the service to run, you should first run a ``master``,
and then connect your Java sercive to it:

.. code-block:: console

  $ /path/to/cpp/sdk/bin/qi-master
  $ java App


You can then call the advertised methods of the ``hello``
service as you would do for any other ``NAOqi`` service,
or using ``qicli``

.. code-block:: console

  $ /path/to/cpp/sdk/bin/qicli hello.greet "world"


Reacting to events
------------------

.. literalinclude:: /samples/ReactToEvents/ReactToEvents.java
   :language: java


Note: This example does not use the familiar ``ALMemory.subscribeToEvent`` method.

Instead it calls the ``ALMemory.subscriber`` method.

This returns a ``com.aldebaran.qimessaging.Object`` on which we can connect
a signal named ``signal`` to our callback.
(This is a bit weird but required for NAOqi 1 backward compatibility)

Note how we specify the "signature" of the function we want to call:
``signal`` returns a ``com.aldebaran.qimessaging.Object``, and the
callback also takes a ``com.aldebaran.qimessaging.Object`` as parameter.


Notes
------

* The ``Application`` constructor *must* be called exactly once
  in the main Java process for the type system and the event loop to work.
