package com.newuniverse.nusmp.heroweapon.weapon.harpoon;

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
 * Why: the behaviour of the {@link HeroKind#HARPOON} weapon, the Vassal Harpoon of Glass' world. It lists the eight techniques (their cooldowns and costs come from the
 * pure {@link HarpoonNumbers} table, so the balance test and the game agree; their bodies are in {@link HarpoonSkills}) and keeps the line of the harpoon:
 * {@link #onTick} redraws it and lets go of a creature that is gone, {@link #onHit} lets a melee blow keep it alive, {@link #onDismiss} cuts it. The passives of the forms
 * are {@code barbed_head} (a melee blow on the tied creature lengthens the line) and {@code storm_charged} (the throw arcs to one more foe). This weapon has no Curse
 * Series. The constructor registers the self-test {@code hero.weapon.harpoon}. The first skill in the list is the throw: the others need its line.
 */
public final class HarpoonLogic implements HeroLogic {
    private final List<HeroSkill> skills;

    public HarpoonLogic() {
        HarpoonSkills.TETHERS.size();                   // set the line memory (and its logout and server-stop cleanup) up now, while the mod starts, not at the first cast
        skills = List.of(
                skill(HarpoonNumbers.THROW, "Harpoon Throw", "Skewer the first foe you look at and tie the line to it for eight seconds. The other techniques act on what is tied.", HarpoonSkills::harpoonThrow),
                skill(HarpoonNumbers.REEL_IN, "Reel In", "Pull the tied creature to you, or, if it is heavy or a boss, pull yourself to it. Shared control lock.", HarpoonSkills::reelIn),
                skill(HarpoonNumbers.DRAGON, "Lightning Strike Dragon", "Charge up to eight blocks wreathed in lightning, hitting what is on the way, then turn and charge back.", HarpoonSkills::lightningStrikeDragon),
                skill(HarpoonNumbers.WHALE, "Whale Hunting Harpoon", "After a visible gathering, one great throw on the tied creature (or the one you look at); a quarter more against a big body. It slams the target down.", HarpoonSkills::whaleHunting),
                skill(HarpoonNumbers.CRUCIFIXION, "Crucifixion", "Pin the tied creature under a cross of lightning that burns it three times. Shared control lock.", HarpoonSkills::crucifixion),
                skill(HarpoonNumbers.SHOCK, "Lightning Shock Harpoon", "Send lightning down the line: the tied creature is shocked and the current jumps to up to three foes near it.", HarpoonSkills::lightningShock),
                skill(HarpoonNumbers.ZERO, "Harpoon 0", "The 0 series: no damage. Strip up to three helpful effects from the tied or aimed creature and one from each of three foes beside it.", HarpoonSkills::harpoonZero),
                skill(HarpoonNumbers.FISH_BLASTER, "Brave Fish Blaster", "After a long gathering, a surge of lightning-charged water in the shape of a great fish along your aim, hurting everything in it.", HarpoonSkills::braveFishBlaster).asUltimate());
        Set<String> ids = new HashSet<>();
        for (Row r : HarpoonNumbers.ALL) ids.add(r.id());
        WeaponSelfTest.register(HeroKind.HARPOON, new FormsFile.Expect("vassal_harpoon", ids, HarpoonNumbers.ULTIMATES, Set.of(), 3, 5, 0, 0));
    }

    private static SimpleSkill skill(Row row, String name, String desc, SimpleSkill.Body body) {
        return SimpleSkill.of(row.id(), name, desc, row.cooldown(), row.cost(), body);
    }

    @Override
    public HeroKind kind() { return HeroKind.HARPOON; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onTick(HeroCtx c) { HarpoonSkills.onTick(c); }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) { HarpoonSkills.onHit(c, target); }

    @Override
    public void onDismiss(HeroCtx c) {
        TetherBook book = HarpoonSkills.TETHERS.peek(c.player);
        if (book != null) book.cut();
    }
}
