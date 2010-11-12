/*
* handler_pool.cpp
*
*  Created on: Oct 13, 2009 at 2:41:05 PM
*      Author: Jean-Charles DELAY
*      Mail  : jdelay@aldebaran-robotics.com
*/

#include <qi/transport/common/handlers_pool.hpp>
#include <qi/transport/common/i_runnable.hpp>
#include <althread/althreadpool.h>

namespace qi {
  namespace transport {

    HandlersPool::HandlersPool()
    {
      fPool = boost::shared_ptr<AL::ALThreadPool>(
        new AL::ALThreadPool(2, 200, 50, 100, 2));
      fPool->init(2, 200, 50, 100, 5);
    }

    HandlersPool::~HandlersPool() {
      //vfPool.wait();
    }

    void HandlersPool::pushTask(boost::shared_ptr<IRunnable> handler)
    {
      fPool->enqueue(handler);
      /* schedule(pool, boost::bind(&Runnable::run, job));*/
    }
  }
}
