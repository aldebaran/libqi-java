/*
** Author(s):
**  - Laurent LEC <llec@aldebaran-robotics.com>
**
** Copyright (C) 2012 Aldebaran Robotics
*/

#include <qi/log.hpp>
#include <qimessaging/qt/QiTransportSocket>
#include "src/qitransportsocket_p.h"
#include <QTcpSocket>
#include <QHostAddress>
#include <qimessaging/message.hpp>
#include <qimessaging/buffer.hpp>
#include "../src/message_p.hpp"

QiTransportSocketPrivate::QiTransportSocketPrivate(QiTransportSocket* self)
  : _self(self)
  , _device(0)
  , _msg(0)
{
}

QiTransportSocketPrivate::~QiTransportSocketPrivate()
{
  if (_msg)
  {
    delete _msg;
  }

  foreach (qi::Message* msg, _pendingMessages)
  {
    delete msg;
  }

  delete _device;
}

void QiTransportSocketPrivate::read()
{
  while (true)
  {
    if (_msg == 0)
    {
      _msg = new qi::Message();
      _readHdr = true;
    }

    if (_readHdr)
    {
      if (_device->bytesAvailable() >= sizeof(qi::MessagePrivate::MessageHeader))
      {
        _device->read(static_cast<char*>(_msg->_p->getHeader()),
                      sizeof(qi::MessagePrivate::MessageHeader));

        if (!_msg->isValid())
        {
          qiLogError("QiTransportSocket") << "incorrect message, dropped";
          // TODO: implement error recovery...
          return;
        }

        _readHdr = false;
      }
      else
      {
        break;
      }
    }

    if (!_readHdr)
    {
      qint64 bufferSize = static_cast<qi::MessagePrivate::MessageHeader*>(_msg->_p->getHeader())->size;

      if (_device->bytesAvailable() >= bufferSize)
      {
        qi::Buffer buffer;
        buffer.reserve(bufferSize);
        _msg->setBuffer(buffer);

        _device->read(static_cast<char*>(buffer.data()), bufferSize);

        _pendingMessages.append(_msg);

        _readHdr = true;

        emit readyRead();
      }
      else
      {
        break;
      }
    }
  }
}

QiTransportSocket::QiTransportSocket(QObject *parent)
  : QObject(parent)
  , _p(new QiTransportSocketPrivate(this))
{
  connect(_p, SIGNAL(readyRead()), this, SIGNAL(readyRead()));
}

QiTransportSocket::~QiTransportSocket()
{
  close();
  delete _p;
}

void QiTransportSocket::close()
{
  if (_p->_device)
  {
    _p->_device->close();
  }
}

void QiTransportSocket::write(const qi::Message& message)
{
  qint64 writtenSize = 0;

  message._p->complete();

  writtenSize = _p->_device->write(static_cast<char*>(message._p->getHeader()),
                                   sizeof(qi::MessagePrivate::MessageHeader));
  if (writtenSize != sizeof(qi::MessagePrivate::MessageHeader))
  {
    qiLogError("QiTransportSocket") << "write error, (" << writtenSize << ")" << _p->_device->errorString().toUtf8().constData();
  }

  writtenSize = _p->_device->write(static_cast<char*>(message._p->buffer.data()),
                                   message._p->buffer.size());
  if (writtenSize != message._p->buffer.size())
  {
    qiLogError("QiTransportSocket") << "write error, (" << writtenSize << ")";
  }
}

qi::Message *QiTransportSocket::read()
{
  return _p->_pendingMessages.empty() ? 0 : _p->_pendingMessages.dequeue();
}

void QiTransportSocket::connectToHost(const QUrl& address)
{
  if (address.scheme() == "tcp")
  {
    QTcpSocket* socket = new QTcpSocket(this);
    connect(socket, SIGNAL(connected()), this, SIGNAL(connected()));
    connect(socket, SIGNAL(disconnected()), this, SIGNAL(disconnected()));
    connect(socket, SIGNAL(readyRead()), _p, SLOT(read()));

    QHostAddress host(address.host());
    socket->connectToHost(host, address.port());
    _p->_peer = address;
    _p->_device = socket;
  }
}

QUrl QiTransportSocket::peer()
{
  return _p->_peer;
}

QiTransportSocket::SocketState QiTransportSocket::state() const
{
  if (!_p->_device)
  {
    return SocketState_Unconnected;
  }

  /*
   * This function must return a SocketState, which is mirroring the values
   * of QAbstractSocket.
   * If the socket is a QAbstractSocket, we can directly return the state
   * given by the API.
   */
  QAbstractSocket *socket;
  if ((socket = dynamic_cast<QAbstractSocket*>(_p->_device)))
  {
    return static_cast<SocketState>(socket->state());
  }

  /*
   * Just in case...
   */
  return SocketState_Unknown;
}