package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.client.relics.djinn.DjinnClientState.Snap;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * What the Djinn does to the bearer's own screen, with events and the GUI only (no shader is needed): a vignette in the Djinn's colour that deepens
 * with the phase, a pulsing colour fringe at the edges and a narrowing of the field of view while an Extreme Magic gathers, a flash and a short shake
 * when it strikes, and a small bar above the hotbar: four phase pips, the Magoi (the player's magicules as the server last reported them), the time
 * left of a Lesser Summon, a Maso or a charge, the Maso lockout and the Extreme Magic cooldown, trust and assimilation.
 * The Zagred GLSL aura shader is reused for the armour (see {@link DjinnArmorLayer}); a full-screen distortion shader would need a post chain and is
 * not attempted.
 */
public final class DjinnScreenFx {
    private DjinnScreenFx() {}

    private static final String[] PHASE = {"", "Lesser Summon", "Djinn Weapon", "Full Equip", "Extreme Magic"};

    /** GUI layer (above everything): the vignette, the flash and the bar. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.level == null) return;
        Snap s = DjinnClientState.local();
        Djinn d = s.djinn();
        if (d == null) return;
        long gt = mc.level.getGameTime();
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float now = gt + partial;
        int w = g.guiWidth(), h = g.guiHeight();
        int rgb = d.glow() & 0xFFFFFF;
        // ---- vignette, fringe, flash
        float base = s.phase() >= 4 ? 0.34f : s.phase() == 3 ? 0.22f : s.phase() == 2 ? 0.10f : 0f;
        float fadeIn = Mth.clamp((now - s.since()) / 10f, 0f, 1f);
        float pulse = s.pulse(gt, partial);
        if (base > 0f) {
            float k = base * fadeIn * (0.85f + 0.15f * Mth.sin(now * (s.phase() >= 4 ? 0.5f : 0.12f)));
            vignette(g, w, h, rgb, k);
            if (s.phase() >= 4) {
                float charge = 1f - Mth.clamp(s.leftNow(gt) / (float) DjinnPhaseRules.EXTREME_CHARGE, 0f, 1f);
                int off = 2 + (int) (4 * charge * (0.5f + 0.5f * Mth.sin(now * 0.9f)));
                fringe(g, w, h, off, 0x40 + (int) (0x50 * charge));
            }
        }
        if (pulse > 0.01f) g.fill(0, 0, w, h, ((int) (110 * pulse) << 24) | 0xFFFFFF);
        // ---- the bar
        boolean show = s.phase() > 0 || s.lockNow(gt) > 0 || s.cooldownNow(gt) > 0 || now - s.since() < 60;
        if (!show) return;
        int bw = 118, x = w / 2 - bw / 2, y = h - 66;
        g.fill(x - 3, y - 12, x + bw + 3, y + 21, 0x99080810);
        g.fill(x - 3, y - 12, x + bw + 3, y - 11, 0xFF000000 | (d.color() & 0xFFFFFF));
        for (int i = 1; i <= 4; i++) {
            int px = x + (i - 1) * 8;
            g.fill(px, y - 9, px + 6, y - 3, i <= s.phase() ? 0xFF000000 | rgb : 0xFF2A2A38);
        }
        String line = s.phase() > 0 ? PHASE[Math.min(4, s.phase())] + (s.phase() == 1 || s.phase() >= 3 ? "  " + (s.leftNow(gt) + 19) / 20 + "s" : "") : d.displayName();
        g.drawString(mc.font, line, x + 36, y - 10, 0xFF000000 | rgb, true);
        g.fill(x, y, x + bw, y + 5, 0xFF1A1A28);                                              // Magoi: the magicule pool
        g.fill(x, y, x + (int) (bw * s.magoi() / 100f), y + 5, 0xFF000000 | mix(0x3A7BFF, rgb, 0.4f));
        g.fill(x, y + 7, x + bw, y + 9, 0xFF1A1A28);                                          // assimilation
        g.fill(x, y + 7, x + (int) (bw * s.assim() / 1000f), y + 9, 0xFFB060FF);
        String foot = "Trust " + s.trust() + (s.lock() > 0 && s.lockNow(gt) > 0 ? "  Maso " + (s.lockNow(gt) + 19) / 20 + "s" : "")
                + (s.cooldownNow(gt) > 0 ? "  Ext " + (s.cooldownNow(gt) + 19) / 20 + "s" : "");
        g.drawString(mc.font, foot, x, y + 11, 0xFFB8B8C8, true);
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (gg << 8) | bb;
    }

    /** Soft coloured edges: bands that fade towards the middle, stepped (a GUI fill cannot gradient sideways). */
    private static void vignette(GuiGraphics g, int w, int h, int rgb, float k) {
        int steps = 14, band = Math.max(12, Math.min(w, h) / 4);
        for (int i = 0; i < steps; i++) {
            float f = 1f - i / (float) steps;
            int a = (int) (255 * k * f * f);
            if (a <= 0) continue;
            int c = (a << 24) | rgb, t = band * i / steps, t2 = band * (i + 1) / steps;
            g.fill(0, t, w, t2, c);
            g.fill(0, h - t2, w, h - t, c);
            g.fill(t, t2, t2, h - t2, c);
            g.fill(w - t2, t2, w - t, h - t2, c);
        }
    }

    /** A red and a blue fringe on opposite edges, offset by {@code off} pixels: the look of the picture splitting. */
    private static void fringe(GuiGraphics g, int w, int h, int off, int a) {
        int c1 = (a << 24) | 0xFF2030, c2 = (a << 24) | 0x2060FF;
        g.fill(0, 0, off, h, c1);
        g.fill(w - off, 0, w, h, c2);
        g.fill(0, 0, w, off, c2);
        g.fill(0, h - off, w, h, c1);
    }

    // ================================================================ field of view and shake
    public static void onFov(ComputeFovModifierEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e.getPlayer() != mc.player || mc.level == null) return;
        Snap s = DjinnClientState.local();
        if (s.djinn() == null || s.phase() < 3) return;
        long gt = mc.level.getGameTime();
        float k = 1.0f + 0.05f * Mth.clamp((gt - s.since()) / 10f, 0f, 1f);
        if (s.phase() >= 4) k -= 0.14f * (1f - Mth.clamp(s.leftNow(gt) / (float) DjinnPhaseRules.EXTREME_CHARGE, 0f, 1f));
        k += 0.10f * s.pulse(gt, 0f);
        e.setNewFovModifier(e.getNewFovModifier() * k);
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Snap s = DjinnClientState.local();
        float p = s.pulse(mc.level.getGameTime(), (float) e.getPartialTick());
        if (p <= 0.01f) return;
        float t = (mc.level.getGameTime() + (float) e.getPartialTick());
        e.setYaw(e.getYaw() + Mth.sin(t * 2.3f) * p * 1.4f);
        e.setPitch(e.getPitch() + Mth.cos(t * 2.9f) * p * 1.1f);
        e.setRoll(e.getRoll() + Mth.sin(t * 1.7f) * p * 0.8f);
    }
}
