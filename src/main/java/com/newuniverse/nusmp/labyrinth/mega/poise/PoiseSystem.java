package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.eco.TurfMarks;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryConfig;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import com.newuniverse.nusmp.labyrinth.squad.Squad;
import com.newuniverse.nusmp.labyrinth.squad.SquadHook;
import com.newuniverse.nusmp.labyrinth.squad.SquadManager;
import com.newuniverse.nusmp.labyrinth.squad.SquadRole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Why: this is the WORLD ADAPTER of the poise layer: the pure rules ({@link PoiseState}, {@link PoiseMath}, {@link Desperation}, {@link PoiseBearers}) say what happens, this class does it to
 * real mobs. It decides who is a bearer ({@link #consider}: an elite or a squad captain of the dungeon, never a boss, a servant or a minion) and attaches the bar; it keeps a weak set of the
 * bearers and steps each one every tick (the bar's regeneration, the desperation check every fifth tick, the embers every half second), so a world with no bearer pays for an empty loop; it
 * answers the two damage-time questions: {@link #onIncomingDamage} (a blow while super armor is up drains the bar and loses a share of its damage; the blow that empties the bar staggers) and
 * {@link #onKnockBack} (no push while the armor holds). The stagger is an effect plus, for a mob with its own brain ({@link PoiseAware}), a cancelled cast. Desperation adds a speed buff, the cue,
 * and either the foundry mob's ultimate (through {@link PoiseAware}) or, for any other mob, the {@link DesperationShockwaveGoal}. Every handler catches everything: a fault here must never stop a mob
 * from being hit or from existing; the first one is logged.
 */
public final class PoiseSystem {
    private PoiseSystem() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;
    private static volatile PoiseTuning tuning = PoiseTuning.DEFAULT;
    private static final Set<Mob> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());
    static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath("nusmp", "poise_desperate_speed");

    // ================================================================ what the rest of the mod asks
    /** The numbers in force (rebuilt from the config once a second). */
    public static PoiseTuning tuning() { return tuning; }

    private static PoiseTuning build() {
        return PoiseTuning.DEFAULT.with(FoundryConfig.get(FoundryConfig.DESPERATE_BELOW), FoundryConfig.get(FoundryConfig.ELITE_TIER), FoundryConfig.get(FoundryConfig.ARMOR_REDUCTION),
                FoundryConfig.get(FoundryConfig.STAGGER_TICKS));
    }

    public static TelegraphLibrary telegraphs() { return TelegraphData.library(); }

    public static boolean enabled() { return FoundryConfig.get(FoundryConfig.POISE); }

    /** The attached data of a mob, or null when it carries no bar. */
    public static PoiseData data(Mob m) { return m.hasData(PoiseRegistry.POISE) ? m.getData(PoiseRegistry.POISE) : null; }

    private static long now(Mob m) { return m.level().getGameTime(); }

    /** Is the mob helpless right now (its bar broke)? Cheap: the foundry's goals ask it every tick. */
    public static boolean staggered(Mob m) { return staggered(m, now(m)); }

    public static boolean staggered(Mob m, long now) {
        PoiseData d = data(m);
        return d != null && d.state.staggered(now);
    }

    public static boolean armored(Mob m) { return armored(m, now(m)); }

    public static boolean armored(Mob m, long now) {
        PoiseData d = data(m);
        return d != null && d.state.armored(now);
    }

    public static boolean desperate(Mob m) {
        PoiseData d = data(m);
        return d != null && d.desperate;
    }

    /** A cooldown (in ticks) as this mob lives it: shorter in desperation, unchanged for a mob with no bar. */
    public static int scale(Mob m, int cooldown) {
        PoiseData d = data(m);
        return d == null || !d.desperate ? cooldown : Desperation.cooldown(cooldown, true, tuning);
    }

    /** Super armor for {@code ticks} ticks (a telegraphed attack begins). A mob with no bar ignores it. */
    public static void raiseArmor(Mob m, int ticks) {
        PoiseData d = data(m);
        if (d != null) d.state.raiseArmor(now(m), ticks);
    }

    public static void dropArmor(Mob m) {
        PoiseData d = data(m);
        if (d != null) d.state.dropArmor();
    }

    // ================================================================ who carries a bar
    private static boolean isBoss(Mob m) {
        if (m instanceof BudgetedBoss || m instanceof FloorWardenEntity) return true;
        if (TurfMarks.anyOf(m.getTags(), TurfMarks.BOSS_TAGS)) return true;
        if (m.getType().is(Tags.EntityTypes.BOSSES)) return true;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(m.getType());
        return id != null && id.toString().equals(TurfMarks.WARDEN_ID);
    }

    /** A creature that answers to a player: a tamed or owned one, a shadow, a relic's summon (the markers the turf wars use). */
    static boolean isServant(Mob m) {
        if (m instanceof TamableAnimal t && t.isTame()) return true;
        if (m instanceof OwnableEntity o && o.getOwnerUUID() != null) return true;
        if (TurfMarks.anyOf(m.getTags(), TurfMarks.SERVANT_TAGS)) return true;
        CompoundTag data = m.getPersistentData();
        if (data.isEmpty()) return false;
        for (String key : TurfMarks.SERVANT_KEYS) if (data.contains(key)) return true;
        return false;
    }

    /** Is this mob the leader of its squad? */
    static boolean isCaptain(Mob m) {
        if (!m.getTags().contains(Squad.TAG)) return false;
        Squad s = SquadManager.of(m);
        if (s != null) return s.leaderId() != null && s.leaderId().equals(m.getUUID());
        return SquadRole.LEADER.id().equals(m.getPersistentData().getString(Squad.ROLE));
    }

    /** What this mob is for the poise layer right now. */
    public static PoiseBearers.Kind classify(Mob m) {
        boolean inDungeon = Labyrinth.in(m.level());
        boolean foundry = m instanceof FoundryMobEntity;
        FoundryMobEntity f = foundry ? (FoundryMobEntity) m : null;
        return PoiseBearers.classify(inDungeon, inDungeon && isBoss(m), inDungeon && isServant(m), f != null && f.isMinion(), inDungeon && isCaptain(m),
                m.getTags().contains(PoiseBearers.ELITE_TAG), f != null && f.foundryId().length() > 0 ? f.tier() : 0, tuning.eliteTier());
    }

    private static boolean candidate(Mob m) {
        return m instanceof FoundryMobEntity || m.getTags().contains(Squad.TAG) || m.getTags().contains(PoiseBearers.ELITE_TAG);
    }

    /**
     * Gives the mob a bar when it is a bearer (and starts stepping it), or just starts stepping a mob that already has one (it came back from a saved chunk). Returns the data, or null when the
     * mob is not a bearer or the layer is off. Safe to call again and again.
     */
    public static PoiseData consider(Mob m) {
        try {
            if (m == null || m.level().isClientSide || !enabled() || !Labyrinth.in(m.level())) return null;
            PoiseData d = data(m);
            if (d != null) {
                if (d.kind == PoiseBearers.Kind.NONE) return null;
                if (TRACKED.add(m) && d.desperate) applyDesperate(m, true);
                return d;
            }
            PoiseBearers.Kind kind = classify(m);
            if (kind == PoiseBearers.Kind.NONE) return null;
            d = m.getData(PoiseRegistry.POISE);
            d.kind = kind;
            int tier = m instanceof FoundryMobEntity f && f.foundryId().length() > 0 ? f.tier() : 1;
            d.state.restore(PoiseMath.maxPoise(m.getMaxHealth(), tier, kind == PoiseBearers.Kind.CAPTAIN, tuning), Double.MAX_VALUE, 0);
            TRACKED.add(m);
            return d;
        } catch (RuntimeException | LinkageError ex) {
            warn("consider", ex);
            return null;
        }
    }

    /** Called by the squad hook once the roles are handed out: the captain (and any elite) of a fresh squad gets its bar now, not at the first blow. */
    public static void considerAll(List<? extends Mob> mobs) {
        for (Mob m : mobs) consider(m);
    }

    // ================================================================ blows
    /** What a blow did: the damage that gets through and what it did to the stance. */
    public record Outcome(float amount, PoiseState.Hit hit) {}

    /**
     * Applies one blow of {@code amount} to the bearer: drains the bar if the armor is up, takes the armor's share off the damage, staggers if the bar broke. A mob without a bar takes the whole
     * blow ({@link PoiseState.Hit#OPEN}). This is what {@link #onIncomingDamage} calls; the self-test calls it directly.
     */
    public static Outcome hit(Mob m, float amount, boolean projectile) {
        PoiseData d = data(m);
        if (d == null || d.kind == PoiseBearers.Kind.NONE) return new Outcome(amount, PoiseState.Hit.OPEN);
        long now = now(m);
        PoiseTuning t = tuning;
        PoiseState.Hit h = d.state.hit(now, PoiseMath.poiseDamage(amount, projectile, t), t);
        float through = switch (h) {
            case ABSORBED -> (float) PoiseMath.damageThrough(amount, true, t);
            default -> amount;
        };
        if (h == PoiseState.Hit.BROKEN) onBroken(m, t);
        return new Outcome(through, h);
    }

    private static void onBroken(Mob m, PoiseTuning t) {
        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t.staggerTicks(), 4, false, true));
        m.getNavigation().stop();
        if (m instanceof PoiseAware a) a.poiseStaggered(t.staggerTicks());
        if (m.level() instanceof ServerLevel level) PoiseCue.broken(level, m);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            if (!(e.getEntity() instanceof Mob m) || m.level().isClientSide) return;
            DamageSource src = e.getSource();
            if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            PoiseData d = data(m);
            if (d == null) {
                if (!candidate(m) || consider(m) == null) return;
            }
            Outcome o = hit(m, e.getAmount(), src.is(DamageTypeTags.IS_PROJECTILE));
            if (o.amount() != e.getAmount()) e.setAmount(o.amount());
        } catch (RuntimeException | LinkageError ex) {
            warn("damage", ex);
        }
    }

    /** No push while the armor holds (the blow that breaks the bar drops the armor first, so that one does push). */
    public static void onKnockBack(LivingKnockBackEvent e) {
        try {
            if (!(e.getEntity() instanceof Mob m) || m.level().isClientSide) return;
            if (armored(m)) e.setCanceled(true);
        } catch (RuntimeException | LinkageError ex) {
            warn("knockback", ex);
        }
    }

    public static void onJoin(EntityJoinLevelEvent e) {
        try {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob m)) return;
            if (m.hasData(PoiseRegistry.POISE) || candidate(m)) consider(m);
        } catch (RuntimeException | LinkageError ex) {
            warn("join", ex);
        }
    }

    // ================================================================ time
    public static void onServerTick(ServerTickEvent.Post e) {
        try {
            if (e.getServer().getTickCount() % 20 == 0) tuning = build();
            if (TRACKED.isEmpty()) return;
            List<Mob> gone = null;
            for (Mob m : TRACKED) {
                if (m.isRemoved() || !m.isAlive() || m.level().isClientSide) {
                    if (gone == null) gone = new ArrayList<>();
                    gone.add(m);
                    continue;
                }
                step(m, now(m));
            }
            if (gone != null) TRACKED.removeAll(gone);
        } catch (RuntimeException | LinkageError ex) {
            warn("tick", ex);
        }
    }

    /** One tick of one bearer at game tick {@code now}: the bar's regeneration, the desperation check (every fifth tick of the mob), the embers (every tenth). Public for the self-test, which drives time by hand. */
    public static void step(Mob m, long now) {
        PoiseData d = data(m);
        if (d == null) return;
        PoiseTuning t = tuning;
        d.state.tick(now, 1, t);
        if (d.state.staggered(now) && !(m instanceof PoiseAware)) m.getNavigation().stop();
        if (m.tickCount % 5 != 0) return;
        float max = m.getMaxHealth();
        double share = max <= 0f ? 1.0 : m.getHealth() / max;
        boolean next = FoundryConfig.get(FoundryConfig.DESPERATION) && Desperation.next(d.desperate, share, t);
        if (next != d.desperate) setDesperate(m, d, next);
        if (d.desperate && m.tickCount % 10 == 0 && m.level() instanceof ServerLevel level) PoiseCue.aura(level, m);
    }

    /** Enters or leaves the desperation phase (public for the self-test). */
    public static void setDesperate(Mob m, PoiseData d, boolean on) {
        d.desperate = on;
        applyDesperate(m, on);
        if (on && m.level() instanceof ServerLevel level) PoiseCue.desperate(level, m);
    }

    private static void applyDesperate(Mob m, boolean on) {
        AttributeInstance speed = m.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (on) speed.addOrReplacePermanentModifier(new AttributeModifier(SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else speed.removeModifier(SPEED);
        }
        if (m instanceof PoiseAware a) {
            a.poiseDesperate(on);
        } else if (on && !hasShockwave(m)) {
            SquadHook.addGoal(m, 1, new DesperationShockwaveGoal(m));
        }
    }

    static boolean hasShockwave(Mob m) {
        return SquadHook.hasGoal(m, DesperationShockwaveGoal.class);
    }

    /** How many bearers are being stepped (the self-test and the admin read it). */
    public static int tracked() { return TRACKED.size(); }

    private static void warn(String what, Throwable t) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] The poise layer failed once in {} and goes on: {}", what, t.toString(), t);
    }
}
