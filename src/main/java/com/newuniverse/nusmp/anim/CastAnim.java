package com.newuniverse.nusmp.anim;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.item.WeaponMagicHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.54: the spell-casting body animations (docs/cast_animation_guide.md). The server selects one-shot release poses and
 * looping idle poses; every client that sees the player plays them
 * ({@code client.CastAnimClient}, keyframes in assets/nusmp/animations/player/cast.animation.json made by tools/gen_cast_animations.py).
 * Nothing here delays or changes a spell: the clips attach to moments that already exist.
 */
public final class CastAnim {
    private CastAnim() {}

    /** The clip names of cast.animation.json. */
    public static final String CHANT = "cast_chant", MANA_ZONE = "cast_mana_zone", THRUST = "cast_release_thrust", SWEEP = "cast_release_sweep",
            SIDE = "cast_release_side", UP = "cast_release_up", SLAM = "cast_release_slam", SIGNATURE = "cast_signature", FAIL = "cast_fail",
            WEAPON_SWORD = "cast_weapon_sword", WEAPON_HEAVY = "cast_weapon_heavy", WEAPON_KATANA = "cast_weapon_katana",
            WEAPON_DANCE = "cast_weapon_dance", KEY_TURN = "key_turn", DICE_FIDDLE = "dice_fiddle", DICE_TOSS = "dice_toss";

    /** Plays a clip on the player for everyone who sees them; a one-shot returns to the previous loop when it finishes. */
    public static void play(ServerPlayer p, String clip) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new CastAnimPayload(p.getId(), clip));
    }

    public static void stop(ServerPlayer p) { play(p, ""); }

    /** The release clip of a page: its own override (BookPage.withAnim), else by tier: starter / mid thrust, zone / daily slam, signature the finisher. */
    public static String releaseFor(BookPage page) {
        if (page == null) return THRUST;
        if (page.weaponTag() != null) {
            if (page.weaponTag().equals(WeaponMagicHelper.KATANAS)) return WEAPON_KATANA;
            if (page.weaponTag().equals(WeaponMagicHelper.GREATSWORDS)) return WEAPON_HEAVY;
            if (page.weaponTag().equals(WeaponMagicHelper.DANCE_WEAPONS)) return WEAPON_DANCE;
            return WEAPON_SWORD;
        }
        if (page.anim() != null && !page.anim().isEmpty()) {
            return switch (page.anim()) { case "out", "thrust" -> THRUST; case "side" -> SIDE; case "up" -> UP; case "sweep" -> SWEEP; case "slam" -> SLAM;
                case "signature" -> SIGNATURE; default -> page.anim(); };
        }
        double c = page.costPercent();
        if (c >= 30) return SIGNATURE;
        if (c >= 15) return SLAM;
        // starters and mids: the hand goes out, up or to the side, by the page (the same page always casts the same way)
        int h = Math.floorMod(page.id() == null ? 0 : page.id().hashCode(), 3);
        return h == 0 ? THRUST : h == 1 ? SIDE : UP;
    }
}
