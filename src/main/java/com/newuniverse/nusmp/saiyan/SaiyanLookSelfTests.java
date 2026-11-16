package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

/**
 * Why: the CI server smoke test of the 3.69.0 look layer (prefix {@code saiyan.look} in SMOKE_EXPECT). The drawing is client code the dedicated server never runs, so what the server can check is
 * what it owns: every form has a look and the code in the aura packet finds it, the aura slot the packet names is the last one (so no saved or networked ordinal moved), and the charge-up maths stays inside
 * its bounds for every tick of its whole length (rocks, streaks, rings, cracks, column) so a bad cell in the table can never make a client draw a runaway shape.
 */
public final class SaiyanLookSelfTests {
    private SaiyanLookSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("saiyan.look.rules", SaiyanLookSelfTests::rules);
        SelfTests.register("saiyan.hair.rules", SaiyanLookSelfTests::hair);                   // docs/saiyan_hair.md: the hair table and the sway / growth maths
    }

    private static Result rules(MinecraftServer server) {
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            SaiyanLook.Look k = SaiyanLook.of(f.id());
            if (k == null) return Result.fail("form " + f.id() + " has no look");
            if (SaiyanLook.ofCode(SaiyanForms.code(f.id())) != k) return Result.fail("the packet code of " + f.id() + " does not find its look");
            if (SaiyanLook.quads(k, false) > SaiyanLook.MAX_QUADS) return Result.fail(f.id() + " draws more quads than the budget");
            if (SaiyanLook.quads(k, true) >= SaiyanLook.quads(k, false)) return Result.fail(f.id() + ": the far version is not lighter");
        }
        if (SaiyanLook.ofCode(0) != null || SaiyanLook.ofCode(9999) != null) return Result.fail("an unknown code found a look");
        Aura[] all = Aura.values();
        if (all[all.length - 1] != Aura.SAIYAN) return Result.fail("Aura.SAIYAN is no longer the last slot (a networked ordinal moved)");
        float[] o = new float[7];
        for (SaiyanLook.Look k : SaiyanLook.all()) {
            for (float t = 0f; t <= SaiyanLook.CRACK_TICKS + 4f; t += 0.5f) {
                float b = k.burst();
                if (SaiyanLook.crackRadius(t, b) > 4.7f || SaiyanLook.ringRadius(t, 0, b) > 9.5f || SaiyanLook.ringRadius(t, 1, b) > 9.5f || SaiyanLook.pillar(t) > 1f
                        || SaiyanLook.flash(t) > 1f || SaiyanLook.pillarWidth(t, b) > 1.2f) return Result.fail(k.id() + ": a charge-up size left its bound at tick " + t);
                for (int i = 0; i < SaiyanLook.MAX_CHIPS; i++) {
                    SaiyanLook.chip(i, t, b, o);
                    if (Float.isNaN(o[1] + o[2]) || o[1] > 6f || o[2] > 3f || o[2] < 0f) return Result.fail(k.id() + ": rock chip " + i + " left its bound at tick " + t);
                }
                if (SaiyanLook.halo(t) < 0f || SaiyanLook.halo(t) > 1f || SaiyanLook.shake(t) > 1f || SaiyanLook.windAlpha(t) > 1f || SaiyanLook.haloSize(t, b) > 6.2f)
                    return Result.fail(k.id() + ": a power-up envelope left its bound at tick " + t);                                  // 3.71.0: the ray burst, the tremor and the wind
                for (int i = 0; i < SaiyanLook.MAX_WIND; i++) {
                    SaiyanLook.wind(i, t, o);
                    if (Float.isNaN(o[0] + o[1] + o[2] + o[3] + o[4]) || o[1] < 0f || o[1] > 3.3f || o[3] < 0f || o[3] > 1f) return Result.fail(k.id() + ": wind streak " + i + " left its bound at tick " + t);
                }
                for (int i = 0; i < SaiyanLook.MAX_STREAKS; i++) {
                    SaiyanLook.streak(i, t, o);
                    if (Float.isNaN(o[0] + o[1] + o[2] + o[3]) || Math.abs(o[0]) > 3.7f || Math.abs(o[2]) > 3.7f || o[3] < 0f || o[3] > 1f) return Result.fail(k.id() + ": streak " + i + " left its bound at tick " + t);
                }
            }
        }
        return Result.ok(SaiyanLook.all().size() + " forms have a look inside the quad budget, Aura.SAIYAN is the last slot, and every charge-up size (ray burst, tremor and wind included) stays bounded for its whole length");
    }

    /** The hair table (docs/saiyan_hair.md): every form has a hair of a known shape with a sane colour, the growth and the sway stay inside their bounds for the whole length of a form, the distance levels step the right way. */
    private static Result hair(MinecraftServer server) {
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            SaiyanHair.Hair h = SaiyanHair.of(f.id());
            if (h == null) return Result.fail("form " + f.id() + " has no hair");
            if (SaiyanHair.shapeIndex(h.shape()) < 0) return Result.fail(f.id() + ": unknown hair shape " + h.shape());
            if (h.tint() == 0 || h.glowK() < 0f || h.glowK() > 1.5f) return Result.fail(f.id() + ": a hair colour or glow strength is out of range");
        }
        if (SaiyanHair.of("base") != null || SaiyanHair.of(null) != null) return Result.fail("Base or null found a hair");
        for (float t = 0f; t <= 400f; t += 0.5f) {
            float g = SaiyanHair.grow(t, 400f - t, true), sc = SaiyanHair.scale(g);
            if (Float.isNaN(g) || g < 0f || g > 1f || sc < 0.39f || sc > 1.13f) return Result.fail("hair growth left its bound at tick " + t);
            for (int slot = 0; slot <= SaiyanHair.TAIL_TIP; slot++) {
                float x = SaiyanHair.swayX(slot, t, 1f), z = SaiyanHair.swayZ(slot, t);
                if (Float.isNaN(x + z) || Math.abs(x) > 0.6f || Math.abs(z) > 0.3f) return Result.fail("hair sway " + slot + " left its bound at tick " + t);
            }
        }
        if (SaiyanHair.wanted(10 * 10) != com.newuniverse.nusmp.book.NoelleFormRules.Lod.STANDARD || SaiyanHair.wanted(30 * 30) != com.newuniverse.nusmp.book.NoelleFormRules.Lod.FAR
                || SaiyanHair.wanted(50 * 50) != com.newuniverse.nusmp.book.NoelleFormRules.Lod.NONE) return Result.fail("the hair distance levels are wrong");
        if (!SaiyanHair.TAIL_MONKEY.equals(SaiyanHair.tailId(true, false)) || !SaiyanHair.TAIL_WRAP.equals(SaiyanHair.tailId(true, true)) || !SaiyanHair.TAIL.equals(SaiyanHair.tailId(false, true))
                || SaiyanHair.monkeyTint(0) != 0xFFFFFF || SaiyanHair.monkeyTint(0xC02020) == 0xC02020) return Result.fail("the monkey tail choice or tint is wrong");     // docs/body_parts_pass.md
        return Result.ok(SaiyanForms.ALL.size() + " forms have a hair of " + SaiyanHair.SHAPES.length + " shapes, growth and sway stay bounded, the distance levels step full / far / none");
    }
}
