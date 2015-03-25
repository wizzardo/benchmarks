package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author: wizzardo
 * Date: 8/31/14
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class MultiplyBenchmarks {

    int value;
    int k;

    @Setup(Level.Iteration)
    public void setup() {
        value = new Random().nextInt(10000);
        k = 1000;
    }

    @Benchmark
    public int simple() {
        int v = value;
        for (int i = 0; i < k; i++) {
            v = v * 100;
            v = v / 100;
        }
        return v;
    }

    @Benchmark
    public int sum() {
        int v = value;
        for (int i = 0; i < k; i++) {
            v = (v << 6) + (v << 5) + (v << 2);
//            v = div10(div10(v));
            v = v / 100;
        }
        return v;
    }

    //    @Benchmark
    public int noop() {
        return value;
    }

    public static int div10(int n) {
        n = (n >> 1) + (n >> 2);
        n += n < 0 ? 9 : 2;
        n = n + (n >> 4);
        n = n + (n >> 8);
        n = n + (n >> 16);
        n = n >> 3;
        return n;
    }

    public static void main(String[] args) {
        int k = 12345;
        System.out.println(k / 10);
        System.out.println(div10(k));
    }
}
