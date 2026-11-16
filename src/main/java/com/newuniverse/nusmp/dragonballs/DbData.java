package com.newuniverse.nusmp.dragonballs;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/**
 * The world's memory of the Dragon Balls, saved with the overworld: per set the stone lock (an epoch-millisecond time, so it runs in REAL time even while the server is off), the generation of the last
 * scatter, whether a ritual is running (so a crash during a summoning scatters the balls again at the next start instead of losing them), and per ball its last known place: the registry the Dragon Radar
 * reads. At most 2 sets x 8 balls = 16 rows, whatever happens.
 */
public final class DbData extends SavedData {
    /** Where a ball was last seen. {@code pending}: the target is chosen but the chunk has not been loaded yet, so there is no item in the world (it is spawned the first time a player loads the place). */
    public static final class Ball {
        public String dim = "minecraft:overworld";
        public double x, y, z;
        public boolean pending;
        /** The ball is (still) a stone: set when the ball is made while its set is locked, cleared by the sweep once the lock has run out. */
        public boolean stone;
        /** The item entity of this ball while it lies on the ground (so the stone mark can be lifted); null when it is carried or unknown. */
        public UUID entity;
    }

    public final long[] lockedUntil = new long[BallSet.values().length];
    public final int[] gen = new int[BallSet.values().length];
    public final boolean[] active = new boolean[BallSet.values().length];
    public final Ball[][] balls = new Ball[BallSet.values().length][DbRules.MAX_COUNT];

    static DbData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DbData::new, DbData::load, null), "nusmp_dragonballs");
    }

    private static DbData load(CompoundTag tag, HolderLookup.Provider provider) {
        DbData d = new DbData();
        for (BallSet s : BallSet.values()) {
            CompoundTag t = tag.getCompound(s.id());
            d.lockedUntil[s.ordinal()] = t.getLong("lock");
            d.gen[s.ordinal()] = t.getInt("gen");
            d.active[s.ordinal()] = t.getBoolean("active");
            ListTag list = t.getList("balls", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && i < DbRules.MAX_COUNT; i++) {
                CompoundTag b = list.getCompound(i);
                if (!b.getBoolean("set")) continue;
                Ball ball = new Ball();
                ball.dim = b.getString("dim");
                ball.x = b.getDouble("x");
                ball.y = b.getDouble("y");
                ball.z = b.getDouble("z");
                ball.pending = b.getBoolean("pending");
                ball.stone = b.getBoolean("stone");
                if (b.hasUUID("entity")) ball.entity = b.getUUID("entity");
                d.balls[s.ordinal()][i] = ball;
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (BallSet s : BallSet.values()) {
            CompoundTag t = new CompoundTag();
            t.putLong("lock", lockedUntil[s.ordinal()]);
            t.putInt("gen", gen[s.ordinal()]);
            t.putBoolean("active", active[s.ordinal()]);
            ListTag list = new ListTag();
            for (int i = 0; i < DbRules.MAX_COUNT; i++) {
                CompoundTag b = new CompoundTag();
                Ball ball = balls[s.ordinal()][i];
                if (ball != null) {
                    b.putBoolean("set", true);
                    b.putString("dim", ball.dim);
                    b.putDouble("x", ball.x);
                    b.putDouble("y", ball.y);
                    b.putDouble("z", ball.z);
                    b.putBoolean("pending", ball.pending);
                    b.putBoolean("stone", ball.stone);
                    if (ball.entity != null) b.putUUID("entity", ball.entity);
                }
                list.add(b);
            }
            t.put("balls", list);
            tag.put(s.id(), t);
        }
        return tag;
    }
}
