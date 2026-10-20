package com.mojang.blaze3d.vertex;

import java.util.ArrayList;
import java.util.List;

public class BufferBuilder {
    private final List<String> out = new ArrayList<>();
    private float x, y, z, u, v;

    public BufferBuilder addVertex(float x, float y, float z) { this.x = x; this.y = y; this.z = z; return this; }
    public BufferBuilder setUv(float u, float v) { this.u = u; this.v = v; return this; }
    /** Vertices are written bit for bit (float bits), so the comparison is exact. */
    public BufferBuilder setColor(int r, int g, int b, int a) {
        out.add(String.format("v %08x %08x %08x uv %08x %08x rgba %d %d %d %d", Float.floatToIntBits(x), Float.floatToIntBits(y), Float.floatToIntBits(z), Float.floatToIntBits(u), Float.floatToIntBits(v), r, g, b, a));
        return this;
    }
    public MeshData build() { return out.isEmpty() ? null : new MeshData(out); }
}
