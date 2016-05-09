package com.aldebaran.qi;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.aldebaran.qi.serialization.QiSerializer;

public class QiSerializerTest
{
  @QiStruct
  public static class Person
  {
    String firstName;
    transient int mustBeIgnored;
    String lastName;
    int age;

    public Person()
    {
    }

    public Person(String firstName, String lastName, int age)
    {
      this.firstName = firstName;
      this.lastName = lastName;
      this.age = age;
    }
  }

  @Test
  public void testDeserializeTuple() throws QiConversionException
  {
    Tuple tuple = Tuple.of("aaa", "bbb", 12);
    Person person = QiSerializer.deserializeTuple(tuple, Person.class);
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
  }

  @Test
  public void testDeserialize() throws QiConversionException
  {
    Tuple tuple = Tuple.of("aaa", "bbb", 12);
    Person person = (Person) QiSerializer.deserialize(tuple, Person.class);
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
    @SuppressWarnings("unchecked")
    Tuple unchanged = (Tuple) QiSerializer.deserialize(tuple, Tuple.class);
    assertEquals("aaa", unchanged.get(0));
    assertEquals("bbb", unchanged.get(1));
    assertEquals(12, unchanged.get(2));
  }

  @Test
  public void testDeserializeList() throws QiConversionException
  {
    List<Tuple> tuples = new ArrayList<Tuple>();
    tuples.add(Tuple.of("aaa", "bbb", 12));
    tuples.add(Tuple.of("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    List<Person> persons = (List<Person>) QiSerializer.deserializeList(tuples, Person.class);
    Person person = persons.get(0);
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
  }

  @Test
  public void testDeserializeWithList() throws QiConversionException
  {
    List<Tuple> tuples = new ArrayList<Tuple>();
    tuples.add(Tuple.of("aaa", "bbb", 12));
    tuples.add(Tuple.of("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    List<Person> persons = (List<Person>) QiSerializer.deserialize(tuples, getListOfPersonsType());
    Person person = persons.get(0);
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
    @SuppressWarnings("unchecked")
    List<Tuple> unchangedTuples = (List<Tuple>) QiSerializer.deserialize(tuples, List.class);
    Tuple unchanged = unchangedTuples.get(0);
    assertEquals("aaa", unchanged.get(0));
    assertEquals("bbb", unchanged.get(1));
    assertEquals(12, unchanged.get(2));
  }

  @Test
  public void testDeserializeMap() throws QiConversionException
  {
    Map<String, Tuple> tuples = new HashMap<String, Tuple>();
    tuples.put("first", Tuple.of("aaa", "bbb", 12));
    tuples.put("second", Tuple.of("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    Map<String, Person> persons = (Map<String, Person>) QiSerializer.deserializeMap(tuples, String.class,
        Person.class);
    Person person = persons.get("first");
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
  }

  @Test
  public void testDeserializeWithMap() throws QiConversionException
  {
    Map<String, Tuple> tuples = new HashMap<String, Tuple>();
    tuples.put("first", Tuple.of("aaa", "bbb", 12));
    tuples.put("second", Tuple.of("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    Map<String, Person> persons = (Map<String, Person>) QiSerializer.deserialize(tuples,
        getMapOfStringPersonsType());
    Person person = persons.get("first");
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
    @SuppressWarnings("unchecked")
    Map<String, Tuple> unchangedTuples = (Map<String, Tuple>) QiSerializer.deserialize(tuples, Map.class);
    Tuple unchanged = unchangedTuples.get("first");
    assertEquals("aaa", unchanged.get(0));
    assertEquals("bbb", unchanged.get(1));
    assertEquals(12, unchanged.get(2));
  }

  @Test
  public void testSerializeStruct() throws QiConversionException
  {
    Person person = new Person("aaa", "bbb", 12);
    Tuple tuple = QiSerializer.serializeStruct(person);
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testSerialize() throws QiConversionException
  {
    Person person = new Person("aaa", "bbb", 12);
    Tuple tuple = (Tuple) QiSerializer.serialize(person);
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testSerializeList() throws QiConversionException
  {
    List<Person> persons = new ArrayList<Person>();
    persons.add(new Person("aaa", "bbb", 12));
    persons.add(new Person("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    List<Tuple> tuples = (List<Tuple>) QiSerializer.serializeList(persons);
    Tuple tuple = tuples.get(0);
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testSerializeWithList() throws QiConversionException
  {
    List<Person> persons = new ArrayList<Person>();
    persons.add(new Person("aaa", "bbb", 12));
    persons.add(new Person("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    List<Tuple> tuples = (List<Tuple>) QiSerializer.serialize(persons);
    Tuple tuple = tuples.get(0);
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testSerializeMap() throws QiConversionException
  {
    Map<String, Person> persons = new HashMap<String, Person>();
    persons.put("first", new Person("aaa", "bbb", 12));
    persons.put("second", new Person("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    Map<String, Tuple> tuples = (Map<String, Tuple>) QiSerializer.serializeMap(persons);
    Tuple tuple = tuples.get("first");
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testSerializeWithMap() throws QiConversionException
  {
    Map<String, Person> persons = new HashMap<String, Person>();
    persons.put("first", new Person("aaa", "bbb", 12));
    persons.put("second", new Person("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    Map<String, Tuple> tuples = (Map<String, Tuple>) QiSerializer.serialize(persons);
    Tuple tuple = tuples.get("first");
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  private static Type getListOfPersonsType()
  {
    return new TypeToken<List<Person>>() {}.getType();
  }

  private static Type getMapOfStringPersonsType()
  {
    return new TypeToken<Map<String, Person>>() {}.getType();
  }
}
