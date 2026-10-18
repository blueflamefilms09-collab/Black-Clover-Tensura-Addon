package com.newuniverse.nusmp.heroweapon.curse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Why: the owner's picture of the Embrace is "hold Sneak and the third hero key for three seconds", and the core keeps which technique keys are held
 * ({@code HeroKeys.isHeld(ServerPlayer, HeroTrigger)}, fed by the client's key heartbeats). The curse asks it through a method found once by name, so this system builds
 * against the shared contract and still finds the core's bookkeeping when the real core is there; when it is not (or its method is not that), this route is simply not
 * offered and the Embrace key, the Weapon Book's button and the command remain. It looks once every ten ticks at a wielder with an open offer; seven looks in a row
 * (sixty ticks apart from the first to the last, three seconds) with Sneak and the key down accept the offer ({@link HoldCount} counts them). Server thread only.
 */
final class KeyThree {
    private KeyThree() {}

    private static final Logger LOG = LogUtils.getLogger();

    private static final HoldCount COUNT = new HoldCount();
    private static Method isHeld;
    private static boolean looked;

    /** True when the core's key bookkeeping was found (looked up on first use). */
    static boolean available() {
        if (!looked) {
            looked = true;
            try {
                isHeld = Class.forName("com.newuniverse.nusmp.heroweapon.skill.HeroKeys").getMethod("isHeld", ServerPlayer.class, HeroTrigger.class);
            } catch (ReflectiveOperationException | LinkageError absent) {
                isHeld = null;
            }
        }
        return isHeld != null;
    }

    /** One look at a wielder with an open offer: true on the look that completes the three seconds. */
    static boolean step(ServerPlayer p) {
        if (!available()) return false;
        boolean down = false;
        if (p.isShiftKeyDown()) {
            try {
                down = Boolean.TRUE.equals(isHeld.invoke(null, p, HeroTrigger.KEY_3));
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOG.warn("[nusmp] The core's key bookkeeping failed; the third hero key no longer accepts the Embrace: {}", e.toString());
                isHeld = null;
            }
        }
        return COUNT.step(p.getUUID(), down, HoldCount.LOOKS);
    }

    /** Forgets a player's count (they left, or have no open offer). */
    static void forget(UUID id) { COUNT.forget(id); }

    /** Forgets every count (the server stopped). */
    static void clear() { COUNT.clear(); }
}
