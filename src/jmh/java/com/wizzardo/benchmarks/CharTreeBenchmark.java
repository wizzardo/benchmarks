package com.wizzardo.benchmarks;

import com.wizzardo.tools.misc.CharTree;
import com.wizzardo.tools.reflection.StringReflection;
import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: wizzardo
 * Date: 30.11.14
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class CharTreeBenchmark {


    static class Key {
        String string;
        char[] chars;

        Key(String string) {
            this.string = string;
            chars = string.toCharArray();
        }
    }

    static class CharsHolder {
        char[] chars;
        int offset;
        int length;
        int hash;

        CharsHolder(char[] chars, int offset, int length) {
            this.chars = chars;
            this.offset = offset;
            this.length = length;


            int h = 0;
            for (int i = 0; i < length; i++) {
                h = 31 * h + chars[offset + i];
            }
            hash = h;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (!(obj instanceof CharsHolder))
                return false;

            CharsHolder that = (CharsHolder) obj;
            if (that.length != length)
                return false;

            for (int i = 0; i < length; i++) {
                if (chars[offset + i] != that.chars[that.offset + i])
                    return false;
            }

            return true;
        }
    }

    Map<CharsHolder, String> map;
    CharTree<String> charTree;
    Key key;

    @Setup(Level.Iteration)
    public void setup() {
        charTree = new CharTree<>();
        map = new HashMap<>();


//        for (int i = 0; i < 1000; i++) {
//            key = new Key("ololo" + i);
//            map.put(key.string, key.string);
//            charTree.append(key.string);
//        }

        key = new Key("oloLo");
        map.put(new CharsHolder(key.chars, 0, key.chars.length), key.string);
        charTree.append(key.string, key.string);

        key = new Key("ololO");
        map.put(new CharsHolder(key.chars, 0, key.chars.length), key.string);
        charTree.append(key.string, key.string);

        key = new Key("olOlo");
        map.put(new CharsHolder(key.chars, 0, key.chars.length), key.string);
        charTree.append(key.string, key.string);

        key = new Key("oloLO");
        map.put(new CharsHolder(key.chars, 0, key.chars.length), key.string);
        charTree.append(key.string, key.string);

        key = new Key("oLoLO");
        map.put(new CharsHolder(key.chars, 0, key.chars.length), key.string);
        charTree.append(key.string, key.string);

        key = new Key("OLoLO");
        map.put(new CharsHolder(key.chars, 0, key.chars.length), key.string);
        charTree.append(key.string, key.string);

        key = new Key("ololo");
        map.put(new CharsHolder(key.chars, 0, key.chars.length), key.string);
        charTree.append(key.string, key.string);
    }

    @Benchmark
    public int hashMap() {
//        return map.get(key.string).length();
//        return map.get(new String(key.chars)).length();
        return map.get(new CharsHolder(key.chars, 0, key.chars.length)).length();
    }

    @Benchmark
    public int charTree() {
        return charTree.get(key.chars).length();
    }
}