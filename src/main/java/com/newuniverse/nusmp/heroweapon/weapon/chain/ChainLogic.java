package com.newuniverse.nusmp.heroweapon.weapon.chain;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Row;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.FormsFile;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.WeaponSelfTest;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the behaviour of the {@link HeroKind#CHAIN} weapon, the Vassal Chain of S'yne's world, to which the design gives the Sloth Curse Series (canon ties Sloth to the
 * Hunting Tool; this is a design choice, stated in the weapon doc). It lists the ten techniques (their cooldowns and costs come from the pure {@link ChainNumbers}
 * table, so the balance test and the game agree; their bodies are in {@link ChainSkills}) and wires the one hook that matters: {@link #onHit}, where the heavy links of
 * every form hang their weight on what the chain strikes in melee. The passive of the forms is {@code heavy_links}. The Sloth techniques (weight, snap, the debt) are
 * only listed by the curse forms. The constructor registers the self-test {@code hero.weapon.chain}.
 */
public final class ChainLogic implements HeroLogic {
    private final List<HeroSkill> skills;

    public ChainLogic() {
        ChainSkills.WEIGHT.size();                      // set the links' memory (and its logout and server-stop cleanup) up now, while the mod starts, not at the first cast
        skills = List.of(
                skill(ChainNumbers.LASH, "Chain Lash", "A lash of the chain along your look direction: it hurts what it crosses and tugs the far ones towards you.", ChainSkills::chainLash),
                skill(ChainNumbers.PULL, "Chain Pull", "Hook the first foe in sight and yank it to you; a boss barely moves. Shared control lock.", ChainSkills::chainPull),
                skill(ChainNumbers.BINDING, "Chain Binding", "Chains coil round one foe and hold it fast. Shared control lock.", ChainSkills::chainBinding),
                skill(ChainNumbers.HYDRA, "Hydra", "After a short, visible gathering the chain is slammed down and many chains race out along the floor in a fan; a foe in any of them is struck once.", ChainSkills::hydra),
                skill(ChainNumbers.DEFENCE, "Chain Defence", "Chains wrap you and the people beside you: Absorption and a little less damage for six seconds.", ChainSkills::chainDefence),
                skill(ChainNumbers.GRAPPLE, "Grapple", "Hook the first foe in sight and swing yourself to it, striking it where you land.", ChainSkills::grapple),
                skill(ChainNumbers.OROCHI, "Multi-Headed Orochi", "Four groups of chains fall on the ground you aim at, bind up to four foes in it and squeeze them twice. Shared control lock.", ChainSkills::orochi).asUltimate(),
                skill(ChainNumbers.SLOTH_WEIGHT, "Sloth: Weight", "Sloth: a heavy drowsiness spreads from the chain. Foes in range are slowed and weakened (shared control lock); you are slowed a little too. Costs corruption.", ChainSkills::slothWeight),
                skill(ChainNumbers.SLOTH_SNAP, "Sloth: Snap", "Sloth: a chain wraps one foe now, and a second after it snaps tight for one heavy blow if the foe is still within reach. Costs corruption.", ChainSkills::slothSnap),
                skill(ChainNumbers.DEBT, "The Bill Comes Due", "Sloth: for six seconds heavy chains lie round you and slow the foes in them and you; then the whole bill comes due on every foe within eleven blocks. Costs a great deal of corruption.", ChainSkills::debtComesDue).asUltimate());
        Set<String> ids = new HashSet<>();
        for (Row r : ChainNumbers.ALL) ids.add(r.id());
        WeaponSelfTest.register(HeroKind.CHAIN, new FormsFile.Expect("vassal_chain", ids, ChainNumbers.ULTIMATES, Set.of("sloth"), 3, 5, 2, 3));
    }

    private static SimpleSkill skill(Row row, String name, String desc, SimpleSkill.Body body) {
        return SimpleSkill.of(row.id(), name, desc, row.cooldown(), row.cost(), body);
    }

    @Override
    public HeroKind kind() { return HeroKind.CHAIN; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) { ChainSkills.onHit(c, target); }

    @Override
    public void onDismiss(HeroCtx c) {
        WeightLedger ledger = ChainSkills.WEIGHT.peek(c.player);
        if (ledger != null) ledger.clear();
    }
}
