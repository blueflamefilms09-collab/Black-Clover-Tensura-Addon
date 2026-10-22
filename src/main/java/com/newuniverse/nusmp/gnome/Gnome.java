package com.newuniverse.nusmp.gnome;

import com.newuniverse.nusmp.audio.voice.Voice;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.gnome.GnomeMoodRules.Situation;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Why: the Gnome, the Earth Spirit of Black Clover (docs/magic/gnome.md): the Spirit Lord variant of EARTH that follows an Earth Magic caster and acts as the emitter of the earth spells, in the way
 * the Salamander does for fire, with a stone body of its own, a face that changes, poses for every situation, and a voice of its own. A per-player "spirit link": a small stone golem (the COMPANION) walks
 * beside its master while the link is on, and every Earth page leaves its fist (the emitter point, {@link GnomeRules#companionHand}). The big pages (cost tier 20 and up: Divided Mother Earth,
 * Rampaging Mother Earth, Swallowing Ground) call the FULL form, a stone giant that rises out of the ground ahead of its master, slams the earth with both fists and stands for
 * {@link GnomeRules#FULL_TICKS} ticks (it hardens its master meanwhile); it may come once per cooldown, one per caster and a few on the whole server. The earth pages themselves are unchanged (ids,
 * numbers, cooldowns): the Gnome only changes WHERE the spell starts, and what is seen and heard.
 *
 * <p><b>Moods.</b> {@link GnomeMoodRules} (pure) says which pose, face, grunt and spoken line go with each situation (link, unlink, a page of each tier, a hit on the master, the master nearly dead, a kill,
 * the master's death, a long stillness); {@link #react} applies one: the pose, the face and the tick stamp go into the synced prop word, the grunt is played to the players in range, and the spoken line goes
 * through the project's voice system ({@code audio.voice.Voice}) to the players within 32 blocks, at most one line every 4 seconds to one listener.</p>
 *
 * <p>All state is per player UUID in {@link #STATES} and is dropped on logout, death, respawn, dimension change and server stop ({@link #forget}); the players that have anything live are in {@link #LIVE}
 * and are visited every fourth tick, nobody else costs a tick. The server decides everything; the clients draw the two props from the synced kind, param and owner.</p>
 */
public final class Gnome {
    private Gnome() {}

    /** The colour of the earth family's effects (the Earth book's own style). */
    static final int EARTH_COLOR = com.newuniverse.nusmp.vfx.EarthFxStyle.color(com.newuniverse.nusmp.vfx.EarthFxStyle.style("EARTH"));

    /** The state of one player's Gnome. */
    static final class State {
        UUID spirit, full;
        long fullReadyAt, lastPacket = Long.MIN_VALUE / 4, lastMessage = Long.MIN_VALUE / 4, poseUntil;
        boolean dozing;
        String lastLine;
        final GnomeMoodRules.Cooldowns mood = new GnomeMoodRules.Cooldowns();
        final GnomeMoodRules.Stillness still = new GnomeMoodRules.Stillness();
    }

    static final Map<UUID, State> STATES = new HashMap<>();
    /** The players that have a Gnome out: only these are visited by the once-a-few-ticks upkeep. */
    static final Set<UUID> LIVE = new HashSet<>();
    static final GnomeLedger SPIRITS = new GnomeLedger(), FULLS = new GnomeLedger();
    static final GnomeSoundRules.Gate GATE = new GnomeSoundRules.Gate();
    /** One spoken line every 4 seconds to one listener, whoever speaks (keyed by the listener's UUID). */
    static final GnomeMoodRules.LineGate LINES = new GnomeMoodRules.LineGate();

    private static State state(UUID id) { return STATES.computeIfAbsent(id, k -> new State()); }

    // ================================================================================================ who may link
    /** May this player have a Gnome: the switch is on and the player holds the Earth grimoire skill. (The canon host, Feremena, has no grimoire of her own in the mod yet: see GnomeRules#isHostCanon.) */
    public static boolean eligible(ServerPlayer p) {
        return GnomeConfig.enabled() && SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_EARTH.getId()).isPresent();
    }

    private static void say(ServerPlayer p, State s, String key, String fallback, ChatFormatting color) {
        long now = p.level().getGameTime();
        if (!GnomeRules.gapOk(now, s.lastMessage, GnomeRules.MESSAGE_GAP)) return;
        s.lastMessage = now;
        p.displayClientMessage(Component.translatableWithFallback(key, fallback).withStyle(color), true);
    }

    // ================================================================================================ the key
    /** Toggles the companion (the Gnome key). Validated: the grimoire, the rate. */
    public static void toggle(ServerPlayer p) {
        State s = state(p.getUUID());
        long now = p.level().getGameTime();
        if (!GnomeRules.gapOk(now, s.lastPacket, GnomeRules.PACKET_GAP)) return;
        s.lastPacket = now;
        if (s.spirit != null) { unlink(p, s, true); return; }
        if (!eligible(p)) {
            say(p, s, "nusmp.gnome.need_earth", "The Gnome answers only an Earth Magic mage.", ChatFormatting.GRAY);
            if (!LIVE.contains(p.getUUID())) STATES.remove(p.getUUID(), s);
            return;
        }
        link(p, s);
    }

    static boolean link(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        if (!SPIRITS.acquire(p.getUUID(), GnomeRules.SPIRITS_PER_PLAYER, GnomeConfig.maxSpirits())) {
            say(p, s, "nusmp.gnome.full", "The Gnome cannot answer: too many spirits are out.", ChatFormatting.RED);
            return false;
        }
        double[] a = GnomeRules.companion(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.GNOME_1, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, 24000, GnomeRules.param(GnomeRules.V_COMPANION, 0), p);
        if (e == null) { SPIRITS.release(p.getUUID()); return false; }
        e.setSize(GnomeRules.COMPANION_W, GnomeRules.COMPANION_H);
        e.setTarget(p);
        s.spirit = e.getUUID();
        s.dozing = false;
        LIVE.add(p.getUUID());
        VfxSpawn.send(sl, VfxShape.EARTHFAM_BURST, new Vec3(a[0], a[1], a[2]), new Vec3(a[0], a[1] + 0.6, a[2]), EARTH_COLOR, 20, 0.5f);
        playCue(sl, p.getUUID(), "link", a[0], a[1], a[2]);
        react(p, s, Situation.LINK, e, -1);                                                         // it kneels and rises out of the earth, and greets
        say(p, s, "nusmp.gnome.linked", "The Gnome rises beside you.", ChatFormatting.GOLD);
        return true;
    }

    static void unlink(ServerPlayer p, State s, boolean speak) {
        ServerLevel sl = p.serverLevel();
        MagicPropEntity e = prop(sl, s.spirit);
        if (e != null) {
            if (speak) {                                                                             // it says goodbye and sinks into the ground, then it is gone
                playCue(sl, p.getUUID(), "unlink", e.getX(), e.getY(), e.getZ());
                react(p, s, Situation.UNLINK, e, -1);
                SpellRuntime.later(sl, GnomeRules.FULL_SINK_TICKS, () -> { if (!e.isRemoved()) e.discard(); });
            } else {
                e.discard();
            }
        }
        if (s.spirit != null) SPIRITS.release(p.getUUID());
        s.spirit = null;
        if (speak) say(p, s, "nusmp.gnome.unlinked", "The Gnome sinks back into the earth.", ChatFormatting.GRAY);
    }

    // ================================================================================================ moods
    /**
     * The Gnome reacts to a situation: if the situation's own cooldown is over, the pose, the face and the stamp go into the prop word of {@code who} (the giant when it stands, else the companion; the
     * clients play the clip and show the face from it), a persistent pose is given its end, the non-verbal cue is played and the spoken line is said, each by its own chance. {@code poseOverride} (a clip
     * id, or -1) replaces the mood's pose (the giant keeps its rise clip). Returns true when it reacted.
     */
    static boolean react(ServerPlayer p, State s, Situation sit, MagicPropEntity who, int poseOverride) {
        if (who == null) return false;
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (!s.mood.ready(sit, now)) return false;
        GnomeMoodRules.Mood m = GnomeMoodRules.of(sit);
        s.mood.mark(sit, now);
        int pose = poseOverride >= 0 ? poseOverride : m.pose();
        who.setParam(GnomeRules.withPose(who.param(), pose, m.face(), GnomeRules.stampOf(now)));
        s.poseUntil = sit == Situation.LOW_HEALTH ? now + GnomeMoodRules.GUARD_TICKS : 0L;
        s.dozing = sit == Situation.SLEEP || sit == Situation.OWNER_DEATH;
        int roll = sl.getRandom().nextInt(10000);
        if (GnomeMoodRules.makesCue(m, roll)) playCue(sl, p.getUUID(), m.cue(), who.getX(), who.getY() + 0.8, who.getZ());
        if (GnomeMoodRules.says(m, roll / 100 + 37)) speak(sl, s, who, m.lineGroup(), roll);
        return true;
    }

    /** The pose and the face go back to nothing (a guard that has been held long enough, a doze that is over). */
    private static void clearPose(ServerLevel sl, State s) {
        s.poseUntil = 0L;
        MagicPropEntity who = standing(sl, s);
        if (who != null) who.setParam(GnomeRules.withPose(who.param(), GnomeRules.CLIP_IDLE, GnomeRules.FACE_CALM, 0));
    }

    /** The prop that is out and speaks: the giant while it stands, else the companion. */
    private static MagicPropEntity standing(ServerLevel sl, State s) {
        MagicPropEntity full = prop(sl, s.full);
        return full != null ? full : prop(sl, s.spirit);
    }

    /** Says a line of a group to the players within 32 blocks of the Gnome, through the voice system, at most one line every 4 seconds to one listener. */
    private static void speak(ServerLevel sl, State s, MagicPropEntity who, String group, int roll) {
        String id = GnomeMoodRules.pick(group, roll, s.lastLine);
        if (id == null) return;
        s.lastLine = id;
        long now = sl.getGameTime();
        double r2 = GnomeMoodRules.LINE_RADIUS * GnomeMoodRules.LINE_RADIUS;
        for (ServerPlayer l : sl.players()) {
            if (l.isSpectator() || l.distanceToSqr(who) > r2) continue;
            if (LINES.allow(l.getUUID(), now)) Voice.say(l, id, GnomeMoodRules.LINE_REPEAT_TICKS);
        }
    }

    /** A hit landed on the master (the damage event, after the damage was taken). */
    public static void onOwnerHurt(ServerPlayer p, float damage) {
        State s = STATES.get(p.getUUID());
        if (s == null || (s.spirit == null && s.full == null)) return;
        Situation sit = GnomeMoodRules.fromDamage(damage, p.getHealth(), p.getMaxHealth());
        if (sit == null) return;
        ServerLevel sl = p.serverLevel();
        react(p, s, sit, standing(sl, s), -1);
    }

    /** The master killed something. */
    public static void onOwnerKill(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || (s.spirit == null && s.full == null)) return;
        react(p, s, Situation.KILL, standing(p.serverLevel(), s), -1);
    }

    /** The master died: the Gnome mourns where it stands (a doze with sleepy eyes, a few words) for three seconds, then sinks away; the state and the ledgers are dropped at once. */
    public static void onOwnerDeath(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s != null && (s.spirit != null || s.full != null) && p.level() instanceof ServerLevel sl) {
            MagicPropEntity who = standing(sl, s);
            if (who != null) {
                react(p, s, Situation.OWNER_DEATH, who, -1);
                MagicPropEntity a = prop(sl, s.spirit), b = prop(sl, s.full);
                SpellRuntime.later(sl, 60, () -> { if (a != null && !a.isRemoved()) a.discard(); if (b != null && !b.isRemoved()) b.discard(); });
                STATES.remove(p.getUUID());                                                          // the props live on for the farewell only: nothing else refers to them
                LIVE.remove(p.getUUID());
                SPIRITS.forget(p.getUUID());
                FULLS.forget(p.getUUID());
                GATE.forget(p.getUUID());
                return;
            }
        }
        forget(p);
    }

    // ================================================================================================ the cast
    /** Wraps the cast of every page of the Earth book: the page does exactly what it did, but while a Gnome is linked its start point moves to the Gnome's fist and the Gnome slams. */
    public static List<BookPage> wrap(List<BookPage> pages) {
        List<BookPage> out = new ArrayList<>(pages.size());
        for (BookPage pg : pages) {
            BookPage.Cast original = pg.cast();
            out.add(new BookPage(pg.id(), pg.name(), pg.incantation(), pg.costPercent(), pg.costFloor(), pg.cooldown(),
                    (book, inst, p, mode) -> cast(book, pg, original, inst, p, mode), pg.anim(), pg.weaponTag()));
        }
        return List.copyOf(out);
    }

    /** The cast of one Earth page of a caster: the generic call when no Gnome is linked, else the same call with the emitter set and the Gnome's slam scheduled. */
    static boolean cast(GrimoireBook book, BookPage page, BookPage.Cast original, ManasSkillInstance inst, ServerPlayer p, int mode) {
        State s = STATES.isEmpty() ? null : STATES.get(p.getUUID());
        if (s == null || s.spirit == null || !GnomeConfig.enabled()) return original.cast(book, inst, p, mode);
        ServerLevel sl = p.serverLevel();
        MagicPropEntity comp = prop(sl, s.spirit);
        if (comp == null) return original.cast(book, inst, p, mode);
        long now = sl.getGameTime();
        MagicPropEntity full = prop(sl, s.full);
        boolean wantFull = full == null && GnomeRules.callsFull(page.costPercent())
                && GnomeRules.fullAllowed(now, s.fullReadyAt, FULLS.count(p.getUUID()), FULLS.total(), GnomeConfig.maxFull());
        double[] at = wantFull ? GnomeRules.full(p.getX(), p.getY(), p.getZ(), p.yBodyRot) : null;
        double[] hand;
        if (full != null) hand = GnomeRules.fullHand(full.getX(), full.getY(), full.getZ(), full.getYRot());
        else if (wantFull) hand = GnomeRules.fullHand(at[0], at[1], at[2], p.yBodyRot);
        else hand = GnomeRules.companionHand(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        Vec3 mouth = new Vec3(hand[0], hand[1], hand[2]);
        if (!withEmitter(p, mouth, () -> original.cast(book, inst, p, mode))) return false;
        Situation sit = GnomeMoodRules.fromTier(page.costPercent());
        if (wantFull && summonFull(p, s, now, at)) {
            react(p, s, sit, prop(sl, s.full), GnomeRules.CLIP_IDLE);                                // the giant's own clip is the rise; the angry face and the words come with it
            return true;
        }
        MagicPropEntity who = full != null ? full : comp;
        react(p, s, sit, who, -1);                                                                    // the pose (punch, slam, both fists), the face and the voice of the tier
        int contact = GnomeRules.contactTick(GnomeMoodRules.of(sit).pose());
        if (contact >= 0) contact(p, sl, contact, mouth, full != null || wantFull);
        return true;
    }

    /** Calls the full form: the ledgers, the entity, the rise; the slam comes on its tick. */
    static boolean summonFull(ServerPlayer p, State s, long now, double[] at) {
        if (!FULLS.acquire(p.getUUID(), GnomeRules.FULL_PER_PLAYER, GnomeConfig.maxFull())) return false;
        ServerLevel sl = p.serverLevel();
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.GNOME_1, new Vec3(at[0], at[1], at[2]), p.yBodyRot, 1.0f, GnomeRules.FULL_TICKS, GnomeRules.param(GnomeRules.V_FULL, 0), p);
        if (e == null) { FULLS.release(p.getUUID()); return false; }
        e.setSize(GnomeRules.FULL_W, GnomeRules.FULL_H);
        e.setTarget(p);
        s.full = e.getUUID();
        s.fullReadyAt = now + GnomeConfig.fullCooldown();
        LIVE.add(p.getUUID());
        VfxSpawn.send(sl, VfxShape.EARTHFAM_ZONE, new Vec3(at[0], at[1] + 0.05, at[2]), new Vec3(at[0], at[1] + 1, at[2]), EARTH_COLOR, GnomeRules.FULL_TICKS, 3.4f);      // it stands on the earth sigil
        playCue(sl, p.getUUID(), "rise", at[0], at[1], at[2]);
        double[] h = GnomeRules.fullHand(at[0], at[1], at[2], p.yBodyRot);
        contact(p, sl, GnomeRules.FULL_SLAM_TICK, new Vec3(h[0], h[1], h[2]), true);
        return true;
    }

    /** The moment the fists land: one earth burst at the fist and the crash (or the grinding of the small one), after {@code delay} ticks, if the caster is still there. */
    private static void contact(ServerPlayer p, ServerLevel sl, int delay, Vec3 at, boolean big) {
        UUID id = p.getUUID();
        SpellRuntime.later(sl, delay, () -> {
            State s = STATES.get(id);
            if (s == null || !p.isAlive() || p.serverLevel() != sl) return;
            Vec3 ground = new Vec3(at.x, at.y - (big ? 1.3 : 0.9), at.z);
            VfxSpawn.send(sl, VfxShape.EARTHFAM_BURST, ground, ground.add(0, 1, 0), EARTH_COLOR, big ? 30 : 16, big ? 1.5f : 0.55f);
            playCue(sl, id, big ? "slam" : "cast", at.x, at.y, at.z);
        });
    }

    // ================================================================================================ the emitter
    private static UUID emitterOwner;
    private static Vec3 emitterAt;

    /** Runs {@code body} (a spell's cast, synchronous) with the caster's start point moved to {@code at}; true when the body says so. */
    static boolean withEmitter(ServerPlayer p, Vec3 at, BooleanSupplier body) {
        UUID prevOwner = emitterOwner;
        Vec3 prevAt = emitterAt;
        emitterOwner = p.getUUID();
        emitterAt = at;
        try {
            return body.getAsBoolean();
        } finally {
            emitterOwner = prevOwner;
            emitterAt = prevAt;
        }
    }

    /** A spell's start point: the Gnome's fist while one is set for this caster and the start was the caster's own, else the start unchanged. */
    public static Vec3 origin(LivingEntity caster, Vec3 start) {
        if (emitterOwner == null || emitterAt == null || !emitterOwner.equals(caster.getUUID())) return start;
        Vec3 eye = caster.getEyePosition(), feet = caster.position();
        double[] r = GnomeRules.emit(start.x, start.y, start.z, eye.x, eye.y, eye.z, feet.x, feet.y, feet.z, emitterAt.x, emitterAt.y, emitterAt.z);
        return new Vec3(r[0], r[1], r[2]);
    }

    // ================================================================================================ sound
    /** Plays a cue of the Gnome's own sound set at a point, to the players in its range, through the gate (a cooldown per cue, a rate per owner, a rate for the server). */
    static void playCue(ServerLevel sl, UUID owner, String cueId, double x, double y, double z) {
        GnomeSoundRules.Cue c = GnomeSoundRules.cue(cueId);
        SoundEvent ev = GnomeSounds.event(cueId);
        if (c == null || ev == null || !GATE.allow(owner, cueId, sl.getGameTime())) return;
        sl.playSound(null, x, y, z, ev, SoundSource.NEUTRAL, c.volume(), c.pitch(sl.getRandom().nextDouble()));
    }

    // ================================================================================================ upkeep and cleanup
    /**
     * Every fourth tick for the players in {@link #LIVE}: lost props are dropped, the link ends when the book or the switch is gone, the master is hardened while the giant stands, a held guard
     * ends, and the stillness of the master is watched: half a minute without a step and the Gnome dozes off (sleepy eyes, a snore), a step wakes it, and after twice as long an idle remark.
     */
    public static void upkeep(ServerPlayer p) {
        UUID id = p.getUUID();
        State s = STATES.get(id);
        if (s == null) { LIVE.remove(id); return; }
        ServerLevel sl = p.serverLevel();
        if (s.full != null && prop(sl, s.full) == null) { s.full = null; FULLS.release(id); }
        if (s.spirit != null && prop(sl, s.spirit) == null) { s.spirit = null; SPIRITS.release(id); }
        if (s.spirit != null && p.tickCount % 20 == 0 && !eligible(p)) unlink(p, s, false);
        if (s.full != null && p.tickCount % 20 == 0) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false));      // the giant hardens its master (the old Earth spirit's aura), only while it stands
        long now = sl.getGameTime();
        if (s.poseUntil != 0L && now >= s.poseUntil) clearPose(sl, s);
        if (s.spirit != null && s.full == null) {
            boolean moved = s.still.update(now, p.getX(), p.getY(), p.getZ());
            int still = s.still.stillTicks(now);
            MagicPropEntity e = prop(sl, s.spirit);
            if (s.dozing) {
                if (moved) react(p, s, Situation.WAKE, e, -1);
            } else if (e != null) {
                if (GnomeMoodRules.dozeDue(still)) react(p, s, Situation.SLEEP, e, -1);
                else if (GnomeMoodRules.idleRemarkDue(still, s.mood.sinceAny(now))) react(p, s, Situation.IDLE, e, -1);
            }
        }
        if (s.spirit == null && s.full == null) LIVE.remove(id);
    }

    /** Everything of this player goes: logout, respawn, dimension change (and a death that has nothing to mourn). */
    public static void forget(ServerPlayer p) {
        UUID id = p.getUUID();
        State s = STATES.remove(id);
        LIVE.remove(id);
        SPIRITS.forget(id);
        FULLS.forget(id);
        GATE.forget(id);
        LINES.forget(id);
        if (s == null || !(p.level() instanceof ServerLevel sl)) return;
        for (UUID e : new UUID[]{s.spirit, s.full}) if (e != null && sl.getEntity(e) instanceof MagicPropEntity m) m.discard();
    }

    /** The server stops: nothing may survive into the next world. */
    public static void clearAll() {
        STATES.clear();
        LIVE.clear();
        SPIRITS.clear();
        FULLS.clear();
        GATE.clear();
        LINES.clear();
        emitterOwner = null;
        emitterAt = null;
    }

    // ================================================================================================ shared with the props (same package)
    static State stateOf(UUID id) { return STATES.get(id); }

    /** The state of a player, created when it does not exist (the self-test and the key path). */
    static State state0(ServerPlayer p) { return state(p.getUUID()); }

    /** A prop ended on its own (its life ran out, or it was an orphan): if it is the one the player's state names, give its ledger slot back. */
    public static void propEnded(UUID owner, int variant, UUID entity) {
        State s = owner == null ? null : STATES.get(owner);
        if (s == null || entity == null) return;
        if (variant == GnomeRules.V_FULL && entity.equals(s.full)) { s.full = null; FULLS.release(owner); }
        else if (variant == GnomeRules.V_COMPANION && entity.equals(s.spirit)) { s.spirit = null; SPIRITS.release(owner); }
    }

    /** The prop entity with this id in this level, or null. */
    static MagicPropEntity prop(ServerLevel sl, UUID id) { return id != null && sl.getEntity(id) instanceof MagicPropEntity e && !e.isRemoved() ? e : null; }

    /** For the self-test: how many live things the ledgers count. */
    public static int liveCount() { return SPIRITS.total() + FULLS.total(); }
}
