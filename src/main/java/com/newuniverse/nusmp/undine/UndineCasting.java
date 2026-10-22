package com.newuniverse.nusmp.undine;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.book.WaterBook;
import com.newuniverse.nusmp.firecast.FireCasting;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
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

/**
 * Why: the Undine as Noelle's casting companion (docs/magic/undine.md). A per-player "spirit link", the same attach / emitter approach as the Salamander of the fire casting: while the link is on, the
 * Undine floats beside the caster's right shoulder, and every spell of Noelle's Water grimoire leaves her open palm (the emitter point {@link UndineRules#companionPalm}). The big pages (cost tier 20
 * and up: the zone and signature pages) call her full form, a tall woman of water who rises from a ring of waves behind the caster for {@link UndineRules#FULL_TICKS} ticks and casts from her raised hands (the
 * emitter becomes {@link UndineRules#fullPalms}); it may come at most once per {@link UndineRules#FULL_COOLDOWN} ticks, one per caster and {@link UndineConfig#maxFull} on the whole server, linked or not.
 * The pages, their ids, their numbers and their cooldowns are the Water grimoire's own and unchanged; nothing is removed.
 *
 * <p>All state is per player UUID in {@link #STATES} (only Noelle's casters ever get an entry) and is dropped on logout, death, respawn, dimension change and server stop. Nothing here scans the world; the
 * link costs one position nudge every five ticks per companion, one upkeep every four ticks for a player who has anything live, and a water-breathing refresh every five seconds.</p>
 */
public final class UndineCasting {
    private UndineCasting() {}

    /** The state of one player's Undine. */
    static final class State {
        UUID spirit, full;
        long fullReadyAt, lastPacket = Long.MIN_VALUE / 4, lastMessage = Long.MIN_VALUE / 4, nextAmbient;
    }

    static final Map<UUID, State> STATES = new HashMap<>();
    /** The players that have anything live: only these are visited by the once-a-few-ticks upkeep. */
    static final Set<UUID> LIVE = new HashSet<>();
    static final UndineLedger SPIRITS = new UndineLedger(), FULLS = new UndineLedger();

    private static State state(UUID id) { return STATES.computeIfAbsent(id, k -> new State()); }

    static State stateOf(ServerPlayer p) { return state(p.getUUID()); }

    // ================================================================================================ who casts how
    /** Does the Undine apply to this caster and book: the switch is on, the book is the Water grimoire and its bound canon book is Noelle's. */
    public static boolean applies(GrimoireBook book, ServerPlayer p) {
        return book instanceof WaterBook && UndineConfig.enabled() && CharacterSpells.characterOf(p) == CanonBook.NOELLE;
    }

    /** A page of the Water book's own family (a spell), not the universal pages (Summon, Store, Mana Strike, ...). */
    private static boolean family(GrimoireBook book, int mode) { return mode >= 0 && mode < book.familyCount(); }

    private static void say(ServerPlayer p, State s, String key, String text, ChatFormatting color) {
        long now = p.level().getGameTime();
        if (!UndineRules.gapOk(now, s.lastMessage, UndineRules.MESSAGE_GAP)) return;
        s.lastMessage = now;
        p.displayClientMessage(Component.translatableWithFallback(key, text).withStyle(color), true);
    }

    // ================================================================================================ the cast
    /**
     * The cast itself (called instead of the generic call of {@link FireCasting#run}, which it falls through to): for Noelle's Water pages the spell leaves the Undine's palm (the full form's, for the big
     * pages); every other caster, every other book and every caster while {@code magic.undine} is off takes the generic path untouched.
     */
    public static boolean run(GrimoireBook book, BookPage page, ManasSkillInstance inst, ServerPlayer p, int mode) {
        if (!family(book, mode) || !applies(book, p)) return FireCasting.run(book, page, inst, p, mode);
        ServerLevel sl = p.serverLevel();
        State s = state(p.getUUID());
        long now = sl.getGameTime();
        Vec3 palm = null;
        boolean full = false;
        if (UndineRules.callsFull(page.costPercent()) && summonFull(p, s, now)) {
            double[] m = UndineRules.fullPalms(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
            palm = new Vec3(m[0], m[1], m[2]);
            full = true;
        } else if (s.spirit != null && prop(sl, s.spirit) != null) {
            double[] m = UndineRules.companionPalm(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
            palm = new Vec3(m[0], m[1], m[2]);
            MagicPropEntity sp = prop(sl, s.spirit);
            if (sp != null && !UndineRules.hidden(sp.param())) sp.setParam((sp.param() & UndineRules.HIDDEN) | UndineRules.param(UndineRules.V_COMPANION, UndineRules.stampOf(now)));      // the client plays the cast pose for a second after this tick
        }
        if (palm == null) return FireCasting.run(book, page, inst, p, mode);
        VfxSpawn.send(sl, VfxShape.WATER_SPLASH, palm, palm, 0xFF4FA8FF, 12, 0.5f);
        UndineAudio.play(sl, p.getUUID(), palm.x, palm.y, palm.z, UndineSoundSet.Cue.CAST, full);
        return withEmitter(p, palm, () -> FireCasting.run(book, page, inst, p, mode));
    }

    /** Calls the full form for this cast if it is allowed (cooldown, one per caster, the server cap). */
    static boolean summonFull(ServerPlayer p, State s, long now) {
        if (now < s.fullReadyAt || s.full != null) return false;
        if (!FULLS.acquire(p.getUUID(), UndineRules.FULL_PER_PLAYER, UndineConfig.maxFull())) return false;
        ServerLevel sl = p.serverLevel();
        double[] a = UndineRules.full(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.UNDINE_1, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, UndineRules.FULL_TICKS,
                UndineRules.param(UndineRules.V_FULL, UndineRules.stampOf(now)), p);
        if (e == null) { FULLS.release(p.getUUID()); return false; }
        e.setTarget(p);
        s.full = e.getUUID();
        s.fullReadyAt = now + UndineRules.FULL_COOLDOWN;
        LIVE.add(p.getUUID());
        VfxSpawn.send(sl, VfxShape.WATER_RING, new Vec3(a[0], a[1], a[2]), new Vec3(a[0], a[1] + 0.2, a[2]), 0xFF4FA8FF, 24, 1.6f);
        UndineAudio.play(sl, p.getUUID(), a[0], a[1] + 1.5, a[2], UndineSoundSet.Cue.FULL, true);
        UUID owner = p.getUUID();
        SpellRuntime.later(sl, 10, () -> UndineAudio.play(sl, owner, a[0], a[1] + 1.0, a[2], UndineSoundSet.Cue.CRASH, true));      // the wave crashes as she finishes rising
        return true;
    }

    // ================================================================================================ what the spells ask
    private static UUID emitterOwner;
    private static Vec3 emitterAt;

    /** Runs {@code body} (a spell's cast, synchronous) with the caster's start point moved to {@code at}; true when the body says so. */
    static boolean withEmitter(ServerPlayer p, Vec3 at, java.util.function.BooleanSupplier body) {
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
        double[] r = UndineRules.emit(start.x, start.y, start.z, eye.x, eye.y, eye.z, feet.x, feet.y, feet.z, emitterAt.x, emitterAt.y, emitterAt.z);
        return new Vec3(r[0], r[1], r[2]);
    }

    // ================================================================================================ the link (the key)
    /** Toggles the companion (the key). Validated: switch, canon book, rate. */
    public static void toggle(ServerPlayer p) {
        State s = state(p.getUUID());
        long now = p.level().getGameTime();
        if (!UndineRules.gapOk(now, s.lastPacket, UndineRules.PACKET_GAP)) return;
        s.lastPacket = now;
        if (!UndineConfig.enabled() || CharacterSpells.characterOf(p) != CanonBook.NOELLE) {
            STATES.remove(p.getUUID(), s);
            return;
        }
        if (s.spirit != null) { unlink(p, s, true); return; }
        link(p, s);
    }

    /** Links the companion (no canon check: the caller did it). */
    static boolean link(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        if (!SPIRITS.acquire(p.getUUID(), UndineRules.SPIRITS_PER_PLAYER, UndineConfig.maxSpirits())) {
            say(p, s, "nusmp.undine.spirit.full", "Undine cannot answer: too many spirits are out.", ChatFormatting.RED);
            return false;
        }
        double[] a = UndineRules.companion(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.UNDINE_1, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, 24000, UndineRules.param(UndineRules.V_COMPANION, 0), p);
        if (e == null) { SPIRITS.release(p.getUUID()); return false; }
        e.setTarget(p);
        s.spirit = e.getUUID();
        s.nextAmbient = sl.getGameTime() + 100;
        LIVE.add(p.getUUID());
        VfxSpawn.send(sl, VfxShape.WATER_RING, new Vec3(a[0], a[1], a[2]), new Vec3(a[0], a[1] + 0.3, a[2]), 0xFF4FA8FF, 16, 0.7f);
        UndineAudio.play(sl, p.getUUID(), a[0], a[1] + 1.0, a[2], UndineSoundSet.Cue.LINK, false);
        say(p, s, "nusmp.undine.spirit.linked", "Undine rises from the water beside you.", ChatFormatting.AQUA);
        return true;
    }

    static void unlink(ServerPlayer p, State s, boolean say) {
        ServerLevel sl = p.serverLevel();
        MagicPropEntity e = prop(sl, s.spirit);
        if (e != null) {
            UndineAudio.play(sl, p.getUUID(), e.getX(), e.getY() + 1.0, e.getZ(), UndineSoundSet.Cue.UNLINK, false);
            e.discard();
        }
        if (s.spirit != null) SPIRITS.release(p.getUUID());
        s.spirit = null;
        if (say) say(p, s, "nusmp.undine.spirit.unlinked", "Undine sinks back into the water.", ChatFormatting.GRAY);
    }

    // ================================================================================================ upkeep and cleanup
    /** Every few ticks for the players in {@link #LIVE}: drops what is no longer valid (a lost prop, a swapped grimoire), the ambient sound. */
    public static void upkeep(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null) { LIVE.remove(p.getUUID()); return; }
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (s.full != null && prop(sl, s.full) == null) { s.full = null; FULLS.release(p.getUUID()); }
        if (s.spirit != null) {
            if (prop(sl, s.spirit) == null) { s.spirit = null; SPIRITS.release(p.getUUID()); }
            else if (p.tickCount % 20 == 0 && (!UndineConfig.enabled() || CharacterSpells.characterOf(p) != CanonBook.NOELLE)) unlink(p, s, false);
            else if (now >= s.nextAmbient) {
                s.nextAmbient = now + 240 + (long) (sl.random.nextInt(240));
                MagicPropEntity e = prop(sl, s.spirit);
                if (e != null && !UndineRules.hidden(e.param())) UndineAudio.play(sl, p.getUUID(), e.getX(), e.getY() + 1.2, e.getZ(), UndineSoundSet.Cue.AMBIENT, false);
            }
        }
        if (s.spirit == null && s.full == null) LIVE.remove(p.getUUID());
    }

    /** Everything of this player goes: logout, death, respawn, dimension change. */
    public static void forget(ServerPlayer p) {
        State s = STATES.remove(p.getUUID());
        LIVE.remove(p.getUUID());
        SPIRITS.forget(p.getUUID());
        FULLS.forget(p.getUUID());
        UndineAudio.forget(p.getUUID());
        if (s == null || !(p.level() instanceof ServerLevel sl)) return;
        for (UUID id : new UUID[]{s.spirit, s.full}) if (id != null && sl.getEntity(id) instanceof MagicPropEntity e) e.discard();
    }

    /** The server stops: nothing may survive into the next world. */
    public static void clearAll() {
        STATES.clear();
        LIVE.clear();
        SPIRITS.clear();
        FULLS.clear();
        UndineAudio.clearAll();
        emitterOwner = null;
        emitterAt = null;
    }

    /** A prop ended on its own (its life ran out, or it was an orphan): if it is the one the player's state names, give its ledger slot back. */
    public static void propEnded(UUID owner, int variant, UUID entity) {
        State s = owner == null ? null : STATES.get(owner);
        if (s == null || entity == null) return;
        if (variant == UndineRules.V_FULL && entity.equals(s.full)) { s.full = null; FULLS.release(owner); }
        else if (variant == UndineRules.V_COMPANION && entity.equals(s.spirit)) { s.spirit = null; SPIRITS.release(owner); }
    }

    /** The prop entity with this id in this level, or null. */
    static MagicPropEntity prop(ServerLevel sl, UUID id) { return id != null && sl.getEntity(id) instanceof MagicPropEntity e && !e.isRemoved() ? e : null; }

    /** What the companion gives her caster while she is near: water breathing, as the Spirit Lord of Water does for her master. */
    static void aura(ServerPlayer p) { p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 220, 0, true, false)); }

    /** For the self-test: how many live things the ledgers count. */
    public static int liveCount() { return SPIRITS.total() + FULLS.total(); }
}
