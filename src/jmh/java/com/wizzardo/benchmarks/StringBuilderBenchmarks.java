package com.wizzardo.benchmarks;

import com.wizzardo.tools.misc.ExceptionDrivenStringBuilder;
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
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class StringBuilderBenchmarks {

    int[] ints;
    String[] strings;
    int k = 1000;

    StringBuilder stringBuilder;
    ExceptionDrivenStringBuilder exceptionDrivenStringBuilder;

    @Setup(Level.Iteration)
    public void setup() {
        ints = new int[100];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = i;
        }
        strings = new String[100];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = String.valueOf(i);
        }

        stringBuilder = new StringBuilder();
        exceptionDrivenStringBuilder = new ExceptionDrivenStringBuilder();
    }

    @Benchmark
    public int stringBuilder_int() {
        for (int i = 0; i < k; i++) {
            stringBuilder.setLength(0);
            for (int value : ints) {
                stringBuilder.append(value);
            }
        }
        return stringBuilder.length();
    }

    @Benchmark
    public int exceptionDrivenStringBuilder_int() {
        for (int i = 0; i < k; i++) {
            exceptionDrivenStringBuilder.setLength(0);
            for (int value : ints) {
                exceptionDrivenStringBuilder.append(value);
            }
        }
        return exceptionDrivenStringBuilder.hashCode();
    }

    @Benchmark
    public int stringBuilder_strings() {
        for (int i = 0; i < k; i++) {
            stringBuilder.setLength(0);
            for (String value : strings) {
                stringBuilder.append(value);
            }
        }
        return stringBuilder.length();
    }

    @Benchmark
    public int exceptionDrivenStringBuilder_strings() {
        for (int i = 0; i < k; i++) {
            exceptionDrivenStringBuilder.setLength(0);
            for (String value : strings) {
                exceptionDrivenStringBuilder.append(value);
            }
        }
        return exceptionDrivenStringBuilder.hashCode();
    }


}
