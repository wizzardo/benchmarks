package com.wizzardo.benchmarks;

import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.misc.pool.*;
import com.wizzardo.tools.misc.*;
import org.openjdk.jmh.annotations.*;


import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created by wizzardo on 18.06.15.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts", "-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder"})
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class PoolBenchmark {
    Pool<StringBuilder> shared = new SharedPool<StringBuilder>() {
        @Override
        public StringBuilder create() {
            return new StringBuilder();
        }
    };

    Pool<StringBuilder> local = new ThreadLocalPool<StringBuilder>() {
        @Override
        public StringBuilder create() {
            return new StringBuilder();
        }
    };

    Pool<StringBuilder> localSoft = new ThreadLocalPool<StringBuilder>() {
        @Override
        public StringBuilder create() {
            return new StringBuilder();
        }

        @Override
        protected Holder<StringBuilder> createHolder(StringBuilder sb) {
            return new SoftHolder<StringBuilder>(this, sb) {
                @Override
                public StringBuilder get() {
                    return reset(super.get());
                }
            };
        }

        StringBuilder reset(StringBuilder sb) {
            sb.setLength(0);
            return sb;
        }
    };

    Pool<StringBuilder> defaultPool = new PoolBuilder<StringBuilder>()
            .supplier(StringBuilder::new)
            .resetter(builder -> builder.setLength(0))
            .build();

    Pool<StringBuilder> customPool = new PoolBuilder<StringBuilder>()
            .supplier(StringBuilder::new)
            .resetter(sb -> sb.setLength(0))
            .queue(new Supplier<Queue<Holder<StringBuilder>>>() {
                ThreadLocal<Queue<Holder<StringBuilder>>> queue = new ThreadLocal<Queue<Holder<StringBuilder>>>() {
                    @Override
                    protected Queue<Holder<StringBuilder>> initialValue() {
                        return new LinkedList<>();
                    }
                };

                @Override
                public Queue<Holder<StringBuilder>> supply() {
                    return queue.get();
                }
            })
            .holder((pool, integer, resetter) -> new SoftHolder<StringBuilder>(pool, integer) {
                @Override
                public StringBuilder get() {
                    StringBuilder t = super.get();
                    resetter.consume(t);
                    return t;
                }
            })
            .build();

    @Benchmark
    public String shared() {
        try (Holder<StringBuilder> holder = shared.holder()) {
            return resetAndConsume(holder.get());
        }
    }

    @Benchmark
    public String local() {
        try (Holder<StringBuilder> holder = local.holder()) {
            return resetAndConsume(holder.get());
        }
    }

    @Benchmark
    public String local_soft() {
        try (Holder<StringBuilder> holder = localSoft.holder()) {
            return consume(holder.get());
        }
    }

    @Benchmark
    public String local_lambda() {
        return Unchecked.call(local.holder(), holder -> resetAndConsume(holder.get()));
    }

    @Benchmark
    public String builder_custom() {
        return Unchecked.call(customPool.holder(), holder -> consume(holder.get()));
    }

    @Benchmark
    public String builder_custom_provide() {
        return customPool.provide(this::consume);
    }

    @Benchmark
    public String builder_default() {
        return Unchecked.call(defaultPool.holder(), holder -> consume(holder.get()));
    }

    @Benchmark
    public String builder_default_provide() {
        return defaultPool.provide(this::consume);
    }

    String consume(StringBuilder sb) {
        sb.append('!');
        return sb.toString();
    }

    String resetAndConsume(StringBuilder sb) {
        sb.setLength(0);
        return consume(sb);
    }
}
