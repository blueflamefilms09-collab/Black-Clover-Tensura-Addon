package com.newuniverse.nusmp.heroweapon.weapon.mirror;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Row;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.FormsFile;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.WeaponSelfTest;
import net.minecraft.world.damagesource.DamageSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the behaviour of the {@link HeroKind#MIRROR} weapon, the Vassal Mirror of Glass' world (the Shield Hero lore: magical defence and movement; Envy is given to
 * it by the design, not by canon). It lists the ten techniques (their cooldowns and costs come from the pure {@link MirrorNumbers} table, so the balance test and the
 * game agree; their bodies are in {@link MirrorSkills}) and wires the one hook that matters: {@link #onDamaged}, where the image soaks a blow, the Spell Ward trims
 * magic and an open rebound turns half of a spell back. The passives of the forms are {@code spell_ward} (magic damage taken -15% while the mirror is in hand) and
 * {@code afterimage} (the image repeats the next Mirror Bash or Prism Ray once). The constructor registers the self-test {@code hero.weapon.mirror}.
 */
public final class MirrorLogic implements HeroLogic {
    private final List<HeroSkill> skills;

    public MirrorLogic() {
        MirrorSkills.STATE.size();                      // set the wielders' memory (and its logout and server-stop cleanup) up now, while the mod starts, not at the first cast
        skills = List.of(
                skill(MirrorNumbers.BASH, "Mirror Bash", "Strike with the edge of the glass: a short cone that hurts, shoves and staggers. Fills the gleam.", MirrorSkills::bash),
                skill(MirrorNumbers.GLASS_SHIELD, "Glass Shield", "Stand a pane of glass ahead for six seconds. It turns three hostile shots back at their shooters, stores their light as gleam, and bursts into cutting shards.", MirrorSkills::glassShield),
                skill(MirrorNumbers.PRISM_RAY, "Prism Ray", "After a short charge, a beam of light that is worth more with gleam and bounces off a pane it passes to strike another foe.", MirrorSkills::prismRay),
                skill(MirrorNumbers.STARDUST, "Stardust Mirror", "A barrier of star-glass round the ally you look at (or you): Absorption, and the next two hostile shots aimed at each are turned back.", MirrorSkills::stardust),
                skill(MirrorNumbers.REBOUND, "Release Rebound", "For four seconds half of every spell is turned back on its caster (capped), and two harmful effects are cleansed from you and may be sent on to a foe.", MirrorSkills::releaseRebound),
                skill(MirrorNumbers.MOVEMENT, "Movement Mirror", "Step through the glass up to nine blocks, leaving an image behind. The image soaks one blow; cast again to step back into it.", MirrorSkills::movementMirror),
                skill(MirrorNumbers.CHAIN_BINDING, "Chain Binding Mirror", "Chains link your standing glass and bind up to four foes near it (shared control lock). Needs a Glass Shield or an image.", MirrorSkills::chainBinding),
                skill(MirrorNumbers.KALEIDOSCOPE, "Kaleidoscope", "A ring of six panes round the ground you aim at; six pulses of light strike every foe inside it. Leave the ring to answer it.", MirrorSkills::kaleidoscope).asUltimate(),
                skill(MirrorNumbers.COVET, "Covet", "Envy: copy up to three of a foe's helpful effects onto yourself for ten seconds at most. Costs extra magicules and corruption.", MirrorSkills::covet),
                skill(MirrorNumbers.RESSENTIMENT, "Ressentiment", "Envy: the creatures near you turn on each other for four seconds. Creatures only, shared control lock, corruption.", MirrorSkills::ressentiment));
        Set<String> ids = new HashSet<>();
        for (Row r : MirrorNumbers.ALL) ids.add(r.id());
        WeaponSelfTest.register(HeroKind.MIRROR, new FormsFile.Expect("vassal_mirror", ids, MirrorNumbers.ULTIMATES, Set.of("envy"), 3, 5, 2, 3));
    }

    private static SimpleSkill skill(Row row, String name, String desc, SimpleSkill.Body body) {
        return SimpleSkill.of(row.id(), name, desc, row.cooldown(), row.cost(), body);
    }

    @Override
    public HeroKind kind() { return HeroKind.MIRROR; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public float onDamaged(HeroCtx c, DamageSource source, float amount) { return MirrorSkills.onDamaged(c, source, amount); }

    @Override
    public void onDismiss(HeroCtx c) {
        MirrorLedger ledger = MirrorSkills.STATE.peek(c.player);
        if (ledger != null) ledger.putAway();
    }
}
