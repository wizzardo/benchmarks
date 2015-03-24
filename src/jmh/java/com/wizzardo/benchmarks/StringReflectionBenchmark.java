package com.wizzardo.benchmarks;

import com.wizzardo.tools.reflection.StringReflection;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by wizzardo on 24.03.15.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class StringReflectionBenchmark {

    String value;

    @Setup(Level.Iteration)
    public void setup() {
        value = "some value foo bar";
    }

    @Benchmark
    public int toCharArray() {
        return value.toCharArray().length;
    }

    @Benchmark
    public int stringReflection() {
        return StringReflection.chars(value).length;
    }

    @Benchmark
    public int base() {
        return value.length();
    }
}
