package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Wind pixel look (Crescent Kamaitachi, Slicing Wind Emperor, Gale / Gust Lane, Wind Ring) is drawn by the client and cannot be seen in a game by its author, so the real game proves what can be proved
 * without eyes (the CI server smoke test runs it; key prefix {@code magic.wind_mc}): (1) the five shapes it owns are exactly Slash, Ring, Emperor, Gale and Gust Lane and the Wind Tornado and Spirit of Zephyr are
 * not among them; (2) the worst case of every effect is under the peak of the code it replaces and its vertex budget is the cap in quads times four, never over the activation limit of 400; (3) all 32 textures
 * are in the jar with the right size, hard alpha and only their five palette colours (the pack palette, or the greys that take the tint of the non-wind melee pages); (4) the option defaults to MC_C and parses
 * V1; (5) the stepped timing (on twos, a flip-book frame every three ticks, four alpha steps) and the tint choice (wind greens keep the pack palette, every other colour is tinted). Pure checks: nothing touches a
 * client class, so it runs on the dedicated server; the effect sends no packet and does no server work.
 */
public final class WindMcSelfTests {
    private WindMcSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.wind_mc", WindMcSelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<VfxShape> owned = WindMc.SHAPES;
        if (owned.size() != 5 || !owned.containsAll(List.of(VfxShape.WIND_SLASH, VfxShape.WIND_RING, VfxShape.WIND_EMPEROR, VfxShape.WIND_GALE, VfxShape.FX_WIND_GUST))) bad.add("the owned shapes are wrong: " + owned);
        for (VfxShape untouched : WindMc.UNTOUCHED) if (WindMc.owns(untouched)) bad.add(untouched + " must stay with the old layer");
        int worst = 0;
        for (VfxShape s : owned) {
            int q = WindMc.worstQuads(s);
            worst = Math.max(worst, q);
            if (q <= 0 || q > WindMc.capQuads(s)) bad.add(s + " draws up to " + q + " quads, over the cap " + WindMc.capQuads(s));
            if (WindMc.vertexBudget(s) != WindMc.capQuads(s) * 4 || WindMc.vertexBudget(s) > 400) bad.add(s + " has a vertex budget of " + WindMc.vertexBudget(s));
        }
        int checked = 0;
        for (String path : WindMc.allTextures()) {
            String file = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
            boolean tint = file.endsWith("_t");
            String name = file.substring(WindMc.PREFIX.length(), file.indexOf('_', WindMc.PREFIX.length()));
            try {
                SquareBugAudit.Pixels px = SquareBugAudit.particle(file);
                if (px == null) { bad.add("missing texture " + file); continue; }
                String problem = WindMc.checkTexture(name, tint, px.w(), px.h(), px.argb());
                if (problem != null) bad.add(file + ": " + problem);
                checked++;
            } catch (java.io.IOException | RuntimeException e) {
                bad.add(file + " cannot be read: " + e);
            }
        }
        if (WindMc.DEFAULT_STYLE != WindMc.Style.MC_C || WindMc.parse("garbage") != WindMc.Style.MC_C || WindMc.parse(null) != WindMc.Style.MC_C || WindMc.parse("v1") != WindMc.Style.V1 || WindMc.parse(" MC_C ") != WindMc.Style.MC_C)
            bad.add("the vfx.element.wind option does not default to MC_C / parse V1");
        if (WindMc.onTwos(5) != 4 || WindMc.onTwos(4) != 4 || WindMc.frame(0) != 0 || WindMc.frame(3) != 1 || WindMc.frame(6) != 0 || WindMc.step4(0.3f) != 0.25f || WindMc.step4(2f) != 1f || WindMc.fade(0, 16, 2, 5) != 0.5f)
            bad.add("the stepped timing is wrong");
        if (!WindMc.nativePalette(0xFF8CFFC2) || !WindMc.nativePalette(0xFFA6FFD6) || WindMc.nativePalette(0xFFD8E2FF) || WindMc.nativePalette(0xFFFF8C1A) || WindMc.nativePalette(0xFF1A1A1A) || WindMc.nativePalette(0xFFFFFFFF))
            bad.add("the tint choice is wrong (wind greens keep the pack palette, every other colour is tinted)");
        if (!WindMc.isPixelTexture("textures/particle/wmc_ring_0.png") || WindMc.isPixelTexture("textures/particle/wind_slash.png") || WindMc.isPixelTexture("textures/particle/time_clock.png"))
            bad.add("the nearest-neighbour test names the wrong textures");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(checked + " textures (hard alpha, 5-colour palettes); worst quads slash " + WindMc.worstQuads(VfxShape.WIND_SLASH) + " / emperor " + WindMc.worstQuads(VfxShape.WIND_EMPEROR) + " / gale "
                + WindMc.worstQuads(VfxShape.WIND_GALE) + " / ring " + WindMc.worstQuads(VfxShape.WIND_RING) + " (caps 72 / 80 / 68 / 100); tornado and zephyr untouched; default MC_C, V1 restores the old layers");
    }
}
