package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by wizzardo on 31.03.15.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
public class SoftReferenceBenchmark {

    Queue<Integer> simpleQueue;
    Queue<SoftReference<Integer>> softQueue;

    @Setup(Level.Iteration)
    public void setup() {
        simpleQueue = new ConcurrentLinkedQueue<>();
        softQueue = new ConcurrentLinkedQueue<>();

        simpleQueue.add(1);
        softQueue.add(new SoftReference<>(1));
    }

    @Benchmark
    public int direct() {
        Integer i = simpleQueue.poll();
        if (i == null)
            i = 1;
        simpleQueue.add(i);
        return i;
    }

    @Benchmark
    public int soft() {
        SoftReference<Integer> ref = softQueue.poll();
        Integer i;
        if (ref == null)
            softQueue.add(new SoftReference<>(i = 1));
        else if ((i = ref.get()) == null)
            softQueue.add(new SoftReference<>(i = 1));
        else
            softQueue.add(ref);

        return i;
    }

    @Benchmark
    public int soft_new() {
        SoftReference<Integer> ref = softQueue.poll();
        Integer i;
        if (ref == null)
            i = 1;
        else if ((i = ref.get()) == null)
            i = 1;

        softQueue.add(new SoftReference<>(i));
        return i;
    }

    private static void fill(List<SoftReference<StringBuilder>> list, int n) {
        Iterator<SoftReference<StringBuilder>> iterator = list.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            SoftReference<StringBuilder> reference = iterator.next();
            if (reference.get() == null){
                iterator.remove();
                i++;
            }
        }

        System.out.println("fill. removed: "+i);

        while (list.size() < n)
            list.add(new SoftReference<>(new StringBuilder(1024 * 1024)));
    }

    public static void main(String[] args) throws InterruptedException {
        List<SoftReference<StringBuilder>> list = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            fill(list, 1500);
            Thread.sleep(5000);
        }
    }
}
