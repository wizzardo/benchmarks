package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * @author: wizzardo
 * Date: 8/31/14
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class LongToStringBenchmarks {

    long value;
    int valueInt;
    char[] chars = new char[22];
    int k = 1000;

    @Setup(Level.Iteration)
    public void setup() {
        value = 0;
        value = 1234;
        value = 1234567;
        value = 1234567890123l;
        valueInt = 0;
        valueInt = 1234;
//        valueInt = 1234567;
//        valueInt = 1234567890;
    }

    @Benchmark
    public char[] java() {
        for (int i = 0; i < k; i++) {
            JavaLangHelper.getChars(value++, JavaLangHelper.stringSizeOfLong(value), chars);
        }
        return chars;
    }

    @Benchmark
    public char[] jackson() {
        for (int i = 0; i < k; i++) {
            outputLong(value++, chars, 0);
        }
        return chars;
    }

    @Benchmark
    public char[] my() {
        for (int i = 0; i < k; i++) {
            MyLongSerializer.toChars(value++, chars, 0);
        }
        return chars;
    }

    @Benchmark
    public char[] java_int() {
        for (int i = 0; i < k; i++) {
            JavaLangHelper.getChars(valueInt++, MyLongSerializer.stringSizeOf(valueInt), chars);
        }
        return chars;
    }

    @Benchmark
    public char[] my_int() {
        for (int i = 0; i < k; i++) {
            MyLongSerializer.toChars(valueInt++, chars, 0);
        }
        return chars;
    }

    @Benchmark
    public char[] my_int_2() {
        for (int i = 0; i < k; i++) {
            MyLongSerializer.toChars2(valueInt++, chars, 0);
        }
        return chars;
    }

    @Benchmark
    public char[] jackson_int() {
        for (int i = 0; i < k; i++) {
            outputInt(valueInt++, chars, 0);
        }
        return chars;
    }

    //Jackson
    private static int MILLION = 1000000;
    private static int BILLION = 1000000000;
    private static long TEN_BILLION_L = 10000000000L;
    private static long THOUSAND_L = 1000L;

    private static long MIN_INT_AS_LONG = (long) Integer.MIN_VALUE;
    private static long MAX_INT_AS_LONG = (long) Integer.MAX_VALUE;

    final static String SMALLEST_LONG = String.valueOf(Long.MIN_VALUE);

    private final static char NC = (char) 0;
    final static char[] LEAD_3 = new char[4000];
    final static char[] FULL_3 = new char[4000];

    static {
        /* Let's fill it with NULLs for ignorable leading digits,
         * and digit chars for others
         */
        int ix = 0;
        for (int i1 = 0; i1 < 10; ++i1) {
            char f1 = (char) ('0' + i1);
            char l1 = (i1 == 0) ? NC : f1;
            for (int i2 = 0; i2 < 10; ++i2) {
                char f2 = (char) ('0' + i2);
                char l2 = (i1 == 0 && i2 == 0) ? NC : f2;
                for (int i3 = 0; i3 < 10; ++i3) {
                    // Last is never to be empty
                    char f3 = (char) ('0' + i3);
                    LEAD_3[ix] = l1;
                    LEAD_3[ix + 1] = l2;
                    LEAD_3[ix + 2] = f3;
                    FULL_3[ix] = f1;
                    FULL_3[ix + 1] = f2;
                    FULL_3[ix + 2] = f3;
                    ix += 4;
                }
            }
        }
    }

    private static int calcLongStrLength(long v) {
        int len = 10;
        long cmp = TEN_BILLION_L;

        // 19 is longest, need to worry about overflow
        while (v >= cmp) {
            if (len == 19) {
                break;
            }
            ++len;
            cmp = (cmp << 3) + (cmp << 1); // 10x
        }
        return len;
    }

    private static int leading3(int t, char[] b, int off) {
        int digitOffset = (t << 2);
        char c = LEAD_3[digitOffset++];
        if (c != NC) {
            b[off++] = c;
        }
        c = LEAD_3[digitOffset++];
        if (c != NC) {
            b[off++] = c;
        }
        // Last is required to be non-empty
        b[off++] = LEAD_3[digitOffset];
        return off;
    }

    private static int full3(int t, char[] b, int off) {
        int digitOffset = (t << 2);
        b[off++] = FULL_3[digitOffset++];
        b[off++] = FULL_3[digitOffset++];
        b[off++] = FULL_3[digitOffset];
        return off;
    }

    /**
     * @return Offset within buffer after outputting int
     */
    public static int outputInt(int v, char[] b, int off) {
        if (v < 0) {
            if (v == Integer.MIN_VALUE) {
                /* Special case: no matching positive value within range;
                 * let's then "upgrade" to long and output as such.
                 */
                return outputLong((long) v, b, off);
            }
            b[off++] = '-';
            v = -v;
        }

        if (v < MILLION) { // at most 2 triplets...
            if (v < 1000) {
                if (v < 10) {
                    b[off++] = (char) ('0' + v);
                } else {
                    off = leading3(v, b, off);
                }
            } else {
                int thousands = v / 1000;
                v -= (thousands * 1000); // == value % 1000
                off = leading3(thousands, b, off);
                off = full3(v, b, off);
            }
            return off;
        }

        // ok, all 3 triplets included
        /* Let's first hand possible billions separately before
         * handling 3 triplets. This is possible since we know we
         * can have at most '2' as billion count.
         */
        boolean hasBillions = (v >= BILLION);
        if (hasBillions) {
            v -= BILLION;
            if (v >= BILLION) {
                v -= BILLION;
                b[off++] = '2';
            } else {
                b[off++] = '1';
            }
        }
        int newValue = v / 1000;
        int ones = (v - (newValue * 1000)); // == value % 1000
        v = newValue;
        newValue /= 1000;
        int thousands = (v - (newValue * 1000));

        // value now has millions, which have 1, 2 or 3 digits
        if (hasBillions) {
            off = full3(newValue, b, off);
        } else {
            off = leading3(newValue, b, off);
        }
        off = full3(thousands, b, off);
        off = full3(ones, b, off);
        return off;
    }

    /**
     * @return Offset within buffer after outputting int
     */
    public static int outputLong(long v, char[] b, int off) {
        // First: does it actually fit in an int?
        if (v < 0L) {
            /* MIN_INT is actually printed as long, just because its
             * negation is not an int but long
             */
            if (v > MIN_INT_AS_LONG) {
                return outputInt((int) v, b, off);
            }
            if (v == Long.MIN_VALUE) {
                // Special case: no matching positive value within range
                int len = SMALLEST_LONG.length();
                SMALLEST_LONG.getChars(0, len, b, off);
                return (off + len);
            }
            b[off++] = '-';
            v = -v;
        } else {
            if (v <= MAX_INT_AS_LONG) {
                return outputInt((int) v, b, off);
            }
        }

        /* Ok: real long print. Need to first figure out length
         * in characters, and then print in from end to beginning
         */
        int origOffset = off;
        off += calcLongStrLength(v);
        int ptr = off;

        // First, with long arithmetics:
        while (v > MAX_INT_AS_LONG) { // full triplet
            ptr -= 3;
            long newValue = v / THOUSAND_L;
            int triplet = (int) (v - newValue * THOUSAND_L);
            full3(triplet, b, ptr);
            v = newValue;
        }
        // Then with int arithmetics:
        int ivalue = (int) v;
        while (ivalue >= 1000) { // still full triplet
            ptr -= 3;
            int newValue = ivalue / 1000;
            int triplet = ivalue - (newValue * 1000);
            full3(triplet, b, ptr);
            ivalue = newValue;
        }
        // And finally, if anything remains, partial triplet
        leading3(ivalue, b, origOffset);

        return off;
    }

    public static class MyLongSerializer {

        final static int DELIMITER = 1000_000_000;
        final static long DELIMITER_LONG = 1_000_000_000_000_000_000l;

        /**
         * All possible chars for representing a number as a String
         */
        final static char[] DIGITS = {
                '0', '1', '2', '3', '4', '5',
                '6', '7', '8', '9', 'a', 'b',
                'c', 'd', 'e', 'f', 'g', 'h',
                'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z'
        };

        final static char[] DIGIT_TENS = {
                '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
                '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
                '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
                '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
                '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
                '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
                '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
                '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
                '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
                '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        };

        final static char[] DIGIT_ONES = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        };

        final static char[] THOUSAND;

        static {
            THOUSAND = new char[4000]; // because <<2 faster than *3
            for (int i = 0; i < 1000; i++) {
                THOUSAND[4 * i] = (char) ('0' + (i % 10));
                THOUSAND[4 * i + 1] = (char) ('0' + ((i / 10) % 10));
                THOUSAND[4 * i + 2] = (char) ('0' + (i / 100));
                THOUSAND[4 * i + 3] = (char) (i > 10 ? (i > 100 ? 3 : 2) : 1); //length of number
            }
        }

        // Requires positive x
        public static int stringSizeOf(long x) {
            long p = 10;
            for (int i = 1; i < 19; i++) {
                if (x < p)
                    return i;
                p = 10 * p;
            }
            return 19;
        }

        public static int stringSizeOf(int x) {
            int p = 10;
            for (int i = 1; i < 10; i++) {
                if (x < p)
                    return i;
                p = 10 * p;
            }
            return 10;
        }

        public static int stringSizeOf2(int i) {
            if (i >= 10000) {
                if (i >= 10_000_000) {
                    if (i >= 1_000_000_000)
                        return 10;
                    else if (i >= 100_000_000)
                        return 9;
                    else
                        return 8;
                } else {
                    if (i >= 1_000_000)
                        return 7;
                    else if (i >= 100_000)
                        return 6;
                    else
                        return 5;
                }
            } else {
                if (i >= 100) {
                    if (i >= 1_000)
                        return 4;
                    else
                        return 3;
                } else {
                    if (i >= 10)
                        return 2;
                    else
                        return 1;
                }
            }
        }

        public static int toChars(long l, char[] buf, int offset) {
            if (l < 0) {
                buf[offset++] = '-';
                l = -l;
            }

            if (l > DELIMITER) {
//            if (l >= DELIMITER_LONG) {
//                //todo mega big long
//            }

                int half = (int) (l / DELIMITER);
                offset += stringSizeOf2(half);
                getChars2(half, offset, buf);

                half = (int) (l - half * 1l * DELIMITER);
                getChars2(half, offset + 9, buf);
                fillWithZeroes(buf, offset, 9 - stringSizeOf2(half));
                return offset + 9;
            } else {
                int i = (int) l;
                offset += stringSizeOf2(i);
                getChars2(i, offset, buf);
                return offset;
            }
        }

        public static void fillWithZeroes(char[] buf, int offset, int length) {
            for (int i = offset; i < length + offset; i++) {
                buf[i] = '0';
            }
        }

        public static int toChars(int i, char[] buf, int offset) {
            if (i < 0) {
                buf[offset++] = '-';
                i = -i;
            }

//        int length = stringSizeOf(i);
//        offset += length;
            offset += getChars3(i, offset, buf);
            return offset;
        }

        public static int toChars2(int i, char[] buf, int offset) {
            if (i < 0) {
                if (i == Integer.MIN_VALUE) { //-2 147 483 648
                    buf[offset++] = '-';
                    buf[offset++] = '2';
                    buf[offset++] = '1';
                    buf[offset++] = '4';
                    buf[offset++] = '7';
                    buf[offset++] = '4';
                    buf[offset++] = '8';
                    buf[offset++] = '3';
                    buf[offset++] = '6';
                    buf[offset++] = '4';
                    buf[offset++] = '8';
                    return offset;
                }
                buf[offset++] = '-';
                i = -i;
            }

            offset += stringSizeOf2(i);
//        try {
            getChars2(i, offset, buf);
//        } catch (ArrayIndexOutOfBoundsException e) {
//            e.printStackTrace();
//            System.out.println("toChars2: " + i + "\tresult: " + new String(buf, offset - length, length));
//            throw new RuntimeException(e);
//        }
            return offset;
        }

        public static void getChars(int i, int index, char[] buf) {
//        while (i >= 65536) {
//            q = i / 100;
//            // really: r = i2 - (q * 100);
////            r = i - ((q << 6) + (q << 5) + (q << 2));
//            r = i - (q * 100);
//            i = q;
//            buf[--charPos] = DIGIT_ONES[r];
//            buf[--charPos] = DIGIT_TENS[r];
//        }
            if (i < 100) {
                if (i < 10) {
                    buf[index - 1] = DIGIT_ONES[i];
                    return;
                }
                buf[--index] = DIGIT_ONES[i];
                buf[index - 1] = DIGIT_TENS[i];
                return;
            }

            int r;
            int q;
            while (i >= 100) {
                q = i / 100;
                // really: r = i2 - (q * 100);
//            r = i - ((q << 6) + (q << 5) + (q << 2));
                r = i - (q * 100);
                buf[--index] = DIGIT_ONES[r];
                buf[--index] = DIGIT_TENS[r];
                i = q;
            }
            if (i < 10) {
                buf[index - 1] = DIGIT_ONES[i];
                return;
            }
            buf[--index] = DIGIT_ONES[i];
            buf[index - 1] = DIGIT_TENS[i];
        }

        public static void getChars2(int i, int index, char[] buf) {
//        while (i >= 65536) {
//            q = i / 100;
//            // really: r = i2 - (q * 100);
////            r = i - ((q << 6) + (q << 5) + (q << 2));
//            r = i - (q * 100);
//            i = q;
//            buf[--charPos] = DIGIT_ONES[r];
//            buf[--charPos] = DIGIT_TENS[r];
//        }


//        if (i < 1000) {
//            pos = i << 2;
//            intFillThousand(i, pos, buf, index);
//            return;
//        }

//        int r;
//        int q;
//        while (i >= 1000) {
//            q = i / 1000;
//            // really: r = i2 - (q * 100);
////            r = i - ((q << 6) + (q << 5) + (q << 2));
//            r = i - (q * 1000);
//            r = r << 2;
//            buf[index - 1] = THOUSAND[r];
//            buf[index - 2] = THOUSAND[r + 1];
//            buf[index -= 3] = THOUSAND[r + 2];
//            i = q;
//        }


//        boolean hasBillions = (i >= 1_000_000_000);
//        if (hasBillions) {
//            i -= 1_000_000_000;
//            if (i >= 1_000_000_000) {
//                i -= 1_000_000_000;
//                buf[index - 10] = '2';
//            } else {
//                buf[index - 10] = '1';
//            }
//        }
//        int newValue = i / 1000;
//        int ones = (i - (newValue * 1000)); // == value % 1000
//        i = newValue;
//        newValue /= 1000;
//        int thousands = (i - (newValue * 1000));
//
//        // value now has millions, which have 1, 2 or 3 digits
//        if (hasBillions) {
//            intFullFillThousand(newValue << 2, buf, index - 6);
//        } else {
//            intFillThousand(newValue, newValue << 2, buf, index - 6);
//        }
//        intFullFillThousand(thousands << 2, buf, index - 3);
//        intFullFillThousand(ones << 2, buf, index);

//        if (i >= 1000) {
//            i = reduce(i, buf, index);
//            index -= 3;
//            if (i >= 1000) {
//                i = reduce(i, buf, index);
//                index -= 3;
//                if (i >= 1000) {
//                    i = reduce(i, buf, index);
//                    index -= 3;
//                }
//            }
//        }
            int r;
            if (i >= 1000) {
                int q = i / 1000;
                r = i - (q * 1000);
                i = q;
                r = r << 2;
                buf[index - 1] = THOUSAND[r];
                buf[index - 2] = THOUSAND[r + 1];
                buf[index -= 3] = THOUSAND[r + 2];
                if (i >= 1000) {
                    q = i / 1000;
                    r = i - (q * 1000);
                    i = q;
                    r = r << 2;
                    buf[index - 1] = THOUSAND[r];
                    buf[index - 2] = THOUSAND[r + 1];
                    buf[index -= 3] = THOUSAND[r + 2];
                    if (i >= 1000) {
                        q = i / 1000;
                        r = i - (q * 1000);
                        i = q;
                        r = r << 2;
                        buf[index - 1] = THOUSAND[r];
                        buf[index - 2] = THOUSAND[r + 1];
                        buf[index -= 3] = THOUSAND[r + 2];
                    }
                }
            }


//        int
            r = i << 2;
//        intFillThousand(i, r, buf, index);
            buf[index - 1] = THOUSAND[r];
            if (i < 10)
                return;
            buf[index - 2] = THOUSAND[r + 1];
            if (i < 100)
                return;
            buf[index - 3] = THOUSAND[r + 2];
        }

        public static int getChars3(int i, int offset, char[] buf) {
            int r;
            if (i >= 1_000_000) {
                if (i >= 1_000_000_000) {
                    int q = i / 1000;
                    r = i - (q * 1000);
                    i = q;
                    r = r << 2;
                    buf[offset + 9] = THOUSAND[r];
                    buf[offset + 8] = THOUSAND[r + 1];
                    buf[offset + 7] = THOUSAND[r + 2];

                    q = i / 1000;
                    r = i - (q * 1000);
                    i = q;
                    r = r << 2;
                    buf[offset + 6] = THOUSAND[r];
                    buf[offset + 5] = THOUSAND[r + 1];
                    buf[offset + 4] = THOUSAND[r + 2];

                    q = i / 1000;
                    r = i - (q * 1000);
                    i = q;
                    r = r << 2;
                    buf[offset + 3] = THOUSAND[r];
                    buf[offset + 2] = THOUSAND[r + 1];
                    buf[offset + 1] = THOUSAND[r + 2];

                    r = i << 2;
                    buf[offset] = THOUSAND[r];
                    return offset + 10;
                }

                offset += intFill3(i / 1_000_000, buf, offset);
                int q = i / 1000;
                r = i - (q * 1000);
                i = q;
                r = r << 2;
                buf[offset + 5] = THOUSAND[r];
                buf[offset + 4] = THOUSAND[r + 1];
                buf[offset + 3] = THOUSAND[r + 2];

                q = i / 1000;
                r = i - (q * 1000);
//            i = q;
                r = r << 2;
                buf[offset + 2] = THOUSAND[r];
                buf[offset + 1] = THOUSAND[r + 1];
                buf[offset] = THOUSAND[r + 2];
                return offset + 6;
            } else if (i >= 1_000) {
                offset += intFill3(i / 1_000, buf, offset);
                int q = i / 1000;
                r = i - (q * 1000);
//            i = q;
                r = r << 2;
                buf[offset + 2] = THOUSAND[r];
                buf[offset + 1] = THOUSAND[r + 1];
                buf[offset] = THOUSAND[r + 2];
                return offset + 3;
            }

//        return offset + intFill3(i << 2, buf, offset);
            r = i << 2;
            if (r < 40) {
                buf[offset] = THOUSAND[r];
                return offset + 1;
            } else if (r < 400) {
                buf[offset + 1] = THOUSAND[r];
                buf[offset] = THOUSAND[r + 1];
                return offset + 2;
            } else {
                buf[offset + 2] = THOUSAND[r];
                buf[offset + 1] = THOUSAND[r + 1];
                buf[offset] = THOUSAND[r + 2];
                return offset + 3;
            }
        }

        private static void intFillThousand(int i, int pos, char[] buf, int index) {
            buf[index - 1] = THOUSAND[pos];
            if (i < 10)
                return;
            buf[index - 2] = THOUSAND[pos + 1];
            if (i < 100)
                return;
            buf[index - 3] = THOUSAND[pos + 2];
        }

        private static int intFill3(int pos, char[] buf, int offset) {
//        int pos = i << 2;
//        switch (THOUSAND[pos + 3]) {
//            case 1:
//                buf[offset] = THOUSAND[pos];
//                return 1;
//            case 2:
//                buf[offset + 1] = THOUSAND[pos];
//                buf[offset] = THOUSAND[pos + 1];
//                return 2;
//            case 3:
//                buf[offset + 2] = THOUSAND[pos];
//                buf[offset + 1] = THOUSAND[pos + 1];
//                buf[offset] = THOUSAND[pos + 2];
//                return 3;
//        }

            if (pos < 40) {
                buf[offset] = THOUSAND[pos];
                return 1;
            } else if (pos < 400) {
                buf[offset + 1] = THOUSAND[pos];
                buf[offset] = THOUSAND[pos + 1];
                return 2;
            } else {
                buf[offset + 2] = THOUSAND[pos];
                buf[offset + 1] = THOUSAND[pos + 1];
                buf[offset] = THOUSAND[pos + 2];
                return 3;
            }
        }

        private static void intFullFillThousand(int pos, char[] buf, int index) {
            buf[index - 1] = THOUSAND[pos];
            buf[index - 2] = THOUSAND[pos + 1];
            buf[index - 3] = THOUSAND[pos + 2];
        }

        private static int reduce(int i, char[] buf, int index) {
            int q = i / 1000;
            // really: r = i2 - (q * 100);
//            r = i - ((q << 6) + (q << 5) + (q << 2));
            int r = i - (q * 1000);
            r = r << 2;
            buf[index - 1] = THOUSAND[r];
            buf[index - 2] = THOUSAND[r + 1];
            buf[index - 3] = THOUSAND[r + 2];
            return q;
        }

        /**
         * Places characters representing the integer i into the
         * character array buf. The characters are placed into
         * the buffer backwards starting with the least significant
         * digit at the specified index (exclusive), and working
         * backwards from there.
         * <p/>
         * Will fail if i == Long.MIN_VALUE
         */
        public static void getChars(long i, int index, char[] buf) {
            long q;
            int r;
            int charPos = index;
            char sign = 0;

            if (i < 0) {
                sign = '-';
                i = -i;
            }

            // Get 2 digits/iteration using longs until quotient fits into an int
            while (i > Integer.MAX_VALUE) {
                q = i / 100;
                // really: r = i - (q * 100);
                r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
                i = q;
                buf[--charPos] = DIGIT_ONES[r];
                buf[--charPos] = DIGIT_TENS[r];
            }

            // Get 2 digits/iteration using ints
            int q2;
            int i2 = (int) i;
            while (i2 >= 65536) {
                q2 = i2 / 100;
                // really: r = i2 - (q * 100);
                r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
                i2 = q2;
                buf[--charPos] = DIGIT_ONES[r];
                buf[--charPos] = DIGIT_TENS[r];
            }

            // Fall thru to fast mode for smaller numbers
            // assert(i2 <= 65536, i2);
            for (; ; ) {
                q2 = (i2 * 52429) >>> (16 + 3);
                r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
                buf[--charPos] = DIGITS[r];
                i2 = q2;
                if (i2 == 0) break;
            }
            if (sign != 0) {
                buf[--charPos] = sign;
            }
        }

        static String valueOf(int i) {
            char[] chars = new char[11];
            int length = toChars(i, chars, 0);
            return new String(chars, 0, length);
        }

        static String valueOf2(int i) {
            char[] chars = new char[11];
            int length = toChars2(i, chars, 0);
            return new String(chars, 0, length);
        }

        static String valueOf(long l) {
            char[] chars = new char[21];
            int length = toChars(l, chars, 0);
            return new String(chars, 0, length);
        }
    }

    public static class JavaLangHelper {

        /**
         * All possible chars for representing a number as a String
         */
        final static char[] DIGITS = {
                '0', '1', '2', '3', '4', '5',
                '6', '7', '8', '9', 'a', 'b',
                'c', 'd', 'e', 'f', 'g', 'h',
                'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z'
        };

        final static char[] DIGIT_TENS = {
                '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
                '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
                '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
                '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
                '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
                '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
                '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
                '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
                '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
                '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        };

        final static char[] DIGIT_ONES = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        };

        // Requires positive x
        public static int stringSizeOfLong(long x) {
            long p = 10;
            for (int i = 1; i < 19; i++) {
                if (x < p)
                    return i;
                p = 10 * p;
            }
            return 19;
        }

        /**
         * Places characters representing the integer i into the
         * character array buf. The characters are placed into
         * the buffer backwards starting with the least significant
         * digit at the specified index (exclusive), and working
         * backwards from there.
         * <p/>
         * Will fail if i == Long.MIN_VALUE
         */
        public static void getChars(long i, int index, char[] buf) {
            long q;
            int r;
            int charPos = index;
            char sign = 0;

            if (i < 0) {
                sign = '-';
                i = -i;
            }

            // Get 2 digits/iteration using longs until quotient fits into an int
            while (i > Integer.MAX_VALUE) {
                q = i / 100;
                // really: r = i - (q * 100);
                r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
                i = q;
                buf[--charPos] = DIGIT_ONES[r];
                buf[--charPos] = DIGIT_TENS[r];
            }

            // Get 2 digits/iteration using ints
            int q2;
            int i2 = (int) i;
            while (i2 >= 65536) {
                q2 = i2 / 100;
                // really: r = i2 - (q * 100);
                r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
                i2 = q2;
                buf[--charPos] = DIGIT_ONES[r];
                buf[--charPos] = DIGIT_TENS[r];
            }

            // Fall thru to fast mode for smaller numbers
            // assert(i2 <= 65536, i2);
            for (; ; ) {
                q2 = (i2 * 52429) >>> (16 + 3);
                r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
                buf[--charPos] = DIGITS[r];
                i2 = q2;
                if (i2 == 0) break;
            }
            if (sign != 0) {
                buf[--charPos] = sign;
            }
        }

        static void getChars(int i, int index, char[] buf) {
            int q, r;
            int charPos = index;
            char sign = 0;

            if (i < 0) {
                sign = '-';
                i = -i;
            }

            // Generate two digits per iteration
            while (i >= 65536) {
                q = i / 100;
                // really: r = i - (q * 100);
                r = i - ((q << 6) + (q << 5) + (q << 2));
                i = q;
                buf[--charPos] = DIGIT_ONES[r];
                buf[--charPos] = DIGIT_TENS[r];
            }

            // Fall thru to fast mode for smaller numbers
            // assert(i <= 65536, i);
            for (; ; ) {
                q = (i * 52429) >>> (16 + 3);
                r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
                buf[--charPos] = DIGITS[r];
                i = q;
                if (i == 0) break;
            }
            if (sign != 0) {
                buf[--charPos] = sign;
            }
        }

    }
}

