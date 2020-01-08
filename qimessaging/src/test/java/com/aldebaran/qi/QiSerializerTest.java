package com.aldebaran.qi;

import com.aldebaran.qi.serialization.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Type;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
class QiSerializerTest {
    @Ignore
    @QiStruct
    static class Person {
        @QiField(0)
        String firstName;
        @SuppressWarnings("unused")
        int mustBeIgnored = 0;
        @QiField(1)
        String lastName;
        @QiField(2)
        int age;

        @SuppressWarnings("unused")
        Person() {
            firstName = "";
            lastName = "";
            age = -1;
        }

        Person(String firstName, String lastName, int age) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            try {
                Person other = (Person) o;
                return firstName.equals(other.firstName) &&
                        lastName.equals(other.lastName) && age == other.age;
            } catch (ClassCastException err) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, lastName, age);
        }
    }

    @Ignore
    static abstract class TestsBase {
        static QiSerializer serializer = QiSerializer.getDefault();

        interface SerializerProxy {
            <T> T serialize(Object obj) throws QiConversionException;

            <T> T deserialize(Object obj, Type target)
                    throws QiConversionException;
        }

        static class Converter implements SerializerProxy {
            QiSerializer.Converter converter;

            Converter(QiSerializer.Converter converter) {
                this.converter = converter;
            }

            @Override
            public <T> T serialize(Object obj) throws QiConversionException {
                return Objects.uncheckedCast(converter.serialize(serializer, obj));
            }

            @Override
            public <T> T deserialize(Object obj, Type target)
                    throws QiConversionException {
                return Objects.uncheckedCast(converter.deserialize(serializer, obj, target));
            }
        }

        static class Serializer implements SerializerProxy {
            @Override
            public <T> T serialize(Object obj) throws QiConversionException {
                return Objects.uncheckedCast(serializer.serialize(obj));
            }

            @Override
            public <T> T deserialize(Object obj, Type target)
                    throws QiConversionException {
                return Objects.uncheckedCast(serializer.deserialize(obj, target));
            }
        }
    }

    public static class TupleTests extends TestsBase {
        StructConverter converter = new StructConverter();

        @Test
        public void deserializeFromConverter() throws QiConversionException {
            Tuple tuple = Tuple.of("aaa", "bbb", 12);
            assertTrue(converter.canDeserialize(tuple, Person.class));
            Person person = (Person) converter
                    .deserialize(serializer, tuple, Person.class);
            assertEquals("aaa", person.firstName);
            assertEquals("bbb", person.lastName);
            assertEquals(12, person.age);
        }

        @Test
        public void deserialize() throws QiConversionException {
            Tuple tuple = Tuple.of("aaa", "bbb", 12);
            Person person =
                    (Person) serializer.deserialize(tuple, Person.class);
            assertEquals("aaa", person.firstName);
            assertEquals("bbb", person.lastName);
            assertEquals(12, person.age);
            Tuple unchanged =
                    (Tuple) serializer.deserialize(tuple, Tuple.class);
            assertEquals("aaa", unchanged.get(0));
            assertEquals("bbb", unchanged.get(1));
            assertEquals(12, unchanged.get(2));
        }
    }

    public static class ListTests extends TestsBase {
        ListConverter converter = new ListConverter();

        static Type getListOfPersonsType() {
            return new TypeToken<List<Person>>() {
            }.getType();
        }

        @Test
        public void deserializeFromConverter() throws QiConversionException {
            List<Tuple> tuples = new ArrayList<Tuple>();
            tuples.add(Tuple.of("aaa", "bbb", 12));
            tuples.add(Tuple.of("ccc", "ddd", 21));
            Type listOfPersonsType = getListOfPersonsType();
            assertTrue(converter.canDeserialize(tuples, listOfPersonsType));
            List<Person> persons = Objects.uncheckedCast(converter
                            .deserialize(serializer, tuples, listOfPersonsType));
            Person person = persons.get(0);
            assertEquals("aaa", person.firstName);
            assertEquals("bbb", person.lastName);
            assertEquals(12, person.age);
        }

        @Test
        public void deserialize() throws QiConversionException {
            List<Tuple> tuples = new ArrayList<Tuple>();
            tuples.add(Tuple.of("aaa", "bbb", 12));
            tuples.add(Tuple.of("ccc", "ddd", 21));
            List<Person> persons = Objects.uncheckedCast(serializer
                            .deserialize(tuples, getListOfPersonsType()));
            Person person = persons.get(0);
            assertEquals("aaa", person.firstName);
            assertEquals("bbb", person.lastName);
            assertEquals(12, person.age);
            List<Tuple> unchangedTuples =
                    Objects.uncheckedCast(serializer.deserialize(tuples, List.class));
            Tuple unchanged = unchangedTuples.get(0);
            assertEquals("aaa", unchanged.get(0));
            assertEquals("bbb", unchanged.get(1));
            assertEquals(12, unchanged.get(2));
        }

        @Test
        public void serializeFromConverter() throws QiConversionException {
            List<Person> persons = new ArrayList<Person>();
            persons.add(new Person("aaa", "bbb", 12));
            persons.add(new Person("ccc", "ddd", 21));
            assertTrue(converter.canSerialize(persons));
            List<Tuple> tuples =
                    Objects.uncheckedCast(converter.serialize(serializer, persons));
            Tuple tuple = tuples.get(0);
            assertEquals("aaa", tuple.get(0));
            assertEquals("bbb", tuple.get(1));
            assertEquals(12, tuple.get(2));
        }

        @Test
        public void serialize() throws QiConversionException {
            List<Person> persons = new ArrayList<Person>();
            persons.add(new Person("aaa", "bbb", 12));
            persons.add(new Person("ccc", "ddd", 21));
            List<Tuple> tuples =
                    Objects.uncheckedCast(serializer.serialize(persons));
            Tuple tuple = tuples.get(0);
            assertEquals("aaa", tuple.get(0));
            assertEquals("bbb", tuple.get(1));
            assertEquals(12, tuple.get(2));
        }
    }

    public static class ArrayTests extends TestsBase {
        ArrayConverter converter = new ArrayConverter();

        @Test
        public void deserializeFromConverter() throws QiConversionException {
            Tuple[] tuples =
                    {Tuple.of("aaa", "bbb", 12), Tuple.of("ccc", "ddd", 21)};
            assertTrue(converter.canDeserialize(tuples, Person[].class));
            Person[] persons = (Person[]) converter
                    .deserialize(serializer, tuples, Person[].class);
            Person person = persons[0];
            assertEquals("aaa", person.firstName);
            assertEquals("bbb", person.lastName);
            assertEquals(12, person.age);
        }

        @Test
        public void deserialize() throws QiConversionException {
            Tuple[] tuples =
                    {Tuple.of("aaa", "bbb", 12), Tuple.of("ccc", "ddd", 21)};
            Person[] persons =
                    (Person[]) serializer.deserialize(tuples, Person[].class);
            Person person = persons[0];
            assertEquals("aaa", person.firstName);
            assertEquals("bbb", person.lastName);
            assertEquals(12, person.age);
            Tuple[] unchangedTuples =
                    (Tuple[]) serializer.deserialize(tuples, Tuple[].class);
            Tuple unchanged = unchangedTuples[0];
            assertEquals("aaa", unchanged.get(0));
            assertEquals("bbb", unchanged.get(1));
            assertEquals(12, unchanged.get(2));
        }

        @Test
        public void serializeFromConverter() throws QiConversionException {
            Person[] persons = {new Person("aaa", "bbb", 12),
                    new Person("ccc", "ddd", 21)};
            assertTrue(converter.canSerialize(persons));
            Object[] tuples = converter.serialize(serializer, persons);
            Tuple tuple = (Tuple) tuples[0];
            assertEquals("aaa", tuple.get(0));
            assertEquals("bbb", tuple.get(1));
            assertEquals(12, tuple.get(2));
        }
    }

    public static class MapTests extends TestsBase {
        MapConverter converter = new MapConverter();

        static Type getMapOfStringPersonsType() {
            return new TypeToken<Map<String, Person>>() {
            }.getType();
        }

        @Test
        public void deserializeFromConverter() throws QiConversionException {
            Map<String, Tuple> tuples = new HashMap<String, Tuple>();
            tuples.put("first", Tuple.of("aaa", "bbb", 12));
            tuples.put("second", Tuple.of("ccc", "ddd", 21));
            Type mapOfStringPersonsType = getMapOfStringPersonsType();
            assertTrue(
                    converter.canDeserialize(tuples, mapOfStringPersonsType));
            Map<String, Person> persons =
                    Objects.uncheckedCast(converter
                            .deserialize(serializer, tuples,
                                    mapOfStringPersonsType));
            Person person = persons.get("first");
            assertEquals("aaa", person.firstName);
            assertEquals("bbb", person.lastName);
            assertEquals(12, person.age);
        }

        @Test
        public void deserialize() throws QiConversionException {
            Map<String, Tuple> tuples = new HashMap<String, Tuple>();
            tuples.put("first", Tuple.of("aaa", "bbb", 12));
            tuples.put("second", Tuple.of("ccc", "ddd", 21));
            Map<String, Person> persons =
                    Objects.uncheckedCast(serializer
                            .deserialize(tuples, getMapOfStringPersonsType()));
            Person person = persons.get("first");
            assertEquals("aaa", person.firstName);
            assertEquals("bbb", person.lastName);
            assertEquals(12, person.age);
            Map<String, Tuple> unchangedTuples =
                    Objects.uncheckedCast(serializer
                            .deserialize(tuples, Map.class));
            Tuple unchanged = unchangedTuples.get("first");
            assertEquals("aaa", unchanged.get(0));
            assertEquals("bbb", unchanged.get(1));
            assertEquals(12, unchanged.get(2));
        }

        @Test
        public void serializeFromConverter() throws QiConversionException {
            Map<String, Person> persons = new HashMap<String, Person>();
            persons.put("first", new Person("aaa", "bbb", 12));
            persons.put("second", new Person("ccc", "ddd", 21));
            assertTrue(converter.canSerialize(persons));
            Map<String, Tuple> tuples =
                    Objects.uncheckedCast(converter
                            .serialize(serializer, persons));
            Tuple tuple = tuples.get("first");
            assertEquals("aaa", tuple.get(0));
            assertEquals("bbb", tuple.get(1));
            assertEquals(12, tuple.get(2));
        }

        @Test
        public void serialize() throws QiConversionException {
            Map<String, Person> persons = new HashMap<String, Person>();
            persons.put("first", new Person("aaa", "bbb", 12));
            persons.put("second", new Person("ccc", "ddd", 21));
            Map<String, Tuple> tuples =
                    Objects.uncheckedCast(serializer.serialize(persons));
            Tuple tuple = tuples.get("first");
            assertEquals("aaa", tuple.get(0));
            assertEquals("bbb", tuple.get(1));
            assertEquals(12, tuple.get(2));
        }
    }

    @RunWith(Enclosed.class)
    public static class OptionalTests extends TestsBase {
        static final OptionalConverter converter = new OptionalConverter();
        static final Tuple tuple = Tuple.of("robert", "paulson", 42);
        static final Optional<Tuple> optTuple = Optional.of(tuple);
        static final Person person = new Person("robert", "paulson", 42);
        static final Optional<Person> optPerson = Optional.of(person);
        static final Type optPersonType = new TypeToken<Optional<Person>>() {
        }.getType();

        public static class WithoutProxy {
            @Test
            public void canDeserialize() {
                assertTrue(converter.canDeserialize(optTuple, optPersonType));
            }

            @Test
            public void deserializeUnchanged() throws QiConversionException {
                Optional<Tuple> optUnchangedTuple = Objects.uncheckedCast(serializer
                        .deserialize(optTuple, Optional.class));
                assertTrue(optUnchangedTuple.isPresent());
                Tuple unchangedTuple = optUnchangedTuple.get();
                assertEquals(tuple, unchangedTuple);
            }
        }

        @RunWith(Parameterized.class)
        public static class WithProxy {
            SerializerProxy proxy;

            public WithProxy(SerializerProxy proxy) {
                this.proxy = proxy;
            }

            @Parameterized.Parameters
            static public Collection proxies() {
                return Arrays.asList(new Object[][]{
                        {new Converter(new OptionalConverter())},
                        {new Serializer()}});
            }

            @Test
            public void serialize() throws QiConversionException {
                Optional<Tuple> optTuple = proxy.serialize(optPerson);
                assertTrue(optTuple.isPresent());
                Tuple tuple = optTuple.get();
                assertEquals(OptionalTests.tuple, tuple);
            }

            @Test
            public void serializeEmpty() throws QiConversionException {
                Optional<Tuple> optTuple = proxy.serialize(Optional.empty());
                assertFalse(optTuple.isPresent());
            }

            @Test
            public void deserialize() throws QiConversionException {
                Optional<Person> optPerson =
                        proxy.deserialize(optTuple, optPersonType);
                assertTrue(optPerson.isPresent());
                Person person = optPerson.get();
                assertEquals(OptionalTests.person, person);
            }

            @Test
            public void deserializeEmpty() throws QiConversionException {
                Optional<Person> optPerson =
                        proxy.deserialize(Optional.empty(), optPersonType);
                assertFalse(optPerson.isPresent());
            }
        }
    }

    public static class StructTests extends TestsBase {
        StructConverter structConverter = new StructConverter();

        @Test
        public void serializeFromConverter() throws QiConversionException {
            Person person = new Person("aaa", "bbb", 12);
            assertTrue(structConverter.canSerialize(person));
            Tuple tuple = structConverter.serialize(serializer, person);
            assertEquals("aaa", tuple.get(0));
            assertEquals("bbb", tuple.get(1));
            assertEquals(12, tuple.get(2));
        }

        @Test
        public void serialize() throws QiConversionException {
            Person person = new Person("aaa", "bbb", 12);
            Tuple tuple = (Tuple) serializer.serialize(person);
            assertEquals("aaa", tuple.get(0));
            assertEquals("bbb", tuple.get(1));
            assertEquals(12, tuple.get(2));
        }
    }
}
