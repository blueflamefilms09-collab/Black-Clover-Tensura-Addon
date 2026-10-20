package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CatastropheRules;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the canon says a tomb collapses when its primary artifact is taken, and that this is a race. This is the runner of that collapse; it reuses the catastrophe clocks of the Crucible
 * ({@link CatastropheRules}: the telegraph of falling masonry, the cap on what one person can take) and adds what a tomb needs: the rooms fall ONE BY ONE, the deepest first ({@link PocketRules#roomFallsAt}),
 * so the way out always runs through rooms that are still standing; a room about to fall shakes and rains dust for five seconds ({@link PocketRules#ROOM_WARN_TICKS}); when it falls everyone still in it
 * is carried to the entry hall with a fifth of their health taken (never lethal), its monsters are removed, its walls and floor and the corridors that led to it are cleared (a standing neighbour's
 * doorway is walled up again, so nobody walks into the void), and the entry hall stays until the end, where the exit rift is. At the end of the clock the tomb throws everyone out and the sweep
 * ({@link PocketClear}) empties the cell. Nothing here can kill: the stones are telegraphed and capped by the catastrophe budget, the fall takes a fifth, and the end is a teleport.
 */
public final class PocketCollapse {
    private static final Logger LOG = LogUtils.getLogger();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int FLAGS = 2 | 16 | 32, BLOCKS_PER_TICK = 6000;

    private final PocketRuntime rt;
    private final int total;
    private final int[] depth;
    private final int maxDepth;
    private final long startedAt;
    private long warned;
    private final ArrayDeque<PocketPlan.Fill> pending = new ArrayDeque<>();
    private PocketPlan.Fill current;
    private long cursor;
    private final List<double[]> strikes = new ArrayList<>();
    private final Map<UUID, Float> taken = new HashMap<>();
    private final Random rnd;
    private final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

    public PocketCollapse(PocketRuntime rt, long startedAt) {
        this.rt = rt;
        this.startedAt = startedAt;
        this.total = CatastropheRules.collapseTotalTicks(rt.def.collapseSeconds());
        this.depth = PocketRules.depthsAll(rt.layout);
        int md = 0;
        for (int d : depth) md = Math.max(md, d);
        this.maxDepth = md;
        this.rnd = new Random(rt.seed ^ 0xC011A95EL);
    }

    public int totalTicks() { return total; }

    public long startedAt() { return startedAt; }

    /** Ticks since the collapse began. */
    public int elapsed(long now) { return (int) Math.max(0, now - startedAt); }

    public boolean finished(long now) { return elapsed(now) >= total; }

    /** The tick (from the start) at which a room falls. */
    public int fallsAt(int roomId) { return PocketRules.roomFallsAt(depth[roomId], maxDepth, total); }

    /** Has this room fallen? */
    public boolean fell(int roomId) { return (rt.fallen & (1L << roomId)) != 0; }

    /** The clearing still to do (blocks are cleared a budget a tick, so a fallen room may be a moment from empty). */
    public boolean busy() { return current != null || !pending.isEmpty(); }

    /** One tick of the collapse (the caller runs it every tick while the tomb is collapsing). */
    public void step(ServerLevel level, PocketRealm.Pocket p, List<ServerPlayer> in, long now) {
        int el = elapsed(now);
        com.newuniverse.nusmp.dungeon.DungeonGuard.begin();         // 1.101.0: a falling room is air over walls, which the Dungeon Guard refuses to everybody but the mod's own builders
        try {
            if (el == 0) start(level, in);
            rooms(level, p, in, el);
            pulses(level, in, el);
            landings(level, in, now);
            clearing(level);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The collapse of {} failed a tick (it goes on): {}", rt.pocketId, ex.toString());
        } finally {
            com.newuniverse.nusmp.dungeon.DungeonGuard.end();
        }
    }

    private void start(ServerLevel level, List<ServerPlayer> in) {
        for (ServerPlayer pl : in) {
            pl.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.collapse.start", "The primary relic is gone. The tomb is coming down: run for the entry hall!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            com.newuniverse.nusmp.labyrinth.mega.rift.RiftService.title(pl, Component.translatableWithFallback("message.nusmp.mega.pocket.collapse.title", "The tomb is collapsing"),
                    Component.translatableWithFallback("message.nusmp.mega.pocket.collapse.sub", "%s seconds. The deepest rooms fall first.", total / 20));
        }
        level.playSound(null, rt.arrival == null ? BlockPos.containing(PocketCell.centreX(rt.slot), PocketPlan.Y0, PocketCell.centreZ()) : rt.arrival, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.AMBIENT, 2.0f, 0.5f);
    }

    // ---------------------------------------------------------------- rooms
    private void rooms(ServerLevel level, PocketRealm.Pocket p, List<ServerPlayer> in, int el) {
        for (PocketLayout.Room r : rt.layout.rooms()) {
            int id = r.id();
            if (id == rt.layout.entryId() || fell(id)) continue;
            int at = fallsAt(id);
            if (PocketRules.roomWarned(el, at)) warn(level, id, in, el);
            if (el >= at) fall(level, id, in);
        }
    }

    private void warn(ServerLevel level, int id, List<ServerPlayer> in, int el) {
        AABB box = rt.roomBox(id);
        if ((warned & (1L << id)) == 0) {
            warned |= 1L << id;
            for (ServerPlayer pl : in)
                if (box.contains(pl.position()))
                    pl.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.collapse.room", "This room is about to fall: get out!").withStyle(ChatFormatting.RED), true);
            level.playSound(null, BlockPos.containing(box.getCenter()), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 3.0f, 0.5f);
        }
        if (el % 4 == 0) {
            double cx = box.getCenter().x, cz = box.getCenter().z;
            Fx.spray(level, FxThemes.DUNGEON_RIFT, FxOld.CAMPFIRE_COSY_SMOKE, cx + (rnd.nextDouble() - 0.5) * (box.maxX - box.minX), box.maxY - 1, cz + (rnd.nextDouble() - 0.5) * (box.maxZ - box.minZ), 1, 0.0, -0.1, 0.0, 0.0);
            Fx.spray(level, FxThemes.DUNGEON_RIFT, FxOld.ASH, cx + (rnd.nextDouble() - 0.5) * (box.maxX - box.minX), box.minY + 2, cz + (rnd.nextDouble() - 0.5) * (box.maxZ - box.minZ), 3, 1.0, 1.0, 1.0, 0.02);
        }
    }

    private void fall(ServerLevel level, int id, List<ServerPlayer> in) {
        rt.fallen |= 1L << id;
        AABB shell = rt.roomShell(id);
        for (ServerPlayer pl : in) if (shell.contains(pl.position())) rescue(level, pl);
        for (Entity e : level.getEntitiesOfClass(Entity.class, shell.inflate(1), en -> !(en instanceof ServerPlayer))) e.discard();
        PocketLayout.Room r = rt.layout.room(id);
        int g = rt.layout.grid(), cx = r.cx(g), cz = r.cz(g), w = r.half() + 1;
        pending.add(new PocketPlan.Fill(cx - w, PocketPlan.Y0 - 4, cz - w, cx + w, PocketPlan.Y0 + r.height() + 4, cz + w, PocketPlan.Mat.AIR));
        for (PocketLayout.Door d : rt.layout.doors()) {
            if (d.a() != id && d.b() != id) continue;
            int[] c = rt.plan.corridorBox(d);
            AABB corridor = new AABB(PocketCell.centreX(rt.slot) + c[0], c[1], PocketCell.centreZ() + c[2], PocketCell.centreX(rt.slot) + c[3] + 1, c[4] + 1, PocketCell.centreZ() + c[5] + 1);
            for (ServerPlayer pl : in) if (corridor.contains(pl.position())) rescue(level, pl);
            pending.add(new PocketPlan.Fill(c[0], c[1], c[2], c[3], c[4], c[5], PocketPlan.Mat.AIR));
            int other = d.a() == id ? d.b() : d.a();
            if (!fell(other)) {                                                  // the standing neighbour's doorway is walled up again
                int[] w2 = rt.plan.doorway(d, other);
                pending.add(new PocketPlan.Fill(w2[0], w2[1], w2[2], w2[3], w2[4], w2[5], PocketPlan.Mat.WALL));
            }
        }
        BlockPos centre = BlockPos.containing(rt.roomBox(id).getCenter());
        level.playSound(null, centre, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 4.0f, 0.5f);
        Fx.spray(level, FxThemes.DUNGEON_RIFT, FxOld.EXPLOSION_EMITTER, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5, 2, 3.0, 2.0, 3.0, 0.0);
    }

    /** Carries a delver caught in a falling room to the entry hall, and takes a fifth of their health (never the last of it). */
    private void rescue(ServerLevel level, ServerPlayer pl) {
        if (rt.arrival == null) return;
        float hurt = PocketRules.trapDamage(pl.getMaxHealth(), pl.getHealth(), PocketRules.FALL_SHARE);
        PocketRealm.move(pl, level, rt.arrival.getX() + 0.5, rt.arrival.getY(), rt.arrival.getZ() + 0.5, pl.getYRot(), 0f);
        pl.resetFallDistance();
        if (hurt > 0f && !pl.isCreative() && !pl.isSpectator()) pl.hurt(level.damageSources().magic(), hurt);
        pl.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.collapse.rescued", "The floor gives way. The mana of the tomb carries you to the entry hall.").withStyle(ChatFormatting.GOLD), true);
        rt.rescued.put(pl.getUUID(), level.getGameTime());
    }

    /** Clears the queued boxes a budget of blocks a tick. */
    private void clearing(ServerLevel level) {
        int cx0 = PocketCell.centreX(rt.slot), cz0 = PocketCell.centreZ(), left = BLOCKS_PER_TICK;
        while (left > 0) {
            if (current == null) {
                current = pending.poll();
                cursor = 0;
                if (current == null) return;
            }
            long vol = current.volume();
            int w = current.x1() - current.x0() + 1, d = current.z1() - current.z0() + 1;
            BlockState st = current.mat() == PocketPlan.Mat.AIR ? AIR : rt.palette.state(current.mat());
            while (cursor < vol && left > 0) {
                int dx = (int) (cursor % w), dz = (int) ((cursor / w) % d), dy = (int) (cursor / ((long) w * d));
                m.set(cx0 + current.x0() + dx, current.y0() + dy, cz0 + current.z0() + dz);
                if (current.mat() == PocketPlan.Mat.AIR ? !level.getBlockState(m).isAir() : true) level.setBlock(m, st, FLAGS);
                cursor++;
                left--;
            }
            if (cursor >= vol) current = null;
        }
    }

    // ---------------------------------------------------------------- stones
    /** A pulse: for each delver in a room that is about to fall, a ring of dust marks where a stone will land, and it lands a moment later. */
    private void pulses(ServerLevel level, List<ServerPlayer> in, int el) {
        if (!CatastropheRules.collapsePulseAt(el, total)) return;
        for (ServerPlayer pl : in) {
            int room = rt.roomAt(pl.getX(), pl.getZ());
            if (room < 0 || room == rt.layout.entryId() || fell(room) || el < fallsAt(room) - PocketRules.ROOM_WARN_TICKS * 3) continue;
            double x = pl.getX() + (rnd.nextDouble() - 0.5) * 2.0, z = pl.getZ() + (rnd.nextDouble() - 0.5) * 2.0;
            strikes.add(new double[]{el + CatastropheRules.DEBRIS_TELEGRAPH_TICKS, x, pl.getY(), z});
            Fx.spray(level, FxThemes.DUNGEON_RIFT, FxOld.CRIT, x, pl.getY() + 0.1, z, 12, 0.6, 0.0, 0.6, 0.0);
            for (int i = 0; i < CatastropheRules.DEBRIS_DECOR_PER_PULSE; i++)
                Fx.spray(level, FxThemes.DUNGEON_RIFT, FxOld.ASH, x + (rnd.nextDouble() - 0.5) * 6, pl.getY() + 4, z + (rnd.nextDouble() - 0.5) * 6, 6, 0.3, 0.8, 0.3, 0.0);
        }
    }

    private void landings(ServerLevel level, List<ServerPlayer> in, long now) {
        int el = elapsed(now);
        for (int i = strikes.size() - 1; i >= 0; i--) {
            double[] s = strikes.get(i);
            if (el < s[0]) continue;
            strikes.remove(i);
            Fx.spray(level, FxThemes.DUNGEON_RIFT, FxOld.EXPLOSION, s[1], s[2] + 0.2, s[3], 1, 0, 0, 0, 0);
            level.playSound(null, BlockPos.containing(s[1], s[2], s[3]), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5f, 0.6f);
            for (ServerPlayer pl : in) {
                if (!CatastropheRules.debrisHits(pl.getX() - s[1], pl.getY() - s[2], pl.getZ() - s[3]) || pl.isCreative() || pl.isSpectator()) continue;
                float already = taken.getOrDefault(pl.getUUID(), 0f);
                float allow = CatastropheRules.collapseAllowance(pl.getMaxHealth(), already);
                float want = Math.min(allow, PocketRules.trapDamage(pl.getMaxHealth(), pl.getHealth(), 0.08));
                if (want <= 0f) continue;
                taken.put(pl.getUUID(), already + want);
                pl.hurt(level.damageSources().magic(), want);
            }
        }
    }

    /** Removes the monsters left in rooms that fell (called by the owner once; the room sweep does it too) - kept for the admin command. */
    public static int purgeMobs(ServerLevel level, int slot) {
        int n = 0;
        for (Mob e : level.getEntitiesOfClass(Mob.class, PocketCell.bounds(slot))) {
            e.discard();
            n++;
        }
        return n;
    }
}
