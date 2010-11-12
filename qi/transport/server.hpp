/*
** Author(s):
**  - Cedric GESTES <gestes@aldebaran-robotics.com>
**  - Chris Kilner  <ckilner@aldebaran-robotics.com>
**
** Copyright (C) 2010 Aldebaran Robotics
*/

#ifndef QI_TRANSPORT_SERVER_HPP_
# define QI_TRANSPORT_SERVER_HPP_

#include <string>
#include <qi/transport/common/i_threadable.hpp>
#include <qi/transport/common/i_server_response_handler.hpp>
#include <qi/transport/common/i_data_handler.hpp>

namespace qi {
  namespace transport {

    class Server : public IThreadable {
    public:
      explicit Server(const std::string &_serverAddress)
        : _serverAddress(_serverAddress),
          _dataHandler(0) {}
      virtual ~Server() {}

      virtual void setDataHandler(IDataHandler* callback) {
        _dataHandler = callback;
      }

      virtual IDataHandler *getDataHandler() {
        return _dataHandler;
      }

    protected:
      std::string   _serverAddress;
      IDataHandler* _dataHandler;
    };
  }
}

#endif  // QI_TRANSPORT_SERVER_HPP_
