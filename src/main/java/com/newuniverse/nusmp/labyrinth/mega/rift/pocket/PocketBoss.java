package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.PocketBossBridge;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: the bespoke mega-boss of a tomb. It is the Labyrinth Guardian (so it brings the colossal body, the leash, the damage budget and the whole D&amp;D layer for free) configured for the tomb: scaled as a
 * Decennial boss {@link PocketRules#BOSS_THREAT_BONUS} levels above the floor the tomb was opened from, wearing the profile in {@code mega/pockets/bosses/<id>.json} (its own phases, legendary and lair
 * actions, a mythic second life, its look), hitting harder and standing larger by the profile's mega numbers, tied to the middle of its lair, and calling the profile's court of the tomb's own monsters
 * once, at half health. {@link PocketBossBridge} is the doorway into the Guardian's package. It wakes when the first delver enters the lair; a one-second sweep puts its profile back after a reload, calls
 * the court, and, if it vanished (a chunk fault, a command), puts a fresh one in the lair twice before it opens the way instead, so a delver is never locked out of a treasury by a boss that is not there.
 */
public final class PocketBoss {
    private PocketBoss() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String TAG = "nusmp_tomb_boss", TAG_ADDS = "nusmp_tomb_boss_adds";
    /** Ticks the boss may be missing from a lair with a delver near it before it is replaced, and how many replacements a tomb gets. */
    public static final int MISSING_TICKS = 100, MAX_REPLACEMENTS = 2;

    /** The living mega-boss of a tomb, or null. */
    public static LabyrinthGuardianEntity find(ServerLevel level, PocketRuntime rt) {
        List<LabyrinthGuardianEntity> found = level.getEntitiesOfClass(LabyrinthGuardianEntity.class, PocketCell.bounds(rt.slot), g -> g.isAlive() && g.getTags().contains(TAG));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Puts the mega-boss in its lair. Returns it, or null when it could not be made. */
    public static LabyrinthGuardianEntity spawn(ServerLevel level, PocketRuntime rt, PocketRealm.Pocket p) {
        if (rt.bossSpawn == null) return null;
        DndProfile profile = PocketDefs.boss(rt.def);
        PocketBossExtra extra = PocketDefs.extra(rt.def);
        LabyrinthGuardianEntity g = LabyrinthGuardianEntity.create(level, rt.bossSpawn, null, null);
        if (g == null) return null;
        int floor = PocketRules.effectiveFloor(p.originFloor, rt.def.floorMin());
        g.configure(-1, PocketRules.bossThreat(floor), RegionTheme.fromId(rt.def.theme()), rt.bossSpawn);
        if (profile != null) PocketBossBridge.apply(g, profile, extra.damage(), extra.scale());
        g.addTag(TAG);
        g.setPersistenceRequired();
        if (!level.addFreshEntity(g)) return null;
        rt.bossSpawnedAt = level.getGameTime();
        rt.bossMissingSince = -1;
        return g;
    }

    /** The words the boss says when it wakes (a chat line to everybody in the tomb), if its profile has them. */
    public static void intro(ServerLevel level, PocketRuntime rt, List<ServerPlayer> in) {
        PocketBossExtra extra = PocketDefs.extra(rt.def);
        if (extra.intro().isEmpty()) return;
        Component line = Component.translatableWithFallback("pocket.nusmp." + rt.def.id() + ".boss.intro", extra.intro()).withStyle(ChatFormatting.ITALIC, ChatFormatting.LIGHT_PURPLE);
        for (ServerPlayer pl : in) pl.displayClientMessage(line, false);
    }

    public static void death(ServerLevel level, PocketRuntime rt, List<ServerPlayer> in) {
        PocketBossExtra extra = PocketDefs.extra(rt.def);
        if (extra.death().isEmpty()) return;
        Component line = Component.translatableWithFallback("pocket.nusmp." + rt.def.id() + ".boss.death", extra.death()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
        for (ServerPlayer pl : in) pl.displayClientMessage(line, false);
    }

    /**
     * The one-second sweep of the boss: profile back after a reload, the court at half health, and the replacement or the opened way when it is gone. Returns true when the way was opened because the
     * boss could not be kept (the caller marks the boss dead and opens the treasury's loot).
     */
    public static boolean sweep(ServerLevel level, PocketRuntime rt, PocketRealm.Pocket p, List<ServerPlayer> in, long now) {
        if (p.bossDead || !PocketRuntime.woken(p, rt.layout.bossId())) return false;
        LabyrinthGuardianEntity g = find(level, rt);
        if (g != null) {
            rt.bossMissingSince = -1;
            DndProfile profile = PocketDefs.boss(rt.def);
            if (profile != null && PocketBossBridge.ensure(g, profile)) LOG.debug("[nusmp] The profile of the boss of {} was put back after a load.", rt.pocketId);
            PocketBossExtra extra = PocketDefs.extra(rt.def);
            if (g.phaseTwo() && !extra.adds().isEmpty() && !g.getTags().contains(TAG_ADDS)) {
                g.addTag(TAG_ADDS);
                int n = PocketMobs.adds(level, rt, p, extra.adds(), extra.addCount(), g.blockPosition());
                if (n > 0) for (ServerPlayer pl : in) pl.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.boss.court", "The master calls its court.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return false;
        }
        BlockPos at = rt.bossSpawn;
        if (at == null || !level.isLoaded(at)) return false;
        boolean near = false;
        for (ServerPlayer pl : in) if (pl.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5) <= 48.0 * 48.0) near = true;
        if (!near) return false;                                                           // nobody is near the lair: its chunks may simply be asleep
        if (rt.bossMissingSince < 0) {
            rt.bossMissingSince = now;
            return false;
        }
        if (now - rt.bossMissingSince < MISSING_TICKS) return false;
        if (rt.bossRespawns < MAX_REPLACEMENTS) {
            rt.bossRespawns++;
            rt.bossMissingSince = -1;
            LOG.info("[nusmp] The boss of {} was missing; a fresh one stands in its lair ({} of {}).", rt.pocketId, rt.bossRespawns, MAX_REPLACEMENTS);
            return spawn(level, rt, p) == null;
        }
        return true;
    }
}
