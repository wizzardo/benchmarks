package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;
import sun.nio.cs.ArrayEncoder;
import sun.nio.cs.Surrogate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by wizzardo on 29.03.15.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class Utf8EncodeBenchmark {

    char[] chars;

    @Setup(Level.Iteration)
    public void setup() {
        chars = "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz".toCharArray();
        chars = "some utf-8 string раз два aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbccccccccccccccccccccccccccccccccccccccccc ййййййййййййййййййййййййййййййййййййййййййййййййййййййййййййй jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj".toCharArray();
        chars = "яяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяя".toCharArray();
    }

    @Benchmark
    public int getBytes() {
        return new String(chars).getBytes(StandardCharsets.UTF_8).length;
    }

    @Benchmark
    public int native_encode() {
        char[] chars = this.chars;
        byte[] bytes = new byte[chars.length * 4];
        int l = ((ArrayEncoder) StandardCharsets.UTF_8.newEncoder()).encode(chars, 0, chars.length, bytes);
        bytes = Arrays.copyOf(bytes, l);
        return bytes.length;
    }

    @Benchmark
    public int custom_with_count() {
        char[] chars = this.chars;
        byte[] bytes = new byte[countUtf8Bytes(chars, 0, chars.length)];
        encode(chars, 0, chars.length, bytes);
        return bytes.length;
    }

    @Benchmark
    public int custom_without_count() {
        char[] chars = this.chars;
        byte[] bytes = new byte[chars.length * 4];
        int l = encode(chars, 0, chars.length, bytes);
        bytes = Arrays.copyOf(bytes, l);
        return bytes.length;
    }


    public int countUtf8Bytes(char[] chars, int off, int length) {
        int limit = off + length;
        int l = 0;

        while (l < length && chars[off] < 128) {
            l++;
            off++;
        }

        Surrogate.Parser sgp = null;

        while (off < limit) {
            char ch = chars[off++];
            if (ch < 128) {
                l++;
            } else if (ch < 2048) {
                l += 2;
            } else if (ch >= '\uD800' && ch < '\uE000') {//isSurrogate
                if (sgp == null) {
                    sgp = new Surrogate.Parser();
                }

                int r = sgp.parse(ch, chars, off - 1, limit);
                if (r < 0) {
                    l++;
                } else {
                    l += 4;
                    ++off;
                }
            } else {
                l += 3;
            }
        }

        return l;
    }

    public int encode(char[] chars, int off, int length, byte[] bytes) {
        int limit = off + length;
        int l = 0;

        int ch;
        int i = l + Math.min(length, bytes.length);
        while (l < i && (ch = chars[off++]) < 128) {
            bytes[l++] = (byte) ch;
        }
        if (l == i)
            return l;

        Surrogate.Parser sgp = null;
        off--;
        while (off < limit) {
            int c = chars[off++];
            if (c < 128) {
                bytes[l++] = (byte) c;
            } else if (c < 2048) {
                bytes[l++] = (byte) (192 | c >> 6);
                bytes[l++] = (byte) (128 | c & 63);
            } else if (c >= '\uD800' && c < '\uE000') {//isSurrogate
                if (sgp == null) {
                    sgp = new Surrogate.Parser();
                }

                int r = sgp.parse((char) c, chars, off - 1, limit);
                if (r < 0) {
                    bytes[l++] = '?';
                } else {
                    bytes[l++] = (byte) (240 | r >> 18);
                    bytes[l++] = (byte) (128 | r >> 12 & 63);
                    bytes[l++] = (byte) (128 | r >> 6 & 63);
                    bytes[l++] = (byte) (128 | r & 63);
                    ++off;
                }
            } else {
                bytes[l++] = (byte) (224 | c >> 12);
                bytes[l++] = (byte) (128 | c >> 6 & 63);
                bytes[l++] = (byte) (128 | c & 63);
            }
        }

        return l;
    }
}
