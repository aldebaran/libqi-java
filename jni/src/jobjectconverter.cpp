/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/


#include <boost/locale.hpp>

#include <qi/log.hpp>
#include <qitype/signature.hpp>
#include <qitype/dynamicobjectbuilder.hpp>
#include <qitype/anyobject.hpp>
#include <qitype/anyvalue.hpp>
#include <qitype/typedispatcher.hpp>
#include <qitype/typeinterface.hpp>

#include <jnitools.hpp>
#include <jobjectconverter.hpp>
#include <map_jni.hpp>
#include <list_jni.hpp>
#include <tuple_jni.hpp>
#include <object_jni.hpp>

qiLogCategory("qimessaging.jni");
using namespace qi;

struct toJObject
{
    toJObject(jobject *result)
      : result(result)
    {
      env = attach.get();
    }

    void visitUnknown(qi::AnyReference value)
    {
      throwJavaError(env, "Error in conversion: Unable to convert unknown type in Java");
    }

    void visitInt(qi::int64_t value, bool isSigned, int byteSize)
    {
      qiLogVerbose() << "visitInt " << value << ' ' << byteSize;
      // Clear all remaining exceptions
      env->ExceptionClear();

      // Get Integer class template
      // ... or Boolean if byteSize is 0
      jclass cls = qi::jni::clazz(byteSize == 0 ? "Boolean" : "Integer");
      if (env->ExceptionCheck())
      {
        qi::jni::releaseClazz(cls);
        throwJavaError(env, "AnyValue to Integer : FindClass error");
        return;
      }

      // Find constructor method ID
      jmethodID mid = env->GetMethodID(cls, "<init>", byteSize == 0 ? "(Z)V" : "(I)V");
      if (!mid)
      {
        qi::jni::releaseClazz(cls);
        throwJavaError(env, "AnyValue to Integer : GetMethodID error");
        return;
      }

      // Instanciate new Integer, yeah !
      jint jval = value;
      *result = env->NewObject(cls, mid, jval);
      checkForError();
      qi::jni::releaseClazz(cls);
    }

    void visitString(char *data, size_t len)
    {
      qiLogVerbose() << "visitString " << len;
      if (data)
      {
        // It is unclear wether wstring and wchar_t are garanteed 16 bytes,
        std::basic_string<jchar> conv = boost::locale::conv::utf_to_utf<jchar>(data, data+len);
        if (conv.empty())
          *result = (jobject) env->NewStringUTF("");
        else
          *result = (jobject) env->NewString(&conv[0], conv.length());
      }
      else
        *result = (jobject) env->NewStringUTF("");
      checkForError();
    }

    void visitVoid()
    {
      jclass cls = env->FindClass("java/lang/Void");
      jmethodID mid = env->GetMethodID(cls, "<init>", "()V");
      *result = env->NewObject(cls, mid);
      checkForError();
    }

    void visitFloat(double value, int byteSize)
    {
      qiLogVerbose() << "visitFloat " << value;
      // Clear all remaining exceptions
      env->ExceptionClear();

      // Get Float class template
      jclass cls = qi::jni::clazz("Float");
      if (env->ExceptionCheck())
      {
        qi::jni::releaseClazz(cls);
        throwJavaError(env, "AnyValue to Float : FindClass error");
        return;
      }

      // Find constructor method ID
      jmethodID mid = env->GetMethodID(cls, "<init>","(F)V");
      if (!mid)
      {
        qi::jni::releaseClazz(cls);
        throwJavaError(env, "AnyValue to Float : GetMethodID error");
        return;
      }

      // Instanciate new Float, yeah !
      jfloat jval = value;
      *result = env->NewObject(cls, mid, jval);
      qi::jni::releaseClazz(cls);
      checkForError();
    }

    void visitList(qi::AnyIterator it, qi::AnyIterator end)
    {
      JNIList list; // this is OK.

      for(; it != end; ++it)
      {
        qi::AnyReference arRes = *it;
        std::pair<qi::AnyReference, bool> converted = arRes.convert(qi::typeOf<jobject>());
        jobject result = *(jobject*)converted.first.rawValue();
        list.push_back(result);
        if (converted.second)
          converted.first.destroy();
      }

      *result = list.object();
    }

    void visitMap(qi::AnyIterator it, qi::AnyIterator end)
    {
      JNIHashTable ht;

      for (; it != end; ++it)
      {
        std::pair<qi::AnyReference, bool> keyConv =
          (*it)[0].convert(qi::typeOf<jobject>());
        std::pair<qi::AnyReference, bool> valConv =
          (*it)[1].convert(qi::typeOf<jobject>());

        ht.setItem(*(jobject*)keyConv.first.rawValue(),
            *(jobject*)valConv.first.rawValue());

        if (keyConv.second)
          keyConv.first.destroy();
        if (valConv.second)
          valConv.first.destroy();
      }

      *result = ht.object();
    }

    void visitObject(qi::GenericObject obj)
    {
      throw std::runtime_error("Cannot convert GenericObject to Jobject.");
    }

    void visitAnyObject(qi::AnyObject o)
    {

      try
      {
        JNIObject obj(o);
        *result = obj.object();

      } catch (std::exception&)
      {
        return;
      }

    }

    void visitPointer(qi::AnyReference pointee)
    {
      qiLogFatal() << "Error in conversion: Unable to convert pointer in Java";
      throwJavaError(env, "Error in conversion: Unable to convert pointer in Java");
    }

    void visitTuple(const std::string& className, const std::vector<qi::AnyReference>& tuple, const std::vector<std::string>& annotations)
    {
      JNITuple jtuple(tuple.size());
      int i = 0;

      for(std::vector<qi::AnyReference>::const_iterator it = tuple.begin(); it != tuple.end(); ++it)
      {
        qi::AnyReference arRes = *it;
        std::pair<qi::AnyReference, bool> converted = arRes.convert(qi::typeOf<jobject>());
        jobject result = *(jobject*)converted.first.rawValue();
        jtuple.set(i++, result);
        if (converted.second)
          converted.first.destroy();
      }

      *result = jtuple.object();
    }

    void visitDynamic(qi::AnyReference pointee)
    {
      qiLogVerbose() << "visitDynamic";
      *result = JObject_from_AnyValue(pointee);
    }

    void visitRaw(qi::AnyReference value)
    {
      qiLogVerbose() << "visitRaw";
      qi::Buffer buf = value.as<qi::Buffer>();

      // Create a new ByteBuffer and reserve enough space
      jclass cls = env->FindClass("java/nio/ByteBuffer");
      jmethodID mid = env->GetStaticMethodID(cls, "allocate", "(I)Ljava/nio/ByteBuffer;");
      jobject ar = env->CallStaticObjectMethod(cls, mid, buf.size());

      // Put qi::Buffer content into a byte[] object
      const jbyte* data = (const jbyte*) buf.data();
      jbyteArray byteArray = env->NewByteArray(buf.size());
      env->SetByteArrayRegion(byteArray, 0, buf.size(), data);

      // Put the byte[] object into the ByteBuffer
      mid = env->GetMethodID(cls, "put","([BII)Ljava/nio/ByteBuffer;");
      *result = env->CallObjectMethod(ar, mid, byteArray, 0, buf.size());
      checkForError();
      env->DeleteLocalRef(cls);
      env->DeleteLocalRef(ar);
      env->DeleteLocalRef(byteArray);
    }

    void visitIterator(qi::AnyReference v)
    {
      visitUnknown(v);
    }

    void checkForError()
    {
      if (result == NULL)
        throwJavaError(env, "Error in conversion to JObject");
    }

    jobject* result;
    JNIEnv*  env;
    qi::jni::JNIAttach attach;

}; // !toJObject

jobject JObject_from_AnyValue(qi::AnyReference val)
{
  jobject result= NULL;
  toJObject tjo(&result);
  qi::typeDispatch<toJObject>(tjo, val);
  return result;
}

void JObject_from_AnyValue(qi::AnyReference val, jobject* target)
{
  toJObject tal(target);
  qi::typeDispatch<toJObject>(tal, val);
}

qi::AnyReference AnyValue_from_JObject_List(jobject val)
{
  JNIEnv* env;
  JNIList list(val);
  std::vector<qi::AnyValue>& res = *new std::vector<qi::AnyValue>();
  int size = 0;

  JVM()->GetEnv((void **) &env, QI_JNI_MIN_VERSION);

  size = list.size();
  res.reserve(size);
  for (int i = 0; i < size; i++)
  {
    jobject current = list.get(i);
    std::pair<qi::AnyReference, bool> conv = AnyValue_from_JObject(current);
    res.push_back(qi::AnyValue(conv.first, !conv.second, true));
  }

  return qi::AnyReference::from(res);
}

qi::AnyReference AnyValue_from_JObject_Map(jobject hashtable)
{
  JNIEnv* env;
  std::map<qi::AnyValue, qi::AnyValue>& res = *new std::map<qi::AnyValue, qi::AnyValue>();
  JNIHashTable ht(hashtable);
  jobject key, value;

  JVM()->GetEnv((void **) &env, QI_JNI_MIN_VERSION);

  JNIEnumeration keys = ht.keys();
  while (keys.hasNextElement())
  {
    key = keys.nextElement();
    value = ht.at(key);
    std::pair<qi::AnyReference, bool> convKey = AnyValue_from_JObject(key);
    std::pair<qi::AnyReference, bool> convValue = AnyValue_from_JObject(value);
    res[qi::AnyValue(convKey.first, !convKey.second, true)] = qi::AnyValue(convValue.first, !convValue.second, true);
  }
  return qi::AnyReference::from(res);
}

qi::AnyReference AnyValue_from_JObject_Tuple(jobject val)
{
  JNITuple tuple(val);
  int i = 0;
  std::vector<qi::AnyReference> elements;
  std::vector<qi::AnyReference> toFree;
  while (i < tuple.size())
  {
    std::pair<qi::AnyReference, bool> convValue = AnyValue_from_JObject(tuple.get(i));
    elements.push_back(convValue.first);
    if (convValue.second)
      toFree.push_back(convValue.first);
    i++;
  }
  qi::AnyReference res = qi::makeGenericTuple(elements); // copies
  for (unsigned i=0; i<toFree.size(); ++i)
    toFree[i].destroy();
  return res;
}

qi::AnyReference AnyValue_from_JObject_RemoteObject(jobject val)
{
  JNIObject obj(val);

  qi::AnyObject* tmp = new qi::AnyObject();
  *tmp = obj.objectPtr();
  return qi::AnyReference::from(*tmp);
}

std::pair<qi::AnyReference, bool> AnyValue_from_JObject(jobject val)
{
  qi::AnyReference res;
  JNIEnv* env;
  bool copy = false;

  if (!val)
    return std::make_pair(qi::AnyReference(), false);

  qi::jni::JNIAttach attach;
  env = attach.get();

  jclass stringClass = qi::jni::clazz("String");
  jclass int32Class = qi::jni::clazz("Integer");
  jclass floatClass = qi::jni::clazz("Float");
  jclass doubleClass = qi::jni::clazz("Double");
  jclass boolClass = qi::jni::clazz("Boolean");
  jclass longClass = qi::jni::clazz("Long");
  jclass mapClass = qi::jni::clazz("Map");
  jclass listClass = qi::jni::clazz("List");
  jclass tupleClass = qi::jni::clazz("Tuple");
  jclass objectClass = qi::jni::clazz("Object");

  if (val == NULL)
  {
    res = qi::AnyReference(qi::typeOf<void>());
  }
  else if (env->IsInstanceOf(val, stringClass))
  {
    const char* data = env->GetStringUTFChars((jstring) val, 0);
    std::string tmp = std::string(data);
    env->ReleaseStringUTFChars((jstring) val, data);
    res = qi::AnyReference::from(tmp).clone();
    copy = true;
  }
  else if (env->IsInstanceOf(val, floatClass))
  {
    jmethodID mid = env->GetMethodID(floatClass, "floatValue","()F");
    jfloat v = env->CallFloatMethod(val, mid);
    res = qi::AnyReference::from((float)v).clone();
    copy = true;
  }
  else if (env->IsInstanceOf(val, doubleClass)) // If double, convert to float
  {
    jmethodID mid = env->GetMethodID(doubleClass, "doubleValue","()D");
    jfloat v = (jfloat) env->CallDoubleMethod(val, mid);
    res = qi::AnyReference::from((float)v).clone();
    copy = true;
  }
  else if (env->IsInstanceOf(val, longClass))
  {
    jmethodID mid = env->GetMethodID(longClass, "longValue","()L");
    jlong v = env->CallLongMethod(val, mid);
    res = qi::AnyReference::from(v).clone();
    copy = true;
  }
  else if (env->IsInstanceOf(val, boolClass))
  {
    jmethodID mid = env->GetMethodID(boolClass, "booleanValue","()Z");
    jboolean v = env->CallBooleanMethod(val, mid);
    res = qi::AnyReference::from((bool) v).clone();
    copy = true;
  }
  else if (env->IsInstanceOf(val, int32Class))
  {
    jmethodID mid = env->GetMethodID(int32Class, "intValue","()I");
    jint v = env->CallIntMethod(val, mid);
    res = qi::AnyReference::from((int) v).clone();
    copy = true;
  }
  else if (env->IsInstanceOf(val, listClass))
  {
    copy = true;
    res = AnyValue_from_JObject_List(val);
  }
  else if (env->IsInstanceOf(val, mapClass))
  {
    copy = true;
    res = AnyValue_from_JObject_Map(val);
  }
  else if (qi::jni::isTuple(val))
  {
    copy = true;
    res = AnyValue_from_JObject_Tuple(val);
  }
  else if (env->IsInstanceOf(val, objectClass))
  {
    copy = true;
    res = AnyValue_from_JObject_RemoteObject(val);
  }
  else
  {
    qiLogError() << "Cannot serialize return value: Unable to convert JObject in AnyValue";
    throw std::runtime_error("Cannot serialize return value: Unable to convert JObject in AnyValue");
  }

  qi::jni::releaseClazz(stringClass);
  qi::jni::releaseClazz(int32Class);
  qi::jni::releaseClazz(floatClass);
  qi::jni::releaseClazz(doubleClass);
  qi::jni::releaseClazz(boolClass);
  qi::jni::releaseClazz(longClass);
  qi::jni::releaseClazz(mapClass);
  qi::jni::releaseClazz(listClass);
  qi::jni::releaseClazz(tupleClass);
  qi::jni::releaseClazz(objectClass);

  return std::make_pair(res, copy);
}


/*
 * Define this struct to add jobject to the type system.
 * That way we can manipulate jobject transparently.
 * - We have to override clone and destroy here to be compliant
 *   with the java reference counting. Otherwise, the value could
 *   be Garbage Collected as we try to manipulate it.
 * - We register the type as 'jobject' since java methods manipulates
 *   objects only by this typedef pointer, never by value and we do not want to copy
 *   a jobject.
 */
class JObjectTypeInterface: public qi::DynamicTypeInterface
{
  public:

    virtual const qi::TypeInfo& info()
    {
      static qi::TypeInfo* result = 0;
      if (!result)
        result = new qi::TypeInfo(typeid(jobject));

      return *result;
    }

    virtual void* initializeStorage(void* ptr = 0)
    {
      // ptr is jobject* (aka _jobject**)
      if (!ptr)
      {
        ptr = new jobject;
        *(jobject*)ptr = NULL;
      }
      return ptr;
    }

    virtual void* ptrFromStorage(void** s)
    {
      jobject** tmp = (jobject**) s;
      return *tmp;
    }

    virtual qi::AnyReference get(void* storage)
    {
      static const unsigned int MEMORY_DEFAULT_SIZE = 20;
      static unsigned int MEMORY_SIZE = 0;
      static bool init = false;
      static qi::AnyReference* memoryBuffer;
      if (!init)
      {
        /* This is such an awful hack, we'd rather provide a way to
        * control it from outside: 0 means disable cleanup entirely.
        */
        init = true;
        std::string v = qi::os::getenv("QI_JAVA_REFERENCE_POOL_SIZE");
        if (!v.empty())
          MEMORY_SIZE = strtol(v.c_str(), 0, 0);
        if (MEMORY_SIZE)
          memoryBuffer = new qi::AnyReference[MEMORY_SIZE];
      }
      static unsigned int memoryPosition = 0;
      std::pair<qi::AnyReference, bool> convValue = AnyValue_from_JObject(*((jobject*)ptrFromStorage(&storage)));
      if (convValue.second && MEMORY_SIZE)
      {
        memoryBuffer[memoryPosition].destroy();
        memoryBuffer[memoryPosition] = convValue.first;
        memoryPosition = (memoryPosition+1) % MEMORY_SIZE;
      }
      return convValue.first;
    }

    virtual void set(void** storage, qi::AnyReference src)
    {
      // storage is jobject**
      jobject* target = *(jobject**)storage;

      JNIEnv *env;
      qi::jni::JNIAttach attach;
      env = attach.get();

      if (*target)
        env->DeleteGlobalRef(*target);

      // Giving jobject* to JObject_from_AnyValue
      JObject_from_AnyValue(src, target);

      if (*target)
        *target = env->NewGlobalRef(*target);
    }

    virtual void* clone(void* obj)
    {
      jobject* ginstance = (jobject*)obj;

      if (!obj)
        return 0;

      jobject* cloned = new jobject;
      *cloned = *ginstance;

      qi::jni::JNIAttach attach;
      JNIEnv *env = attach.get();
      *cloned = env->NewGlobalRef(*cloned);

      return cloned;
    }

    virtual void destroy(void* obj)
    {
      if (!obj)
        return;
      // void* obj is a jobject
      jobject* jobj = (jobject*) obj;

      if (*jobj)
      {
        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();
        env->DeleteGlobalRef(*jobj);
      }
      delete jobj;
    }

    virtual bool less(void* a, void* b)
    {
      jobject* pa = (jobject*) ptrFromStorage(&a);
      jobject* pb = (jobject*) ptrFromStorage(&b);

      return *pa < *pb;
    }
};

/* Register jobject -> See the above comment for explanations */
QI_TYPE_REGISTER_CUSTOM(jobject, JObjectTypeInterface);

