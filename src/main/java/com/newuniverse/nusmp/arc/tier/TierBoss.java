package com.newuniverse.nusmp.arc.tier;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.entity.zogratis.Move;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: sixteen (and later about a hundred and thirty) light bosses must not be sixteen classes. This ONE class is every light boss of the owner's tier list: it is the
 * arcs' {@link ArcBoss} (power curve, telegraphs, the per-second damage budget, the boss bar, the story credit, no regeneration) whose character and moves come from a
 * data row ({@link TierRow}); one entity type is registered per character (so each has its own id, egg, name and floor), all built by the same factory, which hands the
 * row to the entity. Nothing about a fight is saved beyond what the framework saves; the row is looked up by the entity type.
 *
 * <p>Cost: a boss does what the framework does (one decision a second, one move at a time) and every move is bounded ({@link TierMoves}). The set {@link #LIVE} holds the
 * UUIDs of the light bosses that are in loaded chunks, so the summoning door can refuse a new one past the server's cap (config {@code arc.tierBossMax}); it is only ever
 * written by the boss itself (add on its tick, remove when it leaves the world) and holds at most the cap plus those spawned by other doors.
 */
public class TierBoss extends ArcBoss {
    /** The light bosses that are in the world now (server side). */
    private static final Set<UUID> LIVE = ConcurrentHashMap.newKeySet();

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private final TierRow row;
    private List<Move> book;
    /** 3.39.0: what its traits remember about the players it fights (empty for a boss without traits). */
    private final TierState traitState = new TierState();
    private boolean traitWarned;

    public TierBoss(EntityType<? extends TierBoss> type, Level level, TierRow row) {
        super(type, level);
        this.row = row;
        this.xpReward = 20 + 25 * row.letter().ordinal();
    }

    /** How many light bosses stand in loaded chunks. */
    public static int liveCount() { return LIVE.size(); }

    public TierRow row() { return row; }

    /** 3.39.0: the memory of its traits (marks, stances, the Balance's window). */
    public TierState traitState() { return traitState; }

    /** 3.39.0: do the anime powers apply to this fight right now? */
    private boolean powered() { return !level().isClientSide && !row.traits().isEmpty() && ArcConfig.tierPowers(); }

    /** A trait hook that throws is logged once per boss and never takes the tick down. */
    private void traitFailed(String hook, RuntimeException e) {
        if (traitWarned) return;
        traitWarned = true;
        LOG.warn("[nusmp] tier trait hook {} of {} failed (further failures are not logged): {}", hook, row.id(), e.toString());
    }

    @Override public ArcCharacter character() { return row.character(); }

    @Override
    protected List<Move> moves() {
        if (book == null) book = TierMoves.build(row);
        return book;
    }

    @Override
    protected void bossTick() {
        super.bossTick();
        if (level().isClientSide) return;
        LIVE.add(getUUID());
        try {
            TierTraits.tick(this);
        } catch (RuntimeException e) {
            traitFailed("tick", e);
        }
    }

    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        traitState.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        LIVE.remove(getUUID());
        traitState.clear();
        super.remove(reason);
    }

    // ================================================================ 3.39.0: the anime powers (TierTraits does the work, every hook is a no-op when the powers are off)

    @Override
    protected float damageScale() {
        float s = super.damageScale();
        if (!powered()) return s;
        try {
            return s * TierTraits.damageMultiplier(this);
        } catch (RuntimeException e) {
            traitFailed("damageScale", e);
            return s;
        }
    }

    @Override
    protected float mitigate(DamageSource source, float amount) {
        float a = super.mitigate(source, amount);
        if (!powered()) return a;
        try {
            return TierTraits.mitigate(this, source, a);
        } catch (RuntimeException e) {
            traitFailed("mitigate", e);
            return a;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!powered()) return super.hurt(source, amount);
        float before = getHealth();
        boolean landed = super.hurt(source, amount);
        float dealt = before - getHealth();
        if (landed && dealt > 0f) {
            try {
                TierTraits.afterHurt(this, source, dealt);
            } catch (RuntimeException e) {
                traitFailed("afterHurt", e);
            }
        }
        return landed;
    }

    @Override
    public boolean strike(LivingEntity victim, float base, boolean magic) {
        if (!powered()) return super.strike(victim, base, magic);
        float b = base;
        boolean blocking = victim instanceof Player p && p.isBlocking();
        try {
            b = TierTraits.strikeBase(this, victim, base);
        } catch (RuntimeException e) {
            traitFailed("strikeBase", e);
        }
        boolean landed = super.strike(victim, b, magic);
        if (landed) {
            try {
                TierTraits.afterStrike(this, victim, blocking);
            } catch (RuntimeException e) {
                traitFailed("afterStrike", e);
            }
        }
        return landed;
    }

    /** A strike with none of the traits' side effects (the Balance's give-back must not lay soul marks or poison). */
    public boolean plainStrike(LivingEntity victim, float base, boolean magic) {
        return super.strike(victim, base, magic);
    }

    /** A few Magic Stones of the tier's size on every kill (the dungeon's own currency); the Charter grimoire is not given by light bosses. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        Item stone = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("nusmp", "magic_stone"));
        if (stone == null || stone == net.minecraft.world.item.Items.AIR) return;
        int max = row.letter().stones();
        int n = Math.max(1, max / 2 + getRandom().nextInt(max / 2 + 1));
        spawnAtLocation(new ItemStack(stone, n));
    }
}
