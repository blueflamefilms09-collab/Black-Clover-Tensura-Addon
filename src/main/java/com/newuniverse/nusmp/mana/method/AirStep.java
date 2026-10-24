package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Air Step Rune: for a few seconds an invisible platform of vanilla {@code BARRIER} blocks is kept under the caster's feet while they are in the air, so they walk on air; sneaking drops
 * them through. The rules are {@link MobilityRules}; this class is the ledger and the tick, built on the same idea as {@link TrueBlocks}: every block placed is written down and taken back,
 * and a block is only removed if it is still the barrier that was placed.
 *
 * <ul>
 *   <li><b>Bounded</b>: one session per player, at most {@link MobilityRules#MAX_SESSIONS} on the server, at most {@link MobilityRules#MAX_CELLS_PER_CASTER} blocks per caster (the four
 *       cells under the feet), so at most 64 blocks in the world; per session per tick at most four block reads and one write.</li>
 *   <li><b>Safe</b>: only into air cells, only loaded chunks (never loaded), only where the caster {@code mayInteract} (spawn protection, world border), never in the Labyrinth or a world
 *       the Dungeon Guard holds.</li>
 *   <li><b>Clean</b>: a block nobody needs for {@link MobilityRules#CELL_TTL} ticks goes; all of a caster's blocks go when the duration ends, on logout, death, dimension change and when the
 *       server stops. Landing on the platform does not hurt (the fall is reset while a block of the platform is under the feet).</li>
 *   <li><b>Idle</b>: nothing runs while no one walks on air.</li>
 * </ul>
 */
public final class AirStep {
    private AirStep() {}

    private static final class Cell {
        final BlockPos pos;
        long lastNeeded;

        Cell(BlockPos pos, long now) {
            this.pos = pos;
            this.lastNeeded = now;
        }
    }

    private static final class Session {
        final UUID owner;
        final ServerPlayer player;
        final ServerLevel level;
        long until;
        final List<Cell> cells = new ArrayList<>(MobilityRules.MAX_CELLS_PER_CASTER);

        Session(ServerPlayer player, long until) {
            this.owner = player.getUUID();
            this.player = player;
            this.level = player.serverLevel();
            this.until = until;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final PerfProbe.Section PROBE = PerfProbe.section("s.mana.airstep");
    private static final int[] FOOT = new int[12];

    public static int sessions() { return SESSIONS.size(); }

    public static int cellsOf(UUID owner) {
        Session s = SESSIONS.get(owner);
        return s == null ? 0 : s.cells.size();
    }

    /** Every block standing for any caster (at most 16 sessions x 4 blocks). */
    public static int globalCells() {
        int n = 0;
        for (Session s : SESSIONS.values()) n += s.cells.size();
        return n;
    }

    public static List<BlockPos> positionsOf(UUID owner) {
        List<BlockPos> out = new ArrayList<>();
        Session s = SESSIONS.get(owner);
        if (s != null) for (Cell c : s.cells) out.add(c.pos);
        return out;
    }

    // ---------------------------------------------------------------- the cast
    /** Starts (or extends) a caster's platform for {@code seconds}; false, with a message, when it cannot be. */
    public static boolean start(ServerPlayer p, int seconds, long now) {
        if (!TrueBlocks.levelAllows(p.serverLevel())) {
            GrimoireBook.fail(p, "The air does not hold a rune here.");
            return false;
        }
        Session s = SESSIONS.get(p.getUUID());
        if (s == null && SESSIONS.size() >= MobilityRules.MAX_SESSIONS) {
            GrimoireBook.fail(p, "The mana of this world is busy with other runes. Try again in a moment.");
            return false;
        }
        long until = now + MobilityRules.seconds(seconds) * 20L;
        if (s == null || s.level != p.serverLevel()) {
            if (s != null) end(s);
            s = new Session(p, until);
            SESSIONS.put(p.getUUID(), s);
        } else {
            s.until = until;
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, 1.5f);
        p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_method.air_step", "Air Step Rune: walk on air for %s s (sneak to drop)", MobilityRules.seconds(seconds)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) return;
        long t0 = PerfProbe.begin();
        try {
            tickAll(event.getServer().overworld().getGameTime());                  // 3.6.0: the world clock, the one start() writes (the tick count restarts at a server start and fell behind it)
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    /** One pass over every session at tick {@code now} (public for the self-test, which advances time by hand). */
    public static void tickAll(long now) {
        for (Iterator<Session> it = SESSIONS.values().iterator(); it.hasNext(); ) {
            Session s = it.next();
            ServerPlayer p = s.player;
            if (p.isRemoved() || !p.isAlive() || p.level() != s.level || !ManaMethodConfig.mobility()) { end(s); it.remove(); continue; }
            if (now >= s.until) {
                end(s);
                it.remove();
                p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_method.air_step_end", "The Air Step Rune fades.").withStyle(ChatFormatting.GRAY), true);
                continue;
            }
            step(s, now);
        }
    }

    private static void step(Session s, long now) {
        ServerPlayer p = s.player;
        ServerLevel level = s.level;
        int n = MobilityRules.footprint(p.getX(), p.getY(), p.getZ(), FOOT);
        boolean own = false;
        for (int k = 0; k < n && !own; k++) own = find(s, FOOT[k * 3], FOOT[k * 3 + 1], FOOT[k * 3 + 2]) != null;
        switch (MobilityRules.decide(p.isShiftKeyDown(), p.onGround(), own)) {
            case DROP -> {
                for (int k = 0; k < n; k++) {
                    Cell c = find(s, FOOT[k * 3], FOOT[k * 3 + 1], FOOT[k * 3 + 2]);
                    if (c != null) { remove(level, c); s.cells.remove(c); }
                }
            }
            case PLACE, REFRESH -> {
                boolean place = !p.onGround();
                boolean held = false;
                for (int k = 0; k < n; k++) {
                    int x = FOOT[k * 3], y = FOOT[k * 3 + 1], z = FOOT[k * 3 + 2];
                    Cell c = find(s, x, y, z);
                    if (c != null) { c.lastNeeded = now; held = true; continue; }
                    if (place && place(s, level, p, new BlockPos(x, y, z), now)) held = true;
                }
                if (held && place) p.fallDistance = 0f;                                     // landing on the platform does not hurt
            }
            case NONE -> { }
        }
        for (Iterator<Cell> it = s.cells.iterator(); it.hasNext(); ) {                          // blocks nobody needs any more go
            Cell c = it.next();
            if (MobilityRules.cellExpired(now, c.lastNeeded)) { remove(level, c); it.remove(); }
        }
    }

    private static Cell find(Session s, int x, int y, int z) {
        for (Cell c : s.cells) if (c.pos.getX() == x && c.pos.getY() == y && c.pos.getZ() == z) return c;
        return null;
    }

    /** Places one barrier into an air cell under the caster; false if it may not (not loaded, not air, not the caster's to build in, over the caps). */
    private static boolean place(Session s, ServerLevel level, ServerPlayer p, BlockPos pos, long now) {
        if (!MobilityRules.cellAllowed(s.cells.size(), globalCells())) {
            if (s.cells.isEmpty()) return false;
            Cell oldest = s.cells.get(0);                                                      // the platform follows the feet: the oldest block makes room
            remove(level, oldest);
            s.cells.remove(0);
        }
        if (!level.isLoaded(pos) || !level.mayInteract(p, pos)) return false;
        BlockState now0 = level.getBlockState(pos);
        if (!now0.isAir()) return false;
        if (!level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 2)) return false;
        s.cells.add(new Cell(pos.immutable(), now));
        return true;
    }

    /** Takes a block back, but only if it is still the barrier that was placed. */
    private static void remove(ServerLevel level, Cell c) {
        if (level.isLoaded(c.pos) && level.getBlockState(c.pos).is(Blocks.BARRIER)) DungeonGuard.internal(() -> level.removeBlock(c.pos, false));
    }

    private static void end(Session s) {
        for (Cell c : s.cells) remove(s.level, c);
        s.cells.clear();
    }

    // ---------------------------------------------------------------- clean-up
    public static void cancelOwner(UUID owner) {
        Session s = SESSIONS.remove(owner);
        if (s != null) end(s);
    }

    public static void clearAll() {
        for (Session s : SESSIONS.values()) end(s);
        SESSIONS.clear();
    }

    public static void onDeath(LivingDeathEvent e) {
        if (!SESSIONS.isEmpty() && e.getEntity() instanceof ServerPlayer p) cancelOwner(p.getUUID());
    }

    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!SESSIONS.isEmpty() && e.getEntity() instanceof ServerPlayer p) cancelOwner(p.getUUID());
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!SESSIONS.isEmpty() && e.getEntity() instanceof ServerPlayer p) cancelOwner(p.getUUID());
    }

    public static void onServerStopping(MinecraftServer server) { clearAll(); }
}
