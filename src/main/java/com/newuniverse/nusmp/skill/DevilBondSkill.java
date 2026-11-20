package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.devil.DevilConfig;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilContracts;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.devil.dual.DevilRanks;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Devil Bond (Intrinsic, shown with the other intrinsic skills): the body of a devil contract, given with it and gone when it ends (docs/devil_double_magic.md). Modes: Assimilate (let the devil in or push it
 * back, the same as the assimilation key: the union follows at 100 percent) and Bond (read what you hold: the devil, its rank, the level, the corruption of the price). It does no damage: the attacks are the
 * Ultimate skill Devil Magic ({@link DevilMagicSkill}), so the two never overlap. All the work is {@code devil.DevilContracts}, which already checks everything again on the server.
 */
public class DevilBondSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("devil_bond", "Devil Bond",
            "Intrinsic. The body of your devil contract, given with it and gone when it ends. Modes: Assimilate (let the devil in or push it back: horns, wings and at 100 percent the Devil Union) and Bond (read your devil, its rank, the level and the corruption of the price). The attacks are the Ultimate skill Devil Magic.");

    private static final String[] IDS = {"assimilate", "bond"};
    private static final String[] NAMES = {"Assimilate", "Bond"};

    public DevilBondSkill() { super(SkillType.INTRINSIC, TEXT, false); }

    @Override public int getModes(ManasSkillInstance instance) { return IDS.length; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return Math.floorMod(mode + (reverse ? -1 : 1), IDS.length);
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) { return "devil_bond." + IDS[Math.floorMod(mode, IDS.length)]; }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        int m = Math.floorMod(mode, IDS.length);
        return SkillMsg.mode("devil_bond." + IDS[m], NAMES[m]);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive() || !DevilConfig.enabled()) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract()) { SkillUtil.fail(p, SkillMsg.t("devil.no_bond", "You hold no devil contract.")); return; }
        if (Math.floorMod(mode, IDS.length) == 0) { DevilContracts.onToggleAsk(p); return; }
        WorldDevil d = WorldDevil.byId(c.devil);
        String name = d == null ? c.attribute.name().toLowerCase(java.util.Locale.ROOT) + " devil" : d.name();
        String rank = d == null ? DevilRanks.line(DevilRanks.of("", c.tier)) : DevilRanks.line(DevilRanks.of(d));
        p.displayClientMessage(SkillMsg.t("devil_bond.status", "%s: %s. Level %s percent. Time origin: %s. Corruption %s percent.",
                name, rank, Math.round(c.assimilation), com.newuniverse.nusmp.devil.dual.DevilMagic.timeOrigin(p, c).label(), com.newuniverse.nusmp.devil.dual.CorruptionRules.percent(c.corruption)).copy().withStyle(ChatFormatting.DARK_PURPLE), false);
    }
}
