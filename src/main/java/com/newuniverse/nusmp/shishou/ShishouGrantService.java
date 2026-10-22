package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * Why: the glue between Shishou and the Tensura skill system (2.1.0). It gives the wielder the Ultimate skill {@code nusmp:shishou} when he claims the sword, takes it away when the sword is no longer theirs
 * (checked at login, at a claim, and every {@link ShishouGrant#CHECK_PERIOD} ticks from the player tick: one map lookup), and runs a press of the skill key in one of its modes ({@link ShishouModes}): the
 * modes only ROUTE to the existing, server-decided abilities of {@link ShishouService} (the sword's mana, the per-ability cooldowns, the caps). The pick inside a mode is stored in the skill instance's tag (so it
 * syncs to the client's skill screen) and mirrored on the world's record for the HUD. Every decision is on the server and keyed by the player's UUID; nothing here is a static "current player".
 */
public final class ShishouGrantService {
    private ShishouGrantService() {}

    private static final String SEL = "Sel";

    // ---------------------------------------------------------------- grant and revoke
    private static Skills skillsOf(ServerPlayer p) {
        try {
            return SkillAPI.getSkillsFrom(p);
        } catch (RuntimeException noSkills) {                                                 // a player without skill data (a stand-in, a very early login)
            return null;
        }
    }

    public static boolean has(ServerPlayer p) {
        Skills s = skillsOf(p);
        return s != null && s.getSkill(NUSkills.SHISHOU.getId()).isPresent();
    }

    /** Makes the player's skill list right for the current record: the wielder has the skill, everybody else does not. Returns the verdict that was applied. */
    public static ShishouGrant.Verdict sync(ServerPlayer p) {
        if (!ShishouData.bound()) return ShishouGrant.Verdict.KEEP;
        boolean should = ShishouGrant.shouldHave(ShishouConfig.enabled(), ShishouConfig.grantSkill(), ShishouData.get().cap().owner(), p.getUUID());
        Skills skills = skillsOf(p);
        if (skills == null) return ShishouGrant.Verdict.KEEP;
        Optional<ManasSkillInstance> inst = skills.getSkill(NUSkills.SHISHOU.getId());
        ShishouGrant.Verdict v = ShishouGrant.verdict(should, inst.isPresent());
        if (v == ShishouGrant.Verdict.GRANT) {
            skills.learnSkill(NUSkills.SHISHOU.get().createDefaultInstance(), Component.translatableWithFallback("shishou.skill_msg.granted", "Shishou answers: the Ultimate skill Shishou is yours.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        } else if (v == ShishouGrant.Verdict.REVOKE) {
            skills.forgetSkill(NUSkills.SHISHOU.getId(), Component.translatableWithFallback("shishou.skill_msg.revoked", "Shishou is no longer yours: the skill fades.").withStyle(ChatFormatting.GRAY));
        }
        return v;
    }

    /** The periodic check from the player tick: spread over the players, only the cheap compare unless the list is wrong. */
    public static void tick(ServerPlayer p) {
        if (ShishouData.bound() && ShishouGrant.due(p.tickCount, p.getId())) sync(p);
    }

    /** The old wielder (when online) loses the skill at once when the sword is claimed by somebody else. */
    public static void syncAll(net.minecraft.server.MinecraftServer server) {
        if (server == null || !ShishouData.bound()) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p);
    }

    // ---------------------------------------------------------------- the press of the skill key
    private static int stored(ManasSkillInstance inst, int mode) {
        var tag = inst.getOrCreateTag();
        return tag.contains(SEL + mode) ? tag.getInt(SEL + mode) : -1;
    }

    /** The ability a mode currently points at, for the client's mode name and for the press (the instance tag is synced). */
    public static ShishouSkill picked(ManasSkillInstance inst, int mode, int level, long learned) {
        ShishouModes.Mode m = ShishouModes.byIndex(mode);
        return m == null ? null : ShishouModes.current(m, stored(inst, mode), level, learned);
    }

    /** One press of the skill key in {@code modeIndex}. Server side only; the caller is the skill's {@code onPressed}. */
    public static void press(ServerPlayer p, ManasSkillInstance inst, int modeIndex) {
        ShishouModes.Mode m = ShishouModes.byIndex(modeIndex);
        if (m == null || !ShishouService.pressAllowed(p)) return;
        ShishouData d = ShishouData.get();
        if (!d.cap().isOwner(p.getUUID())) {                                                  // the sword is no longer theirs: the skill is on its way out
            sync(p);
            return;
        }
        ShishouRecord rec = d.record();
        int level = rec.level();
        long learned = rec.learned();
        List<ShishouSkill> avail = ShishouModes.available(m, level, learned);
        ShishouModes.Action a = ShishouModes.action(m, p.isShiftKeyDown(), avail.size());
        switch (a) {
            case THROW_RECALL -> ShishouService.throwRecall(p);
            case STATUS -> ShishouService.counsel(p);
            case OPEN_STORAGE -> ShishouService.openStorage(p);
            case CYCLE -> {
                ShishouSkill nx = ShishouModes.cycle(m, ShishouModes.current(m, stored(inst, modeIndex), level, learned), level, learned);
                if (nx == null) return;
                inst.getOrCreateTag().putInt(SEL + modeIndex, nx.bit());
                inst.markDirty();
                rec.select(nx);
                d.touch();
                p.displayClientMessage(Component.translatableWithFallback("shishou.msg.picked", "%s: %s", Component.translatableWithFallback(m.langKey(), m.english()),
                        Component.translatableWithFallback(nx.langKey(), nx.english())).withStyle(ChatFormatting.AQUA), true);
                ShishouService.syncHud(p);
            }
            case CAST -> {
                ShishouSkill cur = ShishouModes.current(m, stored(inst, modeIndex), level, learned);
                if (cur == null) {
                    p.displayClientMessage(Component.translatableWithFallback("shishou.msg.mode_locked", "Nothing in %s is open to me yet.", Component.translatableWithFallback(m.langKey(), m.english())).withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                rec.select(cur);
                ShishouService.cast(p, cur);
            }
        }
    }
}
