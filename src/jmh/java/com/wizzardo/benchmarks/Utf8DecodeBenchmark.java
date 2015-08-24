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

        private static boolean isNotContinuation(int var0) {
            return (var0 & 192) != 128;
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

        private static CoderResult malformedN(ByteBuffer bb, int length) {
            switch (length) {
                case 1:
                case 2:
                    return CoderResult.malformedForLength(1);
                case 3:
                    byte var4 = bb.get();
                    byte var5 = bb.get();
                    return CoderResult.malformedForLength((var4 != -32 || (var5 & 224) != 128) && !isNotContinuation(var5) ? 2 : 1);
                case 4:
                    int var2 = bb.get() & 255;
                    int var3 = bb.get() & 255;
                    if (var2 <= 244 && (var2 != 240 || var3 >= 144 && var3 <= 191) && (var2 != 244 || (var3 & 240) == 128) && !isNotContinuation(var3)) {
                        if (isNotContinuation(bb.get())) {
                            return CoderResult.malformedForLength(2);
                        }

                        return CoderResult.malformedForLength(3);
                    }

                    return CoderResult.malformedForLength(1);
                default:
                    assert false;

                    return null;
            }
        }

        private static ByteBuffer getByteBuffer(ByteBuffer var0, byte[] var1, int var2) {
            if (var0 == null) {
                var0 = ByteBuffer.wrap(var1);
            }

            var0.position(var2);
            return var0;
        }

        public int decode(byte[] bytes, int offset, int length, char[] chars) {
            int to = offset + length;
            int i = 0;
            int l = Math.min(length, chars.length);

            ByteBuffer bb = null;
//            for (bb = null; i < l && bytes[offset] >= 0; chars[i++] = (char) bytes[offset++]) {
//                ;
//            }

            int temp;
            while ((temp = bytes[offset++]) >= 0 && i < l) {
                chars[i++] = (char) temp;
            }
            offset--;

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
                                    bb = getByteBuffer(bb, bytes, offset);
                                    offset += malformedN(bb, 3).length();
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
                            byte var12 = bytes[offset++];
                            int var13 = b << 18 ^ b1 << 12 ^ b2 << 6 ^ var12 ^ 3678080;
                            if (!isMalformed4(b1, b2, var12) && Character.isSupplementaryCodePoint(var13)) {
                                chars[i++] = Character.highSurrogate(var13);
                                chars[i++] = Character.lowSurrogate(var13);
                            } else {
                                chars[i++] = malformed;
                                offset -= 4;
                                bb = getByteBuffer(bb, bytes, offset);
                                offset += malformedN(bb, 4).length();
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
                            chars[i++] = (char) (b << 6 ^ b1 ^ 0xf80);
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
