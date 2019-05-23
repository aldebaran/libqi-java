package com.aldebaran.qi;

import java.util.NoSuchElementException;

/**
 * For documentation purpose, please refer to
 * https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html
 * TODO : remove when java 1.8
 */
public final class Optional<T> {
    private static final Optional<?> EMPTY = new Optional();

    private final T value;

    private Optional() {
        this.value = null;
    }

    public static <T> Optional<T> empty(){
        @SuppressWarnings("unchecked")
        Optional<T> t = (Optional<T>) EMPTY;
        return t;
    }

    private Optional(T value) {
        this.value = Objects.requireNonNull(value);
    }

    public static <T> Optional<T> of(T value) {
        return new Optional<T>(value);
    }

    public static <T> Optional<T> ofNullable(T value) {
        return value == null ? Optional.<T>empty() : of(value);
    }

    public T get() {
        if (!isPresent()) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        return Objects.nonNull(value);
    }

    public void ifPresent(Consumer<? super T> action) throws Throwable {
        if (isPresent()) {
            action.consume(value);
        }
    }

    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) throws Throwable {
        if (isPresent()) {
            action.consume(value);
        } else {
            emptyAction.run();
        }
    }

    public Optional<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (isPresent()) {
            return predicate.test(value) ? this : Optional.<T>empty();
        } else {
            return this;
        }
    }

    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) throws Throwable {
        Objects.requireNonNull(mapper);
        if (isPresent()) {
            return Optional.ofNullable(mapper.execute(value));
        } else {
            return empty();
        }
    }

    public <U> Optional<U> flatMap(Function<? super T, ? extends Optional<? extends U>> mapper) throws Throwable {
        Objects.requireNonNull(mapper);
        if (isPresent()) {
            @SuppressWarnings("unchecked")
            Optional<U> r = (Optional<U>) mapper.execute(value);
            return Objects.requireNonNull(r);
        } else {
            return empty();
        }
    }

    public Optional<T> or(Supplier<? extends Optional<? extends T>> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) {
            return this;
        } else {
            @SuppressWarnings("unchecked")
            Optional<T> r = (Optional<T>) supplier.get();
            return Objects.requireNonNull(r);
        }
    }

    // Stream not available before JAVA 1.8
//    public Stream<T> stream() {
//        if (!isPresent()) {
//            return Stream.empty();
//        } else {
//            return Stream.of(value);
//        }
//    }

    public T orElse(T other) {
        return isPresent() ? value : other;
    }

    public T orElseGet(Supplier<? extends T> supplier) {
        return isPresent() ? value : supplier.get();
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isPresent()) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Optional)) {
            return false;
        }

        Optional<?> other = (Optional<?>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return isPresent()
                ? String.format("Optional[%s]", Objects.toString(value))
                : "Optional.empty";
    }
}
