package com.wizzardo.benchmarks;


import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by wizzardo on 10.02.15.
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class ArrayBenchmarks {

    public char ch;
    static boolean[] allow;

    static long[] integers;
    int length;

    static {
        allow = new boolean[128];

//        if (ch != '"' && ch != '\'' && ch != '}' && ch != ']' && ch != ',')
        allow['"'] = true;
        allow['\''] = true;
        allow[']'] = true;
        allow['}'] = true;
        allow[','] = true;

        integers = new long[]{1, 10, 100, 1000, 1000_0, 1000_00, 1000_000, 1000_000_0, 1000_000_00, 1000_000_000, 1000_000_000_0l, 1000_000_000_00l, 1000_000_000_000l};
    }

    @Setup(Level.Iteration)
    public void setup() {
//        ch = (char) (new Random().nextInt(128));
//        ch = 'a';
        ch = '"';

        length = 10;
    }


    //    @Benchmark
    public int simple() {
        if (ch != '"' && ch != '\'' && ch != '}' && ch != ']' && ch != ',')
            return 1;
        else
            return 0;
    }

    //    @Benchmark
    public int array() {
        if (ch < 128 && !allow[ch])
            return 1;
        else
            return 0;
    }

    @Benchmark
    public double array2() {
        long number = 0;
        for (int i = 0; i < length; i++) {
            number = number * 10 + i;
        }
        return (double) number / integers[length];
    }

    @Benchmark
    public double simple2() {
        long l = 1;
        long number = 0;
        for (int i = 0; i < length; i++) {
            l *= 10;
            number = number * 10 + i;
        }
        return (double) number / l;
    }

}
