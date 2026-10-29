package com.newuniverse.nusmp.arc.tier;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.entity.zogratis.Move;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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

    private final TierRow row;
    private List<Move> book;

    public TierBoss(EntityType<? extends TierBoss> type, Level level, TierRow row) {
        super(type, level);
        this.row = row;
        this.xpReward = 20 + 25 * row.letter().ordinal();
    }

    /** How many light bosses stand in loaded chunks. */
    public static int liveCount() { return LIVE.size(); }

    public TierRow row() { return row; }

    @Override public ArcCharacter character() { return row.character(); }

    @Override
    protected List<Move> moves() {
        if (book == null) book = TierMoves.build(row);
        return book;
    }

    @Override
    protected void bossTick() {
        super.bossTick();
        if (!level().isClientSide) LIVE.add(getUUID());
    }

    @Override
    public void remove(RemovalReason reason) {
        LIVE.remove(getUUID());
        super.remove(reason);
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
