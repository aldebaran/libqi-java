/*
*  Copyright (C) 2012 Aldebaran Robotics
*/

#ifndef _QIMESSAGING_SIGNAL_HXX_
#define _QIMESSAGING_SIGNAL_HXX_

#include <boost/fusion/functional/generation/make_unfused.hpp>
#include <boost/fusion/functional/invocation/invoke_procedure.hpp>

namespace qi
{
  namespace detail
  {
    struct Appender
  {
    inline Appender(std::vector<Value>*target)
    :target(target)
    {
    }
    template<typename T>
    void
    operator() (const T &v) const
    {
      target->push_back(AutoValue(v));
    }

    std::vector<Value>* target;
  };
  template<typename T>
  struct FusedEmit
  {
    typedef typename boost::function_types::result_type<T>::type RetType;
    typedef typename boost::function_types::parameter_types<T>::type ArgsType;
    typedef typename boost::mpl::push_front<ArgsType, SignalBase*>::type InstArgsType;
    typedef typename boost::mpl::push_front<InstArgsType, RetType>::type FullType;
    typedef typename boost::function_types::function_type<FullType>::type LinearizedType;
    FusedEmit(Signal<T>& signal)
    : _signal(signal) {}

    template <class Seq>
    struct result
    {
      typedef typename boost::function_types::result_type<T>::type type;
    };
    template <class Seq>
    typename result<Seq>::type
    operator()(Seq const & s) const
    {
      std::vector<Value> args;
      boost::fusion::for_each(s, Appender(&args));
      _signal.trigger(MetaFunctionParameters(args));
    }
    Signal<T>& _signal;
  };
  } // detail

  template<typename T>
  Signal<T>::Signal()
  : SignalBase(detail::functionArgumentsSignature<T>())
  {
    detail::FusedEmit<T> fusor = detail::FusedEmit<T>(*this);
    * (boost::function<T>*)this = boost::fusion::make_unfused(fusor);
  }
} // qi
#endif