package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.options.IntegerValueConverter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//        Benchmark                                  (type)  Mode  Cnt     Score     Error  Units
//        IterateMapBenchmarks.test_entrySet  LinkedHashMap  avgt    5   365.988 ±  19.590  ns/op
//        IterateMapBenchmarks.test_values    LinkedHashMap  avgt    5   390.728 ±   4.892  ns/op
//        IterateMapBenchmarks.test_keySet    LinkedHashMap  avgt    5  1814.419 ± 179.087  ns/op
//        IterateMapBenchmarks.test_entrySet        HashMap  avgt    5   869.421 ±  60.411  ns/op
//        IterateMapBenchmarks.test_values          HashMap  avgt    5   828.518 ±  51.894  ns/op
//        IterateMapBenchmarks.test_keySet          HashMap  avgt    5  2791.026 ±  65.726  ns/op

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class IterateMapBenchmarks {
    Map<Integer, String> map;

    @Param({"HashMap", "LinkedHashMap"})
    String type;

    @Setup(Level.Iteration)
    public void setup() throws IOException {
        if (type.equals("LinkedHashMap"))
            map = new LinkedHashMap<>();
        if (type.equals("HashMap"))
            map = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            map.put(i, String.valueOf(i));
        }
    }


    @Benchmark
    public int test_keySet() {
        int sum = 0;

        Map<Integer, String> map = this.map;
        for (Integer key : map.keySet())
            sum += map.get(key).length();

        return sum;
    }

    @Benchmark
    public int test_entrySet() {
        int sum = 0;

        for (Map.Entry<Integer, String> entry : map.entrySet())
            sum += entry.getValue().length();

        return sum;
    }

    @Benchmark
    public int test_values() {
        int sum = 0;

        for (String value : map.values())
            sum += value.length();

        return sum;
    }
}
