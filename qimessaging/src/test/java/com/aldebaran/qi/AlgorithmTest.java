package com.aldebaran.qi;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aldebaran.qi.Algorithm.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.everyItem;

public class AlgorithmTest {
    @Test
    public void testForEachWithEmptyIterable() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>();

        forEach(iterable, new Consumer<Boolean>() {
            @Override
            public void consume(Boolean value) {
                fail("Should not consume empty Iterable.");
            }
        });
    }

    @Test
    public void testForEachWithNonEmptyIterable() {
        Iterable<Boolean> iterable = Arrays.asList(Boolean.TRUE);

        final AtomicBoolean consumeCheck = new AtomicBoolean(false);

        forEach(iterable, new Consumer<Boolean>() {
            @Override
            public void consume(Boolean value) {
                consumeCheck.set(true);
            }
        });

        assertThat("During evaluation, the consumer should have set check to true", consumeCheck.get(), is(true));
    }

    @Test
    public void testRemoveIfWithEmptyIterable() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>();

        removeIf(iterable, new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                fail("The predicate should not be called with empty iterable");
                return false;
            }
        });
    }

    @Test
    public void testRemoveIfWithNonEmptyIterable() {
        List<Boolean> list = new ArrayList<Boolean>(Arrays.asList(Boolean.TRUE, Boolean.FALSE));

        final AtomicBoolean predicateCheck = new AtomicBoolean(false);

        removeIf(list, new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                predicateCheck.set(true);
                return aBoolean.booleanValue();
            }
        });

        assertThat("RemoveIf should have removed one element", list.size(), is(1));
        assertThat("The remaining element in list should be Boolean.FALSE", list.get(0), is(Boolean.FALSE));
        assertThat("During evaluation, the predicate should have set check to true", predicateCheck.get(), is(true));
    }

    @Test
    public void testRemoveNullWithEmptyIterable() {
        ArrayList<Boolean> list = new ArrayList<Boolean>();

        removeNull(list);

        assertThat("The list should remain empty", list.size(),  is(0));
        assertThat("The list should remain the same", list, is(new ArrayList<Boolean>()));
    }

    @Test
    public void testRemoveNullWithNonEmptyIterable() {
        List<Boolean> list = new ArrayList<Boolean>(Arrays.asList(Boolean.TRUE, null, Boolean.FALSE));

        removeNull(list);

        assertThat("The list should have 2 elements remaining", list.size(), is(2));
        assertThat("The list should remain the same", list, everyItem(is(notNullValue(Boolean.class))));
    }

    @Test
    public void testTransformWithEmptyCollection() {
        ArrayList<Boolean> collection = new ArrayList<Boolean>();

        ArrayList<Boolean> result = transform(collection, new Function<Boolean, Boolean>() {
            @Override
            public Boolean execute(Boolean value) {
                fail("The function should not be called with empty collection");
                return value;
            }
        });

        assertThat("The result should remain empty", result.size(), is(0));
    }

    @Test
    public void testTransformWithEmptyIterable() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>();

        ArrayList<Boolean> result = transform(iterable, new Function<Boolean, Boolean>() {
            @Override
            public Boolean execute(Boolean value) {
                fail("The function should not be called with empty iterable");
                return value;
            }
        });

        assertThat("The result should remain empty", result.size(), is(0));
    }

    @Test
    public void testTransformWithNonEmptyCollection() {
        ArrayList<Boolean> collection = new ArrayList<Boolean>(Arrays.asList(Boolean.TRUE));

        final AtomicBoolean functionCheck = new AtomicBoolean(false);

        ArrayList<Boolean> result = transform(collection, new Function<Boolean, Boolean>() {
            @Override
            public Boolean execute(Boolean value) {
                functionCheck.set(true);
                return ! value;
            }
        });

        assertThat("The result should have the same size than input", result.size(), is(equalTo(collection.size())));
        assertThat("The result should have opposite element than input", result.get(0), is(false));
        assertThat("During evaluation, the function should have set check to true", functionCheck.get(), is(true));
    }

    @Test
    public void testTransformWithNonEmptyIterable() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>(Arrays.asList(Boolean.TRUE));

        final AtomicBoolean functionCheck = new AtomicBoolean(false);

        ArrayList<Boolean> result = transform(iterable, new Function<Boolean, Boolean>() {
            @Override
            public Boolean execute(Boolean value) {
                functionCheck.set(true);
                return ! value;
            }
        });

        assertThat("The result should have the same size than input", result.size(), is(1));
        assertThat("The result should have opposite element than input", result.get(0), is(false));
        assertThat("During evaluation, the function should have set check to true", functionCheck.get(), is(true));
    }

    @Test
    public void testFindFirstWithEmptyIterable() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>();

        Boolean result = findFirst(iterable, new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                fail("The predicate should not be called with empty iterable");
                return true;
            }
        });

        assertThat("The result should be null", result, is(nullValue(Boolean.class)));
    }

    @Test
    public void testFindFirstWithNonEmptyIterableThatFind() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>(Arrays.asList(Boolean.FALSE, Boolean.TRUE));

        final AtomicBoolean predicateCheck = new AtomicBoolean(false);

        Boolean result = findFirst(iterable, new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                predicateCheck.set(true);
                return aBoolean;
            }
        });

        assertThat("The result should be true element", result, is(true));
        assertThat("During evaluation, the predicate should have set check to true", predicateCheck.get(), is(true));
    }

    @Test
    public void testFindFirstWithNonEmptyIterableThatNotFind() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>(Arrays.asList(Boolean.FALSE, Boolean.FALSE));

        final AtomicBoolean predicateCheck = new AtomicBoolean(false);

        Boolean result = findFirst(iterable, new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                predicateCheck.set(true);
                return aBoolean;
            }
        });

        assertThat("The result should be null", result, is(nullValue(Boolean.class)));
        assertThat("During evaluation, the predicate should have set check to true", predicateCheck.get(), is(true));
    }

    @Test
    public void testFindAllWithEmptyIterable() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>();

        ArrayList<Boolean> result = findAll(iterable, new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                fail("The predicate should not be called with empty iterable");
                return true;
            }
        });

        assertThat("The result list should be empty as the input", result.size(), is(0));
    }

    @Test
    public void testFindAllWithNonEmptyIterableThatFind() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>(Arrays.asList(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE));

        final AtomicBoolean predicateCheck = new AtomicBoolean(false);

        ArrayList<Boolean> result = findAll(iterable, new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                predicateCheck.set(true);
                return aBoolean;
            }
        });

        assertThat("The result list should have 2 elements", result.size(), is(2));
        assertThat("The list should only have true items", result, everyItem(is(true)));
        assertThat("During evaluation, the predicate should have set check to true", predicateCheck.get(), is(true));
    }

    @Test
    public void testFindAllWithNonEmptyIterableThatNotFind() {
        Iterable<Boolean> iterable = new ArrayList<Boolean>(Arrays.asList(Boolean.FALSE, Boolean.FALSE));

        final AtomicBoolean predicateCheck = new AtomicBoolean(false);

        ArrayList<Boolean> result = findAll(iterable, new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                predicateCheck.set(true);
                return aBoolean;
            }
        });

        assertThat("The result list should have 2 elements", result.size(), is(0));
        assertThat("During evaluation, the predicate should have set check to true", predicateCheck.get(), is(true));
    }

    @Test
    public void testCompose() {
        final AtomicBoolean
            fCheck1 = new AtomicBoolean(false),
            fCheck2 = new AtomicBoolean(false);

        Function<Integer, Boolean> composed = compose(new Function<String, Boolean>() {
            @Override
            public Boolean execute(String value) {
                fCheck2.set(fCheck1.get()); // Should be checked second
                return value.length() < 3;
            }
        }, new Function<Integer, String>() {
            @Override
            public String execute(Integer value) {
                fCheck1.set(true); // Should be checked first
                return value.toString();
            }
        });

        Boolean result = null;
        try {
            result = composed.execute(42);
        } catch (Throwable throwable) {
            fail("The function should not throw any exception");
        }

        assertThat("The result should be true", result, is(true));

        assertThat("During evaluation, the functions should have set checks to true",
                Arrays.asList(fCheck1.get(), fCheck2.get()), everyItem(is(true)));

    }

    @Test
    public void testNegate() {
        final AtomicBoolean predicateCheck = new AtomicBoolean(false);

        Predicate<Integer> isGreaterThan42 = new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) {
                predicateCheck.set(true);
                return integer > 42;
            }
        };
        Predicate<Integer> isFeawerThan42 = negate(isGreaterThan42);

        assertThat("The negate predicate is always different than the base predicate",
                isGreaterThan42.test(1), is(not(isFeawerThan42.test(1))));
        assertThat("The negate predicate is always different than the base predicate",
                isGreaterThan42.test(50), is(not(isFeawerThan42.test(50))));

        assertThat("During evaluation, the predicate should have set check to true", predicateCheck.get(), is(true));
    }

    @Test
    public void testConjunction() {
        final AtomicBoolean
                pCheck1 = new AtomicBoolean(false),
                pCheck2 = new AtomicBoolean(false);

        Predicate<Integer> isEven = new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) {
                pCheck1.set(true);
                return (integer % 2) == 0;
            }
        };

        Predicate<Integer> isGreaterThan42 = new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) {
                pCheck2.set(true);
                return integer > 42;
            }
        };

        Predicate<Integer> isEvenAndGreaterThan42 = conjunction(isEven, isGreaterThan42);

        boolean result = isEvenAndGreaterThan42.test(1);

        assertThat("During evaluation, all checks should have been set", Arrays.asList(pCheck1.get(), pCheck2.get()), everyItem(is(true)));
        assertThat("The result should be false", result, is(false));

        pCheck1.set(false); pCheck2.set(false);

        result = isEvenAndGreaterThan42.test(50);

        assertThat("During evaluation, all checks should have been set", Arrays.asList(pCheck1.get(), pCheck2.get()), everyItem(is(true)));
        assertThat("The result should be true", result, is(true));

    }

    @Test
    public void testDisjunction() {
        final AtomicBoolean
                pCheck1 = new AtomicBoolean(false),
                pCheck2 = new AtomicBoolean(false);

        Predicate<Integer> isEven = new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) {
                pCheck1.set(true);
                return (integer % 2) == 0;
            }
        };

        Predicate<Integer> isGreaterThan42 = new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) {
                pCheck2.set(true);
                return integer > 42;
            }
        };

        Predicate<Integer> isEvenOrGreaterThan42 = disjunction(isEven, isGreaterThan42);

        boolean result = isEvenOrGreaterThan42.test(1);

        assertThat("During evaluation, all checks should have been set", Arrays.asList(pCheck1.get(), pCheck2.get()), everyItem(is(true)));
        assertThat("The result should be false", result, is(false));

        pCheck1.set(false); pCheck2.set(false);

        result = isEvenOrGreaterThan42.test(51);

        assertThat("During evaluation, all checks should have been set", Arrays.asList(pCheck1.get(), pCheck2.get()), everyItem(is(true)));
        assertThat("The result should be true", result, is(true));

    }

    @Test
    public void testContain() {
        Predicate<String> containString = contains("string");

        assertThat(containString.test("This is a string"), is(true));
        assertThat(containString.test("string string"), is(true));
        assertThat(containString.test("This is a String"), is(false));
        assertThat(containString.test(""), is(false));
    }

    @Test
    public void testIdentity() {
        Function<Boolean, Boolean> idBoolean = identity();

        try {
            assertThat(idBoolean.execute(true), is(true));
            assertThat(idBoolean.execute(false), is(false));
        } catch (Throwable throwable) {
            fail("The function should not throw any exception");
        }
    }
}