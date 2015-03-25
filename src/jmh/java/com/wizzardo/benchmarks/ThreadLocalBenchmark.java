package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * @author: wizzardo
 * Date: 25.11.14
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class ThreadLocalBenchmark {

    Thread local;

    static ThreadLocal<MyInterface> myInterfaceThreadLocal = new ThreadLocal<MyInterface>() {
        @Override
        protected MyInterface initialValue() {
            return new MyInterface() {
                @Override
                public int get() {
                    return 42;
                }
            };
        }
    };

    @Setup(Level.Iteration)
    public void setup() {
        local = new MyThread();
    }

    static class MyThread extends Thread implements MyInterface {
        @Override
        public int get() {
            return 42;
        }
    }

    static interface MyInterface {
        int get();
    }

    @Benchmark
    public int nativeCall() {
        return Thread.currentThread().getPriority();
    }

    @Benchmark
    public int localCallAndCast() {
        return ((MyInterface) local).get();
    }

    @Benchmark
    public int localCall() {
        return local.getPriority();
    }

    @Benchmark
    public int basic() {
        return 42;
    }

    @Benchmark
    public int threadLocal() {
        return myInterfaceThreadLocal.get().get();
    }
}