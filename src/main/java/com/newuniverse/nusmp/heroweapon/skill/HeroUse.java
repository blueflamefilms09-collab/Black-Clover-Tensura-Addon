package com.newuniverse.nusmp.heroweapon.skill;

import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the dispatcher. One entry point turns an input (the use key, sneak + use, a hero key) into a skill cast: it finds the bond and the ACTIVE form, takes the
 * skill the form lists on that input, refuses when the weapon is not summoned or the skill is on cooldown or the wielder cannot pay, waits out the wind-up, casts,
 * and only then starts the cooldown and charges the cost. Doing all of this in one place is what keeps the owner's balance rules true for every weapon (a
 * skill can never forget its own cooldown). Owned by the core builder, who replaces this stub; the stub does nothing.
 */
public final class HeroUse {
    private HeroUse() {}

    /** Fires the skill on {@code trigger} for the player; true when a skill was cast. */
    public static boolean fire(ServerPlayer player, HeroTrigger trigger) { return false; }
}
