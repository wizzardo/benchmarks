package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;
import sun.nio.cs.ArrayEncoder;
import sun.nio.cs.Surrogate;

import java.nio.charset.CoderResult;
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

    //    @Param({"1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096"})
//    @Param({"4096", "8192", "16384", "32768"})
    @Param({"32768", "65536", "131072", "262144"})
    String size;

    char[] chars;

    @Setup(Level.Iteration)
    public void setup() {
        chars = "some utf-8 string раз два aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbccccccccccccccccccccccccccccccccccccccccc ййййййййййййййййййййййййййййййййййййййййййййййййййййййййййййй jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj".toCharArray();
        chars = "яяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяя".toCharArray();
        chars = "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz".toCharArray();

        StringBuilder sb = new StringBuilder();
        int l = Integer.parseInt(size);
        for (int i = 0; i < l; i++) {
//            sb.append('z');
            sb.append('я');
        }
        chars = sb.toString().toCharArray();
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

        while (l < length) {
            if (chars[off++] < 128)
                l++;
            else {
                off--;
                break;
            }
        }

        while (off < limit) {
            int ch = chars[off++];
            if (ch < 128) {
                l++;
            } else if (ch < 2048) {
                l += 2;
            } else if (ch >= '\uD800' && ch < '\uE000') {//isSurrogate
                int r = parseSurrogate((char) ch, chars, off, limit);
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
        while (l < i) {
            if ((ch = chars[off++]) < 128)
                bytes[l++] = (byte) ch;
            else {
                off--;
                break;
            }
        }

        while (off < limit) {
            int c = chars[off++];
            if (c < 128) {
                bytes[l++] = (byte) c;
            } else if (c < 2048) {
                bytes[l++] = (byte) (192 | c >> 6);
                bytes[l++] = (byte) (128 | c & 63);
            } else if (c >= '\uD800' && c < '\uE000') {//isSurrogate
                int r = parseSurrogate((char) c, chars, off, limit);
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

    public int parseSurrogate(char ch, char[] chars, int offset, int limit) {
        if (Character.isHighSurrogate(ch)) {
            if (limit - offset < 1) {
                return -1;
            } else {
                char ch2 = chars[offset];
                if (Character.isLowSurrogate(ch2)) {
                    return Character.toCodePoint(ch, ch2);
                } else {
                    return -1;
                }
            }
        } else if (Character.isLowSurrogate(ch)) {
            return -1;
        } else {
            return ch;
        }
    }
}
