package com.newuniverse.nusmp.gnome;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
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
 * the Salamander does for fire, with a stone body of its own and a sound set of its own. A per-player "spirit link": a small stone golem (the COMPANION) walks beside its master while the link is on, and
 * every Earth page leaves its fist (the emitter point, {@link GnomeRules#companionHand}). The big pages (cost tier 20 and up: Divided Mother Earth, Rampaging Mother Earth, Swallowing Ground) call the
 * FULL form, a stone giant that rises out of the ground ahead of its master, slams the earth with both fists and stands for {@link GnomeRules#FULL_TICKS} ticks (it hardens its master meanwhile); it
 * may come once per cooldown, one per caster and a few on the whole server. The earth pages themselves are unchanged (ids, numbers, cooldowns): the Gnome only changes WHERE the spell starts, and
 * what is seen and heard.
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
        long fullReadyAt, lastPacket = Long.MIN_VALUE / 4, lastMessage = Long.MIN_VALUE / 4, idleAt;
    }

    static final Map<UUID, State> STATES = new HashMap<>();
    /** The players that have a Gnome out: only these are visited by the once-a-few-ticks upkeep. */
    static final Set<UUID> LIVE = new HashSet<>();
    static final GnomeLedger SPIRITS = new GnomeLedger(), FULLS = new GnomeLedger();
    static final GnomeSoundRules.Gate GATE = new GnomeSoundRules.Gate();

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
        s.idleAt = sl.getGameTime() + 300;
        LIVE.add(p.getUUID());
        VfxSpawn.send(sl, VfxShape.EARTHFAM_BURST, new Vec3(a[0], a[1], a[2]), new Vec3(a[0], a[1] + 0.6, a[2]), EARTH_COLOR, 20, 0.5f);
        playCue(sl, p.getUUID(), "link", a[0], a[1], a[2]);
        say(p, s, "nusmp.gnome.linked", "The Gnome rises beside you.", ChatFormatting.GOLD);
        return true;
    }

    static void unlink(ServerPlayer p, State s, boolean speak) {
        ServerLevel sl = p.serverLevel();
        MagicPropEntity e = prop(sl, s.spirit);
        if (e != null) {
            playCue(sl, p.getUUID(), "unlink", e.getX(), e.getY(), e.getZ());
            e.discard();
        }
        if (s.spirit != null) SPIRITS.release(p.getUUID());
        s.spirit = null;
        if (speak) say(p, s, "nusmp.gnome.unlinked", "The Gnome sinks back into the earth.", ChatFormatting.GRAY);
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
        if (wantFull && summonFull(p, s, now, at)) return true;
        MagicPropEntity who = full != null ? full : comp;
        boolean punch = !GnomeRules.callsFull(page.costPercent());                                   // a page below the big tiers is a punch, a big one a slam of both fists
        who.setParam((who.param() & GnomeRules.HIDDEN) | (punch ? GnomeRules.PUNCH : 0) | GnomeRules.param(GnomeRules.variant(who.param()), GnomeRules.stampOf(now)));      // the clients play the clip for a second after this tick
        contact(p, sl, GnomeRules.contactTick(punch), mouth, full != null || wantFull);
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
    /** Every fourth tick for the players in {@link #LIVE}: lost props are dropped, the link ends when the book or the switch is gone, the master is hardened while the giant stands, an idle rumble now and then. */
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
        if (s.spirit != null && s.full == null && now >= s.idleAt) {
            MagicPropEntity e = prop(sl, s.spirit);
            s.idleAt = now + 400 + sl.getRandom().nextInt(400);
            if (e != null) playCue(sl, id, "idle", e.getX(), e.getY() + 0.5, e.getZ());
        }
        if (s.spirit == null && s.full == null) LIVE.remove(id);
    }

    /** Everything of this player goes: logout, death, respawn, dimension change. */
    public static void forget(ServerPlayer p) {
        UUID id = p.getUUID();
        State s = STATES.remove(id);
        LIVE.remove(id);
        SPIRITS.forget(id);
        FULLS.forget(id);
        GATE.forget(id);
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
