package com.wizzardo.benchmarks;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.json.*;

/**
 * Created by wizzardo on 04.08.15.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)

//@State(Scope.Benchmark)
//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(TimeUnit.NANOSECONDS)
////@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts", "-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder"})
//@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder"})
//@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
//@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class JsonBenchmark {

    public static class TrackingPojo {
        public String event;
        public long brochureClickId;
        public int page;
        public PageViewMode pageViewMode;
        //        public String pageViewMode;
        public Date date;
//        public String date;
    }

    public enum PageViewMode {
        SINGLE_PAGE_MODE,
        DOUBLE_PAGE_MODE
    }


    String[] data;
    int i;
    ObjectMapper jacksonMapper;
    private TypeReference<?> jacksonType;

    @Setup(Level.Iteration)
    public void setup() throws IOException {
        data = FileTools.text("src/jmh/resources/test.json").split("\n");
        i = 0;

        jacksonMapper = new ObjectMapper();
        jacksonType = new TypeReference<TrackingPojo>() {
        };
    }

    @Benchmark
    public Object jackson_pojo() throws IOException {
        return jacksonMapper.readValue(getJson(), jacksonType);
    }

    @Benchmark
    public Object wizzardo_tools_pojo() throws IOException {
        return JsonTools.parse(getJson(), TrackingPojo.class);
    }

    @Benchmark
    public Object jackson_map() throws IOException {
        return jacksonMapper.readTree(getJson());
    }

    @Benchmark
    public Object wizzardo_tools_map() throws IOException {
        return JsonTools.parse(getJson());
    }

    private String getJson() {
        if (i >= data.length)
            i = 0;
        return data[i++];
    }
}
