package com.newuniverse.nusmp.client.skilltree;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.client.skilltree.layout.ArtsBarModel;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.skilltree.PlayerTreeState;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.line.LineText;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: arts of a generated line are not ManasCore skills (the skill registry is frozen at startup and 5,000 nodes must not be 5,000 skills), so they have no Tensura skill key. This is the client half of the ARTS BAR
 * that replaces it: a strip above the hotbar listing the unlocked arts of the line the player has EQUIPPED (the pure {@link ArtsBarModel} decides the list, the same one the server's cast gate accepts), three keys
 * (previous art, next art, use the art; defaults [ ] and \ so they clash with nothing the mod already binds, and every one can be rebound under Controls) and one more that opens the Passives panel. A press of the
 * use key sends {@code CastArtPayload(route, node)}; the server validates the gate, the cooldown, the target and the price, so the bar never decides anything and a modified client gains nothing. The server tells the
 * client when a cooldown starts ({@code ArtState}), so the bar can draw the sweep and the seconds left, and which stance is up. The bar is drawn as a GUI layer, hidden while a screen is open or the GUI is hidden, and
 * a draw that throws switches the bar off with one log line instead of one per frame. The list is re-read every few ticks, not every frame. Everything is reset on leaving the world.
 */
public final class ArtsBarClient {
    private ArtsBarClient() {}

    private static final String CATEGORY = "key.categories.nusmp";
    public static final KeyMapping NEXT = new KeyMapping("key.nusmp.line_arts_next", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY);
    public static final KeyMapping PREV = new KeyMapping("key.nusmp.line_arts_prev", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY);
    public static final KeyMapping CAST = new KeyMapping("key.nusmp.line_arts_cast", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, CATEGORY);
    public static final KeyMapping PASSIVES = new KeyMapping("key.nusmp.line_passives", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SEMICOLON, CATEGORY);

    private static final int CELL = 20, GAP = 3, MAX_VISIBLE = 7, REFRESH_TICKS = 5;
    private static final ArtsBarModel MODEL = new ArtsBarModel();
    /** Art key to {cooldown end, cooldown length} in client game ticks. */
    private static final Map<String, long[]> COOLDOWNS = new HashMap<>();
    private static String stance = "";
    private static int sinceRefresh = REFRESH_TICKS;
    private static boolean broken;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(NEXT);
            e.register(PREV);
            e.register(CAST);
            e.register(PASSIVES);
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "line_arts_bar"), ArtsBarClient::render));
        NeoForge.EVENT_BUS.addListener(ArtsBarClient::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> reset());
    }

    static void reset() {
        MODEL.set(List.of());
        COOLDOWNS.clear();
        stance = "";
        sinceRefresh = REFRESH_TICKS;
        broken = false;
    }

    /** The server's word that an art started its cooldown (and which stance is up). Client thread. */
    public static void onArtState(String key, int cooldownTicks, String stanceKey) {
        Minecraft mc = Minecraft.getInstance();
        stance = stanceKey == null ? "" : stanceKey;
        if (mc.level == null || key == null || key.isEmpty() || cooldownTicks <= 0) return;
        COOLDOWNS.put(key, new long[]{mc.level.getGameTime() + cooldownTicks, cooldownTicks});
    }

    // ------------------------------------------------------------------ keys and the list
    private static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (++sinceRefresh >= REFRESH_TICKS) {
            sinceRefresh = 0;
            refresh(mc);
        }
        while (NEXT.consumeClick()) if (mc.screen == null) MODEL.cycle(1);
        while (PREV.consumeClick()) if (mc.screen == null) MODEL.cycle(-1);
        while (CAST.consumeClick()) {
            ArtsBarModel.Entry sel = MODEL.selected();
            if (mc.screen == null && sel != null && !mc.player.isSpectator()) TreeActions.cast(sel.routeId(), sel.nodeId());
        }
        while (PASSIVES.consumeClick()) if (mc.screen == null) mc.setScreen(new PassivesScreen(null));
    }

    private static void refresh(Minecraft mc) {
        SkillRoute equipped = null;
        String cls = SkillTreeApi.of(mc.player).activeClass();
        if (!cls.isEmpty()) {
            SkillRoute r = RouteRegistry.byClass(cls);
            if (r != null && r.isLine() && SkillTreeApi.hasRoute(mc.player, r.id())) equipped = r;
        }
        MODEL.set(ArtsBarModel.collect(equipped, new PlayerTreeState(SkillTreeApi.of(mc.player))));
    }

    // ------------------------------------------------------------------ the layer
    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (broken || MODEL.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        try {
            draw(g, mc);
        } catch (RuntimeException | LinkageError ex) {
            broken = true;
            LogUtils.getLogger().warn("[nusmp] the Arts bar failed to draw and is switched off until you rejoin", ex);
        }
    }

    private static void draw(GuiGraphics g, Minecraft mc) {
        Font font = mc.font;
        List<ArtsBarModel.Entry> all = MODEL.entries();
        int visible = Math.min(all.size(), MAX_VISIBLE), start = MODEL.windowStart(visible);
        int totalW = visible * CELL + (visible - 1) * GAP;
        int x0 = g.guiWidth() / 2 - totalW / 2, y0 = g.guiHeight() - 22 - 36;
        long now = mc.level.getGameTime();
        ArtsBarModel.Entry sel = MODEL.selected();
        SkillRoute route = sel == null ? null : RouteRegistry.byId(sel.routeId());
        int accent = route == null ? 0xE8C468 : route.color() & 0xFFFFFF;
        g.fill(x0 - 4, y0 - 3, x0 + totalW + 4, y0 + CELL + 3, UiMath.withAlpha(0x05070E, 0.55f));
        for (int i = 0; i < visible; i++) {
            ArtsBarModel.Entry e = all.get(start + i);
            int x = x0 + i * (CELL + GAP);
            boolean selected = e == sel;
            g.fill(x, y0, x + CELL, y0 + CELL, UiMath.withAlpha(selected ? accent : 0x2A3144, selected ? 0.35f : 0.8f));
            g.renderOutline(x, y0, CELL, CELL, selected ? TreeSkin.opaque(accent) : 0xFF3B4358);
            g.renderItem(ItemIcons.of(e.icon(), route == null ? "" : route.icon()), x + 2, y0 + 2);
            long[] cd = COOLDOWNS.get(e.key());
            if (cd != null && cd[0] > now) {
                float frac = Math.min(1f, (cd[0] - now) / (float) Math.max(1, cd[1]));
                g.pose().pushPose();
                g.pose().translate(0f, 0f, 200f);
                g.fill(x + 1, y0 + 1, x + CELL - 1, y0 + 1 + Math.round((CELL - 2) * frac), UiMath.withAlpha(0x05070E, 0.72f));
                long left = cd[0] - now;
                String t = left >= 200 ? Long.toString((left + 19) / 20) : Long.toString(left / 20) + "." + (left % 20) / 2;
                g.drawCenteredString(font, t, x + CELL / 2, y0 + (CELL - 8) / 2, 0xFFFFFFFF);
                g.pose().popPose();
            } else if (e.stance() && e.key().equals(stance)) {
                g.renderOutline(x + 1, y0 + 1, CELL - 2, CELL - 2, TreeSkin.gold());
                g.pose().pushPose();
                g.pose().translate(0f, 0f, 200f);
                g.drawString(font, LineText.tr(LineText.ARTS_STANCE_ON), x + 2, y0 + CELL - 9, TreeSkin.gold(), true);
                g.pose().popPose();
            }
        }
        if (sel != null) {
            long[] cd = COOLDOWNS.get(sel.key());
            boolean ready = cd == null || cd[0] <= now;
            String label = sel.name() + (ready ? "" : "");
            g.drawCenteredString(font, label, g.guiWidth() / 2, y0 - 13, ready ? TreeSkin.opaque(accent) : TreeSkin.dim());
        }
    }
}
