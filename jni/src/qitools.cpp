#include "qitools.hpp"
#include <qi/future.hpp>

namespace qi {

static void connectFutureToPromise(const qi::Future<qi::AnyValue> future, qi::Promise<qi::AnyValue> promise)
{
  if (future.isCanceled())
    promise.setCanceled();
  else if (future.hasError())
    promise.setError(future.error());
  else
    promise.setValue(future.value());
}

qi::Future<qi::AnyValue> localThen(qi::Future<qi::AnyValue> &future, const ThenCallback &callback)
{
  qi::Promise<qi::AnyValue> promiseToNotify;
  future.connect([promiseToNotify, callback](const qi::Future<qi::AnyValue> &future) {
    qi::Future<qi::AnyValue> nextFuture = callback(future);
    nextFuture.connect([promiseToNotify](const qi::Future<qi::AnyValue> &future) {
      connectFutureToPromise(future, promiseToNotify);
    });
  });
  return promiseToNotify.future();
}

qi::Future<qi::AnyValue> localAndThen(qi::Future<qi::AnyValue> &future, const AndThenCallback &callback)
{
  qi::Promise<qi::AnyValue> promiseToNotify;
  future.connect([promiseToNotify, callback](const qi::Future<qi::AnyValue> &future) {
    if (future.hasError() || future.isCanceled()) {
      connectFutureToPromise(future, promiseToNotify);
    } else {
      qi::Future<qi::AnyValue> nextFuture = callback(future.value());
      nextFuture.connect([promiseToNotify](const qi::Future<qi::AnyValue> &future) {
        connectFutureToPromise(future, promiseToNotify);
      });
    }
  });
  return promiseToNotify.future();
}

} // namespace qi
