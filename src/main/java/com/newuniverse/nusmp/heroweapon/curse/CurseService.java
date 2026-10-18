package com.newuniverse.nusmp.heroweapon.curse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroSummon;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.labyrinth.mega.safe.RestState;
import com.newuniverse.nusmp.labyrinth.mega.safe.SafeRegistry;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Why: the Curse Series is a pure machine ({@link CurseMachine}) that knows nothing of the world, and a set of effects ({@link CurseEffects}) that know the world
 * but not the rules; this class joins them. Each public-in-package method is one thing that can happen to a wielder (a despair report, a technique, a death, the
 * once-a-second look, an Embrace, a decline, a charm) and does the same four steps: read the saved state from the bond, ask the machine what changes, apply the
 * answer to the world (a message, a sound, an effect, a modifier, the form), and write the state back BEFORE any call into another system that reads it (the forms
 * system asks {@link HeroCurse#locked} when a form is changed, so the Overrun is saved as over before the form is switched). The clock is passed in, so the
 * self-tests drive the real code in a loop instead of waiting; the numbers are passed in, so a test uses the defaults whatever the config says. Only bonded players
 * who are in survival or adventure are touched by the automatic parts; an administrator's command can act on anyone bonded. Server thread only; nothing here throws
 * out of an event handler (the callers catch and log).
 */
final class CurseService {
    private CurseService() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The quest hook keys (see the documentation): the weapon whispers, a series is embraced, an Overrun begins. */
    static final String HOOK_OFFER = "hero.curse_offer", HOOK_CURSE = "hero.curse", HOOK_OVERRUN = "hero.overrun";

    /** Corruption at which the wielder is warned once that the weapon strains. */
    private static final int WARN_AT = 800;

    static long now(Player p) { return p.level().getGameTime(); }

    /** True for players the automatic parts of the curse leave alone. */
    static boolean exempt(Player p) { return p.isCreative() || p.isSpectator(); }

    private static void hook(ServerPlayer p, String key, String arg) {
        try {
            NusmpEvents.fire(p, key, arg);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The quest hook {} failed and was ignored", key, e);
        }
    }

    private static void sound(ServerPlayer p, SoundEvent sound, float volume, float pitch) {
        p.level().playSound(null, p.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** The curse form the wielder wears now: the active form when it is a curse form AND the weapon is out; null otherwise (a stowed weapon's curse is dormant). */
    static HeroForm wornForm(HeroBond b) {
        HeroForm f = HeroForms.active(b);
        return f != null && f.curse() && b.summoned() ? f : null;
    }

    /** The sin of {@link #wornForm}, or null. */
    static Sin wornSin(HeroBond b) {
        HeroForm f = wornForm(b);
        return f == null ? null : f.sin();
    }

    /** The sin of the ACTIVE form when it is a curse form, whether or not the weapon is out: what the client is shown and what {@link HeroCurse#activeSin} answers. */
    static Sin shownSin(HeroBond b) {
        HeroForm f = HeroForms.active(b);
        return f != null && f.curse() ? f.sin() : null;
    }

    /** True when the ACTIVE form is a curse form, whether or not the weapon is out (a sealed one must not stay active even while stowed). */
    static boolean activeFormIsCurse(HeroBond b) {
        HeroForm f = HeroForms.active(b);
        return f != null && f.curse();
    }

    // ================================================================== despair
    /** Adds despair to a bonded player; returns the amount added (0 when unbound, paused, or at the top). */
    static int addDespair(ServerPlayer p, Cause cause, int amount, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) return 0;
        CurseState s = CurseStore.load(b);
        int added = CurseMachine.addDespair(s, cause, amount, now, t);
        CurseStore.save(b, s);
        return added;
    }

    private static boolean nearDeath(ServerPlayer p, CurseTuning t) {
        float max = p.getMaxHealth();
        return max > 0f && p.getHealth() > 0f && p.getHealth() < max * t.nearDeathShare;
    }

    // ================================================================== the once-a-second look (and the half-second one)
    /**
     * One look at a player. Called every ten ticks; {@code second} is true on every second call. Every ten ticks: an Overrun goes on (a forced swing) or ends, Lust
     * pulls. Once a second: despair fades, a near-death is counted, corruption rises or falls, a finished rest cleanses, the weapon may whisper, the sin's downside is
     * applied, the tax and the weakness are kept in force, and the client is told if anything changed.
     */
    static void tick(ServerPlayer p, long now, boolean second, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound() || p.isSpectator()) return;
        boolean has = b.hasExtra(CurseStore.KEY);
        if (p.isCreative()) {
            if (has) endForCreative(p, b, now, t);
            return;
        }
        if (!has) {
            if (second && nearDeath(p, t)) addDespair(p, Cause.NEAR_DEATH, t.amount(Cause.NEAR_DEATH), now, t);
            return;
        }
        CurseState s = CurseStore.load(b);
        HeroKind kind = b.kind();

        if (CurseMachine.lapseIfExpired(s, now, t)) {
            CurseStore.save(b, s);
            CurseMessages.bar(p, CurseMessages.tr(CurseText.LAPSE).withStyle(ChatFormatting.GRAY));
            if (second) CurseSync.push(p, s, shownSin(b), now, true, t);
        }
        if (CurseMachine.overrunDue(s, now)) overrunEnd(p, b, s, now, t);
        else if (CurseMachine.inOverrun(s, now)) overrunStep(p, t);

        HeroForm form = HeroForms.active(b);
        boolean curseForm = form != null && form.curse();
        boolean wearing = curseForm && b.summoned();
        Sin sin = wearing ? form.sin() : null;
        boolean overrun = CurseMachine.inOverrun(s, now);

        if (!second) {
            if (wearing && sin == Sin.LUST && !overrun) CurseEffects.lust(p, t);
            return;
        }

        CurseMachine.decayDespair(s, now, p.getLastHurtByMob() == null, t);
        if (nearDeath(p, t)) CurseMachine.addDespair(s, Cause.NEAR_DEATH, t.amount(Cause.NEAR_DEATH), now, t);

        if (curseForm && (form.sin() == null || !s.hasSeries(form.sin()))) {
            CurseStore.save(b, s);
            forceBase(p, b);
            CurseMessages.bar(p, CurseMessages.tr(CurseText.FORCED_BASE).withStyle(ChatFormatting.GRAY));
            form = HeroForms.active(b);
            curseForm = false;
            wearing = false;
            sin = null;
        }

        int before = s.corruption;
        CurseMachine.Step step = CurseMachine.tickCorruption(s, wearing, wearing ? form.corruptionPerSecond() : 0, now, t, p.getRandom().nextDouble());
        if (step == CurseMachine.Step.OVERRUN_STARTED) {
            CurseStore.save(b, s);
            overrunStart(p, b, s, sin, now, t);
            overrun = true;
        } else if (wearing && before < WARN_AT && s.corruption >= WARN_AT) {
            CurseMessages.bar(p, CurseMessages.colored(sin, CurseMessages.tr(CurseText.OVERRUN_WARN, s.corruption)));
            sound(p, SoundEvents.WARDEN_HEARTBEAT, 0.8f, 1.0f);
        }

        if (s.corruption > 0 && p.hasData(SafeRegistry.REST_STATE)) {
            RestState rest = p.getData(SafeRegistry.REST_STATE);
            int cleared = CurseMachine.restClear(s, rest.lastRest, now, t);
            if (cleared > 0) {
                CurseMessages.bar(p, CurseMessages.tr(CurseText.REST, cleared).withStyle(ChatFormatting.AQUA));
                sound(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f, 1.2f);
            }
        }

        if (s.offered.isEmpty() && s.despair >= t.offerThreshold) {
            Sin offered = CurseMachine.tryOffer(s, CurseCandidates.of(kind, HeroForms.of(kind)), now, t);
            if (offered != null) {
                CurseStore.save(b, s);
                announceOffer(p, b, s, offered, now, t);
            }
        }

        if (wearing && sin != null && !overrun) CurseEffects.second(p, form, sin, t, now);
        CurseEffects.ensureSinModifiers(p, wearing ? sin : null, t);
        CurseEffects.ensureTax(p, s.tax, t.minHearts, false);
        if (CurseMachine.weak(s, now)) keepWeak(p, (int) Math.min(Integer.MAX_VALUE, s.weakUntil - now));
        CurseMachine.pruneCauses(s, now, t);

        CurseStore.save(b, s);
        CurseSync.push(p, s, curseForm ? form.sin() : null, now, false, t);
    }

    /** A creative player cannot be held by the weapon: an Overrun that began in survival ends at once, quietly. */
    private static void endForCreative(ServerPlayer p, HeroBond b, long now, CurseTuning t) {
        CurseState s = CurseStore.load(b);
        if (s.overrunUntil <= 0) return;
        CurseMachine.abortOverrun(s, t);
        CurseStore.save(b, s);
        CurseSync.push(p, s, shownSin(b), now, true, t);
    }

    private static void keepWeak(ServerPlayer p, int left) {
        if (left <= 0) return;
        MobEffectInstance w = p.getEffect(MobEffects.WEAKNESS);
        if (w == null || w.getAmplifier() < 1 || w.getDuration() < left - 40) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, left, 1, false, true));
        MobEffectInstance sl = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (sl == null || sl.getDuration() < left - 40) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, left, 0, false, true));
    }

    /**
     * The forms system's own door for the curse, {@code HeroForms.forceActive(ServerPlayer, String)}: a change of form without the curse lock and without the lock after
     * a technique, which also re-applies the loadout, refreshes an open Weapon Book and tells the weapon's logic. It is looked up once by name, so this class builds against
     * the forms system's contract and finds the door when the real forms system is there; null when it is not.
     */
    private static final Method FORCE_ACTIVE = lookupForceActive();

    private static Method lookupForceActive() {
        try {
            return HeroForms.class.getMethod("forceActive", ServerPlayer.class, String.class);
        } catch (NoSuchMethodException | SecurityException absent) {
            return null;
        }
    }

    /**
     * Puts the weapon back to its base form. The forms system's forced change does it when there is one (see {@link #FORCE_ACTIVE}); otherwise, and whenever that
     * refuses or fails, the form is set on the bond directly (the plain change of form can refuse, for instance within two seconds of a technique, and would put an
     * error line on the screen at the very moment an Overrun ends), the forms system is asked to re-apply what the form puts on the wielder, and the weapon's logic is
     * told about the change. The curse form never stays on.
     */
    static void forceBase(ServerPlayer p, HeroBond b) {
        if (HeroForm.BASE_ID.equals(b.form())) return;
        if (viaFormsSystem(p)) return;
        HeroForm from = HeroForms.active(b);
        b.setForm(HeroForm.BASE_ID);
        HeroForms.refresh(p);
        try {
            HeroCtx c = HeroCtx.of(p);
            if (c != null) HeroLogics.of(b.kind()).onFormChanged(c, from, HeroForms.active(b));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The weapon logic of {} failed in onFormChanged after the curse form ended", b.kind(), e);
        }
    }

    /** Asks the forms system to put the base form back with its forced change. True when it did. */
    private static boolean viaFormsSystem(ServerPlayer p) {
        if (FORCE_ACTIVE == null) return false;
        try {
            return Boolean.TRUE.equals(FORCE_ACTIVE.invoke(null, p, HeroForm.BASE_ID));
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("[nusmp] The forms system could not put the base form back for the curse, the curse does it itself: {}", e.toString());
            return false;
        }
    }

    // ================================================================== the offer, the Embrace, the decline
    static void announceOffer(ServerPlayer p, HeroBond b, CurseState s, Sin sin, long now, CurseTuning t) {
        HeroKind kind = b.kind();
        int lineNo = CurseText.whisperIndex(now / 100L);
        Component whisper = Component.translatableWithFallback(CurseText.whisperKey(sin, lineNo), CurseText.whispers(sin).get(lineNo));
        CurseMessages.bar(p, CurseMessages.colored(sin, CurseMessages.tr(CurseText.OFFER_BAR, kind.display(), whisper)));
        int hearts = CurseMachine.taxFor(s.series.size(), t);
        CurseMessages.chat(p, CurseMessages.colored(sin, CurseMessages.tr(CurseText.OFFER_CHAT, kind.display(), sin.label(), hearts, Component.keybind("key.nusmp.hero_embrace"), t.offerSeconds)));
        if (KeyThree.available()) CurseMessages.chat(p, CurseMessages.colored(sin, CurseMessages.tr(CurseText.OFFER_KEY3)));
        sound(p, SoundEvents.ELDER_GUARDIAN_CURSE, 0.7f, 0.6f);
        Vec3 at = p.position().add(0.0, 1.0, 0.0);
        VfxSpawn.send(p.serverLevel(), VfxShape.CURSE_FX3, at, at, CurseEffects.argb(sin), 28, 1.0f);
        hook(p, HOOK_OFFER, sin.id());
        CurseSync.push(p, s, shownSin(b), now, true, t);
    }

    /**
     * One look, before the once-a-second one, at a wielder who may be holding Sneak and the core's third hero key to accept an open offer. It goes through the same
     * {@link #embrace} as every other door, which checks everything again. Called every ten ticks, and only when the core's key bookkeeping exists.
     */
    static void pollKeyThree(ServerPlayer p, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound() || exempt(p) || CurseStore.offered(b).isEmpty()) {
            KeyThree.forget(p.getUUID());
            return;
        }
        if (KeyThree.step(p)) embrace(p, now, t);
    }

    /** The wielder accepts the open offer. True when the series was embraced. */
    static boolean embrace(ServerPlayer p, long now, CurseTuning t) {
        if (!CurseConfig.enabled()) {
            CurseMessages.chat(p, CurseMessages.tr(CurseText.REFUSE_OFF));
            return false;
        }
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) {
            CurseMessages.chat(p, CurseMessages.tr(CurseText.REFUSE_NO_WEAPON));
            return false;
        }
        CurseState s = CurseStore.load(b);
        CurseMachine.Embraced e = CurseMachine.embrace(s, now, t);
        CurseStore.save(b, s);
        if (!e.ok()) {
            String key = switch (e.refusal()) {
                case LOCKED -> CurseText.REFUSE_LOCKED;
                case ALREADY -> CurseText.REFUSE_ALREADY;
                default -> CurseText.REFUSE_NO_OFFER;
            };
            CurseMessages.chat(p, CurseMessages.tr(key));
            return false;
        }
        Sin sin = e.sin();
        HeroForm learned = learnFirstForm(p, b, sin);
        CurseEffects.ensureTax(p, s.tax, t.minHearts, true);
        showEmbrace(p, sin, learned, e.hearts());
        hook(p, HOOK_CURSE, sin.id());
        CurseSync.push(p, s, shownSin(b), now, true, t);
        return true;
    }

    /** Teaches the first curse form of the sin in the weapon's forms file (the lowest tier); null when the file has none. */
    static HeroForm learnFirstForm(ServerPlayer p, HeroBond b, Sin sin) {
        HeroForm f = CurseCandidates.firstForm(HeroForms.of(b.kind()), sin);
        if (f == null) return null;
        b.unlock(f.id());
        HeroForms.refresh(p);
        return f;
    }

    static void showEmbrace(ServerPlayer p, Sin sin, HeroForm learned, int hearts) {
        CurseMessages.title(p, CurseMessages.colored(sin, CurseMessages.tr(CurseText.EMBRACE_TITLE, sin.label()).withStyle(ChatFormatting.BOLD)),
                CurseMessages.tr(CurseText.EMBRACE_SUBTITLE, hearts).withStyle(ChatFormatting.GRAY), 10, 70, 25);
        Component chat = learned != null ? CurseMessages.tr(CurseText.EMBRACE_CHAT, sin.label(), learned.name(), hearts) : CurseMessages.tr(CurseText.EMBRACE_CHAT_NO_FORM, sin.label(), hearts);
        CurseMessages.chat(p, CurseMessages.colored(sin, chat.copy()));
        sound(p, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 0.7f);
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        VfxSpawn.send(level, VfxShape.CURSE_FX3, at.add(0.0, 1.0, 0.0), at.add(0.0, 1.0, 0.0), CurseEffects.argb(sin), 30, 2.0f);
        VfxSpawn.send(level, VfxShape.BOSS_DEVIL_SIGIL, at, at, CurseEffects.argb(sin), 40, 3.0f);
    }

    /** The wielder turns the open offer away. True when there was one. */
    static boolean decline(ServerPlayer p, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) {
            CurseMessages.chat(p, CurseMessages.tr(CurseText.REFUSE_NO_WEAPON));
            return false;
        }
        CurseState s = CurseStore.load(b);
        if (!CurseMachine.decline(s, now, t)) {
            CurseMessages.chat(p, CurseMessages.tr(CurseText.REFUSE_NO_OFFER));
            return false;
        }
        CurseStore.save(b, s);
        CurseMessages.bar(p, CurseMessages.tr(CurseText.DECLINE).withStyle(ChatFormatting.GRAY));
        CurseSync.push(p, s, shownSin(b), now, true, t);
        return true;
    }

    // ================================================================== corruption from outside
    /** Corruption from outside the once-a-second rule (a technique, a skill's price, an administrator). */
    static void addCorruption(ServerPlayer p, int amount, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound() || amount == 0) return;
        CurseState s = CurseStore.load(b);
        CurseMachine.Step step = CurseMachine.addCorruption(s, amount, now, t, p.getRandom().nextDouble());
        CurseStore.save(b, s);
        if (step == CurseMachine.Step.OVERRUN_STARTED) overrunStart(p, b, s, wornSin(b), now, t);
        else CurseSync.push(p, s, shownSin(b), now, false, t);
    }

    // ================================================================== the Overrun
    static void overrunStart(ServerPlayer p, HeroBond b, CurseState s, Sin sin, long now, CurseTuning t) {
        p.setShiftKeyDown(false);
        if (!b.summoned() && !p.isCreative()) {
            try {
                HeroSummon.summon(p);
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] The weapon could not be summoned for an Overrun and the Overrun went on without it: {}", e.toString());
            }
        }
        Sin shown = sin != null ? sin : Sin.WRATH;
        CurseMessages.title(p, CurseMessages.colored(shown, CurseMessages.tr(CurseText.OVERRUN_TITLE).withStyle(ChatFormatting.BOLD)),
                CurseMessages.tr(CurseText.OVERRUN_SUBTITLE).withStyle(ChatFormatting.GRAY), 5, 50, 15);
        sound(p, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        VfxSpawn.send(level, VfxShape.CURSE_FX2, at, at, CurseEffects.argb(shown), 80, 4.0f);
        VfxSpawn.send(level, VfxShape.BOSS_DEVIL_CRACKS, at, at, CurseEffects.argb(shown), 24, 5.0f);
        hook(p, HOOK_OVERRUN, b.kind().itemId());
        CurseSync.push(p, s, shownSin(b), now, true, t);
    }

    /** One forced swing: the wielder cannot sneak, and the weapon strikes the nearest living thing in reach (allies included). */
    private static void overrunStep(ServerPlayer p, CurseTuning t) {
        p.setShiftKeyDown(false);
        p.swing(InteractionHand.MAIN_HAND, true);
        LivingEntity target = nearestTarget(p, t);
        if (target == null) return;
        float dmg = BalanceLaw.damage(p, target, (float) t.overrunRaw, 0.5);
        if (target instanceof Player) dmg = OverrunMath.playerCap(dmg, target.getMaxHealth(), t.overrunPlayerShare);
        if (dmg > 0f) target.hurt(p.damageSources().playerAttack(p), dmg);
        Sin shown = wornSinOr(p);
        VfxSpawn.send(p.serverLevel(), VfxShape.CURSE_FX1, p.getEyePosition(), target.getBoundingBox().getCenter(), CurseEffects.argb(shown), 12, 0.6f);
    }

    private static Sin wornSinOr(ServerPlayer p) {
        Sin s = wornSin(HeroBonds.of(p));
        return s != null ? s : Sin.WRATH;
    }

    /** The nearest living thing within reach that the weapon may strike: not the wielder or their mount, not a creative or spectating player, not an armour stand, not a thing that cannot be hurt. */
    static LivingEntity nearestTarget(ServerPlayer p, CurseTuning t) {
        double reach = t.overrunReach;
        AABB box = p.getBoundingBox().inflate(reach);
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> struckByOverrun(p, e, t));
        return OverrunMath.nearest(near, e -> p.distanceToSqr(e), reach * reach);
    }

    private static boolean struckByOverrun(ServerPlayer p, LivingEntity e, CurseTuning t) {
        if (e == p || !e.isAlive() || e.isSpectator() || e.isInvulnerable() || e instanceof ArmorStand) return false;
        if (e == p.getVehicle() || p.isPassengerOfSameVehicle(e)) return false;                     // the weapon does not strike the mount the wielder sits on
        if (e instanceof Player other) return t.overrunHitsPlayers && !other.isCreative() && !other.isSpectator();
        return true;
    }

    /** The Overrun has run its time: the weapon lets go, the curse form ends, the corruption falls, the weakness begins. */
    private static void overrunEnd(ServerPlayer p, HeroBond b, CurseState s, long now, CurseTuning t) {
        if (CurseMachine.endOverrunIfDue(s, now, t) != CurseMachine.Step.OVERRUN_ENDED) return;
        CurseStore.save(b, s);                                // saved as over BEFORE the form is switched: the forms system asks HeroCurse.locked
        forceBase(p, b);
        keepWeak(p, (int) Math.min(Integer.MAX_VALUE, s.weakUntil - now));
        CurseMessages.chat(p, CurseMessages.tr(CurseText.OVERRUN_END).withStyle(ChatFormatting.GRAY));
        sound(p, SoundEvents.BEACON_DEACTIVATE, 0.9f, 0.8f);
        Vec3 at = p.position().add(0.0, 1.0, 0.0);
        VfxSpawn.send(p.serverLevel(), VfxShape.LIGHT_FLARE, at, at, 0xFFFFE6A0, 20, 1.5f);
        CurseSync.push(p, s, null, now, true, t);
    }

    // ================================================================== cleansing
    /**
     * A Purification Charm (or an administrator's purify): clears the corruption and, once, gives hearts of the tax back. True when it did something (the charm is then
     * used up); false with a reason on the screen when it did not.
     */
    static boolean charm(ServerPlayer p, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) {
            CurseMessages.bar(p, CurseMessages.tr(CurseText.PURIFY_NO_WEAPON).withStyle(ChatFormatting.GRAY));
            return false;
        }
        CurseState s = CurseStore.load(b);
        if (!CurseMachine.canCleanse(s, now)) {
            CurseMessages.bar(p, CurseMessages.tr(CurseText.PURIFY_LOCKED).withStyle(ChatFormatting.RED));
            return false;
        }
        CurseMachine.Cleansed c = CurseMachine.charm(s, now, t);
        if (!c.any()) {
            CurseMessages.bar(p, CurseMessages.tr(CurseText.PURIFY_NOTHING).withStyle(ChatFormatting.GRAY));
            return false;
        }
        CurseStore.save(b, s);
        if (c.hearts() > 0) CurseEffects.ensureTax(p, s.tax, t.minHearts, true);
        Component msg = CurseMessages.tr(CurseText.PURIFY_DONE).withStyle(ChatFormatting.GOLD);
        CurseMessages.bar(p, msg);
        if (c.hearts() > 0) CurseMessages.chat(p, CurseMessages.tr(CurseText.PURIFY_REFUND, c.hearts()).withStyle(ChatFormatting.GOLD));
        sound(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9f, 1.4f);
        sound(p, SoundEvents.BEACON_ACTIVATE, 0.5f, 1.6f);
        Vec3 at = p.position();
        ServerLevel level = p.serverLevel();
        VfxSpawn.send(level, VfxShape.DIVINE_SIGIL, at, at, 0xFFFFE6A0, 40, 2.5f);
        VfxSpawn.send(level, VfxShape.LIGHT_FLARE, at.add(0.0, 1.0, 0.0), at.add(0.0, 1.0, 0.0), 0xFFFFF4D0, 20, 1.2f);
        CurseSync.push(p, s, shownSin(b), now, true, t);
        return true;
    }

    // ================================================================== events
    /** A creature died. The wielder dying, an ally dying near a wielder, and a wielder killing something are each handled here. */
    static void onDeath(LivingEntity dead, DamageSource source, long now, CurseTuning t) {
        if (dead instanceof ServerPlayer sp && HeroBonds.has(sp) && !exempt(sp)) wielderDied(sp, source, now, t);
        CurseAllies.Who who = CurseAllies.classify(dead);
        if (who.kind() != CurseAllies.Kind.NONE) mourn(dead, who, now, t);
        Entity killer = source == null ? null : source.getEntity();
        if (killer instanceof ServerPlayer kp && kp != dead && HeroBonds.has(kp) && !exempt(kp)) killedBy(kp, dead, who, now, t);
    }

    private static void wielderDied(ServerPlayer sp, DamageSource source, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(sp);
        CurseState s = CurseStore.load(b);
        CurseMachine.onWielderDeath(s, t);
        CurseMachine.addDespair(s, Cause.DEFEAT, t.amount(Cause.DEFEAT), now, t);
        Entity killer = source == null ? null : source.getEntity();
        if (killer instanceof LivingEntity k && k != sp && BalanceLaw.isBoss(k)) CurseMachine.addDespair(s, Cause.BOSS_LOSS, t.amount(Cause.BOSS_LOSS), now, t);
        CurseStore.save(b, s);
    }

    /** Everyone bonded within reach to whom the dead creature mattered feels it. */
    private static void mourn(LivingEntity dead, CurseAllies.Who who, long now, CurseTuning t) {
        if (!(dead.level() instanceof ServerLevel level)) return;
        Cause cause = CurseAllies.causeOf(who.kind());
        double r2 = t.allyRadius * t.allyRadius;
        for (ServerPlayer q : level.players()) {
            if (q == dead || !q.isAlive() || exempt(q) || q.distanceToSqr(dead) > r2) continue;
            if (!HeroBonds.has(q) || !CurseAllies.mattersTo(who, dead, q)) continue;
            addDespair(q, cause, t.amount(cause), now, t);
        }
    }

    private static void killedBy(ServerPlayer kp, LivingEntity dead, CurseAllies.Who who, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(kp);
        Sin worn = wornSin(b);
        if (worn == Sin.GLUTTONY) CurseEffects.gluttonyFeed(kp, t);
        if (who.kind() == CurseAllies.Kind.NONE || !(dead.level() instanceof ServerLevel level)) return;
        if (!CurseMachine.inOverrun(CurseStore.load(b), now)) return;
        double r2 = t.allyRadius * t.allyRadius;
        for (ServerPlayer q : level.players()) {
            if (q == kp || q == dead || !q.isAlive() || exempt(q) || q.distanceToSqr(dead) > r2) continue;
            if (!HeroBonds.has(q) || !CurseAllies.mattersTo(who, dead, q)) continue;
            addDespair(q, Cause.OVERRUN_KILL, t.amount(Cause.OVERRUN_KILL), now, t);
        }
    }

    /** A wielder took damage from {@code source}: a hand that should have been a friend's is a betrayal. */
    static void onDamaged(ServerPlayer victim, DamageSource source, float amount, long now, CurseTuning t) {
        if (amount < 0.5f || exempt(victim) || !HeroBonds.has(victim)) return;
        if (CurseAllies.isBetrayal(victim, source == null ? null : source.getEntity())) addDespair(victim, Cause.BETRAYAL, t.amount(Cause.BETRAYAL), now, t);
    }

    // ================================================================== the dispatcher's hooks
    /** A technique was cast: with a curse form worn it adds corruption (an ultimate more), Greed charges its surcharge, Sloth starts its lock. */
    static void onTechnique(ServerPlayer p, HeroSkill skill, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound() || exempt(p)) return;
        HeroForm f = HeroForms.active(b);
        if (f == null || !f.curse() || f.sin() == null) return;
        CurseEffects.onTechnique(p, f.sin(), skill, t, now);
        addCorruption(p, CurseMachine.techniqueCorruption(skill.ultimate(), t), now, t);
    }

    /** The reason the curse refuses this technique now (shown to the wielder), or null. */
    static Component refuses(ServerPlayer p, HeroSkill skill, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound() || exempt(p)) return null;
        boolean overrun = CurseStore.overrunUntil(b) > now;
        return CurseEffects.refuses(p, skill, wornSin(b), overrun, t, now);
    }

    // ================================================================== login, respawn, bond changes
    /** After a login or a respawn: the tax is put back in force and the client learns the state. */
    static void onEnter(ServerPlayer p, long now, CurseTuning t) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) {
            CurseSync.reset(p, now);
            return;
        }
        CurseState s = CurseStore.load(b);
        CurseEffects.ensureTax(p, s.tax, t.minHearts, true);
        CurseSync.push(p, s, shownSin(b), now, true, t);
    }

    /** The weapon was released: everything the curse put on the player is taken off and the client is told. */
    static void onReleased(ServerPlayer p, long now) {
        CurseEffects.clearModifiers(p);
        CurseEffects.forget(p.getUUID());
        CurseSync.reset(p, now);
    }
}
