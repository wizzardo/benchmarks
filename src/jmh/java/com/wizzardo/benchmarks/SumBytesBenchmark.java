package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;

import java.util.HashMap;

/**
 * Created by wizzardo on 21.03.15.
 */
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
