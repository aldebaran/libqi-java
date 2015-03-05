/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;public class Tuple16 <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> extends Tuple {public T0 var0;public T1 var1;public T2 var2;public T3 var3;public T4 var4;public T5 var5;public T6 var6;public T7 var7;public T8 var8;public T9 var9;public T10 var10;public T11 var11;public T12 var12;public T13 var13;public T14 var14;public T15 var15;public Tuple16() {var0 = null;var1 = null;var2 = null;var3 = null;var4 = null;var5 = null;var6 = null;var7 = null;var8 = null;var9 = null;var10 = null;var11 = null;var12 = null;var13 = null;var14 = null;var15 = null;}public Tuple16(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8, T9 arg9, T10 arg10, T11 arg11, T12 arg12, T13 arg13, T14 arg14, T15 arg15) {var0 = arg0;var1 = arg1;var2 = arg2;var3 = arg3;var4 = arg4;var5 = arg5;var6 = arg6;var7 = arg7;var8 = arg8;var9 = arg9;var10 = arg10;var11 = arg11;var12 = arg12;var13 = arg13;var14 = arg14;var15 = arg15;}
  public <T> T get(int i) throws IndexOutOfBoundsException, ClassCastException, IllegalArgumentException, IllegalAccessException
  {
    return super.get(i);
  }

  public <T> void set(int index, T value) throws IllegalArgumentException, IllegalAccessException
  {
    super.<T>set(index, value);
  }
}
