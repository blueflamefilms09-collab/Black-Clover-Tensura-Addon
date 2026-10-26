package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Solomon, Lord of Sorcery: the fourth of the five Veldanava ultimate skills (the numbers are in {@link SolomonRules}, the passive rules in {@link SolomonEvents} and {@link SolomonHooks}). Ten modes: four toggle
 * passives (Infinite Grimoire, Omnipotent Magic, Mana Skin, Elemental Sovereignty), five actives (Grand Spell Copy, Arcane Convergence, Spell Chain, Mana Zone Erasure and the held Realm of Magic) and the mastery
 * active The 72 Pillars. The press, cost, cooldown and toggle code is {@link VeldanavaSkillBase}.
 *
 * <p>Worst case per press (rule 11): Grand Spell Copy a walk of the 64-line cast log, no entity query; replaying the copy one grimoire spell (its own bounds); Arcane Convergence one box query of projectiles (at
 * most 64 discarded), one of clouds (32), the bolt list, the field list, no more than 96 things touched; Spell Chain three grimoire spells (their own bounds); Mana Zone Erasure the same sweep plus one summon
 * query (24 removed) and at most 24 casters stuttered; Realm of Magic one field and, once a second, one box query with at most 24 enemies touched; The 72 Pillars one box query, at most 8 targets marked (8 marks,
 * 8 pillar packets), one field of 31 pulses that lands at most 72 impacts in all (a box query each, at most 12 creatures hit, at most 600 blocks removed). The passives run once a second per owner: two attribute
 * checks, one box query of 24 blocks for the presence sense (8 creatures marked). One effect packet per press (the Realm: one every 80 ticks while it lasts).
 */
public class SolomonSkill extends VeldanavaSkillBase {
    public SolomonSkill() { super(SolomonRules.SOLOMON); }

    // ---------------------------------------------------------------- the passives that need a look once a second
    @Override
    protected void secondTick(ManasSkillInstance i, ServerPlayer p, int toggles, boolean mastered) {
        SolomonState.applyMagic(p, JudgmentRules.on(toggles, 0), mastered);
        if (JudgmentRules.on(toggles, 1)) presence(p);
        if (JudgmentRules.on(toggles, 3)) shrugOff(p);
    }

    @Override
    protected void toggled(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean on) {
        if (m.index() == 0) SolomonState.applyMagic(p, on, JudgmentState.mastered(p));
        if (m.index() == 3 && on) shrugOff(p);
    }

    @Override
    protected void forgotten(ServerPlayer p) {
        SolomonState.clear(p);
        SolomonCasts.dropCopy(p);
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), REALM_KIND);
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), SolomonPillars.KIND);
    }

    /** Omnipotent Magic's presence sense: the (at most eight) nearest foes within twenty-four blocks glow for two seconds, through walls, renewed every second. */
    private static void presence(ServerPlayer p) {
        double r = SolomonRules.PRESENCE_RADIUS, r2 = r * r;
        List<LivingEntity> near = new ArrayList<>(p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                x -> x != p && x.isAlive() && !x.isSpectator() && x.distanceToSqr(p) <= r2 && Foes.isFoe(p, x)));
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= SolomonRules.PRESENCE_MAX) break;
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, SolomonRules.PRESENCE_TICKS, 0, false, false, false));
        }
    }

    /** Elemental Sovereignty's second: flame, poison and frost on the wearer are put out (the blows from them are cancelled in {@link SolomonEvents}). */
    private static void shrugOff(ServerPlayer p) {
        if (p.isOnFire()) p.clearFire();
        if (p.hasEffect(MobEffects.POISON)) p.removeEffect(MobEffects.POISON);
        if (p.getTicksFrozen() > 0) p.setTicksFrozen(0);
    }

    // ---------------------------------------------------------------- the press
    /** Grand Spell Copy is two presses in one: with a copy in the Grimoire a press casts it, free, whatever the cooldown of copying says; without one it makes the copy. */
    @Override
    public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
        if (mode == 4 && e instanceof ServerPlayer p && JudgmentConfig.enabled() && SolomonCasts.hasCopy(p)) {
            castCopy(p);
            return;
        }
        super.onPressed(i, e, key, mode);
    }

    @Override
    protected boolean activate(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {
        return switch (m.index()) {
            case 4 -> copy(p);
            case 5 -> convergence(p);
            case 7 -> chain(p);
            case 8 -> erasure(p);
            default -> pillars(p);
        };
    }

    /**
     * Grand Spell Copy: reads the newest grimoire spell that somebody else cast within {@link SolomonRules#COPY_RANGE} blocks in the last {@link SolomonRules#COPY_WINDOW_TICKS} ticks and writes it into the
     * Grimoire; it is cast once, free, by the next press of this mode. Only grimoire spells are copied (the log is {@link SolomonCasts}).
     */
    private static boolean copy(ServerPlayer p) {
        SolomonCasts.Entry e = SolomonCasts.findToCopy(p);
        if (e == null) { fail(p, "No spell was cast near you lately."); return false; }
        SolomonCasts.storeCopy(p, e.spell());
        SolomonFx.copy(p.serverLevel(), p, e.at().add(0.0, 1.0, 0.0));
        String name = SolomonCasts.nameOf(e.spell());
        p.displayClientMessage(Component.literal("Copied " + (name.isEmpty() ? "a spell" : name) + ". Press Grand Spell Copy again to cast it free.").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** The free cast of the stored copy: once; a spell that does nothing keeps the copy. */
    private static void castCopy(ServerPlayer p) {
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong(GAP)) return;
        String spell = SolomonCasts.copy(p);
        if (!SolomonCasts.replayable(spell)) { SolomonCasts.dropCopy(p); fail(p, "The copied spell has faded from the Grimoire."); return; }
        if (!SolomonCasts.replay(p, spell)) { fail(p, "The copied spell found nothing to act on."); return; }
        SolomonCasts.dropCopy(p);
        p.getPersistentData().putLong(GAP, now + JudgmentRules.ACTIVE_GAP_TICKS);
        SolomonFx.copy(p.serverLevel(), p, p.position().add(0.0, 1.0, 0.0));
    }

    /** Arcane Convergence: what flies and lingers within thirty blocks is drawn in and turned into Magicule (never above a quarter of the maximum). */
    private static boolean convergence(ServerPlayer p) {
        SolomonSweep.Result r = SolomonSweep.sweep(p, SolomonRules.CONVERGENCE_RADIUS, SolomonRules.CONVERGENCE_MAX_PROJECTILES, SolomonRules.CONVERGENCE_MAX_CLOUDS, 0, false);
        if (r.total() <= 0) { fail(p, "Nothing within " + (int) SolomonRules.CONVERGENCE_RADIUS + " blocks to draw in."); return false; }
        double gain = SolomonRules.convergenceGain(r.projectiles(), r.clouds(), r.fields(), TensuraBridge.maxMagicule(p));
        TensuraBridge.refundMagicule(p, gain);
        SolomonFx.convergence(p.serverLevel(), p);
        p.displayClientMessage(Component.literal("Drew in " + r.total() + " (+" + (long) gain + " magicules).").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** Spell Chain: the last three grimoire spells the caster cast are cast together, free (a replay is never logged, so a chain does not feed itself). */
    private static boolean chain(ServerPlayer p) {
        List<String> last = SolomonCasts.lastOf(p.getUUID());
        int cast = 0;
        for (String s : last) {
            if (cast >= SolomonRules.CHAIN_COUNT) break;
            if (SolomonCasts.replay(p, s)) cast++;
        }
        if (cast == 0) { fail(p, "You have no spell to chain: cast a grimoire spell first."); return false; }
        SolomonFx.chain(p.serverLevel(), p);
        return true;
    }

    /** Mana Zone Erasure: the enemy's projectiles, bolts, clouds, fields and summons within forty blocks are deleted and their casters stutter for the time the Balance Law allows. */
    private static boolean erasure(ServerPlayer p) {
        SolomonSweep.Result r = SolomonSweep.sweep(p, SolomonRules.ERASURE_RADIUS, SolomonRules.ERASURE_MAX_PROJECTILES, SolomonRules.ERASURE_MAX_CLOUDS, SolomonRules.ERASURE_MAX_SUMMONS, true);
        if (r.total() <= 0) { fail(p, "Nothing of the enemy's within " + (int) SolomonRules.ERASURE_RADIUS + " blocks to erase."); return false; }
        for (LivingEntity c : r.casters()) {
            c.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_STUTTER, BalanceLaw.controlTicks(c, SolomonRules.ERASURE_STUTTER_TICKS), 0, false, true, true));
        }
        SolomonFx.erasure(p.serverLevel(), p);
        p.displayClientMessage(Component.literal("Erased " + r.total() + "; " + r.casters().size() + " casters stutter.").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /**
     * The 72 Pillars: up to eight foes within sixty blocks (players first, then the nearest) are marked and the seventy-two pillars are shared out between them ({@link SolomonRules#pillarsOnTarget}). One field
     * of the {@code pillars} kind lands them over about six seconds ({@link SolomonPillars}); each target gets one effect packet that carries the number of its pillars and a seed.
     */
    private static boolean pillars(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        double r = SolomonRules.PILLAR_RANGE, r2 = r * r;
        List<LivingEntity> near = new ArrayList<>(l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r, Math.min(r, 64.0), r),
                x -> x != p && x.isAlive() && !x.isSpectator() && x.distanceToSqr(p) <= r2 && Foes.isFoe(p, x)));
        if (near.isEmpty()) { fail(p, "No enemy within " + (int) r + " blocks to mark."); return false; }
        near.sort(Comparator.<LivingEntity>comparingInt(x -> x instanceof Player ? 0 : 1).thenComparingDouble(x -> x.distanceToSqr(p)));
        int marked = Math.min(SolomonRules.PILLAR_TARGETS_MAX, near.size());
        List<SolomonPillars.Target> targets = new ArrayList<>();
        for (int k = 0; k < marked; k++) {
            LivingEntity t = near.get(k);
            int count = SolomonRules.pillarsOnTarget(k, marked);
            targets.add(new SolomonPillars.Target(t.position(), count, SolomonRules.packSeed(l.random.nextLong(), count)));
        }
        long now = l.getGameTime();
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), SolomonPillars.KIND);
        if (!JudgmentFields.add(p, l, null, p.position(), SolomonRules.PILLAR_RANGE, SolomonRules.PILLAR_SPAN_TICKS + SolomonRules.PILLAR_DELAY_TICKS + SolomonRules.PILLAR_PULSE_TICKS,
                new SolomonPillars(targets, now), SolomonPillars.KIND)) {
            fail(p, "Too many fields stand on this server.");
            return false;
        }
        for (int k = 0; k < marked; k++) {
            SolomonPillars.Target t = targets.get(k);
            SolomonFx.mark(l, near.get(k));
            SolomonFx.pillars(l, t.ground(), t.seed());
        }
        SolomonFx.pillarsCast(l, p);
        p.displayClientMessage(Component.literal("The 72 Pillars fall on " + marked + (marked == 1 ? " target." : " targets.")).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    // ---------------------------------------------------------------- Realm of Magic (held)
    static final String REALM_KIND = "realm";

    @Override
    protected void startHeld(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {
        double perSecond = JudgmentRules.cost(m, mastered, JudgmentConfig.costScale());
        if (!p.isCreative() && !TensuraBridge.spendMagicule(p, perSecond)) { fail(p, "Not enough magicules (need " + (long) perSecond + " a second)."); return; }
        ServerLevel l = p.serverLevel();
        JudgmentFields.clearKind(p.getServer(), p.getUUID(), REALM_KIND);
        if (!JudgmentFields.add(p, l, p, p.position(), SolomonRules.REALM_RADIUS, SolomonRules.REALM_MAX_TICKS, new Realm(perSecond), REALM_KIND)) {
            if (!p.isCreative()) TensuraBridge.refundMagicule(p, perSecond);
            fail(p, "Too many fields stand on this server.");
            return;
        }
        SolomonState.realm(p);
        SolomonFx.realm(l, p, true);
        startCooldown(i, m, mastered);
        addMasteryPoint(i, p);
        i.markDirty();
    }

    @Override
    protected boolean held(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks, boolean mastered) {
        return JudgmentFields.find(p.getUUID(), REALM_KIND) != null;
    }

    @Override
    protected void released(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks) {
        if (JudgmentFields.clearKind(p.getServer(), p.getUUID(), REALM_KIND) > 0) fail(p, "The Realm of Magic is let go.");
    }

    /**
     * The Realm of Magic as a field that follows its owner: once a second it takes the next second's Magicule (and ends when there is none), renews the realm record the grimoire reads (allies inside cast for
     * nothing with a one-tick chant, see {@link SolomonHooks}), silences and locks the (at most 24) enemies inside, and draws the domain again every 80 ticks.
     */
    static final class Realm implements JudgmentFields.Behavior {
        private final double costPerSecond;
        private int pulses;

        Realm(double costPerSecond) { this.costPerSecond = costPerSecond; }

        @Override public int period() { return SolomonRules.REALM_PULSE_TICKS; }

        @Override
        public void pulse(ServerPlayer owner, ServerLevel level, JudgmentFields.Field f) {
            if (!owner.isCreative() && costPerSecond > 0.0 && !TensuraBridge.spendMagicule(owner, costPerSecond)) {
                fail(owner, "The Realm of Magic falls: no magicules left.");
                f.endsAt = 0L;
                return;
            }
            SolomonState.realm(owner);
            if (++pulses % (SolomonRules.REALM_FX_EVERY_TICKS / SolomonRules.REALM_PULSE_TICKS) == 0) SolomonFx.realm(level, owner, false);
            double r = f.radius, r2 = r * r;
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(r, Math.min(r, 48.0), r),
                    x -> x != owner && x.isAlive() && !x.isSpectator() && x.distanceToSqr(owner) <= r2);
            int n = 0;
            for (LivingEntity t : near) {
                if (n >= SolomonRules.REALM_MAX_ENEMIES) break;
                if (!Foes.isFoe(owner, t)) continue;
                n++;
                JudgmentEffects.silence(t, SolomonRules.REALM_SILENCE_TICKS);
                t.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, BalanceLaw.controlTicks(t, SolomonRules.REALM_LOCK_TICKS), 0, false, true, true));
            }
        }

        @Override
        public void end(ServerLevel level, JudgmentFields.Field f) { SolomonState.endRealm(f.owner); }
    }
}
