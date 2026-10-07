package com.newuniverse.nusmp.entity;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 0.49: lets a mob (the Zagred boss, painted constructs, mirror doubles, spirit lords) cast Tensura's own magic.
 * <ul>
 *   <li>{@link #learn}: picks Tensura magic skills (classes under io.github.manasmods.tensura.ability.magic) from the skill
 *       registry by keyword and teaches them to the mob through ManasCore, exactly as a player learns them.</li>
 *   <li>{@link #cast}: faces the target and drives a skill the way a player's key does: onPressed, onHeld every tick while it
 *       charges, then onRelease. Cooldowns set by the skill itself are respected.</li>
 *   <li>A skill that throws (some Tensura skills may assume a player caster) is dropped from the kit and logged once, so a bad
 *       skill can never break the fight.</li>
 * </ul>
 */
public final class TensuraCaster {
    private TensuraCaster() {}

    static final String MAGIC_PACKAGE = "io.github.manasmods.tensura.ability.magic.";
    private static final Set<ResourceLocation> BROKEN = new HashSet<>();

    /** Teaches 'e' up to 'max' Tensura magic skills whose id contains one of the keywords (in keyword order). Returns their ids. */
    public static List<ResourceLocation> learn(LivingEntity e, int max, String... keywords) {
        List<ResourceLocation> out = new ArrayList<>();
        try {
            var registry = SkillAPI.getSkillRegistry();
            List<ResourceLocation> ids = new ArrayList<>(registry.getIds());
            ids.sort(null);
            var skills = SkillAPI.getSkillsFrom(e);
            for (String k : keywords) {
                for (ResourceLocation id : ids) {
                    if (out.size() >= max) return out;
                    if (out.contains(id) || BROKEN.contains(id) || !"tensura".equals(id.getNamespace())) continue;
                    String path = id.getPath().toLowerCase(Locale.ROOT);
                    if (!path.contains(k) || path.contains("barrier") || path.contains("summon")) continue;
                    ManasSkill skill = registry.get(id);
                    if (skill == null || !skill.getClass().getName().startsWith(MAGIC_PACKAGE)) continue;
                    if (skills.getSkill(id).isEmpty()) skills.learnSkill(skill.createDefaultInstance());
                    out.add(id);
                }
            }
            skills.markDirty();
        } catch (Throwable t) {
            LogUtils.getLogger().warn("[nusmp] could not teach Tensura skills to {}: {}", e.getName().getString(), t.toString());
        }
        return out;
    }

    /** Gives 'e' a magicule pool to cast from (Tensura's skills spend it like a player's). */
    public static void ensureMana(LivingEntity e, double maxMagicule) {
        try {
            if (EnergyHelper.getMaxMagicule(e) < maxMagicule) EnergyHelper.setMaxMagicule(e, maxMagicule);
            var ex = TensuraStorages.getExistenceFrom(e);
            if (ex != null && ex.getMagicule() < maxMagicule * 0.5) { ex.setMagicule(EnergyHelper.getMaxMagicule(e)); ex.markDirty(); }
        } catch (Throwable ignored) {}
    }

    /**
     * Casts one ready skill from 'kit' at 'target': aims, presses, holds for 'hold' ticks, releases. Returns the skill cast,
     * or null if none was ready.
     */
    public static ResourceLocation cast(Mob caster, LivingEntity target, List<ResourceLocation> kit, int hold) {
        if (kit.isEmpty() || !(caster.level() instanceof ServerLevel sl)) return null;
        var skills = SkillAPI.getSkillsFrom(caster);
        List<ResourceLocation> order = new ArrayList<>(kit);
        java.util.Collections.shuffle(order, new java.util.Random(caster.getRandom().nextLong()));
        for (ResourceLocation id : order) {
            if (BROKEN.contains(id)) continue;
            ManasSkillInstance inst = skills.getSkill(id).orElse(null);
            if (inst == null) continue;
            int mode = inst.getModes() <= 1 ? 0 : caster.getRandom().nextInt(inst.getModes());
            if (inst.onCoolDown(mode)) continue;
            aim(caster, target);
            if (!guard(id, () -> inst.onPressed(caster, 0, mode))) continue;
            int[] held = {0};
            SpellRuntime.zone(sl, hold, 1, age -> {
                if (!caster.isAlive() || BROKEN.contains(id)) return;
                aim(caster, target);
                held[0]++;
                guard(id, () -> inst.onHeld(caster, held[0], mode));
            });
            SpellRuntime.later(sl, hold + 1, () -> {
                if (!caster.isAlive() || BROKEN.contains(id)) return;
                aim(caster, target);
                guard(id, () -> inst.onRelease(caster, held[0], 0, mode));
                skills.markDirty();
            });
            return id;
        }
        return null;
    }

    static void aim(Mob caster, LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        caster.getLookControl().setLookAt(target, 360, 360);
        caster.lookAt(target, 360, 360);
        caster.setYHeadRot(caster.getYRot());
    }

    /** Runs a skill call; if it throws, the skill is marked broken for every caster and false is returned. */
    static boolean guard(ResourceLocation id, Runnable call) {
        try { call.run(); return true; }
        catch (Throwable t) {
            if (BROKEN.add(id)) LogUtils.getLogger().warn("[nusmp] Tensura skill {} can't be cast by a mob, dropped: {}", id, t.toString());
            return false;
        }
    }
}
