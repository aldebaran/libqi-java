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
