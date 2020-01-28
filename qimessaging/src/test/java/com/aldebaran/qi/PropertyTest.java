package com.aldebaran.qi;

import com.aldebaran.qi.serialization.QiSerializer;
import com.aldebaran.qi.serialization.StructConverter;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PropertyTest {

    private <T> void constructWithAValue(T value) {
        Property<T> prop = new Property<T>(value);
        try {
            assertEquals(value, prop.getValue().get());
        } catch (ExecutionException e) {
            fail(e.getMessage());
        }
    }

    private <T> void constructWithAValueAndClass(Class<T> cls, T value) {
        Property<T> prop = new Property<T>(cls, value);
        try {
            assertEquals(value, prop.getValue().get());
        } catch (ExecutionException e) {
            fail(e.getMessage());
        }
    }

    private void constructWithAValueAndClass(Class<byte[]> cls, byte[] value) {
        Property<byte[]> prop = new Property<byte[]>(cls, value);
        try {
            assertArrayEquals(value, prop.getValue().get());
        } catch (ExecutionException e) {
            fail(e.getMessage());
        }
    }

    private void assertThrowsNullPointerException(Function<Void, Void> function) {
        try {
            function.execute(null);
        } catch (NullPointerException ex) {
            // Success.
        } catch (Throwable throwable) {
            fail("The call threw an exception that was not " +
                    "NullPointerException: " + throwable.getMessage());
        }
    }

    @Test
    public void constructedWithNullValueThrows() {
        assertThrowsNullPointerException(new Function<Void, Void>() {
            @Override
            public Void execute(Void v) throws Throwable {
                new Property<Integer>((Integer) null);
                return null;
            }
        });
    }

    @Test
    public void constructedWithNullClassThrows() {
        assertThrowsNullPointerException(new Function<Void, Void>() {
            @Override
            public Void execute(Void v) throws Throwable {
                new Property<Integer>((Class<Integer>) null);
                return null;
            }
        });
    }

    @Test
    public void constructedWithNullValueOrClassThrows() {
        final Integer value = 42;
        final Class<Integer> cls = (Class<Integer>) value.getClass();

        assertThrowsNullPointerException(new Function<Void, Void>() {
            @Override
            public Void execute(Void v) {
                new Property<Integer>((Class<Integer>) null, value);
                return null;
            }
        });
        assertThrowsNullPointerException(new Function<Void, Void>() {
            @Override
            public Void execute(Void v) throws QiConversionException {
                new Property<Integer>(cls, (Integer) null);
                return null;
            }
        });
    }

    @Test
    public void constructedWithAStringValueSucceeds() {
        String value = "Hello world !";
        constructWithAValue(value);
        //noinspection unchecked
        constructWithAValueAndClass((Class<String>) value.getClass(),
                value);
    }

    @Test
    public void constructedWithAIntegerValueSucceeds() {
        Integer value = 42;
        constructWithAValue(value);
        //noinspection unchecked
        constructWithAValueAndClass((Class<Integer>) value.getClass(),
                value);
    }

    @Test
    public void constructedWithABooleanValueSucceeds() {
        Boolean value = true;
        constructWithAValue(value);
        //noinspection unchecked
        constructWithAValueAndClass((Class<Boolean>) value.getClass(),
                value);
    }

    @Test
    public void constructedWithADoubleValueSucceeds() {
        Double value = 3.14;
        constructWithAValue(value);
        //noinspection unchecked
        constructWithAValueAndClass((Class<Double>) value.getClass(),
                value);
    }

    @Test
    public void constructedWithAFloatValueSucceeds() {
        Float value = 3.14f;
        constructWithAValue(value);
        //noinspection unchecked
        constructWithAValueAndClass((Class<Float>) value.getClass(),
                value);
    }

    @Test
    public void constructedWithAListValueSucceeds() {
        List value = Arrays.asList("This is", "an array", "of values");
        constructWithAValue(value);
        //noinspection unchecked
        constructWithAValueAndClass((Class<List>) value.getClass(),
                value);
    }

    @Test
    public void constructedWithAMapValueSucceds() {
        HashMap<Integer, String> value = new HashMap<Integer, String>();
        value.put(42, "forty two");
        value.put(13, "thirteen");
        value.put(9000, "way too many");
        constructWithAValue(value);

        //noinspection unchecked
        constructWithAValueAndClass((Class<HashMap>) value.getClass(),
                value);
    }

    @Test
    public void constructedWithATupleValueSucceeds() {
        Tuple value = Tuple.of(42, "zwei und vierzig", true, 42.0);
        constructWithAValue(value);
        //noinspection unchecked
        constructWithAValueAndClass((Class<Tuple>) value.getClass(),
                value);
    }

    @Test
    public void constructedWithAByteBufferValueSucceeds() {
        ByteBuffer originalBuffer = ByteBuffer.wrap("Coucou les amis".getBytes());

        // Need to duplicate the buffer in order to compare remaining elements.
        ByteBuffer consumedBuffer = originalBuffer.duplicate();

        Property<ByteBuffer> prop = new Property<ByteBuffer>(consumedBuffer);
        try {
            assertEquals(originalBuffer, prop.getValue().get());
        } catch (ExecutionException e) {
            fail(e.getMessage());
        }

        // Reduplicate the buffer since consumedBuffer has no remaining elements.
        consumedBuffer = originalBuffer.duplicate();
        prop = new Property<ByteBuffer>(ByteBuffer.class, consumedBuffer);

        try {
            assertEquals(originalBuffer, prop.getValue().get());
        } catch (ExecutionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void constructedWithAByteArrayValueSucceeds() {
        byte[] value = "Coucou les amis".getBytes();
        Property<byte[]> prop = new Property<byte[]>(value);

        constructWithAValueAndClass((Class<byte[]>) value.getClass(), value);
    }

    @Test
    public void constructedWithAnEmptyByteArrayValueSucceeds() {
        byte[] value = new byte[0];
        Property<byte[]> prop = new Property<byte[]>(value);

        constructWithAValueAndClass((Class<byte[]>) value.getClass(), value);
    }

    @Test
    public void setPropertyWithNullValueThrows() {
        final Property<Integer> prop = new Property<Integer>(42);
        assertThrowsNullPointerException(new Function<Void, Void>() {
            @Override
            public Void execute(Void v) {
                prop.setValue(null);
                return null;
            }
        });
    }

    @QiStruct
    static class QiStructForTest {
        @QiField(0)
        int i;
        @QiField(1)
        String s;

        QiStructForTest() {
        }

        QiStructForTest(int i, String s) {
            this.i = i;
            this.s = s;
        }

        /**
         * Customized equality operator to make assertion work.
         *
         * @param obj
         * @return
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof QiStructForTest))
                return false;

            QiStructForTest casted = (QiStructForTest) obj;
            return casted.i == i && casted.s.equals(s);
        }
    }

    @Test
    public void constructedWithAStructValueSucceeds() {
        QiStructForTest value = new QiStructForTest(42, "toto");
        constructWithAValue(value);
        //noinspection unchecked
        constructWithAValueAndClass((Class<QiStructForTest>) value.getClass(), value);
    }


    @Test
    public void constructedWithValueDefaultSerializer() throws ExecutionException {
        Property<Integer> property = new Property<Integer>(42);
        assertEquals(property.getValue().get(), new Integer(42));

        property.setValue(0);
        assertEquals(property.getValue().get(), new Integer(0));
    }

    @Test
    public void constructedWithValueMockedSerializer() throws ExecutionException {
        QiSerializer mockedSerializer = mock(QiSerializer.class);
        when(mockedSerializer.serialize(42)).thenReturn(42);
        when(mockedSerializer.serialize(0)).thenReturn(0);
        when(mockedSerializer.deserialize(42, Integer.class)).thenReturn(0);
        when(mockedSerializer.deserialize(0, Integer.class)).thenReturn(-1);

        Property<Integer> property = new Property<Integer>(42, mockedSerializer);
        assertEquals(property.getValue().get(), new Integer(0));

        property.setValue(0);
        assertEquals(property.getValue().get(), new Integer(-1));
    }
}