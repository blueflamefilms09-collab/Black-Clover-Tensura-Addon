package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxRelic;

/**
 * Wave 4: the Amulet of the Forest's figurines. Touching one gives its spirit's blessing for a game day (one spirit at a time); the
 * numbers are in {@link Wave4Forest} and are applied once a second by {@link Wave4Upkeep}. Touching the peacock again while it blesses
 * you casts Charm Person on the humanoids within five blocks. A seventh act sends the spirit away.
 */
final class Wave4ForestActs {
    private Wave4ForestActs() {}

    static final int FOREST_GREEN = 0x7CE0A0;

    /** A figurine was touched. */
    static void touch(ActCtx c, Wave4Forest spirit) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        final long now = lv.getGameTime();
        ItemStack st = c.stack();
        CompoundTag t = Wave4Store.read(st);
        Wave4Forest cur = Wave4Forest.byId(t.getString(Wave4Store.K_SPIRIT));
        long until = t.getLong(Wave4Store.K_BLESSED_UNTIL);
        boolean on = cur != null && Wave4Forest.active(until, now);
        if (on && cur == spirit) {
            if (spirit == Wave4Forest.PEACOCK) { charm(c); return; }
            Fx.say(p, "The " + spirit.label + " spirit is with you for " + Wave4Forest.minutesLeft(until, now) + " more minutes.");
            return;
        }
        if (on) {
            Fx.say(p, "The " + cur.label + " spirit still walks with you: send it away first, then touch another figurine.");
            return;
        }
        final String id = spirit.id;
        Wave4Store.edit(st, x -> {
            x.putString(Wave4Store.K_SPIRIT, id);
            x.putLong(Wave4Store.K_BLESSED_UNTIL, now + Wave4Forest.DURATION);
        });
        Wave4Upkeep.forest(p);
        FxRelic.ring(lv, p.position().add(0, 0.2, 0), 2.0, FOREST_GREEN);
        FxRelic.burst(lv, p.position().add(0, 1.0, 0), FOREST_GREEN);
        Fx.say(p, "The " + spirit.label + " spirit answers: its blessing is yours for a day.");
    }

    /** Sends the blessing away early. */
    static void dismiss(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        CompoundTag t = Wave4Store.read(c.stack());
        Wave4Forest cur = Wave4Forest.byId(t.getString(Wave4Store.K_SPIRIT));
        if (cur == null || !Wave4Forest.active(t.getLong(Wave4Store.K_BLESSED_UNTIL), now)) {
            Fx.say(p, "No spirit walks with you.");
            return;
        }
        Wave4Store.edit(c.stack(), x -> {
            x.remove(Wave4Store.K_SPIRIT);
            x.remove(Wave4Store.K_BLESSED_UNTIL);
        });
        Wave4Upkeep.forest(p);
        Fx.say(p, "The " + cur.label + " spirit leaves you.");
    }

    /** Charm Person (the peacock): humanoids within 5 blocks forget you and will not turn on any player for 20 s; the mighty resist. */
    static void charm(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        long now = lv.getGameTime();
        List<Mob> mobs = lv.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(Wave4Charm.RADIUS), m -> m.isAlive() && isHumanoid(m) && !ownedBy(m, p.getUUID()));
        int charmed = 0, resisted = 0, seen = 0;
        for (Mob m : mobs) {
            if (seen++ >= Wave4Charm.MAX_TARGETS) break;
            UUID id = m.getUUID();
            if (Wave4State.CHARM_IMMUNE.active(id, now)) continue;
            if (Wave4Charm.resists(m.getMaxHealth())) {
                Wave4State.CHARM_IMMUNE.put(id, now + Wave4Charm.IMMUNE_TICKS, now);   // a failed save: immune for a day
                resisted++;
                continue;
            }
            Wave4State.CHARM.put(id, now + Wave4Charm.TICKS, now);
            Wave4Upkeep.forget(m);
            charmed++;
        }
        FxRelic.ring(lv, p.position().add(0, 0.5, 0), Wave4Charm.RADIUS, 0x3FD0B0);
        Fx.say(p, charmed + resisted == 0 ? "The peacock's display finds nobody to charm."
                : "Charm Person: " + charmed + " charmed for 20 seconds" + (resisted > 0 ? ", " + resisted + " shrugged it off (immune for a day)." : "."));
    }

    private static boolean isHumanoid(Mob m) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(m.getType());
        return key != null && Wave4Charm.humanoid(key.getNamespace(), key.getPath());
    }

    private static boolean ownedBy(Mob m, UUID player) { return m instanceof OwnableEntity o && player.equals(o.getOwnerUUID()); }
}
