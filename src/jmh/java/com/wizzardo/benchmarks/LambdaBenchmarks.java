package com.wizzardo.benchmarks;


import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

/**
 * Created by wizzardo on 10.02.15.
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts", "-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder"})
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class LambdaBenchmarks {
    int[] array;

    @Setup(Level.Iteration)
    public void setup() {
        array = new int[]{1, 2, 3};
    }

    interface Args {
        int get(int i);

        static Args create(int... args) {
            return i -> LambdaBenchmarks.get(i, args);
        }

        static Args create2(int... args) {
            return i -> createGetter(i, args).get();
        }
    }

    interface Getter {
        int get();
    }

    static Getter createGetter(int i, int... args) {
        if (i >= args.length)
            return () -> -1;
        else
            return () -> args[i];
    }

    static int get(int i, int... args) {
        return args.length <= i ? -1 : args[i];
    }

    @Benchmark
    public int args_empty() {
        return Args.create().get(0);
    }

    @Benchmark
    public int args_1() {
        return Args.create(1).get(0);
    }

    @Benchmark
    public int args_2() {
        return Args.create(0, 1, 2).get(0);
    }

    @Benchmark
    public int args_3() {
        return Args.create(array).get(0);
    }

    @Benchmark
    public int args_2_empty() {
        return Args.create2().get(0);
    }

    @Benchmark
    public int args_2_1() {
        return Args.create2(1).get(0);
    }

    @Benchmark
    public int args_2_2() {
        return Args.create2(0, 1, 2).get(0);
    }

    @Benchmark
    public int args_2_3() {
        return Args.create2(array).get(0);
    }

    @Benchmark
    public int array_empty() {
        return get(0);
    }

    @Benchmark
    public int array_1() {
        return get(0, 1);
    }

    @Benchmark
    public int array_2() {
        return get(0, 1, 2);
    }

    @Benchmark
    public int array_3() {
        return get(0, array);
    }

}
