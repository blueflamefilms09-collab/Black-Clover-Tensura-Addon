package net.minecraft.client.renderer;

/** Why: harness shim. */
public class RenderType {
    private static final RenderType GUI = new RenderType();
    public static RenderType gui() { return GUI; }
}
