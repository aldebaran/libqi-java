package com.aldebaran.qi;

import com.aldebaran.qi.serialization.*;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QiSerializerTest {
    @QiStruct
    public static class Person {
        @QiField(0)
        String firstName;
        int mustBeIgnored;
        @QiField(1)
        String lastName;
        @QiField(2)
        int age;

        public Person() {
        }

        public Person(String firstName, String lastName, int age) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }
    }

    private QiSerializer serializer;

    @Before
    public void setUp() {
        serializer = QiSerializer.getDefault();
    }

    @Test
    public void testDeserializeTuple() throws QiConversionException {
        Tuple tuple = Tuple.of("aaa", "bbb", 12);
        StructConverter structConverter = new StructConverter();
        assertTrue(structConverter.canDeserialize(tuple, Person.class));
        Person person = (Person) structConverter.deserialize(serializer, tuple, Person.class);
        assertEquals("aaa", person.firstName);
        assertEquals("bbb", person.lastName);
        assertEquals(12, person.age);
    }

    @Test
    public void testDeserialize() throws QiConversionException {
        Tuple tuple = Tuple.of("aaa", "bbb", 12);
        Person person = (Person) serializer.deserialize(tuple, Person.class);
        assertEquals("aaa", person.firstName);
        assertEquals("bbb", person.lastName);
        assertEquals(12, person.age);
        Tuple unchanged = (Tuple) serializer.deserialize(tuple, Tuple.class);
        assertEquals("aaa", unchanged.get(0));
        assertEquals("bbb", unchanged.get(1));
        assertEquals(12, unchanged.get(2));
    }

    @Test
    public void testDeserializeList() throws QiConversionException {
        List<Tuple> tuples = new ArrayList<Tuple>();
        tuples.add(Tuple.of("aaa", "bbb", 12));
        tuples.add(Tuple.of("ccc", "ddd", 21));
        Type listOfPersonsType = getListOfPersonsType();
        ListConverter listConverter = new ListConverter();
        assertTrue(listConverter.canDeserialize(tuples, listOfPersonsType));
        @SuppressWarnings("unchecked")
        List<Person> persons = (List<Person>) listConverter.deserialize(serializer, tuples, listOfPersonsType);
        Person person = persons.get(0);
        assertEquals("aaa", person.firstName);
        assertEquals("bbb", person.lastName);
        assertEquals(12, person.age);
    }

    @Test
    public void testDeserializeWithList() throws QiConversionException {
        List<Tuple> tuples = new ArrayList<Tuple>();
        tuples.add(Tuple.of("aaa", "bbb", 12));
        tuples.add(Tuple.of("ccc", "ddd", 21));
        @SuppressWarnings("unchecked")
        List<Person> persons = (List<Person>) serializer.deserialize(tuples, getListOfPersonsType());
        Person person = persons.get(0);
        assertEquals("aaa", person.firstName);
        assertEquals("bbb", person.lastName);
        assertEquals(12, person.age);
        @SuppressWarnings("unchecked")
        List<Tuple> unchangedTuples = (List<Tuple>) serializer.deserialize(tuples, List.class);
        Tuple unchanged = unchangedTuples.get(0);
        assertEquals("aaa", unchanged.get(0));
        assertEquals("bbb", unchanged.get(1));
        assertEquals(12, unchanged.get(2));
    }

    @Test
    public void testDeserializeArray() throws QiConversionException {
        Tuple[] tuples =
                {
                        Tuple.of("aaa", "bbb", 12), Tuple.of("ccc", "ddd", 21)
                };
        ArrayConverter arrayConverter = new ArrayConverter();
        assertTrue(arrayConverter.canDeserialize(tuples, Person[].class));
        Person[] persons = (Person[]) arrayConverter.deserialize(serializer, tuples, Person[].class);
        Person person = persons[0];
        assertEquals("aaa", person.firstName);
        assertEquals("bbb", person.lastName);
        assertEquals(12, person.age);
    }

    @Test
    public void testDeserializeWithArray() throws QiConversionException {
        Tuple[] tuples =
                {
                        Tuple.of("aaa", "bbb", 12), Tuple.of("ccc", "ddd", 21)
                };
        Person[] persons = (Person[]) serializer.deserialize(tuples, Person[].class);
        Person person = persons[0];
        assertEquals("aaa", person.firstName);
        assertEquals("bbb", person.lastName);
        assertEquals(12, person.age);
        Tuple[] unchangedTuples = (Tuple[]) serializer.deserialize(tuples, Tuple[].class);
        Tuple unchanged = unchangedTuples[0];
        assertEquals("aaa", unchanged.get(0));
        assertEquals("bbb", unchanged.get(1));
        assertEquals(12, unchanged.get(2));
    }

    @Test
    public void testDeserializeMap() throws QiConversionException {
        Map<String, Tuple> tuples = new HashMap<String, Tuple>();
        tuples.put("first", Tuple.of("aaa", "bbb", 12));
        tuples.put("second", Tuple.of("ccc", "ddd", 21));
        Type mapOfStringPersonsType = getMapOfStringPersonsType();
        MapConverter mapConverter = new MapConverter();
        assertTrue(mapConverter.canDeserialize(tuples, mapOfStringPersonsType));
        @SuppressWarnings("unchecked")
        Map<String, Person> persons = (Map<String, Person>) mapConverter.deserialize(serializer, tuples,
                mapOfStringPersonsType);
        Person person = persons.get("first");
        assertEquals("aaa", person.firstName);
        assertEquals("bbb", person.lastName);
        assertEquals(12, person.age);
    }

    @Test
    public void testDeserializeWithMap() throws QiConversionException {
        Map<String, Tuple> tuples = new HashMap<String, Tuple>();
        tuples.put("first", Tuple.of("aaa", "bbb", 12));
        tuples.put("second", Tuple.of("ccc", "ddd", 21));
        @SuppressWarnings("unchecked")
        Map<String, Person> persons = (Map<String, Person>) serializer.deserialize(tuples, getMapOfStringPersonsType());
        Person person = persons.get("first");
        assertEquals("aaa", person.firstName);
        assertEquals("bbb", person.lastName);
        assertEquals(12, person.age);
        @SuppressWarnings("unchecked")
        Map<String, Tuple> unchangedTuples = (Map<String, Tuple>) serializer.deserialize(tuples, Map.class);
        Tuple unchanged = unchangedTuples.get("first");
        assertEquals("aaa", unchanged.get(0));
        assertEquals("bbb", unchanged.get(1));
        assertEquals(12, unchanged.get(2));
    }

    @Test
    public void testSerializeStruct() throws QiConversionException {
        Person person = new Person("aaa", "bbb", 12);
        StructConverter structConverter = new StructConverter();
        assertTrue(structConverter.canSerialize(person));
        Tuple tuple = structConverter.serialize(serializer, person);
        assertEquals("aaa", tuple.get(0));
        assertEquals("bbb", tuple.get(1));
        assertEquals(12, tuple.get(2));
    }

    @Test
    public void testSerialize() throws QiConversionException {
        Person person = new Person("aaa", "bbb", 12);
        Tuple tuple = (Tuple) serializer.serialize(person);
        assertEquals("aaa", tuple.get(0));
        assertEquals("bbb", tuple.get(1));
        assertEquals(12, tuple.get(2));
    }

    @Test
    public void testSerializeList() throws QiConversionException {
        List<Person> persons = new ArrayList<Person>();
        persons.add(new Person("aaa", "bbb", 12));
        persons.add(new Person("ccc", "ddd", 21));
        ListConverter listConverter = new ListConverter();
        assertTrue(listConverter.canSerialize(persons));
        @SuppressWarnings("unchecked")
        List<Tuple> tuples = (List<Tuple>) listConverter.serialize(serializer, persons);
        Tuple tuple = tuples.get(0);
        assertEquals("aaa", tuple.get(0));
        assertEquals("bbb", tuple.get(1));
        assertEquals(12, tuple.get(2));
    }

    @Test
    public void testSerializeWithList() throws QiConversionException {
        List<Person> persons = new ArrayList<Person>();
        persons.add(new Person("aaa", "bbb", 12));
        persons.add(new Person("ccc", "ddd", 21));
        @SuppressWarnings("unchecked")
        List<Tuple> tuples = (List<Tuple>) serializer.serialize(persons);
        Tuple tuple = tuples.get(0);
        assertEquals("aaa", tuple.get(0));
        assertEquals("bbb", tuple.get(1));
        assertEquals(12, tuple.get(2));
    }

    @Test
    public void testSerializeArray() throws QiConversionException {
        Person[] persons =
                {
                        new Person("aaa", "bbb", 12), new Person("ccc", "ddd", 21)
                };
        ArrayConverter arrayConverter = new ArrayConverter();
        assertTrue(arrayConverter.canSerialize(persons));
        Object[] tuples = arrayConverter.serialize(serializer, persons);
        Tuple tuple = (Tuple) tuples[0];
        assertEquals("aaa", tuple.get(0));
        assertEquals("bbb", tuple.get(1));
        assertEquals(12, tuple.get(2));
    }

    @Test
    public void testSerializeWithArray() throws QiConversionException {
        Person[] persons =
                {
                        new Person("aaa", "bbb", 12), new Person("ccc", "ddd", 21)
                };
        Object[] tuples = (Object[]) serializer.serialize(persons);
        Tuple tuple = (Tuple) tuples[0];
        assertEquals("aaa", tuple.get(0));
        assertEquals("bbb", tuple.get(1));
        assertEquals(12, tuple.get(2));
    }

    @Test
    public void testSerializeMap() throws QiConversionException {
        Map<String, Person> persons = new HashMap<String, Person>();
        persons.put("first", new Person("aaa", "bbb", 12));
        persons.put("second", new Person("ccc", "ddd", 21));
        MapConverter mapConverter = new MapConverter();
        assertTrue(mapConverter.canSerialize(persons));
        @SuppressWarnings("unchecked")
        Map<String, Tuple> tuples = (Map<String, Tuple>) mapConverter.serialize(serializer, persons);
        Tuple tuple = tuples.get("first");
        assertEquals("aaa", tuple.get(0));
        assertEquals("bbb", tuple.get(1));
        assertEquals(12, tuple.get(2));
    }

    @Test
    public void testSerializeWithMap() throws QiConversionException {
        Map<String, Person> persons = new HashMap<String, Person>();
        persons.put("first", new Person("aaa", "bbb", 12));
        persons.put("second", new Person("ccc", "ddd", 21));
        @SuppressWarnings("unchecked")
        Map<String, Tuple> tuples = (Map<String, Tuple>) serializer.serialize(persons);
        Tuple tuple = tuples.get("first");
        assertEquals("aaa", tuple.get(0));
        assertEquals("bbb", tuple.get(1));
        assertEquals(12, tuple.get(2));
    }

    private static Type getListOfPersonsType() {
        return new TypeToken<List<Person>>() {
        }.getType();
    }

    private static Type getMapOfStringPersonsType() {
        return new TypeToken<Map<String, Person>>() {
        }.getType();
    }
}
