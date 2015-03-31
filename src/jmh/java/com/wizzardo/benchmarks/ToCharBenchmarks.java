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
public class ToCharBenchmarks {

    char[] chars = new char[10];
    int value;
    Random random = new Random();
    int k = 1000;

    @Setup(Level.Iteration)
    public void setup() {
        value = random.nextInt(24);
    }

    @Benchmark
    public char[] toChar1() {
        for (int i = 0; i < k; i++) {
            chars[1] = (char) ('0' + mod10Native(value));
            value /= 10;
            if (value > 0)
                chars[0] = (char) ('0' + mod10Native(value));
            else
                chars[0] = '0';

        }
        return chars;
    }

    @Benchmark
    public char[] toChar2() {
        for (int i = 0; i < k; i++) {
            chars[1] = (char) ('0' + mod10(value));
            value /= 10;
            if (value > 0)
                chars[0] = (char) ('0' + mod10(value));
            else
                chars[0] = '0';
        }
        return chars;
    }

    @Benchmark
    public char[] toChar3() {
        for (int i = 0; i < k; i++) {
            chars[1] = (char) ('0' + mod10_(value));
            value /= 10;
            if (value > 0)
                chars[0] = (char) ('0' + mod10_(value));
            else
                chars[0] = '0';
        }
        return chars;
    }

    private static int mod10Native(int v) {
        return v % 10;
    }

    private static int mod10(int v) {
        int k = v / 10;
        return v - (k << 3) - (k << 1);
    }

    private static int mod10_(int v) {
        int k = v / 10;
        return v - k * 10;
    }

    public static void main(String[] args) {
        System.out.println(1 << 3);
        System.out.println(2 << 3);
        System.out.println(3 << 3);
        System.out.println();
        System.out.println(mod10(13));
        System.out.println(mod10(3));
        System.out.println(mod10(22));
        System.out.println(mod10(125));
    }
}
