package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.devil.DevilConfig;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.dual.DualRules;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/**
 * Devil Magic (Ultimate, Dragon class): the skill every devil contract gives (docs/devil_double_magic.md), granted when the contract is signed and taken when it ends ({@code devil.dual.DevilSkillGrant}).
 * Two arts, switched with the mode key: the devil's own art (mode 0: a cone of its attribute's power in front of you, the effect and push or pull of that attribute) and the double art (mode 1: the same art
 * with what your own grimoire magic does on a hit added to it, the way the Dark Triad fight with their own magic and their devil's). The work is {@link DevilArts}; this class is the price, the cooldown and the names.
 * It is the attack of the bond. The body of the bond (letting the devil in, the union) is the intrinsic skill Devil Bond ({@link DevilBondSkill}); the two do not overlap.
 */
public class DevilMagicSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("devil_magic", "Devil Magic",
            "Ultimate. Given by a devil contract and gone when it ends. Modes: the Devil's Art (a cone of your devil's attribute: damage, its effect on the foes, a push or a pull) and the Double Art (the same art with what your own grimoire magic does on a hit added to it).");

    private static final String[] IDS = {"art", "double"};
    private static final String[] NAMES = {"Devil's Art", "Double Art"};

    public DevilMagicSkill() { super(SkillType.ULTIMATE, TEXT, true); }
    @Override public UltimateClass ultimateClass() { return UltimateClass.DRAGON; }

    @Override public int getModes(ManasSkillInstance instance) { return IDS.length; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return Math.floorMod(mode + (reverse ? -1 : 1), IDS.length);
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) { return "devil." + IDS[Math.floorMod(mode, IDS.length)]; }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        int m = Math.floorMod(mode, IDS.length);
        return SkillMsg.mode("devil." + IDS[m], NAMES[m]);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive() || !DevilConfig.enabled()) return;
        int m = Math.floorMod(mode, IDS.length);
        boolean combo = m == 1;
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract()) { SkillUtil.fail(p, SkillMsg.t("devil.no_bond", "You hold no devil contract.")); return; }
        if (instance.onCoolDown(m)) { SkillUtil.fail(p, SkillMsg.t("devil.cooldown", "The art is not ready.")); return; }
        String hit = "";
        if (combo) {
            Optional<ManasSkillInstance> g = GrimoirePages.grimoireOf(p);
            MagicType magic = g.map(GrimoirePages::magicOf).orElse(null);
            if (magic == null) { SkillUtil.fail(p, SkillMsg.t("devil.no_grimoire", "The double art needs your own grimoire magic as well.")); return; }
            hit = magic.hitEffect().name();
        }
        if (!pay(instance, p, m, DualRules.cost(c.tier, combo))) return;
        int hits = DevilArts.fire(p, c.attribute, c.tier, combo, hit, masteryFrac(instance), ultimateClass());
        cooldown(instance, m, DualRules.cooldown(combo));
        grow(instance, p, hits > 0 ? 4.0 : 0.5);
    }
}
