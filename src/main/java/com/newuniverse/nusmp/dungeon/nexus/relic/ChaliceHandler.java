package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.dungeon.DungeonEffects;
import com.newuniverse.nusmp.dungeon.DungeonFood;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.UUID;

/**
 * The Glutton's Chalice (docs/living_dungeon.md section 7). Worn as a charm and attuned, it digests the Dungeon's raw monster meat into a
 * rich stew: no sickness, a full stomach and saturation, and for a few minutes ({@link DigestTable}) the thin air, the scorched floor and
 * the magicule radiation cannot touch the wearer. Its curse, Symbiotic Hunger ({@link HungerClock}): it is alive, and unfed for three
 * in-game days it drinks magicules from its wearer (since 1.99.0 never the food bar: the hunger ban) until it is fed again. The wearer is warned at two days, at
 * two and a half, and when it starts. The immunity ends the moment the chalice comes off, and on death, relog and a change of dimension.
 */
final class ChaliceHandler {
    private ChaliceHandler() {}

    /** Is this stack one of the Dungeon's raw cuts (or ruined meat)? */
    static boolean isRawMeat(ItemStack s) {
        return !s.isEmpty() && (s.getItem() instanceof DungeonFood.RawMeat || s.is(DungeonFood.RUINED_MEAT.get()));
    }

    private static int nutritionOf(ItemStack s) { return DigestTable.nutrition(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath()); }

    // ---------------------------------------------------------------- eating
    /** A meal of raw meat is finished: with the chalice worn, it is a stew. */
    static void onEat(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !isRawMeat(e.getItem())) return;
        ItemStack chalice = Worn.stack(p, Relic.GLUTTONS_CHALICE);
        if (chalice.isEmpty()) return;
        int nutrition = nutritionOf(e.getItem());
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        UUID id = p.getUUID();
        p.removeEffect(DungeonEffects.MAGICULE_SICKNESS);
        p.removeEffect(MobEffects.HUNGER);
        FoodData food = p.getFoodData();
        food.setFoodLevel(DigestTable.foodAfter(food.getFoodLevel(), nutrition));
        food.setSaturation(food.getFoodLevel());
        RelicState.IMMUNE.put(id, DigestTable.extend(now, RelicState.IMMUNE.getOrDefault(id, 0L), nutrition));
        markFed(chalice, now);
        long left = RelicState.IMMUNE.get(id) - now;
        p.displayClientMessage(Component.literal("The Chalice turns the meat into a rich stew. The Dungeon's air cannot hurt you for " + (left / 1200) + " min "
                + ((left % 1200) / 20) + " s.").withStyle(ChatFormatting.GREEN), true);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0f, 0.6f);
        VfxSpawn.sendFollowing(level, VfxShape.ELF_CIRCLE, p, p.position().add(0, 0.08, 0), 0xFF9CFFB4, 40, 1.4f);
    }

    /** Using the chalice in the main hand with raw meat in the off-hand: the chalice drinks it, the player is not fed. */
    static void feedByHand(ServerPlayer p, ItemStack chalice) {
        ItemStack meat = p.getOffhandItem();
        if (!isRawMeat(meat)) return;
        if (!p.isCreative()) meat.shrink(1);
        ServerLevel level = p.serverLevel();
        markFed(chalice, level.getGameTime());
        p.displayClientMessage(Component.literal("The Chalice drinks the meat down, and settles.").withStyle(ChatFormatting.GREEN), true);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, 0.5f);
    }

    private static void markFed(ItemStack chalice, long now) {
        chalice.set(RelicComponents.FED_AT.get(), HungerClock.fed(now));
        chalice.set(RelicComponents.HUNGER.get(), HungerClock.Phase.FED.ordinal());
    }

    // ---------------------------------------------------------------- the immunity (a few times a second)
    /** Keeps the bad air out while the stew lasts; ends it the moment the chalice is not worn. */
    static void shield(ServerPlayer p, int mask) {
        UUID id = p.getUUID();
        if (!RelicState.IMMUNE.containsKey(id)) return;
        long now = p.level().getGameTime();
        if ((mask & Relic.GLUTTONS_CHALICE.bit()) == 0 || !RelicState.immune(id, now)) {
            RelicState.IMMUNE.remove(id);
            return;
        }
        if (p.hasEffect(DungeonEffects.SUFFOCATION)) p.removeEffect(DungeonEffects.SUFFOCATION);
    }

    /** Hot floors and the thin air's drowning do nothing to a wearer still digesting. */
    static void onDamage(LivingIncomingDamageEvent e) {
        if (RelicState.IMMUNE.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        DamageSource src = e.getSource();
        boolean heat = src.is(DamageTypes.HOT_FLOOR), thin = src.is(DamageTypes.DROWN) && p.getAirSupply() > 0;     // really drowning (no air) still hurts
        if (!heat && !thin) return;
        if (RelicState.immune(p.getUUID(), p.level().getGameTime()) && Worn.has(p, Relic.GLUTTONS_CHALICE)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- the hunger (once a second)
    static void hunger(ServerPlayer p, int mask) {
        if ((mask & Relic.GLUTTONS_CHALICE.bit()) == 0) return;
        ItemStack chalice = Worn.stack(p, Relic.GLUTTONS_CHALICE);
        if (chalice.isEmpty()) return;
        long now = p.level().getGameTime();
        Long fedAt = chalice.get(RelicComponents.FED_AT.get());
        if (fedAt == null) {                                                    // one that came from a command starts fed
            markFed(chalice, now);
            return;
        }
        long since = HungerClock.since(now, fedAt);
        HungerClock.Phase phase = HungerClock.phase(since);
        Integer shown = chalice.get(RelicComponents.HUNGER.get());
        int before = shown == null ? 0 : shown;
        if (before != phase.ordinal()) {
            chalice.set(RelicComponents.HUNGER.get(), phase.ordinal());
            if (phase.ordinal() > before) announce(p, phase);
        }
        if (phase != HungerClock.Phase.STARVING || p.isCreative() || p.isSpectator()) return;
        // 1.99.0, THE HUNGER BAN: the Chalice no longer takes food points from its wearer (HungerClock.foodLoss is 0); it drinks magicules only
        double share = HungerClock.magiculeLoss(since);
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null && share > 0) {
            double max = EnergyHelper.getMaxMagicule(p);
            ex.setMagicule(Math.max(0.0, ex.getMagicule() - max * share));
            ex.markDirty();
        }
        if (RelicState.due(p.getUUID(), "chalice_drink", now, 200)) {
            p.displayClientMessage(Component.literal("The Chalice drinks from you. Feed it raw monster meat.").withStyle(ChatFormatting.DARK_RED), true);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8f, 0.7f);
        }
    }

    private static void announce(ServerPlayer p, HungerClock.Phase phase) {
        switch (phase) {
            case STIRRING -> p.displayClientMessage(Component.literal("The Chalice stirs. It will want feeding soon.").withStyle(ChatFormatting.DARK_GREEN), true);
            case HUNGRY -> p.displayClientMessage(Component.literal("The Chalice is hungry. Feed it raw monster meat before it feeds on you.").withStyle(ChatFormatting.YELLOW), true);
            case STARVING -> p.displayClientMessage(Component.literal("The Chalice is starving. It drinks from you until it is fed!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            default -> { }
        }
    }
}
