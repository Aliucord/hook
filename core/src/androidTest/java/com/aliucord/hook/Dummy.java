package com.aliucord.hook;

public class Dummy {
    public boolean initialized;

    Object a;
    int b;
    Integer c;
    String d;

    public Dummy() {
        initialized = true;
    }

    public Dummy(Object a, int b, Integer c, String d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public Dummy(Object... varargs) {}
}
