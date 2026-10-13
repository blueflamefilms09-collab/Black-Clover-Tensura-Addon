package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.mana.Heritage;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.multiverse.Race;
import com.newuniverse.nusmp.relics.RelicFlags;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * Amulet of the Huntress. Predator's Stealth: a sneaking wearer is only noticed by creatures within eight blocks, and creeps faster. Weapons of
 * Choice: +1 damage with blades, axes, tridents and arrows, doubled when the wearer fights with claws or teeth (a beastfolk, vampire or
 * lizard-kin blood, bare-handed); a sneaking blow on a creature that has not noticed them (the pounce) adds more.
 */
final class Wave5Huntress {
    private Wave5Huntress() {}

    static final String FLAG = "w5_huntress";
    private static final int SWEEP_CAP = 16;

    /** Slow tick (every second): the creeping speed, the cached "natural weapons" flag, and the mobs that lose a sneaking wearer. */
    static void tick(ServerPlayer p) {
        boolean worn = RelicFlags.has(p, FLAG);
        Wave5Attr.sync(p, Attributes.SNEAKING_SPEED, Wave5Attr.id("huntress_sneak"), 0.5, Operation.ADD_MULTIPLIED_BASE, worn);
        boolean stalking = worn && p.isShiftKeyDown();
        // Predator's Stealth in Tensura's terms: a sneaking wearer's presence is dimmed (stat) and the Presence Concealment skill is lent while they stalk
        Wave5Attr.sync(p, TensuraAttributes.PRESENCE_CONCEALMENT, Wave5Attr.id("huntress_stealth"), 0.3, Operation.ADD_VALUE, stalking);
        if (!worn) return;
        Wave5Data.player(p).putBoolean("natural", natural(p));
        if (!stalking) return;
        Wave5Skills.lend(p, Wave5Skills.PRESENCE_CONCEALMENT, 60);
        List<Mob> seen = p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32.0),
                m -> m.isAlive() && m.getTarget() == p && m.distanceToSqr(p) > Wave5Pure.HUNTRESS_SEEN_WITHIN * Wave5Pure.HUNTRESS_SEEN_WITHIN);
        int n = 0;
        for (Mob m : seen) {
            if (n++ >= SWEEP_CAP) break;
            forget(m);
        }
    }

    /** A creature far away is about to notice a sneaking wearer: it does not. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        if (e.getEntity().distanceToSqr(p) <= Wave5Pure.HUNTRESS_SEEN_WITHIN * Wave5Pure.HUNTRESS_SEEN_WITHIN) return;
        if (RelicFlags.has(p, FLAG)) e.setCanceled(true);
    }

    /** The wearer's own blow. */
    static void onOutgoing(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || !RelicFlags.has(p, FLAG)) return;
        Entity direct = src.getDirectEntity();
        boolean melee = direct == p;
        ItemStack weapon = p.getMainHandItem();
        boolean cutting = (melee && (weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES) || weapon.getItem() instanceof TridentItem)) || direct instanceof AbstractArrow;
        boolean natural = Wave5Data.hasPlayerData(p) && Wave5Data.player(p).getBoolean("natural");
        boolean claws = melee && weapon.isEmpty() && natural;
        if (!cutting && !claws) return;
        boolean pounce = p.isShiftKeyDown() && e.getEntity() instanceof Mob m && m.getTarget() != p;
        e.setAmount(e.getAmount() + Wave5Pure.huntressBonus(natural, pounce));
    }

    /** Claws and teeth: the races (or blood) that have them. */
    private static boolean natural(ServerPlayer p) {
        try {
            return isNatural(MultiverseProfile.race(p)) || isNatural(Heritage.blood(p));
        } catch (RuntimeException | LinkageError other) {
            return false;
        }
    }

    private static boolean isNatural(Race r) { return r == Race.BEASTFOLK || r == Race.VAMPIRE || r == Race.DRAGONEWT; }

    private static void forget(Mob m) {
        try {
            m.setTarget(null);
            m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            m.getNavigation().stop();
        } catch (RuntimeException | LinkageError otherBrain) {
            // a brain of another shape: the plain target slot is already clear
        }
    }
}
