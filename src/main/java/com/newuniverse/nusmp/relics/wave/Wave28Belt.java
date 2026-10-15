package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the card's one rule is a small curse with a fix, a "natural 1" on any check; the game's checks are jumps and blows, so those roll the die, and
 * the "disadvantage on all checks" is a plain Slowness and Weakness kept on while the pantaloons are down.
 * <p>
 * Wave 28, the Buffoon's Pantaloons: the natural 1 that drops them and the action it takes to pull them up again.
 */
final class Wave28Belt {
    private Wave28Belt() {}

    private static final String DOWN = "pants_down";

    static boolean down(ServerPlayer p) {
        CompoundTag d = Wave28Util.peek(p);
        return d != null && d.getBoolean(DOWN);
    }

    /** A check was made (a jump, a blow): a d20 comes up, and on a natural 1 the pantaloons fall around the ankles. */
    static void check(ServerPlayer p) {
        if (!RelicFlags.has(p, Wave28Rules.F_PANTS) || down(p)) return;
        if (!Wave28Rules.naturalOne(p.getRandom().nextInt(Wave28Rules.PANTS_DIE))) return;
        Wave28Util.data(p).putBoolean(DOWN, true);
        Wave28Util.burst(p.serverLevel(), p.position().add(0, 0.4, 0), RukhFx.Alignment.LIGHT, 6);
        Wave28Util.say(p, "A natural 1! Your pantaloons fall down around your ankles. (Spend an action to pull them up.)", ChatFormatting.LIGHT_PURPLE);
    }

    /** Once a second while they are worn and down: disadvantage on everything (a little slower, a little weaker). */
    static void pass(ServerPlayer p) {
        if (!down(p)) return;
        Wave28Util.giveQuiet(p, 60, 0, "minecraft:slowness");
        Wave28Util.giveQuiet(p, 60, 0, "minecraft:weakness");
    }

    /** Pull them up: the action that ends the disadvantage. */
    static void pullUp(ActCtx c) {
        ServerPlayer p = c.player();
        if (!down(p)) {
            Wave28Util.tell(p, "They are up, for now.", ChatFormatting.GRAY);
            return;
        }
        Wave28Util.data(p).putBoolean(DOWN, false);
        Wave28Util.remove(p, "minecraft:slowness", "minecraft:weakness");
        Wave28Util.tell(p, "You hitch the pantaloons up with great dignity.");
    }
}
