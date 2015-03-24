package com.wizzardo.benchmarks;


import org.apache.commons.lang3.SerializationUtils;
import org.openjdk.jmh.annotations.*;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by wizzardo on 13.01.15.
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class CloneBenchmark {

    static class TestClass implements Cloneable, Serializable {
        String value;

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        TestClass simpleClone() {
            TestClass clone = new TestClass();
            clone.value = value;
            return clone;
        }
    }

    static class TestClass2 extends TestClass {
        Integer another;

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        TestClass2 simpleClone() {
            TestClass2 clone = new TestClass2();
            clone.value = value;
            clone.another = another;
            return clone;
        }
    }

    TestClass object_1;

    @Setup(Level.Iteration)
    public void setup() throws CloneNotSupportedException {
        object_1 = new TestClass2();
        object_1.value = "foobar";
        ((TestClass2) object_1).another = 123;

        TestClass2 clone = ((TestClass2) object_1.clone());
        assert clone != object_1;
        assert clone.value == object_1.value;
        assert clone.another == ((TestClass2) object_1).another;
    }

    @Benchmark
    public int java_clone() throws CloneNotSupportedException {
        return ((TestClass) object_1.clone()).value.length();
    }

    @Benchmark
    public int apache_clone() throws CloneNotSupportedException {
        return ((TestClass) SerializationUtils.clone(object_1)).value.length();
    }

    @Benchmark
    public int cloner() throws CloneNotSupportedException {
        return new com.rits.cloning.Cloner().deepClone(object_1).value.length();
    }

    @Benchmark
    public int simple() throws CloneNotSupportedException {
        return object_1.simpleClone().value.length();
    }

}
