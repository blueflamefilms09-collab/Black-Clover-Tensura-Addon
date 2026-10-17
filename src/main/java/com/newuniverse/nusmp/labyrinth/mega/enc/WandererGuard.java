package com.newuniverse.nusmp.labyrinth.mega.enc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

import java.util.Iterator;

/**
 * Why: three rules about a wandering boss are really rules about the rest of the world, and each needs an event hook. (1) HIDING WORKS: a boss
 * with its own target goals would pick the nearest player by itself the moment the player came into its follow range, through a wall, in the
 * dark, crouching. The mind decides who may be hunted; this guard cancels every attempt to target a player that the mind has not approved
 * (the one exception, a blow taken, is passed to the mind on its next pass, so the boss still answers a player who hits it, half a second
 * later). Brains that set their target memory directly do not fire the event; the Director's pass releases such a target within half a second.
 * (2) NO SECOND WIND: the boss rules forbid healing a boss in combat, and they know only this mod's own bosses; a wandering boss can be any
 * body, so a heal of a boss struck within the last ten seconds is cancelled here, whatever the source (a regeneration effect, a skill, a potion).
 * (3) NO PROGRESSION ITEM FROM THE HALLS: the Floor Warden drops a Shadow Key by code, the key of the old Spire's doors; nothing a wandering boss
 * drops may open a door of another layout, so that item is removed from its drops (its other loot stays).
 */
public final class WandererGuard {
    private WandererGuard() {}

    private static final String SHADOW_KEY = "nusmp:shadow_key";
    /** A heal of a boss struck within this many ticks is refused. */
    private static final int COMBAT_TICKS = 200;

    public static void onChangeTarget(LivingChangeTargetEvent e) {
        try {
            if (!(e.getEntity() instanceof Mob mob) || !mob.getTags().contains(WandererDirector.TAG)) return;
            if (!(e.getNewAboutToBeSetTarget() instanceof Player next)) return;
            Wanderer w = WandererDirector.of(mob);
            if (w == null) return;
            if (next.getUUID().equals(w.mind.approvedTarget())) return;
            e.setCanceled(true);
        } catch (Throwable t) {
            // a guard that fails lets the target through: the Director's pass releases it within half a second
        }
    }

    public static void onHeal(LivingHealEvent e) {
        try {
            if (!(e.getEntity() instanceof Mob mob) || !mob.getTags().contains(WandererDirector.TAG)) return;
            if (mob.getLastHurtByMob() != null && mob.tickCount - mob.getLastHurtByMobTimestamp() < COMBAT_TICKS) e.setCanceled(true);
        } catch (Throwable t) {
            // never throw into the heal
        }
    }

    public static void onDrops(LivingDropsEvent e) {
        try {
            Level level = e.getEntity().level();
            if (level.isClientSide() || !e.getEntity().getTags().contains(WandererDirector.TAG)) return;
            Iterator<ItemEntity> it = e.getDrops().iterator();
            while (it.hasNext()) {
                ItemEntity item = it.next();
                if (SHADOW_KEY.equals(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString())) it.remove();
            }
        } catch (Throwable t) {
            // never throw into the drops
        }
    }
}
