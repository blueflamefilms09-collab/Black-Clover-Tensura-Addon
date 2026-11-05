package com.newuniverse.nusmp.client.beastwill;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.beastwill.BeastRank;
import com.newuniverse.nusmp.beastwill.BeastWillNet;
import com.newuniverse.nusmp.beastwill.BeastWillRules;
import com.newuniverse.nusmp.client.multiverse.MultiverseClientConfig;
import com.newuniverse.nusmp.client.multiverse.MultiverseStatusScreen;
import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the client half of the Beast Will system (3.45.0): five rebindable keys that only ASK the server (Acquire form, Integrate form, ability 1, ability 2, swap the will; Shift swaps backwards), the
 * packet hooks that cache what the server says ({@code Sync} every change, {@code Info} when the active will changes, {@code Preview} for the absorption screen), and the HUD panel: the equipped will with
 * its beast, rank and affinity, the form, assimilation / mastery / strain bars with their tier warnings, what unlocks next, the abilities with their cooldowns (counted down here between packets),
 * the fit with the player's race and every active penalty or lock. The HUD is a render layer of the GUI Move / GUI Size framework (id {@code beastwill_hud}), so it can be moved, resized or hidden.
 * Nothing here decides anything: every number on screen is the server's. Cost: one panel of about 40 fills and 12 strings per frame, only while the player holds a will; the text lines are rebuilt twice
 * a second, not per frame.
 */
public final class BeastWillClient {
    private BeastWillClient() {}

    public static final String CATEGORY = "key.categories.nusmp";
    public static final KeyMapping ACQUIRE = new KeyMapping("key.nusmp.beastwill_acquire", InputConstants.Type.KEYSYM, InputConstants.KEY_LBRACKET, CATEGORY);
    public static final KeyMapping INTEGRATE = new KeyMapping("key.nusmp.beastwill_integrate", InputConstants.Type.KEYSYM, InputConstants.KEY_RBRACKET, CATEGORY);
    public static final KeyMapping ABILITY1 = new KeyMapping("key.nusmp.beastwill_ability1", InputConstants.Type.KEYSYM, InputConstants.KEY_SEMICOLON, CATEGORY);
    public static final KeyMapping ABILITY2 = new KeyMapping("key.nusmp.beastwill_ability2", InputConstants.Type.KEYSYM, InputConstants.KEY_BACKSLASH, CATEGORY);
    public static final KeyMapping SWAP = new KeyMapping("key.nusmp.beastwill_swap", InputConstants.Type.KEYSYM, InputConstants.KEY_EQUALS, CATEGORY);

    static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath("nusmp", "beastwill_hud");

    private static volatile BeastWillNet.Sync sync;
    private static volatile BeastWillNet.Info info;
    private static long syncAt;
    private static long lastAsk = -100;

    /** Called once by the client setup with the mod bus. */
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(ACQUIRE);
            e.register(INTEGRATE);
            e.register(ABILITY1);
            e.register(ABILITY2);
            e.register(SWAP);
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(LAYER, BeastWillClient::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
        BeastWillNet.clientSync = BeastWillClient::onSync;
        BeastWillNet.clientInfo = i -> info = i;
        BeastWillNet.clientPreview = BeastWillClient::onPreview;
    }

    private static void clear() {
        sync = null;
        info = null;
        lines = null;
        pending = null;
    }

    private static void onSync(BeastWillNet.Sync s) {
        Minecraft mc = Minecraft.getInstance();
        syncAt = mc.level == null ? 0 : mc.level.getGameTime();
        sync = s.wills().isEmpty() ? null : s;
        lines = null;
    }

    private static void onPreview(BeastWillNet.Preview p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (MultiverseClientConfig.get(MultiverseClientConfig.BEASTWILL_CONFIRM_IN_MENU)) {   // 3.52.0: the ask is a page of the main menu, not a window of its own
            pending = p;
            mc.setScreen(new MultiverseStatusScreen(mc.screen instanceof MultiverseStatusScreen m ? m.parent() : mc.screen, true));
        } else mc.setScreen(new BeastWillConfirmScreen(p));
    }

    /** The absorb ask waiting for an answer in the main menu (null = none). Closing the menu without answering declines. */
    private static volatile BeastWillNet.Preview pending;

    static BeastWillNet.Preview pending() { return pending; }

    /** One ask from the main menu's buttons: same 3-tick throttle as the keys; the server validates everything. */
    static void ask(int op, int arg) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        BeastWillNet.Sync s = sync;
        if (mc.player == null || mc.getConnection() == null || s == null || s.active().isEmpty() || now - lastAsk < 3) return;
        lastAsk = now;
        PacketDistributor.sendToServer(new BeastWillNet.Action(op, arg));
    }

    /** The cached state (null = no Beast Will): for the Codex page. */
    static BeastWillNet.Sync sync() { return sync; }

    /** The facts of the active will, only when they belong to the synced one. */
    static BeastWillNet.Info info() {
        BeastWillNet.Sync s = sync;
        BeastWillNet.Info i = info;
        return s != null && i != null && i.will().equals(s.active()) ? i : null;
    }

    /** Ticks since the last sync (the seconds fields count down from there). */
    static long sinceSync() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : Math.max(0, mc.level.getGameTime() - syncAt);
    }

    /** Whole seconds left of a field of the last sync, counted down locally. */
    static int left(int secAtSync) { return (int) Math.max(0, (secAtSync * 20L - sinceSync() + 19) / 20); }

    // ---------------------------------------------------------------- keys
    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean can = mc.player != null && mc.getConnection() != null && mc.screen == null && sync != null && !sync.active().isEmpty();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        press(ACQUIRE, can, now, BeastWillNet.OP_FORM, BeastWillRules.ACQUIRE);
        press(INTEGRATE, can, now, BeastWillNet.OP_FORM, BeastWillRules.INTEGRATE);
        press(ABILITY1, can, now, BeastWillNet.OP_ABILITY, 1);
        press(ABILITY2, can, now, BeastWillNet.OP_ABILITY, 2);
        press(SWAP, can, now, BeastWillNet.OP_SWAP, Screen.hasShiftDown() ? -1 : 1);
    }

    /** Every queued click is drained (so none is kept for later), and at most one ask goes out every three ticks. */
    private static void press(KeyMapping key, boolean can, long now, int op, int arg) {
        while (key.consumeClick()) {
            if (!can || now - lastAsk < 3) continue;
            lastAsk = now;
            PacketDistributor.sendToServer(new BeastWillNet.Action(op, arg));
        }
    }

    /** Sends the player's answer to the absorption ask (used by the confirm screen). */
    static void answer(boolean yes) { pending = null; PacketDistributor.sendToServer(new BeastWillNet.Action(yes ? BeastWillNet.OP_CONFIRM : BeastWillNet.OP_DECLINE, 0)); }

    // ---------------------------------------------------------------- text
    static String name(String will) { return Component.translatableWithFallback("nusmp.beastwill." + will + ".name", will).getString(); }

    static String abilityName(String id) { return Component.translatableWithFallback("nusmp.beastwill.ability." + id, id).getString(); }

    static String rankName(int ordinal) {
        BeastRank[] r = BeastRank.values();
        return r[Math.max(0, Math.min(r.length - 1, ordinal))].name();
    }

    static String t(String key, Object... args) { return Component.translatable("nusmp.beastwill.hud." + key, args).getString(); }

    // ---------------------------------------------------------------- the HUD
    /** The text of the panel, rebuilt when a packet lands and twice a second. */
    record Lines(long builtAt, String title, String rank, String sub, String fit, String form, String assim, String mastery, String strain, int tier,
                         List<String> unlock, List<String> abilities, String status, String keys, boolean mustWarn) {}

    private static volatile Lines lines;

    static Lines build(BeastWillNet.Sync s, BeastWillNet.Info i, long now) {
        boolean known = i != null;
        String title = name(s.active());
        String rank = known ? t("rank", rankName(i.rank())) : "";
        String sub = known ? i.beast() + "  ·  " + i.affinity() : "";
        String fit = t("fit", s.compat());
        String form = t("form", t("phase." + (s.phase() == BeastWillRules.INTEGRATE ? "integrate" : s.phase() == BeastWillRules.ACQUIRE ? "acquire" : "off")));
        int tier = s.strain() >= BeastWillRules.OVERLOAD_AT ? 3 : s.strain() >= BeastWillRules.EXHAUSTED_AT ? 2 : s.strain() >= BeastWillRules.STRAINED_AT ? 1 : 0;
        String assim = t("assimilation", s.assimilation());
        String mastery = t("mastery", s.mastery());
        String strain = t("strain", s.strain(), t("tier." + new String[]{"calm", "strained", "exhausted", "overload"}[tier]));

        List<String> unlock = new ArrayList<>(2);
        if (known) {
            boolean acq = s.assimilation() >= i.acqAssim() && s.mastery() >= i.acqMastery();
            boolean integ = s.assimilation() >= i.intAssim() && s.mastery() >= i.intMastery();
            if (!acq) unlock.add(t("unlock.acquire", i.acqAssim(), i.acqMastery()));
            else if (!integ) unlock.add(t("unlock.integrate", i.intAssim(), i.intMastery()));
            else unlock.add(t("unlock.done"));
            unlock.add(t("drain", (s.phase() == BeastWillRules.INTEGRATE ? i.intDrain() : i.acqDrain()) / 10.0));
        }
        List<String> abilities = new ArrayList<>(2);
        if (known) {
            int shown = s.phase() == BeastWillRules.OFF ? BeastWillRules.ACQUIRE : s.phase();
            for (BeastWillNet.AbilityInfo a : i.abilities()) {
                if (a.phase() != shown) continue;
                int cd = left(a.slot() == 1 ? s.cd1Sec() : s.cd2Sec());
                String key = (a.slot() == 1 ? ABILITY1 : ABILITY2).getTranslatedKeyMessage().getString();
                abilities.add(t(cd > 0 ? "ability.cd" : "ability.ready", key, abilityName(a.id()), cd > 0 ? cd : a.costPct()));
            }
        }
        List<String> flags = new ArrayList<>(4);
        if ((s.flags() & BeastWillNet.Sync.F_MEDITATING) != 0) flags.add(t("meditating"));
        if ((s.flags() & BeastWillNet.Sync.F_VEILED) != 0) flags.add(t("veiled"));
        if ((s.flags() & BeastWillNet.Sync.F_REDUCED) != 0) flags.add(t("reduced"));
        if ((s.flags() & BeastWillNet.Sync.F_CONFLICT) != 0) flags.add(t("conflict"));
        int lock = left(s.lockSec()), swap = left(s.swapSec());
        if (lock > 0) flags.add(t("lock", lock));
        if (swap > 0 && s.phase() == BeastWillRules.OFF) flags.add(t("swap", swap));
        String status = String.join("  ·  ", flags);
        String keys = t("keys", ACQUIRE.getTranslatedKeyMessage().getString(), INTEGRATE.getTranslatedKeyMessage().getString(), SWAP.getTranslatedKeyMessage().getString());
        return new Lines(now, title, rank, sub, fit, form, assim, mastery, strain, tier, unlock, abilities, status, keys, lock > 0 || tier >= 2);
    }

    private static final int W = 176;

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        BeastWillNet.Sync s = sync;
        if (s == null || s.active().isEmpty() || mc.options.hideGui || mc.player == null || mc.level == null) return;
        if (!MultiverseClientConfig.get(MultiverseClientConfig.BEASTWILL_HUD)) return;    // 3.52.0: the panel lives in the main menu now; this setting brings the old HUD back
        long now = mc.level.getGameTime();
        BeastWillNet.Info i = info();
        Lines l = lines;
        if (l == null || now - l.builtAt() >= 10 || now < l.builtAt()) lines = l = build(s, i, now);
        Font font = mc.font;
        int h = height(l);
        int x = 6, y = Math.max(4, (g.guiHeight() - h) / 2);
        GuiMove.report("beastwill_hud", x, y, W, h);                                   // GUI Move / GUI Size: where the panel is drawn (the layer hook moves it)
        paint(g, font, x, y, l, s, i);
    }

    static int height(Lines l) {
        return 8 + 11 + 11 + 11 + 3 * 15 + l.unlock().size() * 10 + l.abilities().size() * 10 + (l.status().isEmpty() ? 0 : 10) + 11 + 4;
    }

    /** The panel itself (the HUD layer and the main menu draw the same thing). */
    static void paint(GuiGraphics g, Font font, int x, int y, Lines l, BeastWillNet.Sync s, BeastWillNet.Info i) {
        int tint = i == null || i.tint() == 0 ? UiTheme.CYAN : 0xFF000000 | i.tint();
        int h = height(l);
        float time = UiTheme.now();
        float alert = l.tier() >= 3 ? 0.55f + 0.45f * UiMath.pulse(time, 8f, 0f) : l.tier() == 2 ? 0.6f : 0.25f;
        UiPanel.panel(g, x, y, W, h, l.tier() >= 2 ? UiTheme.RED : tint, alert, 0.92f);

        int cx = x + 7, cy = y + 6, iw = W - 14;
        g.drawString(font, font.plainSubstrByWidth(l.title(), iw - 40), cx, cy, tint, true);
        if (!l.rank().isEmpty()) g.drawString(font, l.rank(), x + W - 7 - font.width(l.rank()), cy, UiTheme.GOLD, false);
        cy += 11;
        g.drawString(font, font.plainSubstrByWidth(l.sub(), iw - 44), cx, cy, UiTheme.TEXT_DIM, false);
        g.drawString(font, l.fit(), x + W - 7 - font.width(l.fit()), cy, s.compat() >= 100 ? UiTheme.GREEN : UiTheme.GOLD, false);
        cy += 11;
        int formCol = s.phase() == BeastWillRules.INTEGRATE ? UiTheme.VIOLET : s.phase() == BeastWillRules.ACQUIRE ? UiTheme.GREEN : UiTheme.TEXT_DIM;
        g.drawString(font, l.form(), cx, cy, formCol, false);
        cy += 12;
        cy = bar(g, font, l.assim(), s.assimilation() / 100f, UiTheme.CYAN, cx, cy, iw, time);
        cy = bar(g, font, l.mastery(), s.mastery() / 100f, UiTheme.GOLD, cx, cy, iw, time);
        int strainCol = l.tier() >= 3 ? UiTheme.RED : l.tier() == 2 ? 0xFFFF9A5C : l.tier() == 1 ? UiTheme.GOLD : UiTheme.GREEN;
        cy = bar(g, font, l.strain(), s.strain() / 100f, strainCol, cx, cy, iw, time);
        for (String u : l.unlock()) {
            g.drawString(font, font.plainSubstrByWidth(u, iw), cx, cy, UiTheme.TEXT_DIM, false);
            cy += 10;
        }
        for (String a : l.abilities()) {
            g.drawString(font, font.plainSubstrByWidth(a, iw), cx, cy, UiTheme.TEXT, false);
            cy += 10;
        }
        if (!l.status().isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth(l.status(), iw), cx, cy, l.mustWarn() ? UiTheme.RED : UiTheme.GOLD, false);
            cy += 10;
        }
        g.drawString(font, font.plainSubstrByWidth(l.keys(), iw), cx, y + h - 12, UiTheme.TEXT_FAINT, false);
    }

    private static int bar(GuiGraphics g, Font font, String label, float frac, int rgb, int x, int y, int w, float time) {
        g.drawString(font, label, x, y, UiTheme.TEXT, false);
        UiBar.draw(g, x, y + 10, w, 3, frac, rgb, time, 1f);
        return y + 15;
    }
}
