package com.newuniverse.nusmp.arc.kakashi;

import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place that puts the Kakashi boss into the arcs' systems: the roster entry (so the boss registry, the commands and the spawn egg find him the way they find every other arc boss),
 * the entity type, and the door check every summoning goes through ({@link #gate}). He is NOT a dungeon boss: his record has no floors, so no floor table can place him; he is made by the
 * command, the spawn egg or an arena, and the coordinator connects him to the boss realm. Called from {@code ArcCharacters.bootstrap()}, before the registry events.
 */
public final class KakashiBosses {
    private KakashiBosses() {}

    private static boolean done;

    /** The shared person body is the FALLBACK look (the Blender body {@code kakashi} is drawn over it by {@code BossMeshRouting} unless the client setting is off). */
    public static final String BODY = "npc_default";
    public static final float BODY_HEIGHT = 1.97f;

    public static boolean owns(String characterId) { return KakashiBoss.ID.equals(characterId); }

    public static ArcLook look() { return new ArcLook(BODY, BODY_HEIGHT, 1.6f, 2.6f, 0.5f, 1.4f, 0.8f, true); }

    /** Registers the roster entry and the entity type. Safe to call twice. */
    public static synchronized void registerAll() {
        if (done) return;
        done = true;
        ArcRoster.register(KakashiBoss.CHARACTER);
        ArcEntities.<KakashiBoss>boss(KakashiBoss.ID, KakashiBoss::new, 0.62f, 1.95f, look(), KakashiBoss::attributes);
    }

    /** Two of him never share an arena: a summoning closer than this (blocks, same dimension) to a standing one is refused. */
    public static final double ARENA_CLEARANCE = 24.0;

    /** May one more Kakashi be summoned now? Null when yes; DISABLED when {@code enabled} is off; FAILED when {@code globalCap} of them already stand. */
    public static ArcSpawner.Outcome gate() {
        if (!KakashiConfig.enabled()) return ArcSpawner.Outcome.DISABLED;
        if (KakashiBoss.liveCount() >= KakashiConfig.globalCap()) return ArcSpawner.Outcome.FAILED;
        return null;
    }

    /** The same, and also ALREADY_PRESENT when another one stands within {@link #ARENA_CLEARANCE} blocks of {@code at} in the same dimension. */
    public static ArcSpawner.Outcome gate(ServerLevel sl, Vec3 at) {
        ArcSpawner.Outcome no = gate();
        if (no != null) return no;
        double r2 = ARENA_CLEARANCE * ARENA_CLEARANCE;
        for (KakashiBoss b : KakashiBoss.live()) if (b.level() == sl && b.position().distanceToSqr(at) < r2) return ArcSpawner.Outcome.ALREADY_PRESENT;
        return null;
    }
}
