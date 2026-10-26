package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Why: the Will of the Creator now gives one ultimate skill of five (instead of one of three attribute boons), once in a player's life. This is the one place that says which skill an
 * id means, whether the player already took theirs, and that gives it. The "already received" flag is the player's own key {@link #TAG_CHOICE} (kept through death and relogging, copied
 * with the player), and the claim is validated by id against {@link JudgmentRules#claimable}, so a packet or command can never name anything else. Server side.
 */
public final class VeldanavaSkills {
    private VeldanavaSkills() {}

    /** The player's key: the id of the skill the Creator gave them (empty or missing = none yet). */
    public static final String TAG_CHOICE = "nusmp_veldanava_skill";

    /** The skill for a card id, or null when this build has none (a sealed card). Releases add one line per skill. */
    public static DeferredHolder<ManasSkill, ? extends ManasSkill> holder(String id) {
        if ("sandalphon".equals(id)) return NUSkills.SANDALPHON;
        if ("israfil".equals(id)) return NUSkills.ISRAFIL;
        return null;
    }

    /** The id of the skill the player was given, or "" when none. */
    public static String chosen(Player p) { return p.getPersistentData().getString(TAG_CHOICE); }

    public static boolean received(Player p) { return !chosen(p).isEmpty(); }

    /** Gives the skill, once. False when the card is not claimable, the player already took theirs, or the skill is missing. Server side. */
    public static boolean grant(ServerPlayer p, String id) {
        if (!JudgmentRules.claimable(id) || received(p)) return false;
        DeferredHolder<ManasSkill, ? extends ManasSkill> h = holder(id);
        if (h == null) return false;
        var skills = SkillAPI.getSkillsFrom(p);
        if (skills.getSkill(h.getId()).isEmpty()) {
            boolean ok = skills.learnSkill(h.get().createDefaultInstance(), Component.literal("The Will of the Creator grants you " + JudgmentRules.card(id).title() + ".").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            if (!ok) return false;
        }
        p.getPersistentData().putString(TAG_CHOICE, id);
        return true;
    }

    /** Takes the skill and the flag back (the self-tests and an admin use it; the game never does). */
    public static void revoke(ServerPlayer p) {
        String id = chosen(p);
        p.getPersistentData().remove(TAG_CHOICE);
        DeferredHolder<ManasSkill, ? extends ManasSkill> h = holder(id);
        if (h != null) SkillAPI.getSkillsFrom(p).forgetSkill(h.getId());
    }
}
