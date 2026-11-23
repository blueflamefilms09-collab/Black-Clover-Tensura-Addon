package com.newuniverse.nusmp.client.lords;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.lords.LordNet;
import com.newuniverse.nusmp.lords.LordRules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client half of the Lord weapons decides nothing: it keeps the state the server sends (at most once a second, only on change) and draws it, so the wielder can SEE what the weapon takes. Two thin bars per
 * weapon at the bottom left: the mastery (level and progress to the next) and the named drawback (the Burden of the four weapons, the satiation of Greed's blade), tinted by the state (strained orange, overloaded red,
 * ravenous red, half-starved dark red). The form key is Greed's (R): {@code GreedClient} forwards it here, so one key serves the sword and the four weapons; this class only ASKS the server to switch. The same state is what
 * the Tensura status menu's mastery bar reads ({@link #state}). Cost: a few fills and strings per frame, only while the local player wields at least one Lord weapon; the strings are rebuilt when the packet changes.
 */
public final class LordClient {
    private LordClient() {}

    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "lords_hud");
    private static final LordNet.Hud[] STATE = new LordNet.Hud[LordRules.Sin.values().length];
    private static final String[] LINE = new String[STATE.length];
    private static final String[] STATUS = new String[STATE.length];                  // the status-menu line of each sin, rebuilt when its packet arrives

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(LAYER, LordClient::render));
        LordNet.clientHud = LordClient::onHud;
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            for (int i = 0; i < STATE.length; i++) {
                STATE[i] = null;
                LINE[i] = null;
                STATUS[i] = null;
            }
        });
    }

    private static void onHud(LordNet.Hud h) {
        if (h.sin() < 0 || h.sin() >= STATE.length) return;
        if ((h.flags() & LordNet.Hud.F_OWNER) == 0) {
            STATE[h.sin()] = null;
            LINE[h.sin()] = null;
            STATUS[h.sin()] = null;
            return;
        }
        STATE[h.sin()] = h;
        LordRules.Sin s = LordRules.Sin.values()[h.sin()];
        STATUS[h.sin()] = Component.translatable("lords.status.line", Component.translatable(s.masteryKey()), h.level(), LordRules.MAX_LEVEL, h.pct()).getString();
        LINE[h.sin()] = Component.translatable("lords.hud.line", Component.translatable("lords.name." + s.id), h.level(), LordRules.MAX_LEVEL).getString();
    }

    /** The cached state of one sin for the wielder, or null: the Tensura status menu's mastery bar reads this. */
    public static LordNet.Hud state(LordRules.Sin s) { return STATE[s.ordinal()]; }

    /**
     * The mastery bars of the Tensura status menu (the second bar: it fills over time): one line and one thin bar per weapon the local player wields (a Lord weapon, or the Black Sword for the
     * Half-Starved Mastery), top down from (x, y), never below maxY. Returns the height used; 0 when none or when the client setting lords.statusMasteryBar is off. Cost: a few fills and strings per frame.
     */
    public static int drawMastery(GuiGraphics g, net.minecraft.client.gui.Font font, int x, int y, int w, int maxY) {
        if (!com.newuniverse.nusmp.vfx.client.LordVfxConfig.statusBar()) return 0;
        int used = 0;
        for (int i = 0; i < STATE.length; i++) {
            LordNet.Hud h = STATE[i];
            if (h == null || STATUS[i] == null) continue;
            if (used == 0) {
                if (y + 28 > maxY) return 0;
                g.drawString(font, Component.translatable("lords.status.header").getString(), x, y, 0xFFD9B35A, false);
                used = 11;
            }
            if (y + used + 11 > maxY) break;
            int rgb = 0xFF000000 | LordRules.Sin.values()[i].rgb;
            g.drawString(font, font.plainSubstrByWidth(STATUS[i], w), x, y + used, 0xFFE0E0E0, false);
            bar(g, x, y + used + 10, w, 3, h.pct(), 100, rgb, 0xAA000000);
            used += 16;
        }
        return used;
    }

    /** The compact Tensura status panel: appends one row per wielded weapon (label, level) and its bar ({percent, rgb}) to the two parallel lists. */
    public static void masteryRows(java.util.List<String[]> rows, java.util.List<int[]> bars) {
        if (!com.newuniverse.nusmp.vfx.client.LordVfxConfig.statusBar()) return;
        for (int i = 0; i < STATE.length; i++) {
            LordNet.Hud h = STATE[i];
            if (h == null) continue;
            LordRules.Sin sin = LordRules.Sin.values()[i];
            rows.add(new String[] {Component.translatable("lords.status.header").getString(), "Lv " + h.level() + "/" + LordRules.MAX_LEVEL + " " + Component.translatable("lords.name." + sin.id).getString()});
            bars.add(new int[] {h.pct(), sin.rgb});
        }
    }

    /** True when the local player wields at least one Lord weapon: GreedClient then forwards its form key. */
    public static boolean wielder() {
        for (int i = 0; i < STATE.length; i++) if (STATE[i] != null && LordRules.Sin.values()[i].isLord()) return true;
        return false;
    }

    /** The form key: asks the server for the next (or previous) open form. */
    public static void switchForm(boolean back) {
        if (wielder()) PacketDistributor.sendToServer(new LordNet.SwitchForm(back ? -2 : -1));
    }

    private static void render(GuiGraphics g, DeltaTracker delta) { SafeCast.client("lords.hud", () -> draw(g, delta)); }

    private static void draw(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int row = 0, count = 0;
        for (LordNet.Hud st : STATE) if (st != null) count++;
        if (count == 0) return;
        int[] spot = com.newuniverse.nusmp.client.hud.HudDock.take(com.newuniverse.nusmp.client.hud.HudDock.Side.LEFT, g.guiWidth(), g.guiHeight(), 82, count * 22, null);       // 5.0.0: one block in the shared left column (null = the dock is off: the old place)
        for (int i = 0; i < STATE.length; i++) {
            LordNet.Hud s = STATE[i];
            if (s == null) continue;
            LordRules.Sin sin = LordRules.Sin.values()[i];
            int x = spot == null ? 6 : spot[0], y = spot == null ? g.guiHeight() - 30 - row * 22 : spot[1] + row * 22 + 10;
            boolean strained = (s.flags() & LordNet.Hud.F_STRAINED) != 0, over = (s.flags() & LordNet.Hud.F_OVERLOAD) != 0, half = (s.flags() & LordNet.Hud.F_HALF) != 0;
            int rgb = 0xFF000000 | sin.rgb;
            g.drawString(mc.font, LINE[i] == null ? "" : LINE[i], x, y - 9, over ? 0xFFFF4040 : 0xFFE0E0E0, true);
            bar(g, x, y, 80, 3, s.pct(), 100, rgb, 0xAA000000);                                                       // the mastery: progress to the next level
            int meterColor = over ? 0xFFE02020 : strained ? 0xFFE08A20 : half ? 0xFF8A1020 : 0xFF6A5A7A;
            if (sin == LordRules.Sin.GREED) meterColor = (s.flags() & LordNet.Hud.F_OVERLOAD) != 0 ? 0xFF501018 : strained ? 0xFFE02020 : 0xFFC03040;
            bar(g, x, y + 5, 80, 3, s.meter(), 100, meterColor, 0xAA000000);                                         // the drawback: burden (or satiation)
            row++;
        }
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, int value, int max, int color, int back) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, back);
        g.fill(x, y, x + Mth.clamp(w * value / Math.max(1, max), 0, w), y + h, color);
    }
}
