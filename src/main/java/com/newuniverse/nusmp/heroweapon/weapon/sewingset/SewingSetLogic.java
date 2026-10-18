package com.newuniverse.nusmp.heroweapon.weapon.sewingset;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Row;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.FormsFile;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.WeaponSelfTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the behaviour of the {@link HeroKind#SEWING_SET} weapon, the Vassal Sewing Set of S'yne's world (drawn as a needle with a thread spool for a grip, see the weapon
 * doc). It lists the eight techniques (their cooldowns and costs come from the pure {@link SewingNumbers} table, so the balance test and the game agree; their bodies
 * are in {@link SewingSkills}) and, while the set is out, draws a faint thread from the wielder to every ally with a needle in them. The one passive of the forms is
 * {@code fine_stitch} (a stitch mark in a foe lasts eighty ticks longer). This weapon has no Curse Series. The constructor registers the self-test
 * {@code hero.weapon.sewingset}. The first skill is the Needle Shot: the Jumping Needle, the Stitch Mend and Ariadne's Thread use the needles it leaves.
 */
public final class SewingSetLogic implements HeroLogic {
    private final List<HeroSkill> skills;

    public SewingSetLogic() {
        SewingSkills.BOARD.size();                      // set the needle memory (and its logout and server-stop cleanup) up now, while the mod starts, not at the first cast
        skills = List.of(
                skill(SewingNumbers.NEEDLE_SHOT, "Needle Shot", "Three needles in a narrow fan: each leaves a stitch mark in the foe it strikes. With nothing struck, the middle one marks the ally you look at, or plants itself in the ground.", SewingSkills::needleShot),
                skill(SewingNumbers.BIND_WIRE, "Bind Wire", "Silk binds one foe fast and seals its skills for the length of the bind. Shared control lock.", SewingSkills::bindWire),
                skill(SewingNumbers.JUMPING_NEEDLE, "Jumping Needle", "Jump to the nearest ally you have put a needle in, or to the needle planted in the ground. With none, plant one where you stand.", SewingSkills::jumpingNeedle),
                skill(SewingNumbers.STITCH_MEND, "Stitch Mend", "Close wounds with silk: allies beside you, and up to three you have put a needle in even far away, mend a little and shed one harmful effect.", SewingSkills::stitchMend),
                skill(SewingNumbers.SCISSOR_CUT, "Scissor Cut", "Two great snips crossed where you look, after a short, visible crossing of lines. A foe with a stitch mark takes 30 percent more. It also snips you free of slowness.", SewingSkills::scissorCut),
                skill(SewingNumbers.STITCHED_BARRIER, "Stitched Barrier", "A seam of stitches across the ground ahead for six seconds: it catches up to six hostile shots, and foes that touch it are slowed and pricked once.", SewingSkills::stitchedBarrier),
                skill(SewingNumbers.SPIDER_NET, "Spider Net", "A net of silk flung where you look: the foes caught in it are held almost fast and pricked. Shared control lock.", SewingSkills::spiderNet),
                skill(SewingNumbers.ARIADNE, "Ariadne's Thread", "After a visible gathering of threads, up to four allies are drawn to your side, and every foe within twenty-four blocks is lit up for eight seconds.", SewingSkills::ariadnesThread).asUltimate());
        Set<String> ids = new HashSet<>();
        for (Row r : SewingNumbers.ALL) ids.add(r.id());
        WeaponSelfTest.register(HeroKind.SEWING_SET, new FormsFile.Expect("vassal_sewing_set", ids, SewingNumbers.ULTIMATES, Set.of(), 3, 5, 0, 0));
    }

    private static SimpleSkill skill(Row row, String name, String desc, SimpleSkill.Body body) {
        return SimpleSkill.of(row.id(), name, desc, row.cooldown(), row.cost(), body);
    }

    @Override
    public HeroKind kind() { return HeroKind.SEWING_SET; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onTick(HeroCtx c) { SewingSkills.onTick(c); }
}
