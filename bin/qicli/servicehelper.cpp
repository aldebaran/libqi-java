#include <boost/date_time.hpp>
#include <boost/regex.hpp>

#include <qi/future.hpp>
#include <qi/iocolor.hpp>
#include <qitype/functiontype.hpp>
#include <boost/foreach.hpp>
#include "servicehelper.hpp"
#include "qicli.hpp"


ServiceHelper::ServiceHelper(const qi::ObjectPtr &service, const std::string &name)
  :_name(name),
    _service(service)

{}

ServiceHelper::ServiceHelper(const ServiceHelper &other)
  :_name(other._name),
    _service(other._service)
{
}

std::ostream &operator<<(std::ostream &os, const std::vector<qi::GenericValuePtr> &gvv)
{
  for (unsigned int i = 0; i < gvv.size(); ++i)
  {
    os << qi::encodeJSON(gvv[i]);
    if (i + 1 != gvv.size())
      os << " ";
  }
  return os;
}

template<typename T>
std::list<std::string> ServiceHelper::getMatchingMembersName(const std::map<unsigned int, T> &metaMemberMap, const std::string &pattern, bool getHidden) const
{
  std::list<std::string> metaMemberVec;

  if (isNumber(pattern))
  {
    unsigned int uid = ::atoi(pattern.c_str());
    if (metaMemberMap.count(uid))
      metaMemberVec.push_back(metaMemberMap.find(uid)->second.name());
    return metaMemberVec;
  }
  std::pair<unsigned int, T> it;
  BOOST_FOREACH(it, metaMemberMap)
  {
    if (it.second.name() == pattern)
    {
      metaMemberVec.push_back(it.second.name());
      return metaMemberVec;
    }
    if (byPassMember(it.second.name(), it.second.uid(), getHidden))
      continue;
    if (qi::os::fnmatch(pattern, it.second.name()))
      metaMemberVec.push_back(it.second.name());
  }
  return metaMemberVec;
}

std::list<std::string> ServiceHelper::getMatchingSignalsName(const std::string &pattern, bool getHidden) const
{
  return getMatchingMembersName<qi::MetaSignal>(_service->metaObject().signalMap(), pattern, getHidden);
}

std::list<std::string> ServiceHelper::getMatchingMethodsName(const std::string &pattern, bool getHidden) const
{
  return getMatchingMembersName<qi::MetaMethod>(_service->metaObject().methodMap(), pattern, getHidden);
}

std::list<std::string> ServiceHelper::getMatchingPropertiesName(const std::string &pattern, bool getHidden) const
{
  return getMatchingMembersName<qi::MetaProperty>(_service->metaObject().propertyMap(), pattern, getHidden);
}

const ServiceHelper& ServiceHelper::operator=(const qi::ObjectPtr &service)
{
  _service = service;
  return *this;
}

const ServiceHelper& ServiceHelper::operator=(const ServiceHelper &other)
{
  _service = other._service;
  _name = other._name;
  return *this;
}

const std::string &ServiceHelper::name() const
{
  return _name;
}

const qi::ObjectPtr& ServiceHelper::objPtr() const
{
  return _service;
}

int ServiceHelper::showProperty(const std::string &propertyName)
{
  std::cout << _name << "." << propertyName << ": ";
  int propertyId = _service->metaObject().propertyId(propertyName);
  if (propertyId == -1)
  {
    std::cout << "error: property not found" << std::endl;
    return 1;
  }
  qi::FutureSync<qi::GenericValue> result = _service->property(propertyId);

  if (result.hasError())
    std::cout << "error: " << result.error() << std::endl;
  else
    std::cout << qi::encodeJSON(result.value()) << std::endl;
  return 0;
}

int ServiceHelper::setProperty(const std::string &propertyName, const qi::GenericValue &gvArg)
{
  std::cout << _name << "." << propertyName << ": ";
  qi::FutureSync<void> result = _service->setProperty(propertyName, gvArg);

  if (result.hasError())
    std::cout << "error: " << result.error() << std::endl;
  else
    std::cout << "OK" << std::endl;
  return 0;
}

int ServiceHelper::watch(const std::string &signalName, bool showTime)
{
  WatchOptions options;
  options.showTime = showTime;
  options.signalName = signalName;
  qi::SignalSubscriber sigSub(qi::makeDynamicGenericFunction(boost::bind(&ServiceHelper::defaultWatcher, this, options, _1)));
  qi::FutureSync<qi::Link> futLink = _service->connect(signalName, sigSub);
  if (futLink.hasError())
  {
    std::cout << _name << "." << signalName << ": " << futLink.error() << std::endl;
    return 1;
  }
  return 0;
}

int ServiceHelper::post(const std::string &signalName, const qi::GenericFunctionParameters &gvArgList)
{
  std::cout << _name << "." << signalName << ": ";
  std::cout.flush();
  _service->metaPost(signalName, gvArgList);
  std::cout << "OK" << std::endl;
  return 0;
}

int ServiceHelper::call(const std::string &methodName, const qi::GenericFunctionParameters &gvArgList)
{
  std::cout << _name << "." << methodName << ": ";
  std::cout.flush();
  qi::FutureSync<qi::GenericValuePtr> result = _service->metaCall(methodName, gvArgList);
  if (result.hasError())
    std::cout << "error: " << result.error() << std::endl;
  else
    std::cout << qi::encodeJSON(result.value()) << std::endl;

  return 0;
}

qi::GenericValuePtr ServiceHelper::defaultWatcher(const ServiceHelper::WatchOptions &options, const std::vector<qi::GenericValuePtr> &params)
{
  static boost::mutex m;
  std::ostringstream ss;
  if (options.showTime)
    ss << getTime() << ": ";
  ss << _name << ".";
  ss << options.signalName << " : ";
  ss << params;
  ss << std::endl;
  {
    // gain ownership of std::cout to avoid text overlap on terminal
    boost::lock_guard<boost::mutex> lock(m);
    std::cout << ss.str();
  }
  return qi::GenericValuePtr();
}

bool ServiceHelper::byPassMember(const std::string &name, unsigned int uid, bool showHidden) const
{
  if (showHidden)
    return false;
  if (qi::MetaObject::isPrivateMember(name, uid))
    return true;
  return false;
}
