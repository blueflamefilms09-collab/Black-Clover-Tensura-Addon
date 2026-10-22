package com.newuniverse.nusmp.spiritcontrol;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: COMBAT MODE. A per-player toggle (set from the grimoire's "Spirit Combat Mode" page or {@code /multiverse spirit combat on|off}). While it is on and a spirit is out, the spirit helps in the fight: on its
 * own cooldown it casts one of its small list of spells ({@link SpiritCatalog}) at the owner's hostile target, and every cast costs BOTH pools: the spirit's own mana ({@link SpiritMana}, a pool that grows
 * with the bond and refills lazily) and a share of the owner's magicules ({@link ManaSplit}, config {@code combat.userManaShare}, half and half by default). The spell's power follows the bond and both pools
 * ({@link CombatRules#powerMult}). When either pool cannot pay, the spirit stops casting and says so on the action bar (at most once every five seconds).
 *
 * <p><b>Bounds (docs/magic/spirit_control.md):</b> the work is only done for players who switched the mode on (a map lookup per player per tick, nothing when the map is empty); one decision per player every
 * {@code decisionTicks} ticks (spread over the players by UUID), at most {@code maxDecisionsPerTick} decisions on the whole server per tick ({@link TickBudget}); a decision does at most ONE entity query (and none
 * when the owner's last target is still good, or when idle and the idle clock has not run out), looks at no more than {@code targetCap} candidates, and casts at most one spell; at most one cast per player per
 * {@code minCastGapTicks} and {@code maxCastsPerSecondServer} on the whole server ({@link RateWindow}); the damage of a cast is capped ({@code maxDamagePerCast}); a cast is one effect packet to the players
 * near it, through the effect budget, plus one level sound. No chunk is loaded (only entities already in loaded chunks are asked), no block is touched. The targets are hostile mobs the owner fights, or a
 * player only where the server already treats them as an enemy of the owner (PvP allowed, not on the owner's FTB team); never the owner's pets, summons, allies or teammates.</p>
 *
 * <p>All state is per player UUID, in {@link #ACTIVE} (the mode) and {@link #POOLS} (the pools and cooldowns), and is dropped at logout, death, respawn, dimension change and server stop.</p>
 */
public final class SpiritCombat {
    private SpiritCombat() {}

    /** The state of one player's combat mode. */
    static final class Fight {
        long nextCastAt, nextScan, nextSay;
        int rotate;
    }

    /** The pools and spell cooldowns of one player, per spirit id. */
    static final class Pools {
        final Map<String, SpiritMana> mana = new HashMap<>(4);
        final Map<String, long[]> ready = new HashMap<>(4);
    }

    static final Map<UUID, Fight> ACTIVE = new HashMap<>();
    static final Map<UUID, Pools> POOLS = new HashMap<>();
    private static final TickBudget DECISIONS = new TickBudget();
    private static RateWindow casts = new RateWindow(SpiritControlConfig.serverCastsPerSecond(), 20);
    private static final PerfProbe.Section DECIDE_SECTION = PerfProbe.section("s.spirit_control.decide");

    // ================================================================================================ the toggle
    public static boolean isOn(UUID id) { return ACTIVE.containsKey(id); }

    /** The Spirit Lord's own free casts give way while the mode is on (it casts through here instead, and pays). */
    public static boolean takesOver(LivingEntity owner) {
        return !ACTIVE.isEmpty() && owner instanceof ServerPlayer p && ACTIVE.containsKey(p.getUUID()) && SpiritControlConfig.combat() && SpiritControlConfig.spiritOn("lord");
    }

    public static void set(ServerPlayer p, boolean on) {
        UUID id = p.getUUID();
        if (on) {
            if (ACTIVE.putIfAbsent(id, new Fight()) == null) {
                p.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_control.combat_on", "Spirit combat mode ON: your spirits cast with their mana and yours.").withStyle(ChatFormatting.GOLD), true);
            }
        } else if (ACTIVE.remove(id) != null) {
            p.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_control.combat_off_now", "Spirit combat mode off.").withStyle(ChatFormatting.GRAY), true);
        }
        com.newuniverse.nusmp.spiritcontrol.SpiritControlPages.refresh(p);
    }

    // ================================================================================================ the tick
    /** Called each tick for a player in {@link #ACTIVE} by the event (the caller already checked the map). */
    static void tick(ServerPlayer p) {
        Fight f = ACTIVE.get(p.getUUID());
        if (f == null) return;
        long now = p.level().getGameTime();
        if (!CombatRules.decisionTick(now, p.getUUID().hashCode(), SpiritControlConfig.decisionTicks())) return;
        if (!SpiritControlConfig.combat()) { ACTIVE.remove(p.getUUID()); return; }
        if (!p.isAlive() || p.isSpectator()) return;
        if (!DECISIONS.take(now, SpiritControlConfig.decisionsPerTick())) return;
        long t0 = PerfProbe.begin();
        try {
            decide(p, f, now);
        } finally {
            PerfProbe.end(DECIDE_SECTION, t0);
        }
    }

    /** The first spirit that is out, starting after the one that cast last (so two spirits take turns). */
    private static SpiritHandle outSpirit(ServerPlayer p, Fight f) {
        List<SpiritHandle> all = SpiritControl.REG.all();
        int n = all.size();
        for (int k = 0; k < n; k++) {
            SpiritHandle h = all.get(Math.floorMod(f.rotate + k, n));
            if (SpiritControlConfig.spiritOn(h.id()) && h.isOut(p)) { f.rotate = Math.floorMod(f.rotate + k + 1, n); return h; }
        }
        return null;
    }

    private static void decide(ServerPlayer p, Fight f, long now) {
        if (now < f.nextCastAt) return;
        SpiritHandle h = outSpirit(p, f);
        if (h == null) return;
        List<SpiritSpellDef> spells = h.spellsFor(p);
        if (spells.isEmpty()) return;
        Pools pools = POOLS.computeIfAbsent(p.getUUID(), k -> new Pools());
        int trust = h.trust(p);
        SpiritMana mana = pools.mana.get(h.id());
        if (mana == null) { mana = new SpiritMana(SpiritControlConfig.manaMax(trust), SpiritControlConfig.manaRegen(trust), now); pools.mana.put(h.id(), mana); }
        else mana.configure(SpiritControlConfig.manaMax(trust), SpiritControlConfig.manaRegen(trust));
        mana.advance(now);
        long[] ready = pools.ready.get(h.id());
        if (ready == null || ready.length != spells.size()) { ready = new long[spells.size()]; pools.ready.put(h.id(), ready); }
        double share = SpiritControlConfig.userShare();
        double range = 0;
        for (SpiritSpellDef s : spells) range = Math.max(range, s.range());
        range *= SpiritControlConfig.rangeScale();
        LivingEntity target = findTarget(p, f, now, range);
        if (target == null) return;
        if (!p.hasLineOfSight(target)) return;
        double dist = p.distanceTo(target);
        // the owner's pool
        var ex = p.isCreative() ? null : TensuraStorages.getExistenceFrom(p);
        double max = p.isCreative() ? 1.0 : EnergyHelper.getMaxMagicule(p), have = ex == null ? 0.0 : ex.getMagicule();
        double costMult = ManaLaw.costMult(p);
        double[] userCost = new double[spells.size()];
        if (!p.isCreative()) for (int i = 0; i < userCost.length; i++) userCost[i] = ManaSplit.userMagicules(spells.get(i).units(), share, max, SpiritControlConfig.unitPercent(), SpiritControlConfig.userFloor(), costMult);
        double spendable = p.isCreative() ? Double.MAX_VALUE : Math.max(0.0, have - max * SpiritControlConfig.reservePercent() / 100.0);
        CombatRules.Pick pick = CombatRules.pick(scaled(spells), ready, now, dist, mana.current(), share, userCost, spendable);
        if (!pick.cast()) {
            if (pick.why() == CombatRules.Refusal.NO_SPIRIT_MANA || pick.why() == CombatRules.Refusal.NO_USER_MANA) outOfMana(p, f, now, h, pick.why());
            return;
        }
        if (casts.max() != SpiritControlConfig.serverCastsPerSecond()) casts = new RateWindow(SpiritControlConfig.serverCastsPerSecond(), 20);
        if (!casts.tryAcquire(now)) return;                                    // the whole server's ceiling this second
        SpiritSpellDef spell = spells.get(pick.index());
        double userFill = p.isCreative() || max <= 0 ? 1.0 : Math.min(1.0, have / max);
        double power = CombatRules.powerMult(trust, mana.fill(), userFill);
        mana.spend(ManaSplit.spiritUnits(spell.units(), share));
        if (ex != null && userCost[pick.index()] > 0) {
            ex.setMagicule(Math.max(0.0, ex.getMagicule() - userCost[pick.index()]));
            ex.markDirty();
        }
        ready[pick.index()] = now + spell.cooldownTicks();
        f.nextCastAt = now + SpiritControlConfig.castGap();
        apply(p, h, spell, target, power);
    }

    /** The spell list with every range scaled by the config (a new list only when the scale is not 1; the common case allocates nothing). */
    private static List<SpiritSpellDef> scaled(List<SpiritSpellDef> spells) {
        double k = SpiritControlConfig.rangeScale();
        if (k == 1.0) return spells;
        List<SpiritSpellDef> out = new ArrayList<>(spells.size());
        for (SpiritSpellDef s : spells) out.add(new SpiritSpellDef(s.id(), s.label(), s.raw(), s.units(), s.cooldownTicks(), s.range() * k, s.effect(), s.shape(), s.color(), s.sound()));
        return out;
    }

    private static void outOfMana(ServerPlayer p, Fight f, long now, SpiritHandle h, CombatRules.Refusal why) {
        if (now < f.nextSay) return;
        f.nextSay = now + 100;
        if (why == CombatRules.Refusal.NO_SPIRIT_MANA) p.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_control.spirit_dry", "%s has no mana left to cast.", h.display()).withStyle(ChatFormatting.AQUA), true);
        else p.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_control.user_dry", "%s waits: you have no mana to spare.", h.display()).withStyle(ChatFormatting.AQUA), true);
    }

    // ================================================================================================ the target
    /** The owner's current target, else (one query, behind its clock) the nearest hostile mob. Null when there is none. */
    private static LivingEntity findTarget(ServerPlayer p, Fight f, long now, double range) {
        int window = SpiritControlConfig.fightWindow();
        LivingEntity c = p.getLastHurtMob();
        if (c != null && p.tickCount - p.getLastHurtMobTimestamp() <= window && valid(p, c, range)) return c;
        c = p.getLastHurtByMob();
        if (c != null && p.tickCount - p.getLastHurtByMobTimestamp() <= window && valid(p, c, range)) return c;
        boolean fight = CombatRules.inFight(p.tickCount, p.getLastHurtMobTimestamp(), p.getLastHurtByMobTimestamp(), window);
        if (!fight && now < f.nextScan) return null;
        f.nextScan = now + SpiritControlConfig.idleScan();
        AABB box = p.getBoundingBox().inflate(range, range * 0.5, range);
        List<Mob> found = p.serverLevel().getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER || e.getTarget() == p));
        int n = Math.min(found.size(), SpiritControlConfig.targetCap());
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            Mob m = found.get(i);
            double d = m.distanceToSqr(p);
            if (d < bestD && valid(p, m, range)) { best = m; bestD = d; }
        }
        return best;
    }

    /** The legal-target rule with the facts of the game gathered (see {@link CombatRules#validTarget}). */
    static boolean valid(ServerPlayer p, LivingEntity e, double range) {
        if (e == null || p.distanceToSqr(e) > range * range) return false;
        boolean isPlayer = e instanceof Player;
        boolean hostile = e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER;
        boolean hunting = e instanceof Mob m && m.getTarget() == p;
        boolean foe = Foes.isFoe(p, e);
        return foe && CombatRules.validTarget(e == p, e.isAlive(), e.isSpectator(), e.isAlliedTo(p), Foes.belongsToSide(p, e), isPlayer, isPlayer && foe, hostile, hunting);
    }

    // ================================================================================================ the cast
    private static final Map<String, VfxShape> SHAPES = new HashMap<>();

    private static VfxShape shape(String name) {
        VfxShape s = SHAPES.get(name);
        if (s == null) {
            try { s = VfxShape.valueOf(name); } catch (IllegalArgumentException e) { s = VfxShape.MAGIC_CIRCLE_EXPLOSION; }
            SHAPES.put(name, s);
        }
        return s;
    }

    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

    private static SoundEvent sound(String key) {
        SoundEvent s = SOUNDS.get(key);
        if (s == null) {
            s = switch (key) {
                case "fire" -> SoundEvents.BLAZE_SHOOT;
                case "water" -> SoundEvents.PLAYER_SPLASH;
                case "wind" -> SoundEvents.PHANTOM_FLAP;
                case "earth" -> SoundEvents.GRAVEL_BREAK;
                case "light" -> SoundEvents.AMETHYST_BLOCK_CHIME;
                case "dark" -> SoundEvents.WITHER_SHOOT;
                default -> SoundEvents.ENDERMAN_TELEPORT;
            };
            SOUNDS.put(key, s);
        }
        return s;
    }

    /** Deals one cast: the capped damage through the balance law (credited to the owner), the spell's small effect, one effect packet, one sound. */
    static void apply(ServerPlayer p, SpiritHandle h, SpiritSpellDef spell, LivingEntity target, double power) {
        ServerLevel sl = p.serverLevel();
        float raw = (float) (spell.raw() * SpiritControlConfig.damageScale() * power);
        float dmg = CombatRules.clampDamage(BalanceLaw.damage(p, target, raw, 0.5), SpiritControlConfig.maxDamage());
        if (dmg > 0f) target.hurt(sl.damageSources().indirectMagic(p, p), dmg);
        switch (spell.effect()) {
            case IGNITE -> target.igniteForSeconds(3);
            case SLOW -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(target, 60), 1, false, true));
            case KNOCKUP -> {
                if (!BalanceLaw.isBoss(target)) { target.setDeltaMovement(target.getDeltaMovement().add(0, 0.35, 0)); target.hurtMarked = true; }
            }
            case GLOW -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false, false));
            case WEAKEN -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BalanceLaw.controlTicks(target, 80), 0, false, true));
            default -> { }
        }
        Vec3 from = h.origin(p), to = target.getBoundingBox().getCenter();
        VfxSpawn.send(sl, shape(spell.shape()), from, to, spell.color(), 14, 0.8f);
        sl.playSound(null, from.x, from.y, from.z, sound(spell.sound()), SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    // ================================================================================================ report and cleanup
    /** "mana 42/90 (+3.6/s)" for the status line, or null when the spirit has no pool yet. */
    static String manaLine(ServerPlayer p, SpiritHandle h) {
        Pools pools = POOLS.get(p.getUUID());
        SpiritMana m = pools == null ? null : pools.mana.get(h.id());
        if (m == null) return null;
        m.advance(p.level().getGameTime());
        return String.format(java.util.Locale.ROOT, "mana %d/%d (+%.1f/s)", (int) m.current(), (int) m.max(), m.regenPerSecond());
    }

    /** The mode ends (death, dimension change); the pools stay so a change of world does not refill them. */
    public static void stop(UUID id) { ACTIVE.remove(id); }

    /** Logout: everything of this player. */
    public static void forget(UUID id) {
        ACTIVE.remove(id);
        POOLS.remove(id);
    }

    public static void clearAll() {
        ACTIVE.clear();
        POOLS.clear();
    }

    // ================================================================================================ for the self-test
    /** One forced cast of a given spell on a given target with a given spirit, paying both pools (no target search, no cooldown check): the test's way to see the pools drain. */
    static boolean castNow(ServerPlayer p, SpiritHandle h, int spellIndex, LivingEntity target, long now) {
        List<SpiritSpellDef> spells = h.spellsFor(p);
        Pools pools = POOLS.computeIfAbsent(p.getUUID(), k -> new Pools());
        int trust = h.trust(p);
        SpiritMana mana = pools.mana.computeIfAbsent(h.id(), k -> new SpiritMana(SpiritControlConfig.manaMax(trust), SpiritControlConfig.manaRegen(trust), now));
        SpiritSpellDef s = spells.get(spellIndex);
        double share = SpiritControlConfig.userShare();
        if (!mana.spend(ManaSplit.spiritUnits(s.units(), share))) return false;
        apply(p, h, s, target, CombatRules.powerMult(trust, mana.fill(), 1.0));
        return true;
    }

    static SpiritMana manaOf(UUID id, String spirit) {
        Pools pools = POOLS.get(id);
        return pools == null ? null : pools.mana.get(spirit);
    }

    /** All the players in combat mode (a copy of the ids, for the report). */
    public static List<UUID> active() { return new ArrayList<>(ACTIVE.keySet()); }

}
