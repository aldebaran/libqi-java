package com.aldebaran.qi;

public interface FutureFunction<Ret, Arg>
{
  Future<Ret> execute(Future<Arg> future) throws Throwable;
}
