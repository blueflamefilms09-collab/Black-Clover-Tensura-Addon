package com.newuniverse.nusmp.client.skilltree;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.skilltree.layout.MenuCheckbox;
import com.newuniverse.nusmp.skilltree.menu.MenuConfig;
import com.newuniverse.nusmp.skilltree.menu.MenuText;
import io.github.manasmods.tensura.client.screen.AbilitySelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * Why: the owner wants the passive switch to be the checkbox of Tensura's own ability screen: a box that is ticked when the passive is ON (tooltip "ON") and empty when it is OFF (tooltip "OFF"). This is that
 * widget, as a real {@code AbstractWidget} (so it gets the standard hover state and narration) that the nested panel positions and draws once per visible row. It does not own the click rule: the panel asks
 * {@link MenuCheckbox#next} and sends the existing {@code TogglePassive} payload, the same request the Passives panel sends, so the server's checks and its truth-sync are the only authority. Two looks: the
 * vanilla widget sprites (always available, the default), and, when the config asks for {@code tensura}, Tensura's own checkbox texture (its protected static {@code CHECKBOX}, read by reflection) drawn only if
 * its size says it is a vertical strip of square frames ({@link MenuCheckbox#stripV}); any other shape, a missing texture or an error falls back to the vanilla sprites for good, because drawing a wrong part of
 * an image nobody can inspect here would be worse than a plain box. The texture's size is read once from the resource, not guessed.
 */
final class TensuraCheckbox extends AbstractWidget {
    static final int SIZE = 12;
    private static final ResourceLocation BOX = ResourceLocation.withDefaultNamespace("widget/checkbox"), BOX_HOVER = ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted"),
            BOX_ON = ResourceLocation.withDefaultNamespace("widget/checkbox_selected"), BOX_ON_HOVER = ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");

    private static boolean tensuraLooked;
    private static ResourceLocation tensuraTexture;
    private static int tensuraW, tensuraH;

    private boolean checked;
    private boolean dim;

    TensuraCheckbox() { super(0, 0, SIZE, SIZE, Component.empty()); }

    void place(int x, int y, boolean checked, boolean dim) {
        setPosition(x, y);
        this.checked = checked;
        this.dim = dim;
    }

    boolean checked() { return checked; }

    /** The tooltip of the box: ON when ticked, OFF when empty. */
    Component tooltip() { return MenuText.tr(MenuCheckbox.tooltipKey(checked)); }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        boolean hover = isMouseOver(mx, my);
        if (!drawTensura(g)) g.blitSprite(checked ? (hover ? BOX_ON_HOVER : BOX_ON) : (hover ? BOX_HOVER : BOX), getX(), getY(), SIZE, SIZE);
        if (dim) g.fill(getX(), getY(), getX() + SIZE, getY() + SIZE, 0x66000000);
    }

    private boolean drawTensura(GuiGraphics g) {
        if (!"tensura".equals(MenuConfig.checkboxSkin())) return false;
        lookAtTensura();
        if (tensuraTexture == null) return false;
        int v = MenuCheckbox.stripV(tensuraW, tensuraH, checked);
        if (v < 0) return false;
        g.blit(tensuraTexture, getX(), getY(), SIZE, SIZE, 0f, (float) v, tensuraW, tensuraW, tensuraW, tensuraH);
        return true;
    }

    /** Reads Tensura's checkbox texture and its size, once. */
    private static void lookAtTensura() {
        if (tensuraLooked) return;
        tensuraLooked = true;
        try {
            Field f = AbilitySelectionScreen.class.getDeclaredField("CHECKBOX");
            f.setAccessible(true);
            ResourceLocation rl = (ResourceLocation) f.get(null);
            var res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) return;
            try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                tensuraW = img.getWidth();
                tensuraH = img.getHeight();
            }
            tensuraTexture = rl;
        } catch (Exception | LinkageError e) {
            LogUtils.getLogger().warn("[nusmp] tensura menu: Tensura's checkbox texture could not be read, the vanilla box is used", e);
            tensuraTexture = null;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }
}
