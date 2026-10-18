package com.newuniverse.nusmp.heroweapon.weapon.instrument;

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
 * Why: the behaviour of the {@link HeroKind#INSTRUMENT} weapon, the Vassal Musical Instrument of Glass' world (drawn as a violin, see the weapon doc). It lists the ten
 * techniques (their cooldowns and costs come from the pure {@link InstrumentNumbers} table, so the balance test and the game agree; their bodies are in
 * {@link InstrumentSkills}, each run inside the beat of the phrase) and keeps the beat clear when the instrument is put away. The passives of the forms are
 * {@code perfect_pitch} (the wielder's accuracy: notes never stray), {@code fairy_choir} (songs reach a quarter farther) and {@code steady_beat} (the beat pocket is
 * twenty ticks wider). This weapon has no Curse Series. The constructor registers the self-test {@code hero.weapon.instrument}.
 */
public final class InstrumentLogic implements HeroLogic {
    private final List<HeroSkill> skills;

    public InstrumentLogic() {
        InstrumentSkills.TEMPO.size();                  // set the beat memory (and its logout and server-stop cleanup) up now, while the mod starts, not at the first cast
        skills = List.of(
                skill(InstrumentNumbers.MUSIC_STRIKE, "Music Strike", "Shoot a note like an arrow. With no harmony it strays; keep the beat and it flies true and hits harder.", c -> InstrumentSkills.perform(c, InstrumentSkills::musicStrike)),
                skill(InstrumentNumbers.DEMONS_MELODY, "Demon's Melody", "Seven notes in a fan. The fan narrows with harmony; one foe takes at most three notes.", c -> InstrumentSkills.perform(c, InstrumentSkills::demonsMelody)),
                skill(InstrumentNumbers.STUN_BEAT, "Stun Beat", "A guitar note that bursts in a flash where you look: it hurts and, behind the control lock, stuns and dazzles.", c -> InstrumentSkills.perform(c, InstrumentSkills::stunBeat)),
                skill(InstrumentNumbers.MARCH, "March", "A song that follows you for ten seconds: allies in range move and work faster (Speed II from harmony three).", c -> InstrumentSkills.perform(c, InstrumentSkills::march)),
                skill(InstrumentNumbers.WAR_SONG, "War Song", "A song that follows you for eight seconds: allies in range hit harder, and from harmony three take less damage.", c -> InstrumentSkills.perform(c, InstrumentSkills::warSong)),
                skill(InstrumentNumbers.LULLABY, "Lullaby", "A drowsy tune that slows and weakens the foes in range (shared control lock).", c -> InstrumentSkills.perform(c, InstrumentSkills::lullaby)),
                skill(InstrumentNumbers.DIRGE, "Dirge", "A funeral tune that poisons and weakens the foes in range. Poison never kills a player.", c -> InstrumentSkills.perform(c, InstrumentSkills::dirge)),
                skill(InstrumentNumbers.GLUTTON, "Glutton God Tango", "A tune for the grilled-fish instrument: allies in range eat a little and mend a little.", c -> InstrumentSkills.perform(c, InstrumentSkills::gluttonGodTango)),
                skill(InstrumentNumbers.ZERO_NOTE, "Zero Note", "A silent note, no damage: it frees every ally in range of two harmful effects and of a sealed grimoire.", c -> InstrumentSkills.perform(c, InstrumentSkills::zeroNote)),
                skill(InstrumentNumbers.CRESCENDO, "Crescendo", "Needs harmony three, and spends the whole phrase: after a long breath the song swells for six pulses, hurting and slowing foes and mending allies.", InstrumentSkills::crescendo).asUltimate());
        Set<String> ids = new HashSet<>();
        for (Row r : InstrumentNumbers.ALL) ids.add(r.id());
        WeaponSelfTest.register(HeroKind.INSTRUMENT, new FormsFile.Expect("vassal_instrument", ids, InstrumentNumbers.ULTIMATES, Set.of(), 3, 5, 0, 0));
    }

    private static SimpleSkill skill(Row row, String name, String desc, SimpleSkill.Body body) {
        return SimpleSkill.of(row.id(), name, desc, row.cooldown(), row.cost(), body);
    }

    @Override
    public HeroKind kind() { return HeroKind.INSTRUMENT; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onDismiss(HeroCtx c) {
        Tempo t = InstrumentSkills.TEMPO.peek(c.player);
        if (t != null) t.reset();
    }
}
