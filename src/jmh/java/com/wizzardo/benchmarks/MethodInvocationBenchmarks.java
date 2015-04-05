package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author: wizzardo
 * Date: 8/31/14
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 15, time = 2, timeUnit = TimeUnit.SECONDS)
public class MethodInvocationBenchmarks {

    static class A {
        static int add(int i) {
            return i + 1;
        }

        static long add(long i) {
            return i + 1;
        }
    }

    static class B {
        static int addInt(int i) {
            return i + 1;
        }

        static long addLong(long i) {
            return i + 1;
        }

        static void toChars(int i, char[] chars) {
            chars[0] = (char) ('0' + i);
        }

    }

    static class Wrapper {
        Method method;

        Wrapper() {
            try {
                method = B.class.getDeclaredMethod("toChars", int.class, char[].class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        void toChars(int i, char[] chars) {
            try {
                method.invoke(null, i, chars);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }


    int k = 10000;
    char[] chars = new char[10];
    Wrapper wrapper = new Wrapper();

    //    @Benchmark
    public int testA() {
        int sum = 0;
        for (int i = 0; i < k; i++) {
            sum += A.add(i);
        }
        return sum;
    }

    //    @Benchmark
    public int testB() {
        int sum = 0;
        for (int i = 0; i < k; i++) {
            sum += B.addInt(i);
        }
        return sum;
    }

    @Benchmark
    public char[] testDirect() {
        for (int i = 0; i < k; i++) {
            B.toChars(i % 10, chars);
        }
        return chars;
    }

    @Benchmark
    public char[] testReflection() {
        for (int i = 0; i < k; i++) {
            wrapper.toChars(i % 10, chars);
        }
        return chars;
    }

    public static void main(String[] args) {
        Wrapper wrapper = new Wrapper();
        char[] chars = new char[1];
        wrapper.toChars(1, chars);
        System.out.println(new String(chars));
    }
}
