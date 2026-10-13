package com.newuniverse.nusmp.dungeon.sentient;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Death in the sentient Dungeon (the owner's design: "mobs do not leave carcasses; they turn to ash and drop a Magic Stone"). What dies
 * here leaves no corpse and none of its own drops: it goes up in ash (the corpse is gone within two ticks) and leaves Magic Stones, and
 * now and then a rare synthesis material, for the player who killed it, the player's pet, or the player's arrow ({@link StoneDrops}). A mob
 * that no player killed leaves a stone one time in four, so the Dungeon's own fights are no farm. A floor boss keeps its own death (its
 * body is its own business); it still leaves its stones.
 */
public final class SentientDeath {
    private SentientDeath() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The death time (of 20) a corpse is put to, so the vanilla poof removes it two ticks later. */
    private static final int ASH_DEATH_TIME = 18;
    /** A corpse that is still there this many ticks after it was put to ash is removed by hand, and the most that are waited for. */
    private static final int REAP_AFTER = 30, MAX_REAP = 256;

    private record Reap(Mob mob, long due) {}

    private static final List<Reap> REAPS = new ArrayList<>();

    /**
     * Called by DungeonEcology.onDrops for whatever dies in the Dungeon while the Sentient Dungeon is on (a player aside): clears the
     * drops, turns the corpse to ash and adds the stones as the event's drops, so that other handlers (and Looting) see them.
     */
    public static void onDrops(LivingDropsEvent e, ServerLevel sl, LivingEntity victim) {
        e.getDrops().clear();
        if (!(victim instanceof Mob mob)) return;
        try {
            boolean boss = mob.getTags().contains(SentientTags.FLOOR_BOSS);
            ash(sl, mob, boss);
            ServerPlayer killer = credited(e.getSource());
            double scale = DungeonConfig.get(DungeonConfig.STONE_SCALE);
            for (ItemStack stack : StoneDrops.roll(sl, mob, killer, scale)) {
                ItemEntity drop = new ItemEntity(sl, mob.getX(), mob.getY() + 0.3, mob.getZ(), stack);
                drop.setDefaultPickUpDelay();
                e.getDrops().add(drop);
            }
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] The stones of a creature of the Dungeon could not be made", failure);
        }
    }

    /** The player who gets the credit for a death: the one who struck, their pet (a tamed wolf, a summoned follower), or the one who shot. Null if no player did. */
    static ServerPlayer credited(DamageSource source) {
        Entity cause = source.getEntity(), direct = source.getDirectEntity();
        if (cause instanceof ServerPlayer sp) return sp;
        if (cause instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner) return owner;
        if (direct instanceof Projectile shot && shot.getOwner() instanceof ServerPlayer shooter) return shooter;
        if (direct instanceof OwnableEntity pet2 && pet2.getOwner() instanceof ServerPlayer owner2) return owner2;
        return null;
    }

    /** Ash, smoke and a few soul flames where the creature fell, a soft hiss; and the corpse goes within two ticks (a floor boss's does not: it is the boss's own). */
    private static void ash(ServerLevel sl, Mob mob, boolean boss) {
        double x = mob.getX(), y = mob.getY() + mob.getBbHeight() * 0.5, z = mob.getZ();
        double w = Math.max(0.2, mob.getBbWidth() * 0.5), h = Math.max(0.2, mob.getBbHeight() * 0.4);
        sl.sendParticles(ParticleTypes.ASH, x, y, z, 40, w, h, w, 0.02);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 8, w, h, w, 0.02);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 5, w, h, w, 0.03);
        sl.playSound(null, mob.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.5f, 0.6f);
        if (boss) return;
        mob.deathTime = ASH_DEATH_TIME;                                      // the vanilla poof removes it two ticks later
        if (REAPS.size() < MAX_REAP) REAPS.add(new Reap(mob, sl.getServer().getTickCount() + REAP_AFTER));
    }

    /** Removes by hand the corpses that did not go by themselves (a mob whose own death animation runs longer). Called every tick; cheap while the list is empty. */
    static void reap(long tick) {
        if (REAPS.isEmpty()) return;
        for (Iterator<Reap> it = REAPS.iterator(); it.hasNext(); ) {
            Reap r = it.next();
            if (r.mob().isRemoved()) {
                it.remove();
            } else if (tick >= r.due()) {
                r.mob().discard();
                it.remove();
            }
        }
    }

    static void clear() { REAPS.clear(); }
}
