package com.newuniverse.nusmp.sylph;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.WindBook;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Why: Yuno's wind Spirit Lord, Sylph, as a small winged girl (docs/magic/sylph.md). A per-player "spirit link": while it is on, a small Sylph floats beside the caster's head and every page of
 * the Wind grimoire leaves her hand (the emitter point {@link SylphRules#smallHand}); the big pages (cost tier 20 and up: the zone and signature pages, and the Spirit of Zephyr) call her POWERED
 * form, a bigger Sylph with long streaming hair and wide wings that hangs above and behind the caster for {@link SylphRules#POWERED_TICKS} ticks and casts from her outstretched hands (the emitter
 * becomes {@link SylphRules#poweredHand}); it may come at most once per {@link SylphRules#POWERED_COOLDOWN} ticks, one per caster and a few on the whole server, linked or not. All of it is for the
 * canon book of Yuno only (the same lock as the Spirit of Zephyr page) and behind the server config {@code magic.sylph}; for every other caster, and with the switch off, the wind pages are
 * cast the old way, untouched.
 *
 * <p>The server decides everything and the clients draw what they are told: the props follow their owner and are placed by the clients from the owner's own interpolated body each frame, so the
 * link costs one position nudge every five ticks per Sylph and a small status check every four ticks. All state is per player UUID in {@link #STATES} and is dropped on logout, death, respawn,
 * dimension change and server stop. Her sounds go through {@link SylphSounds} (nearby players only, per listener budget).</p>
 */
public final class SylphCasting {
    private SylphCasting() {}

    /** The state of one player's Sylph. */
    static final class State {
        UUID spirit, powered;
        /** Her moods (pure rules, docs/magic/sylph.md): the pose she holds, the idle clock, the cooldowns of what she says. */
        final SylphMood.State mood = new SylphMood.State();
        /** Game time until which she stays after her owner died (a cry, then she goes); 0 = not mourning. */
        long mourningUntil;
        long poweredReadyAt, lastPacket = Long.MIN_VALUE / 4, nextChime, lastGiggle = Long.MIN_VALUE / 4, lastFlutter = Long.MIN_VALUE / 4;
    }

    static final Map<UUID, State> STATES = new HashMap<>();
    /** The players that have anything live: only these are visited by the once-a-few-ticks upkeep. */
    static final Set<UUID> LIVE = new HashSet<>();
    static final SylphLedger SPIRITS = new SylphLedger(), POWERED = new SylphLedger();

    private static State state(UUID id) { return STATES.computeIfAbsent(id, k -> new State()); }

    /** Is this player one the Sylph applies to: the switch is on and the grimoire bound to them is Yuno's? */
    static boolean applies(ServerPlayer p) { return SylphConfig.enabled() && CharacterSpells.characterOf(p) == CanonBook.YUNO; }

    // ================================================================================================ the link
    /** Toggles the small Sylph (the key). Validated: the switch, Yuno's grimoire, a rate limit. */
    public static void toggle(ServerPlayer p) {
        State s = state(p.getUUID());
        long now = p.level().getGameTime();
        if (!SylphRules.gapOk(now, s.lastPacket, SylphRules.PACKET_GAP)) return;
        s.lastPacket = now;
        if (!applies(p)) {
            if (s.spirit == null && s.powered == null) STATES.remove(p.getUUID(), s);
            return;
        }
        if (s.spirit != null) { unlink(p, s, true); return; }
        link(p, s);
    }

    static boolean link(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        if (!SPIRITS.acquire(p.getUUID(), SylphRules.SPIRITS_PER_PLAYER, SylphConfig.maxSpirits())) {
            p.displayClientMessage(Component.translatableWithFallback("nusmp.sylph.full", "Sylph cannot answer: too many spirits are out.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        double[] a = SylphRules.small(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.SYLPH_1, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, 24000, SylphRules.param(SylphRules.V_SMALL, 0), p);
        if (e == null) { SPIRITS.release(p.getUUID()); return false; }
        e.setSize(SylphRules.SMALL_W, SylphRules.SMALL_H);
        e.setTarget(p);
        s.spirit = e.getUUID();
        s.nextChime = sl.getGameTime() + 100;
        s.mood.lastX = p.getX();
        s.mood.lastZ = p.getZ();
        LIVE.add(p.getUUID());
        VfxSpawn.send(sl, VfxShape.WIND_GALE, new Vec3(a[0], a[1] - 0.2, a[2]), new Vec3(a[0], a[1] + 0.6, a[2]), SYLPH_COLOR, 12, 0.4f);
        SylphSounds.play(sl, a[0], a[1], a[2], "link");
        SylphMoods.fire(p, s, SylphMood.Event.LINK);
        p.displayClientMessage(Component.translatableWithFallback("nusmp.sylph.linked", "Sylph floats to your side.").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    static void unlink(ServerPlayer p, State s, boolean say) {
        ServerLevel sl = p.serverLevel();
        MagicPropEntity e = prop(sl, s.spirit);
        if (e != null) {
            SylphMoods.fire(p, s, SylphMood.Event.UNLINK);
            SylphSounds.play(sl, e.getX(), e.getY(), e.getZ(), "unlink");
            e.discard();
        }
        if (s.spirit != null) SPIRITS.release(p.getUUID());
        s.spirit = null;
        if (say) p.displayClientMessage(Component.translatableWithFallback("nusmp.sylph.unlinked", "Sylph drifts away on the wind.").withStyle(ChatFormatting.GRAY), true);
    }

    /** The colour of Sylph's effects (the wind spirit's pale green). */
    static final int SYLPH_COLOR = 0xFF8CFFC2;

    // ================================================================================================ the cast
    /**
     * The cast of one of Yuno's Wind pages (called by the grimoire's cast path in place of the plain cast): the spell leaves Sylph's hand (the powered form's, for the big pages). For every
     * caster the Sylph does not apply to, and for every other book, the plain {@code inner} cast runs untouched.
     */
    public static boolean run(GrimoireBook book, BookPage page, ServerPlayer p, int mode, BooleanSupplier inner) {
        if (!(book instanceof WindBook) || mode < 0 || mode >= book.familyCount() || page == null || !applies(p)) return inner.getAsBoolean();
        ServerLevel sl = p.serverLevel();
        State s = STATES.get(p.getUUID());
        long now = sl.getGameTime();
        double[] hand = null;
        boolean big = false, bigPage = SylphRules.callsPowered(page.costPercent());
        if (bigPage) {
            s = s == null ? state(p.getUUID()) : s;
            if (summonPowered(p, s, now, "spirit_of_zephyr".equals(page.id()))) {
                hand = SylphRules.poweredHand(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
                big = true;
            }
        }
        if (hand == null && s != null && prop(sl, s.spirit) != null) {
            hand = SylphRules.smallHand(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
            stamp(sl, s.spirit, now);
        }
        if (hand == null) return inner.getAsBoolean();
        if (s != null && !big) SylphMoods.fire(p, s, bigPage ? SylphMood.Event.CAST_BIG : SylphMood.Event.CAST);                 // (the powered form's arrival has its own shout)
        Vec3 at = new Vec3(hand[0], hand[1], hand[2]);
        Vec3 look = p.getViewVector(1f);
        VfxSpawn.send(sl, VfxShape.WIND_GALE, at, at.add(look.scale(big ? 6.0 : 3.5)), SYLPH_COLOR, big ? 14 : 10, big ? 0.8f : 0.45f);
        SylphSounds.play(sl, at.x, at.y, at.z, big ? "gale" : "gust");
        return withEmitter(p, at, inner);
    }

    /** Stamps the cast tick on a small Sylph so the clients play her cast clip for a moment. */
    private static void stamp(ServerLevel sl, UUID id, long now) {
        MagicPropEntity sp = prop(sl, id);
        if (sp != null) sp.setParam((sp.param() & SylphRules.KEEP_ON_STAMP) | SylphRules.param(SylphRules.V_SMALL, SylphRules.stamp(now)));
    }

    /** Calls the powered form for this cast if it is allowed (cooldown, one per caster, the server cap). */
    private static boolean summonPowered(ServerPlayer p, State s, long now, boolean zephyr) {
        if (now < s.poweredReadyAt || s.powered != null) return false;
        if (!POWERED.acquire(p.getUUID(), SylphRules.POWERED_PER_PLAYER, SylphConfig.maxPowered())) return false;
        ServerLevel sl = p.serverLevel();
        double[] a = SylphRules.powered(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.SYLPH_1, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, SylphRules.poweredTicks(zephyr),
                SylphRules.param(SylphRules.V_POWERED, SylphRules.stamp(now)), p);
        if (e == null) { POWERED.release(p.getUUID()); return false; }
        e.setSize(SylphRules.POWERED_W, SylphRules.POWERED_H);
        e.setTarget(p);
        s.powered = e.getUUID();
        s.poweredReadyAt = now + SylphRules.POWERED_COOLDOWN;
        LIVE.add(p.getUUID());
        SylphSounds.play(sl, a[0], a[1], a[2], "surge");
        SylphMoods.fire(p, s, SylphMood.Event.POWERED_ARRIVE);
        return true;
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

    /** A spell's start point: the emitter while one is set for this caster and the start was the caster's own, else the start unchanged. */
    public static Vec3 origin(LivingEntity caster, Vec3 start) {
        if (emitterOwner == null || emitterAt == null || !emitterOwner.equals(caster.getUUID())) return start;
        Vec3 eye = caster.getEyePosition(), feet = caster.position();
        double[] r = SylphRules.emit(start.x, start.y, start.z, eye.x, eye.y, eye.z, feet.x, feet.y, feet.z, emitterAt.x, emitterAt.y, emitterAt.z);
        return new Vec3(r[0], r[1], r[2]);
    }

    // ================================================================================================ upkeep and cleanup
    /** Every few ticks for the players in {@link #LIVE}: drops what is no longer valid, and gives her her small sounds (chimes, flutter, giggle). */
    public static void upkeep(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null) { LIVE.remove(p.getUUID()); return; }
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (s.powered != null && prop(sl, s.powered) == null) { s.powered = null; POWERED.release(p.getUUID()); }
        if (s.spirit != null) {
            MagicPropEntity sp = prop(sl, s.spirit);
            if (sp == null) { s.spirit = null; SPIRITS.release(p.getUUID()); }
            else if (!applies(p)) unlink(p, s, false);
            else {
                voice(p, s, sp, sl, now);
                SylphMoods.idleTick(p, s, sl, now, sp);
            }
        }
        if (s.spirit == null && s.powered == null) LIVE.remove(p.getUUID());
    }

    /** The small sounds of a linked small Sylph: a chime now and then, the flutter while the caster moves fast, a giggle at the start of each laugh of her clip. */
    private static void voice(ServerPlayer p, State s, MagicPropEntity sp, ServerLevel sl, long now) {
        if (SylphRules.hidden(sp.param())) return;
        double[] a = SylphRules.small(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        if (now >= s.nextChime) {
            s.nextChime = now + 200 + sl.getRandom().nextInt(200);
            SylphSounds.play(sl, a[0], a[1], a[2], "chime");
        }
        if (p.getDeltaMovement().horizontalDistanceSqr() > 0.03 && now - s.lastFlutter >= 50) {
            s.lastFlutter = now;
            SylphSounds.play(sl, a[0], a[1], a[2], "flutter");
        }
        long t = Math.floorMod(now + (sp.seed() & 0x3FF), 600L);
        if (t < 4 && now - s.lastGiggle > 300) {                                                 // the laugh of her idle clip begins on this tick of the cycle (SylphRules.smallClip)
            s.lastGiggle = now;
            SylphSounds.play(sl, a[0], a[1], a[2], "giggle");
        }
    }

    /** How long she stays, crying, after her owner died (ticks), before she goes. */
    static final int MOURN_TICKS = 70;

    /**
     * The owner died: if a small Sylph is out she cries for a moment (the cry pose and her sad line) and then goes; everything else of theirs goes at once. The prop's own tick ends the vigil
     * ({@link SylphProps}); a respawn, a logout or a dimension change before that cleans up as always.
     */
    static void mourn(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        ServerLevel sl = p.serverLevel();
        if (s == null || prop(sl, s.spirit) == null || !SylphConfig.poses()) { forget(p); return; }
        MagicPropEntity pw = prop(sl, s.powered);
        if (pw != null) pw.discard();
        s.powered = null;
        POWERED.release(p.getUUID());
        s.mourningUntil = sl.getGameTime() + MOURN_TICKS;
        SylphMoods.fire(p, s, SylphMood.Event.DEATH);
    }

    /** Everything of this player goes: logout, death, respawn, dimension change. */
    public static void forget(ServerPlayer p) {
        State s = STATES.remove(p.getUUID());
        LIVE.remove(p.getUUID());
        SPIRITS.forget(p.getUUID());
        POWERED.forget(p.getUUID());
        SylphSounds.forget(p.getUUID());
        SylphVoice.forget(p.getUUID());
        if (s == null || !(p.level() instanceof ServerLevel sl)) return;
        for (UUID id : new UUID[]{s.spirit, s.powered}) if (id != null && sl.getEntity(id) instanceof MagicPropEntity e) e.discard();
    }

    /** The server stops: nothing may survive into the next world. */
    public static void clearAll() {
        STATES.clear();
        LIVE.clear();
        SPIRITS.clear();
        POWERED.clear();
        SylphSounds.clearAll();
        SylphVoice.clearAll();
        emitterOwner = null;
        emitterAt = null;
    }

    // ================================================================================================ shared with the props (same package)
    static State stateOf(ServerPlayer p) { return state(p.getUUID()); }

    /** A prop ended on its own (its life ran out, or it was an orphan): if it is the one the player's state names, give its ledger slot back. */
    public static void propEnded(UUID owner, int variant, UUID entity) {
        State s = owner == null ? null : STATES.get(owner);
        if (s == null || entity == null) return;
        if (variant == SylphRules.V_POWERED && entity.equals(s.powered)) { s.powered = null; POWERED.release(owner); }
        else if (variant == SylphRules.V_SMALL && entity.equals(s.spirit)) { s.spirit = null; SPIRITS.release(owner); }
    }

    /** The prop entity with this id in this level, or null. */
    static MagicPropEntity prop(ServerLevel sl, UUID id) { return id != null && sl.getEntity(id) instanceof MagicPropEntity e && !e.isRemoved() ? e : null; }

    /** What she gives the caster while she is near: she makes them light on their feet (Slow Falling), as the exclusive Sylph does. */
    static void aura(ServerPlayer p) { p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, true, false)); }

    /** For the self-test: how many live Sylphs the ledgers count. */
    public static int liveCount() { return SPIRITS.total() + POWERED.total(); }
}
