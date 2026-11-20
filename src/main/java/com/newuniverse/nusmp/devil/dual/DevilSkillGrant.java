package com.newuniverse.nusmp.devil.dual;

import com.newuniverse.nusmp.devil.DevilConfig;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: any devil magic gives the contracted player the Ultimate skill Devil Magic and the intrinsic skill Devil Bond, the way the other Ultimates are given (Shishou: {@code ShishouGrantService}): learned once
 * when the contract exists, forgotten when it ends. {@link #sync} is the whole mechanism and is idempotent: it asks the pure {@link DualRules#verdict}, so asking twice never grants twice and a player with no
 * contract never keeps either skill. It is called from {@code DevilContracts.syncNow}, which already runs at every moment the contract changes (sign, grant, clear, dissolve, login, respawn, dimension change).
 * Cost: two skill lookups per call. {@code devilDual.ultimate.enabled = false} / {@code devilDual.intrinsic.enabled = false} grant nothing and take the skill back at the next sync.
 */
public final class DevilSkillGrant {
    private DevilSkillGrant() {}

    private static Skills skillsOf(ServerPlayer p) {
        try {
            return SkillAPI.getSkillsFrom(p);
        } catch (RuntimeException noSkills) {                                                 // a player without skill data (a very early login)
            return null;
        }
    }

    public static boolean has(ServerPlayer p) {
        Skills s = skillsOf(p);
        return s != null && s.getSkill(NUSkills.DEVIL_MAGIC.getId()).isPresent();
    }

    public static boolean hasBond(ServerPlayer p) {
        Skills s = skillsOf(p);
        return s != null && s.getSkill(NUSkills.DEVIL_BOND.getId()).isPresent();
    }

    public static boolean hasAwakening(ServerPlayer p) {
        Skills s = skillsOf(p);
        return s != null && s.getSkill(NUSkills.DEVIL_AWAKENING.getId()).isPresent();
    }

    /** Makes the player's skill list right for the contract: a holder has Devil Magic and Devil Bond once each, anybody else has neither. Returns the verdict applied to Devil Magic. */
    public static DualRules.Verdict sync(ServerPlayer p, DevilContract c) {
        Skills skills = skillsOf(p);
        if (skills == null) return DualRules.Verdict.KEEP;
        boolean contract = DevilConfig.enabled() && c != null && c.hasContract();
        DualRules.Verdict v = one(skills, NUSkills.DEVIL_MAGIC.getId(), NUSkills.DEVIL_MAGIC.get(), contract && DualConfig.ultimate(),
                "granted", "The devil's power takes shape: the Ultimate skill Devil Magic is yours.", "revoked", "The bond is gone: Devil Magic fades from you.");
        one(skills, NUSkills.DEVIL_BOND.getId(), NUSkills.DEVIL_BOND.get(), contract && DualConfig.intrinsic(),
                "bond_granted", "The devil settles in your body: the intrinsic skill Devil Bond is yours.", "bond_revoked", "The bond is gone: Devil Bond fades from you.");
        boolean awakening = contract && DevilConfig.masteryOn();                                  // Devil Awakening (docs/devil_mastery.md): the extra skill with its own mastery
        DualRules.Verdict a = one(skills, NUSkills.DEVIL_AWAKENING.getId(), NUSkills.DEVIL_AWAKENING.get(), awakening,
                "awaken_granted", "The devil answers: the extra skill Devil Awakening is yours. Master it to wake more of its power.", "awaken_revoked", "The bond is gone: Devil Awakening fades from you.");
        if (a == DualRules.Verdict.GRANT) mirror(p, c);
        return v;
    }

    /** The skill's own mastery bar follows the contract's mastery (percent of the skill's maximum); a no-op without the skill. */
    public static void mirror(ServerPlayer p, DevilContract c) {
        Skills skills = skillsOf(p);
        if (skills == null || c == null || !c.hasContract()) return;
        skills.getSkill(NUSkills.DEVIL_AWAKENING.getId()).ifPresent(i -> {
            double want = com.newuniverse.nusmp.devil.mastery.Mastery.of(c) / 100.0 * i.getMaxMastery();
            if (Math.abs(i.getMastery() - want) > 0.5) { i.setMastery(want); i.markDirty(); }
        });
    }

    private static DualRules.Verdict one(Skills skills, ResourceLocation id, ManasSkill skill, boolean should, String gKey, String gText, String rKey, String rText) {
        DualRules.Verdict v = DualRules.verdict(should, skills.getSkill(id).isPresent());
        if (v == DualRules.Verdict.GRANT) {
            skills.learnSkill(skill.createDefaultInstance(), Component.translatableWithFallback("anime_multiverse.devil.dual." + gKey, gText).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        } else if (v == DualRules.Verdict.REVOKE) {
            skills.forgetSkill(id, Component.translatableWithFallback("anime_multiverse.devil.dual." + rKey, rText).withStyle(ChatFormatting.GRAY));
        }
        return v;
    }
}
