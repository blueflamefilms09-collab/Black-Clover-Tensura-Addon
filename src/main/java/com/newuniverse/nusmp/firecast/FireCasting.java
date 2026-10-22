package com.newuniverse.nusmp.firecast;

import com.newuniverse.nusmp.anim.CastAnim;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.FireBook;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the casting styles of the Fire grimoire for four canon characters (docs/magic/fire_casting.md). The pages, their ids and their numbers are the existing ones; what is new is HOW
 * they are cast when the caster's bound canon grimoire is Magna's, Mereoleona's, Fuegoleon's or Leopold's (every other caster, and every caster while {@code magic.fireCasting} is off, takes
 * the old generic path untouched). The grimoire's cast path ({@link GrimoireBook}) calls the hooks below at five places: the hold ({@link #held}), the release ({@link #release}), the
 * gate before mana is spent ({@link #refuse}), the cast itself ({@link #run}) and the animation ({@link #releaseClip}); the spells ask for their start point ({@link #origin}), for a homing
 * target ({@link #homing}) and for the charge multipliers ({@link #damageMult}, {@link #sizeMult}).
 *
 * <p>The server decides everything; the client draws the props and plays the clips it is told. All state is per player UUID in {@link #STATES} (only players of these styles ever get an
 * entry) and is dropped on logout, death, dimension change and server stop. Nothing here scans the world on a tick: the one entity search (Magna's homing target) runs once, on the swing's
 * contact tick.</p>
 */
public final class FireCasting {
    private FireCasting() {}

    /** The state of one player's styles. */
    static final class State {
        long lastSwing = Long.MIN_VALUE / 4, lastMessage = Long.MIN_VALUE / 4, lastPacket = Long.MIN_VALUE / 4, dragonReadyAt;
        final PunchCombo combo = new PunchCombo();
        int armedMode = -1;
        long armedUntil;
        // Leopold: the charge being wound, the level the last release fixed, and the multipliers of a charged cast while they last
        boolean charging, broke;
        int chargeMode = -1, paidHeld = FireCastRules.CHARGE_FREE_TICKS, pendingLevel, castLevel;
        long lastHeldTick, pendingUntil, effectUntil;
        double effectDamage = 1.0, effectSize = 1.0;
        // the live props (entity ids) and whether each holds a slot of its ledger
        UUID ball, spiral, spirit, dragon;
    }

    /** The custom particle theme of the fire styles (looked up once, not per call). */
    private static final ThemeKey FLAME = ThemeKey.of("magic.FLAME");

    static final Map<UUID, State> STATES = new HashMap<>();
    /** The players that have anything live (a charge, a prop): only these are visited by the once-a-few-ticks upkeep. */
    static final Set<UUID> LIVE = new HashSet<>();
    static final CapLedger BALLS = new CapLedger(), SPIRALS = new CapLedger(), SPIRITS = new CapLedger(), DRAGONS = new CapLedger();

    private static State state(UUID id) { return STATES.computeIfAbsent(id, k -> new State()); }

    // ================================================================================================ who casts how
    /** The style of this caster for this book: NONE unless the switch is on, the book is the Fire grimoire and its bound canon book is one of the four. */
    public static FireStyle styleOf(GrimoireBook book, ServerPlayer p) {
        if (!(book instanceof FireBook) || !FireCastConfig.enabled()) return FireStyle.NONE;
        CanonBook c = CharacterSpells.characterOf(p);
        return c == null ? FireStyle.NONE : FireStyle.ofCanon(c.id());
    }

    /** A page of the Fire book's own family (a spell), not the universal pages (Summon, Store, Mana Strike, ...). */
    private static boolean family(GrimoireBook book, int mode) { return mode >= 0 && mode < book.familyCount(); }

    /** Style of the caster for a page the styles apply to (a family page of the Fire book), else NONE. */
    private static FireStyle styleFor(GrimoireBook book, ServerPlayer p, int mode) { return family(book, mode) ? styleOf(book, p) : FireStyle.NONE; }

    static boolean hasBat(ServerPlayer p) { return p.getMainHandItem().getItem() instanceof MagnasSoulBatItem; }

    private static void say(ServerPlayer p, State s, String key, String text, ChatFormatting color) {
        long now = p.level().getGameTime();
        if (!FireCastRules.gapOk(now, s.lastMessage, FireCastRules.MESSAGE_GAP)) return;
        s.lastMessage = now;
        p.displayClientMessage(Component.literal(text).withStyle(color), true);      // literal on purpose: the text carries the page name and numbers
    }

    // ================================================================================================ the five hooks of the cast path
    /** The key is being held (every tick). Leopold's spiral winds here; nothing else uses the hold. */
    public static void held(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int heldTicks, int mode) {
        if (!(book instanceof FireBook) || !family(book, mode) || styleOf(book, p) != FireStyle.LEOPOLD) return;
        BookPage page = book.page(mode);
        if (page == null || page.id().equals("flame_armour")) return;
        State s = state(p.getUUID());
        long now = p.level().getGameTime();
        if (!book.chargeReady(inst, p, mode)) { cancelCharge(p, s); return; }
        if (!s.charging || s.chargeMode != mode || heldTicks <= 1) {
            s.charging = true; s.broke = false; s.chargeMode = mode; s.paidHeld = FireCastRules.CHARGE_FREE_TICKS; s.pendingLevel = 0;
            LIVE.add(p.getUUID());
        }
        s.lastHeldTick = now;
        if (heldTicks <= FireCastRules.CHARGE_FREE_TICKS || s.broke) return;
        if (FireCastRules.chargeStepDue(heldTicks)) {
            double cost = book.getMagiculeCost(p, inst, mode) * FireCastRules.CHARGE_STEP_COST * FireCastConfig.chargeCostScale();
            if (draw(p, cost)) {
                s.paidHeld = heldTicks;
                int level = FireCastRules.chargeLevel(s.paidHeld);
                if (level >= 1) spiral(p, s, level);
            } else {
                s.broke = true;
                say(p, s, "nusmp.firecast.spent", "The spiral stops winding: no mana left.", ChatFormatting.RED);
            }
        }
        if (heldTicks == FireCastRules.CHARGE_FREE_TICKS + 1) CastAnim.play(p, CastAnim.FIRE_CHARGE);
        if (heldTicks % 4 == 0 && s.spiral != null) {                                          // a few custom flame particles round the spiral, through the budgets
            int level = FireCastRules.chargeLevel(s.paidHeld);
            Vec3 at = p.getEyePosition().add(p.getViewVector(1f).scale(1.5));
            Fx.send(p.serverLevel(), FLAME, at.x, at.y - 0.3, at.z, 2 + level, 0.35 + 0.08 * level);
        }
    }

    /**
     * The key was released. Returns true when the style took the release itself (Mereoleona readies the page instead of casting it); Leopold fixes the charge level the cast will use and
     * returns false so the grimoire casts as usual.
     */
    public static boolean release(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int heldTicks, int mode) {
        FireStyle st = styleFor(book, p, mode);
        if (st == FireStyle.NONE) return false;
        State s = state(p.getUUID());
        long now = p.level().getGameTime();
        if (st == FireStyle.MEREOLEONA) {
            BookPage page = book.page(mode);
            if (page == null || !book.chargeReady(inst, p, mode)) return false;                // a page that cannot be cast fizzles with the usual message
            s.armedMode = mode;
            s.armedUntil = now + FireCastRules.ARM_TICKS;
            int hits = FireCastRules.finisherHits(page.costPercent(), ManaLaw.zoneActive(p));
            say(p, s, "nusmp.firecast.ready", "Ready: " + page.name() + ". Punch " + hits + " times to release it.", ChatFormatting.GOLD);
            return true;
        }
        if (st == FireStyle.LEOPOLD) {
            s.pendingLevel = s.charging && s.chargeMode == mode ? FireCastRules.chargeLevel(s.paidHeld) : 0;
            s.pendingUntil = now + 40;
            s.charging = false;
            s.broke = false;
            s.paidHeld = FireCastRules.CHARGE_FREE_TICKS;
        }
        return false;
    }

    /** Is the key allowed to wind up the hold circle for this page (false: the style would refuse the cast anyway, so no circle is shown). */
    public static boolean ready(GrimoireBook book, ServerPlayer p, int mode) {
        FireStyle st = styleFor(book, p, mode);
        return st == FireStyle.NONE || (st == FireStyle.MAGNA && hasBat(p)) || st == FireStyle.FUEGOLEON || st == FireStyle.LEOPOLD;
    }

    /** Does the key start the chant clip for this page? (Mereoleona does not chant: she brawls.) */
    public static boolean chants(GrimoireBook book, ServerPlayer p, int mode) { return styleFor(book, p, mode) != FireStyle.MEREOLEONA; }

    /** Called by the cast path after the generic checks and before any mana is spent: a message to refuse with, or null to go on. */
    public static String refuse(GrimoireBook book, ServerPlayer p, int mode) {
        FireStyle st = styleFor(book, p, mode);
        if (st == FireStyle.MAGNA) {
            State s = state(p.getUUID());
            if (!hasBat(p)) return "Magna casts with his Soul Bat: hold Magna's Soul Bat in your main hand.";
            long now = p.level().getGameTime();
            if (s.ball != null || !FireCastRules.gapOk(now, s.lastSwing, FireCastRules.MIN_SWING_GAP)) return "The bat is still swinging.";
            if (BALLS.count(p.getUUID()) >= FireCastRules.BALLS_PER_PLAYER || BALLS.total() >= FireCastConfig.maxBalls()) return "Too many fireballs in the air.";
        } else if (st == FireStyle.MEREOLEONA && bypass == null) {
            return "Mereoleona fights with her fists: ready the page with the key, then punch.";
        }
        return null;
    }

    /** The release clip the cast path plays after a cast: the style's own (or none, when it already started one), else the page's usual clip. */
    public static String releaseClip(GrimoireBook book, ServerPlayer p, int mode, String usual) {
        FireStyle st = styleFor(book, p, mode);
        return switch (st) {
            case MAGNA -> null;                                                                // the swing clip began at the toss
            case MEREOLEONA -> CastAnim.FIRE_PUNCH_FINISHER;
            case LEOPOLD -> { State s = STATES.get(p.getUUID()); yield s != null && s.castLevel >= 1 ? CastAnim.FIRE_CHARGE_RELEASE : usual; }
            default -> usual;
        };
    }

    /** The cast itself (called instead of {@code page.cast().cast(...)}): the generic call for every caster without a style, the style's way for the four. */
    public static boolean run(GrimoireBook book, BookPage page, ManasSkillInstance inst, ServerPlayer p, int mode) {
        FireStyle st = styleFor(book, p, mode);
        switch (st) {
            case MAGNA: return MagnaSwing.begin(book, page, inst, p, mode);
            case MEREOLEONA: {
                double[] f = FireCastRules.fistPosition(p.getEyePosition().x, p.getEyePosition().y, p.getEyePosition().z, p.getViewVector(1f).x, p.getViewVector(1f).y, p.getViewVector(1f).z);
                return withEmitter(p, new Vec3(f[0], f[1], f[2]), null, () -> page.cast().cast(book, inst, p, mode));
            }
            case FUEGOLEON: return Salamander.cast(book, page, inst, p, mode);
            case LEOPOLD: {
                State s = state(p.getUUID());
                long now = p.level().getGameTime();
                int level = now <= s.pendingUntil ? s.pendingLevel : 0;
                s.pendingLevel = 0;
                s.castLevel = level;
                s.effectDamage = FireCastRules.chargeDamage(level);
                s.effectSize = FireCastRules.chargeSize(level);
                s.effectUntil = level >= 1 ? now + FireCastRules.CHARGE_EFFECT_TICKS : 0;
                endSpiral(p, s);
                return page.cast().cast(book, inst, p, mode);
            }
            default: return page.cast().cast(book, inst, p, mode);
        }
    }

    // ================================================================================================ what the spells ask
    private static volatile UUID emitterOwner, bypass;
    private static Vec3 emitterAt, homingTarget;
    private static LivingEntity homingEntity;

    /** Runs {@code body} (a spell's cast, synchronous) with the caster's start point moved to {@code at} and an optional homing target; true when the body says so. */
    static boolean withEmitter(ServerPlayer p, Vec3 at, LivingEntity homing, java.util.function.BooleanSupplier body) {
        UUID prevOwner = emitterOwner;
        Vec3 prevAt = emitterAt;
        LivingEntity prevHoming = homingEntity;
        emitterOwner = p.getUUID();
        emitterAt = at;
        homingEntity = homing;
        try {
            return body.getAsBoolean();
        } finally {
            emitterOwner = prevOwner;
            emitterAt = prevAt;
            homingEntity = prevHoming;
        }
    }

    /** A spell's start point: the emitter while one is set for this caster and the start was the caster's own, else the start unchanged. */
    public static Vec3 origin(LivingEntity caster, Vec3 start) {
        if (emitterOwner == null || emitterAt == null || !emitterOwner.equals(caster.getUUID())) return start;
        Vec3 eye = caster.getEyePosition(), feet = caster.position();
        double[] r = FireCastRules.emit(start.x, start.y, start.z, eye.x, eye.y, eye.z, feet.x, feet.y, feet.z, emitterAt.x, emitterAt.y, emitterAt.z);
        return new Vec3(r[0], r[1], r[2]);
    }

    /** The homing target of a bolt fired during a Magna launch (when the spell gave none): the one found on the contact tick. */
    public static LivingEntity homing(LivingEntity caster, LivingEntity given) {
        if (given != null || emitterOwner == null || homingEntity == null || !emitterOwner.equals(caster.getUUID())) return given;
        return homingEntity.isAlive() ? homingEntity : null;
    }

    /** Damage multiplier of a charged cast of this caster (1 unless a Leopold charge is in effect). */
    public static double damageMult(LivingEntity caster) {
        if (STATES.isEmpty()) return 1.0;
        State s = STATES.get(caster.getUUID());
        return s == null || caster.level().getGameTime() > s.effectUntil ? 1.0 : s.effectDamage;
    }

    /** Size multiplier of a charged cast of this caster (1 unless a Leopold charge is in effect). */
    public static double sizeMult(LivingEntity caster) {
        if (STATES.isEmpty()) return 1.0;
        State s = STATES.get(caster.getUUID());
        return s == null || caster.level().getGameTime() > s.effectUntil ? 1.0 : s.effectSize;
    }

    /** True while a style's own cast (the finisher) is running for this player, so the cast path does not refuse it. */
    static void withBypass(ServerPlayer p, Runnable body) {
        UUID prev = bypass;
        bypass = p.getUUID();
        try { body.run(); } finally { bypass = prev; }
    }

    // ================================================================================================ Leopold's spiral
    private static void spiral(ServerPlayer p, State s, int level) {
        ServerLevel sl = p.serverLevel();
        if (s.spiral != null && sl.getEntity(s.spiral) instanceof MagicPropEntity e && !e.isRemoved()) {
            e.setParam(FireCastRules.param(FireCastRules.V_SPIRAL, level));
            return;
        }
        if (!SPIRALS.acquire(p.getUUID(), 1, FireCastConfig.maxSpirals())) return;
        Vec3 at = p.getEyePosition().add(p.getViewVector(1f).scale(1.5));
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.FIRECAST_1, at.subtract(0, 0.5, 0), p.getYRot(), 1.0f, FireCastRules.MAX_HOLD + 40, FireCastRules.param(FireCastRules.V_SPIRAL, level), p);
        if (e == null) { SPIRALS.release(p.getUUID()); return; }
        e.setTarget(p);
        s.spiral = e.getUUID();
        LIVE.add(p.getUUID());
    }

    private static void endSpiral(ServerPlayer p, State s) {
        if (s.spiral == null) return;
        if (p.serverLevel().getEntity(s.spiral) instanceof MagicPropEntity e && !e.isRemoved()) e.discard();
        s.spiral = null;
        SPIRALS.release(p.getUUID());
    }

    private static void cancelCharge(ServerPlayer p, State s) {
        s.charging = false;
        s.broke = false;
        s.pendingLevel = 0;
        s.paidHeld = FireCastRules.CHARGE_FREE_TICKS;
        endSpiral(p, s);
    }

    /** Mana drawn straight from the caster's magicule (the amount already carries the mana law's cost multiplier). False when there is not enough. */
    private static boolean draw(ServerPlayer p, double amount) {
        if (p.isCreative() || amount <= 0) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || ex.getMagicule() < amount) return false;
        ex.setMagicule(ex.getMagicule() - amount);
        ex.markDirty();
        return true;
    }

    // ================================================================================================ Mereoleona's fists
    /** A punch landed (the damage event of the player's own unarmed melee attack). */
    public static void onPunch(ServerPlayer p, LivingEntity target) {
        if (!FireCastConfig.enabled() || target == p || !p.getMainHandItem().isEmpty()) return;
        Optional<ManasSkillInstance> opt = SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_FIRE.getId());
        if (opt.isEmpty() || !(opt.get().getSkill() instanceof FireBook book) || styleOf(book, p) != FireStyle.MEREOLEONA) return;
        State s = state(p.getUUID());
        long now = p.level().getGameTime();
        boolean zone = ManaLaw.zoneActive(p);
        boolean armed = s.armedMode >= 0 && now <= s.armedUntil && book.page(s.armedMode) != null && s.armedMode < book.familyCount();
        BookPage page = armed ? book.page(s.armedMode) : null;
        PunchCombo.Result r = s.combo.land(now, zone, page == null ? 0 : page.costPercent(), armed);
        int n = s.combo.count(now, zone);
        ServerLevel sl = p.serverLevel();
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
        if (r == PunchCombo.Result.FINISHER) {
            int mode = s.armedMode;
            s.armedMode = -1;
            ManasSkillInstance inst = opt.get();
            withBypass(p, () -> book.castFromStyle(inst, p, mode));
            return;
        }
        CastAnim.play(p, n % 2 == 1 ? CastAnim.FIRE_PUNCH_A : CastAnim.FIRE_PUNCH_B);
        Fx.send(sl, FLAME, at.x, at.y, at.z, armed ? 4 + Math.min(4, n) : 2, 0.25);
        if (armed && n == 1) sl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    // ================================================================================================ the Salamander (Fuegoleon)
    /** Toggles the small Salamander (the key, or the sneak command of the spirit). Validated: style, rate. */
    public static void toggleSpirit(ServerPlayer p) {
        State s = state(p.getUUID());
        long now = p.level().getGameTime();
        if (!FireCastRules.gapOk(now, s.lastPacket, FireCastRules.PACKET_GAP)) return;
        s.lastPacket = now;
        if (!FireCastConfig.enabled() || CharacterSpells.characterOf(p) != CanonBook.FUEGOLEON) {
            STATES.remove(p.getUUID(), s);
            return;
        }
        if (s.spirit != null) { Salamander.unlink(p, s, true); return; }
        Salamander.link(p, s);
    }

    // ================================================================================================ upkeep and cleanup
    /** Every few ticks for the players in {@link #LIVE}: drops what is no longer valid (a lost key release, a swap, a dead or gone prop). */
    public static void upkeep(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null) { LIVE.remove(p.getUUID()); return; }
        long now = p.level().getGameTime();
        if (s.charging && now - s.lastHeldTick > 3) cancelCharge(p, s);
        if (s.spiral != null && !(p.serverLevel().getEntity(s.spiral) instanceof MagicPropEntity e && !e.isRemoved())) { s.spiral = null; SPIRALS.release(p.getUUID()); }
        if (s.ball != null && !(p.serverLevel().getEntity(s.ball) instanceof MagicPropEntity e2 && !e2.isRemoved())) { s.ball = null; BALLS.release(p.getUUID()); }
        Salamander.upkeep(p, s);
        if (!s.charging && s.spiral == null && s.ball == null && s.spirit == null && s.dragon == null) LIVE.remove(p.getUUID());
    }

    /** Everything of this player goes: logout, death, dimension change. */
    public static void forget(ServerPlayer p) {
        State s = STATES.remove(p.getUUID());
        LIVE.remove(p.getUUID());
        BALLS.forget(p.getUUID());
        SPIRALS.forget(p.getUUID());
        SPIRITS.forget(p.getUUID());
        DRAGONS.forget(p.getUUID());
        if (s == null || !(p.level() instanceof ServerLevel sl)) return;
        for (UUID id : new UUID[]{s.ball, s.spiral, s.spirit, s.dragon}) if (id != null && sl.getEntity(id) instanceof MagicPropEntity e) e.discard();
    }

    /** The server stops: nothing may survive into the next world. */
    public static void clearAll() {
        STATES.clear();
        LIVE.clear();
        BALLS.clear();
        SPIRALS.clear();
        SPIRITS.clear();
        DRAGONS.clear();
        emitterOwner = null;
        emitterAt = null;
        bypass = null;
        homingEntity = null;
    }

    // ================================================================================================ shared with Magna and the Salamander (same package)
    static State stateOf(ServerPlayer p) { return state(p.getUUID()); }

    static void markLive(ServerPlayer p) { LIVE.add(p.getUUID()); }

    /** A prop ended on its own (its life ran out, or it was an orphan): if it is the one the player's state names, give its ledger slot back. */
    public static void propEnded(UUID owner, int variant, UUID entity) {
        State s = owner == null ? null : STATES.get(owner);
        if (s == null || entity == null) return;
        if (variant == FireCastRules.V_BALL && entity.equals(s.ball)) { s.ball = null; BALLS.release(owner); }
        else if (variant == FireCastRules.V_SPIRAL && entity.equals(s.spiral)) { s.spiral = null; SPIRALS.release(owner); }
        else if (variant == FireCastRules.V_DRAGON && entity.equals(s.dragon)) { s.dragon = null; DRAGONS.release(owner); }
        else if (variant == FireCastRules.V_SALAMANDER && entity.equals(s.spirit)) { s.spirit = null; SPIRITS.release(owner); }
    }

    /** The prop entity with this id in this level, or null. */
    static MagicPropEntity prop(ServerLevel sl, UUID id) { return id != null && sl.getEntity(id) instanceof MagicPropEntity e && !e.isRemoved() ? e : null; }

    static void aura(ServerPlayer p) { p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 220, 0, true, false)); }

    /** For the self-test: how many live things the ledgers count. */
    public static int liveCount() { return BALLS.total() + SPIRALS.total() + SPIRITS.total() + DRAGONS.total(); }

    static void flare(ServerPlayer p, GrimoireBook book, Vec3 at, float power) { book.vfx(p, VfxShape.FLAMEFX_BURST, at, at, 14, power); }

    static boolean isMob(LivingEntity e) { return e instanceof Mob; }

    static void later(ServerLevel sl, int delay, Runnable r) { SpellRuntime.later(sl, delay, r); }

    static boolean safe(GrimoireBook book, ServerPlayer p, java.util.function.BooleanSupplier body) {
        return SafeCast.run(book.magicId(), p.level().getGameTime(), p, body);
    }
}
