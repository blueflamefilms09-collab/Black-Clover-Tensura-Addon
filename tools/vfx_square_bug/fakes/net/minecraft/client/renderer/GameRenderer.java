package net.minecraft.client.renderer;

public class GameRenderer {
    private static final ShaderInstance POSITION_TEX_COLOR = new ShaderInstance();
    public static ShaderInstance getPositionTexColorShader() { return POSITION_TEX_COLOR; }
}
