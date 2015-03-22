package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static com.wizzardo.benchmarks.UrlMappingBenchmark.*;

/**
 * Created by wizzardo on 21.03.15.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class PathBenchmark {

    byte[] bytes;
    byte[] bytes2;
    byte[] bytes3;

    @Setup(Level.Iteration)
    public void setup() {
        bytes = "/some_path/foo/bar".getBytes();
        bytes2 = "/some_pat!/fo!/ba!".getBytes();
        bytes3 = "/!ome_path/!oo/!ar".getBytes();

        byteTree = new ByteTree();
        byteTree.append("some_path");
        byteTree.append("foo");
        byteTree.append("bar");
    }

    @Benchmark
    public int string() {
        return new String(bytes).hashCode();
    }

    @Benchmark
    public int stringSplit() {
        return new String(bytes).split("/").length;
    }

    @Benchmark
    public int stringAscii() {
        return UrlMappingBenchmark.AsciiReader.read(bytes).hashCode();
    }

    @Benchmark
    public int path() {
        return readPath(bytes).size();
    }

    @Benchmark
    public int path2() {
        return readPath2(bytes).size();
    }

    @Benchmark
    public int path3() {
        return readPath3(bytes).size();
    }

    @Benchmark
    public int path3_not_prepared() {
        return readPath3(bytes2).size();
    }

    @Benchmark
    public int path3_not_prepared_2() {
        return readPath3(bytes3).size();
    }
}
