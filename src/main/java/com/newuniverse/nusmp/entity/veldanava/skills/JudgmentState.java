package com.newuniverse.nusmp.entity.veldanava.skills;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the passives of a Veldanava skill act inside damage and projectile events, and an event must not walk a player's skill list on every hit. The skill keeps its toggles in its own
 * instance and copies them, once a second and at every change, into one int on the player ({@link #MASK}); the events read that int. The mastered flag rides in bit 4. Bits 0..3 are the
 * four toggle modes. Server thread only.
 */
public final class JudgmentState {
    private JudgmentState() {}

    public static final String MASK = "nusmp_judgment_mask";
    public static final int MASTERED_BIT = 16, TOGGLES = 15, SKILL_SHIFT = 5, SKILL_BITS = 0xE0;

    /** The toggles and the mastered bit of a creature (0 for everything that is not a player). */
    public static int mask(LivingEntity e) {
        if (!(e instanceof Player)) return 0;
        CompoundTag d = e.getPersistentData();
        return d.contains(MASK) ? d.getInt(MASK) : 0;
    }

    /** The code of the skill whose toggles the mask holds (see {@link JudgmentRules#code}); 0 = none, or a mask written before 3.14.0, which was always Sandalphon's. */
    public static int skill(LivingEntity e) { return (mask(e) & SKILL_BITS) >> SKILL_SHIFT; }

    /** The mask a skill writes: its toggles, the mastered bit and its own code. */
    public static int compose(int code, int toggles, boolean mastered) {
        return (toggles & TOGGLES) | (mastered ? MASTERED_BIT : 0) | ((code << SKILL_SHIFT) & SKILL_BITS);
    }

    /** True when toggle {@code mode} of Sandalphon is on (the passives of 3.13.0 read this; another skill's toggles never switch them on). */
    public static boolean on(LivingEntity e, int mode) {
        int c = skill(e);
        return (c == 0 || c == 1) && (mask(e) & JudgmentRules.bit(mode)) != 0;
    }

    /** True when toggle {@code mode} of the skill with this code is on. */
    public static boolean onFor(LivingEntity e, int code, int mode) {
        return skill(e) == code && (mask(e) & JudgmentRules.bit(mode)) != 0;
    }

    public static boolean mastered(LivingEntity e) { return (mask(e) & MASTERED_BIT) != 0; }

    public static void write(ServerPlayer p, int mask) {
        if (mask == 0) p.getPersistentData().remove(MASK);
        else p.getPersistentData().putInt(MASK, mask);
    }

    // ---------------------------------------------------------------- the instance's own toggles
    private static final String TAG_TOGGLES = "toggles";

    public static int toggles(ManasSkillInstance i) { return i.getOrCreateTag().getInt(TAG_TOGGLES) & TOGGLES; }

    public static void setToggles(ManasSkillInstance i, int toggles) {
        i.getOrCreateTag().putInt(TAG_TOGGLES, toggles & TOGGLES);
        i.markDirty();
    }

    // ---------------------------------------------------------------- aiming
    /** The creatures along the player's line of sight, nearest first, at most {@code max}: the shot pierces blocks and creatures, so this is a segment test, not a block ray. */
    public static List<LivingEntity> along(ServerPlayer p, double range, double width, int max) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1f).scale(range));
        List<LivingEntity> all = com.newuniverse.nusmp.book.GrimoireBook.along(p, eye, end, width);
        all.removeIf(e -> !e.isAlive() || e.isSpectator());
        all.sort((a, b) -> Double.compare(a.distanceToSqr(eye), b.distanceToSqr(eye)));
        return all.size() > max ? new ArrayList<>(all.subList(0, max)) : all;
    }

    /** The nearest creature along the line of sight, or null. */
    public static LivingEntity first(ServerPlayer p, double range, double width) {
        List<LivingEntity> l = along(p, range, width, 1);
        return l.isEmpty() ? null : l.get(0);
    }
}
