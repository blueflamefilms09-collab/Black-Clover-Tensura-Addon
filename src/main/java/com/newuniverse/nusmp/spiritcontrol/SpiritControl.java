package com.newuniverse.nusmp.spiritcontrol;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the ONE control plane for every spirit of the mod (replacing the spirit keybindings, which were removed in 3.0.0). The grimoire's spirit pages and the {@code /multiverse spirit} command both call the
 * methods here, so they can never disagree: summon, release, toggle, one of the spirit's own abilities, and the combat mode toggle. Each spirit package registers one {@link SpiritHandle} (an append-only line
 * in its Boot); the handle does what that spirit's old key did. Everything is decided on the server: the player must be a real player the spirit answers, the control must be switched on
 * ({@code magic.spiritControl}), the ability must be one the spirit lists, the slot must be in range, and a player may ask at most once per {@code control.askGapTicks} ticks (per UUID, from page or command alike).
 * State is only the rate-limit map (a few longs per player who asked, dropped at logout) and, in {@link SpiritCombat}, the combat mode.
 */
public final class SpiritControl {
    private SpiritControl() {}

    /** Every registered spirit, in registration order. */
    public static final SpiritRegistry<SpiritHandle> REG = new SpiritRegistry<>();

    private static final PlayerGap GAP = new PlayerGap();

    /** The widest waypoint slot number a command may name (the Space spirit's hard limit, 0-based below it). */
    public static final int MAX_SLOT = 6;

    /** What an ask ended in. */
    public enum Verdict { DONE, REFUSED, RATE, OFF, NONE }

    /** Registers a spirit (called from the spirit package's Boot; a repeat of the same object is ignored). */
    public static void register(SpiritHandle h) { REG.register(h); }

    // ================================================================================================ who answers
    /** The spirits that answer this player now (enabled, and the package's own rule holds), in registration order. */
    public static List<SpiritHandle> eligible(ServerPlayer p) {
        List<SpiritHandle> out = new ArrayList<>(2);
        if (!SpiritControlConfig.enabled()) return out;
        for (SpiritHandle h : REG.all()) if (h.eligible(p)) out.add(h);
        return out;
    }

    /** The spirits of the book of this magic that answer this player. */
    public static List<SpiritHandle> eligibleFor(ServerPlayer p, String magic) {
        List<SpiritHandle> out = new ArrayList<>(2);
        if (!SpiritControlConfig.enabled() || magic == null) return out;
        for (SpiritHandle h : REG.forMagic(magic)) if (h.eligibleFor(p, magic)) out.add(h);
        return out;
    }

    /** The spirit a command names (by id), or, with no name, the first that answers the player and is out (when {@code preferOut}) or the first that answers. */
    public static SpiritHandle find(ServerPlayer p, String idOrNull, boolean preferOut) {
        if (idOrNull != null) {
            SpiritHandle h = REG.get(idOrNull);
            return h != null && h.eligible(p) && SpiritControlConfig.enabled() ? h : null;
        }
        List<SpiritHandle> all = eligible(p);
        if (all.isEmpty()) return null;
        if (preferOut) for (SpiritHandle h : all) if (h.isOut(p)) return h;
        for (SpiritHandle h : all) if (!h.isOut(p)) return h;
        return all.get(0);
    }

    // ================================================================================================ the asks
    private static boolean allowed(ServerPlayer p) { return GAP.allow(p.getUUID(), p.level().getGameTime(), SpiritControlConfig.askGap()); }

    private static void say(ServerPlayer p, String key, String text, ChatFormatting color, Object... args) {
        p.displayClientMessage(Component.translatableWithFallback(key, text, args).withStyle(color), true);
    }

    /** Calls the spirit. */
    public static Verdict summon(ServerPlayer p, SpiritHandle h) {
        if (!SpiritControlConfig.enabled()) return Verdict.OFF;
        if (h == null) { say(p, "nusmp.spirit_control.none", "No spirit answers you.", ChatFormatting.GRAY); return Verdict.NONE; }
        if (!h.eligible(p)) { say(p, "nusmp.spirit_control.refused", "%s does not answer you.", ChatFormatting.GRAY, h.display()); return Verdict.REFUSED; }
        if (!allowed(p)) return Verdict.RATE;
        if (h.isOut(p)) return Verdict.DONE;
        return h.summon(p) ? Verdict.DONE : Verdict.REFUSED;
    }

    /** Sends the spirit away. */
    public static Verdict release(ServerPlayer p, SpiritHandle h) {
        if (!SpiritControlConfig.enabled()) return Verdict.OFF;
        if (h == null) { say(p, "nusmp.spirit_control.none", "No spirit answers you.", ChatFormatting.GRAY); return Verdict.NONE; }
        if (!allowed(p)) return Verdict.RATE;
        if (!h.isOut(p)) { say(p, "nusmp.spirit_control.notout", "%s is not out.", ChatFormatting.GRAY, h.display()); return Verdict.NONE; }
        h.release(p);
        return Verdict.DONE;
    }

    /** Summon when away, release when out (the grimoire page). */
    public static Verdict toggle(ServerPlayer p, SpiritHandle h) {
        if (h == null) return summon(p, null);
        return h.isOut(p) ? release(p, h) : summon(p, h);
    }

    /** One of the spirit's own abilities. {@code arg} is the 0-based waypoint slot (0 for abilities that take none). */
    public static Verdict ability(ServerPlayer p, SpiritHandle h, String abilityId, int arg) {
        if (!SpiritControlConfig.enabled()) return Verdict.OFF;
        if (h == null) { say(p, "nusmp.spirit_control.none", "No spirit answers you.", ChatFormatting.GRAY); return Verdict.NONE; }
        SpiritAbilityDef def = abilityOf(h, abilityId);
        if (def == null || arg < 0 || arg >= MAX_SLOT) return Verdict.NONE;
        if (!h.eligible(p)) { say(p, "nusmp.spirit_control.refused", "%s does not answer you.", ChatFormatting.GRAY, h.display()); return Verdict.REFUSED; }
        if (!h.isOut(p)) { say(p, "nusmp.spirit_control.callfirst", "Call %s first.", ChatFormatting.GRAY, h.display()); return Verdict.NONE; }
        if (!allowed(p)) return Verdict.RATE;
        return h.ability(p, def.id(), arg) ? Verdict.DONE : Verdict.REFUSED;
    }

    /** The ability of this spirit with this id (any case), or null. */
    public static SpiritAbilityDef abilityOf(SpiritInfo h, String id) {
        if (h == null || id == null) return null;
        for (SpiritAbilityDef d : h.abilities()) if (d.id().equalsIgnoreCase(id)) return d;
        return null;
    }

    /** Switches combat mode on or off for this player. */
    public static Verdict combat(ServerPlayer p, boolean on) {
        if (!SpiritControlConfig.enabled()) return Verdict.OFF;
        if (on && !SpiritControlConfig.combat()) { say(p, "nusmp.spirit_control.combat_off", "Spirit combat mode is switched off on this server.", ChatFormatting.GRAY); return Verdict.OFF; }
        if (on && eligible(p).isEmpty()) { say(p, "nusmp.spirit_control.none", "No spirit answers you.", ChatFormatting.GRAY); return Verdict.NONE; }
        if (!allowed(p)) return Verdict.RATE;
        SpiritCombat.set(p, on);
        return Verdict.DONE;
    }

    /** Toggles combat mode (the grimoire page). */
    public static Verdict toggleCombat(ServerPlayer p) { return combat(p, !SpiritCombat.isOn(p.getUUID())); }

    // ================================================================================================ the report
    /** The lines of {@code /multiverse spirit status}: each spirit that answers, whether it is out, its abilities, and the combat mode with its pools. */
    public static List<String> status(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        if (!SpiritControlConfig.enabled()) { out.add("The spirit control plane is switched off on this server."); return out; }
        List<SpiritHandle> all = eligible(p);
        if (all.isEmpty()) out.add("No spirit answers you.");
        for (SpiritHandle h : all) {
            StringBuilder sb = new StringBuilder(h.display()).append(": ").append(h.isOut(p) ? "out" : "away");
            if (!h.abilities().isEmpty()) {
                sb.append("; abilities:");
                for (SpiritAbilityDef d : h.abilities()) sb.append(' ').append(d.id());
            }
            String mana = SpiritCombat.manaLine(p, h);
            if (mana != null) sb.append("; ").append(mana);
            out.add(sb.toString());
        }
        out.add("Combat mode: " + (SpiritCombat.isOn(p.getUUID()) ? "ON" : "off"));
        return out;
    }

    // ================================================================================================ cleanup
    /** A player left, died or changed dimension: their asks, their combat mode and their pools go. */
    public static void forget(UUID id) {
        GAP.forget(id);
        SpiritCombat.forget(id);
    }

    public static void clearAll() {
        GAP.clear();
        SpiritCombat.clearAll();
    }
}
