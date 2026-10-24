package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Fire V2 look (Leo Rugiens, Sol Linea, Ignis Columna, the flame explosion, Spiral Flame, Wild Bursting Flame, the Flame cast / zone / burst, the flame trail and explosion) is drawn by the client and
 * cannot be seen in a game by its author, so the real game proves what can be proved without eyes (the CI server smoke test runs it; key prefix {@code magic.fire_v2}): (1) the eleven shapes it owns are exactly
 * the old fire shapes and the Calderos eruption and the Demon Fire family are not among them; (2) the worst case of every effect is under the peak of the code it replaces and its vertex budget is the cap in
 * quads times four, never over the activation limit of 400; (3) all 66 textures are in the jar with the right size, clear corners for the square sprites and, for the grey twins, red = green = blue; (4) the
 * options default to V2 / flashes on and parse V1; (5) the reduced-flashes rules (no hard flash, a soft fade that never rises, a flip-book of at most three changes a second) and the tint choice (fire colours
 * keep the baked textures, every other colour is tinted). Pure checks: nothing touches a client class, so it runs on the dedicated server; the effect sends no packet and does no server work.
 */
public final class FireV2SelfTests {
    private FireV2SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.fire_v2", FireV2SelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<VfxShape> owned = FireV2.SHAPES;
        if (owned.size() != 11 || !owned.containsAll(List.of(VfxShape.FIRE_LION, VfxShape.FIRE_SPEAR, VfxShape.FIRE_PILLAR, VfxShape.FIRE_BURST, VfxShape.FIRE_SPIRAL, VfxShape.FIRE_WILD,
                VfxShape.FLAMEFX_CAST, VfxShape.FLAMEFX_ZONE, VfxShape.FLAMEFX_BURST, VfxShape.FLAME_TRAIL, VfxShape.FLAME_EXPLOSION))) bad.add("the owned shapes are wrong: " + owned);
        for (VfxShape untouched : FireV2.UNTOUCHED) if (FireV2.owns(untouched)) bad.add(untouched + " must stay with its own layer");
        int worst = 0;
        for (VfxShape s : owned) {
            int q = FireV2.worstQuads(s);
            worst = Math.max(worst, q);
            if (q <= 0 || q > FireV2.capQuads(s)) bad.add(s + " draws up to " + q + " quads, over the cap " + FireV2.capQuads(s));
            if (FireV2.vertexBudget(s) != FireV2.capQuads(s) * 4 || FireV2.vertexBudget(s) > 400) bad.add(s + " has a vertex budget of " + FireV2.vertexBudget(s));
        }
        int checked = 0;
        for (String path : FireV2.allTextures()) {
            String file = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
            boolean tint = file.endsWith("_t");
            String name = file.substring(FireV2.PREFIX.length(), tint ? file.length() - 2 : file.length());
            try {
                SquareBugAudit.Pixels px = SquareBugAudit.particle(file);
                if (px == null) { bad.add("missing texture " + file); continue; }
                String problem = FireV2.checkTexture(name, tint, px.w(), px.h(), px.argb());
                if (problem != null) bad.add(file + ": " + problem);
                checked++;
            } catch (java.io.IOException | RuntimeException e) {
                bad.add(file + " cannot be read: " + e);
            }
        }
        if (FireV2.DEFAULT_STYLE != FireV2.Style.V2 || FireV2.DEFAULT_REDUCED_FLASHES || FireV2.parse("garbage") != FireV2.Style.V2 || FireV2.parse(null) != FireV2.Style.V2
                || FireV2.parse("v1") != FireV2.Style.V1 || FireV2.parse(" V2 ") != FireV2.Style.V2)
            bad.add("the vfx.element.fire option does not default to V2 / parse V1, or reduced flashes is not off by default");
        if (FireV2.hardFlash(0, 7, true) != 0f || FireV2.hardFlash(0, 7, false) != 1f || FireV2.hardFlash(7, 7, false) != 0f || FireV2.softFlash(0, 7, false) != 0f
                || FireV2.softFlash(0, 7, true) != 0.5f || FireV2.softFlash(5, 7, true) > FireV2.softFlash(2, 7, true) || FireV2.framesPerSecond(true) > 3f
                || FireV2.frame(0, 0, 3, false) != 0 || FireV2.frame(2, 0, 3, false) != 1 || FireV2.frame(6, 0, 3, false) != 0 || FireV2.frame(5, 0, 3, true) != 0)
            bad.add("the reduced-flash rules or the flip-book are wrong");
        if (!FireV2.nativeFire(0xFFFF8A2A) || !FireV2.nativeFire(0xFFFFD86B) || !FireV2.nativeFire(0xFFE5401A) || FireV2.nativeFire(0xFF5599FF) || FireV2.nativeFire(0xFF55FF88)
                || FireV2.nativeFire(0xFF1A1A1A) || FireV2.nativeFire(0xFFFFFFFF))
            bad.add("the tint choice is wrong (fire colours keep the baked textures, every other colour is tinted)");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(checked + " textures (clear corners, grey twins); worst quads lion " + FireV2.worstQuads(VfxShape.FIRE_LION) + " / pillar " + FireV2.worstQuads(VfxShape.FIRE_PILLAR) + " / wild "
                + FireV2.worstQuads(VfxShape.FIRE_WILD) + " / spiral " + FireV2.worstQuads(VfxShape.FIRE_SPIRAL) + " (every one under the old layer's peak); eruption and demon fire untouched; default V2, V1 restores the old layers; "
                + "reduced flashes drop the hard flash and rays");
    }
}
