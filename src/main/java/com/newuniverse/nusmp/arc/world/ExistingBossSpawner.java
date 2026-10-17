package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.arena.SpawnGate;
import com.newuniverse.nusmp.entity.zogratis.DevilStyle;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Why: the seven bosses the mod had before the arcs (Goliath, the Charybdis Spawn, the Devil Avatar, Dante, Vanica, Zenon, Lucius) are not {@code ArcBoss}
 * subclasses and are not in {@code ArcEntities}, yet the roster lists them and the Chronicle, the commands and the dungeon must be able to bring them in like
 * any other character. This class is the door for them: it finds the entity type from the id ({@link ExistingBosses#entityPath}, looked up in the registry at
 * the moment of use, so a missing type is a polite refusal and not a crash), applies the same rules as {@code ArcSpawner.summon} (the layer is on, the story
 * gate lets the party through unless an operator forces it, one at a time) and places the boss the way the older code does it ({@code ZogratisBoss.spawnAt}:
 * the arena is the spot, a circle in the boss's own colour, a sound). Nothing about the fight itself is changed: they are spawned by id, as the brief says.
 * Lucius is the one that keeps his own rule (he exists only in the Final Hour's arena unless the server switched that off): the spawn gate refuses him
 * anywhere else and this class reports that as a failed summoning rather than hiding it.
 */
public final class ExistingBossSpawner {
    private ExistingBossSpawner() {}

    /** What {@link #make} did, and the boss if one was made. */
    public record Made(ArcSpawner.Outcome outcome, ZogratisBoss boss) {}

    /** The entity type of an existing boss, or null (not one of them, or its type is not registered). */
    public static EntityType<?> typeOf(String characterId) {
        String path = ExistingBosses.entityPath(characterId);
        if (path == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path)).orElse(null);
    }

    /** The live bosses of this character within {@link ArcSpawner#PRESENT_RADIUS} of a spot. */
    public static List<ZogratisBoss> standing(ServerLevel sl, String characterId, Vec3 near) {
        EntityType<?> type = typeOf(characterId);
        List<ZogratisBoss> out = new ArrayList<>();
        if (type == null) return out;
        out.addAll(sl.getEntitiesOfClass(ZogratisBoss.class, new AABB(near, near).inflate(ArcSpawner.PRESENT_RADIUS), b -> b.isAlive() && b.getType() == type));
        return out;
    }

    /** The door {@code ArcSpawner.summon} opens for the existing bosses: the same answers, the boss itself is not an {@code ArcBoss}, so the result carries none. */
    public static ArcSpawner.Result summon(ServerLevel sl, ArcCharacter c, Vec3 at, float yaw, Collection<? extends Player> party, boolean force) {
        if (!ArcConfig.enabled()) return new ArcSpawner.Result(ArcSpawner.Outcome.DISABLED, null, List.of());
        if (c == null || !c.isBoss()) return new ArcSpawner.Result(ArcSpawner.Outcome.NOT_A_BOSS, null, List.of());
        if (typeOf(c.id()) == null) return new ArcSpawner.Result(ArcSpawner.Outcome.NO_ENTITY, null, List.of());
        if (!force) {
            if (!ArcStory.gateOpen(party, c)) return new ArcSpawner.Result(ArcSpawner.Outcome.LOCKED, null, ArcStory.gateMissing(party, c));
            if (!standing(sl, c.id(), at).isEmpty()) return new ArcSpawner.Result(ArcSpawner.Outcome.ALREADY_PRESENT, null, List.of());
        }
        Made m = make(sl, c, at, yaw, false);
        return new ArcSpawner.Result(m.outcome(), null, List.of());
    }

    /**
     * Makes the boss and adds it to the level at {@code at} with its arena there. No gate and no "already present" check (the caller has made them); the
     * layer-off check and the entity check are kept. {@code arenaOk} puts the spawn gate's tag on it, for the places that are deliberately his room.
     */
    public static Made make(ServerLevel sl, ArcCharacter c, Vec3 at, float yaw, boolean arenaOk) {
        if (!ArcConfig.enabled()) return new Made(ArcSpawner.Outcome.DISABLED, null);
        EntityType<?> type = typeOf(c == null ? null : c.id());
        if (type == null) return new Made(ArcSpawner.Outcome.NO_ENTITY, null);
        Entity e = type.create(sl);
        if (!(e instanceof ZogratisBoss boss)) {
            if (e != null) e.discard();
            return new Made(ArcSpawner.Outcome.FAILED, null);
        }
        boss.moveTo(at.x, at.y, at.z, yaw, 0f);
        boss.setArena(BlockPos.containing(at));
        if (arenaOk) boss.getPersistentData().putBoolean(SpawnGate.TAG, true);
        if (!sl.addFreshEntity(boss)) return new Made(ArcSpawner.Outcome.FAILED, null);       // refused by an event (Lucius outside his arena): the caller says so
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, at.add(0, 0.05, 0), at.add(0, 1, 0), 0xFF000000 | DevilStyle.color(boss.devil()), 60, 3f);
        sl.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.6f);
        return new Made(ArcSpawner.Outcome.SPAWNED, boss);
    }
}
