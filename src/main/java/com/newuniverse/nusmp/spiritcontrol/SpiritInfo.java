package com.newuniverse.nusmp.spiritcontrol;

import java.util.List;
import java.util.Set;

/**
 * Why: the pure half of a spirit that the control plane knows: its id, its name, which grimoire magics it belongs to, its special abilities and its combat spells. No Minecraft types, so the registry, the
 * page gating and the decision rules are unit tested. The game half (summon, release, the real abilities) is {@code SpiritHandle}.
 */
public interface SpiritInfo {
    /** The lower case id the command and the config use ({@code sylph}, {@code spirit_light}). */
    String id();

    /** The name shown to the player ("Sylph"). */
    String display();

    /** The grimoire magics (MagicType names such as FLAME, WIND) whose book carries this spirit's pages. */
    Set<String> magics();

    /** The special abilities, at most {@link PageGate#MAX_ABILITIES}. */
    List<SpiritAbilityDef> abilities();

    /** The combat spells (never empty for a spirit that fights). */
    List<SpiritSpellDef> spells();
}
