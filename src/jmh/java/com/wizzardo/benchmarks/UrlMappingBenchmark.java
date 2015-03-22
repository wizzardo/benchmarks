package com.wizzardo.benchmarks;


import com.wizzardo.tools.reflection.StringReflection;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by wizzardo on 28.12.14.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2048m", "-server", "-XX:+AggressiveOpts"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class UrlMappingBenchmark {

    byte[] bytes;
    byte[] bytes2;
    byte[] bytes3;
    byte[] staticPath;
    byte[] dynamicPath_0;
    byte[] dynamicPath_1;
    byte[] dynamicPath_100;
    UrlMapping urlMapping;
    UrlMappingTree urlMappingTree;

    static ByteTree byteTree;

    @Setup(Level.Iteration)
    public void setup() {
        bytes = "/some_path/foo/bar".getBytes();
        bytes2 = "/some_pat!/fo!/ba!".getBytes();
        bytes3 = "/!ome_path/!oo/!ar".getBytes();
        staticPath = "/some_path/foo/bar".getBytes();
        dynamicPath_0 = "/some_path/foo/bar_0/123".getBytes();
        dynamicPath_1 = "/some_path/foo/bar_1/123".getBytes();
        dynamicPath_100 = "/some_path/foo/bar_100/123".getBytes();


        byteTree = new ByteTree();
        byteTree.append("some_path");
        byteTree.append("foo");
        byteTree.append("bar");

        urlMapping = new UrlMapping();
        urlMappingTree = new UrlMappingTree();

        urlMapping.append("/some_path/foo/bar", 0);
        urlMappingTree.append("/some_path/foo/bar", 0);

        for (int i = 0; i <= 100; i++) {
            urlMapping.append("/some_path/foo/bar_" + i + "/*", 1);
            urlMappingTree.append("/some_path/foo/bar_" + i + "/*", 1);
            byteTree.append("bar_" + i);
        }

    }

    @Benchmark
    public int staticUM() throws IOException {
        return urlMapping.handle(new String(staticPath));
    }

    @Benchmark
    public int staticUMT() throws IOException {
        return urlMappingTree.handle(readPath2(staticPath));
    }

    @Benchmark
    public int dynamicUM_0() throws IOException {
        return urlMapping.handle(new String(dynamicPath_0));
    }

    @Benchmark
    public int dynamicUM_1() throws IOException {
        return urlMapping.handle(new String(dynamicPath_1));
    }

    @Benchmark
    public int dynamicUM_100() throws IOException {
        return urlMapping.handle(new String(dynamicPath_100));
    }

    @Benchmark
    public int dynamicUMT_0() throws IOException {
        return urlMappingTree.handle(readPath2(dynamicPath_0));
    }

    @Benchmark
    public int dynamicUMT_1() throws IOException {
        return urlMappingTree.handle(readPath2(dynamicPath_1));
    }

    @Benchmark
    public int dynamicUMT_100() throws IOException {
        return urlMappingTree.handle(readPath2(dynamicPath_100));
    }

    @Benchmark
    public int dynamicUMT_0_withByteTree() throws IOException {
        return urlMappingTree.handle(readPath3(dynamicPath_0));
    }

    @Benchmark
    public int dynamicUMT_1_withByteTree() throws IOException {
        return urlMappingTree.handle(readPath3(dynamicPath_1));
    }

    @Benchmark
    public int dynamicUMT_100_withByteTree() throws IOException {
        return urlMappingTree.handle(readPath3(dynamicPath_100));
    }

    static Path readPath(byte[] bytes) {
        if (bytes[0] != '/')
            throw new IllegalStateException("path must starts with '/'");

        int from = 1;
        int i = from;
        Path path = new Path();
        int end = bytes.length;
        while (i < end && bytes[i] != ' ') {
            while (i < end && bytes[i] != '/' && bytes[i] != ' ') {
                i++;
            }
            if (i != from) {
                path.add(bytes, from, i);
                i++;
                from = i;
            }
        }

        return path;
    }

    static Path readPath2(byte[] bytes) {
        if (bytes[0] != '/')
            throw new IllegalStateException("path must starts with '/'");

        Path path = new Path();
        int length = bytes.length;

        int h = 0;
        int offset = 0;
        int k;

        int partStart = 1;
        int partHash = 0;

        char[] data = new char[length];
        for (int i = 1; i < length; i++) {
            data[i] = (char) (k = (bytes[offset + i] & 0xff));
            h = 31 * h + k;
            if (k == '/') {
                if (i == 0)
                    continue;
                char[] part = new char[i - partStart];
                System.arraycopy(data, partStart, part, 0, i - partStart);
                path.parts.add(StringReflection.createString(part, partHash));
                partStart = i + 1;
                partHash = 0;
            } else
                partHash = 31 * partHash + k;
        }
        if (partStart != length) {
            char[] part = new char[length - partStart];
            System.arraycopy(data, partStart, part, 0, length - partStart);
            path.parts.add(StringReflection.createString(part, partHash));
        }

        path.path = StringReflection.createString(data, h);

        return path;
    }

    static Path readPath3(byte[] bytes) {
        if (bytes[0] != '/')
            throw new IllegalStateException("path must starts with '/'");

        Path path = new Path();
        int length = bytes.length;

        int h = 0;
        int offset = 0;
        int k;

        int partStart = 1;
        int partHash = 0;
        ByteTree.Node node = byteTree.getRoot();

        char[] data = new char[length];
        for (int i = 1; i < length; i++) {
            data[i] = (char) (k = (bytes[offset + i] & 0xff));
            h = 31 * h + k;
            if (k == '/') {
                if (i == 0)
                    continue;

                String value = null;
                if (node != null) {
                    value = node.getValue();
                }
                if (value == null) {
                    char[] part = new char[i - partStart];
                    System.arraycopy(data, partStart, part, 0, i - partStart);
                    value = StringReflection.createString(part, partHash);
                }

                path.parts.add(value);
                partStart = i + 1;
                partHash = 0;
                node = byteTree.getRoot();
            } else {
                partHash = 31 * partHash + k;
                if (node != null) {
                    node = node.next(bytes[offset + i]);
                }
            }
        }
        if (partStart != length) {
            String value = null;
            if (node != null) {
                value = node.getValue();
            }
            if (value == null) {
                char[] part = new char[length - partStart];
                System.arraycopy(data, partStart, part, 0, length - partStart);
                value = StringReflection.createString(part, partHash);
            }
            path.parts.add(value);
        }

        path.path = StringReflection.createString(data, h);

        return path;
    }

    public static void main(String[] args) throws IOException {
//        System.out.println(new AsciiStringBenchmark().readPath("/ololo/foo/bar/123".getBytes()));
//        System.out.println(new AsciiStringBenchmark().readPath2("/ololo/foo/bar/123".getBytes()));

//        UrlMappingTree tree = new UrlMappingTree();
//        tree.append("/ololo/foo/bar_1/*", 1);
//        tree.append("/ololo/foo/bar_2/*", 2);
//        System.out.println(tree.handle("/ololo/foo/bar_1/*"));
//        System.out.println(tree.handle("/ololo/foo/bar_2/*"));

        ByteTree byteTree = new ByteTree()
                .append("ololo")
                .append("foo")
                .append("bar_1");

        System.out.println(byteTree.getRoot().get("/ololo/foo/bar_1/*".getBytes(), 1, 5));
        System.out.println(byteTree.getRoot().get("/ololo/foo/bar_1/*".getBytes(), 1, 5) == byteTree.getRoot().get("/ololo/foo/bar_1/*".getBytes(), 1, 5));
    }

    public static class Path {
        private List<String> parts = new ArrayList<>(10);
        private String path;

        public void add(byte[] bytes, int from, int to) {
            parts.add(AsciiReader.read(bytes, from, to - from));
        }

        public int size() {
            return parts.size();
        }

        @Override
        public String toString() {
            return "path: " + parts;
        }
    }

    public static class AsciiReader {

        public static String read(byte[] bytes) {
            return read(bytes, 0, bytes.length);
        }

        public static String read(byte[] bytes, int offset, int length) {
            if (length <= 0)
                return new String();

            int h = 0;
            int k;
            char[] data = new char[length];
            for (int i = 0; i < length; i++) {
                data[i] = (char) (k = (bytes[offset + i] & 0xff));
                h = 31 * h + k;
            }

            return StringReflection.createString(data, h);
        }

        public static String read(byte[] bytes, int offset, int length, int hash) {
//            return read(bytes, offset, length);
            if (length == 0)
                return new String();

            char[] data = new char[length];
            for (int i = 0; i < length; i++) {
                data[i] = (char) (bytes[offset + i] & 0xff);
            }

            return StringReflection.createString(data, hash);
        }

        public static String read(byte[] buffer, int bufferLength, byte[] bytes, int offset, int length) {
            char[] data = new char[bufferLength + length];

            int h = 0;
            int k;
            for (int i = 0; i < bufferLength; i++) {
                data[i] = (char) (k = (buffer[i] & 0xff));
                h = 31 * h + k;
            }
            for (int i = 0; i < length; i++) {
                data[i + bufferLength] = (char) (k = (bytes[offset + i] & 0xff));
                h = 31 * h + k;
            }
            return StringReflection.createString(data, h);
        }
    }

    static public class UrlMapping {

        private static Pattern VARIABLES = Pattern.compile("\\$\\{?([a-zA-Z_]+[\\w]*)\\}?");

        protected HashMap<String, Integer> mapping = new HashMap<>();
        protected LinkedHashMap<Pattern, Integer> regexpMapping = new LinkedHashMap<>();

        public Integer handle(String path) throws IOException {
            Integer handler = mapping.get(path);
            if (handler != null)
                return handler;

            for (Map.Entry<Pattern, Integer> entry : regexpMapping.entrySet()) {
                if (entry.getKey().matcher(path).matches())
                    return entry.getValue();
            }

            return null;
        }

        public UrlMapping append(String url, Integer handler) {
            if (url.contains("*")) {
                regexpMapping.put(Pattern.compile(url.replace("*", ".*")), handler);
            } else if (url.contains("$")) {
                url = url.replaceAll(VARIABLES.pattern(), "([^/]+)").replace("/([^/]+)?", "/?([^/]+)?");
                Pattern pattern = Pattern.compile(url);
                regexpMapping.put(pattern, handler);
            } else {
                mapping.put(url, handler);
            }

            return this;
        }
    }

    static public class UrlMappingTree {

        private static Pattern VARIABLES = Pattern.compile("\\$\\{?([a-zA-Z_]+[\\w]*)\\}?");

        protected HashMap<String, UrlMappingTree> tree = new HashMap<>();
        protected HashMap<String, Integer> mapping = new HashMap<>();
        protected LinkedHashMap<Pattern, Integer> regexpMapping = new LinkedHashMap<>();

        public Integer handle(String path) throws IOException {
            String[] parts = path.split("/");
            return handle(parts);
        }

        public Integer handle(Path path) throws IOException {
            return handle(path.parts);
        }

        public Integer handle(String[] parts) throws IOException {
            UrlMappingTree tree = this;
            for (int i = 0; i < parts.length - 1 && tree != null; i++) {
                String part = parts[i];
                if (part.isEmpty())
                    continue;

                tree = tree.tree.get(part);
            }
            String last = parts[parts.length - 1];
            Integer handler = tree.mapping.get(last);
            if (handler != null)
                return handler;

            for (Map.Entry<Pattern, Integer> entry : tree.regexpMapping.entrySet()) {
                if (entry.getKey().matcher(last).matches())
                    return entry.getValue();
            }

            return null;
        }

        public Integer handle(List<String> parts) throws IOException {
            UrlMappingTree tree = this;
            for (int i = 0; i < parts.size() - 1 && tree != null; i++) {
                String part = parts.get(i);
                if (part.isEmpty())
                    continue;

                tree = tree.tree.get(part);
            }
            String last = parts.get(parts.size() - 1);
            Integer handler = tree.mapping.get(last);
            if (handler != null)
                return handler;

            for (Map.Entry<Pattern, Integer> entry : tree.regexpMapping.entrySet()) {
                if (entry.getKey().matcher(last).matches())
                    return entry.getValue();
            }

            return null;
        }

        private UrlMappingTree appendPart(String url, Integer handler) {
            if (url.contains("*")) {
                regexpMapping.put(Pattern.compile(url.replace("*", ".*")), handler);
            } else if (url.contains("$")) {
                url = url.replaceAll(VARIABLES.pattern(), "([^/]+)").replace("/([^/]+)?", "/?([^/]+)?");
                Pattern pattern = Pattern.compile(url);
                regexpMapping.put(pattern, handler);
            } else {
                mapping.put(url, handler);
            }

            return this;
        }

        public UrlMappingTree append(String url, Integer handler) {
            String[] parts = url.split("/");
            UrlMappingTree tree = this;
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                if (part.isEmpty())
                    continue;

                UrlMappingTree next = tree.tree.get(part);
                if (next == null) {
                    next = new UrlMappingTree();
                    tree.tree.put(part, next);
                }

                tree = next;
            }
            tree.appendPart(parts[parts.length - 1], handler);

            return this;
        }
    }

    static class ByteTree {

        private Node root;

        public ByteTree() {
        }

        public Node getRoot() {
            return root;
        }

        public ByteTree(String s) {
            byte[] bytes = s.getBytes();

            root = new SingleNode();
            Node temp = root;

            for (int i = 0; i < bytes.length; i++) {
                byte b = bytes[i];
                temp = temp.append(b).next(b);
            }
            temp.setValue(s);
        }

        public ByteTree append(String s) {
            return append(s.getBytes(), s);
        }

        public ByteTree append(byte[] bytes, String s) {
            if (root == null)
                root = new SingleNode();

            byte b = bytes[0];
            root = root.append(b);
            Node temp = root.next(b);
            Node prev = root;
            byte p = b;
            for (int i = 1; i < bytes.length; i++) {
                b = bytes[i];
                Node next = temp.append(b);
                prev.set(p, next);
                prev = next;
                temp = next.next(b);
                p = b;
            }
            temp.setValue(s);

            return this;
        }

        public boolean contains(String name) {
            if (root == null)
                return false;

            return root.get(name.getBytes()) != null;
        }

        public static abstract class Node {
            protected String value;

            public abstract Node next(byte b);

            public abstract Node append(byte b);

            public abstract Node set(byte b, Node node);

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            public String get(byte[] bytes) {
                return get(bytes, 0, bytes.length);
            }

            public String get(byte[] bytes, int offset, int length) {
                Node node = getNode(bytes, offset, length);
                return node == null ? null : node.value;
            }

            public Node getNode(byte[] bytes, int offset, int length) {
                Node node = this;
                for (int i = offset; i < offset + length && node != null; i++) {
                    node = node.next(bytes[i]);
                }
                return node;
            }
        }

        public static class ArrayNode extends Node {
            private Node[] nodes;

            public ArrayNode(int size) {
                increase(size);
            }

            @Override
            public Node next(byte b) {
                int i = b & 0xff;
                if (i >= nodes.length)
                    return null;

                return nodes[i];
            }

            @Override
            public Node append(byte b) {
                int i = b & 0xff;
                increase(i + 1);

                if (nodes[i] == null)
                    nodes[i] = new SingleNode();

                return this;
            }

            @Override
            public Node set(byte b, Node node) {
                int i = b & 0xff;
                increase(i + 1);

                nodes[i] = node;

                return this;
            }

            private void increase(int size) {
                if (nodes == null)
                    nodes = new Node[size];
                else if (nodes.length < size) {
                    Node[] temp = new Node[size];
                    System.arraycopy(nodes, 0, temp, 0, nodes.length);
                    nodes = temp;
                }
            }
        }

        public static class SingleNode extends Node {
            private byte b;
            private Node next;

            @Override
            public Node next(byte b) {
                if (b == this.b)
                    return next;
                return null;
            }

            @Override
            public Node append(byte b) {
                if (next != null && this.b != b) {
                    ArrayNode node = new ArrayNode(Math.max(this.b & 0xff, b & 0xff));
                    node.set(this.b, next);
                    node.append(b);
                    return node;
                } else if (this.b == b)
                    return this;
                else {
                    this.b = b;
                    next = new SingleNode();
                    return this;
                }
            }

            @Override
            public Node set(byte b, Node n) {
                if (next != null && this.b != b) {
                    ArrayNode node = new ArrayNode(Math.max(this.b & 0xff, b & 0xff));
                    node.set(this.b, next);
                    node.set(b, n);
                    return node;
                } else if (this.b == b) {
                    next = n;
                    return this;
                } else {
                    this.b = b;
                    next = n;
                    return this;
                }
            }

            @Override
            public String toString() {
                return "single " + b;
            }
        }
    }
}
