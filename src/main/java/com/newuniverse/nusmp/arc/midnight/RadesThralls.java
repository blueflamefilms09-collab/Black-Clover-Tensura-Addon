package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

import java.util.UUID;

/**
 * Why: Rades Spirito's army is the dead of the Royal Capital, and in the story they were plain zombies that the party cut down by the dozen. The fight needs
 * those bodies (the ward of his corpses thickens with each one that stands), but it must not need a new entity type, a model and a renderer for them: a vanilla
 * zombie is exactly what they were. This class makes one, tags it with the boss that raised it, and takes away everything that would make an army a farm:
 * a risen corpse drops nothing and gives no experience, and one that comes back from a saved chunk (the boss it served is long gone) is never let into the
 * world. The tag is the whole contract: {@link #isThrall} is how the rest of the mod can ask "is this one of his?". The two drop handlers and the join
 * handler are registered once, when the boss class loads.
 */
public final class RadesThralls {
    private RadesThralls() {}

    /** The persistent-data key of a risen corpse: the UUID of the boss that raised it. */
    public static final String TAG = "nusmp_rades_thrall";

    private static boolean installed;

    /** Registers the three handlers, once. */
    static synchronized void install() {
        if (installed) return;
        installed = true;
        NeoForge.EVENT_BUS.addListener(RadesThralls::onDrops);
        NeoForge.EVENT_BUS.addListener(RadesThralls::onXp);
        NeoForge.EVENT_BUS.addListener(RadesThralls::onJoin);
    }

    public static boolean isThrall(Entity e) { return e != null && e.getPersistentData().hasUUID(TAG); }

    /** The boss that raised this corpse, or null. */
    public static UUID ownerOf(Entity e) { return isThrall(e) ? e.getPersistentData().getUUID(TAG) : null; }

    static void onDrops(LivingDropsEvent e) {
        if (isThrall(e.getEntity())) e.setCanceled(true);
    }

    static void onXp(LivingExperienceDropEvent e) {
        if (isThrall(e.getEntity())) e.setCanceled(true);
    }

    /** A risen corpse that arrives from a saved chunk is a leftover of a stopped fight: it does not join the world. */
    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !e.loadedFromDisk()) return;
        if (isThrall(e.getEntity())) e.setCanceled(true);
    }

    /**
     * Raises one corpse at {@code at}, for {@code owner}, with the stats of the boss's power ({@link RadesMath}), already looking for {@code target}. Null if the
     * entity could not be made or placed.
     */
    static Zombie raise(ServerLevel sl, ZogratisBoss owner, Vec3 at, int power, LivingEntity target) {
        Zombie z = EntityType.ZOMBIE.create(sl);
        if (z == null) return null;
        z.moveTo(at.x, at.y, at.z, owner.getRandom().nextFloat() * 360f, 0f);
        z.setBaby(false);
        z.setPersistenceRequired();
        z.setCanPickUpLoot(false);
        set(z.getAttribute(Attributes.MAX_HEALTH), RadesMath.thrallHealth(power));
        z.setHealth((float) RadesMath.thrallHealth(power));
        set(z.getAttribute(Attributes.ATTACK_DAMAGE), RadesMath.thrallAttack(power));
        set(z.getAttribute(Attributes.MOVEMENT_SPEED), RadesMath.thrallSpeed(power));
        set(z.getAttribute(Attributes.FOLLOW_RANGE), 40.0);
        set(z.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE), 0.0);
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));        // keeps the sun off it in an open arena, and marks it as one of his
        z.setDropChance(EquipmentSlot.HEAD, 0f);
        z.setCustomName(Component.literal("Risen Corpse"));
        z.getPersistentData().putUUID(TAG, owner.getUUID());
        if (target != null && target.isAlive()) z.setTarget(target);
        return sl.addFreshEntity(z) ? z : null;
    }

    private static void set(AttributeInstance a, double v) {
        if (a != null) a.setBaseValue(v);
    }
}
