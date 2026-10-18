package com.newuniverse.nusmp.heroweapon.skill;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroLoan;
import com.newuniverse.nusmp.heroweapon.bond.HeroNet;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.rules.Spacing;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the keys of the hero weapons (three technique keys, summon and put away, change form) arrive from the client as small packets, and the server must
 * decide what each means: it never trusts the client (a press is answered at most every few ticks, a technique goes through the same dispatcher as the use key,
 * summon and cycle go through the same doors as the commands). This class is that decision, and it also keeps how long each technique key has been held
 * ({@link KeyHold}) so another system, the Curse Series, can ask "has the player held sneak and key three for three seconds" without a key packet of its own.
 */
public final class HeroKeys {
    private HeroKeys() {}

    /** The server answers the summon key and the form key at most this often (ticks): a held key repeats nothing. */
    private static final int ACTION_SPACING = 10;

    private static final KeyHold HOLD = new KeyHold();
    private static final Map<UUID, long[]> LAST_PRESS = new HashMap<>();
    private static final Map<UUID, Long> SUMMON_LAST = new HashMap<>(), FORM_LAST = new HashMap<>();

    // ================================================================ technique keys
    /** A technique key from the client: slot 0 to 2, phase press, hold or release ({@link HeroNet}). Never throws. */
    public static void onKey(ServerPlayer p, int slot, int phase) {
        SafeCast.tick("hero.key", () -> key(p, slot, phase));
    }

    private static void key(ServerPlayer p, int slot, int phase) {
        if (slot < 0 || slot > 2 || !HeroConfig.enabled() || !p.isAlive()) return;
        long now = p.level().getGameTime();
        UUID id = p.getUUID();
        if (phase == HeroNet.RELEASE) {
            HOLD.release(id, slot);
            return;
        }
        if (phase == HeroNet.HOLD) {
            HOLD.beat(id, slot, now);
            return;
        }
        if (phase != HeroNet.PRESS) return;
        long[] last = LAST_PRESS.computeIfAbsent(id, k -> new long[]{Spacing.LONG_AGO, Spacing.LONG_AGO, Spacing.LONG_AGO});
        if (!Spacing.due(now, last[slot], HeroConfig.keySpacingTicks())) return;
        last[slot] = now;
        HOLD.press(id, slot, now);
        if (HeroBonds.has(p)) HeroUse.fire(p, trigger(slot));
    }

    private static HeroTrigger trigger(int slot) { return slot == 0 ? HeroTrigger.KEY_1 : slot == 1 ? HeroTrigger.KEY_2 : HeroTrigger.KEY_3; }

    // ================================================================ summon, put away, change form
    /** A key action from the client: toggle the weapon, change form, summon, put away. Never throws. */
    public static void onAction(ServerPlayer p, int action) {
        SafeCast.tick("hero.action", () -> act(p, action));
    }

    private static void act(ServerPlayer p, int action) {
        if (!HeroConfig.enabled() || !p.isAlive()) return;
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) {
            say(p, "hero.msg.unbound");
            return;
        }
        long now = p.level().getGameTime();
        UUID id = p.getUUID();
        Map<UUID, Long> clock = action == HeroNet.ACT_CYCLE ? FORM_LAST : SUMMON_LAST;
        if (!Spacing.due(now, clock.getOrDefault(id, Spacing.LONG_AGO), ACTION_SPACING)) return;
        switch (action) {
            case HeroNet.ACT_TOGGLE -> {
                clock.put(id, now);
                if (b.summoned()) HeroLoan.dismiss(p, true);
                else HeroLoan.summon(p, true);
            }
            case HeroNet.ACT_SUMMON -> {
                clock.put(id, now);
                HeroLoan.summon(p, true);
            }
            case HeroNet.ACT_DISMISS -> {
                clock.put(id, now);
                HeroLoan.dismiss(p, true);
            }
            case HeroNet.ACT_CYCLE -> {
                clock.put(id, now);
                cycle(p, b.kind());
            }
            default -> { }
        }
    }

    private static void cycle(ServerPlayer p, HeroKind kind) {
        String next = HeroForms.cycle(p);
        if (next == null) {
            say(p, "hero.msg.no_other_form");
            return;
        }
        HeroForm f = HeroForms.get(kind, next);
        if (HeroConfig.showMessages()) p.displayClientMessage(Component.translatable("hero.msg.form", f == null ? next : f.name()), true);
    }

    private static void say(ServerPlayer p, String key) {
        if (HeroConfig.showMessages()) p.displayClientMessage(Component.translatable(key), true);
    }

    // ================================================================ held keys, for other systems
    /** For how many ticks the player has held the technique key of this trigger ({@code KEY_1} to {@code KEY_3}); 0 for any other trigger or a key that is up. */
    public static int heldTicks(ServerPlayer p, HeroTrigger trigger) {
        int slot = slot(trigger);
        return slot < 0 ? 0 : HOLD.heldTicks(p.getUUID(), slot, p.level().getGameTime());
    }

    /** Is the technique key of this trigger down right now? */
    public static boolean isHeld(ServerPlayer p, HeroTrigger trigger) {
        int slot = slot(trigger);
        return slot >= 0 && HOLD.isHeld(p.getUUID(), slot, p.level().getGameTime());
    }

    private static int slot(HeroTrigger t) {
        return switch (t) {
            case KEY_1 -> 0;
            case KEY_2 -> 1;
            case KEY_3 -> 2;
            default -> -1;
        };
    }

    /** Forgets a player's key state (they left). */
    public static void forget(UUID player) {
        HOLD.forget(player);
        LAST_PRESS.remove(player);
        SUMMON_LAST.remove(player);
        FORM_LAST.remove(player);
    }

    /** Forgets everything (the server stopped). */
    public static void clear() {
        HOLD.clear();
        LAST_PRESS.clear();
        SUMMON_LAST.clear();
        FORM_LAST.clear();
    }
}
