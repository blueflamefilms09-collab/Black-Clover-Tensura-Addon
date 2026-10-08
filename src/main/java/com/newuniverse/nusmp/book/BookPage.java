package com.newuniverse.nusmp.book;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * One page = one mode of a grimoire Unique skill.
 * costPercent / costFloor feed getMagiculeCost (percent of max magicule, never below the floor).
 * cooldown is in ticks and starts only after a successful cast (0 = the page sets its own).
 */
public record BookPage(String id, String name, String incantation, double costPercent, double costFloor, int cooldown, Cast cast, String anim,
                       TagKey<Item> weaponTag) {
    /** The usual constructor (0.54: the body animation of the release is null = by tier, see anim.CastAnim.releaseFor). */
    public BookPage(String id, String name, String incantation, double costPercent, double costFloor, int cooldown, Cast cast) {
        this(id, name, incantation, costPercent, costFloor, cooldown, cast, null, null);
    }

    public BookPage(String id, String name, String incantation, double costPercent, double costFloor, int cooldown, Cast cast, String anim) {
        this(id, name, incantation, costPercent, costFloor, cooldown, cast, anim, null);
    }

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

    public BookPage withCooldown(int ticks) { return new BookPage(id, name, incantation, costPercent, costFloor, ticks, cast, anim, weaponTag); }

    /** 0.54: the release body animation of this page: "cast_release_thrust", "cast_release_sweep" (slashes), "cast_release_slam", "cast_signature". */
    public BookPage withAnim(String clip) { return new BookPage(id, name, incantation, costPercent, costFloor, cooldown, cast, clip, weaponTag); }

    /** Requires a matching weapon in either hand and selects the corresponding weapon casting animation. */
    public BookPage withWeapon(TagKey<Item> tag) { return new BookPage(id, name, incantation, costPercent, costFloor, cooldown, cast, anim, tag); }
}
