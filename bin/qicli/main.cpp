#include <boost/program_options.hpp>
#include <qi/application.hpp>

#include "qicli.hpp"

namespace po = boost::program_options;

std::map<std::string, SubCmd> subCmdMap;

void init()
{
  subCmdMap["service"] = &subCmd_service;
  subCmdMap["call"] = &subCmd_call;
  subCmdMap["post"] = &subCmd_post;
  subCmdMap["watch"] = &subCmd_watch;
  subCmdMap["get"] = &subCmd_get;
  subCmdMap["set"] = &subCmd_set;
}

int                 main(int argc, char **argv)
{
  qi::Application app(argc, argv);
  int             subCmdArgc = 0;
  char            **subCmdArgv = 0;
  SubCmd          subCmd = 0;

  init();
  for (int i = 0; i < argc; ++i)
  {
    std::map<std::string, SubCmd>::const_iterator it = subCmdMap.find(argv[i]);
    if (it != subCmdMap.end())
    {
      subCmdArgc = argc - i;
      subCmdArgv = &argv[i];
      subCmd = it->second;
      argc = i + 1;
      break;
    }
  }

  po::options_description desc("Usage: qicli [OPTIONS] SUBCMD [-h] [OPTIONS] [ARGS]");

  MainOptions options;

  desc.add_options()
      ("address,a", po::value<std::string>(&options.address)->default_value("tcp://127.0.0.1:9559"), "The address of the service directory")
      ("command,c", "sub command to execute")
      ("help,h", "Print this help message and exit");

  po::positional_options_description positionalOptions;
  positionalOptions.add("command", 1);
  po::variables_map vm;

  try {
    po::store(po::command_line_parser(argc, argv).options(desc).run(), vm);
    po::notify(vm);
  } catch (po::error &e) {
    std::cerr << e.what() << std::endl;
    return false;
  }

  if (vm.count("help") || subCmdArgv == 0)
  {
    showHelp(desc);
    return 0;
  }

  int ret;
  try {
    ret = subCmd(subCmdArgc, subCmdArgv, options);
  } catch (const std::exception& e)
  {
    std::cout << "error: " << e.what() << std::endl;
    return 1;
  } catch (...) {
    std::cout << "unknown error catch" << std::endl;
    return 1;
  }
  return ret;
}
