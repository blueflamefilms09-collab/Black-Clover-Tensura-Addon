package com.newuniverse.nusmp.elite.items.armor;

import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the "Nightsong" set bonus of the four Nightingale pieces ({@link NightingaleRules} holds the numbers). Checked once a second per player on the server: Speed I and, in the dark, Night Vision are
 * (re)applied for a few seconds so they vanish soon after a piece comes off; sneaking, the Lullaby heals the wearer and allied players nearby every two seconds, with Light Rukh as the only particles
 * (the owner's rule). Off with {@code nightingale.setBonus} or the master switch.
 */
public final class NightingaleBonus {
    private NightingaleBonus() {}

    /** How many Nightingale pieces the player wears. */
    public static int worn(Player p) {
        int n = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(s);
            if (st.getItem() instanceof NightingaleArmorItem a && a.getEquipmentSlot() == s) n++;
        }
        return n;
    }

    /** Game bus: the per-player tick. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.tickCount % 20 != 3) return;
        if (!EliteItemsConfig.enabled() || !EliteItemsConfig.get(EliteItemsConfig.NIGHTINGALE_BONUS)) return;
        if (!NightingaleRules.bonusActive(worn(p))) return;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, NightingaleRules.SPEED_TICKS, 0, true, false, true));
        ServerLevel level = p.serverLevel();
        BlockPos at = p.blockPosition();
        if (NightingaleRules.needsNightVision(level.getMaxLocalRawBrightness(at)))
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NightingaleRules.NIGHT_VISION_TICKS, 0, true, false, true));
        if (p.isShiftKeyDown() && NightingaleRules.lullabySecond(p.tickCount / 20)) lullaby(p, level);
    }

    private static void lullaby(ServerPlayer p, ServerLevel level) {
        p.heal(NightingaleRules.LULLABY_HEAL);
        for (Player ally : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(NightingaleRules.LULLABY_RADIUS), x -> x != p && x.isAlive() && x.isAlliedTo(p))) ally.heal(NightingaleRules.LULLABY_HEAL);
        Vec3 c = p.position().add(0, 1.0, 0);
        RukhFx.burst(level, c, RukhFx.Alignment.LIGHT, 3, 0.8);
    }
}
