package com.aldebaran.qi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

class Algorithm {
    /// Functional algorithm

    static <E, Ite extends Iterable<E>> void forEach(Ite input, Consumer<E> consumer) {
        for (E element : input) {
            try {
                consumer.consume(element);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    static <E, Ite extends Iterable<E>> Ite removeIf(Ite input, Predicate<E> predicate) {
        for( Iterator<E> iterator = input.iterator(); iterator.hasNext();)
            if(predicate.test(iterator.next()))
                iterator.remove();

        return input;
    }

    static <E, Ite extends Iterable<E>> Ite removeNull(Ite list) {
        return removeIf(list, new Predicate<E>() {
            @Override
            public boolean test(E e) {
                return Objects.isNull(e);
            }
        });
    }

    static <I, O, Col extends Collection<I>> ArrayList<O> transform(Col input, Function<I, O> function) {
        ArrayList<O> output =  new ArrayList<O>(input.size());

        for (I element : input)
            try {
                output.add(function.execute(element));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }

        return output;
    }

    static <I, O, Ite extends Iterable<I>> ArrayList<O> transform(Ite input, Function<I, O> function) {
        ArrayList<O> output =  new ArrayList<O>();

        for (I element : input)
            try {
                output.add(function.execute(element));
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }

        return output;
    }

    static <E, Ite extends Iterable<E>> E findFirst(Ite input, Predicate<E> predicate) {
        for (E element : input)
            if(predicate.test(element))
                return element;
        return null;
    }

    public static <E, Ite extends Iterable<E>> ArrayList<E> findAll(Ite input, Predicate<E> predicate) {
        ArrayList<E> result = new ArrayList<E>();
        for (E element : input)
            if(predicate.test(element))
                result.add(element);
        return result;
    }

    /// Functional utility

    static <A, B, C> Function<A, C> compose(final Function<B, C> f, final Function<A, B> g) {
        return new Function<A, C>() {
            @Override
            public C execute(A value) throws Throwable {
                return f.execute(g.execute(value));
            }
        };
    }

    static <E> Predicate<E> negate(final Predicate<E> predicate) {
        return new Predicate<E>() {
            @Override
            public boolean test(E element) {
                return ! predicate.test(element);
            }
        };
    }

    /**
     * Create a predicate that is the conjunction of all predicates passed in arguments.
     * This predicate will force evaluation of all predicates.
     */
    static <E> Predicate<E> conjunction(final Predicate<E>... predicates) {
        return new Predicate<E>() {
            @Override
            public boolean test(E element) {
                boolean res = true;
                for (Predicate<E> predicate : predicates) {
                    res &= predicate.test(element);
                }
                return res;
            }
        };
    }


    /**
     * Create a predicate that is the disjunction of all predicates passed in arguments.
     * This predicate will force evaluation of all predicates.
     */
    static <E> Predicate<E> disjunction(final Predicate<E>... predicates) {
        return new Predicate<E>() {
            @Override
            public boolean test(E element) {
                boolean res = false;
                for (Predicate<E> predicate : predicates) {
                    res |= predicate.test(element);
                }
                return res;
            }
        };
    }

    static Predicate<String> contains(final String subString) {
        return new Predicate<String>() {
            @Override
            public boolean test(String string) {
                return string.contains(subString);
            }
        };
    }

    static <T> Function<T, T> identity() {
        return new Function<T, T>() {
            @Override
            public T execute(T value) {
                return value;
            }
        };
    }
}
