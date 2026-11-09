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
                for (int i = 0; i < SaiyanLook.MAX_STREAKS; i++) {
                    SaiyanLook.streak(i, t, o);
                    if (Float.isNaN(o[0] + o[1] + o[2] + o[3]) || Math.abs(o[0]) > 3.7f || Math.abs(o[2]) > 3.7f || o[3] < 0f || o[3] > 1f) return Result.fail(k.id() + ": streak " + i + " left its bound at tick " + t);
                }
            }
        }
        return Result.ok(SaiyanLook.all().size() + " forms have a look inside the quad budget, Aura.SAIYAN is the last slot, and every charge-up size stays bounded for its whole length");
    }
}
