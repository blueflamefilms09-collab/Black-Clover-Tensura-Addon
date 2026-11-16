package com.newuniverse.nusmp.saiyan;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.tensura.config.race.RaceConfig;
import io.github.manasmods.tensura.race.template.DefaultRace;

/**
 * Why: the Saiyan as a real Tensura race, so it is in Tensura's race registry, shows in Tensura's race menu (the reincarnation screen reads {@code DefaultRace.getDefaultConfig()} for its stat box) and is saved as the
 * player's race. It is the smallest one: Tensura's own {@link DefaultRace} template (like {@code HumanRace}), no evolution (a Saiyan's stages are the mod's own {@link SaiyanRaceRules}, earned by deeds, not by Tensura's
 * existence points), no intrinsic skills (the Saiyan skills are the Saiyan system's). The stat numbers are {@link SaiyanRaceRules}' (the stage 1 buff and the aura/magicule ranges); Tensura's own races read theirs from
 * a ManasCore config file, this one is fixed in code so there is no new Tensura config file. Registered by {@link SaiyanRaces}; linked to the Saiyan flag by {@link SaiyanRaceLink}.
 * Cost: nothing per tick; Tensura calls it only when a race is set, read or shown.
 */
public class SaiyanTensuraRace extends DefaultRace {
    private static final RaceConfig.Default CONFIG = new Stats();

    public SaiyanTensuraRace() {
        super(ManasRace.Difficulty.INTERMEDIATE);
        applyDefaultAttributeModifiers();          // as HumanRace's no-argument constructor does: the config's flat modifiers become the race's attribute modifiers
    }

    public RaceConfig.Default getDefaultConfig() { return CONFIG; }

    /** The race's numbers (no @Override on the getters: only the names of Tensura 2.0.1.0's RaceConfig.Default matter, a missing one is a compile error that names it). */
    private static final class Stats extends RaceConfig.Default {
        public double getMinAura() { return SaiyanRaceRules.RACE_MIN_AURA; }
        public double getMaxAura() { return SaiyanRaceRules.RACE_MAX_AURA; }
        public double getMinMagicule() { return SaiyanRaceRules.RACE_MIN_MAGICULE; }
        public double getMaxMagicule() { return SaiyanRaceRules.RACE_MAX_MAGICULE; }
        public double getSize() { return 0.0; }
        public double getMaxHealth() { return SaiyanRaceRules.raceHealth(); }
        public double getMaxSpiritualHealth() { return 0.0; }
        public double getAttack() { return SaiyanRaceRules.raceAttack(); }
        public double getAttackSpeed() { return 0.0; }
        public double getKnockbackResistance() { return SaiyanRaceRules.raceKnockback(); }
        public double getMovementSpeed() { return SaiyanRaceRules.raceMovement(); }
        public double getSwimSpeed() { return 0.0; }
    }
}
