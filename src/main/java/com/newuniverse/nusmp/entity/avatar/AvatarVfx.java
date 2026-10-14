package com.newuniverse.nusmp.entity.avatar;

import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 1.35.0: the Devil Avatar's move effects, one telegraph and one strike per move, drawn by the client's {@code DevilBossLayer}: hellfire circles and
 * devil sigils on the ground for the pulls and the marks, chains from the roof for the gravity, brimstone cracks for everything that lands, and the
 * brand's pillar for the Dread Brand. The effects are only drawn (no block, no push); the gameplay of every move stays in {@link AvatarMoves}. They
 * keep the Avatar's own tint (violet for Lucifero, crimson for Megicula) and go through {@link BossMoveVfx}'s per-tick budget.
 */
final class AvatarVfx {
    private AvatarVfx() {}

    private static int tint(DevilAvatarBoss b) { return AvatarFx.argb(b); }

    /** The roof above a point: 14 blocks up (a pull lifts at most that far), where the chains hang from. */
    private static Vec3 roof(Vec3 at) { return at.add(0, 14, 0); }

    // ---- Void Claws
    static void clawsTell(DevilAvatarBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 mid = b.position().add(AvatarFx.facing(b).scale(AvatarMath.CLAW_REACH * 0.5));
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_CRACKS, mid, mid.add(0, 1, 0), tint(b), 14, AvatarMath.CLAW_REACH * 0.55);
    }

    static void clawsStrike(DevilAvatarBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 mid = b.position().add(AvatarFx.facing(b).scale(AvatarMath.CLAW_REACH * 0.5));
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_CRACKS, mid, mid.add(0, 1, 0), tint(b), AvatarMath.CLAW_REACH * 0.55);
    }

    // ---- Hex Volley
    static void volleyTell(DevilAvatarBoss b, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_SIGIL, b.position(), b.position().add(0, 1, 0), tint(b), windUp, 2.6);
    }

    static void volleyStrike(DevilAvatarBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_SIGIL, b.position(), b.position().add(0, 1, 0), tint(b), 2.6);
    }

    // ---- Shadow Step
    static void stepTell(DevilAvatarBoss b, Vec3 exit, int windUp) {
        if (!(b.level() instanceof ServerLevel sl) || exit == null) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_SIGIL, exit, exit.add(0, 1, 0), tint(b), windUp, AvatarMath.SHADOW_STRIKE_RADIUS);
    }

    static void stepStrike(DevilAvatarBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_CRACKS, b.position(), b.position().add(0, 1, 0), tint(b), AvatarMath.SHADOW_STRIKE_RADIUS + 1.5);
    }

    // ---- Ceiling Pull (and the first half of the Abyssal Collapse)
    static void pullTell(DevilAvatarBoss b, double radius, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position();
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_SIGIL, c, c.add(0, 1, 0), tint(b), windUp, radius);
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_CHAINS, c, roof(c), tint(b), windUp, radius);
    }

    static void pullStrike(DevilAvatarBoss b, double radius) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position();
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_CHAINS, c, roof(c), tint(b), radius);
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_SIGIL, c, c.add(0, 1, 0), tint(b), radius);
    }

    // ---- Floor Crush
    static void crushTell(DevilAvatarBoss b, Vec3 mark, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_SIGIL, mark, mark.add(0, 1, 0), tint(b), windUp, AvatarMath.CRUSH_RADIUS);
    }

    static void crushStrike(DevilAvatarBoss b, Vec3 mark) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_CRACKS, mark, mark.add(0, 1, 0), tint(b), AvatarMath.CRUSH_RADIUS + 0.8);
    }

    // ---- Inversion Field
    static void fieldTell(DevilAvatarBoss b, Vec3 mark, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_SIGIL, mark, mark.add(0, 1, 0), tint(b), windUp, AvatarMath.FIELD_RADIUS);
    }

    /** The field stays for its whole length: a chain cage over it (the roof is the anchor) and a flare of the sigil. */
    static void fieldStrike(DevilAvatarBoss b, Vec3 mark) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_DEVIL_CHAINS, mark, roof(mark), tint(b), 40, AvatarMath.FIELD_RADIUS);
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_SIGIL, mark, mark.add(0, 1, 0), tint(b), AvatarMath.FIELD_RADIUS);
    }

    // ---- Dread Brand
    static void brandTell(DevilAvatarBoss b, Vec3 mark, double radius, int windUp) {
        if (!(b.level() instanceof ServerLevel sl) || mark == null) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_BRAND, mark, mark.add(0, 1, 0), tint(b), windUp, radius);
    }

    static void brandStrike(DevilAvatarBoss b, Vec3 at, double radius) {
        if (!(b.level() instanceof ServerLevel sl) || at == null) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_BRAND, at, at.add(0, 1, 0), tint(b), radius);
    }

    // ---- Curse Nova
    static void novaTell(DevilAvatarBoss b, double reach, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_CRACKS, b.position(), b.position().add(0, 1, 0), tint(b), windUp, reach);
    }

    static void novaStrike(DevilAvatarBoss b, Vec3 centre, double reach) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_CRACKS, centre, centre.add(0, 1, 0), tint(b), reach);
    }

    // ---- Call the Disciples
    static void callTell(DevilAvatarBoss b, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_SIGIL, b.position(), b.position().add(0, 1, 0), tint(b), windUp, 4.5);
    }

    static void callStrike(DevilAvatarBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_SIGIL, b.position(), b.position().add(0, 1, 0), tint(b), 4.5);
    }

    // ---- Brimstone Eruption: evenly spaced spokes (the layer's "spoke mode": the horizontal distance of 'to' from 'from' is the spoke count, its direction the first spoke)
    static void eruptionTell(DevilAvatarBoss b, double base, int spokes, boolean second, int windUp) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position();
        Vec3 to = c.add(Math.cos(base) * spokes, 0, Math.sin(base) * spokes);
        BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_CRACKS, c, to, tint(b), windUp, AvatarMath.ERUPT_LENGTH);
        if (second) {
            double b2 = base + Math.PI / spokes;
            BossMoveVfx.telegraph(sl, VfxShape.BOSS_DEVIL_CRACKS, c, c.add(Math.cos(b2) * spokes, 0, Math.sin(b2) * spokes), tint(b), windUp, AvatarMath.ERUPT_LENGTH);
        }
    }

    static void eruptionStrike(DevilAvatarBoss b, Vec3 c, double base, int spokes) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_CRACKS, c, c.add(Math.cos(base) * spokes, 0, Math.sin(base) * spokes), tint(b), AvatarMath.ERUPT_LENGTH);
    }

    // ---- the Gates (the true form): a sigil as wide as the room's heart, cracks and chains from the roof
    static void gates(DevilAvatarBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 c = b.position();
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_DEVIL_SIGIL, c, c.add(0, 1, 0), tint(b), 50, 9.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_CRACKS, c, c.add(0, 1, 0), tint(b), 10.0);
        BossMoveVfx.strike(sl, VfxShape.BOSS_DEVIL_CHAINS, c, roof(c), tint(b), 8.0);
    }
}
