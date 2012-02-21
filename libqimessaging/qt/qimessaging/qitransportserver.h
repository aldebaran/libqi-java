/*
** Author(s):
**  - Laurent LEC   <llec@aldebaran-robotics.com>
**
** Copyright (C) 2012 Aldebaran Robotics
*/

#ifndef   	QT_QITRANSPORTSERVER_H_
# define   	QT_QITRANSPORTSERVER_H_

#include <QtCore/QObject>
#include <qimessaging/qitransportsocket.h>

class QiTransportServerPrivate;

class QiTransportServer : public QObject {
  Q_OBJECT

public:
  QiTransportServer();
  virtual ~QiTransportServer();

  void start(QString            address,
             unsigned short     port,
             struct event_base *base);

  QiTransportSocket *nextPendingConnection();

signals:
  void connected();

protected:
  QiTransportServerPrivate *_p;
};


#endif 	    /* !QITRANSPORTSERVER_H_ */