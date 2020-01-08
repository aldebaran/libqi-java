package com.aldebaran.qi;

import java.util.NoSuchElementException;

/**
 * An object which may contain either a non-null value or a non-null error.
 *
 * Iff a value is present, isPresent() will return true, getValue() will return the value and getError() will throw
 * NoSuchElementException. Otherwise, isPresent() will return false, getValue() will throw NoSuchElementException
 * and getError() will return the error.
 *
 * Additional methods that depend on the presence of a contained value or an error are provided, such as orElse() and
 * ifPresent().
 *
 * @warning This is a value-based class; use of identity-sensitive operations (including reference equality (==), identity hash,
 * or synchronization) on instances of Result may have unpredictable results and should be avoided.
 *
 * @param <T> The value type.
 * @param <E> The error type.
 */
class Result<T, E> {
    private final T value;
    private final E err;

    private Result(T value, E err) {
        this.value = value;
        this.err = err;
    }

    public static <T, E> Result<T, E> of(T value) {
        return new Result<T, E>(Objects.requireNonNull(value), null);
    }

    public static <T, E> Result<T, E> error(E err) {
        return new Result<T, E>(null, Objects.requireNonNull(err));
    }

    private <U, F> Result<U, F> selfCast() {
        return Objects.uncheckedCast(this);
    }

    public T get() {
        if (!isPresent()) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public E getErr() {
        if (isPresent()) {
            throw new NoSuchElementException("No error present");
        }
        return err;
    }

    boolean isPresent() {
        return Objects.nonNull(value);
    }

    void ifPresent(Consumer<? super T> action) throws Throwable {
        Objects.requireNonNull(action);
        if (isPresent()) {
            action.consume(value);
        }
    }

    void ifErrPresent(Consumer<? super E> action) throws Throwable {
        Objects.requireNonNull(action);
        if (!isPresent()) {
            action.consume(err);
        }
    }

    void ifPresentOrElse(Consumer<? super T> valueAction, Consumer<? super E> errorAction) throws Throwable {
        Objects.requireNonNull(valueAction);
        Objects.requireNonNull(errorAction);
        if (isPresent()) {
            valueAction.consume(value);
        } else {
            errorAction.consume(err);
        }
    }

    <U> Result<U, E> map(Function<? super T, ? extends U> mapper) throws Throwable {
        Objects.requireNonNull(mapper);
        if (isPresent()) {
            return Result.of(mapper.execute(value));
        } else {
            return selfCast();
        }
    }

    <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) throws Throwable {
        Objects.requireNonNull(mapper);
        if (isPresent()) {
            return selfCast();
        } else {
            return Result.error(mapper.execute(err));
        }
    }

    <U> Result<U, E> flatMap(Function<? super T, ? extends Result<? extends U, E>> mapper) throws Throwable {
        Objects.requireNonNull(mapper);
        if (isPresent()) {
            Result<U, E> r = Objects.uncheckedCast(mapper.execute(value));
            return Objects.requireNonNull(r);
        } else {
            return selfCast();
        }
    }

    <F> Result<T, F> flatMapErr(Function<? super E, ? extends Result<T, ? extends F>> mapper) throws Throwable {
        Objects.requireNonNull(mapper);
        if (isPresent()) {
            return selfCast();
        } else {
            Result<T, F> r = Objects.uncheckedCast(mapper.execute(err));
            return Objects.requireNonNull(r);
        }
    }

    public Result<T, E> or(Supplier<? extends Result<? extends T, ? extends E>> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) {
            return this;
        } else {
            Result<T, E> r = Objects.uncheckedCast(supplier.get());
            return Objects.requireNonNull(r);
        }
    }

    T orElse(T other) {
        return isPresent() ? value : other;
    }

    T orElseGet(Supplier<? extends T> supplier) {
        return isPresent() ? value : supplier.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Result)) {
            return false;
        }

        Result<?, ?> other = Objects.uncheckedCast(obj);
        return Objects.equals(value, other.value) && Objects.equals(err, other.err);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, err);
    }

    @Override
    public String toString() {
        return isPresent()
                ? String.format("Ok(%s)", Objects.toString(value))
                : String.format("Err(%s)", Objects.toString(err));
    }
}
