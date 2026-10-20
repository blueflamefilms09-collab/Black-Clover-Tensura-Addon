package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import com.newuniverse.nusmp.particle.fx.FxRelic;

/**
 * Wave 4: the Beast Masters Amulet. Two acts: Store (the creature under your crosshair, within 6 blocks, goes into the amulet) and
 * Release (the last creature stored steps out in front of you, with the health it had). The amulet holds ten points; the weight of a
 * creature comes from its maximum health ({@link Wave4Beasts}). Your own tamed creatures go in freely; any other must first be
 * wounded to half its health. A stored creature's whole saved data rides in the amulet's custom data, so it stays itself.
 */
final class Wave4BeastActs {
    private Wave4BeastActs() {}

    static final int GREY = 0xB8C0CC;
    /** The persistent-data key the core puts on every creature a relic summons. */
    private static final String RELIC_SUMMON = "nusmp_relic_owner";
    /** Parts of a saved creature that must not be restored as they were. */
    private static final String[] DROP = {"UUID", "Passengers", "Leash", "Brain"};

    static void store(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        LivingEntity t = Wave4Look.target(p, Wave4Beasts.RANGE, true, e -> e instanceof Mob && !e.isPassenger() && !e.isVehicle());
        if (t == null) {
            Fx.say(p, "Look at a creature within " + (int) Wave4Beasts.RANGE + " blocks to draw it into the amulet.");
            return;
        }
        if (t.getPersistentData().hasUUID(RELIC_SUMMON)) {                       // a relic's own summon would outlive its time if it were let out again
            Fx.say(p, "A summoned creature cannot be bound.");
            return;
        }
        boolean owned = t instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID());
        if (!owned && t instanceof OwnableEntity o && o.getOwnerUUID() != null) {
            Fx.say(p, "It belongs to someone else: the amulet will not take it.");
            return;
        }
        int w = Wave4Beasts.weight(t.getMaxHealth());
        if (w == 0) {
            Fx.say(p, "It is too mighty for the amulet.");
            return;
        }
        if (!Wave4Beasts.willing(owned, t.getHealth(), t.getMaxHealth())) {
            Fx.say(p, "It fights the amulet. Wound it to half its health first, or tame it.");
            return;
        }
        CompoundTag state = Wave4Store.read(c.stack());
        ListTag list = state.getList(Wave4Store.K_BEASTS, 10);
        int used = 0;
        for (int i = 0; i < list.size(); i++) used += list.getCompound(i).getInt("w");
        if (list.size() >= Wave4Beasts.MAX_STORED || !Wave4Beasts.fits(used, w)) {
            Fx.say(p, "The amulet is full (" + used + "/" + Wave4Beasts.CAPACITY + "); this one would take " + w + ".");
            return;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(t.getType());
        CompoundTag data = t.saveWithoutId(new CompoundTag());
        for (String k : DROP) data.remove(k);
        data.putString("id", key.toString());
        if (data.toString().length() > Wave4Beasts.MAX_DATA_CHARS) {
            Fx.say(p, "It carries too much about it to be bound.");
            return;
        }
        CompoundTag row = new CompoundTag();
        row.putInt("w", w);
        row.putString("n", t.getName().getString());
        row.put("d", data);
        Wave4Store.edit(c.stack(), x -> {
            ListTag l = x.getList(Wave4Store.K_BEASTS, 10);
            l.add(row);
            x.put(Wave4Store.K_BEASTS, l);
        });
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.5, 0);
        t.discard();
        FxRelic.burst(lv, at, GREY);
        Fx.say(p, row.getString("n") + " is drawn into the amulet (" + (used + w) + "/" + Wave4Beasts.CAPACITY + ").");
    }

    static void release(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        CompoundTag state = Wave4Store.read(c.stack());
        ListTag list = state.getList(Wave4Store.K_BEASTS, 10);
        if (list.isEmpty()) {
            Fx.say(p, "The amulet holds no creature.");
            return;
        }
        CompoundTag row = list.getCompound(list.size() - 1);
        Optional<Entity> made = EntityType.create(row.getCompound("d").copy(), lv);
        if (made.isEmpty()) {
            Fx.say(p, row.getString("n") + " cannot come back (its kind is gone from this world); it stays in the amulet.");
            return;
        }
        Entity e = made.get();
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 spot = p.position().add(dir.scale(2.0));
        e.moveTo(spot.x, p.getY(), spot.z, p.getYRot() + 180f, 0f);
        if (!lv.noCollision(e)) e.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot() + 180f, 0f);
        if (!lv.addFreshEntity(e)) {
            Fx.say(p, "There is no room to let it out here.");
            return;
        }
        final String name = row.getString("n");
        Wave4Store.edit(c.stack(), x -> {
            ListTag l = x.getList(Wave4Store.K_BEASTS, 10);
            if (!l.isEmpty()) l.remove(l.size() - 1);
            x.put(Wave4Store.K_BEASTS, l);
        });
        FxRelic.burst(lv, e.position().add(0, e.getBbHeight() * 0.5, 0), GREY);
        Fx.say(p, name + " steps out of the amulet.");
    }
}
