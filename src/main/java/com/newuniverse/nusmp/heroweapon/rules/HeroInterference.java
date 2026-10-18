package com.newuniverse.nusmp.heroweapon.rules;

import net.minecraft.world.entity.player.Player;

/**
 * Why: the third restriction of the Holy Weapons (the owner's text): the heroes gain no experience when they stand close to one another, because their weapons
 * interfere. One question, answered in one place, for every place that grants experience (vanilla orbs, kill experience, the weapon's own experience, the
 * System's): is this player's gain blocked right now? Seven Star and Vassal wielders are never blocked and never block anyone. Owned by the core builder, who
 * replaces this stub; the stub never blocks.
 */
public final class HeroInterference {
    private HeroInterference() {}

    /** True while a Holy wielder has another Holy wielder within the repel radius (config) in the same dimension. */
    public static boolean blocked(Player player) { return false; }
}
