package com.newuniverse.nusmp.firecast;

import com.newuniverse.nusmp.firecast.SalamanderMoodRules.Decision;
import com.newuniverse.nusmp.firecast.SalamanderMoodRules.Event;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the server half of the Salamander's moods (docs/magic/fire_casting.md, section 4.2). The rules are {@link SalamanderMoodRules} (a pure table and a pure tracker); this class only carries them
 * out: it asks the player's tracker what a situation means, writes the pose and face into the prop's one synced param (so the clients play the clip and swap the face plate from the clock, nothing
 * more is sent when the pose ends) and plays the creature sound through {@link SalamanderSounds} (nearby players, per listener gate).
 *
 * <p>Cost: a situation is rare (a cast, a hit, a kill, a link) and every situation has a cooldown in the tracker; the only periodic work is the idle check and the low health check, done inside the
 * Salamander prop's own tick that already runs once every {@link FireCastProps#NUDGE} ticks (a few integer compares, no world access, no allocation unless a pose starts). Nothing is stored beyond the
 * tracker's dozen numbers in the player's fire-style state, which goes on logout, death and dimension change with the rest of it. Everything is skipped with
 * {@code magic.salamanderMoods} / {@code magic.salamanderSounds} switched off.</p>
 */
final class SalamanderMood {
    private SalamanderMood() {}

    /** A situation of this player's Salamander (the small form on the shoulder, or the big form while it is out). */
    static void event(ServerPlayer p, FireCasting.State s, Event ev) {
        if (!FireCastConfig.moods() && !FireCastConfig.sounds()) return;
        ServerLevel sl = p.serverLevel();
        MagicPropEntity dragonProp = FireCasting.prop(sl, s.dragon), spirit = FireCasting.prop(sl, s.spirit);
        boolean dragon = dragonProp != null;
        MagicPropEntity prop = dragon ? dragonProp : spirit;
        if (prop == null) return;
        apply(sl, s, prop, dragon, s.mood.decide(ev, dragon, sl.getGameTime()), sl.getGameTime());
    }

    private static void apply(ServerLevel sl, FireCasting.State s, MagicPropEntity prop, boolean dragon, Decision d, long now) {
        if (d == SalamanderMoodRules.NOTHING || !d.any()) return;
        if (d.changesParam() && FireCastConfig.moods()) {
            int variant = dragon ? FireCastRules.V_DRAGON : FireCastRules.V_SALAMANDER;
            prop.setParam(SalamanderMoodRules.encode(variant, FireCastRules.hidden(prop.param()), d.pose(), d.face(), (int) now));
        }
        if (d.cue() != null && FireCastConfig.sounds()) SalamanderSounds.play(sl, prop.getX(), prop.getY() + (dragon ? 0.0 : 0.2), prop.getZ(), d.cue());
    }

    /** The small form has just been linked. */
    static void linked(ServerPlayer p, FireCasting.State s) {
        s.mood.begin(p.level().getGameTime());
        event(p, s, Event.LINK);
    }

    /** The link is ending (the prop is still there for this call). */
    static void unlinking(ServerPlayer p, FireCasting.State s) { event(p, s, Event.UNLINK); }

    /** A cast went through the Salamander's mouth: the pose and face follow the page's tier. */
    static void cast(ServerPlayer p, FireCasting.State s, double costPercent) { event(p, s, SalamanderMoodRules.castTier(costPercent)); }

    /**
     * The big form has just been summoned: the param it is spawned with (the descent plays from its own life, the breath begins {@link FireCastRules#DRAGON_BREATH_TICK} ticks after the summon: the
     * stamp of a pose may lie in the future) and the roar of its arrival. The old encoding when the moods are off.
     */
    static int dragonParam(long now) {
        return FireCastConfig.moods()
                ? SalamanderMoodRules.dragonSummonParam(now)
                : FireCastRules.param(FireCastRules.V_DRAGON, FireCastRules.DRAGON_BREATH_TICK);
    }

    static void dragonCame(ServerPlayer p, FireCasting.State s, MagicPropEntity prop, long now) {
        s.mood.playing(SalamanderMoodRules.POSE_BREATHE, now, now + FireCastRules.DRAGON_BREATH_TICK + SalamanderMoodRules.poseTicks(true, SalamanderMoodRules.POSE_BREATHE));
        if (FireCastConfig.sounds()) {
            ServerLevel sl = p.serverLevel();
            SalamanderSounds.play(sl, prop.getX(), prop.getY(), prop.getZ(), SalamanderMoodRules.moodFor(Event.DRAGON_COME, true).cue());
        }
    }

    /** Every few ticks for a live Salamander prop (the small form or the big one): the owner on low health, the idle poses. */
    static void tick(ServerPlayer p, FireCasting.State s, MagicPropEntity e, boolean dragon) {
        if (!FireCastConfig.moods() && !FireCastConfig.sounds()) return;
        if (dragon != (s.dragon != null)) return;                                                 // while the big form is out only it reacts; the hidden small one stays quiet
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (SalamanderMoodRules.lowHealth(p.getHealth(), p.getMaxHealth())) apply(sl, s, e, dragon, s.mood.decide(Event.LOW_HEALTH, dragon, now), now);
        apply(sl, s, e, dragon, s.mood.idle(dragon, now), now);
    }
}
