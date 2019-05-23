    package com.aldebaran.qi;

    import com.aldebaran.qi.serialization.QiSerializer;
    import org.junit.Test;

    import java.util.NoSuchElementException;
    import java.util.concurrent.ExecutionException;
    import java.util.concurrent.atomic.AtomicBoolean;

    import static org.hamcrest.CoreMatchers.*;
    import static org.junit.Assert.*;

    public class OptionalTest {
        /**
         * A unique exception used for checking exception types.
         */
        private class ObscureException extends RuntimeException { }

        @Test
        public void testEmptyObjectMethods() {
            Optional<Boolean> empty = Optional.empty();
            Optional<String> presentEmptyString = Optional.of("");
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            // empty
            assertThat(empty, is(empty));
            assertThat(empty, is(Optional.<Boolean>empty()));
            assertThat(empty, is(not(present)));
            assertThat(empty.hashCode(), equalTo(0));
            assertThat(empty.toString().isEmpty(), is(false));
            assertThat(empty.toString(), is(not(equalTo(presentEmptyString.toString()))));
            assertThat(empty.isPresent(), is(false));
        }

        @Test
        public void testPresentObjectMethods() {
            Optional<Boolean> empty = Optional.empty();
            Optional<String> presentEmptyString = Optional.of("");
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            // present
            assertThat(present, is(present));
            assertThat(present, is(Optional.of(Boolean.TRUE)));
            assertThat(present, is(not(empty)));
            assertThat(present.hashCode(), equalTo(Boolean.TRUE.hashCode()));
            assertThat(present.toString().isEmpty(), is(false));
            assertThat(present.toString(), is(not(equalTo(presentEmptyString.toString()))));
            assertThat(present.toString().indexOf(Boolean.TRUE.toString()), is(not(equalTo(-1))));
            assertThat(present.get(), is(Boolean.TRUE));
        }

        @Test
        public void testEmptyIfPresentNotConsume() throws Throwable {
            Optional<Boolean> empty = Optional.empty();

            Consumer<Boolean> consumer = new Consumer<Boolean>() {
                @Override
                public void consume(Boolean value) {
                    fail("Should not consume empty optional.");
                }
            };

            empty.ifPresent(consumer);
        }

        @Test
        public void testEmptyIfPresentOrElseDoRunCheck() throws Throwable {
            Optional<Boolean> empty = Optional.empty();

            final AtomicBoolean emptyCheck = new AtomicBoolean(false);

            empty.ifPresentOrElse(new Consumer<Boolean>() {
                @Override
                public void consume(Boolean value) {
                    fail("Should not consume empty optional");
                }
            }, new Runnable() {
                @Override
                public void run() {
                    emptyCheck.set(true);
                }
            });

            assertThat("EmptyAction should check check to true.", emptyCheck.get(), is(true));
        }

        @Test(expected = ObscureException.class)
        public void testEmptyIfPresentOrElseRunCheckThrow() throws Throwable {
            Optional<Boolean> empty = Optional.empty();

            empty.ifPresentOrElse(new Consumer<Boolean>() {
                @Override
                public void consume(Boolean value) {
                    fail("Should not consume empty optional");
                }
            }, new Runnable() {
                @Override
                public void run() {
                    throw new ObscureException();
                }
            });

            fail("Should not exec this line : ifPresentOrElse method should call empty action.");
        }

        @Test
        public void testEmptyOrElse() {
            Optional<Boolean> empty = Optional.empty();

            assertThat(empty.orElse(null), is(nullValue()));
            assertThat(empty.orElse(Boolean.FALSE), is(sameInstance(Boolean.FALSE)));
        }

        @Test
        public void testEmptyOrElseGet() {
            Optional<Boolean> empty = Optional.empty();

            assertThat(empty.orElseGet(new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return null;
                }
            }), is(nullValue()));
            assertThat(empty.orElseGet(new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.FALSE;
                }
            }), is(sameInstance(Boolean.FALSE)));
        }

        @Test
        public void testOptionalIfPresentWithNullNotThrow() throws Throwable {
            Optional<Boolean> empty = Optional.empty();
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            try {
                present.ifPresentOrElse(new Consumer<Boolean>() {
                    @Override
                    public void consume(Boolean value) {}
                }, null);
                empty.ifPresent(null);
                empty.ifPresentOrElse(null, new Runnable() {
                    @Override
                    public void run() {}
                });

            } catch (NullPointerException ex) {
                fail("Should not invoke null Consumer nor runnable.");
            }
        }

        @Test(expected = NullPointerException.class)
        public void testOptionalIfPresentWithNullThrow() throws Throwable {
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            present.ifPresent(null);

            fail("Should not exec this line : ifPresent method should throw calling null action.");
        }

        @Test(expected = NullPointerException.class)
        public void testOptionalIfPresentOrElseWithNullThrow() throws Throwable {
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            present.ifPresentOrElse(null, new Runnable() {
                @Override
                public void run() {}
            });

            fail("Should not exec this line : ifPresentOrElse method should throw calling null action.");
        }

        @Test(expected = NullPointerException.class)
        public void testEmptyIfPresentOrElseWithNullThrow() throws Throwable {
            Optional<Boolean> empty = Optional.empty();

            empty.ifPresentOrElse(new Consumer<Boolean>() {
                @Override
                public void consume(Boolean value) {}
            }, null);

            fail("Should not exec this line : ifPresentOrElse method should throw calling null empty action.");
        }

        @Test(expected = NoSuchElementException.class)
        public void testEmptyGet() {
            Optional<Boolean> empty = Optional.empty();

            @SuppressWarnings("unused")
            Boolean got = empty.get();
        }

        @Test(expected = NullPointerException.class)
        public void testEmptyOrElseGetNull() {
            Optional<Boolean> empty = Optional.empty();

            @SuppressWarnings("unused")
            Boolean got = empty.orElseGet(null);
        }

        @Test(expected = NullPointerException.class)
        public void testEmptyOrElseThrowNull() throws Throwable {
            Optional<Boolean> empty = Optional.empty();

            @SuppressWarnings("unused")
            Boolean got = empty.orElseThrow(null);
        }

        @Test(expected = ObscureException.class)
        public void testEmptyOrElseThrow() throws Throwable {
            Optional<Boolean> empty = Optional.empty();

            @SuppressWarnings("unused")
            Boolean got = empty.orElseThrow(new Supplier<Throwable>() {
                @Override
                public Throwable get() {
                    return new ObscureException();
                }
            });
        }

        @Test
        public void testPresentIfPresentCheck() throws Throwable {
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            final AtomicBoolean presentCheck = new AtomicBoolean(false);

            present.ifPresent(new Consumer<Boolean>() {
                @Override
                public void consume(Boolean value) {
                    presentCheck.set(true);
                }
            });

            assertThat(presentCheck.get(), is(true));
        }

        @Test
        public void testPresentIfPresentOrElseCheck() throws Throwable {
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            final AtomicBoolean presentCheck = new AtomicBoolean(false);

            present.ifPresentOrElse(new Consumer<Boolean>() {
                @Override
                public void consume(Boolean value) {
                    presentCheck.set(true);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    fail("Should not run else with not empty optional");
                }
            });

            assertThat(presentCheck.get(), is(true));
        }

        @Test(expected = ObscureException.class)
        public void testPresentIfPresentThrow() throws Throwable {
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            present.ifPresent(new Consumer<Boolean>() {
                @Override
                public void consume(Boolean value) {
                    throw new ObscureException();
                }
            });

            fail("Should not exec this line : ifPresent method should call consumer action.");
        }

        @Test(expected = ObscureException.class)
        public void testPresentIfPresentOrElseThrow() throws Throwable {
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            present.ifPresentOrElse(new Consumer<Boolean>() {
                @Override
                public void consume(Boolean value) {
                    throw new ObscureException();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    fail("Should not run else with not empty optional.");
                }
            });

            fail("Should not exec this line : ifPresentOrElse method should call consumer action.");
        }

        @Test
        public void testPresentOrElse() {
            Optional<Boolean> present = Optional.of(Boolean.TRUE);

            assertThat(present.orElse(null), is(sameInstance(Boolean.TRUE)));
            assertThat(present.orElse(Boolean.FALSE), is(sameInstance(Boolean.TRUE)));
            assertThat(present.orElseGet(null), is(sameInstance(Boolean.TRUE)));
            assertThat(present.orElseGet(new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return null;
                }
            }), is(sameInstance(Boolean.TRUE)));
            assertThat(present.orElseGet(new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.FALSE;
                }
            }), is(sameInstance(Boolean.TRUE)));
            assertThat(present.<RuntimeException>orElseThrow(null), is(sameInstance(Boolean.TRUE)));
            assertThat(present.<RuntimeException>orElseThrow(null), is(sameInstance(Boolean.TRUE)));
            assertThat(present.<RuntimeException>orElseThrow(new Supplier<ObscureException>() {
                @Override
                public ObscureException get() {
                    return new ObscureException();
                }
            }), is(sameInstance(Boolean.TRUE)));
        }

        @Test(expected = NullPointerException.class)
        public void testFilterWithNullThrow() {
            // Null mapper function
            Optional<String> empty = Optional.empty();

            @SuppressWarnings("unused")
            Optional<String> result = empty.filter(null);

            fail("Should not exec this line : filter method should throw calling null predicate.");
        }

        @Test
        public void testEmptyFilterIsPresent() {
            // Null mapper function
            Optional<String> empty = Optional.empty();

            Optional<String> result = empty.filter(new Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return s.isEmpty();
                }
            });
            assertThat(result.isPresent(), is(false));
        }

        @Test
        public void testOptionalFilterIsPresent() {
            Optional<String> duke = Optional.of("Duke");

            Optional<String> result = duke.filter(new Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return s.isEmpty();
                }
            });
            assertThat(result.isPresent(), is(false));
        }

        @Test
        public void testOptionalFilterIsPresentAndGetWord() {
            Optional<String> duke = Optional.of("Duke");

            Optional<String> result = duke.filter(new Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return s.startsWith("D");
                }
            });
            assertThat(result.isPresent(), is(true));
            assertThat(result.get(), equalTo("Duke"));
        }

        @Test
        public void testOptionalFilterIsPresentAndGetEmpty() {
            Optional<String> emptyString = Optional.of("");

            Optional<String> result = emptyString.filter(new Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return s.isEmpty();
                }
            });
            assertThat(result.isPresent(), is(true));
            assertThat(result.get(), equalTo(""));
        }

        @Test(expected = NullPointerException.class)
        public void testEmptyMapWithNullThrow() throws Throwable {
            Optional<String> empty = Optional.empty();

            @SuppressWarnings("unused")
            Optional<Boolean> b = empty.map(null);

            fail("Should not exec this line : map method should throw calling null mapper.");
        }

        @Test
        public void testEmptyMapNotIsPresent() throws Throwable {
            Optional<String> empty = Optional.empty();

            Optional<Boolean> b = empty.map(new Function<String, Boolean>() {
                    @Override
                    public Boolean execute(String value) {
                        return value.isEmpty();
                    }
                });

            assertThat(b.isPresent(), is(false));
        }

        @Test
        public void testEmptyMapReturnNullNotIsPresent() throws Throwable {
            Optional<String> empty = Optional.empty();

            Optional<Boolean> b = empty.map(new Function<String, Boolean>() {
                    @Override
                    public Boolean execute(String value) {
                        return null;
                    }
                });

            assertThat(b.isPresent(), is(false));
        }

        @Test
        public void testOptionalMapReturnNullNotIsPresent() throws Throwable {
            Optional<String> duke = Optional.of("Duke");

            Optional<Boolean> b = duke.map(new Function<String, Boolean>() {
                    @Override
                    public Boolean execute(String value) {
                        return null;
                    }
                });

            assertThat(b.isPresent(), is(false));
        }

        @Test
        public void testOptionalMapReturnFunctionValue() throws Throwable {
            Optional<String> duke = Optional.of("Duke");

            Optional<Integer> l = duke.map(new Function<String, Integer>() {
                    @Override
                    public Integer execute(String value) {
                        return value.length();
                    }
                });

            assertThat(l.get(), is(4));
        }

        @Test(expected = NullPointerException.class)
        public void testEmptyFlatMapThrow() throws Throwable {
            Optional<String> empty = Optional.empty();

            @SuppressWarnings("unused")
            Optional<Boolean> b = empty.flatMap(null);

            fail("Should not exec this line : flatMap method should throw calling null mapper.");
        }

        @Test(expected = NullPointerException.class)
        public void testOptionalFlatMapToNullThrow() throws Throwable {
            Optional<String> duke = Optional.of("Duke");

            @SuppressWarnings("unused")
            Optional<Boolean> b = duke.flatMap(new Function<String, Optional<Boolean>>() {
                @Override
                public Optional<Boolean> execute(String value) {
                    return null;
                }
            });

            fail("Should not exec this line: flatMap method should throw calling mapper that returns null.");
        }


        @Test
        public void testEmptyFlatMapToNullNotThrow() throws Throwable {
            Optional<String> empty = Optional.empty();

            Optional<Boolean> b = null;

            try {
                b = empty.flatMap(new Function<String, Optional<Boolean>>() {
                    @Override
                    public Optional<Boolean> execute(String value) {
                        return null;
                    }
                });
            } catch (NullPointerException npe) {
                fail("Mapper function should not be invoked.");
            }

            assertThat(b.isPresent(), is(false));
        }

        @Test
        public void testEmptyFlatMapToOptionalNotIsPresent() throws Throwable {
            Optional<String> empty = Optional.empty();

            Optional<Integer> l = empty.flatMap(new Function<String, Optional<? extends Integer>>() {
                @Override
                public Optional<? extends Integer> execute(String value) {
                    return Optional.of(value.length());
                }
            });

            assertThat(l.isPresent(), is(false));
        }

        @Test
        public void testOptionalFlatMapToOptionalIsPresent() throws Throwable {
            Optional<String> duke = Optional.of("Duke");

            Optional<Integer> l = duke.flatMap(new Function<String, Optional<? extends Integer>>() {
                @Override
                public Optional<? extends Integer> execute(String value) {
                    return Optional.of(value.length());
                }
            });

            assertThat(l.isPresent(), is(true));
            assertThat(l.get(), is(4));
        }

        @Test
        public void testOptionalFlatMapCaptureAndReturn() throws Throwable {
            Optional<String> duke = Optional.of("Duke");

            // Map to value
            final Optional<Integer> fixture = Optional.of(Integer.MAX_VALUE);

            // Verify same instance
            Optional<Integer> l = duke.flatMap(new Function<String, Optional<? extends Integer>>() {
                @Override
                public Optional<? extends Integer> execute(String value) {
                    return fixture;
                }
            });

            assertThat(l, is(sameInstance(fixture)));
        }



        @Test(expected = NullPointerException.class)
        public void testOrOnEmptyWithNullSupplier() {
            Optional<String> empty = Optional.empty();

            @SuppressWarnings("unused")
            Optional<String> b = empty.or(null);

            fail("Should not exec this line: or method should throw calling null supplier.");
        }

        @Test(expected = NullPointerException.class)
        public void testOrOnEmptyWithSupplierReturnsNull() {
            Optional<String> empty = Optional.empty();

            @SuppressWarnings("unused")
            Optional<String> b = empty.or(new Supplier<Optional<String>>() {
                @Override
                public Optional<String> get() {
                    return null;
                }
            });

            fail("Should not exec this line: or method should throw calling supplier that returns null.");
        }

        @Test
        public void testOrOnNonEmptyWithSupplierReturnsNull() {
            final Optional<String> duke = Optional.of("Duke");

            Optional<String> b = null;

            try {
                b = duke.or(new Supplier<Optional<String>>() {
                    @Override
                    public Optional<String> get() {
                        return null;
                    }
                });
            } catch (NullPointerException npe) {
                fail("Supplier should not be invoked.");
            }

            assertThat(b.isPresent(), is(true));
        }

        @Test
        public void testOrSuppliedOnEmpty() {
            Optional<String> empty = Optional.empty();
            final Optional<String> duke = Optional.of("Duke");

            // Supply for empty
            Optional<String> suppliedDuke = empty.or(new Supplier<Optional<? extends String>>() {
                @Override
                public Optional<? extends String> get() {
                    return duke;
                }
            });

            assertThat(suppliedDuke.isPresent(), is(true));
            assertThat(suppliedDuke, is(sameInstance(duke)));
        }

        @Test
        public void testOrSuppliedOnActual() {
            final Optional<String> duke = Optional.of("Duke");

            // Supply for non-empty
            Optional<String> actualDuke = duke.or(new Supplier<Optional<? extends String>>() {
                @Override
                public Optional<? extends String> get() {
                    return Optional.of("Other Duke");
                }
            });

            assertThat(actualDuke.isPresent(), is(true));
            assertThat(actualDuke, is(sameInstance(duke)));
        }

        interface ComplexSignatureInterface {
            @SuppressWarnings("unused")
            Optional<String> maybeToString(Optional<Integer> opt) throws Throwable;
        }

        static class ComplexSignature implements ComplexSignatureInterface {
            public Optional<String> maybeToString(Optional<Integer> opt) throws Throwable {
                return opt.map(new Function<Integer, String>() {
                    @Override
                    public String execute(Integer value) {
                        return value.toString();
                    }
                });
            }
        }

        @Test
        public void testAdvertiseMethodWithOptionalWithValueInParameterAndReturnType() {
            DynamicObjectBuilder dynamicObjectBuilder = new DynamicObjectBuilder();

            dynamicObjectBuilder.advertiseMethods(QiSerializer.getDefault(), ComplexSignatureInterface.class, new ComplexSignature());

            try {
                Optional<Integer> opt = Optional.of(34);

                // Tries to call maybeToString with non empty optional.
                Optional<String> suppliedString = dynamicObjectBuilder.object().<Optional<String>>call("maybeToString", opt).get();

                assertThat(suppliedString.isPresent(), is(true));
                assertThat(suppliedString.get(), is(equalTo(opt.get().toString())));
            } catch (ExecutionException e) {
                fail("Call to 'maybeToString' with non empty optional failed: " + e.getMessage() + ".");
            }
        }

        @Test
        public void testAdvertiseMethodWithEmptyOptionalInParameterAndReturnType() {
            DynamicObjectBuilder dynamicObjectBuilder = new DynamicObjectBuilder();

            dynamicObjectBuilder.advertiseMethods(QiSerializer.getDefault(), ComplexSignatureInterface.class, new ComplexSignature());

            try {
                // Tries to call maybeToString with empty optional.
                Optional<String> suppliedString = dynamicObjectBuilder.object().<Optional<String>>call("maybeToString", Optional.empty()).get();

                assertThat(suppliedString.isPresent(), is(false));
            } catch (ExecutionException e) {
                fail("Call to 'maybeToString' with empty optional failed: " + e.getMessage() + ".");
            }
        }

    }