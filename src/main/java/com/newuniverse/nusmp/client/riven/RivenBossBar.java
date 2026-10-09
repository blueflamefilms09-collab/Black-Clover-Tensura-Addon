package com.newuniverse.nusmp.client.riven;

import com.newuniverse.nusmp.entity.riven.RivenBossEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Marquis Remake's animated boss bar, drawn over the HUD (vanilla's bar for him is hidden, the BossEvent still exists for the locator
 * and sound ducking). Crown, MARQUIS REMAKE and a subtitle that follows the phase; a violet-to-blue fill with a travelling lightning
 * shimmer; the Black Bull skull that cracks at 70% and 30%; the skill name typing on while he casts and burning out after;
 * a story-charge pip row from phase II; a pulsing frame in phase III; HP as a number and the phase as a numeral.
 */
public final class RivenBossBar {
    private RivenBossBar() {}

    private static final String[] SKULL = {
            "..XXXXXXXX..",
            ".XXXXXXXXXX.",
            "XXXXXXXXXXXX",
            "XXXXXXXXXXXX",
            "XX..XXXX..XX",
            "XX..XXXX..XX",
            "XXXXX..XXXXX",
            ".XXXXXXXXXX.",
            "..XXXXXXXX..",
            "..X.X..X.X..",
            "..X.XXXX.X.."};
    private static final String[] NUMERALS = {"", "I", "II", "III", "IV"};

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "riven_bar"), RivenBossBar::render));
        NeoForge.EVENT_BUS.addListener((CustomizeGuiOverlayEvent.BossEventProgress e) -> {
            if (e.getBossEvent().getName().getString().startsWith("Marquis Remake")) e.setCanceled(true);
        });
    }

    private static RivenBossEntity nearest(Minecraft mc) {
        RivenBossEntity best = null;
        double bd = 64 * 64;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof RivenBossEntity r && r.isAlive()) {
                double d = r.distanceToSqr(mc.player);
                if (d < bd) { bd = d; best = r; }
            }
        }
        return best;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        RivenBossEntity boss = nearest(mc);
        if (boss == null) return;
        RivenClientState.State st = RivenClientState.get(boss.getId());
        if (st == null) return;
        float time = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
        st.hpShown += (st.hp - st.hpShown) * 0.12f;
        int phase = Mth.clamp(st.phase, 1, 4);
        Font font = mc.font;
        int sw = g.guiWidth(), cx = sw / 2, w = 260, x0 = cx - w / 2, y = 30, h = 10;

        float pulse = phase == 3 ? 0.5f + 0.5f * Mth.sin(time * 0.35f) : 0f;

        // crown
        int gold = 0xFFE8C860;
        g.fill(cx - 7, 5, cx + 7, 8, gold);
        g.fill(cx - 7, 2, cx - 5, 5, gold); g.fill(cx - 1, 1, cx + 1, 5, gold); g.fill(cx + 5, 2, cx + 7, 5, gold);
        // name and subtitle
        g.pose().pushPose();
        g.pose().translate(cx, 9, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.drawCenteredString(font, "MARQUIS REMAKE", 0, 0, 0xFFE8E0FF);
        g.pose().popPose();
        String sub = phase == 1 ? "THE BLACK BULLS' BARD" : phase == 2 ? "FICTIONAL REMAKE" : phase == 3 ? "THE RIFT" : "FINAL FORM";
        g.drawCenteredString(font, sub, cx, 22, phase >= 3 ? lerpColor(0xFFB89AFF, 0xFFFFFFFF, pulse) : 0xFFB89AFF);

        // frame and track
        int frame = lerpColor(0xFF2A1F4A, 0xFF8A6AFF, pulse);
        g.fill(x0 - 2, y - 2, x0 + w + 2, y + h + 2, frame);
        g.fill(x0 - 1, y - 1, x0 + w + 1, y + h + 1, 0xFF0A0614);
        int fillW = (int) (w * Mth.clamp(st.hpShown, 0f, 1f));
        for (int i = 0; i < fillW; i++) {                                    // violet -> blue, a column at a time
            float f = i / (float) w;
            int base = lerpColor(0xFF8A3CFF, 0xFF3AA0FF, f);
            float band = Math.abs(((i - time * 3.2f) % 90f + 90f) % 90f - 12f);   // a bright lightning band sweeping along the bar
            int col = band < 10f ? lerpColor(base, 0xFFFFFFFF, (1f - band / 10f) * 0.75f) : base;
            g.fill(x0 + i, y, x0 + i + 1, y + h, col);
        }
        g.fill(x0, y, x0 + fillW, y + 2, 0x44FFFFFF);
        for (int k = 1; k < 10; k++) g.fill(x0 + w * k / 10, y, x0 + w * k / 10 + 1, y + h, 0x88000000);   // pips

        // skull, cracked at 70% and 30%
        drawSkull(g, x0 - 30, y - 6, st.hp < 0.7f, st.hp < 0.3f, phase == 3 ? lerpColor(0xFFC8C0E8, 0xFFFFFFFF, pulse) : 0xFFC8C0E8);

        // numbers (colour-blind safe): phase numeral and HP
        g.drawString(font, NUMERALS[phase] + "  " + Math.max(0, Math.round(st.hp * 100f)) + "%", x0 + w + 6, y + 1, 0xFFE8E0FF, true);

        // the skill he is casting: types on, then burns out
        int textY = y + h + 5;
        long now = mc.level.getGameTime();
        if (!st.lastCast.isEmpty()) {
            long age = now - st.castStart;
            long since = now - st.castEnd;
            if (since < 24) {
                String full = st.lastCast;
                int chars = (int) Math.min(full.length(), Math.max(1, age * 2));
                String shown = full.substring(0, chars);
                int alpha = st.cast.isEmpty() ? (int) (255 * (1f - since / 24f)) : 255;
                int color = st.cast.isEmpty() ? lerpColor(0xFFFFB060, 0xFFFF4020, since / 24f) : 0xFFFFFFFF;
                g.drawCenteredString(font, shown, cx, textY, (alpha << 24) | (color & 0xFFFFFF));
                textY += 11;
            }
        }
        // story charge, from phase II
        if (phase >= 2) {
            int pipW = 14;
            int total = 10 * pipW + 9 * 2;
            int sx = cx - total / 2;
            for (int i = 0; i < 10; i++) {
                boolean on = st.story >= (i + 1) * 10 - 5;
                g.fill(sx + i * (pipW + 2), textY, sx + i * (pipW + 2) + pipW, textY + 3, on ? lerpColor(0xFFB08AFF, 0xFFFFFFFF, pulse * 0.6f) : 0xFF1C1430);
            }
            textY += 6;
        }
        if (!st.combatType.isEmpty() || !st.grimoire.isEmpty()) {
            String page = st.combatType + (st.grimoire.isEmpty() ? "" : " · " + st.grimoire);
            g.drawCenteredString(font, page, cx, textY + 1, 0xFFE3D6FF);
            textY += 10;
        }
        if (!st.status.isEmpty()) g.drawCenteredString(font, st.status, cx, textY + 1, 0xFFFFD68A);
        else g.drawCenteredString(font, "A Dreamer · A Fighter · A Story Still Being Written", cx, textY + 1, 0x88B89AFF);
    }

    private static void drawSkull(GuiGraphics g, int x, int y, boolean crack1, boolean crack2, int color) {
        for (int r = 0; r < SKULL.length; r++)
            for (int c = 0; c < SKULL[r].length(); c++)
                if (SKULL[r].charAt(c) == 'X') g.fill(x + c * 2 - 0, y + r * 2, x + c * 2 + 2, y + r * 2 + 2, color);
        if (crack1) { g.fill(x + 12, y, x + 14, y + 6, 0xFF0A0614); g.fill(x + 14, y + 6, x + 16, y + 10, 0xFF0A0614); }
        if (crack2) { g.fill(x + 6, y + 4, x + 8, y + 12, 0xFF0A0614); g.fill(x + 8, y + 12, x + 10, y + 16, 0xFF0A0614); g.fill(x + 16, y + 10, x + 18, y + 16, 0xFF0A0614); }
    }

    private static int lerpColor(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255), gr = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255), bl = (int) Mth.lerp(t, a & 255, b & 255);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }
}
