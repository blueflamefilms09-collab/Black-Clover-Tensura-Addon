package com.newuniverse.nusmp.attune;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.power.PowerConfig;
import com.newuniverse.nusmp.power.PowerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the server half of meditation. {@link #open} (from /meditate) records where and when the player sat down and sends them what they own; {@link #confirm} (the one packet up) is checked
 * again from nothing: config on, a session open, the player still within {@link #MAX_MOVE} blocks and {@link #SESSION_TICKS} of it, the ask gap, every id owned, the slots, the cooldown, and the
 * element clashes (a clash is refused with a short, never lethal backlash and nothing changes). {@link #factor} is the damage hook's arithmetic. Sessions are keyed by player UUID and dropped on
 * logout, death and a change of dimension; nothing runs per tick.
 */
public final class AttuneService {
    private AttuneService() {}

    /** A confirm is taken only this close to where the screen opened (blocks) and this soon after it (ticks). */
    public static final double MAX_MOVE = 1.5;
    public static final int SESSION_TICKS = 2400;
    /** One open or confirm per player per this many ticks (a spammed packet costs one map lookup). */
    public static final int ASK_GAP_TICKS = 10;

    private record Session(Vec3 at, long openedAt) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_ASK = new ConcurrentHashMap<>();

    static boolean hasSession(UUID id) { return SESSIONS.containsKey(id); }

    /** Self-tests only: a stand-in's packets arrive in one tick, so the ask gap is reset between them. */
    static void resetAsk(UUID id) { LAST_ASK.remove(id); }

    public static void forget(UUID id) {
        SESSIONS.remove(id);
        LAST_ASK.remove(id);
    }

    public static void clearAll() {
        SESSIONS.clear();
        LAST_ASK.clear();
    }

    private static boolean asked(ServerPlayer p, long now) {
        Long last = LAST_ASK.get(p.getUUID());
        if (last != null && now >= last && now - last < ASK_GAP_TICKS) return false;
        LAST_ASK.put(p.getUUID(), now);
        return true;
    }

    static void say(ServerPlayer p, String key, String fallback, ChatFormatting color, Object... args) {
        p.sendSystemMessage(Component.translatableWithFallback(key, fallback, args).withStyle(color));
    }

    // ------------------------------------------------------------------ open
    /** Sits the player down and sends the screen's data. Returns false (with a message) when meditation is off or asked too fast. */
    public static boolean open(ServerPlayer p) {
        if (!AttuneConfig.enabled()) { say(p, "attune.msg.off", "Meditation attunement is switched off on this server.", ChatFormatting.GRAY); return false; }
        long now = p.level().getGameTime();
        if (!asked(p, now)) return false;
        SESSIONS.put(p.getUUID(), new Session(p.position(), now));
        AttuneNet.send(p, openPacket(p, now));
        return true;
    }

    static AttuneNet.Open openPacket(ServerPlayer p, long now) {
        List<AttuneNet.Entry> owned = new ArrayList<>();
        for (AttunePowers.View v : AttunePowers.owned(p)) owned.add(new AttuneNet.Entry(v.power().id(), v.power().system(), v.power().elements(), v.power().rank(), v.label()));
        AttuneStore.State s = AttuneStore.peek(p);
        List<String> attuned = s == null ? List.of() : List.copyOf(s.slots);
        long cd = s == null ? 0 : AttuneRules.cooldownLeft(s.lastChange, now, AttuneConfig.cooldownTicks());
        return new AttuneNet.Open(AttuneConfig.slots(), (int) cd, (int) Math.round(AttuneConfig.resonance() * 100), attuned, owned);
    }

    // ------------------------------------------------------------------ confirm
    public enum Verdict { DONE, OFF, RATE, NO_SESSION, MOVED, BAD, COOLDOWN, REJECTED }

    /** The one packet up. Everything is decided here, from the server's own data. */
    public static Verdict confirm(ServerPlayer p, List<String> ids) {
        if (!AttuneConfig.enabled()) return Verdict.OFF;
        long now = p.level().getGameTime();
        if (!asked(p, now)) return Verdict.RATE;
        Session ses = SESSIONS.get(p.getUUID());
        if (ses == null) { say(p, "attune.msg.no_session", "Sit down with /meditate first.", ChatFormatting.GRAY); return Verdict.NO_SESSION; }
        Vec3 d = p.position().subtract(ses.at());
        if (!AttuneRules.stillMeditating(d.x, d.y, d.z, MAX_MOVE, ses.openedAt(), now, SESSION_TICKS)) {
            SESSIONS.remove(p.getUUID());
            say(p, "attune.msg.moved", "Your meditation broke: stay still while you attune.", ChatFormatting.YELLOW);
            return Verdict.MOVED;
        }
        if (ids == null || ids.size() > AttuneRules.MAX_SLOTS) return Verdict.BAD;
        List<AttunePowers.View> owned = AttunePowers.owned(p);
        Map<String, AttunePowers.View> byId = new HashMap<>();
        for (AttunePowers.View v : owned) byId.put(v.power().id(), v);
        List<AttuneRules.Power> chosen = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (!AttuneRules.validId(id)) { say(p, "attune.msg.not_owned", "You do not hold that power.", ChatFormatting.RED); return Verdict.BAD; }
            AttunePowers.View v = byId.get(id);
            chosen.add(v != null ? v.power() : new AttuneRules.Power(id, AttuneRules.systemOf(id), 0, 0));
        }
        AttuneRules.Refusal r = AttuneRules.check(chosen, AttunePowers.ids(owned), AttuneConfig.slots());
        if (r != null && !r.reason().equals("reject")) {
            say(p, "attune.msg." + r.reason(), "That attunement cannot be made (" + r.reason() + ").", ChatFormatting.RED);
            return Verdict.BAD;
        }
        AttuneStore.State s = AttuneStore.peek(p);
        if (s != null && s.slots.equals(ids)) { SESSIONS.remove(p.getUUID()); return Verdict.DONE; }          // nothing changes: no cooldown spent
        long left = s == null ? 0 : AttuneRules.cooldownLeft(s.lastChange, now, AttuneConfig.cooldownTicks());
        if (left > 0) {
            say(p, "attune.msg.cooldown", "Your mind is not settled yet: %s s.", ChatFormatting.YELLOW, (left + 19) / 20);
            return Verdict.COOLDOWN;
        }
        if (r != null) {                                                                                           // the clash: refused, a short backlash, nothing changes
            String a = labelOf(byId, chosen.get(r.first())), b = labelOf(byId, chosen.get(r.second()));
            say(p, "attune.msg.reject", "%s and %s reject each other: their elements clash.", ChatFormatting.RED, a, b);
            backlash(p);
            return Verdict.REJECTED;
        }
        AttuneStore.State w = AttuneStore.of(p);
        w.slots.clear();
        w.slots.addAll(ids);
        w.lastChange = Math.max(1L, now);
        SESSIONS.remove(p.getUUID());
        int pairs = AttuneRules.resonatingPairs(chosen);
        int bonus = (int) Math.round((AttuneRules.resonance(pairs, AttuneConfig.resonance()) - 1.0) * 100);
        say(p, "attune.msg.done", "Attuned: %s power(s), resonance +%s%%.", ChatFormatting.GOLD, ids.size(), bonus);
        return Verdict.DONE;
    }

    private static String labelOf(Map<String, AttunePowers.View> byId, AttuneRules.Power pw) {
        AttunePowers.View v = byId.get(pw.id());
        return v != null ? v.label() : pw.id();
    }

    /** The clash hurts a little and slows for three seconds; never lethal ({@link AttuneRules#backlash}). */
    static void backlash(ServerPlayer p) {
        float dmg = AttuneRules.backlash(p.getHealth());
        if (dmg > 0f) p.hurt(p.damageSources().magic(), dmg);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
    }

    /** Forget the attunement (the command). The cooldown is kept. */
    public static void clear(ServerPlayer p) {
        AttuneStore.State s = AttuneStore.peek(p);
        if (s != null) s.slots.clear();
    }

    // ------------------------------------------------------------------ damage
    /**
     * The extra factor for one blow of {@code p}. The powers out now are ordered attuned first (slot order), then the rest; a clashing pair loses its weaker side (when suppression is on). The factor
     * wanted is the largest of: the 3.74.0 power raise of a Djinn or Beast Will that is out and not suppressed, the attunement factor of each attuned power out and not suppressed, and the hero weapon factor
     * the power raise already counts; times the resonance of the attuned powers out. Divided by what the power raise already put on the blow, so the two hooks never multiply each other.
     * 1.0 = leave the blow alone.
     */
    public static double factor(ServerPlayer p) {
        List<AttuneRules.Power> out = AttunePowers.out(p);
        if (out.isEmpty()) return 1.0;
        AttuneStore.State s = AttuneStore.peek(p);
        if ((s == null || s.slots.isEmpty()) && out.size() < 2) return 1.0;                                    // no attunement and nothing to clash: the old behaviour
        List<String> slots = s == null ? List.of() : s.slots;
        List<AttuneRules.Power> ordered = AttuneRules.order(out, slots);
        Set<Integer> gone = gone(ordered, slots);
        boolean power = PowerConfig.enabled();
        double want = 1.0;
        List<AttuneRules.Power> resonating = new ArrayList<>(4);
        for (int i = 0; i < ordered.size(); i++) {
            if (gone.contains(i)) continue;
            AttuneRules.Power pw = ordered.get(i);
            if (power && pw.system() == AttuneRules.DJINN && PowerEvents.djinnRaised(p)) want = Math.max(want, PowerConfig.djinn());
            if (power && pw.system() == AttuneRules.BEAST) want = Math.max(want, PowerConfig.beastWill());
            if (slots.contains(pw.id())) {
                want = Math.max(want, AttuneConfig.factor(pw.system()));
                resonating.add(pw);
            }
        }
        double applied = PowerEvents.appliedFactor(p);
        double hero = PowerEvents.heroFactorOf(p);
        want = Math.max(want, hero) * AttuneRules.resonance(AttuneRules.resonatingPairs(resonating), AttuneConfig.resonance());
        return AttuneRules.extra(want, applied);
    }

    /** The powers out that lose their damage raise: the weaker of a clash (suppressClashes) and, with the equip limit on, the ones it blocks (an old save with three out, or a Devil with a Spirit). */
    static Set<Integer> gone(List<AttuneRules.Power> ordered, List<String> slots) {
        Set<Integer> gone = new java.util.HashSet<>();
        if (AttuneConfig.suppress()) gone.addAll(AttuneRules.suppressed(ordered));
        if (AttuneConfig.equipLimit()) gone.addAll(AttuneRules.blocked(ordered, slots));
        return gone;
    }

    /** The blow after {@link #factor}, held at the PvP hit cap against a player. */
    public static float apply(ServerPlayer dealer, LivingEntity target, float amount) {
        double f = factor(dealer);
        if (f == 1.0) return amount;
        return AttuneRules.dealt(amount, f, target instanceof Player, target.getMaxHealth(), NUGameRules.pvpHitCap(target.level()));
    }

    /** A line per power for /meditate status: attuned or not, out or not, suppressed or not. */
    public static List<String> status(ServerPlayer p) {
        List<String> lines = new ArrayList<>();
        AttuneStore.State s = AttuneStore.peek(p);
        List<String> slots = s == null ? List.of() : s.slots;
        List<AttuneRules.Power> ordered = AttuneRules.order(AttunePowers.out(p), slots);
        Set<Integer> gone = new java.util.HashSet<>(AttuneRules.suppressed(ordered));
        if (AttuneConfig.equipLimit()) gone.addAll(AttuneRules.blocked(ordered, slots));
        lines.add("Slots " + slots.size() + "/" + AttuneConfig.slots() + (AttuneConfig.enabled() ? "" : " (switched off)"));
        for (AttunePowers.View v : AttunePowers.owned(p)) {
            String id = v.power().id();
            int at = ordered.indexOf(v.power());
            lines.add(" " + v.label() + " [" + AttuneRules.systemKey(v.power().system()) + ", " + AttuneRules.elements(v.power().elements()) + "]"
                    + (slots.contains(id) ? " attuned" : "") + (at >= 0 ? " out" : "") + (at >= 0 && gone.contains(at) ? " SUPPRESSED" : ""));
        }
        return lines;
    }
}
