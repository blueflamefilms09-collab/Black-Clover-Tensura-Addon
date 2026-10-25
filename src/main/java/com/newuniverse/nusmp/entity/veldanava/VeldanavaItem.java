package com.newuniverse.nusmp.entity.veldanava;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the loot of Veldanava is a handful of relics, and four of them do something when used: the Creator's Essence (the panic button), the Justice King
 * Michael Fragment (a barrier against one fatal blow a day, see {@link VeldanavaEvents}), the Covenant King Uriel Fragment (a dash), and the Nihility Fragment (held in the
 * off hand it makes whatever you strike unable to heal for a few seconds, see {@link VeldanavaEvents}). The rest (the Star King Dragon Core, the Genesis Fragment,
 * Veldanava's Divine Core) are materials with a description that says so. Everything happens on the server; the client only sees the result.
 */
public final class VeldanavaItem extends Item {
    public enum Kind { MATERIAL, ESSENCE, MICHAEL, URIEL, NIHILITY }

    /** The player's flag of the Michael barrier, and the day it last held. */
    public static final String TAG_MICHAEL = "nusmp_veldanava_michael", TAG_MICHAEL_DAY = "nusmp_veldanava_michael_day";
    private static final int DASH_BLOCKS = 14, DASH_COOLDOWN = 40;

    private final Kind kind;

    public VeldanavaItem(Kind kind, Rarity rarity, int stack) {
        super(new Item.Properties().stacksTo(stack).rarity(rarity).fireResistant());
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp) || kind == Kind.MATERIAL || kind == Kind.NIHILITY) {
            if (!level.isClientSide && kind == Kind.NIHILITY)
                player.displayClientMessage(Component.literal("Hold it in your off hand: what you strike cannot heal for a few seconds.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.pass(stack);
        }
        ServerLevel sl = sp.serverLevel();
        switch (kind) {
            case ESSENCE -> {
                essence(sp);
                stack.shrink(1);
            }
            case MICHAEL -> {
                if (sp.getPersistentData().getBoolean(TAG_MICHAEL)) {
                    sp.displayClientMessage(Component.literal("The barrier already protects you.").withStyle(ChatFormatting.GRAY), true);
                    return InteractionResultHolder.fail(stack);
                }
                sp.getPersistentData().putBoolean(TAG_MICHAEL, true);
                sp.displayClientMessage(Component.literal("Justice King Michael guards you: one fatal blow a day is turned away.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                sl.playSound(null, sp.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
                stack.shrink(1);
            }
            case URIEL -> {
                if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
                if (!dash(sp)) {
                    sp.displayClientMessage(Component.literal("There is no room to step through.").withStyle(ChatFormatting.GRAY), true);
                    return InteractionResultHolder.fail(stack);
                }
                sp.getCooldowns().addCooldown(this, DASH_COOLDOWN);
            }
            default -> { }
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** The panic button: full health and food, no bad effect, a short shield, the magicules full and every item cooldown cleared. */
    private static void essence(ServerPlayer p) {
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(20f);
        List<net.minecraft.core.Holder<MobEffect>> bad = new ArrayList<>();
        for (MobEffectInstance e : p.getActiveEffects()) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(e.getEffect());
        for (var e : bad) p.removeEffect(e);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 3));
        try {
            io.github.manasmods.tensura.util.EnergyHelper.gainMagicule(p, io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(p), io.github.manasmods.tensura.util.EnergyHelper.GainType.NORMAL);
        } catch (Throwable noTensuraData) { /* a stand-in player or no Tensura data: the rest still works */ }
        BuiltInRegistries.ITEM.forEach(i -> p.getCooldowns().removeCooldown(i));
        com.newuniverse.nusmp.heroweapon.skill.HeroUse.clearCooldowns(p.getUUID());
        p.displayClientMessage(Component.literal("The Creator's Essence floods you. Everything is as it was at the beginning.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    /** Steps up to {@link #DASH_BLOCKS} blocks along the look direction, to the furthest spot the body fits; no fall damage after. */
    private static boolean dash(ServerPlayer p) {
        Vec3 from = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        HitResult hit = p.level().clip(new ClipContext(from, from.add(look.scale(DASH_BLOCKS)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double reach = hit.getType() == HitResult.Type.MISS ? DASH_BLOCKS : Math.max(0, hit.getLocation().distanceTo(from) - 1.0);
        for (double d = reach; d >= 2.0; d -= 1.0) {
            Vec3 at = p.position().add(look.scale(d));
            if (p.level().noCollision(p, p.getBoundingBox().move(at.subtract(p.position())))) {
                p.teleportTo(at.x, at.y, at.z);
                p.fallDistance = 0f;
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.5f);
                return true;
            }
        }
        return false;
    }
}
