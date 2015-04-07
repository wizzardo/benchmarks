package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

//import sun.misc.FloatingDecimal;
//
//import java.io.IOException;

/**
 * @author: wizzardo
 * Date: 8/31/14
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
public class CharSequenceBenchmarks {

    CharSequence cs;
    String s;
    StringBuilder stringBuilder;

    @Setup(Level.Iteration)
    public void setup() {
        stringBuilder = new StringBuilder();

        s = "foobar";
        s = "foobar looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong";
        cs = " " + s;
    }

    @Benchmark
    public int char_sequence() {
        stringBuilder.setLength(0);
        return stringBuilder.append(cs, 1, cs.length()).length();
    }


    @Benchmark
    public int string() {
        stringBuilder.setLength(0);
        return stringBuilder.append(s).length();
    }


}
