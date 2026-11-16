package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place the Saiyan techniques send their anime Ki effects (the {@code KI_*} shapes drawn by {@code KiLayer}, docs/vfx_remake/ki_dragonball.md). Each call is ONE effect packet through
 * {@link VfxSpawn} (the per-tick effect budget, players within 96 blocks only); the techniques call it once or twice per cast or impact, and the three held / standing ones (the Kamehameha charge every
 * 10 ticks, the Spirit Bomb gather every 20, the Ki Barrier once a second) re-send a short copy that cross-fades, so an effect never outlives what it shows by more than a second. The old particle sprays
 * stay where they were (they are what the client setting {@code saiyan.classicVfx} shows). The colour is the ki colour of the caster's form: {@link SaiyanForms#kiColor} (pure, unit tested).
 */
final class SaiyanKiFx {
    private SaiyanKiFx() {}

    static final int BASE = SaiyanForms.KI_BASE, KAME = SaiyanForms.KI_KAME, SPIRIT = SaiyanForms.KI_SPIRIT, GOLD = SaiyanForms.KI_GOLD;

    /** The ki colour of the caster's current form for a technique ({@link SaiyanForms#kiColor}). */
    static int kiColor(ServerPlayer p, int own, boolean followsForm) {
        SaiyanManager.Run r = SaiyanManager.peekRun(p);
        return SaiyanForms.kiColor(r == null ? null : SaiyanFormEngine.active(r), own, followsForm);
    }

    /** A fixed effect between two points. */
    static void send(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int color, int duration, float power) {
        VfxSpawn.send(level, shape, from, to, color, duration, power);
    }

    /** An effect that follows {@code e}, starting at {@code at} (kept at the same offset from it). */
    static void follow(ServerLevel level, VfxShape shape, Entity e, Vec3 at, int color, int duration, float power) {
        VfxSpawn.send(level, new VfxPayload(shape.ordinal(), at, at, color, duration, power, e.getId(), level.random.nextLong()));
    }

    /** The chest of a creature (a little under the eyes). */
    static Vec3 chest(Entity e) { return e.position().add(0.0, e.getBbHeight() * 0.6, 0.0); }

    // ------------------------------------------------------------------ CI server smoke test (prefix saiyan.kivfx in SMOKE_EXPECT)
    private static boolean registered;

    static void registerSelfTests() {
        if (registered) return;
        registered = true;
        com.newuniverse.nusmp.labyrinth.mega.SelfTests.register("saiyan.kivfx", SaiyanKiFx::selfTest);
    }

    /** The ki colour rules hold, and every Ki shape goes out through the effect budget on a dedicated server without throwing (to nobody: no player is near the spawn in CI). */
    private static com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result selfTest(net.minecraft.server.MinecraftServer server) {
        if (SaiyanForms.kiColor(null, KAME, false) != KAME || SaiyanForms.kiColor(SaiyanForms.SUPER_SAIYAN, BASE, true) != SaiyanForms.SUPER_SAIYAN.rgb()
                || SaiyanForms.kiColor(SaiyanForms.SUPER_SAIYAN, KAME, false) != KAME || SaiyanForms.kiColor(SaiyanForms.SUPER_SAIYAN_4, KAME, false) != SaiyanForms.SUPER_SAIYAN_4.rgb()
                || SaiyanForms.kiColor(SaiyanForms.SUPER_SAIYAN_BLUE, GOLD, false) != GOLD)
            return com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result.fail("the ki colour rules moved");
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        int sent = 0;
        for (VfxShape s : VfxShape.values()) {
            if (!s.name().startsWith("KI_")) continue;
            send(level, s, at, at.add(0, 0, 4), BASE, 10, 1f);
            sent++;
        }
        if (sent != 13) return com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result.fail("expected the 13 KI_ shapes, found " + sent);
        return com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result.ok("ki colours (base, form, Kamehameha red in a red form, gold kept) and the 13 Ki shapes sent through VfxSpawn");
    }
}
