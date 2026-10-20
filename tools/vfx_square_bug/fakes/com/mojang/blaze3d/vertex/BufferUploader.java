package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.systems.RenderSystem;

/** Harness fake: a draw prints the state it sees and every vertex it gets. */
public class BufferUploader {
    public static void drawWithShader(MeshData mesh) {
        RenderSystem.LOG.add("DRAW");
        RenderSystem.LOG.add("STATE tex=" + RenderSystem.texture0 + " blend=" + RenderSystem.blend + " func=" + RenderSystem.blendFunc + " cull=" + RenderSystem.cull + " depthTest=" + RenderSystem.depthTest
                + " depthMask=" + RenderSystem.depthMask + " shader=" + RenderSystem.shaderSet + " shaderColor=" + java.util.Arrays.toString(RenderSystem.shaderColor));
        for (String s : mesh.vertices) RenderSystem.LOG.add(s);
        RenderSystem.LOG.add("END " + mesh.vertices.size());
    }
}
