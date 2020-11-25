cmake_minimum_required(VERSION 2.8)
project(qimessaging-jni)

find_package(qibuild)

qi_sanitize_compile_flags()
if (NOT ANDROID AND NOT I_AM_A_ROBOT)
  find_package(JNI REQUIRED)
  include_directories(SYSTEM ${JNI_INCLUDE_DIRS})
endif()
if (I_AM_A_ROBOT)
  find_package(oracle-jdk)
  include_directories(SYSTEM ${ORACLE-JDK_INCLUDE_DIRS})
endif()

include_directories("${PROJECT_SOURCE_DIR}")

set(SOURCE
   jni/session_jni.hpp
   jni/application_jni.hpp
   jni/rawapplication_jni.hpp
   jni/callbridge.hpp
   jni/servicedirectory_jni.hpp
   jni/objectbuilder.hpp
   jni/jnitools.hpp
   jni/jobjectconverter.hpp
   jni/map_jni.hpp
   jni/list_jni.hpp
   jni/future_jni.hpp
   jni/tuple_jni.hpp
   jni/optional_jni.hpp
   jni/futurehandler.hpp
   jni/object_jni.hpp
   jni/object.hpp
   jni/promise_jni.hpp
   jni/property.hpp

   src/session_jni.cpp
   src/application_jni.cpp
   src/rawapplication_jni.cpp
   src/callbridge.cpp
   src/servicedirectory_jni.cpp
   src/objectbuilder.cpp
   src/jnitools.cpp
   src/jobjectconverter.cpp
   src/map_jni.cpp
   src/list_jni.cpp
   src/optional_jni.cpp
   src/future_jni.cpp
   src/tuple_jni.cpp
   src/futurehandler.cpp
   src/object_jni.cpp
   src/object.cpp
   src/promise_jni.cpp
   src/property.cpp
   )

# Compile qimessaging java compatibility layer using jni
qi_create_lib(
  qimessagingjni
  ${SOURCE}
  SHARED
  DEPENDS
    QI
    ASSUME_SYSTEM_INCLUDE BOOST
)

qi_stage_lib(qimessagingjni)

if (NOT ANDROID)

   qi_create_gtest(
     test_qijni

     "test/test_qijni.cpp"
     "test/test_futurejni.cpp"
     "test/test_common.cpp"

     ARGUMENTS "${CMAKE_CURRENT_LIST_DIR}/../qimessaging/target/classes"

     DEPENDS
     jni
     qi
     qimessagingjni
   )
endif()
