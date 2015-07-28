package com.wizzardo.benchmarks;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.wizzardo.tools.reflection.FieldReflection;
import com.wizzardo.tools.misc.Unchecked;

import org.openjdk.jmh.annotations.*;

/**
 * Created by wizzardo on 28.07.15.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
public class FieldReflectionBenchmark {

//    Benchmark                                                   Mode  Cnt   Score   Error  Units
//    FieldReflectionBenchmark.fieldReflection_get                avgt   10   4.225 ± 0.171  ns/op
//    FieldReflectionBenchmark.fieldReflection_set                avgt   10   6.480 ± 1.151  ns/op
//    FieldReflectionBenchmark.integerReflectionGetterSetter_get  avgt   10  10.305 ± 0.888  ns/op
//    FieldReflectionBenchmark.integerReflectionGetterSetter_set  avgt   10  10.576 ± 0.361  ns/op
//    FieldReflectionBenchmark.integerUnsafeGetterSetter_get      avgt   10   3.343 ± 0.076  ns/op
//    FieldReflectionBenchmark.integerUnsafeGetterSetter_set      avgt   10   5.786 ± 1.054  ns/op
//    FieldReflectionBenchmark.native_get                         avgt   10   2.964 ± 0.116  ns/op
//    FieldReflectionBenchmark.native_set                         avgt   10   3.292 ± 0.105  ns/op


    static class TestClass {
        int i = 42;
    }

    static class IntegerReflectionGetterSetter extends FieldReflection {

        public IntegerReflectionGetterSetter(Class clazz, String name) throws NoSuchFieldException {
            super(clazz, name);
        }

        @Override
        public void setInteger(Object object, int value) {
            try {
                field.setInt(object, value);
            } catch (IllegalAccessException e) {
                throw Unchecked.rethrow(e);
            }
        }

        @Override
        public int getInteger(Object object) {
            try {
                return field.getInt(object);
            } catch (IllegalAccessException e) {
                throw Unchecked.rethrow(e);
            }
        }
    }

    static class IntegerUnsafeGetterSetter extends FieldReflection {

        public IntegerUnsafeGetterSetter(Class clazz, String name) throws NoSuchFieldException {
            super(clazz, name);
        }

        @Override
        public int getInteger(Object object) {
            return unsafe.getInt(object, offset);
        }

        @Override
        public void setInteger(Object object, int value) {
            unsafe.putInt(object, offset, value);
        }
    }

    FieldReflection fieldReflection;
    TestClass testClass;
    IntegerUnsafeGetterSetter integerUnsafeGetterSetter;
    IntegerReflectionGetterSetter integerReflectionGetterSetter;

    @Setup(Level.Iteration)
    public void setup() throws NoSuchFieldException, IOException {
        testClass = new TestClass();
        fieldReflection = new FieldReflection(TestClass.class, "i");
        integerUnsafeGetterSetter = new IntegerUnsafeGetterSetter(TestClass.class, "i");
        integerReflectionGetterSetter = new IntegerReflectionGetterSetter(TestClass.class, "i");
    }

    @Benchmark
    public int fieldReflection_set() {
        int value = getInt();
        fieldReflection.setInteger(testClass, value);
        return value;
    }

    @Benchmark
    public int fieldReflection_get() {
        return fieldReflection.getInteger(testClass);
    }

    @Benchmark
    public int integerUnsafeGetterSetter_set() {
        int value = getInt();
        integerUnsafeGetterSetter.setInteger(testClass, value);
        return value;
    }

    @Benchmark
    public int integerUnsafeGetterSetter_get() {
        return integerUnsafeGetterSetter.getInteger(testClass);
    }

    @Benchmark
    public int integerReflectionGetterSetter_set() {
        int value = getInt();
        integerReflectionGetterSetter.setInteger(testClass, value);
        return value;
    }

    @Benchmark
    public int integerReflectionGetterSetter_get() {
        return integerReflectionGetterSetter.getInteger(testClass);
    }

    @Benchmark
    public int native_set() {
        int value = getInt();
        testClass.i = value;
        return value;
    }

    @Benchmark
    public int native_get() {
        return testClass.i;
    }

    @CompilerControl(value = CompilerControl.Mode.DONT_INLINE)
    private int getInt() {
        return 42;
    }
}
