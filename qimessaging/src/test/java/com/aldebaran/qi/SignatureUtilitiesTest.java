package com.aldebaran.qi;

import com.aldebaran.qi.serialization.SignatureUtilities;
import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Tests of {@link SignatureUtilities}
 */
public class SignatureUtilitiesTest {
    /**
     * Interface to have some methods with different signatures for test
     */
    static interface Signatures {
        public void voidNoParameter();

        public boolean booleanBoolean(boolean b);

        public char charChar(char c);

        public int intInt(int i);

        public long longLong(long l);

        public float floatFloat(float f);

        public double doubleDouble(double d);

        public String stringString(String s);

        public void listString(List<String> list);

        public void listListString(List<List<String>> list);

        public void mapIntegerString(Map<Integer, String> map);

        public void mapFloatMapStringInteger(Map<Float, Map<String, Integer>> map);

        public List<String> mapStringListString(Map<String, List<String>> map);

        public void triplet(Triplet triplet);

        public void triplet2(Triplet2 triplet);
    }

    /**
     * Tuple with fields in good order
     */
    @QiStruct
    static class Triplet extends Tuple {
        @QiField(0)
        public int x;
        @QiField(1)
        public String string;
        @QiField(2)
        public List<String> list;

        public Triplet() {
            super(3);
        }
    }

    /**
     * Tuple with fields in random order
     */
    @QiStruct
    static class Triplet2 extends Tuple {
        @QiField(1)
        public String string;
        @QiField(2)
        public List<String> list;
        @QiField(0)
        public int x;

        public Triplet2() {
            super(3);
        }
    }

    /**
     * Obtain a method of {@link Signatures} interface
     *
     * @param name Method name
     * @return Obtained method
     */
    static Method getMethod(final String name) {
        for (final Method method : Signatures.class.getDeclaredMethods()) {
            if (name.equals(method.getName())) {
                return method;
            }
        }

        return null;
    }

    /**
     * Check if {@link SignatureUtilities#computeSignatureForMethod} return the
     * good signature
     *
     * @param description Message associate to the test
     * @param signature   Expected signature
     * @param method      Method name to test
     */
    private void assertSignature(final String description, final String signature, final String method) {
        Assert.assertEquals(description, signature,
                SignatureUtilities.computeSignatureForMethod(SignatureUtilitiesTest.getMethod(method)));
    }

    /**
     * Launch tests of signature
     */
    @Test
    public void testComputeSignatureForMethod() {
        this.assertSignature("void voidNoParameter()", "voidNoParameter::v()", "voidNoParameter");
        this.assertSignature("boolean booleanBoolean(boolean)", "booleanBoolean::b(b)", "booleanBoolean");
        this.assertSignature("char charChar(char)", "charChar::c(c)", "charChar");
        this.assertSignature("int intInt(int)", "intInt::i(i)", "intInt");
        this.assertSignature("long longLong(long)", "longLong::l(l)", "longLong");
        this.assertSignature("float floatFloat(float)", "floatFloat::f(f)", "floatFloat");
        this.assertSignature("double doubleDouble(double)", "doubleDouble::d(d)", "doubleDouble");
        this.assertSignature("String stringString(String)", "stringString::s(s)", "stringString");
        this.assertSignature("void listString(List<String>)", "listString::v([s])", "listString");
        this.assertSignature("void listListString(List<List<String>>)", "listListString::v([[s]])", "listListString");
        this.assertSignature("void mapIntegerString(Map<Integer, String>)", "mapIntegerString::v({is})", "mapIntegerString");
        this.assertSignature("void mapFloatMapStringInteger(Map<Float, Map<String, Integer>>)",
                "mapFloatMapStringInteger::v({f{si}})", "mapFloatMapStringInteger");
        this.assertSignature("List<String> mapStringListString(Map<String, List<String>>)", "mapStringListString::[s]({s[s]})",
                "mapStringListString");
        this.assertSignature("void triplet(Triplet)", "triplet::v((is[s]))", "triplet");
        this.assertSignature("void triplet2(Triplet2)", "triplet2::v((is[s]))", "triplet2");
    }

    @Test
    public void testConvert() {
        Object result = SignatureUtilities.convert(73, Long.class);
        Assert.assertEquals(Long.class, result.getClass());
        result = SignatureUtilities.convert(73, float.class);
        Assert.assertEquals(Float.class, result.getClass());
    }
}
