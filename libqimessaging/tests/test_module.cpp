/*
**
** Copyright (C) 2012 Aldebaran Robotics
*/

#include <boost/filesystem.hpp>

#include <gtest/gtest.h>

#include <qi/application.hpp>

#include <qimessaging/object.hpp>
#include <qimessaging/session.hpp>
#include <qimessaging/service_directory.hpp>

qi::Session* session;
TEST(Module, Load)
{
  boost::filesystem::path p(qi::Application::program());
  session->loadService(
    p.parent_path().string() + "/../lib/testmodule");
  qi::os::msleep(500);
  qi::Object* o = session->service("test");
  ASSERT_TRUE(o);
  int res = o->call<int>("testMethod", 12);
  ASSERT_EQ(13, res);
}

int main(int argc, char **argv) {
  qi::Application app(argc, argv);
  ::testing::InitGoogleTest(&argc, argv);
  qi::Session s;
  qi::ServiceDirectory sd;
  sd.listen("tcp://localhost:0");
  s.connect(sd.listenUrl());
  s.listen("tcp://localhost:0");
  session = &s;
  return RUN_ALL_TESTS();
}
