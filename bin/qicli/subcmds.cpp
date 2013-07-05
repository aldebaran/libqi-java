#include <iomanip>

#include <boost/program_options.hpp>
#include <qimessaging/session.hpp>

#include "qicli.hpp"

static const int qicli_call_cmd_style = po::command_line_style::unix_style ^ po::command_line_style::allow_short;

int subCmd_service(int argc, char **argv, const MainOptions &options)
{
  po::options_description     desc("Usage: qicli service [<ServicePattern>...]");
  std::vector<std::string>    serviceList;

  desc.add_options()
      ("service,s", po::value<std::vector<std::string> >(&serviceList), "service to display")
      ("help,h", "Print this help message and exit")
      ("list,l", "List services (default when no service specified)")
      ("info,i", "print service info, methods, signals and properties")
      ("hidden", "show hidden services, methods, signals and properties")
      ("show-doc", "show documentation for methods, signals and properties");

  po::positional_options_description positionalOptions;
  positionalOptions.add("service", -1);

  po::variables_map vm;
  if (!poDefault(po::command_line_parser(argc, argv).options(desc).positional(positionalOptions), vm, desc))
    return 1;

  bool details = false;
  if (vm.count("info") && vm.count("list"))
    throw std::runtime_error("You cannot specify --list and --info together.");

  //smart details/list
  if (vm.count("info"))
    details = true;
  else if (vm.count("list"))
    details = false;
  else {
    //list/details not specified, use details if services have been specified.
    details = (serviceList.size()) != 0;
  }

  if (serviceList.empty())
    serviceList.push_back("*");

  SessionHelper session(options.address);
  session.info(serviceList, details, vm.count("hidden"), vm.count("show-doc"));
  return 0;
}

int subCmd_call(int argc, char **argv, const MainOptions &options)
{
  po::options_description     desc("Usage: qicli call <ServicePattern.MethodPattern> [<JsonParameter>...]");
  std::string                 fullName;
  std::vector<std::string>    argList;

  desc.add_options()
      ("method,m", po::value<std::string>(&fullName)->required(), "method's name")
      ("arg,a", po::value<std::vector<std::string> >(&argList), "method's args")
      ("hidden", "call hidden methods if they match the given pattern")
      ("help,h", "Print this help message and exit");

  po::positional_options_description positionalOptions;
  positionalOptions.add("method", 1);
  positionalOptions.add("arg", -1);

  po::variables_map vm;
  if (!poDefault(po::command_line_parser(argc, argv).options(desc).positional(positionalOptions)
                 .style(qicli_call_cmd_style), vm, desc))
    return 1;
  SessionHelper session(options.address);

  session.call(fullName, argList, vm.count("hidden"));
  return 0;
}

int subCmd_post(int argc, char **argv, const MainOptions &options)
{
  po::options_description     desc("Usage: qicli post <ServicePattern.SignalPattern> [<JsonParameter>...]");
  std::string                 fullName;
  std::vector<std::string>    argList;

  desc.add_options()
      ("signal,s", po::value<std::string>(&fullName)->required(), "signal's name")
      ("arg,a", po::value<std::vector<std::string> >(&argList), "method's args")
      ("hidden", "post hidden signals if they match the given pattern")
      ("help,h", "Print this help message and exit");

  po::positional_options_description positionalOptions;
  positionalOptions.add("signal", 1);
  positionalOptions.add("arg", -1);

  po::variables_map vm;
  if (!poDefault(po::command_line_parser(argc, argv).options(desc).positional(positionalOptions)
                 .style(qicli_call_cmd_style), vm, desc))
    return 1;

  SessionHelper session(options.address);

  session.post(fullName, argList, vm.count("hidden"));
  return 0;
}

int subCmd_get(int argc, char **argv, const MainOptions &options)
{
  po::options_description   desc("Usage: qicli get <ServicePattern.PropertyPattern>...");
  std::vector<std::string>  patternList;

  desc.add_options()
      ("prop,p", po::value<std::vector<std::string> >(&patternList), "property's name")
      ("hidden", "get hidden properties if they match the given pattern")
      ("help,h", "Print this help message and exit");

  po::positional_options_description positionalOptions;
  positionalOptions.add("prop", -1);

  po::variables_map vm;
  if (!poDefault(po::command_line_parser(argc, argv).options(desc).positional(positionalOptions), vm, desc))
    return 1;

  SessionHelper session(options.address);

  session.get(patternList, vm.count("hidden"));
  return 0;
}

int subCmd_set(int argc, char **argv, const MainOptions &options)
{
  po::options_description   desc("Usage: qicli set <ServicePattern.PropertyPattern>... <JsonParameter>");
  std::vector<std::string>  argList;

  desc.add_options()
      ("prop,p", po::value<std::vector<std::string> >(&argList), "property's name")
      ("hidden", "set hidden properties if they match the given pattern")
      ("help,h", "Print this help message and exit");

  po::positional_options_description positionalOptions;
  positionalOptions.add("prop", -1);

  po::variables_map vm;
  if (!poDefault(po::command_line_parser(argc, argv).options(desc).positional(positionalOptions)
                 .style(qicli_call_cmd_style), vm, desc))
    return 1;

  if (argList.size() < 2)
  {
    showHelp(desc);
    return 1;
  }
  std::string jsonValue = argList.back();
  argList.pop_back();

  SessionHelper session(options.address);
  session.set(argList, jsonValue, vm.count("hidden"));
  return 0;
}

int subCmd_watch(int argc, char **argv, const MainOptions &options)
{
  po::options_description   desc("Usage: qicli watch <ServicePattern.SignalPattern>...");
  std::vector<std::string>  patternList;

  desc.add_options()
      ("signal,s", po::value<std::vector<std::string> >(&patternList), "service's name")
      ("time,t", "Print time")
      ("hidden", "watch hidden signals if they match the given pattern")
      ("help,h", "Print this help message and exit");

  po::positional_options_description positionalOptions;
  positionalOptions.add("signal", -1);

  po::variables_map vm;
  if (!poDefault(po::command_line_parser(argc, argv).options(desc).positional(positionalOptions), vm, desc))
    return 1;

  SessionHelper session(options.address);

  session.watch(patternList, vm.count("time"), vm.count("hidden"));
  ::getchar();
  return 0;
}
