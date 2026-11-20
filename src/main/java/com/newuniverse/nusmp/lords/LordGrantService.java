package com.newuniverse.nusmp.lords;

import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Why: the glue between a Lord weapon and the Tensura skill system, the same shape as {@code GreedGrantService}: the wielder holds the weapon's Lord skill (Lord of Slothful Weight, Lord of Green-Eyed Envy, Lord of Empty
 * Vanity, Seifort's Holy Judgement) while the weapon is theirs, and loses it when it is not (checked at login, at a bind, at a release and every few seconds from the scan). One map lookup per check, a change only when the
 * list is wrong. A player who removed the weapon's skill by hand ({@code sinweapons.SinSkillService#remove}) is not given it again.
 */
public final class LordGrantService {
    private LordGrantService() {}

    private static DeferredHolder<ManasSkill, ? extends ManasSkill> holder(LordRules.Sin s) {
        return switch (s) {
            case SLOTH -> NUSkills.LORD_OF_SLOTHFUL_WEIGHT;
            case ENVY -> NUSkills.LORD_OF_GREEN_EYED_ENVY;
            case VANITY -> NUSkills.LORD_OF_EMPTY_VANITY;
            case HOLY -> NUSkills.SEIFORT_HOLY_JUDGEMENT;
            default -> NUSkills.LORD_OF_GLUTTONOUS_GREED;
        };
    }

    private static Skills skillsOf(ServerPlayer p) {
        try {
            return SkillAPI.getSkillsFrom(p);
        } catch (RuntimeException noSkills) {
            return null;
        }
    }

    public static boolean has(ServerPlayer p, LordRules.Sin s) {
        Skills sk = skillsOf(p);
        return sk != null && sk.getSkill(holder(s).getId()).isPresent();
    }

    /** Makes the player's skill list right for the record. Returns true when something changed. */
    public static boolean sync(ServerPlayer p, LordRules.Sin s) {
        if (!s.isLord() || !LordData.bound()) return false;
        boolean should = LordConfig.enabled() && LordData.get().isOwner(s, p.getUUID()) && !offByHand(p, s);
        Skills skills = skillsOf(p);
        if (skills == null) return false;
        boolean has = skills.getSkill(holder(s).getId()).isPresent();
        if (should && !has) {
            ManasSkill skill = holder(s).get();
            skills.learnSkill(skill.createDefaultInstance(), Component.translatable("lords.msg.granted", skill.getName()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            return true;
        }
        if (!should && has && LordConfig.enabled()) {
            skills.forgetSkill(holder(s).getId(), Component.translatable("lords.msg.revoked", holder(s).get().getName()).withStyle(ChatFormatting.GRAY));
            return true;
        }
        return false;
    }

    public static void syncAll(ServerPlayer p) {
        for (LordRules.Sin s : LordRules.Sin.values()) if (s.isLord()) sync(p, s);
    }

    private static boolean offByHand(ServerPlayer p, LordRules.Sin s) {
        com.newuniverse.nusmp.sinweapons.SinRules.Kind k = switch (s) {
            case SLOTH -> com.newuniverse.nusmp.sinweapons.SinRules.Kind.SLOTH;
            case ENVY -> com.newuniverse.nusmp.sinweapons.SinRules.Kind.ENVY;
            case VANITY -> com.newuniverse.nusmp.sinweapons.SinRules.Kind.VANITY;
            default -> com.newuniverse.nusmp.sinweapons.SinRules.Kind.HOLY;
        };
        return com.newuniverse.nusmp.sinweapons.SinSkillService.isOff(p, k);
    }
}
