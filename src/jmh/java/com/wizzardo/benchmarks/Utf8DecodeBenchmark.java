package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;
import sun.nio.cs.ArrayDecoder;
import sun.nio.cs.Surrogate;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)
public class Utf8DecodeBenchmark {

    byte[] bytes;
    char[] chars;
    Charset utf8 = Charset.forName("UTF-8");

    @Setup(Level.Iteration)
    public void setup() throws UnsupportedEncodingException {
        String s = "some utf-8 string раз два aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbccccccccccccccccccccccccccccccccccccccccc ййййййййййййййййййййййййййййййййййййййййййййййййййййййййййййй jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj";
        s = "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz";
        s = "яяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяя";
        s = "€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€";

        bytes = s.getBytes("utf-8");
        chars = new char[s.length()];

        Decoder decoder = new Decoder();
        decoder.decode(bytes, 0, bytes.length, chars);
        assert s.endsWith(new String(chars));
    }

    @Benchmark
    public int custom_decode() {
        Decoder decoder = new Decoder();
        return decoder.decode(bytes, 0, bytes.length, chars);
    }

    @Benchmark
    public int native_decode() {
        ArrayDecoder decoder = (ArrayDecoder) utf8.newDecoder();
        return decoder.decode(bytes, 0, bytes.length, chars);
    }

    private static class Decoder implements ArrayDecoder {

        private final static char malformed = '�';

        private static boolean isNotContinuation(int ch) {
            return (ch & 0b11000000) != 0b10000000;
        }

        private static boolean isMalformed3(int var0, int var1, int var2) {
            return var0 == -32 && (var1 & 224) == 128 || (var1 & 192) != 128 || (var2 & 192) != 128;
        }

        private static boolean isMalformed3_2(int var0, int var1) {
            return var0 == -32 && (var1 & 224) == 128 || (var1 & 192) != 128;
        }

        private static boolean isMalformed4(int var0, int var1, int var2) {
            return (var0 & 192) != 128 || (var1 & 192) != 128 || (var2 & 192) != 128;
        }

        private static boolean isMalformed4_2(int var0, int var1) {
            return var0 == 240 && (var1 < 144 || var1 > 191) || var0 == 244 && (var1 & 240) != 128 || (var1 & 192) != 128;
        }

        private static boolean isMalformed4_3(int var0) {
            return (var0 & 192) != 128;
        }

        private static int malformedN(byte[] bytes, int offset, int length) {
            switch (length) {
                case 1:
                case 2:
                    return 1;
                case 3:
                    byte b1 = bytes[offset];
                    byte b2 = bytes[offset + 1];
                    return (b1 != -32 || (b2 & 224) != 128) && !isNotContinuation(b2) ? 2 : 1;
                case 4:
                    int i1 = bytes[offset] & 255;
                    int i2 = bytes[offset + 1] & 255;
                    if (i1 <= 244 && (i1 != 240 || i2 >= 144 && i2 <= 191) && (i1 != 244 || (i2 & 240) == 128) && !isNotContinuation(i2)) {
                        if (isNotContinuation(bytes[offset + 2]))
                            return 2;

                        return 3;
                    }

                    return 1;
                default:
                    assert false;
                    return -1;
            }
        }

        private static ByteBuffer getByteBuffer(ByteBuffer bb, byte[] bytes, int offset) {
            if (bb == null) {
                bb = ByteBuffer.wrap(bytes);
            }

            bb.position(offset);
            return bb;
        }

        public int decode(byte[] bytes, int offset, int length, char[] chars) {
            int to = offset + length;
            int i = 0;
            int l = Math.min(length, chars.length);

            int temp;
            while (i < l) {
                if ((temp = bytes[offset++]) >= 0)
                    chars[i++] = (char) temp;
                else {
                    offset--;
                    break;
                }
            }

            while (offset < to) {
                byte b = bytes[offset++];
                if (b < 0) {
                    byte b1;
                    if (b >> 5 != -2 || (b & 0x1e) == 0) {
                        byte b2;
                        if (b >> 4 == -2) {
                            if (offset + 1 < to) {
                                b1 = bytes[offset++];
                                b2 = bytes[offset++];
                                if (isMalformed3(b, b1, b2)) {
                                    chars[i++] = malformed;
                                    offset -= 3;
                                    offset += malformedN(bytes, offset, 3);
                                } else {
                                    char ch = (char) (b << 12 ^ b1 << 6 ^ b2 ^ -123008);
                                    if (Character.isSurrogate(ch)) {
                                        chars[i++] = malformed;
                                    } else {
                                        chars[i++] = ch;
                                    }
                                }
                            } else {
                                if (offset >= to || !isMalformed3_2(b, bytes[offset])) {
                                    chars[i++] = malformed;
                                    return i;
                                }

                                chars[i++] = malformed;
                            }
                        } else if (b >> 3 != -2) {
                            chars[i++] = malformed;
                        } else if (offset + 2 < to) {
                            b1 = bytes[offset++];
                            b2 = bytes[offset++];
                            byte b3 = bytes[offset++];
                            int value = b << 18 ^ b1 << 12 ^ b2 << 6 ^ b3 ^ 3678080;
                            if (!isMalformed4(b1, b2, b3) && Character.isSupplementaryCodePoint(value)) {
                                chars[i++] = Character.highSurrogate(value);
                                chars[i++] = Character.lowSurrogate(value);
                            } else {
                                chars[i++] = malformed;
                                offset -= 4;
                                offset += malformedN(bytes, offset, 4);
                            }
                        } else {
                            int var14 = b & 0xff;
                            if (var14 <= 244 && (offset >= to || !isMalformed4_2(var14, bytes[offset] & 0xff))) {
                                ++offset;
                                if (offset >= to || !isMalformed4_3(bytes[offset])) {
                                    chars[i++] = malformed;
                                    return i;
                                }

                                chars[i++] = malformed;
                            } else {
                                chars[i++] = malformed;
                            }
                        }
                    } else {
                        if (offset >= to) {
                            chars[i++] = malformed;
                            return i;
                        }

                        b1 = bytes[offset++];
                        if (isNotContinuation(b1)) {
                            chars[i++] = malformed;
                            --offset;
                        } else {
                            chars[i++] = (char) (b << 6 ^ b1 ^ 0b1111_1000_0000);
                        }
                    }
                } else {
                    chars[i++] = (char) b;
                }
            }

            return i;
        }
    }
}
