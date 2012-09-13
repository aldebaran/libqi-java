/*
**
** Copyright (C) 2012 Aldebaran Robotics
*/

#include <src/metasignal_p.hpp>
#include <qimessaging/object.hpp>

namespace qi {

  MetaSignalPrivate::MetaSignalPrivate()
    : _signature(),
      _uid(0)
  {
  }

  MetaSignalPrivate::MetaSignalPrivate(const std::string &sig)
    : _signature(sig),
      _uid(0)
  {
  }

  MetaSignalPrivate::MetaSignalPrivate(const MetaSignalPrivate &rhs)
  {
    _signature = rhs._signature;
    _uid = rhs._uid;
  }

  MetaSignalPrivate& MetaSignalPrivate::operator=(const MetaSignalPrivate &rhs)
  {
    _signature = rhs._signature;
    _uid = rhs._uid;
    return (*this);
  }

}