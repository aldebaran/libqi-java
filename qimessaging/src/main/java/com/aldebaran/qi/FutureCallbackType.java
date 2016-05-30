package com.aldebaran.qi;

public enum FutureCallbackType
{
  // keep values synchronized with qi::FutureCallbackType in libqi/qi/detail/future_fwd.hpp
  Sync(0), Async(1), Auto(2);

  int nativeValue;

  private FutureCallbackType(int nativeValue)
  {
    this.nativeValue = nativeValue;
  }
}
