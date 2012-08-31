/*
**
** Author(s):
**  - Pierre Roullon <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012 Aldebaran Robotics
*/

#ifndef   	_QIMESSAGING_OBJECT_C_P_H_
# define   	_QIMESSAGING_OBJECT_C_P_H_

#include <qimessaging/functor.hpp>
#include <qimessaging/c/message_c.h>
#include "message_c_p.h"

class CFunctorResultBase : public qi::FunctorResultBase
{
public:
  CFunctorResultBase(const std::string &signature)
    : _signature(signature),
     _promise(new qi::Promise<void *>())
  {
  }
  ~CFunctorResultBase() {}

  inline virtual void setValue(const qi::Buffer &result)
  {
    if (_signature == "v") {
      _promise->setValue((void *) 0);
      return;
    }

    qi_message_data_t *new_message = (qi_message_data_t *) malloc(sizeof(qi_message_data_t));
    if (!new_message)
      _promise->setError("Cannot allocate memory to handle answer");
    else
    {
      memset(new_message, 0, sizeof(qi_message_data_t));
      new_message->buff = new qi::Buffer(result);
      _promise->setValue((void *) new_message);
    }
  }

  inline virtual void setError(const std::string &sig, const qi::Buffer &error) {
    qi::IDataStream ds(error);
    std::string err;
     if (sig != "s") {
      std::stringstream ss;
      ss << "Can't report the correct error message because the error signature is :" << sig;
      _promise->setError(ss.str());
      return;
    }
    ds >> err;
    _promise->setError(err);
  }

public:
  std::string               _signature;
  qi::Promise<void *>       *_promise;
};

class CFunctorResult : public qi::FunctorResult
{
public:
  CFunctorResult(boost::shared_ptr<CFunctorResultBase> base)
    : FunctorResult(base)
  {
    CFunctorResultBase *p = reinterpret_cast<CFunctorResultBase *>(_p.get());
    _future = p->_promise->future();
  }

  ~CFunctorResult()
  {
  }

qi::Future<void *> *future() { return &_future; }

private:
  qi::Future<void *>          _future;
};

class CFunctor : public qi::Functor {
public:
  CFunctor(const char *complete_sig, qi_object_method_t func, void *data = 0)
    : _func(func),
      _complete_sig(strdup(complete_sig)),
      _data(data)
  {
  }

  virtual void call(const qi::FunctorParameters &params, qi::FunctorResult result) const
  {
    qi_message_data_t* message_c = (qi_message_data_t *) malloc(sizeof(qi_message_data_t));
    qi_message_data_t* answer_c = (qi_message_data_t *) malloc(sizeof(qi_message_data_t));

    memset(message_c, 0, sizeof(qi_message_data_t));
    memset(answer_c, 0, sizeof(qi_message_data_t));

    message_c->buff = new qi::Buffer(params.buffer());
    answer_c->buff = new qi::Buffer();

    if (_func)
      _func(_complete_sig, (qi_message_t *) message_c, reinterpret_cast<qi_message_t *>(answer_c), _data);

    result.setValue(*answer_c->buff);
    qi_message_destroy((qi_message_t *) message_c);
    qi_message_destroy((qi_message_t *) answer_c);
  }

  virtual ~CFunctor() {
    free(_complete_sig);
  }

private:
  qi_object_method_t  _func;
  char               *_complete_sig;
  void               *_data;

};

#endif