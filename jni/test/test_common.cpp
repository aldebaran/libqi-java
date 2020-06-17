#include "test_common.hpp"
#include "jni/future_jni.hpp"
#include <stdlib.h>
#include <jni/jnitools.hpp>
#include <boost/program_options.hpp>

namespace qi { namespace jni { namespace test
{

  void loadFutureNativeMethods(JNIEnv* env)
  {
    static const JNINativeMethod futureMethods[] = {
      {(char*)"qiFutureCallGet", (char*)"(JI)Ljava/lang/Object;", reinterpret_cast<void*>(&Java_com_aldebaran_qi_Future_qiFutureCallGet)},
      {(char*)"qiFutureDestroy", (char*)"(J)V", reinterpret_cast<void*>(&Java_com_aldebaran_qi_Future_qiFutureDestroy)},
      {(char*)"qiFutureThenVoid", (char*)"(JLcom/aldebaran/qi/Consumer;)Ljava/lang/Object;", reinterpret_cast<void*>(&Java_com_aldebaran_qi_Future_qiFutureThenVoid)},
      {(char*)"qiFutureCallIsCancelled", (char*)"(J)Z", reinterpret_cast<void*>(&Java_com_aldebaran_qi_Future_qiFutureCallIsCancelled)},
      {(char*)"qiFutureCallIsDone", (char*)"(J)Z", reinterpret_cast<void*>(&Java_com_aldebaran_qi_Future_qiFutureCallIsDone)},
      {(char*)"qiFutureCallWaitWithTimeout", (char*)"(JI)V", reinterpret_cast<void*>(&Java_com_aldebaran_qi_Future_qiFutureCallWaitWithTimeout)},
      {(char*)"qiFutureCallConnectCallback", (char*)"(JLcom/aldebaran/qi/Future$Callback;I)V", reinterpret_cast<void*>(&Java_com_aldebaran_qi_Future_qiFutureCallConnectCallback)}
    };
    const auto futureClass = env->FindClass("com/aldebaran/qi/Future");
    env->RegisterNatives(futureClass, futureMethods, sizeof(futureMethods)/sizeof(JNINativeMethod));
  }

  Environment::Environment(int argc, char** argv)
      : argc(argc),
        argv(argv)
  {
  }

  void Environment::readArguments()
  {
      constexpr const auto classPathOpt = "classpath";

      namespace bpo = boost::program_options;
      bpo::options_description desc{"Options"};
      desc.add_options()
              (classPathOpt, bpo::value<std::vector<std::string>>());

      bpo::positional_options_description pdesc;
      pdesc.add(classPathOpt, -1);

      bpo::variables_map varMap;
      bpo::store(bpo::command_line_parser(argc, argv).options(desc).positional(pdesc).run(), varMap);
      bpo::notify(varMap);

      if(!varMap.count(classPathOpt))
      {
          FAIL() << "At least one classpath must be specified as a command line argument "
                    "(e.g. '~/libqi-java/qimessaging/target/classes')\n"
                    << desc;
          return;
      }
      classPaths = varMap[classPathOpt].as<std::vector<std::string>>();
  }

  void Environment::SetUp()
  {
    JavaVMOption options[2];
    JavaVMInitArgs vm_args;

    readArguments();

    // get java classPath separator
    #ifdef _WIN32
        const char pathSeparator = ';';
    #else
        const char pathSeparator = ':';
    #endif

    // configure java classpath
    std::string str{"-Djava.class.path="};
    for(auto classPath: classPaths)
        str.append(classPath + pathSeparator);

    char *classPathDefinition = new char[str.length() + 1];
    strcpy(classPathDefinition, str.c_str());
    options[0].optionString = classPathDefinition;

    // This line is added to allow attaching a Java debugger.
    options[1].optionString = (char*)"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005";

    memset(&vm_args, 0, sizeof(vm_args));
    vm_args.version = QI_JNI_MIN_VERSION;
    vm_args.nOptions = 2;
    vm_args.options = options;

    assertSuccess(
      [&]{ return JNI_CreateJavaVM(&javaVM, reinterpret_cast<void**>(&jniEnv), &vm_args); },
      "Failed to create the Java virtual machine.");
    assertSuccess(
      [&]{ return javaVM->AttachCurrentThread(reinterpret_cast<void**>(&jniEnv), nullptr); },
      "Failed to attach current thread.");
    const auto embeddedToolsClass = jniEnv->FindClass("com/aldebaran/qi/EmbeddedTools");
    QI_ASSERT_NOT_NULL(embeddedToolsClass);
    // We need to notify the Java code that the native libraries are already loaded.
    const auto atomicBooleanClass = jniEnv->FindClass("java/util/concurrent/atomic/AtomicBoolean");
    QI_ASSERT_NOT_NULL(atomicBooleanClass);
    const auto embeddedLibrariesLoaded = scopeJObject(jniEnv->GetStaticObjectField(
          embeddedToolsClass, jniEnv->GetStaticFieldID(embeddedToolsClass, "embeddedLibrariesLoaded", "Ljava/util/concurrent/atomic/AtomicBoolean;")));
    QI_ASSERT_NOT_NULL(embeddedLibrariesLoaded.value);
    const auto embeddedLibrariesLoadedSet = jniEnv->GetMethodID(
        atomicBooleanClass, "set", "(Z)V");
    QI_ASSERT_NOT_NULL(embeddedLibrariesLoadedSet);
    jniEnv->CallVoidMethod(embeddedLibrariesLoaded.value, embeddedLibrariesLoadedSet, true);
    QI_ASSERT_FALSE(jniEnv->ExceptionCheck());

    app.emplace(argc, argv);

    // Real Java apps will always call this when loading the library.
    ASSERT_NO_THROW(EXPECT_EQ(QI_JNI_MIN_VERSION, JNI_OnLoad(javaVM, nullptr)));
    loadFutureNativeMethods(jniEnv);
    ASSERT_NO_THROW(Java_com_aldebaran_qi_EmbeddedTools_initTypeSystem(jniEnv));
  }

  void Environment::TearDown()
  {
    try
    {
      app.reset();
      ASSERT_NO_THROW(JNI_OnUnload(javaVM, nullptr));
      jniEnv = nullptr;

      if (const auto localJavaVM = ka::exchange(javaVM, nullptr))
      {
        assertSuccess([&]{ return localJavaVM->DetachCurrentThread(); },
                      "Failed to detach current thread.");
        assertSuccess([&]{ return localJavaVM->DestroyJavaVM(); },
                      "Failed to destroy the Java virtual machine.");
      }
    }
    catch(...){}
  }

  Environment* environment = nullptr;
}}} // namespace jni::test
