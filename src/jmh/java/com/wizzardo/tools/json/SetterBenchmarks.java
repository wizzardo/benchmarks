package com.wizzardo.tools.json;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
public class SetterBenchmarks {

    JsonBinder binder;
    JsonFieldSetter fieldSetter;
    String value;

    static class Request {
        String method;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }
    }

    @Setup(Level.Iteration)
    public void setup() throws IOException {
        binder = Binder.getObjectBinder(new Generic(Request.class));
        binder.setTemporaryKey("method");
        fieldSetter = binder.getFieldSetter();
        value = "GET";
    }

    @Benchmark
    public int old_set() {
        Request request = (Request) binder.getObject();
        fieldSetter.setObject(request, new JsonItem(value).getAs(String.class));
        return request.getMethod().length();
    }

    @Benchmark
    public int new_set() {
        Request request = (Request) binder.getObject();
        fieldSetter.setString(request, value);
        return request.getMethod().length();
    }

    @Benchmark
    public int native_set() {
        Request request = (Request) binder.getObject();
        request.setMethod(value);
        return request.getMethod().length();
    }
}
