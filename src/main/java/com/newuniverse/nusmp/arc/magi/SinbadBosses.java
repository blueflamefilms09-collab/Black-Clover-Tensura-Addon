package com.newuniverse.nusmp.arc.magi;

import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place that puts the Sinbad boss into the arcs' systems (the same job as {@code GokuBosses}): the roster entry (so the boss registry, the buff row, the commands and the spawn
 * egg find him like every other arc boss), the entity type, how he is drawn, and the door check every summoning goes through ({@link #gate}). Called from {@code ArcCharacters.bootstrap()}.
 */
public final class SinbadBosses {
    private SinbadBosses() {}

    private static boolean done;

    /** The shared person body (the one the Tier List bosses and Goku wear): no model or texture of his own exists yet (see docs/sinbad_boss.md). */
    public static final String BODY = "npc_default";
    public static final float BODY_HEIGHT = 1.97f;

    public static boolean owns(String characterId) { return SinbadBoss.ID.equals(characterId); }

    /** How he is drawn: the shared body in its own texture (no skin of his own). */
    public static ArcLook look() { return new ArcLook(BODY, BODY_HEIGHT, 1.6f, 2.6f, 0.5f, 1.4f, 0.8f, true); }

    /** Registers the roster entry and the entity type. Safe to call twice. */
    public static synchronized void registerAll() {
        if (done) return;
        done = true;
        ArcRoster.register(SinbadBoss.CHARACTER);
        ArcEntities.<SinbadBoss>boss(SinbadBoss.ID, SinbadBoss::new, 0.62f, 1.95f, look(), SinbadBoss::attributes);
    }

    /** Two of him never share an arena: a summoning closer than this (blocks, same dimension) to a standing one is refused. */
    public static final double ARENA_CLEARANCE = 24.0;

    /** May one more Sinbad be summoned now? Null when yes; DISABLED when {@code enabled} is off; FAILED when {@code globalCap} of them already stand. Forced summonings go through it too. */
    public static ArcSpawner.Outcome gate() {
        if (!SinbadBossConfig.enabled()) return ArcSpawner.Outcome.DISABLED;
        if (SinbadBoss.liveCount() >= SinbadBossConfig.globalCap()) return ArcSpawner.Outcome.FAILED;
        return null;
    }

    /** The same, and also ALREADY_PRESENT when another one stands within {@link #ARENA_CLEARANCE} blocks of {@code at} in the same dimension (one boss per arena). */
    public static ArcSpawner.Outcome gate(ServerLevel sl, Vec3 at) {
        ArcSpawner.Outcome no = gate();
        if (no != null) return no;
        double r2 = ARENA_CLEARANCE * ARENA_CLEARANCE;
        for (SinbadBoss b : SinbadBoss.live()) if (b.level() == sl && b.position().distanceToSqr(at) < r2) return ArcSpawner.Outcome.ALREADY_PRESENT;
        return null;
    }
}
