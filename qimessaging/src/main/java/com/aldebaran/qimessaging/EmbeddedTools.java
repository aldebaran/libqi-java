package com.aldebaran.qimessaging;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

/**
 * Tool class providing QiMessaging<->Java type system loading and
 * dynamic library loader designed to load libraries included in jar package.
 * @author proullon
 *
 */
public class EmbeddedTools
{

  private File tmpDir = null;

  public static boolean LOADED_EMBEDDED_LIBRARY = false;
  private static native void initTypeSystem(java.lang.Object str, java.lang.Object i, java.lang.Object f, java.lang.Object d, java.lang.Object l, java.lang.Object m, java.lang.Object al, java.lang.Object t, java.lang.Object o, java.lang.Object b);
  private static native void initTupleInTypeSystem(java.lang.Object t1, java.lang.Object t2, java.lang.Object t3, java.lang.Object t4, java.lang.Object t5, java.lang.Object t6, java.lang.Object t7, java.lang.Object t8, java.lang.Object t9, java.lang.Object t10, java.lang.Object t11, java.lang.Object t12, java.lang.Object t13, java.lang.Object t14, java.lang.Object t15, java.lang.Object t16, java.lang.Object t17, java.lang.Object t18, java.lang.Object t19, java.lang.Object t20, java.lang.Object t21, java.lang.Object t22, java.lang.Object t23, java.lang.Object t24, java.lang.Object t25, java.lang.Object t26, java.lang.Object t27, java.lang.Object t28, java.lang.Object t29, java.lang.Object t30, java.lang.Object t31, java.lang.Object t32);


  /**
   * To work correctly, QiMessaging<->java type system needs to compare type class template.
   * Unfortunately, call template cannot be retrieve on native android thread.
   * The only available way to do is to store instance of wanted object
   * and get fresh class template from it right before using it.
   */
  private boolean initTypeSystem()
  {
    String  str = new String();
    Integer i   = new Integer(0);
    Float   f   = new Float(0);
    Double  d   = new Double(0);
    Long    l   = new Long(0);
    Tuple   t   = new Tuple1<java.lang.Object>();
    Boolean b   = new Boolean(true);

    DynamicObjectBuilder ob = new DynamicObjectBuilder();
    Object obj  = ob.object();

    Map<java.lang.Object, java.lang.Object> m  = new Hashtable<java.lang.Object, java.lang.Object>();
    ArrayList<java.lang.Object>             al = new ArrayList<java.lang.Object>();

    // Initialize generic type system
    EmbeddedTools.initTypeSystem(str, i, f, d, l, m, al, t, obj, b);

    Tuple t1 = Tuple.makeTuple(0);
    Tuple t2 = Tuple.makeTuple(0, 0);
    Tuple t3 = Tuple.makeTuple(0, 0, 0);
    Tuple t4 = Tuple.makeTuple(0, 0, 0, 0);
    Tuple t5 = Tuple.makeTuple(0, 0, 0, 0, 0);
    Tuple t6 = Tuple.makeTuple(0, 0, 0, 0, 0, 0);
    Tuple t7 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0);
    Tuple t8 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t9 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t10 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t11 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t12 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t13 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t14 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t15 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t16 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t17 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t18 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t19 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t20 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t21 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t22 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t23 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t24 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t25 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t26 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t27 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t28 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t29 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t30 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t31 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    Tuple t32 = Tuple.makeTuple(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    // Initialize tuple
    EmbeddedTools.initTupleInTypeSystem(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22, t23, t24, t25, t26, t27, t28, t29, t30, t31, t32);
    return true;
  }

  /**
   * Override directory where native libraries are extracted.
   */
  public void overrideTempDirectory(File newValue)
  {
    SharedLibrary.overrideTempDirectory(newValue);

  }

  /**
   * Native C++ libraries are packaged with java sources.
   * This way, we are able to load them anywhere, anytime.
   */
  public boolean loadEmbeddedLibraries()
  {
    if (LOADED_EMBEDDED_LIBRARY == true)
    {
      System.out.print("Native libraries already loaded");
      return true;
    }

    // Only present on android
    SharedLibrary.loadLib("gnustl_shared");

    if (SharedLibrary.loadLib("qi") == false
            || SharedLibrary.loadLib("qimessagingjni") == false) {
        LOADED_EMBEDDED_LIBRARY = false;
        return false;
    }

    System.out.printf("Libraries loaded. Initializing type system...\n");
    LOADED_EMBEDDED_LIBRARY = true;
    if (initTypeSystem() == false)
    {
      System.out.printf("Cannot initialize type system\n");
      LOADED_EMBEDDED_LIBRARY = false;
      return false;
    }

    return true;
  }

}
