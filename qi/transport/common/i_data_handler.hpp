/*
** Author(s):
**  - Jean-Charles DELAY <jdelay@aldebaran-robotics.com>
**  - Cedric GESTES      <gestes@aldebaran-robotics.com>
**  - Chris Kilner       <ckilner@aldebaran-robotics.com>
**
** Copyright (C) 2010 Aldebaran Robotics
*/

#ifndef QI_TRANSPORT_DATAHANDLER_INTERFACE_HPP_
#define QI_TRANSPORT_DATAHANDLER_INTERFACE_HPP_

namespace qi {
  namespace transport {
    class IDataHandler {
    public:
      /// <summary>
      /// Data handler. Used by transport's server to delegate the data handling.
      /// </summary>
      /// <param name="requestData"> The request data. </param>
      /// <param name="responseData"> [in,out] The response data. </param>
      virtual void dataHandler(const std::string &requestData, std::string &responseData) = 0;
    };
  }
}

#endif  // QI_TRANSPORT_DATAHANDLER_HPP_
