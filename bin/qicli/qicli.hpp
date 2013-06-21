#ifndef QICLI_HPP_
# define QICLI_HPP_

# include <qimessaging/session.hpp>
# include <boost/program_options.hpp>

# include "sessionhelper.hpp"

struct MainOptions
{
  std::string   address;
};

typedef int (*SubCmd)(int argc, char **argv, const MainOptions &options);

namespace po = boost::program_options;


/* SUBCMDS */
int subCmd_service(int argc, char **argv, const MainOptions &options);
int subCmd_call(int argc, char **argv, const MainOptions &options);
int subCmd_post(int argc, char **argv, const MainOptions &options);
int subCmd_service(int argc, char **argv, const MainOptions &options);
int subCmd_watch(int argc, char **argv, const MainOptions &options);
int subCmd_get(int argc, char **argv, const MainOptions &options);
int subCmd_set(int argc, char **argv, const MainOptions &options);

/* UTILS */
bool splitName(const std::string &fullName, std::string &beforePoint, std::string &afterPoint);
int readNumericInput();
std::string readAlphaInput();
bool poDefault(const po::command_line_parser &clp, po::variables_map &vm, const po::options_description &desc);
void showHelp(const po::options_description &desc);
std::string getTime();

#endif /* !QICLI_HPP_ */
