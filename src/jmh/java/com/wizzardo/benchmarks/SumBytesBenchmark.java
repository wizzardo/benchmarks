package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by wizzardo on 21.03.15.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class SumBytesBenchmark {
    byte[] bytes;
    int[] ints;

    @Setup(Level.Iteration)
    public void setup() {
        bytes = "/some_path/foo/bar".getBytes();
        ints = new int[bytes.length];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = bytes[i];
        }
    }

    @Benchmark
    public int sum_bytes() {
        int sum = 0;
        for (byte b : bytes) {
            sum += b;
        }
        return sum;
    }

    @Benchmark
    public int sum_bytes2() {
        int sum = 0;
        for (byte b : bytes) {
            sum += b & 0xff;
        }
        return sum;
    }

    @Benchmark
    public int sum_ints() {
        int sum = 0;
        for (int b : ints) {
            sum += b;
        }
        return sum;
    }

}
