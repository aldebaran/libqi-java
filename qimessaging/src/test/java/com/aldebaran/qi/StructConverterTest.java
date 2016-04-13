package com.aldebaran.qi;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class StructConverterTest
{
  @QiStruct
  static class Person
  {
    String firstName;
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
  public void testTupleToStruct() throws QiConversionException
  {
    Tuple tuple = Tuple.of("aaa", "bbb", 12);
    Person person = StructConverter.tupleToStruct(tuple, Person.class);
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
  }

  @Test
  public void testTuplesToStructs() throws QiConversionException
  {
    Tuple tuple = Tuple.of("aaa", "bbb", 12);
    Person person = (Person) StructConverter.tuplesToStructs(tuple, Person.class);
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
    @SuppressWarnings("unchecked")
    Tuple unchanged = (Tuple) StructConverter.tuplesToStructs(tuple, Tuple.class);
    assertEquals("aaa", unchanged.get(0));
    assertEquals("bbb", unchanged.get(1));
    assertEquals(12, unchanged.get(2));
  }

  @Test
  public void testTuplesToStructsInList() throws QiConversionException
  {
    List<Tuple> tuples = new ArrayList<Tuple>();
    tuples.add(Tuple.of("aaa", "bbb", 12));
    tuples.add(Tuple.of("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    List<Person> persons = (List<Person>) StructConverter.tuplesToStructsInList(tuples, Person.class);
    Person person = persons.get(0);
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
  }

  @Test
  public void testListTuplesToStructs() throws QiConversionException
  {
    List<Tuple> tuples = new ArrayList<Tuple>();
    tuples.add(Tuple.of("aaa", "bbb", 12));
    tuples.add(Tuple.of("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    List<Person> persons = (List<Person>) StructConverter.tuplesToStructs(tuples, getListOfPersonsType());
    Person person = persons.get(0);
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
    @SuppressWarnings("unchecked")
    List<Tuple> unchangedTuples = (List<Tuple>) StructConverter.tuplesToStructs(tuples, List.class);
    Tuple unchanged = unchangedTuples.get(0);
    assertEquals("aaa", unchanged.get(0));
    assertEquals("bbb", unchanged.get(1));
    assertEquals(12, unchanged.get(2));
  }

  @Test
  public void testTuplesToStructsInMap() throws QiConversionException
  {
    Map<String, Tuple> tuples = new HashMap<String, Tuple>();
    tuples.put("first", Tuple.of("aaa", "bbb", 12));
    tuples.put("second", Tuple.of("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    Map<String, Person> persons = (Map<String, Person>) StructConverter.tuplesToStructsInMap(tuples, String.class,
        Person.class);
    Person person = persons.get("first");
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
  }

  @Test
  public void testMapTuplesToStructs() throws QiConversionException
  {
    Map<String, Tuple> tuples = new HashMap<String, Tuple>();
    tuples.put("first", Tuple.of("aaa", "bbb", 12));
    tuples.put("second", Tuple.of("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    Map<String, Person> persons = (Map<String, Person>) StructConverter.tuplesToStructs(tuples,
        getMapOfStringPersonsType());
    Person person = persons.get("first");
    assertEquals("aaa", person.firstName);
    assertEquals("bbb", person.lastName);
    assertEquals(12, person.age);
    @SuppressWarnings("unchecked")
    Map<String, Tuple> unchangedTuples = (Map<String, Tuple>) StructConverter.tuplesToStructs(tuples, Map.class);
    Tuple unchanged = unchangedTuples.get("first");
    assertEquals("aaa", unchanged.get(0));
    assertEquals("bbb", unchanged.get(1));
    assertEquals(12, unchanged.get(2));
  }

  @Test
  public void testStructToTuple() throws QiConversionException
  {
    Person person = new Person("aaa", "bbb", 12);
    Tuple tuple = StructConverter.structToTuple(person);
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testStructsToTuples() throws QiConversionException
  {
    Person person = new Person("aaa", "bbb", 12);
    Tuple tuple = (Tuple) StructConverter.structsToTuples(person);
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testStructsToTuplesInList() throws QiConversionException
  {
    List<Person> persons = new ArrayList<Person>();
    persons.add(new Person("aaa", "bbb", 12));
    persons.add(new Person("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    List<Tuple> tuples = (List<Tuple>) StructConverter.structsToTuplesInList(persons);
    Tuple tuple = tuples.get(0);
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testListStructsToTuples() throws QiConversionException
  {
    List<Person> persons = new ArrayList<Person>();
    persons.add(new Person("aaa", "bbb", 12));
    persons.add(new Person("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    List<Tuple> tuples = (List<Tuple>) StructConverter.structsToTuples(persons);
    Tuple tuple = tuples.get(0);
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testStructsToTuplesInMap() throws QiConversionException
  {
    Map<String, Person> persons = new HashMap<String, Person>();
    persons.put("first", new Person("aaa", "bbb", 12));
    persons.put("second", new Person("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    Map<String, Tuple> tuples = (Map<String, Tuple>) StructConverter.structsToTuplesInMap(persons);
    Tuple tuple = tuples.get("first");
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  @Test
  public void testMapStructsToTuples() throws QiConversionException
  {
    Map<String, Person> persons = new HashMap<String, Person>();
    persons.put("first", new Person("aaa", "bbb", 12));
    persons.put("second", new Person("ccc", "ddd", 21));
    @SuppressWarnings("unchecked")
    Map<String, Tuple> tuples = (Map<String, Tuple>) StructConverter.structsToTuples(persons);
    Tuple tuple = tuples.get("first");
    assertEquals("aaa", tuple.get(0));
    assertEquals("bbb", tuple.get(1));
    assertEquals(12, tuple.get(2));
  }

  private static Type getListOfPersonsType()
  {
    class X
    {
      List<Person> list;
    }
    return X.class.getDeclaredFields()[0].getGenericType();
  }

  private static Type getMapOfStringPersonsType()
  {
    class X
    {
      Map<String, Person> map;
    }
    return X.class.getDeclaredFields()[0].getGenericType();
  }
}
