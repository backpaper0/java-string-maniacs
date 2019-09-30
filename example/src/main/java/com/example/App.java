package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.LongSupplier;

public class App {

    String s = "hello";

    public static void main(final String[] args) throws Exception {
        //        new App();
        run();

        final LongSupplier f = () -> Runtime.getRuntime().freeMemory();
        long freeMemory = f.getAsLong();
        do {
            System.gc();
        } while (freeMemory < (freeMemory = f.getAsLong()));

        final Object const4 = "hello";
        System.out.println(System.identityHashCode(const4));
    }

    static void run() throws Exception {
        final MyClassLoader loader1 = new MyClassLoader();
        final Class<?> class1 = Class.forName("com.example.App$Foo", true, loader1);
        final Constructor<?> constructor1 = class1.getDeclaredConstructor();
        final Object instance1 = constructor1.newInstance();
        final Field field1 = instance1.getClass().getDeclaredField("CONST");
        final Object const1 = field1.get(null);

        final MyClassLoader loader2 = new MyClassLoader();
        final Class<?> class2 = Class.forName("com.example.App$Foo", true, loader2);
        final Constructor<?> constructor2 = class2.getDeclaredConstructor();
        final Object instance2 = constructor2.newInstance();
        final Field field2 = instance2.getClass().getDeclaredField("CONST");
        final Object const2 = field2.get(null);

        final String s = new String(new char[] { 'h', 'e', 'l', 'l', 'o' });
        final Object const3 = s.intern();

        System.out.println(const1 == const2);
        System.out.println(const1 == const3);
        System.out.println(const1 == s);
        System.out.println(System.identityHashCode(const1));
        System.out.println(System.identityHashCode(const2));
        System.out.println(System.identityHashCode(const3));
        System.out.println(System.identityHashCode(s));
    }

    public static class Foo {
        public static final String CONST = "hello";
    }

    public static class MyClassLoader extends ClassLoader {

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve)
                throws ClassNotFoundException {
            if (name.startsWith("com.example.") == false) {
                return super.loadClass(name, resolve);
            }
            try {
                final URL url = getResource(name.replace('.', '/') + ".class");
                final URLConnection conn = url.openConnection();
                try (InputStream in = url.openStream()) {
                    final byte[] b = new byte[conn.getContentLength()];
                    in.read(b);
                    final Class<?> c = defineClass(name, b, 0, b.length);
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
