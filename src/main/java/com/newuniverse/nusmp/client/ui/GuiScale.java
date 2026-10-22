package com.newuniverse.nusmp.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleMath;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleOptions;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: the owner asked that players can change the size of the mod's GUIs. This is the one helper every GUI goes through.
 * <ul>
 * <li>Screens of the mod are scaled around the centre of the window by hooks on the screen events, so no screen is rewritten: {@code ScreenEvent.Render.Pre}
 * draws the screen itself inside a scaled pose with the mouse position carried into the screen's own coordinates (and cancels the plain draw), and the
 * mouse press / release / drag / scroll events do the same for clicks, so hit-testing and tooltips line up. Only classes of this mod listed in
 * {@link GuiScaleRegistry} are touched; vanilla and other mods' screens never are.</li>
 * <li>HUD layers listed there with a layer name are scaled around their configured anchor by {@code RenderGuiLayerEvent.Pre/Post}.</li>
 * <li>HUD elements that already place themselves (System window, hero HUD, Djinn panel, boss bar) call {@link #hud} / {@link #begin} at their own anchor, so
 * their positions and the Djinn {@code anchor} option stay as they were.</li>
 * </ul>
 * The numbers come from {@link GuiScaleMath}; the values from {@link GuiScaleOptions} (nusmp-gui-client.toml).
 */
public final class GuiScale {
    public static final KeyMapping KEY = new KeyMapping("key.nusmp.gui_size", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_F8, "key.categories.nusmp");

    private static final String PREFIX = "com.newuniverse.nusmp.";
    private static final Map<Class<?>, GuiScaleRegistry.Entry> CLASS_CACHE = new HashMap<>();
    private static final GuiScaleRegistry.Entry NONE = new GuiScaleRegistry.Entry("", GuiScaleRegistry.Kind.SCREEN, GuiScaleRegistry.Mode.NONE, "", "", 0, 0, GuiScaleMath.Anchor.CENTER, "");
    private static final Set<ResourceLocation> PUSHED_LAYERS = new HashSet<>();

    private GuiScale() {}

    /** Call once from the client section of the mod constructor: config, key, config screen button, and the event hooks. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, GuiScaleOptions.SPEC, "nusmp-gui-client.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) (c, parent) -> new GuiSizeScreen(parent, c));
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            while (KEY.consumeClick()) if (mc.screen == null && mc.player != null) mc.setScreen(new GuiSizeScreen(null, null));
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, GuiScale::onRenderPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, GuiScale::onPress);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, GuiScale::onRelease);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, GuiScale::onDrag);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, GuiScale::onScroll);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GuiScale::onLayerPre);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GuiScale::onLayerPost);
        GuiMove.init(modBus, container);                                                    // GUI Move: nusmp-gui-layout-client.toml, the edit key, /nusmp gui move
    }

    // ---- values ----------------------------------------------------------------------------------------------------------------------------------

    /** The size a GUI asks for (global or its own), before the window limit. */
    public static double requested(GuiScaleRegistry.Entry e) {
        double own = GuiScaleOptions.override(e.id());
        if (e.kind() == GuiScaleRegistry.Kind.HUD && !GuiScaleOptions.hudFollowsGlobal()) return GuiScaleMath.hasOverride(own) ? GuiScaleMath.snap(own) : 1.0;
        return GuiScaleMath.resolve(GuiScaleOptions.global(), own);
    }

    /** The scale of a HUD element by its id; 1 when the id is unknown. */
    public static float hud(String id) {
        GuiScaleRegistry.Entry e = GuiScaleRegistry.byId(id);
        return e == null ? 1f : (float) requested(e);
    }

    /**
     * For a HUD element that places itself: pushes a pose scaled by {@link #hud} around (pivotX, pivotY) (screen GUI pixels) and returns true; the caller pops
     * the pose when it is done (only if this returned true). Returns false, and pushes nothing, at the normal size.
     */
    public static boolean begin(GuiGraphics g, String id, float pivotX, float pivotY) {
        float s = hud(id);
        if (GuiScaleMath.isIdentity(s)) return false;
        around(g, pivotX, pivotY, s);
        return true;
    }

    /** Pushes the pose and scales it around a point. Pair with {@code g.pose().popPose()}. */
    public static void around(GuiGraphics g, float px, float py, float s) {
        g.pose().pushPose();
        g.pose().translate(px, py, 0f);
        g.pose().scale(s, s, 1f);
        g.pose().translate(-px, -py, 0f);
    }

    /** The registry entry of a mod screen (its class or the nearest mod superclass), or null for every other screen. */
    public static GuiScaleRegistry.Entry entryOf(Screen screen) {
        if (screen == null) return null;
        GuiScaleRegistry.Entry cached = CLASS_CACHE.get(screen.getClass());
        if (cached == null) {
            cached = NONE;
            for (Class<?> c = screen.getClass(); c != null && c.getName().startsWith(PREFIX); c = c.getSuperclass()) {
                if (GuiScaleRegistry.exempt(c.getName())) break;
                GuiScaleRegistry.Entry e = GuiScaleRegistry.byClass(c.getName());
                if (e != null) { cached = e; break; }
            }
            CLASS_CACHE.put(screen.getClass(), cached);
        }
        return cached == NONE ? null : cached;
    }

    /** The scale a screen is drawn at right now: requested size, held to the window when fit-to-window is on. 1 for screens that are not the mod's. */
    public static double screenScale(Screen screen) {
        GuiScaleRegistry.Entry e = entryOf(screen);
        if (e == null) return 1.0;
        return screenScale(e, screen.width, screen.height, designW(e, screen), designH(e, screen));
    }

    public static double screenScale(GuiScaleRegistry.Entry e, int guiW, int guiH, int designW, int designH) {
        return GuiScaleMath.effective(requested(e), GuiScaleOptions.fit(), designW, designH, guiW, guiH);
    }

    private static int designW(GuiScaleRegistry.Entry e, Screen s) {
        return e.designW() >= 0 ? e.designW() : s instanceof AbstractContainerScreen<?> c ? c.getXSize() : 0;
    }

    private static int designH(GuiScaleRegistry.Entry e, Screen s) {
        return e.designH() >= 0 ? e.designH() : s instanceof AbstractContainerScreen<?> c ? c.getYSize() : 0;
    }

    // ---- screens ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * How the screen is drawn right now, as {@code screen = t * local + o}: reused (no allocation per event). Two cases fill it: the plain GUI Size scaling about the
     * middle of the window (as always), or, when the player placed this screen's panel with GUI Move, the panel's saved place at its own size times the GUI Size size
     * (applied once, see {@code GuiMoveMath.total}). The whole screen is drawn inside it and every mouse position goes back through it.
     */
    private static final GuiMove.Xf XF = new GuiMove.Xf();

    /** Fills {@link #XF} for a screen; false when nothing needs to be hooked (a normal-size screen that was not moved). */
    private static boolean prepare(Screen s) {
        double sc = screenScale(s);
        XF.placed = false;
        if (GuiMove.screenXf(s, sc, XF)) return true;
        if (GuiScaleMath.isIdentity(sc)) return false;
        XF.t = sc;
        XF.ox = s.width / 2.0 * (1.0 - sc);
        XF.oy = s.height / 2.0 * (1.0 - sc);
        return true;
    }

    private static double local(double mouse, double o) {
        return (mouse - o) / XF.t;
    }

    private static void onRenderPre(ScreenEvent.Render.Pre e) {
        Screen s = e.getScreen();
        if (!prepare(s)) return;
        double t = XF.t, ox = XF.ox, oy = XF.oy;
        GuiGraphics g = e.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0.0);
        g.pose().scale((float) t, (float) t, 1f);
        try {
            int mx = (int) Math.round((e.getMouseX() - ox) / t), my = (int) Math.round((e.getMouseY() - oy) / t);
            s.renderWithTooltip(g, mx, my, e.getPartialTick());
        } finally {
            g.pose().popPose();
        }
        e.setCanceled(true);                                           // the screen was drawn above; the plain draw must not happen twice
    }

    private static void onPress(ScreenEvent.MouseButtonPressed.Pre e) {
        Screen s = e.getScreen();
        if (!prepare(s)) return;
        e.setCanceled(true);
        s.mouseClicked(local(e.getMouseX(), XF.ox), local(e.getMouseY(), XF.oy), e.getButton());
    }

    private static void onRelease(ScreenEvent.MouseButtonReleased.Pre e) {
        Screen s = e.getScreen();
        if (!prepare(s)) return;
        e.setCanceled(true);
        s.mouseReleased(local(e.getMouseX(), XF.ox), local(e.getMouseY(), XF.oy), e.getButton());
    }

    private static void onDrag(ScreenEvent.MouseDragged.Pre e) {
        Screen s = e.getScreen();
        if (!prepare(s)) return;
        e.setCanceled(true);
        s.mouseDragged(local(e.getMouseX(), XF.ox), local(e.getMouseY(), XF.oy), e.getMouseButton(),
                GuiScaleMath.deltaToLocal(e.getDragX(), XF.t), GuiScaleMath.deltaToLocal(e.getDragY(), XF.t));
    }

    private static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        Screen s = e.getScreen();
        if (!prepare(s)) return;
        e.setCanceled(true);
        s.mouseScrolled(local(e.getMouseX(), XF.ox), local(e.getMouseY(), XF.oy), e.getScrollDeltaX(), e.getScrollDeltaY());
    }

    // ---- HUD layers ------------------------------------------------------------------------------------------------------------------------------

    private static void onLayerPre(RenderGuiLayerEvent.Pre e) {
        ResourceLocation name = e.getName();
        if (!"nusmp".equals(name.getNamespace())) return;
        GuiScaleRegistry.Entry en = GuiScaleRegistry.byLayer(name.getPath());
        if (en == null) return;
        int moved = GuiMove.layerPre(e.getGuiGraphics(), en.id());     // GUI Move: a hidden layer is not drawn, a placed one is drawn at its saved place (at its own size times the GUI Size size, once)
        if (moved == GuiMove.LAYER_HIDDEN) {
            e.setCanceled(true);
            return;
        }
        if (moved == GuiMove.LAYER_PUSHED) {
            PUSHED_LAYERS.add(name);
            return;
        }
        float s = (float) requested(en);
        if (GuiScaleMath.isIdentity(s)) return;
        GuiGraphics g = e.getGuiGraphics();
        GuiScaleMath.Anchor a = GuiScaleOptions.anchor(en.id());
        around(g, a.pivotX(g.guiWidth()), a.pivotY(g.guiHeight()), s);
        PUSHED_LAYERS.add(name);
    }

    private static void onLayerPost(RenderGuiLayerEvent.Post e) {
        if (!PUSHED_LAYERS.isEmpty() && PUSHED_LAYERS.remove(e.getName())) e.getGuiGraphics().pose().popPose();      // 1.99.0: runs for every HUD layer every frame
    }
}
