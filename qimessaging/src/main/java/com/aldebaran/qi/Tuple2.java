/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;public class Tuple2 <T0, T1> extends Tuple {public T0 var0;public T1 var1;public Tuple2() {var0 = null;var1 = null;}public Tuple2(T0 arg0, T1 arg1) {var0 = arg0;var1 = arg1;}
  public <T> T get(int i) throws IndexOutOfBoundsException, ClassCastException, IllegalArgumentException, IllegalAccessException
  {
    return super.get(i);
  }

  public <T> void set(int index, T value) throws IllegalArgumentException, IllegalAccessException
  {
    super.<T>set(index, value);
  }
}
