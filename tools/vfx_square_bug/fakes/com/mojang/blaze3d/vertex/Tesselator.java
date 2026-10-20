package com.mojang.blaze3d.vertex;

public class Tesselator {
    private static final Tesselator INSTANCE = new Tesselator();
    public static Tesselator getInstance() { return INSTANCE; }
    public BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) { return new BufferBuilder(); }
}
