package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
public class MapBenchmarks {
    Map<Class, Integer> map;

    Class[] classes;

    @Setup(Level.Iteration)
    public void setup() throws IOException {
        map = new HashMap<>();

        classes = new Class[]{Integer.class, Long.class, Short.class, Byte.class, Float.class, Double.class, Boolean.class, Character.class, String.class};
        for (int i = 0; i < classes.length; i++) {
            map.put(classes[i], i + 1);
        }
    }


    @Benchmark
    public int test_map() {
        int sum = 0;

        for (Class aClass : classes)
            sum += getFromMap(aClass);

        return sum;
    }

    @Benchmark
    public int test_if() {
        int sum = 0;

        for (Class aClass : classes)
            sum += getFromIf(aClass);

        return sum;
    }


    public Integer getFromMap(Class aClass) {
        return map.get(aClass);
    }

    public Integer getFromIf(Class aClass) {
        if (aClass == Integer.class)
            return 1;
        if (aClass == Long.class)
            return 2;
        if (aClass == Short.class)
            return 3;
        if (aClass == Byte.class)
            return 4;
        if (aClass == Float.class)
            return 5;
        if (aClass == Double.class)
            return 6;
        if (aClass == Boolean.class)
            return 7;
        if (aClass == Character.class)
            return 8;
        if (aClass == String.class)
            return 9;
        return 0;
    }
}
