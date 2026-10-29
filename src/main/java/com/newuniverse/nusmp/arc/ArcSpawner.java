package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Why: a character enters the world through a few doors (the summoning command, the spawn egg, a story arena that wakes its boss, the Hub placing its
 * NPCs), and each must apply the same rules: the layer is on, the character is a boss with an entity type, the story gate lets the party summon it
 * (unless an operator forces it), and there is not already one standing at the arena. This class owns those rules and the plain act of spawning, so no
 * door re-implements them. A boss is spawned the way {@code ZogratisBoss.spawnAt} does it (the arena is the spot, a circle in the character's own
 * colour, a sound) except that the circle uses the character's palette instead of a devil's.
 */
public final class ArcSpawner {
    private ArcSpawner() {}

    /** Why a summoning did or did not happen. */
    public enum Outcome { SPAWNED, DISABLED, NOT_A_BOSS, NO_ENTITY, LOCKED, ALREADY_PRESENT, FAILED }

    /** The result of {@link #summon}: what happened, the boss if one was made, and for LOCKED what the party still has to defeat. */
    public record Result(Outcome outcome, ArcBoss boss, List<String> missing) {}

    /** How far from the spot an already standing boss of the same character counts as "already here". */
    public static final double PRESENT_RADIUS = 96.0;

    /** The entity type of a boss character, or null (not a boss, or its class is not loaded). */
    public static EntityType<? extends ArcBoss> typeOf(String characterId) {
        for (ArcEntities.Entry e : ArcEntities.bosses()) {
            if (e.id().equals(characterId)) {
                EntityType<? extends ArcBoss> t = e.type().get();
                return t;
            }
        }
        return null;
    }

    /**
     * Summons a boss at a spot.
     *
     * @param party the players it is summoned for (the story gate is asked about them); may be empty when {@code force} is set
     * @param force an operator's summoning: ignores the story gate and a boss that is already standing
     */
    public static Result summon(ServerLevel sl, ArcCharacter c, Vec3 at, float yaw, Collection<? extends Player> party, boolean force) {
        if (!ArcConfig.enabled()) return new Result(Outcome.DISABLED, null, List.of());
        if (c == null || !c.isBoss()) return new Result(Outcome.NOT_A_BOSS, null, List.of());
        if (c.saga() == ArcSaga.TIER_LIST) {                                                     // 3.33.0: the Tier List bosses have their own switch and a server-wide live cap
            if (!ArcConfig.tierBosses()) return new Result(Outcome.DISABLED, null, List.of());
            if (!force && com.newuniverse.nusmp.arc.tier.TierBoss.liveCount() >= ArcConfig.tierBossMax()) return new Result(Outcome.FAILED, null, List.of());
        }
        if (com.newuniverse.nusmp.arc.world.ExistingBosses.owns(c.id())) return com.newuniverse.nusmp.arc.world.ExistingBossSpawner.summon(sl, c, at, yaw, party, force);   // 1.61.0: the seven older bosses are not ArcBosses; they are spawned by id
        EntityType<? extends ArcBoss> type = typeOf(c.id());
        if (type == null) return new Result(Outcome.NO_ENTITY, null, List.of());
        if (!force) {
            if (!ArcStory.gateOpen(party, c)) return new Result(Outcome.LOCKED, null, ArcStory.gateMissing(party, c));
            if (!standing(sl, c.id(), at).isEmpty()) return new Result(Outcome.ALREADY_PRESENT, null, List.of());
        }
        ArcBoss boss = type.create(sl);
        if (boss == null) return new Result(Outcome.FAILED, null, List.of());
        boss.moveTo(at.x, at.y, at.z, yaw, 0f);
        boss.setArena(BlockPos.containing(at));
        if (!sl.addFreshEntity(boss)) return new Result(Outcome.FAILED, null, List.of());
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, at.add(0, 0.05, 0), at.add(0, 1, 0), 0xFF000000 | boss.styleColor(), 60, 3f);
        sl.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.6f);
        return new Result(Outcome.SPAWNED, boss, List.of());
    }

    /** The live bosses of this character within {@link #PRESENT_RADIUS} of a spot. */
    public static List<ArcBoss> standing(ServerLevel sl, String characterId, Vec3 near) {
        List<ArcBoss> out = new ArrayList<>(sl.getEntitiesOfClass(ArcBoss.class, new AABB(near, near).inflate(PRESENT_RADIUS),
                b -> b.isAlive() && b.character().id().equals(characterId)));
        return out;
    }

    /** Places a friendly character with its post at the spot. Null if the character is not an NPC, the layer is off, or the entity could not be made. */
    public static ArcNpcEntity spawnNpc(ServerLevel sl, ArcCharacter c, Vec3 at, float yaw) {
        if (!ArcConfig.enabled() || c == null || !c.isNpc()) return null;
        ArcNpcEntity npc = ArcEntities.NPC.get().create(sl);
        if (npc == null) return null;
        npc.moveTo(at.x, at.y, at.z, yaw, 0f);
        npc.setCharacter(c.id());
        npc.setHome(BlockPos.containing(at));
        return sl.addFreshEntity(npc) ? npc : null;
    }

    /** Every NPC in the level that is this character (loaded chunks only). */
    public static List<ArcNpcEntity> npcs(ServerLevel sl, String characterId) {
        List<ArcNpcEntity> out = new ArrayList<>();
        out.addAll(sl.getEntities(EntityTypeTest.<net.minecraft.world.entity.Entity, ArcNpcEntity>forClass(ArcNpcEntity.class), n -> characterId.equals(n.characterId())));
        return out;
    }
}
