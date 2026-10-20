package com.newuniverse.nusmp.client.gui.bossbar;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.boss.bars.BarLayoutMath;
import com.newuniverse.nusmp.boss.bars.BarStyle;
import com.newuniverse.nusmp.boss.bar.BossBarColors;
import com.newuniverse.nusmp.boss.bars.BossBarState;
import com.newuniverse.nusmp.boss.bars.PhaseMarkers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Why: draws one custom boss bar. A bar is the atlas of its style (the dark back layer with the name banner and the socket disc, the frame with its ornaments, the glow layer drawn additively),
 * the fill built from gradients and a scrolling effect tile, and the live parts on top: the damage-lag chip, the shield band, the phase notches, the chains of an invulnerable boss, the wind-up
 * bar, the lair action ring and the small texts. About twenty quads and a handful of strings per bar, no object built per frame by this class (every label is a cached {@link CustomBarState.Txt},
 * every location is a static or lives in {@link BarStyles.Loaded}). The numbers of the layout are {@link BarLayoutMath}'s; {@code tools/boss_bar_preview.py} draws the same bar from the same
 * files for the contact sheets. A shatter (the death of the boss) draws the back, frame and glow layers in twelve strips and the fill clipped to each.
 */
final class CustomBarPainter {
    private CustomBarPainter() {}

    private static final int WHITE = 0xFFFFFF, W = BarLayoutMath.WIDTH, H = BarLayoutMath.FRAME_H;
    private static final int WX = BarLayoutMath.WIN_X, WY = BarLayoutMath.WIN_Y, WW = BarLayoutMath.WIN_W, WH = BarLayoutMath.WIN_H;
    private static final int STRIPS = 12;
    private static final float[] RING_COS = new float[12], RING_SIN = new float[12];
    private static final Vector3f SCRATCH = new Vector3f();
    private static final Quaternionf FLIP = new Quaternionf().rotateZ((float) Math.PI);
    private static boolean portraitFailed;

    static {
        for (int i = 0; i < 12; i++) {
            RING_COS[i] = (float) Math.cos(Math.PI * 2 * i / 12 - Math.PI / 2);
            RING_SIN[i] = (float) Math.sin(Math.PI * 2 * i / 12 - Math.PI / 2);
        }
    }

    // ================================================================ one bar
    /**
     * Draws the bar whose widget starts at ({@code ox}, {@code oy}) with the boss at health {@code hp}. {@code eventName} is the name of the vanilla boss event (used when the state names nobody).
     * {@code compact} = no under-row, smaller texts (three or more bars).
     */
    static void paint(GuiGraphics g, Font font, Minecraft mc, CustomBarState st, Component eventName, float hp, int ox, int oy, double time, long now, boolean compact, boolean motion) {
        BarStyles.Loaded ls = st.style;
        BarStyle bs = ls.style;
        BossBarState s = st.s;

        // ---- clocks
        if (Double.isNaN(st.enterTime)) {
            st.enterTime = time;
            st.ghost.reset(hp);
            st.lastHp = hp;
        }
        if (Double.isNaN(st.phaseFlashTime)) st.phaseFlashTime = time;
        if (st.dying && Double.isNaN(st.deathTime)) st.deathTime = time;
        st.ghost.update(time, hp);
        st.dps.tick(now, hp);
        float drop = st.lastHp - hp;
        if (drop > 0.04f && motion) st.shakeStart = time;
        st.lastHp = hp;

        float ek = BarLayoutMath.entrance(time - st.enterTime);
        float dk = st.dying ? (float) Math.min(1.0, (time - st.deathTime) / BarLayoutMath.DEATH_TICKS) : 0f;
        float alpha = 1f, dy = 0f, dx = 0f;
        switch (ls.entrance) {
            case 0 -> { dy = -(1f - ek) * 34f; alpha = Math.min(1f, ek * 1.6f); }
            case 1 -> { dy = (1f - ek) * 16f; alpha = ek; }
            case 2 -> alpha = ek;
            default -> alpha = Math.min(1f, ek * 2.2f);
        }
        float flash = ls.entrance == 3 ? 1f - ek : 0f;
        boolean shatter = st.dying && ls.death == 0;
        if (st.dying) {
            if (ls.death == 1) { alpha *= 1f - dk; dy -= dk * 8f; }
            else alpha *= 1f - dk * dk;
        }
        double sk = time - st.shakeStart;
        if (motion && sk >= 0 && sk < 8) {
            float amp = 2.2f * (1f - (float) sk / 8f);
            dx += amp * (float) Math.sin(sk * 2.7);
            dy += amp * 0.6f * (float) Math.cos(sk * 3.3);
        }
        if (alpha <= 0.01f) return;

        // ---- the state's texts (rebuilt only when the state changed)
        updateTexts(font, st, s, eventName, hp, now, time);

        g.pose().pushPose();
        g.pose().translate(ox + dx, oy + dy, 0f);
        boolean depth = false;
        try {
            RenderSystem.enableBlend();
            if (shatter) {
                paintShattered(g, font, st, ls, bs, s, hp, time, alpha, dk);
            } else {
                depth = paintWhole(g, font, mc, st, ls, bs, s, hp, time, now, alpha, flash, compact, motion);
            }
        } finally {
            g.setColor(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
            g.pose().popPose();
        }
        if (depth) RenderSystem.clear(256, Minecraft.ON_OSX);                 // the live portrait left depth values behind; later GUI layers draw at z 0
    }

    // ================================================================ texts
    private static void updateTexts(Font font, CustomBarState st, BossBarState s, Component eventName, float hp, long now, double time) {
        if (!s.name().isEmpty()) st.name.set(font, CustomBarState.resolve(s.name()), s.name());
        else st.name.set(font, eventName, eventName);
        st.title.set(font, CustomBarState.resolve(s.title()), s.title());
        if (s.phaseCount() > 1) {
            int ph = s.phase();
            if (st.roman.differs(ph)) st.roman.setNum(font, Component.literal(PhaseMarkers.roman(ph)), ph);
            if (st.phaseLine.differs(ph * 1000L + (s.phaseName().hashCode() & 0x3FF)))
                st.phaseLine.setNum(font, s.phaseName().isEmpty() ? Component.translatableWithFallback("nusmp.bossbar.phase", "Phase %s", PhaseMarkers.roman(ph))
                        : Component.translatableWithFallback("nusmp.bossbar.phase_named", "Phase %s: %s", PhaseMarkers.roman(ph), CustomBarState.resolve(s.phaseName())), ph * 1000L + (s.phaseName().hashCode() & 0x3FF));
        }
        int pc = BarLayoutMath.percent(hp);
        if (st.pct.differs(pc)) st.pct.setNum(font, Component.literal(pc + "%"), pc);
        if (s.adds() >= 0 && st.adds.differs(s.adds())) st.adds.setNum(font, Component.translatableWithFallback("nusmp.bossbar.adds", "Adds %s", s.adds()), s.adds());
        if (s.fighters() > 0 && st.fighters.differs(s.fighters()))
            st.fighters.setNum(font, s.fighters() == 1 ? Component.translatableWithFallback("nusmp.bossbar.fighter", "1 fighter") : Component.translatableWithFallback("nusmp.bossbar.fighters", "%s fighters", s.fighters()), s.fighters());
        if (s.casting()) st.cast.set(font, CustomBarState.resolve(s.castName()), s.castName());
        if (s.invulnerable() && !st.invulnerable.isSet()) st.invulnerable.set(font, Component.translatableWithFallback("nusmp.bossbar.invulnerable", "Invulnerable"), BarStyles.class);
        if (s.enraged() && !st.enraged.isSet()) st.enraged.set(font, Component.translatableWithFallback("nusmp.bossbar.enraged", "Enraged"), BarStyles.class);
        if (BossBarOptions.showHp() && st.portrait != null && !st.portrait.isRemoved()) {
            int cur = Math.round(st.portrait.getHealth()), max = Math.round(st.portrait.getMaxHealth());
            long k = cur * 1000003L + max;
            if (st.hp.differs(k)) st.hp.setNum(font, Component.literal(cur + " / " + max), k);
        }
        if (BossBarOptions.showDps()) {
            int d = Math.round(st.dps.percentPerSecond());
            if (st.dpsText.differs(d)) st.dpsText.setNum(font, Component.translatableWithFallback("nusmp.bossbar.dps", "-%s%%/s", d), d);
        }
    }

    // ================================================================ the whole bar
    /** Returns true when a live portrait was drawn (the caller clears the depth buffer afterwards). */
    private static boolean paintWhole(GuiGraphics g, Font font, Minecraft mc, CustomBarState st, BarStyles.Loaded ls, BarStyle bs, BossBarState s, float hp, double time, long now, float alpha,
                                      float flash, boolean compact, boolean motion) {
        ResourceLocation atlas = ls.atlas;
        boolean invul = s.invulnerable();
        boolean rage = s.enraged();
        // back layer
        tint(g, WHITE, alpha);
        layer(g, atlas, 0, 0, W, 0f, 0f);
        // socket: the boss itself if it is near, else the emblem
        boolean live = false;
        LivingEntity e = mc.level != null && (BossBarOptions.portrait() || BossBarOptions.showHp()) && !st.preview ? st.entity(mc.level, now) : null;
        if (e != null && BossBarOptions.portrait() && !portraitFailed && !compact) live = portrait(g, e, BarLayoutMath.SOCK_X, BarLayoutMath.SOCK_Y, BarLayoutMath.SOCK_R - 1);
        if (!live) {
            tint(g, WHITE, alpha);
            g.blit(BarStyles.EMBLEMS, BarLayoutMath.SOCK_X - 13, BarLayoutMath.SOCK_Y - 13, 26, 26, (float) ls.emblemU, (float) ls.emblemV, 32, 32, 256, 160);
        } else {
            g.pose().translate(0f, 0f, 150f);                                   // everything from here on is in front of the entity's depth
            tint(g, WHITE, alpha);
            g.blit(BarStyles.EMBLEMS, BarLayoutMath.SOCK_X + 4, BarLayoutMath.SOCK_Y + 4, 11, 11, (float) ls.emblemU, (float) ls.emblemV, 32, 32, 256, 160);   // the emblem as a badge
        }
        // the fill
        int fw = BarLayoutMath.fillPixels(hp);
        int gw = BarLayoutMath.fillPixels(st.ghost.ghost());
        fill(g, st, bs, fw, gw, 0, W, time, alpha, invul, rage, s);
        // frame and glow
        tint(g, bs.color("frame"), alpha);
        layer(g, atlas, 1, 0, W, 0f, 0f);
        markers(g, st, bs, s, hp, alpha);
        glow(g, atlas, bs, st, s, hp, time, alpha, flash, invul, rage, 0, W, 0f, 0f);
        g.setColor(1f, 1f, 1f, 1f);
        // texts
        plate(g, font, st, bs, s, hp, alpha, compact, invul, rage, time);
        if (!compact) underRow(g, font, st, bs, s, time, now, alpha, invul);
        return live;
    }

    // ---------------------------------------------------------------- layers of the atlas
    /** Blits columns [x0, x1) of layer {@code layer} of the atlas to the same place (plus an offset), at 2x source resolution. */
    private static void layer(GuiGraphics g, ResourceLocation atlas, int layer, int x0, int x1, float dx, float dy) {
        g.blit(atlas, Math.round(x0 + dx), Math.round(dy), x1 - x0, H, (float) (x0 * 2), (float) BarLayoutMath.LAYER_Y[layer], (x1 - x0) * 2, BarLayoutMath.LAYER_H, BarLayoutMath.ATLAS_W, BarLayoutMath.ATLAS_H);
    }

    private static void glow(GuiGraphics g, ResourceLocation atlas, BarStyle bs, CustomBarState st, BossBarState s, float hp, double time, float alpha, float flash, boolean invul, boolean rage,
                             int x0, int x1, float dx, float dy) {
        int rgb = rage ? BossBarColors.lerp(bs.color("glow"), 0xFF4A24, 0.7f) : invul ? bs.color("invulnerable") : bs.color("glow");
        float pulse = BarLayoutMath.glowPulse(time, rage);
        if (invul) pulse = 0.55f + 0.45f * (float) Math.sin(time * 0.5);                    // the cinematic pulse of a phase shift
        double phaseAge = time - st.phaseFlashTime;
        float phaseFlash = phaseAge >= 0 && phaseAge < 24 ? 1f - (float) phaseAge / 24f : 0f;
        float low = BarLayoutMath.lowPulse(hp, time);
        float a = Math.min(1f, (0.8f * pulse + flash + phaseFlash * 0.9f + low * 0.6f)) * alpha;
        if (low > 0f) rgb = BossBarColors.lerp(rgb, 0xFF2A2A, Math.min(1f, low * 1.4f));
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        tint(g, rgb, a);
        layer(g, atlas, 2, x0, x1, dx, dy);
        RenderSystem.defaultBlendFunc();
    }

    // ================================================================ the fill
    private static void fill(GuiGraphics g, CustomBarState st, BarStyle bs, int fw, int gw, int clipX0, int clipX1, double time, float alpha, boolean invul, boolean rage, BossBarState s) {
        int x = WX, y = WY;
        int top = bs.color("fill_top"), bot = bs.color("fill_bottom");
        if (invul) {
            top = BossBarColors.lerp(top, bs.color("invulnerable"), 0.55f);
            bot = BossBarColors.lerp(bot, bs.color("invulnerable"), 0.55f);
        }
        // the damage-lag chip: where the health was, draining slower
        if (gw > fw) rectGradient(g, x + fw, y, x + gw, y + WH, clipX0, clipX1, argb(WHITE, 0.85f * alpha), argb(bs.color("glow"), 0.85f * alpha));
        if (fw > 0) {
            rectGradient(g, x, y, x + fw, y + WH, clipX0, clipX1, argb(top, alpha), argb(bot, alpha));
            // the effect tile, scrolling, and a slower white shimmer on top of it
            int row = bs.fxIndex();
            float speed = (rage ? 1.7f : 1f) * 0.9f;
            scrollFx(g, row, x, y, fw, WH, clipX0, clipX1, -(int) Math.floor(time * speed), bs.color("glow"), 0.55f * alpha);
            scrollFx(g, 0, x, y, fw, WH, clipX0, clipX1, (int) Math.floor(time * 0.5 * bs.shimmerSpeed()) + 17, WHITE, bs.shimmerStrength() * 0.6f * alpha);
            rect(g, x, y, x + fw, y + 1, clipX0, clipX1, argb(WHITE, 0.35f * alpha));
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            rect(g, x + fw - 2, y, x + fw, y + WH, clipX0, clipX1, argb(bs.color("glow"), 0.7f * alpha));
            RenderSystem.defaultBlendFunc();
            if (s.healing()) {                                                  // regeneration: a green wash that pulses along the fill
                float p = 0.25f + 0.2f * (float) Math.sin(time * 0.4);
                rect(g, x, y, x + fw, y + WH, clipX0, clipX1, argb(0x55FF66, p * alpha));
            }
        }
        // the shield: a pale band over the lower part of the window, as wide as the absorption
        if (s.shield() > 0) {
            int sw = Math.round(Math.min(1f, s.shield() / 100f) * WW);
            int sc = bs.color("shield");
            rect(g, x, y + WH - 4, x + sw, y + WH, clipX0, clipX1, argb(sc, 0.55f * alpha));
            scrollFx(g, 12, x, y, sw, WH, clipX0, clipX1, -(int) Math.floor(time * 0.2), sc, 0.5f * alpha);
            rect(g, x, y + WH - 4, x + sw, y + WH - 3, clipX0, clipX1, argb(WHITE, 0.8f * alpha));
        }
        if (invul) {                                                            // the boss cannot be hurt: an ice-blue veil and chains across the fill
            rect(g, x, y, x + WW, y + WH, clipX0, clipX1, argb(bs.color("invulnerable"), 0.32f * alpha));
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            tint(g, bs.color("invulnerable"), 0.9f * alpha);
            int off = (int) Math.floor(time * 0.6);
            scrollTile(g, BarStyles.CHAINS, 16, x, y + WH / 2 - 8, WW, 0, 16, clipX0, clipX1, off);
            RenderSystem.defaultBlendFunc();
        }
        float low = BarLayoutMath.lowPulse(hpOf(st), time);
        if (low > 0f) rect(g, x, y, x + WW, y + WH, clipX0, clipX1, argb(0xFF2020, 0.28f * low * alpha));
    }

    private static float hpOf(CustomBarState st) { return st.lastHp; }

    /** The phase notches (after the frame: the triangles sit in the bezel above the window). */
    private static void markers(GuiGraphics g, CustomBarState st, BarStyle bs, BossBarState s, float hp, float alpha) {
        if (s.phaseCount() <= 1 || !BossBarOptions.showPhaseMarkers()) return;
        int mc = bs.color("marker");
        for (int t : s.thresholds()) {
            int mx = WX + PhaseMarkers.markerX(t);
            boolean passed = PhaseMarkers.passed(hp, t);
            float a = (passed ? 0.4f : 1f) * alpha;
            g.fill(mx, WY, mx + 1, WY + WH, argb(0x000000, 0.85f * alpha));
            g.fill(mx + 1, WY, mx + 2, WY + WH, argb(mc, a * 0.85f));
            g.fill(mx - 1, WY - 3, mx + 3, WY - 2, argb(mc, a));
            g.fill(mx, WY - 2, mx + 2, WY - 1, argb(mc, a));
        }
    }

    // ---------------------------------------------------------------- fill helpers with a clip range (for the strips of a shatter)
    private static void rect(GuiGraphics g, int x0, int y0, int x1, int y1, int cx0, int cx1, int argb) {
        int a = Math.max(x0, cx0), b = Math.min(x1, cx1);
        if (b > a && y1 > y0) g.fill(a, y0, b, y1, argb);
    }

    private static void rectGradient(GuiGraphics g, int x0, int y0, int x1, int y1, int cx0, int cx1, int top, int bottom) {
        int a = Math.max(x0, cx0), b = Math.min(x1, cx1);
        if (b > a && y1 > y0) g.fillGradient(a, y0, b, y1, top, bottom);
    }

    /** Draws the effect tile of row {@code row} of fillfx.png over [x, x+w) (additive, tinted), scrolled by {@code offset}, clipped to [cx0, cx1). */
    private static void scrollFx(GuiGraphics g, int row, int x, int y, int w, int h, int cx0, int cx1, int offset, int rgb, float alpha) {
        if (w <= 0 || alpha <= 0.01f) return;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        tint(g, rgb, alpha);
        scrollTile(g, BarStyles.FILLFX, 208, x, y, w, row * BarLayoutMath.FX_ROW_H + 3, h, cx0, cx1, offset);
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** Tiles a 64-wide texture over [x, x+w) starting at u = offset, clipped to [cx0, cx1): at most a few quads. {@code texH} is the texture's height, {@code v} the first row to draw. */
    private static void scrollTile(GuiGraphics g, ResourceLocation tex, int texH, int x, int y, int w, int v, int h, int cx0, int cx1, int offset) {
        int from = Math.max(x, cx0), to = Math.min(x + w, cx1);
        int u = Math.floorMod(offset + (from - x), BarLayoutMath.FX_W);
        int drawn = from;
        while (drawn < to) {
            int seg = Math.min(BarLayoutMath.FX_W - u, to - drawn);
            g.blit(tex, drawn, y, (float) u, (float) v, seg, h, BarLayoutMath.FX_W, texH);
            drawn += seg;
            u = 0;
        }
    }

    // ================================================================ texts
    private static void plate(GuiGraphics g, Font font, CustomBarState st, BarStyle bs, BossBarState s, float hp, float alpha, boolean compact, boolean invul, boolean rage, double time) {
        int cx = BarLayoutMath.PLATE_X + BarLayoutMath.PLATE_W / 2;
        int nameRgb = bs.color("name");
        int ny = compact ? 8 : 3;
        draw(g, font, st.name.seq, cx - st.name.w / 2f, ny, 1f, nameRgb, alpha);
        if (!compact) {
            if (invul) {
                float blink = 0.65f + 0.35f * (float) Math.sin(time * 0.35);
                draw(g, font, st.invulnerable.seq, cx - st.invulnerable.w * 0.7f / 2f, 12.5f, 0.7f, bs.color("invulnerable"), alpha * blink);
            } else if (st.title.w > 0) {
                draw(g, font, st.title.seq, cx - st.title.w * 0.7f / 2f, 12.5f, 0.7f, bs.color("title"), alpha);
            }
        }
        if (s.phaseCount() > 1 && BossBarOptions.showPhaseMarkers()) draw(g, font, st.roman.seq, BarLayoutMath.PLATE_X + 5, compact ? 8 : 5, 1f, bs.color("marker"), alpha);
        int right = BarLayoutMath.PLATE_X + BarLayoutMath.PLATE_W - 5;
        draw(g, font, st.pct.seq, right - st.pct.w, compact ? 8 : 5, 1f, WHITE, alpha);
        if (rage && !compact) draw(g, font, st.enraged.seq, BarLayoutMath.PLATE_X + 5, 12.5f, 0.7f, 0xFF6040, alpha);
        if (!compact && BossBarOptions.showHp() && st.hp.w > 0) draw(g, font, st.hp.seq, right - st.hp.w * 0.7f, 12.5f, 0.7f, 0xE6E6E6, alpha);
    }

    private static void underRow(GuiGraphics g, Font font, CustomBarState st, BarStyle bs, BossBarState s, double time, long now, float alpha, boolean invul) {
        int y = BarLayoutMath.ROW_Y;
        int x0 = WX, x1 = WX + WW;
        boolean busy = false;
        if (s.casting()) {                                                     // the wind-up of the boss's current move
            int cw = Math.round(WW * Math.max(0, Math.min(100, s.castPercent())) / 100f);
            g.fill(x0, BarLayoutMath.CAST_Y, x1, BarLayoutMath.CAST_Y + BarLayoutMath.CAST_H, argb(0x000000, 0.7f * alpha));
            g.fillGradient(x0, BarLayoutMath.CAST_Y, x0 + cw, BarLayoutMath.CAST_Y + BarLayoutMath.CAST_H, argb(BossBarColors.lerp(bs.color("cast"), WHITE, 0.5f), alpha), argb(bs.color("cast"), alpha));
            draw(g, font, st.cast.seq, (x0 + x1) / 2f - st.cast.w * 0.7f / 2f, y, 0.7f, bs.color("cast"), alpha);
            busy = true;
        } else if (s.lairPending()) {
            long left = lairLeft(st, s, now);
            if (left >= 0 && left <= BossBarState.LAIR_WARNING_TICKS) {       // the lair action about to fire: its name flashes, a ring counts the time down
                int tenths = (int) (left / 2);
                if (st.lair.differs(tenths + (s.lairId().hashCode() & 0xFFFF) * 1000L)) {
                    Component name = Component.translatableWithFallback("nusmp.bossbar.lair." + s.lairId(), CustomBarState.humanize(s.lairId()));
                    st.lair.setNum(font, Component.translatableWithFallback("nusmp.bossbar.lair_warning", "%s  %ss", name, String.format("%.1f", left / 20.0)),
                            tenths + (s.lairId().hashCode() & 0xFFFF) * 1000L);
                }
                float blink = 0.55f + 0.45f * (float) Math.sin(time * 0.9);
                float tx = (x0 + x1) / 2f - st.lair.w * 0.7f / 2f;
                draw(g, font, st.lair.seq, tx, y, 0.7f, bs.color("lair"), alpha * blink);
                ring(g, Math.round(tx) - 8, y + 3, left, s.lairTotal(), bs.color("lair"), alpha);
                busy = true;
            }
        }
        if (s.phaseCount() > 1 && !busy) draw(g, font, st.phaseLine.seq, x0, y, 0.7f, bs.color("title"), alpha);
        float rx = x1;
        if (s.fighters() > 0) {
            rx -= st.fighters.w * 0.7f;
            draw(g, font, st.fighters.seq, rx, y, 0.7f, 0xE6E6E6, alpha);
            rx -= 8;
        }
        if (s.adds() >= 0) {
            rx -= st.adds.w * 0.7f;
            draw(g, font, st.adds.seq, rx, y, 0.7f, 0xFFD27A, alpha);
            rx -= 8;
        }
        if (BossBarOptions.showDps() && st.dps.percentPerSecond() >= 0.5f) {
            rx -= st.dpsText.w * 0.7f;
            draw(g, font, st.dpsText.seq, rx, y, 0.7f, 0xFF8A8A, alpha);
        }
    }

    /** Ticks until the lair action fires (a preview loops its countdown so the warning can be looked at). */
    private static long lairLeft(CustomBarState st, BossBarState s, long now) {
        long elapsed = Math.max(0, now - st.arrivedAt);
        if (st.preview) return Math.floorMod(s.lairTicks() - elapsed, 200L);
        return s.lairTicks() - elapsed;
    }

    /** A ring of twelve dots, as many lit as the time left says. */
    private static void ring(GuiGraphics g, int cx, int cy, long left, int total, int rgb, float alpha) {
        int lit = BarLayoutMath.ringDots(12, (int) left, total);
        for (int i = 0; i < 12; i++) {
            int px = cx + Math.round(RING_COS[i] * 4f), py = cy + Math.round(RING_SIN[i] * 4f);
            g.fill(px, py, px + 1, py + 1, argb(rgb, (i < lit ? 1f : 0.25f) * alpha));
        }
    }

    /** A text at (x, y) of the widget at {@code scale}, in an rgb colour with an opacity, with the game's shadow. */
    private static void draw(GuiGraphics g, Font font, FormattedCharSequence seq, float x, float y, float scale, int rgb, float alpha) {
        int a = Math.max(5, Math.min(255, Math.round(alpha * 255f)));
        int color = (a << 24) | (rgb & 0xFFFFFF);
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        if (scale != 1f) g.pose().scale(scale, scale, 1f);
        g.drawString(font, seq, 0, 0, color, true);
        g.pose().popPose();
    }

    // ================================================================ the live portrait
    /** Draws the boss inside the round socket (clipped to its box) looking at the viewer; false when it could not be drawn (the emblem is drawn instead). */
    private static boolean portrait(GuiGraphics g, LivingEntity e, int cx, int cy, int r) {
        float h = Math.max(0.3f, e.getBbHeight()), w = Math.max(0.3f, e.getBbWidth());
        float shown = Math.min(h, Math.max(1.5f, Math.max(w * 1.3f, h * 0.45f)));        // the top of the body: the whole of a small boss, the head and shoulders of a giant
        float scale = (2f * r - 2f) / shown;
        float vecY = h - shown / 2f;                                                      // where the middle of what is shown sits (feet = 0)
        try {
            Matrix4f m = g.pose().last().pose();
            m.transformPosition(SCRATCH.set(cx - r, cy - r, 0f));
            float ax = SCRATCH.x, ay = SCRATCH.y;
            m.transformPosition(SCRATCH.set(cx + r, cy + r, 0f));
            g.enableScissor((int) Math.floor(Math.min(ax, SCRATCH.x)), (int) Math.floor(Math.min(ay, SCRATCH.y)), (int) Math.ceil(Math.max(ax, SCRATCH.x)), (int) Math.ceil(Math.max(ay, SCRATCH.y)));
            float yb = e.yBodyRot, yr = e.getYRot(), xr = e.getXRot(), hr0 = e.yHeadRotO, hr = e.yHeadRot;
            e.yBodyRot = 180f;
            e.setYRot(180f);
            e.setXRot(0f);
            e.yHeadRot = 180f;
            e.yHeadRotO = 180f;
            try {
                InventoryScreen.renderEntityInInventory(g, cx, cy, scale, SCRATCH.set(0f, vecY, 0f), FLIP, null, e);
            } finally {
                e.yBodyRot = yb;
                e.setYRot(yr);
                e.setXRot(xr);
                e.yHeadRotO = hr0;
                e.yHeadRot = hr;
                g.disableScissor();
            }
            return true;
        } catch (RuntimeException | LinkageError ex) {
            portraitFailed = true;
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] the boss bar portrait is off: drawing the boss failed", ex);
            return false;
        }
    }

    // ================================================================ the shatter of a dying bar
    private static void paintShattered(GuiGraphics g, Font font, CustomBarState st, BarStyles.Loaded ls, BarStyle bs, BossBarState s, float hp, double time, float alpha, float k) {
        ResourceLocation atlas = ls.atlas;
        long seed = st.id.getLeastSignificantBits() ^ st.id.getMostSignificantBits();
        int fw = BarLayoutMath.fillPixels(hp);
        int gw = BarLayoutMath.fillPixels(st.ghost.ghost());
        for (int c = 0; c < STRIPS; c++) {
            int x0 = W * c / STRIPS, x1 = W * (c + 1) / STRIPS;
            float dx = BarLayoutMath.shatterDx(c, STRIPS, k, seed), dy = BarLayoutMath.shatterDy(c, k, seed);
            g.pose().pushPose();
            g.pose().translate(dx, dy, 0f);
            tint(g, WHITE, alpha);
            layer(g, atlas, 0, x0, x1, 0f, 0f);
            fill(g, st, bs, fw, gw, x0, x1, time, alpha, false, false, s);
            tint(g, bs.color("frame"), alpha);
            layer(g, atlas, 1, x0, x1, 0f, 0f);
            glow(g, atlas, bs, st, s, hp, time, alpha, 1f - k, false, false, x0, x1, 0f, 0f);
            g.pose().popPose();
        }
        g.setColor(1f, 1f, 1f, 1f);
        draw(g, font, st.name.seq, BarLayoutMath.PLATE_X + BarLayoutMath.PLATE_W / 2f - st.name.w / 2f, 3 - k * 10f, 1f, bs.color("name"), alpha);
    }

    // ================================================================ colour helpers
    private static void tint(GuiGraphics g, int rgb, float alpha) {
        g.setColor(BossBarColors.red(rgb), BossBarColors.green(rgb), BossBarColors.blue(rgb), Math.max(0f, Math.min(1f, alpha)));
    }

    private static int argb(int rgb, float a) {
        int al = Math.max(5, Math.min(255, Math.round(a * 255f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }
}
