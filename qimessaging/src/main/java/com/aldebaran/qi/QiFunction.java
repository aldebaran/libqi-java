package com.aldebaran.qi;

public interface QiFunction<Ret, Arg>
{
  Future<Ret> execute(Future<Arg> arg) throws Exception;
}
