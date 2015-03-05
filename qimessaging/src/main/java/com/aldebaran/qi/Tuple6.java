/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;public class Tuple6 <T0, T1, T2, T3, T4, T5> extends Tuple {public T0 var0;public T1 var1;public T2 var2;public T3 var3;public T4 var4;public T5 var5;public Tuple6() {var0 = null;var1 = null;var2 = null;var3 = null;var4 = null;var5 = null;}public Tuple6(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) {var0 = arg0;var1 = arg1;var2 = arg2;var3 = arg3;var4 = arg4;var5 = arg5;}
  public <T> T get(int i) throws IndexOutOfBoundsException, ClassCastException, IllegalArgumentException, IllegalAccessException
  {
    return super.get(i);
  }

  public <T> void set(int index, T value) throws IllegalArgumentException, IllegalAccessException
  {
    super.<T>set(index, value);
  }
}
