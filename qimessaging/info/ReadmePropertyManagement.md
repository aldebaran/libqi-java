Their two property types. They are managed in different manner. 
**Don't mix the usage**.



First case: The object comes from external
------------------------------------------

In that case you have an **AnyObject** provides by the service:

### Get property value

    AnyObject.getProperty(QiSerializer, Class<T>, String)

### Set property value

    AnyObject.setProperty(QiSerializer, String, Object)

### Listen property changes

    AnyObject.connect(String, QiSignalListener)

**Warning**: The value received by the listener are serialized (One of **libqi-java** managed type). Have to use a QiSerializer to have a custom type.



Second case: The object is created local and shared to external
---------------------------------------------------------------

Steps to follow: 

1) Create a **com.aldebaran.qi.Property**

2) In a **com.aldebaran.qi.DynamicObjectBuilder** advertise the property:

    DynamicObjectBuilder.advertiseProperty(String, Property<T>)

3) Create the **AnyObject**

    DynamicObjectBuilder.object()

**Warning**: It is important to have advertise all methods, properties and signals before call the **object()** method. Never advertise something after called this method.

### Get property value

    Property.getValue(QiSerializer)

**Don't use: AnyObject.getProperty**

### Set property value

    Property.setValue(QiSerializer, T)

**Don't use: AnyObject.setProperty**

### Listen property changes

    AnyObject.connect(String, QiSignalListener)

**Warning**: The value received by the listener are serialized (One of **libqi-java** managed type). Have to use a QiSerializer to have a custom type.



Remarks:
--------

Pay a real attention in witch case you are before using a method.

