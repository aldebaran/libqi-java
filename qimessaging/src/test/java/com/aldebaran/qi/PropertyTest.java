package com.aldebaran.qi;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class PropertyTest {

    private <T> void canBeConstructedWithAValue(T value) {
        Property<T> prop = new Property<T>(value);
        try {
            Assert.assertEquals(value, prop.getValue().get());
        } catch (ExecutionException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void canBeConstructedWithAValue_String() {
        canBeConstructedWithAValue("Hello world !");
    }

    @Test
    public void canBeConstructedWithAValue_Integer() {
        canBeConstructedWithAValue(42);
    }

    @Test
    public void canBeConstructedWithAValue_Boolean() {
        canBeConstructedWithAValue(true);
    }

    @Test
    public void canBeConstructedWithAValue_Double() {
        canBeConstructedWithAValue(3.14);
    }

    @Test
    public void canBeConstructedWithAValue_Float() {
        canBeConstructedWithAValue(3.14f);
    }

    @Test
    public void canBeConstructedWithAValue_List() {
        canBeConstructedWithAValue(Arrays.asList("This is", "an array", "of values"));
    }

    @Test
    public void canBeConstructedWithAValue_Map() {
        HashMap<Integer, String> m = new HashMap<Integer, String>();
        m.put(42, "forty two");
        m.put(13, "thirteen");
        m.put(9000, "way too many");
        canBeConstructedWithAValue(m);
    }

    @Test
    public void canBeConstructedWithAValue_Tuple() {
        Tuple t = Tuple.of(42, "zwei und vierzig", true, 42.0);
        canBeConstructedWithAValue(t);
    }
}
