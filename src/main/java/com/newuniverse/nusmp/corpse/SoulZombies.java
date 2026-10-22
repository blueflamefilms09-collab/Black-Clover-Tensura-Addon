package com.newuniverse.nusmp.corpse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the one door through which Soul Corpse Magic makes zombies (the Marionette, Necropolis and Legion pages of the Soul Corpse book, Reanimation, and True Soul Corpse Magic's re-raising of
 * fallen pets). All the rules of "a summon" are here once: the caster owns at most {@code corpse.zombieCap} at a time over every page (summoning more dismisses the oldest first), each has a
 * clock ({@code Life} in its tag, ended by {@link Corpses}' sweep, which also ends it when the owner logs out, dies or changes dimension), its health and blow scale with the grimoire's power
 * ({@link SoulZombieRules}), it appears with a violet custom-particle cue and is never saved, never drops anything and never burns in daylight ({@link SoulZombieEntity}). Server only. The
 * state is per owner UUID in {@link Corpses#ZOMBIES}; nothing is static per "current player".
 */
public final class SoulZombies {
    private SoulZombies() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /**
     * Summons up to 'n' zombies round 'owner' for 'lifeTicks' ticks, grown by 'scale' (1.0 to 1.4, see {@code SongRebuildKit.size}). 'firstAt' is where the first one climbs out when not null
     * (the place a creature fell), else every one gets a free spot 1.5 to 4 blocks from the owner. Returns the zombies that stood up (possibly none; the world can refuse a body).
     */
    public static List<SoulZombieEntity> summon(ServerPlayer owner, int n, int lifeTicks, double scale, Vec3 firstAt) {
        List<SoulZombieEntity> out = new ArrayList<>();
        if (owner == null || !owner.isAlive() || n <= 0) return out;
        ServerLevel level = owner.serverLevel();
        UUID id = owner.getUUID();
        int cap = CorpseConfig.zombieCap();
        int make = SoulZombieRules.fitGlobal(Corpses.ZOMBIES.count(id), n, cap, Corpses.ZOMBIES.total(), CorpseConfig.zombieGlobalCap())[0];
        int life = Math.max(SoulZombieRules.MIN_LIFE, Math.min(SoulZombieRules.MAX_LIFE, lifeTicks));
        if (make <= 0) LOG.debug("[nusmp] soul zombies: none made for {} (live {}, wanted {}, cap {}, server {}/{})", owner.getGameProfile().getName(), Corpses.ZOMBIES.count(id), n, cap, Corpses.ZOMBIES.total(), CorpseConfig.zombieGlobalCap());
        for (int k = 0; k < make; k++) {
            SoulZombieEntity z = CorpseEntities.SOUL_ZOMBIE.get().create(level);
            if (z == null) { LOG.debug("[nusmp] soul zombies: the world refused to create a soul_zombie (feature flags?)"); continue; }
            Vec3 spot = k == 0 && firstAt != null ? firstAt : Corpses.findSpot(level, z.getType().getDimensions(), owner.position(), 1.5, 4.0, owner.getRandom(), false);
            if (spot == null) spot = owner.position();
            z.moveTo(spot.x, spot.y, spot.z, owner.getYRot() + 180f, 0f);
            z.setOwner(id);
            AttributeInstance hp = z.getAttribute(Attributes.MAX_HEALTH), atk = z.getAttribute(Attributes.ATTACK_DAMAGE);
            if (hp != null) hp.setBaseValue(SoulZombieRules.health(scale));
            if (atk != null) atk.setBaseValue(SoulZombieRules.attack(scale));
            z.setHealth(z.getMaxHealth());
            CompoundTag t = new CompoundTag();
            t.putUUID("Owner", id);
            t.putLong("Born", level.getGameTime());
            t.putInt("Life", life);
            t.putString("Kind", "zombie");
            z.getPersistentData().put(Corpses.TAG, t);
            if (!level.addFreshEntity(z)) { LOG.debug("[nusmp] soul zombies: addFreshEntity refused a zombie at {}", spot); continue; }
            for (UUID old : Corpses.ZOMBIES.add(id, z.getUUID(), cap)) Corpses.crumbleById(owner.getServer(), old);
            Corpses.mark(z, 0);
            Fx.spray(level, FxThemes.CORPSE, FxTone.LIGHT, z.getX(), z.getY() + 0.3, z.getZ(), 10, 0.35, 0.25, 0.35, 0.05);
            Fx.ring(level, FxThemes.CORPSE, FxTone.DARK, z.getX(), z.getY() + 0.1, z.getZ(), 0.9, 12);
            out.add(z);
        }
        if (!out.isEmpty()) {
            SoulZombieEntity first = out.get(0);
            CorpseVfx.raise(level, first.position(), 1.0f, 30);
            level.playSound(null, first.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
        }
        return out;
    }

    /** Why the last request made none, for the action bar: the server is full, or the world refused the body. */
    public static String refusal(UUID owner) {
        return Corpses.ZOMBIES.total() >= CorpseConfig.zombieGlobalCap() && Corpses.ZOMBIES.count(owner) == 0
                ? "Too many risen dead on this server right now." : "The dead cannot rise here.";
    }

    /** How many zombies the owner has standing right now. */
    public static int count(UUID owner) { return Corpses.ZOMBIES.count(owner); }
}
