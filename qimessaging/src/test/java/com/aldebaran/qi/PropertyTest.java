package com.aldebaran.qi;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PropertyTest {

    private <T> void canBeConstructedWithAValue(T value) {
        Property<T> prop = new Property<T>(value);
        try {
            assertEquals(value, prop.getValue().get());
        } catch (ExecutionException e) {
            fail(e.getMessage());
        }
    }

    private <T> void canBeConstructedWithAValueAndClass(Class<T> cls, T value) {
        Property<T> prop = new Property<T>(cls, value);
        try {
            assertEquals(value, prop.getValue().get());
        } catch (ExecutionException e) {
            fail(e.getMessage());
        }
    }

    private void assertThrowsNullPointerException(Function<Void, Void> function) {
        try {
            function.execute(null);
        }
        catch (NullPointerException ex) {
            // Success.
        }
        catch (Throwable throwable) {
            fail("The call threw an exception that was not " +
                    "NullPointerException: " + throwable.getMessage());
        }
    }

    @Test
    public void constructedWithNullValueThrows() {
        assertThrowsNullPointerException(new Function<Void, Void>() {
            @Override
            public Void execute(Void v) throws Throwable {
                new Property<Integer>((Integer)null);
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
        final Class<Integer> cls = (Class<Integer>)value.getClass();

        assertThrowsNullPointerException(new Function<Void, Void>() {
            @Override
            public Void execute(Void v) {
                new Property<Integer>((Class<Integer>) null, value);
                return null;
            }
        });
        assertThrowsNullPointerException(new Function<Void, Void>() {
            @Override
            public Void execute(Void v) {
                new Property<Integer>(cls, null);
                return null;
            }
        });
    }

    @Test
    public void canBeConstructedWithAStringValue() {
        String value = "Hello world !";
        canBeConstructedWithAValue(value);
        //noinspection unchecked
        canBeConstructedWithAValueAndClass((Class<String>)value.getClass(),
                value);
    }

    @Test
    public void canBeConstructedWithAIntegerValue() {
        Integer value = 42;
        canBeConstructedWithAValue(value);
        //noinspection unchecked
        canBeConstructedWithAValueAndClass((Class<Integer>)value.getClass(),
                value);
    }

    @Test
    public void canBeConstructedWithABooleanValue() {
        Boolean value = true;
        canBeConstructedWithAValue(value);
        //noinspection unchecked
        canBeConstructedWithAValueAndClass((Class<Boolean>)value.getClass(),
                value);
    }

    @Test
    public void canBeConstructedWithADoubleValue() {
        Double value = 3.14;
        canBeConstructedWithAValue(value);
        //noinspection unchecked
        canBeConstructedWithAValueAndClass((Class<Double>)value.getClass(),
                value);
    }

    @Test
    public void canBeConstructedWithAFloatValue() {
        Float value = 3.14f;
        canBeConstructedWithAValue(value);
        //noinspection unchecked
        canBeConstructedWithAValueAndClass((Class<Float>)value.getClass(),
                value);
    }

    @Test
    public void canBeConstructedWithAListValue() {
        List value = Arrays.asList("This is", "an array", "of values");
        canBeConstructedWithAValue(value);
        //noinspection unchecked
        canBeConstructedWithAValueAndClass((Class<List>)value.getClass(),
                value);
    }

    @Test
    public void canBeConstructedWithAMapValue() {
        HashMap<Integer, String> value = new HashMap<Integer, String>();
        value.put(42, "forty two");
        value.put(13, "thirteen");
        value.put(9000, "way too many");
        canBeConstructedWithAValue(value);

        //noinspection unchecked
        canBeConstructedWithAValueAndClass((Class<HashMap>)value.getClass(),
                value);
    }

    @Test
    public void canBeConstructedWithATupleValue() {
        Tuple value = Tuple.of(42, "zwei und vierzig", true, 42.0);
        canBeConstructedWithAValue(value);
        //noinspection unchecked
        canBeConstructedWithAValueAndClass((Class<Tuple>)value.getClass(),
                value);
    }
}
