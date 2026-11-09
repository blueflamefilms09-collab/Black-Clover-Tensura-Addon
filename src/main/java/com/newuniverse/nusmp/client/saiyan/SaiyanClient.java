package com.newuniverse.nusmp.client.saiyan;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.saiyan.SaiyanFlightRules;
import com.newuniverse.nusmp.saiyan.SaiyanForms;
import com.newuniverse.nusmp.saiyan.SaiyanNet;
import com.newuniverse.nusmp.saiyan.SaiyanRules;
import com.newuniverse.nusmp.saiyan.SaiyanSenseRules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the client half of the Saiyan system (3.63.0, extended in 3.65.0): rebindable keys that only ASK the server (Ki Blast, Rapid Ki Barrage; 3.65.0: Ki Flight, the burst dash, the senses, Ki Suppression,
 * Rock Scissors Paper, the Kamehameha (hold to charge, release to fire) and the three form keys), the hooks that cache what the server says ({@code SaiyanNet.Sync}, at most four a second, only on change, and
 * {@code SaiyanNet.Ping}, the radar of the senses, one every two seconds), and the HUD panel of a Saiyan: rank and Battle Power, the Ki bar and the stamina bar, the awakening progress, the two attacks with their
 * cooldowns (counted down here between packets), and only when they apply: the form, the flight, the senses, the Goku techniques, the Kaio-ken strain, the Kamehameha charge and the radar (drawn INSIDE this panel,
 * so there is no new element for GUI Move). Ki is the player's Tensura magicule, so while the local player is a Saiyan, Tensura's own magicule bar is hidden (the owner's decision: Ki replaces the magicule bar). The HUD is a render
 * layer of the GUI Move / GUI Size framework (id {@code saiyan_hud}). Nothing here decides anything: every number on screen is the server's. Cost: one panel of about 30 fills and 6 strings per frame, only
 * while the local player is a Saiyan; the text is rebuilt twice a second, not per frame; the layer filter is one namespace compare per HUD layer per frame.
 */
public final class SaiyanClient {
    private SaiyanClient() {}

    private static final Logger LOG = LogUtils.getLogger();

    public static final String CATEGORY = "key.categories.nusmp";
    public static final KeyMapping BLAST = new KeyMapping("key.nusmp.saiyan_blast", InputConstants.Type.KEYSYM, InputConstants.KEY_COMMA, CATEGORY);
    public static final KeyMapping BARRAGE = new KeyMapping("key.nusmp.saiyan_barrage", InputConstants.Type.KEYSYM, InputConstants.KEY_MINUS, CATEGORY);

    public static final KeyMapping FLIGHT = new KeyMapping("key.nusmp.saiyan_flight", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping BURST = new KeyMapping("key.nusmp.saiyan_burst", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping SENSE = new KeyMapping("key.nusmp.saiyan_sense", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_INSERT, CATEGORY);
    public static final KeyMapping SUPPRESS = new KeyMapping("key.nusmp.saiyan_suppress", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_HOME, CATEGORY);
    public static final KeyMapping COMBO = new KeyMapping("key.nusmp.saiyan_combo", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_END, CATEGORY);
    public static final KeyMapping KAME = new KeyMapping("key.nusmp.saiyan_kamehameha", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, CATEGORY);
    public static final KeyMapping FORM = new KeyMapping("key.nusmp.saiyan_form", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, CATEGORY);
    public static final KeyMapping FORM_NEXT = new KeyMapping("key.nusmp.saiyan_form_next", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_UP, CATEGORY);
    public static final KeyMapping FORM_PREV = new KeyMapping("key.nusmp.saiyan_form_prev", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_DOWN, CATEGORY);

    static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath("nusmp", "saiyan_hud");

    private static volatile SaiyanNet.Sync sync;
    private static long syncAt, lastAsk = -100;
    private static volatile Lines lines;
    /** The latest radar ping and the game tick it stays valid until (the server sends one every two seconds while the senses are on). */
    private static volatile SaiyanNet.Ping ping;
    private static long pingUntil;
    /** True while the Kamehameha key is held (so a release is sent exactly once). */
    private static boolean kameHeld;
    /** Tensura layer names already written to the log (so the owner can see the real ids if the bar is not hidden). */
    private static final Set<String> SEEN = new HashSet<>();

    /** Called once by the client setup with the mod bus. */
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(BLAST);
            e.register(BARRAGE);
            e.register(FLIGHT);
            e.register(BURST);
            e.register(SENSE);
            e.register(SUPPRESS);
            e.register(COMBO);
            e.register(KAME);
            e.register(FORM);
            e.register(FORM_NEXT);
            e.register(FORM_PREV);
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(LAYER, SaiyanClient::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, SaiyanClient::onLayer);
        SaiyanNet.clientSync = SaiyanClient::onSync;
        SaiyanNet.clientPing = SaiyanClient::onPing;
    }

    private static void clear() {
        sync = null;
        lines = null;
        ping = null;
        kameHeld = false;
    }

    private static void onPing(SaiyanNet.Ping p) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        ping = p.seconds() <= 0 ? null : p;
        pingUntil = now + 60;
    }

    private static void onSync(SaiyanNet.Sync s) {
        Minecraft mc = Minecraft.getInstance();
        syncAt = mc.level == null ? 0 : mc.level.getGameTime();
        sync = s.saiyan() ? s : null;
        lines = null;
    }

    /** True while the local player is a Saiyan (the server said so). */
    public static boolean active() { return sync != null; }

    // ---------------------------------------------------------------- Ki replaces the magicule bar
    /** Hides Tensura's magicule bar while the local player is a Saiyan: the Ki bar of the Saiyan HUD takes its place. Best effort by layer name; the Tensura layer names are logged once. */
    private static void onLayer(RenderGuiLayerEvent.Pre e) {
        ResourceLocation n = e.getName();
        if (sync == null || !"tensura".equals(n.getNamespace())) return;
        if (SEEN.add(n.getPath())) LOG.info("[nusmp] Saiyan: Tensura HUD layer '{}' {}", n, n.getPath().toLowerCase(Locale.ROOT).contains("magicule") ? "hidden (Ki replaces the magicule bar)" : "kept");
        if (n.getPath().toLowerCase(Locale.ROOT).contains("magicule")) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- keys
    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean can = mc.player != null && mc.getConnection() != null && mc.screen == null && sync != null;
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        press(BLAST, can, now, SaiyanNet.OP_BLAST);
        press(BARRAGE, can, now, SaiyanNet.OP_BARRAGE);
        press(FLIGHT, can, now, SaiyanNet.OP_FLIGHT);
        press(BURST, can, now, SaiyanNet.OP_BURST);
        press(SENSE, can, now, SaiyanNet.OP_SENSE);
        press(SUPPRESS, can, now, SaiyanNet.OP_SUPPRESS);
        press(COMBO, can, now, SaiyanNet.OP_COMBO);
        press(FORM, can, now, SaiyanNet.OP_FORM_TOGGLE);
        press(FORM_NEXT, can, now, SaiyanNet.OP_FORM_NEXT);
        press(FORM_PREV, can, now, SaiyanNet.OP_FORM_PREV);
        kamehameha(can, now);
        SaiyanNet.Ping pg = ping;
        if (pg != null && now > pingUntil) ping = null;
    }

    /** Hold to charge, release to fire: the start is a normal ask, the release is always sent (the server never drops it) so a charge can not stay stuck. */
    private static void kamehameha(boolean can, long now) {
        while (KAME.consumeClick()) { /* the held state below is what counts */ }
        boolean down = can && KAME.isDown();
        if (down && !kameHeld) {
            kameHeld = true;
            lastAsk = now;
            PacketDistributor.sendToServer(new SaiyanNet.Action(SaiyanNet.OP_KAME_START));
        } else if (!down && kameHeld) {
            kameHeld = false;
            PacketDistributor.sendToServer(new SaiyanNet.Action(SaiyanNet.OP_KAME_RELEASE));
        }
    }

    /** Every queued click is drained (so none is kept for later), and at most one ask goes out every four ticks. */
    private static void press(KeyMapping key, boolean can, long now, int op) {
        while (key.consumeClick()) {
            if (!can || (now >= lastAsk && now - lastAsk < SaiyanNet.MIN_GAP_TICKS)) continue;
            lastAsk = now;
            PacketDistributor.sendToServer(new SaiyanNet.Action(op));
        }
    }

    // ---------------------------------------------------------------- text
    static String t(String key, Object... args) { return Component.translatable("nusmp.saiyan.hud." + key, args).getString(); }

    private static String num(double v) { return String.format(Locale.ROOT, "%,d", Math.round(v)); }

    /** The text of the panel, rebuilt when a packet lands and twice a second. {@code extra}: the optional lines (form, flight, senses, Goku techniques), each with whether it is "ready" (bright) or not. */
    record Lines(long builtAt, String rank, String power, String ki, String stamina, String awaken, String blast, String barrage, boolean blastReady, boolean barrageReady, List<String> extra,
                 List<Boolean> extraReady, String strain, String charge) {}

    private static int left(int cdAtSync, long now) { return (int) Math.max(0, cdAtSync - Math.max(0, now - syncAt)); }

    private static String name(String key) { return Component.translatable(key).getString(); }

    static Lines build(SaiyanNet.Sync s, long now) {
        int b = left(s.blastCd(), now), r = left(s.barrageCd(), now);
        List<String> extra = new ArrayList<>(5);
        List<Boolean> ready = new ArrayList<>(5);
        if (s.awakened()) {
            SaiyanForms.Form active = SaiyanForms.ofCode(s.form()), picked = SaiyanForms.ofCode(s.selected());
            if (active != null) {
                extra.add(t("form", name(active.key()), s.formMastery()));
                ready.add(true);
            } else if (picked != null) {
                extra.add(t("form_ready", FORM.getTranslatedKeyMessage().getString(), name(picked.key())));
                ready.add(false);
            }
            String tier = name(SaiyanFlightRules.Tier.of(s.flightTier()).key());
            extra.add(s.flying() ? t("flying", tier) : t("flight_ready", FLIGHT.getTranslatedKeyMessage().getString(), tier));
            ready.add(s.flying());
            int sc = left(s.senseCd(), now);
            String st = name(SaiyanSenseRules.Tier.of(s.senseTier()).key());
            extra.add(s.suppressed() ? t("suppressed", SUPPRESS.getTranslatedKeyMessage().getString()) : sc > 0 ? t("sense_cd", SENSE.getTranslatedKeyMessage().getString(), st, (sc + 19) / 20)
                    : t("sense_ready", SENSE.getTranslatedKeyMessage().getString(), st));
            ready.add(s.suppressed() || sc <= 0);
            if (s.goku()) {
                int k = left(s.kameCd(), now);
                extra.add(k > 0 ? t("kame_cd", KAME.getTranslatedKeyMessage().getString(), (k + 19) / 20) : t("kame_ready", KAME.getTranslatedKeyMessage().getString()));
                ready.add(k <= 0);
                String part = switch (s.combo()) {
                    case 1 -> t("rsp_scissors");
                    case 2 -> t("rsp_paper");
                    default -> t("rsp_rock");
                };
                extra.add(t("rsp", COMBO.getTranslatedKeyMessage().getString(), part));
                ready.add(true);
            }
        }
        return new Lines(now, Component.translatable(SaiyanRules.Rank.byOrdinal(s.rank()).key()).getString(), t("bp", num(s.battlePower())), t("ki", num(s.ki()), num(s.kiMax())),
                t("stamina", num(s.stamina()), num(s.staminaMax())), s.awakened() ? "" : t("meditate", s.meditate()),
                attack(BLAST, "blast", b), attack(BARRAGE, "barrage", r), b <= 0, r <= 0, extra, ready,
                s.strain() > 0 ? t("strain", s.strain()) : "", s.charge() > 0 ? t("charge", s.charge()) : "");
    }

    private static String attack(KeyMapping key, String id, int cdTicks) {
        String k = key.getTranslatedKeyMessage().getString();
        return cdTicks > 0 ? t("cd", k, t(id), (cdTicks + 19) / 20) : t("ready", k, t(id));
    }

    // ---------------------------------------------------------------- the HUD
    private static final int W = 170, RADAR = 64;

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        SaiyanNet.Sync s = sync;
        if (s == null || mc.options.hideGui || mc.player == null || mc.level == null) return;
        long now = mc.level.getGameTime();
        Lines l = lines;
        if (l == null || now - l.builtAt() >= 10 || now < l.builtAt()) lines = l = build(s, now);
        SaiyanNet.Ping pg = ping;
        int h = height(l, pg != null);
        int x = g.guiWidth() - W - 6, y = g.guiHeight() - h - 6;
        GuiMove.report("saiyan_hud", x, y, W, h);                                       // GUI Move / GUI Size: where the panel is drawn (the layer hook moves it)
        paint(g, mc.font, x, y, l, s, pg, mc.player.getYRot());
    }

    static int height(Lines l, boolean radar) {
        return 8 + 11 + 2 * 15 + (l.awaken().isEmpty() ? 0 : 15) + 2 * 10 + 4 + 10 * l.extra().size() + (l.strain().isEmpty() ? 0 : 15) + (l.charge().isEmpty() ? 0 : 15) + (radar ? RADAR + 6 : 0);
    }

    private static void paint(GuiGraphics g, Font font, int x, int y, Lines l, SaiyanNet.Sync s, SaiyanNet.Ping pg, float yaw) {
        int h = height(l, pg != null);
        float time = UiTheme.now();
        UiPanel.panel(g, x, y, W, h, UiTheme.GOLD, 0.3f, 0.92f);
        int cx = x + 7, cy = y + 6, iw = W - 14;
        g.drawString(font, font.plainSubstrByWidth(l.rank(), iw - 60), cx, cy, UiTheme.GOLD, true);
        g.drawString(font, l.power(), x + W - 7 - font.width(l.power()), cy, UiTheme.TEXT, false);
        cy += 12;
        cy = bar(g, font, l.ki(), s.kiMax() <= 0f ? 0f : s.ki() / s.kiMax(), UiTheme.CYAN, cx, cy, iw, time);
        cy = bar(g, font, l.stamina(), s.staminaMax() <= 0f ? 0f : s.stamina() / s.staminaMax(), UiTheme.GREEN, cx, cy, iw, time);
        if (!l.awaken().isEmpty()) cy = bar(g, font, l.awaken(), s.meditate() / 100f, UiTheme.VIOLET, cx, cy, iw, time);
        if (!l.strain().isEmpty()) cy = bar(g, font, l.strain(), s.strain() / 100f, UiTheme.RED, cx, cy, iw, time);
        if (!l.charge().isEmpty()) cy = bar(g, font, l.charge(), s.charge() / 100f, UiTheme.CYAN, cx, cy, iw, time);
        g.drawString(font, font.plainSubstrByWidth(l.blast(), iw), cx, cy, l.blastReady() ? UiTheme.TEXT : UiTheme.TEXT_FAINT, false);
        cy += 10;
        g.drawString(font, font.plainSubstrByWidth(l.barrage(), iw), cx, cy, l.barrageReady() ? UiTheme.TEXT : UiTheme.TEXT_FAINT, false);
        cy += 10;
        for (int i = 0; i < l.extra().size(); i++) {
            g.drawString(font, font.plainSubstrByWidth(l.extra().get(i), iw), cx, cy, l.extraReady().get(i) ? UiTheme.TEXT : UiTheme.TEXT_FAINT, false);
            cy += 10;
        }
        if (pg != null) radar(g, x + (W - RADAR) / 2, cy + 2, pg, yaw);
    }

    /** The radar of the senses: you in the middle, forward is up, one dot per presence (colour = how strong, a square = a boss, cyan = a player). At most 32 dots a frame. */
    private static void radar(GuiGraphics g, int x, int y, SaiyanNet.Ping pg, float yaw) {
        g.fill(x, y, x + RADAR, y + RADAR, 0xAA05080F);
        g.fill(x, y, x + RADAR, y + 1, UiTheme.TEXT_FAINT);
        g.fill(x, y + RADAR - 1, x + RADAR, y + RADAR, UiTheme.TEXT_FAINT);
        g.fill(x, y, x + 1, y + RADAR, UiTheme.TEXT_FAINT);
        g.fill(x + RADAR - 1, y, x + RADAR, y + RADAR, UiTheme.TEXT_FAINT);
        int mid = RADAR / 2;
        g.fill(x + mid, y + 2, x + mid + 1, y + RADAR - 2, 0x33FFFFFF);
        g.fill(x + 2, y + mid, x + RADAR - 2, y + mid + 1, 0x33FFFFFF);
        double rad = Math.toRadians(yaw), sin = Math.sin(rad), cos = Math.cos(rad), scale = (mid - 3) / (double) Math.max(1, pg.radius());
        for (SaiyanNet.Entry e : pg.entries()) {
            double dx = SaiyanSenseRules.unpack(e.dx()), dz = SaiyanSenseRules.unpack(e.dz());
            double right = -dx * cos - dz * sin, fwd = -dx * sin + dz * cos;
            int px = x + mid + (int) Math.round(right * scale), py = y + mid - (int) Math.round(fwd * scale);
            if (px < x + 1 || px > x + RADAR - 3 || py < y + 1 || py > y + RADAR - 3) continue;
            int rgb = e.kind() == 1 ? UiTheme.CYAN : switch (e.signature()) {
                case 0 -> UiTheme.TEXT_FAINT;
                case 1 -> UiTheme.GREEN;
                case 2 -> UiTheme.GOLD;
                case 3 -> 0xFFFF9A40;
                default -> UiTheme.RED;
            };
            int size = e.kind() == 2 ? 4 : 2;
            g.fill(px, py, px + size, py + size, rgb);
        }
        g.fill(x + mid - 1, y + mid - 1, x + mid + 2, y + mid + 2, UiTheme.WHITE);
    }

    private static int bar(GuiGraphics g, Font font, String label, float frac, int rgb, int x, int y, int w, float time) {
        g.drawString(font, font.plainSubstrByWidth(label, w), x, y, UiTheme.TEXT, false);
        UiBar.draw(g, x, y + 10, w, 3, UiMath.clamp01(frac), rgb, time, 1f);
        return y + 15;
    }
}
