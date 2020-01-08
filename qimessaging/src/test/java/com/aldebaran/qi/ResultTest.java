package com.aldebaran.qi;

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ResultTest {
    @Test(expected = NullPointerException.class)
    public void testOfWithNull() {
        Result.of(null);
        fail("Should not exec this line : of method should not be able to return the value from null value.");
    }

    @Test(expected = NullPointerException.class)
    public void testErrorWithNull() {
        Result.error(null);
        fail("Should not exec this line : error method should not be able to return the value from null error.");
    }

    @Test
    public void testGetValueWithValue() {
        assertThat("", Result.of(42).get(), is(42));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetValueWithErrorThrow() {
        Result.error(42).get();

        fail("Should not exec this line : getValue method should not be able to return the value.");
    }

    @Test
    public void testGetErrorWithError() {
        assertThat("", Result.error(42).getErr(), is(42));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetErrorWithValueThrow() {
        Result.of(42).getErr();

        fail("Should not exec this line : getError method should not be able to return the value.");
    }

    @Test
    public void testIsPresent() {
        assertThat("Result of value must be present", Result.of(42).isPresent(), is(true));
        assertThat("Result of error must not be present", Result.error(42).isPresent(), is(false));
    }

    private void testIfPresent(Result<Integer, Integer> res) throws Throwable {
        final AtomicBoolean check = new AtomicBoolean(false);

        res.ifPresent(new Consumer<Integer>() {
            @Override
            public void consume(Integer value) throws Throwable {
                assertThat("The value must be 42", value, is(42));
                check.set(true);
            }
        });

        assertThat("The consumer must be executed", res.isPresent(), is(check.get()));
    }

    @Test
    public void testIfPresentWithValueWillConsume() throws Throwable {
        Result<Integer, Integer> resValue = Result.of(42);
        testIfPresent(resValue);

        Result<Integer, Integer> resError = Result.error(42);
        testIfPresent(resError);
    }

    @Test(expected = NullPointerException.class)
    public void testIfPresentWithNullWillThrow() throws Throwable {
        Result.of(42).ifPresent(null);

        fail("ifPresent must throw if consumer if null");
    }


    private void testIfErrPresent(Result<Integer, Integer> res) throws Throwable {
        final AtomicBoolean check = new AtomicBoolean(false);

        res.ifErrPresent(new Consumer<Integer>() {
            @Override
            public void consume(Integer value) throws Throwable {
                assertThat("The value must be 42", value, is(42));
                check.set(true);
            }
        });

        assertThat("The consumer must be executed", res.isPresent(), is(not(check.get())));
    }

    @Test
    public void testIfErrPresentWithValueWillConsume() throws Throwable {
        Result<Integer, Integer> resValue = Result.of(42);
        testIfErrPresent(resValue);

        Result<Integer, Integer> resError = Result.error(42);
        testIfErrPresent(resError);
    }

    @Test(expected = NullPointerException.class)
    public void testIfErrPresentWithNullWillThrow() throws Throwable {
        Result.of(42).ifErrPresent(null);

        fail("ifPresent must throw if consumer if null");
    }

    private void testIfPresentOrElse(Result<Integer, Integer> res) throws Throwable {
        final AtomicBoolean
                checkValue = new AtomicBoolean(false),
                checkError = new AtomicBoolean(false);

        res.ifPresentOrElse(
                new Consumer<Integer>() {
                    @Override
                    public void consume(Integer value) throws Throwable {
                        assertThat("The value must be 42", value, is(42));
                        checkValue.set(true);
                    }
                },
                new Consumer<Integer>() {
                    @Override
                    public void consume(Integer value) throws Throwable {
                        assertThat("The error must be 42", value, is(42));
                        checkError.set(true);
                    }
                });

        assertThat("If res is in value state, value consume must be call", res.isPresent(), is(checkValue.get()));
        assertThat("If res is in error state, error consume must be call", res.isPresent(), is(not(checkError.get())));
    }

    @Test
    public void testIfPresentOrElseWillConsumeValueOrError() throws Throwable {
        Result<Integer, Integer> resValue = Result.of(42);
        testIfPresentOrElse(resValue);

        Result<Integer, Integer> resError = Result.error(42);
        testIfPresentOrElse(resError);
    }

    @Test(expected = NullPointerException.class)
    public void testIfPresentOrElseWillThrowWithValueIfNull() throws Throwable {
        Result.<Integer, Integer>of(42).ifPresentOrElse(
                new Consumer<Integer>() {
                    @Override
                    public void consume(Integer value) throws Throwable {

                    }
                }, null);

        fail("ifPresentOrElse must throw if one of consumer is null");
    }

    @Test(expected = NullPointerException.class)
    public void testIfPresentOrElseWillThrowWithErrorIfNull() throws Throwable {
        Result.<Integer, Integer>error(42).ifPresentOrElse(
                null,
                new Consumer<Integer>() {
                    @Override
                    public void consume(Integer value) throws Throwable {

                    }
                });

        fail("ifPresentOrElse must throw if one of consumer is null");
    }

    private void testMap(Result<Integer, Integer> resInt) throws Throwable {
        final AtomicBoolean check = new AtomicBoolean(false);

        Result<Boolean, Integer> resBool = resInt.map(new Function<Integer, Boolean>() {
            @Override
            public Boolean execute(Integer value) throws Throwable {
                check.set(true);
                return value == 42;
            }
        });

        assertThat("Check must be set only if resInt is in value state", resInt.isPresent(), is(check.get()));
        assertThat("resBool and resInt must be in the same state", resInt.isPresent(), is(resBool.isPresent()));
    }

    @Test
    public void testMapWillExecute() throws Throwable {
        Result<Integer, Integer> resValue = Result.of(42);
        testMap(resValue);

        Result<Integer, Integer> resError = Result.error(42);
        testMap(resError);
    }

    @Test(expected = NullPointerException.class)
    public void testMapWillThrowIfNullWithValue() throws Throwable {
        Result.of(42).map(null);

        fail("map must throw if the function is null");
    }

    @Test(expected = NullPointerException.class)
    public void testMapWillThrowIfNullWithError() throws Throwable {
        Result.error(42).map(null);

        fail("map must throw if the function is null");
    }

    private void testMapErr(Result<Integer, Integer> resInt) throws Throwable {
        final AtomicBoolean check = new AtomicBoolean(false);

        Result<Integer, Boolean> resBool = resInt.mapErr(new Function<Integer, Boolean>() {
            @Override
            public Boolean execute(Integer value) throws Throwable {
                check.set(true);
                return value == 42;
            }
        });

        assertThat("Check must be set only if resInt is not in value state", resInt.isPresent(), is(not(check.get())));
        assertThat("resBool and resInt must be in the same state", resInt.isPresent(), is(resBool.isPresent()));
    }

    @Test
    public void testMapErrWillExecute() throws Throwable {
        Result<Integer, Integer> resValue = Result.of(42);
        testMapErr(resValue);

        Result<Integer, Integer> resError = Result.error(42);
        testMapErr(resError);
    }

    @Test(expected = NullPointerException.class)
    public void testMapErrWillThrowIfNullWithValue() throws Throwable {
        Result.of(42).mapErr(null);

        fail("map must throw if the function is null");
    }

    @Test(expected = NullPointerException.class)
    public void testMapErrWillThrowIfNullWithError() throws Throwable {
        Result.error(42).mapErr(null);

        fail("map must throw if the function is null");
    }

    private void testFlatMap(Result<Integer, Integer> res) throws Throwable {
        final AtomicBoolean check = new AtomicBoolean(false);

        Result<Integer, Integer> mapRes = res.flatMap(new Function<Integer, Result<Integer, Integer>>() {
            @Override
            public Result<Integer, Integer> execute(Integer value) throws Throwable {
                check.set(true);
                return Result.of(value);
            }
        });

        assertThat("Check must be set only if res is in value state", res.isPresent(), is(check.get()));
        assertThat("If res is not present, flatMap return this", res == mapRes, is(not(res.isPresent())));
        assertThat("res and mapRes must be in the same state", res.isPresent(), is(mapRes.isPresent()));
    }

    @Test
    public void testFlatMapWillExecute() throws Throwable {
        Result<Integer, Integer> resValue = Result.of(42);
        testFlatMap(resValue);

        Result<Integer, Integer> resError = Result.error(42);
        testFlatMap(resError);
    }

    @Test(expected = NullPointerException.class)
    public void testFlatMapWillThrowIfNullWithValue() throws Throwable {
        Result.of(42).flatMap(null);

        fail("flatMap must throw if the function is null");
    }

    @Test(expected = NullPointerException.class)
    public void testFlatMapWillThrowIfNullWithError() throws Throwable {
        Result.error(42).flatMap(null);

        fail("flatMap must throw if the function is null");
    }

    private void testFlatMapErr(Result<Integer, Integer> res) throws Throwable {
        final AtomicBoolean check = new AtomicBoolean(false);

        Result<Integer, Integer> mapRes = res.flatMapErr(new Function<Integer, Result<Integer, Integer>>() {
            @Override
            public Result<Integer, Integer> execute(Integer value) throws Throwable {
                check.set(true);
                return Result.error(value);
            }
        });

        assertThat("Check must be set only if res is not in value state", res.isPresent(), is(not(check.get())));
        assertThat("If res is present, flatMapErr return this", res == mapRes, is(res.isPresent()));
        assertThat("res and mapRes must be in the same state", res.isPresent(), is(mapRes.isPresent()));
    }

    @Test
    public void testFlatMapErrWillExecute() throws Throwable {
        Result<Integer, Integer> resValue = Result.of(42);
        testFlatMapErr(resValue);

        Result<Integer, Integer> resError = Result.error(42);
        testFlatMapErr(resError);
    }

    @Test(expected = NullPointerException.class)
    public void testFlatMapErrWillThrowIfNullWithValue() throws Throwable {
        Result.of(42).flatMapErr(null);

        fail("flatMapErr must throw if the function is null");
    }

    @Test(expected = NullPointerException.class)
    public void testFlatMapErrWillThrowIfNullWithError() throws Throwable {
        Result.error(42).flatMapErr(null);

        fail("flatMapErr must throw if the function is null");
    }

    void testOr(Result<Integer, Integer> res) {
        final AtomicBoolean check = new AtomicBoolean(false);

        Result<Integer, Integer> orRes = res.or(new Supplier<Result<Integer, Integer>>() {
            @Override
            public Result<Integer, Integer> get() {
                check.set(true);
                return Result.of(42);
            }
        });

        assertThat("Supplier is only call if res is not in value state", res.isPresent(), is(not(check.get())));
        assertThat("If res is present, or return this", res == orRes, is(res.isPresent()));
        assertThat("orRes must be in value state", orRes.isPresent(), is(true));
    }

    @Test
    public void testOrWillExecute() {
        Result<Integer, Integer> resValue = Result.of(42);
        testOr(resValue);

        Result<Integer, Integer> resError = Result.error(42);
        testOr(resError);
    }

    @Test(expected = NullPointerException.class)
    public void testOrWillThrowIfNullWithValue() {
        Result.of(42).or(null);

        fail("or must throw if the function is null");
    }

    @Test(expected = NullPointerException.class)
    public void testOrWillThrowIfNullWithError() {
        Result.error(42).or(null);

        fail("or must throw if the function is null");
    }

    @Test
    public void testOrElse() {
        Integer valueRes = Result.of(42).orElse(24);
        assertThat("valueRes get result value", valueRes, is(42));

        Integer errorRes = Result.<Integer, Integer>error(42).orElse(24);
        assertThat("errorRes get else value", errorRes, is(24));
    }

    @Test
    public void testOrElseGet() {
        final AtomicBoolean check = new AtomicBoolean(false);

        Supplier<Integer> supplier = new Supplier<Integer>() {
            @Override
            public Integer get() {
                check.set(true);
                return 24;
            }
        };

        Integer valueRes = Result.of(42).orElseGet(supplier);

        assertThat("valueRes get result value", valueRes, is(42));
        assertThat("Supplier is not called", check.get(), is(false));

        check.set(false);

        Integer errorRes = Result.<Integer, Integer>error(42).orElseGet(supplier);
        assertThat("errorRes get else value", errorRes, is(24));
        assertThat("Supplier is called", check.get(), is(true));
    }

    @Test
    public void testNestedResult() throws Throwable {
        Result<Integer, Integer> res = Result.of(42);

        Result<Result<Integer, Integer>, Integer> resOfRes = Result.of(res);

        assertThat(resOfRes.isPresent(), is(true));
        assertThat(resOfRes.get().isPresent(), is(true));

        Result<Integer, Integer> mappedRes = resOfRes.flatMap(new Function<Result<Integer, Integer>, Result<Integer, Integer>>() {
            @Override
            public Result<Integer, Integer> execute(Result<Integer, Integer> value) throws Throwable {
                return value.flatMap(new Function<Integer, Result<Integer, Integer>>() {
                    @Override
                    public Result<Integer, Integer> execute(Integer value) throws Throwable {
                        return Result.of(value * 2);
                    }
                });
            }
        });

        assertThat(mappedRes.isPresent(), is(true));
        assertThat(mappedRes.get(), is(res.get() * 2));
    }

}
