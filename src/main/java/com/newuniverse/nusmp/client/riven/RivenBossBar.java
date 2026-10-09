package com.newuniverse.nusmp.client.riven;

import com.newuniverse.nusmp.entity.riven.RivenBossEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Riven Remake's animated boss bar, drawn under the vanilla bar (which stays for the boss locator and sound ducking). Crown, RIVEN REMAKE, a phase
 * subtitle (THE BLACK BULLS' BARD / FICTIONAL REMAKE / FINAL FORM), a violet-to-blue fill with a travelling shimmer, a Black Bull skull that cracks at
 * 70% and 30%, HP as a number and the phase as a roman numeral (colour-blind safe), a story-charge pip row from phase 2, the typed-on skill name,
 * a pulse in phase 3, and the footer "Fictional Remake". Hidden when the player is far from him. Reads the synced entity fields, interpolating the fill.
 */
public final class RivenBossBar {
    private RivenBossBar() {}

    private static final String[] SUBTITLE = {"THE BLACK BULLS' BARD", "FICTIONAL REMAKE", "FINAL FORM"};
    private static final String[] ROMAN = {"I", "II", "III"};
    private static float shown = 1f;

    public static void register(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "riven_boss_bar"), RivenBossBar::draw);
    }

    private static RivenBossEntity find(Minecraft mc) {
        RivenBossEntity best = null;
        double bd = 48 * 48;
        for (var en : mc.level.entitiesForRendering()) {
            if (en instanceof RivenBossEntity r && r.isAlive()) {
                double d = r.distanceToSqr(mc.player);
                if (d < bd) { bd = d; best = r; }
            }
        }
        return best;
    }

    private static void draw(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        RivenBossEntity r = find(mc);
        if (r == null) return;
        float time = (r.tickCount + dt.getGameTimeDeltaPartialTick(false));
        int ph = Mth.clamp(r.clientPhase(), 1, 3);
        float frac = Mth.clamp(r.getHealth() / r.getMaxHealth(), 0f, 1f);
        shown += (frac - shown) * 0.15f;
        int w = 256, x = (g.guiWidth() - w) / 2, y = 44, h = 9;
        float pulse = ph == 3 ? 0.75f + 0.25f * Mth.sin(time * 0.35f) : 1f;
        int a = (int) (255 * pulse);

        var font = mc.font;
        g.drawString(font, "\u265b", x, y - 22, 0xFFC9B8FF, true);                                    // the small crown
        g.pose().pushPose();
        g.pose().translate(x + 12, y - 24, 0);
        g.pose().scale(1.3f, 1.3f, 1f);
        g.drawString(font, "RIVEN REMAKE", 0, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
        g.drawString(font, SUBTITLE[ph - 1], x + 12, y - 12, 0xFF9C8CFF, true);
        g.drawString(font, ROMAN[ph - 1], x + w - font.width(ROMAN[ph - 1]), y - 22, 0xFFFFFFFF, true);

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF0B0714);
        g.fill(x, y, x + w, y + h, 0xFF1A1230);
        int fw = Math.round(w * shown);
        for (int i = 0; i < fw; i += 2) {                                                             // violet -> blue
            float k = i / (float) w;
            int rr = (int) Mth.lerp(k, 0x7A, 0x3C), gg = (int) Mth.lerp(k, 0x3C, 0x8C), bb = 0xFF;
            g.fill(x + i, y, Math.min(x + i + 2, x + fw), y + h, (a << 24) | (rr << 16) | (gg << 8) | bb);
        }
        int sx = (int) ((time * 3f) % (w + 30)) - 15;                                                 // the lightning shimmer, masked by the fill
        for (int i = 0; i < 14; i++) {
            int px = sx + i;
            if (px < 0 || px >= fw) continue;
            int al = (int) (150 * (1f - Math.abs(i - 7) / 7f));
            g.fill(x + px, y, x + px + 1, y + h, (al << 24) | 0xE8E0FF);
        }
        for (int i = 1; i < 10; i++) g.fill(x + w * i / 10, y, x + w * i / 10 + 1, y + h, 0x60000000);

        drawSkull(g, x - 14, y - 2, frac < 0.3f ? 2 : frac < 0.7f ? 1 : 0);

        String hp = (int) Math.ceil(r.getHealth()) + " / " + (int) r.getMaxHealth();
        g.drawString(font, hp, x + w - font.width(hp), y + h + 3, 0xFFD8D0FF, true);
        g.drawString(font, "Fictional Remake", x + 12, y + h + 14, 0xFF6F5FB5, false);                // the footer mark

        if (ph >= 2) {                                                                                // story charge: ten pips
            int pips = Mth.clamp(r.clientCharge() / 10, 0, 10);
            for (int i = 0; i < 10; i++) g.fill(x + 12 + i * 8, y + h + 3, x + 18 + i * 8, y + h + 5, i < pips ? 0xFFB69CFF : 0xFF2A2145);
        }
        String skill = r.clientSkill();
        if (!skill.isEmpty() && r.clientClipAge() < 70) {                                             // the skill name types on, then burns out
            int chars = Math.min(skill.length(), r.clientClipAge() * 2);
            int al = r.clientClipAge() < 50 ? 255 : (int) (255 * (70 - r.clientClipAge()) / 20f);
            g.drawCenteredString(font, skill.substring(0, chars), g.guiWidth() / 2, y + h + 26, (al << 24) | 0xFFC9B8);
        }
    }

    /** A 7 x 8 Black Bull skull in pixels; crack level 1 (70%) and 2 (30%) add dark fissures. */
    private static void drawSkull(GuiGraphics g, int x, int y, int cracks) {
        int c = 0xFFE8E4F5;
        g.fill(x + 1, y, x + 6, y + 1, c);
        g.fill(x, y + 1, x + 7, y + 5, c);
        g.fill(x + 1, y + 5, x + 6, y + 8, c);
        g.fill(x + 1, y + 2, x + 3, y + 4, 0xFF14101F);
        g.fill(x + 4, y + 2, x + 6, y + 4, 0xFF14101F);
        g.fill(x - 1, y - 1, x + 1, y + 1, c);                                                        // the horns
        g.fill(x + 6, y - 1, x + 8, y + 1, c);
        if (cracks >= 1) g.fill(x + 3, y, x + 4, y + 3, 0xFF14101F);
        if (cracks >= 2) { g.fill(x + 2, y + 4, x + 3, y + 8, 0xFF14101F); g.fill(x + 5, y + 4, x + 6, y + 6, 0xFF14101F); }
    }
}
