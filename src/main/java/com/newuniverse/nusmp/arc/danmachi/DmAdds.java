package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: three of the monsters call help with a roar (the Spartoi of Udaeus, the monsters of the Amphisbaena's Water City, the monsters that the Black
 * Goliath's roar brings), and a called monster is a plain ordinary mob in the world: it must belong to one boss, be counted against the boss's cap
 * ({@link AddPlan}), vanish when its time is up or the fight is over, and never become a farm (no drops, no experience). The mobs are vanilla ones
 * (skeletons, drowned, husks, spiders) so no new entity needs a model, a renderer or a registration; they are tagged in their persistent data with the
 * boss's id and the game tick they expire at, and found again from that tag. Nothing here changes a block.
 */
final class DmAdds {
    private DmAdds() {}

    /** The persistent-data keys of a called monster: the boss that called it (UUID) and the game tick it expires at. */
    static final String OWNER_KEY = "nusmp_dm_owner", UNTIL_KEY = "nusmp_dm_until";

    /** Which monsters a roar brings. */
    enum Kind { SPARTOI, WATER, DUNGEON }

    /** The grey of the dust a called monster leaves when its time is up (0xRRGGBB). */
    private static final int DUST = 0x8A8A9A;

    private static boolean listening;

    /** The kit's base health and attack of a called monster of this kind, before the power scaling of {@link AddPlan}. */
    static double baseHealth(Kind kind) { return kind == Kind.DUNGEON ? 30.0 : kind == Kind.WATER ? 40.0 : 36.0; }

    static double baseAttack(Kind kind) { return kind == Kind.DUNGEON ? 6.0 : 7.0; }

    /** Registers the two listeners that take the drops and the experience of called monsters, once, the first time a monster is called. */
    private static synchronized void ensureListeners() {
        if (listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener(DmAdds::onDrops);
        NeoForge.EVENT_BUS.addListener(DmAdds::onExperience);
    }

    private static void onDrops(LivingDropsEvent e) {
        if (isAdd(e.getEntity())) e.setCanceled(true);
    }

    private static void onExperience(LivingExperienceDropEvent e) {
        if (isAdd(e.getEntity())) e.setDroppedExperience(0);
    }

    /** Is this entity a monster that a DanMachi boss called? */
    static boolean isAdd(Entity e) { return e != null && e.getPersistentData().hasUUID(OWNER_KEY); }

    /** How far from the boss a called monster is still looked for (the arenas are 35 blocks across their radius). */
    private static final double RANGE = 80.0;

    /** The called monsters of this boss that are alive (found again from their tag, within {@link #RANGE} blocks). */
    static List<Mob> of(DanMachiBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return List.of();
        UUID me = boss.getUUID();
        AABB box = boss.getBoundingBox().inflate(RANGE);
        List<Mob> out = new ArrayList<>();
        for (Mob m : sl.getEntitiesOfClass(Mob.class, box, x -> x.isAlive() && x.getPersistentData().hasUUID(OWNER_KEY))) {
            CompoundTag d = m.getPersistentData();
            if (me.equals(d.getUUID(OWNER_KEY))) out.add(m);
        }
        return out;
    }

    static int alive(DanMachiBoss boss) { return of(boss).size(); }

    /**
     * Calls up to {@code count} monsters of {@code kind} on a ring of {@code radius} blocks round {@code around}, as many as the boss has room for (the caller
     * has already applied {@link AddPlan#toSummon}). Returns how many came.
     */
    static int summon(DanMachiBoss boss, Kind kind, int count, Vec3 around, double radius, DmFx.Style style) {
        if (count <= 0 || !(boss.level() instanceof ServerLevel sl)) return 0;
        ensureListeners();
        int made = 0;
        for (int i = 0; i < count; i++) {
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            Vec3 spot = ZogratisBoss.groundOrNull(sl, around.add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
            if (spot == null) continue;
            Mob mob = create(sl, kind, i);
            if (mob == null) continue;
            mob.moveTo(spot.x, spot.y, spot.z, boss.getRandom().nextFloat() * 360f, 0f);
            if (!sl.noCollision(mob)) continue;
            equip(mob, kind);
            scale(mob, kind, boss.power());
            CompoundTag d = mob.getPersistentData();
            d.putUUID(OWNER_KEY, boss.getUUID());
            d.putLong(UNTIL_KEY, sl.getGameTime() + AddPlan.lifetime(boss.power()));
            sl.addFreshEntity(mob);
            LivingEntity foe = nearestFoe(boss, spot);
            if (foe != null) mob.setTarget(foe);
            if (kind == Kind.SPARTOI) VfxSpawn.send(sl, VfxShape.CORPSE_RAISE, spot, spot.add(0, 1, 0), 0xFFC8B8FF, 24, 1.0f);
            DmFx.spotStrike(boss, style, spot, 1.2);
            made++;
        }
        if (made > 0) boss.markCalled();
        return made;
    }

    private static Mob create(ServerLevel sl, Kind kind, int index) {
        EntityType<? extends Mob> type = switch (kind) {
            case SPARTOI -> EntityType.SKELETON;
            case WATER -> EntityType.DROWNED;
            case DUNGEON -> index % 2 == 0 ? EntityType.HUSK : EntityType.SPIDER;
        };
        return type.create(sl);
    }

    /** What a called monster holds and wears: a sword and a helmet for a Spartoi (so it fights close and does not burn in the sun), nothing that drops. */
    private static void equip(Mob mob, Kind kind) {
        if (kind != Kind.SPARTOI) return;
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0f);
        mob.setDropChance(EquipmentSlot.HEAD, 0f);
    }

    private static void scale(Mob mob, Kind kind, int power) {
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(AddPlan.health(baseHealth(kind), power));
        mob.setHealth(mob.getMaxHealth());
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(AddPlan.attack(baseAttack(kind), power));
        AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(48.0);
    }

    private static LivingEntity nearestFoe(DanMachiBoss boss, Vec3 at) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : boss.foesAround(at, 40.0)) {
            double d = e.distanceToSqr(at.x, at.y, at.z);
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    /** Takes away the called monsters whose time is up (all of them when {@code everything}: the fight is over). Once a second is enough. */
    static void sweep(DanMachiBoss boss, boolean everything) {
        if (!(boss.level() instanceof ServerLevel sl) || !everything && !boss.calledAny()) return;
        long now = sl.getGameTime();
        List<Mob> mine = of(boss);
        if (mine.isEmpty()) boss.forgetCalled();
        for (Mob m : mine) {
            if (everything || now >= m.getPersistentData().getLong(UNTIL_KEY)) {
                Tells.burst(sl, m.position().add(0, m.getBbHeight() / 2, 0), DUST, 8);
                m.discard();
            }
        }
    }
}
