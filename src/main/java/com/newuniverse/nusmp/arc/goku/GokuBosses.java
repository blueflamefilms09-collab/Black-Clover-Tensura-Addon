package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place that puts the Goku boss into the arcs' systems: the roster entry (so the boss registry, the buff row, the commands and the spawn egg find him the way they find every
 * other arc boss), the entity type, and how he is drawn (the shared person body in his skin; the client swaps the skin by form, see {@code ArcBossRenderer}). Called from
 * {@code ArcCharacters.bootstrap()}, before the registry events. Also the one door check every summoning goes through ({@link #gate}).
 */
public final class GokuBosses {
    private GokuBosses() {}

    private static boolean done;

    /** The shared person body (the same one the Tier List bosses wear) and its true height. */
    public static final String BODY = "npc_default";
    public static final float BODY_HEIGHT = 1.97f;

    public static boolean owns(String characterId) { return GokuBoss.ID.equals(characterId); }

    /** How he is drawn: the shared body in the Base skin. */
    public static ArcLook look() {
        return new ArcLook(BODY, BODY_HEIGHT, 1.6f, 2.6f, 0.5f, 1.4f, 0.8f, true, GokuLadder.SKINS.get(0));
    }

    /** Registers the roster entry and the entity type. Safe to call twice. */
    public static synchronized void registerAll() {
        if (done) return;
        done = true;
        ArcRoster.register(GokuBoss.CHARACTER);
        ArcEntities.<GokuBoss>boss(GokuBoss.ID, GokuBoss::new, 0.7f, 1.95f, look(), GokuBoss::attributes);
    }

    /** Two of him never share an arena: a summoning closer than this (blocks, same dimension) to a standing one is refused. */
    public static final double ARENA_CLEARANCE = 24.0;

    /**
     * May one more Goku boss be summoned now? Null when yes; DISABLED when {@code enabled} is off; FAILED when {@code globalCap} of them already stand. Called by
     * {@code ArcSpawner.summon} for every door (command, egg, arena), forced summonings included: the cap is what keeps a server from lagging.
     */
    public static ArcSpawner.Outcome gate() {
        if (!GokuBossConfig.enabled()) return ArcSpawner.Outcome.DISABLED;
        if (GokuBoss.liveCount() >= GokuBossConfig.globalCap()) return ArcSpawner.Outcome.FAILED;
        return null;
    }

    /** The same, and also: ALREADY_PRESENT when another one stands within {@link #ARENA_CLEARANCE} blocks of {@code at} in the same dimension (one boss per arena). */
    public static ArcSpawner.Outcome gate(ServerLevel sl, Vec3 at) {
        ArcSpawner.Outcome no = gate();
        if (no != null) return no;
        double r2 = ARENA_CLEARANCE * ARENA_CLEARANCE;
        for (GokuBoss b : GokuBoss.live()) if (b.level() == sl && b.position().distanceToSqr(at) < r2) return ArcSpawner.Outcome.ALREADY_PRESENT;
        return null;
    }
}
