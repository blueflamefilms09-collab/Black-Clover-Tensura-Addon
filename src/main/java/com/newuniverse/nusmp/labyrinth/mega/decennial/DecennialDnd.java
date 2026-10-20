package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Guardian already has a D&D layer (legendary resistances, legendary actions, the anti-magic aura, frightful presence, magic absorption ...) and the owner's brief says the 200 Decennial
 * bosses REUSE the guard boss framework and add no entity type. So a Decennial boss does not get a second set of those rules: it gets a {@link DndProfile} built from its own profile, and the
 * Guardian's existing {@code DndBoss} runs it. This class is that translation, with no Minecraft type in it so a test can pin it: the id carries the prefix {@code dec:} (which is how the layer and
 * {@code DndBoss} recognise a Decennial profile and leave the three-form phase announcements to the Decennial phase machine), the three "forms" are the first, a middle and the last phase name, the
 * legendary actions are the abilities of the phases reached so far (each phase swaps the profile, so a new phase unlocks its moves), the lair list is EMPTY (the hall's lair actions are the
 * Decennial layer's own, richer ones), and there is no Mythic Trait (the phases are the second life) and no Word of Ending (its three-second cast is the one rule the brief's balance does not need
 * twice).
 */
public final class DecennialDnd {
    private DecennialDnd() {}

    /** The id prefix of a profile made here. */
    public static final String PREFIX = "dec:";

    /** The legendary actions that cost two points of the round (the heavy ones); every other costs one. */
    public static final List<String> HEAVY = List.of("wing_buffet", "frightful_presence", "haki_pulse", "gravity_crush", "dragon_breath", "death_touch", "mind_blast");

    /** Is this the id of a profile made by {@link #of}? */
    public static boolean isDecennial(String dndId) { return dndId != null && dndId.startsWith(PREFIX); }

    /** The D&D profile of a boss while it is in phase {@code phase} (1-based): the abilities of every phase up to it. */
    public static DndProfile of(DecennialProfile p, int phase) {
        int shown = Math.max(1, Math.min(p.phaseCount(), phase));
        List<String> legendary = p.abilitiesUpTo(shown);
        List<Integer> costs = new ArrayList<>(legendary.size());
        for (String a : legendary) costs.add(HEAVY.contains(a) ? 2 : 1);
        int n = p.phaseCount();
        List<String> forms = List.of(p.phase(1).name(), p.phase(Math.min(2, n)).name(), p.phase(n).name());
        int tier = DecennialRules.tierOf(p.level());
        DecennialProfile.Dnd d = p.dnd();
        return new DndProfile(PREFIX + p.id(), p.side(), tier, 0, p.name(), List.of(), List.of(p.creature()), forms, DndRules.clampResistances(d.resistances()),
                DndRules.points(d.legendaryPoints(), false), List.copyOf(legendary), List.copyOf(costs), List.of(), d.antiMagicAura(), d.frightfulAura(), d.absorbMagic(), false, false, false, tier, tier,
                d.breath(), d.reflective(), p.look());
    }
}
