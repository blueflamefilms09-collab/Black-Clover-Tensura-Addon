package com.newuniverse.nusmp.elite.skills.evolution;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.EliteSkillsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the {@code kill_evolution} trigger needs "how many foes did this player defeat, and how many of them while owning THIS skill". Nothing in the game keeps that, so this class keeps it in a
 * compound of the player's own persistent data ({@code nusmp_elite_kills}): the totals per filter (any, boss, monster, undead) and, for every skill that has an evolution path in the
 * data file, the same totals counted only while the skill was owned (so mastering a skill and then killing with it is what counts, not a stockpile of kills from before). Players, armour stands and
 * kills of one's own pets are never counted, so the number cannot be farmed by PvP or by killing your own animals. Switchable by {@code evolution.killTracking}.
 */
public final class KillTracker {
    private KillTracker() {}

    public static final String ROOT = "nusmp_elite_kills";
    public static final List<String> FILTERS = List.of("any", "boss", "monster", "undead");

    public static String totalKey(String filter) { return "total." + norm(filter); }

    public static String withKey(String skillId, String filter) { return "with." + skillId + "." + norm(filter); }

    private static String norm(String filter) {
        String f = filter == null ? "any" : filter.toLowerCase(Locale.ROOT);
        return FILTERS.contains(f) ? f : "any";
    }

    public static int get(ServerPlayer p, String key) { return p.getPersistentData().getCompound(ROOT).getInt(key); }

    /** Which filters a victim counts under (always "any"). */
    public static List<String> filtersOf(boolean boss, boolean monster, boolean undead) {
        List<String> out = new ArrayList<>(List.of("any"));
        if (boss) out.add("boss");
        if (monster) out.add("monster");
        if (undead) out.add("undead");
        return out;
    }

    /** A kill by a player. Counts it under the totals and under every owned skill that can evolve. */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (!EliteSkillsConfig.get(EliteSkillsConfig.KILL_TRACKING) || victim instanceof Player || victim instanceof ArmorStand) return;
        if (victim instanceof net.minecraft.world.entity.OwnableEntity own && own.getOwnerUUID() != null && own.getOwnerUUID().equals(killer.getUUID())) return;
        List<String> filters = filtersOf(BalanceLaw.isBoss(victim), victim instanceof Enemy, victim.getType().is(EntityTypeTags.UNDEAD));
        CompoundTag root = killer.getPersistentData().getCompound(ROOT);
        for (String f : filters) bump(root, totalKey(f));
        Set<String> bases = EvolutionRuntime.baseSkills();
        for (String base : bases) {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(base);
            if (rl == null || io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(killer).getSkill(rl).isEmpty()) continue;
            for (String f : filters) bump(root, withKey(base, f));
        }
        killer.getPersistentData().put(ROOT, root);
    }

    private static void bump(CompoundTag root, String key) { root.putInt(key, root.getInt(key) + 1); }
}
