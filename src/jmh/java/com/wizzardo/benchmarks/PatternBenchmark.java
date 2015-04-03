package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wizzardo on 26.03.15.
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class PatternBenchmark {

    Pattern pattern;
    String test;

    @Setup(Level.Iteration)
    public void setup() {
        pattern = Pattern.compile("(.+)");

        test = "benchmark";
    }

    @Benchmark
    public int pattern() {
        Matcher matcher = pattern.matcher(test);
        matcher.find();
        return matcher.group().length();
    }

    @Benchmark
    public int base() {
        return test.length();
    }
}
