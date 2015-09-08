package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;
import sun.nio.cs.ArrayDecoder;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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

    @Param({"1", "2", "3"})
    String bytesPerChar;

    @Setup(Level.Iteration)
    public void setup() throws UnsupportedEncodingException {
        String s = "";
        int n = 300;
        if ("1".equals(bytesPerChar))
            s = makeString('z', 1, n);

        if ("2".equals(bytesPerChar))
            s = makeString('¢', 2, n);

        if ("3".equals(bytesPerChar))
            s = makeString('€', 3, n);

        bytes = s.getBytes("utf-8");
        assert bytes.length == n;
        chars = new char[s.length()];

        Decoder decoder = new Decoder();
        decoder.decode(bytes, 0, bytes.length, chars);
        assert s.equals(new String(chars));
    }

    private String makeString(char ch, int bytesPerChar, int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i += bytesPerChar) {
            builder.append(ch);
        }
        return builder.toString();
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

        private static boolean isNotContinuation(int b) {
            return (b & 0b11000000) != 0b10000000;
        }

        private static boolean isMalformed3(int b1, int b2, int b3) {
            return b1 == -32 && (b2 & 224) == 128 || (b2 & 192) != 128 || (b3 & 192) != 128;
        }

        private static boolean isMalformed3_2(int b1, int b2) {
            return b1 == -32 && (b2 & 224) == 128 || (b2 & 192) != 128;
        }

        private static boolean isMalformed4(int b1, int b2, int b3) {
            return (b1 & 192) != 128 || (b2 & 192) != 128 || (b3 & 192) != 128;
        }

        private static boolean isMalformed4_2(int b1, int b2) {
            return b1 == 240 && (b2 < 144 || b2 > 191) || b1 == 244 && (b2 & 240) != 128 || (b2 & 192) != 128;
        }

        private static boolean isMalformed4_3(int b1) {
            return (b1 & 192) != 128;
        }

        public static boolean isSurrogate(char ch) {
            return ch >= '\uD800' && ch < '\uE000';
        }

        public static boolean isSupplementaryCodePoint(int codePoint) {
            return codePoint >= 0x010000 && codePoint < 0x110000;
        }

        public static char highSurrogate(int codePoint) {
            return (char) ((codePoint >>> 10) + 55232);
        }

        public static char lowSurrogate(int codePoint) {
            return (char) ((codePoint & 0x3ff) + '\uDC00');
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
                                } else {
                                    char ch = (char) (b << 12 ^ b1 << 6 ^ b2 ^ -123008);
                                    if (isSurrogate(ch)) {
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
                            if (!isMalformed4(b1, b2, b3) && isSupplementaryCodePoint(value)) {
                                chars[i++] = highSurrogate(value);
                                chars[i++] = lowSurrogate(value);
                            } else {
                                chars[i++] = malformed;
                            }
                        } else {
                            int i1 = b & 0xff;
                            if (i1 <= 244 && (offset >= to || !isMalformed4_2(i1, bytes[offset] & 0xff))) {
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
