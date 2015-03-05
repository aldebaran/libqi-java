/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.lang.reflect.Field;

/**
 * QiMessaging Tuple implementation.
 * Created to avoid Objects list, Tuple ensures type safety in QiMessaging calls.
 * Warning : Maximum size is limited to 8 elements.
 * @author proullon
 */
public abstract class Tuple
{

  /**
   * Tuple factory.
   * @param objs is a variadic list of tuple member
   * @return an implemented tuple behind Tuple interface.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static Tuple makeTuple(Object ... objs)
  {
    switch (objs.length)
    {
    case 1:
      return new Tuple1(objs[0]);
    case 2:
      return new Tuple2(objs[0], objs[1]);
    case 3:
      return new Tuple3(objs[0], objs[1], objs[2]);
    case 4:
      return new Tuple4(objs[0], objs[1], objs[2], objs[3]);
    case 5:
      return new Tuple5(objs[0], objs[1], objs[2], objs[3], objs[4]);
    case 6:
      return new Tuple6(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5]);
    case 7:
      return new Tuple7(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6]);
    case 8:
      return new Tuple8(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7]);
    case 9:
      return new Tuple9(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8]);
    case 10:
      return new Tuple10(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9]);
    case 11:
      return new Tuple11(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10]);
    case 12:
      return new Tuple12(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11]);
    case 13:
      return new Tuple13(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12]);
    case 14:
      return new Tuple14(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13]);
    case 15:
      return new Tuple15(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14]);
    case 16:
      return new Tuple16(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15]);
    case 17:
      return new Tuple17(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16]);
    case 18:
      return new Tuple18(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17]);
    case 19:
      return new Tuple19(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18]);
    case 20:
      return new Tuple20(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19]);
    case 21:
      return new Tuple21(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20]);
    case 22:
      return new Tuple22(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21]);
    case 23:
      return new Tuple23(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22]);
    case 24:
      return new Tuple24(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22], objs[23]);
    case 25:
      return new Tuple25(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22], objs[23], objs[24]);
    case 26:
      return new Tuple26(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22], objs[23], objs[24], objs[25]);
    case 27:
      return new Tuple27(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22], objs[23], objs[24], objs[25], objs[26]);
    case 28:
      return new Tuple28(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22], objs[23], objs[24], objs[25], objs[26], objs[27]);
    case 29:
      return new Tuple29(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22], objs[23], objs[24], objs[25], objs[26], objs[27], objs[28]);
    case 30:
      return new Tuple30(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22], objs[23], objs[24], objs[25], objs[26], objs[27], objs[28], objs[29]);
    case 31:
      return new Tuple31(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22], objs[23], objs[24], objs[25], objs[26], objs[27], objs[28], objs[29], objs[30]);
    case 32:
      return new Tuple32(objs[0], objs[1], objs[2], objs[3], objs[4], objs[5], objs[6], objs[7], objs[8], objs[9], objs[10], objs[11], objs[12], objs[13], objs[14], objs[15], objs[16], objs[17], objs[18], objs[19], objs[20], objs[21], objs[22], objs[23], objs[24], objs[25], objs[26], objs[27], objs[28], objs[29], objs[30], objs[31]);
    }

    return null;
  }

  /**
   * Tuple getter.
   * @param index
   * @return
   * @throws IndexOutOfBoundsException if index is unknown
   * @throws ClassCastException if given parameter does not fit in tuple element
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  @SuppressWarnings("unchecked")
  public <T> T get(int index) throws IndexOutOfBoundsException, ClassCastException, IllegalArgumentException, IllegalAccessException
  {
    Field[] fields = this.getClass().getFields();

    if (index < fields.length)
    {
      Object t = fields[index].get(this);
      return (T) t;
    }

    throw new IndexOutOfBoundsException("No " + index + " index in " + fields.length + " elements tuple");
  }

  /**
   * Tuple setter.
   * @param index
   * @param value
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public <T> void set(int index, T value) throws IllegalArgumentException, IllegalAccessException
  {
    Field[] fields = this.getClass().getFields();
    if (index < fields.length)
      fields[index].set(this, (T) value);
  }

  /**
   * Return the number of elements in tuple.
   * @return tuple size
   */
  public final int size()
  {
    return this.getClass().getFields().length;
  }

  /**
   * Fancy representation of tuple.
   */
  public final String toString()
  {
    Field[] fields = this.getClass().getFields();
    int index = 0;

    String ret = "(";
    while (index < fields.length)
    {
      Object t;
      try
      {
        t = fields[index].get(this);
      } catch (Exception e)
      {
        return "()";
      }

      ret += t.toString();
      if (index + 1 != fields.length)
        ret += ", ";

      index++;
    }

    ret += ")";
    return ret;

  }
}
