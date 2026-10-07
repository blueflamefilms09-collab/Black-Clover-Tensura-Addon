package com.newuniverse.nusmp.vfx.client.layer;

import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 0.56: the bridge between {@link DarkSlashLayer} and the Dimension Slash shader. The VFX vertex buffer draws every batch with one
 * fixed shader, so the tear's main quad goes through this hook instead: the client wires {@code drawer} to
 * {@code NURenderTypes.drawDimensionSlash} when the shaders register. Where there is no drawer (the headless preview, a driver that
 * could not compile the shader) {@link #quad} returns false and the layer draws the same texture as a plain sprite.
 */
public final class DarkSlashShaderPass {
    private DarkSlashShaderPass() {}

    /** Draws one quad now: corners camera-relative, p0 bottom left, p1 bottom right, p2 top right, p3 top left; false = not drawn. */
    public interface Drawer {
        boolean draw(ResourceLocation tex, Matrix4f modelView, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, int argb);
    }

    public static volatile Drawer drawer;

    static boolean quad(ResourceLocation tex, Matrix4f modelView, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, int argb) {
        Drawer d = drawer;
        return d != null && d.draw(tex, modelView, p0, p1, p2, p3, argb);
    }
}
