package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

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
public class StringBufferBenchmarks {

    String value;
    int k;
    StringBuffer buffer;
    StringBuilder builder;

    @Setup(Level.Iteration)
    public void setup() {
        value = "1234567890";
        k = 100000;

        buffer = new StringBuffer(value.length() * k);
        builder = new StringBuilder(value.length() * k);
    }

    @Benchmark
    public int string_builder() {
        StringBuilder builder = this.builder;
        builder.setLength(0);
        for (int i = 0; i < k; i++) {
            builder.append(value);
        }
        return builder.length();
    }

    @Benchmark
    public int string_buffer() {
        StringBuffer buffer = this.buffer;
        buffer.setLength(0);
        for (int i = 0; i < k; i++) {
            buffer.append(value);
        }
        return buffer.length();
    }

    @Benchmark
    public int new_string_builder() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < k; i++) {
            builder.append(value);
        }
        return builder.length();
    }

    @Benchmark
    public int new_string_buffer() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < k; i++) {
            buffer.append(value);
        }
        return buffer.length();
    }

}
