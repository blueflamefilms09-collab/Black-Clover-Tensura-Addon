package com.newuniverse.nusmp.elite.races.race;

import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.data.RaceTemplates;
import io.github.manasmods.tensura.config.race.RaceConfig;

/**
 * Why: Tensura's {@code DefaultRace} asks a {@code RaceConfig.Default} for the twelve numbers of a race (aura and magicule ranges, size, health, spiritual health, attack, attack
 * speed, knockback resistance, movement and swim speed). Tensura's own races read theirs from a config file; this one reads them from the race's template, with the same shape
 * the other add-ons use for races that ship their numbers in code (an immutable holder of the twelve doubles). The block is built once per race when the race is built, so a
 * datapack cannot change it afterwards (documented in docs/elite/races.md). A missing template answers zeros, never null.
 */
public final class TemplateStats extends RaceConfig.Default {
    private final RaceTemplate.Stats s;

    private TemplateStats(RaceTemplate.Stats s) { this.s = s; }

    public static TemplateStats of(String templateId) {
        RaceTemplate t = RaceTemplates.get(templateId);
        return new TemplateStats(t == null ? new RaceTemplate.Stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) : t.stats());
    }

    @Override public double getMinAura() { return s.auraMin(); }

    @Override public double getMaxAura() { return s.auraMax(); }

    @Override public double getMinMagicule() { return s.magiculeMin(); }

    @Override public double getMaxMagicule() { return s.magiculeMax(); }

    @Override public double getSize() { return s.size(); }

    @Override public double getMaxHealth() { return s.maxHealth(); }

    @Override public double getMaxSpiritualHealth() { return s.spiritualHealth(); }

    @Override public double getAttack() { return s.attack(); }

    @Override public double getAttackSpeed() { return s.attackSpeed(); }

    @Override public double getKnockbackResistance() { return s.knockbackResistance(); }

    @Override public double getMovementSpeed() { return s.movementSpeed(); }

    @Override public double getSwimSpeed() { return s.swimSpeed(); }
}
