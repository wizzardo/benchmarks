package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by wizzardo on 02.03.15.
 */


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class ForWhileBenchmark {

    char[] data;

    @Setup(Level.Iteration)
    public void setup() throws IOException {
        data = "method_long_key_ololololololololololololololo\":".toCharArray();
    }

    @Benchmark
    public int test_1() {
        char quote = '"';
        int i = 0;
        char[] s = data;
        int to = s.length;
        boolean escape = false;
        char ch;
        for (; i < to; i++) {
            if (escape) {
                escape = false;
            } else {
                ch = s[i];
                if (ch == quote)
                    break;

                if (ch == '\\')
                    escape = true;
            }
        }
        return i;
    }

    @Benchmark
    public int test_2() {
        char quote = '"';
        int i = 0;
        char[] s = data;
        int to = s.length;
        int escape = -2;
        char ch;
        for (; i < to; i++) {
            ch = s[i];
            if (ch == quote)
                if (escape != -2 && escape != i - 1)
                    break;

            if (ch == '\\' && escape != i - 1)
                escape = i;
        }
        return i;
    }

    @Benchmark
    public int test_3() {
        char quote = '"';
        int i = 0;
        char[] s = data;
        int to = s.length;
        char ch;
        int escape = -2;
        while (i < to) {
            ch = s[i];
            if (ch == quote)
                if (escape != -2 && escape != i - 1)
                    break;

            if (ch == '\\' && escape != i - 1)
                escape = i;
            i++;
        }
        return i;
    }

    @Benchmark
    public int test_4() {
        char quote = '"';
        char[] s = data;
        int to = s.length;
        char ch = 0;
        int escape = -3;
        int k = -1;
        for (int i = 0; i < to - 1; i += 2) {
            ch = s[i];
            if (ch == quote)
                if (escape != -3 && escape != i - 1) {
                    k = i;
                    break;
                }

            if (ch == '\\' && escape != i - 1)
                escape = i;
            ch = s[i + 1];
            if (ch == quote)
                if (escape != -3 && escape != i - 2) {
                    k = i;
                    break;
                }

            if (ch == '\\' && escape != i - 2)
                escape = i;
        }
        if (k < to && ch != quote) {
            ch = s[to - 1];
            if (ch != quote)
                throw new IllegalStateException("here must be \"");
        }
        return k;
    }
}
