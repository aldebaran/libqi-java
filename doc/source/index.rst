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


Note: This example does not use the familiar ``ALMemory.subscribeToEvent`` method,
but a new generic Signal system, bridged to the old API through the
``ALMemory.subscriber`` method.

This method returns a ``com.aldebaran.qimessaging.Object``, which has a
signal named ``signal``, on which we can connect our callback.

The main advantage of this new approach is that it no longuer requires
you to register a module in order to monitor events.

Note that at this point you have to specify the "signature", of both
the signal, and the callback function we want to call:

"m" stands for *anything*, which means that ``signal`` accepts all
instances of ``java.lang.Object``, and that the callback should
accept a ``java.lang.Object`` as parameter. The effective type
of the argument will vary depending on what was passed to
``signal``, for instance a ``java.lang.Integer`` or a
``java.lang.String``.


Notes
------

* The ``Application`` constructor *must* be called exactly once
  in the main Java process for the type system and the event loop to work.
