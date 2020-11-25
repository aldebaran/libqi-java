/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.aldebaran.qi.serialization.QiSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectTest {
    public AnyObject proxy = null;
    public AnyObject proxyts = null;
    public AnyObject obj = null;
    public AnyObject objts = null;
    public Session s = null;
    public Session client = null;
    public ServiceDirectory sd = null;
    public DynamicObjectBuilder ob = null;

    @Before
    public void setUp() throws Exception {
        sd = new ServiceDirectory();
        s = new Session();
        client = new Session();

        // Get Service directory listening url.
        String url = sd.listenUrl();

        // Create new QiMessaging generic object
        ob = new DynamicObjectBuilder();

        // Get instance of ReplyService
        QiService reply = new ReplyService();

        // Register event 'Fire'
        ob.advertiseSignal("fire::(i)");
        ob.advertiseMethod("reply::s(s)", reply, "Concatenate given argument with 'bim !'");
        ob.advertiseMethod("answer::s()", reply, "Return given argument");
        ob.advertiseMethod("add::i(iii)", reply, "Return sum of arguments");
        ob.advertiseMethod("info::(sib)(sib)", reply, "Return a tuple containing given arguments");
        ob.advertiseMethod("answer::i(i)", reply, "Return given parameter plus 1");
        ob.advertiseMethod("answerFloat::f(f)", reply, "Return given parameter plus 1");
        ob.advertiseMethod("answerBool::b(b)", reply, "Flip given parameter and return it");
        ob.advertiseMethod("abacus::{ib}({ib})", reply, "Flip all booleans in map");
        ob.advertiseMethod("echoFloatList::[m]([f])", reply, "Return the exact same list");
        ob.advertiseMethod("createObject::o()", reply, "Return a test object");
        ob.advertiseMethod("createNullObject::o()", reply, "Return a null object");
        ob.advertiseMethod("setStored::v(i)", reply, "Set stored value");
        ob.advertiseMethod("waitAndAddToStored::i(ii)", reply, "Wait given time, and return stored + val");
        ob.advertiseMethod("genTuple::(is)()", reply, "Return a tuple");
        ob.advertiseMethod("genTuples::[(is)]()", reply, "Return a tuple list");
        ob.advertiseMethod("getFirstFieldValue::i((is))", reply, "Return the first field value as int");

        ob.advertiseMethods(QiSerializer.getDefault(), TestInterface.class, new TestClass());

        QiService replyts = new ReplyService();
        DynamicObjectBuilder obts = new DynamicObjectBuilder();
        obts.advertiseMethod("setStored::v(i)", replyts, "Set stored value");
        obts.advertiseMethod("waitAndAddToStored::i(ii)", replyts, "Wait given time, and return stored + val");
        obts.advertiseMethod("throwUp::v()", replyts, "Throws");
        obts.setThreadingModel(DynamicObjectBuilder.ObjectThreadingModel.MultiThread);

        // Connect session to Service Directory
        s.connect(url).sync();

        // Register service as serviceTest
        obj = ob.object();
        objts = obts.object();
        assertTrue("Service must be registered", s.registerService("serviceTest", obj) > 0);
        assertTrue("Service must be registered", s.registerService("serviceTestTs", objts) > 0);

        // Connect client session to service directory
        client.connect(url).sync();

        // Get a proxy to serviceTest
        proxy = client.service("serviceTest").get();
        proxyts = client.service("serviceTestTs").get();
        assertNotNull(proxy);
        assertNotNull(proxyts);
    }

    @After
    public void tearDown() {
        obj = null;
        proxy = null;

        s.close();
        client.close();
        sd.close();

        s = null;
        client = null;
        sd = null;
    }

    @Test
    public void singleThread() throws Exception {
        Future<Integer> v0;
        v0 = proxy.<Integer>call("waitAndAddToStored", 500, 0);
        Thread.sleep(10);
        Future<Void> v1 = proxy.<Void>call("setStored", 42);
        assertEquals(v0.get(), new Integer(0));
    }

    @Test
    public void multiThread() throws Exception {
        Future<Integer> v0 = proxyts.<Integer>call("waitAndAddToStored", 500, 0);
        Thread.sleep(10);
        Future<Void> v1 = proxyts.<Void>call("setStored", 42);
        assertEquals(v0.get(), new Integer(42));
    }

    @Test
    public void callThrow() throws Exception {
        Future<Integer> v0 = proxyts.<Integer>call("throwUp");
        assertTrue(v0.hasError());
        assertEquals(v0.getErrorMessage(), "I has faild");
    }

    @Test
    public void getErrorOnSuccess() throws Exception {
        Future<Void> v0 = proxyts.<Void>call("setStored", 18);
        assertNull(v0.getError());
        assertFalse(v0.hasError());
    }

    @Test
    public void getObject() {
        boolean ok;
        AnyObject anyObject = null;

        try {
            anyObject = proxy.<AnyObject>call("createObject").get();
        } catch (Exception e) {
            fail("Call must not fail: " + e.getMessage());
        }

        assertNotNull(anyObject);
        try {
            assertEquals("foo", anyObject.<String>property("name").get());
        } catch (Exception e1) {
            fail("Property must not fail");
        }

        Map<Object, Object> settings = new HashMap<Object, Object>();
        settings.put("foo", true);
        settings.put("bar", "This is bar");
        try {
            anyObject.setProperty("settings", settings).get();
        } catch (Exception e) {
            fail("Call must succeed: " + e.getMessage());
        }
        Map<Object, Object> readSettings = null;
        try {
            readSettings = anyObject.<Map<Object, Object>>property("settings").get();
        } catch (ExecutionException e) {
            fail("Execution must not fail: " + e.getMessage());
        }
        assertEquals(true, readSettings.get("foo"));
        assertEquals("This is bar", readSettings.get("bar"));

        String ret = null;
        try {
            ret = anyObject.<String>call("answer").get();
        } catch (Exception e) {
            fail("Call must succeed : " + e.getMessage());
        }
        assertEquals("42 !", ret);

        ok = false;
        try {
            ret = anyObject.<String>call("add", 42).get();
        } catch (Exception e) {
            ok = true;
            System.out.println(e.getMessage());
            assertTrue(e.getMessage().startsWith("Could not find suitable " +
                    "method"));
        }
        assertTrue(ok);

        ok = false;
        try {
            ret = anyObject.<String>call("add", "42", 42, 42).get();
        } catch (ExecutionException e) {
            ok = true;
            String prefix = "Could not find suitable method";
            System.out.println(e.getMessage());
            assertTrue(e.getMessage().startsWith(prefix));
        }
        assertTrue(ok);

        ok = false;
        try {
            ret = anyObject.<String>call("addFoo").get();
        } catch (Exception e) {
            ok = true;
            String expected = "Can't find method: addFoo\n";
            System.out.println(e.getMessage());
            System.out.println(expected);
            assertTrue(e.getMessage().startsWith("Could not find suitable " +
                    "method"));
        }
        assertTrue(ok);
    }

    @QiStruct
    static class Item {
        @QiField(0)
        int i;
        @QiField(1)
        String s;

        Item() {
        }

        Item(int i, String s) {
            this.i = i;
            this.s = s;
        }
    }

    @Test
    public void testCallReturnStructConversion() throws ExecutionException {
        Tuple tuple = proxy.<Tuple>call("genTuple").get();
        assertEquals(42, tuple.get(0));
        assertEquals("forty-two", tuple.get(1));

        Item item = proxy.call(Item.class, "genTuple").get();
        assertEquals(42, item.i);
        assertEquals("forty-two", item.s);

        Type listOfItemsType = new TypeToken<List<Item>>() {
        }.getType();
        List<Item> items = proxy.<List<Item>>call(listOfItemsType, "genTuples").get();
        item = items.get(0);
        assertEquals(42, item.i);
        assertEquals("forty-two", item.s);
    }

    @Test
    public void testCallParameterStructConversion() throws ExecutionException {
        Item item = new Item(42, "forty-two");
        int value = proxy.<Integer>call(int.class, "getFirstFieldValue", item).get();
        assertEquals(42, value);
    }

    @Test
    public void callsCanReturnNullAkaInvalidObject() {
        try {
            AnyObject anyObject = proxy.<AnyObject>call("createNullObject").get();
            assertNull(anyObject);
        } catch (Exception e) {
            fail("Call to 'createNullObject' failed: " + e.getMessage());
        }
    }

    /**
     * Utility structures for `advertiseMethodsWithComplexSignatureCanBeCalledWithSameType` test
     * consisting of an interface and a class with a method with a "complex" signature (List of String).
     */

    interface TestInterface {
        Integer stringListMethod(List<String> values);
        byte[] byteArrayMethod(byte[] buffer);
        ByteBuffer byteBufferMethod(ByteBuffer buffer);
    }

    static class TestClass implements TestInterface {
        public Integer stringListMethod(List<String> values) {
            return values.hashCode();
        }

        public byte[] byteArrayMethod(byte[] buffer) {
            return buffer;
        }

        public ByteBuffer byteBufferMethod(ByteBuffer buffer) {
            return buffer;
        }
    }

    /**
     * The purpose of this test is to verify that generic types don't lose type parameter
     * information when advertised.
     * In this test case, the method waits for a `List<String>` so the test checks that
     * it advertised as such and it can be called accordingly.
     * See issue #41933.
     */
    @Test
    public void advertiseMethodsWithComplexSignatureCanBeCalledWithSameType() {
        try {
            ArrayList<String> l = new ArrayList<String>();

            l.add("Test");

            // Tries to call complexMethod with coinciding parameters.
            Future<Integer> f = ob.object().<Integer>call("stringListMethod", l);

            assertEquals(f.get().intValue(), l.hashCode());
        } catch (ExecutionException e) {
            fail("Call to 'stringListMethod' failed: " + e.getMessage());
        }
    }

    @Test
    public void advertiseMethodWithByteArray() {
        try {
            byte[] bytes = "Coucou les amis".getBytes();

            Future<byte[]> f = ob.object().call("byteArrayMethod", bytes);

            assertArrayEquals(f.<byte[]>get(), bytes);
        } catch (ExecutionException e) {
            fail("Call to 'byteArrayMethod' failed: " + e.getMessage());
        }
    }

    @Test
    public void advertiseMethodWithEmptyByteArray() {
        try {
            byte[] bytes = new byte[0];

            Future<byte[]> f = ob.object().call("byteArrayMethod", bytes);

            assertArrayEquals(f.<byte[]>get(), bytes);
        } catch (ExecutionException e) {
            fail("Call to 'byteArrayMethod' failed: " + e.getMessage());
        }
    }

    @Test
    public void advertiseMethodWithEmptyByteBuffer() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(0);

            Future<ByteBuffer> f = ob.object().call(ByteBuffer.class, "byteBufferMethod", buffer);

            assertEquals(buffer, f.<ByteBuffer>get());
        } catch (ExecutionException e) {
            fail("Call to 'byteBufferMethod' failed: " + e.getMessage());
        }
    }

    @Test
    public void advertiseMethodWithDirectByteBuffer() {
        try {
            ByteBuffer originalBuffer = ByteBuffer.allocateDirect(10);

            // Need to duplicate the buffer in order to compare remaining elements.
            ByteBuffer consumedBuffer = originalBuffer.duplicate();

            Future<ByteBuffer> f = ob.object().call(ByteBuffer.class, "byteBufferMethod", consumedBuffer);

            ByteBuffer result = f.get();

            assertEquals(originalBuffer, result);
        } catch (ExecutionException e) {
            fail("Call to 'byteBufferMethod' failed: " + e.getMessage());
        }
    }

    @Test
    public void advertiseMethodWithValuesInByteBuffer() {
        try {
            // TODO: Replace the following capacity expression when switching to java 8 by
            //  `Byte.BYTES + Character.BYTES + Integer.BYTES + Double.BYTES`.
            ByteBuffer buffer = ByteBuffer.allocate(1 + 2 + 4 + 8);

            byte   v1 = 24;
            char   v2 = 'x';
            int    v3 = 42;
            double v4 = 3.14;

            buffer.put(v1);
            buffer.putChar(v2);
            buffer.putInt(v3);
            buffer.putDouble(v4);
            buffer.rewind();

            Future<ByteBuffer> f = ob.object().call(ByteBuffer.class, "byteBufferMethod", buffer);
            ByteBuffer result = f.get();
            assertEquals(v1, result.get());
            assertEquals(v2, result.getChar());
            assertEquals(v3, result.getInt());
            assertEquals(v4, result.getDouble(), 0.1);

        } catch (ExecutionException e) {
            fail("Call to 'byteBufferMethod' failed: " + e.getMessage());
        }
    }

    @Test
    public void advertiseMethodWithByteBuffer() {
        try {
            byte[] array = "Coucou les amis".getBytes();
            ByteBuffer originalBuffer = ByteBuffer.wrap(array);

            // Need to duplicate the buffer in order to compare remaining elements.
            ByteBuffer consumedBuffer = originalBuffer.duplicate();

            Future<ByteBuffer> f = ob.object().call(ByteBuffer.class, "byteBufferMethod", consumedBuffer);

            ByteBuffer result = f.get();

            assertEquals(originalBuffer, result);
            // None of the two buffers should be sliced. Underling array can be compared.
            assertArrayEquals(originalBuffer.array(), result.array());
        } catch (ExecutionException e) {
            fail("Call to 'byteBufferMethod' failed: " + e.getMessage());
        }
    }

    @Test
    public void advertiseMethodWithSlicedByteBuffer() {
        try {
            ByteBuffer originalBuffer = ByteBuffer.wrap("Coucou les amis".getBytes(), 2, 5);

            // Need to duplicate the buffer in order to compare remaining elements.
            ByteBuffer consumedBuffer = originalBuffer.duplicate();

            Future<ByteBuffer> f = ob.object().call(ByteBuffer.class, "byteBufferMethod", consumedBuffer);

            assertEquals(originalBuffer, f.<ByteBuffer>get());
        } catch (ExecutionException e) {
            fail("Call to 'byteBufferMethod' failed: " + e.getMessage());
        }
    }

    static class BigObject extends QiService
    {
        public void returnBigNames(List<Object> input)
        {
        };
    }

    @Test
    public void advertiseMethodWithBigList()
    {
        DynamicObjectBuilder ob = new DynamicObjectBuilder();
        try
        {
            List<String> names = new ArrayList<String>();
            for(int i = 0; i < 1000; i++)
            {
                names.add("name" + i);
            }
            List<Tuple> itemList = new ArrayList<Tuple>();
            for(int i = 0; i < names.size(); ++i)
            {
                itemList.add(Tuple.of(names));
            }

            BigObject bigO = new BigObject();
            ob.advertiseMethod("returnBigNames::v(m)", bigO, "calling big boy");
            Future<Void> fut = ob.object().call("returnBigNames", itemList);
            fut.get();
        } catch (Exception e)
        {
            fail("cannot advertise methods: " + e.getMessage());
        }
    }
}
