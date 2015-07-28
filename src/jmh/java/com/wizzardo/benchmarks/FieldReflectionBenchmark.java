package com.wizzardo.benchmarks;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.wizzardo.tools.reflection.FieldReflection;

import org.openjdk.jmh.annotations.*;

/**
 * Created by wizzardo on 28.07.15.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class FieldReflectionBenchmark {


    static class TestClass {
        int i = 42;
    }

    FieldReflection fieldReflection;
    TestClass testClass;

    @Setup(Level.Iteration)
    public void setup() throws NoSuchFieldException, IOException {
        testClass = new TestClass();
        fieldReflection = new FieldReflection(TestClass.class, "i");
    }

    @Benchmark
    public int fieldReflection_set() {
        fieldReflection.setInteger(testClass, testClass.i + 1);
        return testClass.i;
    }

    @Benchmark
    public int native_set() {
        testClass.i = testClass.i + 1;
        return testClass.i;
    }

    @Benchmark
    public int fieldReflection_get() {
        return fieldReflection.getInteger(testClass);
    }

    @Benchmark
    public int native_get() {
        return testClass.i;
    }
}
