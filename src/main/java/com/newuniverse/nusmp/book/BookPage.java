package com.newuniverse.nusmp.book;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;

/**
 * One page = one mode of a grimoire Unique skill.
 * costPercent / costFloor feed getMagiculeCost (percent of max magicule, never below the floor).
 * cooldown is in ticks and starts only after a successful cast (0 = the page sets its own).
 */
public record BookPage(String id, String name, String incantation, double costPercent, double costFloor, int cooldown, Cast cast) {
    @FunctionalInterface
    public interface Cast {
        /** Do the spell. Return false if nothing happened (no cooldown, magicule refunded by not spending it). */
        boolean cast(GrimoireBook book, ManasSkillInstance instance, ServerPlayer caster, int mode);
    }

    // Balance tiers from the spec (ticks, % of max magicule, flat floor).
    public static BookPage starter(String id, String name, Cast c)   { return new BookPage(id, name, name, 8, 150, 80, c); }
    public static BookPage mid(String id, String name, Cast c)       { return new BookPage(id, name, name, 12, 300, 160, c); }
    public static BookPage zone(String id, String name, Cast c)      { return new BookPage(id, name, name, 20, 600, 300, c); }
    public static BookPage signature(String id, String name, Cast c) { return new BookPage(id, name, name, 35, 1200, 600, c); }
    public static BookPage daily(String id, String name, Cast c)     { return new BookPage(id, name, name, 20, 600, 0, c); }

    public BookPage withCooldown(int ticks) { return new BookPage(id, name, incantation, costPercent, costFloor, ticks, cast); }
}
