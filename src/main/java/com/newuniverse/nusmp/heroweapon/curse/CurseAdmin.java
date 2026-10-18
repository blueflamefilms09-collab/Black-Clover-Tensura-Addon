package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: an owner who tests or runs a server needs to read the curse of a player and to set every part of it by hand (despair, corruption, a series, an offer, a wipe,
 * a cleansing), and the in-game self-test needs the very same operations. They are written once here, on top of the same machine and service the game uses, so a
 * command does exactly what a real event would do (an unlocked series learns its first form and takes the tax, an offer whispers, corruption at 1000 starts a real
 * Overrun) and the command class stays a thin list of literals. Every method works on a bonded player and says in its answer when it could not.
 */
final class CurseAdmin {
    private CurseAdmin() {}

    /** The state of a player as three short lines. */
    static List<String> show(ServerPlayer p, long now, CurseTuning t) {
        List<String> out = new ArrayList<>();
        HeroBond b = HeroBonds.of(p);
        String name = p.getGameProfile().getName();
        if (!b.isBound()) {
            out.add(name + " has no hero weapon.");
            return out;
        }
        CurseState s = CurseStore.load(b);
        HeroForm form = HeroForms.active(b);
        out.add(name + ": " + b.kind().display() + ", level " + b.level() + ", form " + b.form() + (form != null && form.curse() ? " (curse form of " + form.sin() + ")" : ""));
        out.add("despair " + s.despair + "/1000, corruption " + s.corruption + "/1000, offer " + (CurseMachine.offerOpen(s, now, t) ? s.offered + " (" + CurseMachine.offerTicksLeft(s, now, t) / 20 + " s left)" : "none")
                + ", overrun " + (CurseMachine.inOverrun(s, now) ? (s.overrunUntil - now) / 20 + " s left" : "none") + ", weak " + (CurseMachine.weak(s, now) ? (s.weakUntil - now) / 20 + " s left" : "no"));
        out.add("series " + (s.series.isEmpty() ? "none" : String.join(", ", s.series)) + ", tax " + s.tax + " hearts, charm refunds used " + s.refunds);
        return out;
    }

    /** Sets the despair (0 to 1000); the weapon whispers at the next look if it reached the threshold. */
    static void setDespair(ServerPlayer p, int amount, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        CurseState s = CurseStore.load(b);
        s.despair = Math.max(0, Math.min(CurseTuning.MAX, amount));
        s.lastGain = now;
        CurseStore.save(b, s);
        CurseSync.push(p, s, CurseService.shownSin(b), now, true, t);
    }

    /** Sets the corruption (0 to 1000); 1000 starts a real Overrun at once. */
    static void setCorruption(ServerPlayer p, int amount, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        CurseState s = CurseStore.load(b);
        s.corruption = Math.max(0, Math.min(CurseTuning.MAX, amount));
        boolean start = s.corruption >= CurseTuning.MAX && !CurseMachine.inOverrun(s, now);
        if (start) CurseMachine.startOverrun(s, now, t, p.getRandom().nextDouble());
        CurseStore.save(b, s);
        if (start) CurseService.overrunStart(p, b, s, CurseService.wornSin(b), now, t);
        else CurseSync.push(p, s, CurseService.shownSin(b), now, true, t);
    }

    /**
     * Unlocks a Curse Series without an offer: the first curse form of the sin is learned and, with {@code tax}, the stage's hearts are taken like a real Embrace.
     * Returns null on success or the reason it could not.
     */
    static String unlock(ServerPlayer p, Sin sin, boolean tax, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        CurseState s = CurseStore.load(b);
        int hearts = CurseMachine.grant(s, sin, tax, t);
        if (hearts < 0) return p.getGameProfile().getName() + " has embraced " + sin.label() + " already.";
        CurseStore.save(b, s);
        HeroForm learned = CurseService.learnFirstForm(p, b, sin);
        CurseEffects.ensureTax(p, s.tax, t.minHearts, true);
        CurseService.showEmbrace(p, sin, learned, hearts);
        CurseSync.push(p, s, CurseService.shownSin(b), now, true, t);
        return null;
    }

    /** Opens an offer of a sin at once. Returns null on success or the reason it could not. */
    static String offer(ServerPlayer p, Sin sin, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        CurseState s = CurseStore.load(b);
        if (!CurseMachine.forceOffer(s, sin, now)) return "Cannot offer " + sin.label() + ": it is embraced already or another offer is open.";
        CurseStore.save(b, s);
        CurseService.announceOffer(p, b, s, sin, now, t);
        return null;
    }

    /** Wipes the whole record: series, tax, despair, corruption, offer, Overrun, weakness. A curse form being worn goes back to the base form. */
    static void clear(ServerPlayer p, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        CurseState s = CurseStore.load(b);
        CurseMachine.clearAll(s);
        CurseStore.save(b, s);
        if (CurseService.activeFormIsCurse(b)) CurseService.forceBase(p, b);
        CurseEffects.clearModifiers(p);
        p.removeEffect(MobEffects.WEAKNESS);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        CurseSync.push(p, s, null, now, true, t);
    }

    /** A Purification Charm without the item (the same effect, the same refund rule). True when it did something. */
    static boolean purify(ServerPlayer p, long now, CurseTuning t) { return CurseService.charm(p, now, t); }

    /** The sin with this id, or null; accepts any case. */
    static Sin sinOf(String id) { return id == null ? null : Sin.byId(id.toLowerCase(Locale.ROOT)); }
}
