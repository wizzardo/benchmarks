package com.wizzardo.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;

/**
 * Created by wizzardo on 21.03.15.
 */
public class SwitchVsIfBenchmark {

    byte[] bytes;

    @Setup(Level.Iteration)
    public void setup() {
        bytes = "/some_path/foo/bar".getBytes();
    }

    @Benchmark
    public int test_switch() {
        int sum = 0;
        for (byte b : bytes) {
            switch (b) {
                case '/':
                    sum += 1;
                    break;
                case ('0'):
                    sum += 2;
                    break;
                case ('1'):
                    sum += 2;
                    break;
//                case 'z':
//                    sum += 2;
//                    break;
//                case 'x':
//                    sum += 2;
//                    break;
            }
        }
        return sum;
    }

    @Benchmark
    public int test_if() {
        int sum = 0;
        for (byte b : bytes) {
            if (b == '/')
                sum += 1;
            else if (b == ' ')
                sum += 2;
            else if (b == 'z')
                sum += 2;
//            else if (b == 'x')
//                sum += 2;
        }
        return sum;
    }
}
