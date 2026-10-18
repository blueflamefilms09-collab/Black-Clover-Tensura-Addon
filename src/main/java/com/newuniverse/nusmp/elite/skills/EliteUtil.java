package com.newuniverse.nusmp.elite.skills;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Why: the elite skills keep needing the same few small operations that have no home in the rebuilt-skill helpers: find a player's instance of a skill, move magicules without a message, keep a
 * little data on an item stack (a rune, a temper), and read the vanilla moon. They live here once so each skill reads as intent. Nothing here changes the world.
 */
public final class EliteUtil {
    private EliteUtil() {}

    /** The player's instance of the skill, if they own it. */
    public static Optional<ManasSkillInstance> instance(LivingEntity owner, Supplier<? extends ManasSkill> skill) {
        if (owner == null) return Optional.empty();
        return SkillAPI.getSkillsFrom(owner).getSkill(skill.get().getRegistryName());
    }

    public static boolean owns(LivingEntity owner, Supplier<? extends ManasSkill> skill) { return instance(owner, skill).isPresent(); }

    // ---------------------------------------------------------------- magicules without a message
    public static double magicule(LivingEntity e) {
        IExistence ex = TensuraStorages.getExistenceFrom(e);
        return ex == null ? 0.0 : ex.getMagicule();
    }

    public static double maxMagicule(LivingEntity e) {
        try { return EnergyHelper.getMaxMagicule(e); } catch (Throwable notReady) { return 0.0; }
    }

    /** Adds (or, negative, removes) magicules, clamped to 0..max. Returns the change actually made. */
    public static double addMagicule(LivingEntity e, double amount) {
        IExistence ex = TensuraStorages.getExistenceFrom(e);
        if (ex == null || amount == 0) return 0.0;
        double max = maxMagicule(e);
        double before = ex.getMagicule();
        double after = Math.max(0.0, Math.min(max > 0 ? max : before + Math.max(0.0, amount), before + amount));
        ex.setMagicule(after);
        ex.markDirty();
        return after - before;
    }

    /** Takes the magicules only if all are there (no message). */
    public static boolean tryDrain(LivingEntity e, double amount) {
        if (amount <= 0) return true;
        IExistence ex = TensuraStorages.getExistenceFrom(e);
        if (ex == null || ex.getMagicule() < amount) return false;
        ex.setMagicule(ex.getMagicule() - amount);
        ex.markDirty();
        return true;
    }

    public static double ep(LivingEntity e) {
        IExistence ex = TensuraStorages.getExistenceFrom(e);
        return ex == null ? 0.0 : ex.getEP();
    }

    // ---------------------------------------------------------------- item data
    public static CompoundTag itemTag(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    public static void editItemTag(ItemStack stack, java.util.function.Consumer<CompoundTag> edit) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, edit);
    }

    // ---------------------------------------------------------------- the moon
    public static int moonPhase(net.minecraft.world.level.Level level) { return level.getMoonPhase(); }

    /** A game day number (game time / 24000), the unit of daily caps. */
    public static long day(net.minecraft.world.level.Level level) { return level.getGameTime() / 24000L; }
}
