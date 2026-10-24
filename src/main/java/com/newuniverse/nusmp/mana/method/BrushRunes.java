package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.perf.PerfProbe;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The Brush Rune ledger (3.6.0): the rune arrays Rill paints. The rules and numbers are {@link BrushRules}; this class keeps the list of painted arrays on the server.
 *
 * <p>An array is NOT an entity and NOT a block: it is a small record in a list (nothing is saved, nothing is placed in the world, no chunk is loaded). Each one:
 * <ul>
 *   <li>is painted where the caster looks (a block face within {@value BrushRules#REACH} blocks), only where the caster {@code mayInteract} (spawn protection, world border), never in the Labyrinth
 *       or a world the Dungeon Guard holds, only in a loaded chunk, only on a face whose front cell is free;</li>
 *   <li>counts against {@code brushPerCaster} (default 3) and {@code brushGlobal} (default 24) and keeps {@value BrushRules#MIN_SPACING} blocks from the caster's other arrays;</li>
 *   <li>lives {@code brushSeconds} x 0.5..1 and then fades; goes at once when its caster dies, logs out, changes dimension, the server stops or the setting is switched off;</li>
 *   <li>looks around every {@value BrushRules#CHECK_EVERY} ticks (staggered by id) with ONE entity query of a small box, and all arrays together may do {@value BrushRules#QUERIES_PER_TICK} queries
 *       per tick ({@link PerfBudget}); a foe inside the trigger fires ONE bolt through {@link RuneManager#beginAt} (credited to the caster, never hurting the caster or an ally), and the array is spent.
 *       When the rune caps are full the array simply keeps waiting.</li>
 * </ul>
 * Worst case per server tick: 24 arrays, at most 8 small queries, no block access. Packets: one when painted, one when it ends early (fired, cleaned up); an array that simply runs out is faded by
 * the client itself (it was told the life span). Time is the WORLD clock ({@code getGameTime}) everywhere.
 */
public final class BrushRunes {
    private BrushRunes() {}

    private static final class Paint {
        final int id;
        final UUID owner;
        final ServerPlayer player;
        final ServerLevel level;
        final GrimoireBook book;
        final ManasSkillInstance inst;
        final int mode;
        final RuneSpec spec;
        final Charge charge;
        final double cx, cy, cz, nx, ny, nz, radius;
        final long born, expires;
        final AABB box;

        Paint(int id, ServerPlayer p, GrimoireBook book, ManasSkillInstance inst, int mode, RuneSpec spec, Charge charge, Vec3 c, double[] n, double radius, long born, long expires) {
            this.id = id;
            this.owner = p.getUUID();
            this.player = p;
            this.level = p.serverLevel();
            this.book = book;
            this.inst = inst;
            this.mode = mode;
            this.spec = spec;
            this.charge = charge;
            this.cx = c.x;
            this.cy = c.y;
            this.cz = c.z;
            this.nx = n[0];
            this.ny = n[1];
            this.nz = n[2];
            this.radius = radius;
            this.born = born;
            this.expires = expires;
            this.box = new AABB(lo(c.x, n[0], radius), lo(c.y, n[1], radius), lo(c.z, n[2], radius), hi(c.x, n[0], radius), hi(c.y, n[1], radius), hi(c.z, n[2], radius));
        }

        /** The trigger box on one axis: across the surface the radius, along the normal from {@link BrushRules#REACH_IN} behind to {@link BrushRules#REACH_OUT} in front. */
        private static double lo(double c, double n, double r) { return Math.abs(n) < 0.5 ? c - r : (n > 0 ? c - BrushRules.REACH_IN : c - BrushRules.REACH_OUT); }

        private static double hi(double c, double n, double r) { return Math.abs(n) < 0.5 ? c + r : (n > 0 ? c + BrushRules.REACH_OUT : c + BrushRules.REACH_IN); }
    }

    private static final List<Paint> PAINTS = new ArrayList<>();
    private static final PerfBudget QUERIES = new PerfBudget("mana-method-brush", BrushRules.QUERIES_PER_TICK);
    private static final PerfProbe.Section PROBE = PerfProbe.section("s.mana.brush");
    private static int nextId = 1;

    public static int count() { return PAINTS.size(); }

    public static int countOf(UUID owner) {
        int n = 0;
        for (Paint p : PAINTS) if (p.owner.equals(owner)) n++;
        return n;
    }

    // ---------------------------------------------------------------- the cast
    /**
     * Paints one array where {@code p} looks. {@code spec} is the rune that is painted (its element and Pursue / Grow mods). False, with a message, when it cannot be: nothing in reach, a place
     * the caster may not build in, the caps are full, too near another array.
     */
    public static boolean paint(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int mode, RuneSpec spec, Charge charge, long now) {
        ServerLevel level = p.serverLevel();
        if (!TrueBlocks.levelAllows(level)) {
            GrimoireBook.fail(p, "The brush does not take here.");
            return false;
        }
        int own = countOf(p.getUUID());
        if (!BrushRules.allowed(own, PAINTS.size(), ManaMethodConfig.brushPerCaster(), ManaMethodConfig.brushGlobal())) {
            GrimoireBook.fail(p, own >= BrushRules.clampPerCaster(ManaMethodConfig.brushPerCaster()) ? "Your painted runes are still waiting (" + BrushRules.clampPerCaster(ManaMethodConfig.brushPerCaster()) + " at most)." : "The mana of this world is busy with other runes. Try again in a moment.");
            return false;
        }
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().normalize().scale(BrushRules.REACH));
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            GrimoireBook.fail(p, "Look at a surface within " + (int) BrushRules.REACH + " blocks to paint on it.");
            return false;
        }
        BlockPos pos = bhr.getBlockPos();
        Direction face = bhr.getDirection();
        BlockPos front = pos.relative(face);
        if (!level.isLoaded(pos) || !level.isLoaded(front) || !level.mayInteract(p, pos) || !level.getWorldBorder().isWithinBounds(front)) {
            GrimoireBook.fail(p, "You may not paint here.");
            return false;
        }
        if (!level.getBlockState(front).getCollisionShape(level, front).isEmpty()) {
            GrimoireBook.fail(p, "There is no room in front of that surface for a rune.");
            return false;
        }
        double[] n = new double[3];
        BrushRules.normal(face.get3DDataValue(), n);
        Vec3 loc = bhr.getLocation();
        Vec3 c = new Vec3(BrushRules.lifted(loc.x, n[0], 0.03), BrushRules.lifted(loc.y, n[1], 0.03), BrushRules.lifted(loc.z, n[2], 0.03));
        for (Paint o : PAINTS) {
            if (o.owner.equals(p.getUUID()) && o.level == level && BrushRules.tooClose(c.x, c.y, c.z, o.cx, o.cy, o.cz)) {
                GrimoireBook.fail(p, "Too close to another of your painted runes.");
                return false;
            }
        }
        double radius = BrushRules.radius(charge);
        int life = BrushRules.lifeTicks(charge, ManaMethodConfig.brushSeconds());
        Paint a = new Paint(nextId(), p, book, inst, mode, spec, charge, c, n, radius, now, now + life);
        PAINTS.add(a);
        RuneNet.sendBrush(level, a.id, RuneNet.PHASE_APPEAR, face.get3DDataValue(), c, p.getYRot(), radius, spec.pack(), life);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, 1.4f);
        p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_method.brush", "Brush Rune: the array is painted (%s s)", Math.round(life / 20.0)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    private static int nextId() {
        int id = nextId;
        nextId = nextId >= 0x3FFFFFFF ? 1 : nextId + 1;
        return id;
    }

    // ---------------------------------------------------------------- the tick
    /** The sweep: nothing while no array is painted. {@code now} is the world clock. */
    public static void tick(MinecraftServer server, long now) {
        if (PAINTS.isEmpty()) return;
        long t0 = PerfProbe.begin();
        try {
            sweep(now);
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    /** One pass over the arrays at world tick {@code now} (public for the self-test, which advances time by hand). */
    public static void sweep(long now) {
        boolean on = ManaMethodConfig.brushRunes() && ManaMethodConfig.enabled();
        for (Iterator<Paint> it = PAINTS.iterator(); it.hasNext(); ) {
            Paint a = it.next();
            ServerPlayer p = a.player;
            if (!on || p.isRemoved() || !p.isAlive() || p.level() != a.level) { gone(a, p.isRemoved()); it.remove(); continue; }
            if (!BrushRules.alive(now, a.expires)) { it.remove(); continue; }           // the client fades it by its own clock (it was told the life span)
            if (!BrushRules.armed(now, a.born) || !BrushRules.due(now, a.id)) continue;
            if (!QUERIES.tryTake(now, 1)) continue;                                      // the budget of the tick is spent: the next look is four ticks away
            boolean[] spent = {false};
            boolean ok = SafeCast.tick("mana-method-brush", () -> spent[0] = look(a, now));
            if (!ok || spent[0]) it.remove();
        }
    }

    /** One look of one array: ONE entity query of the trigger box; fires when a foe is inside. True when the array is spent. */
    private static boolean look(Paint a, long now) {
        ServerPlayer p = a.player;
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : a.level.getEntitiesOfClass(LivingEntity.class, a.box, e -> e.isAlive() && !e.isSpectator() && Foes.isFoe(p, e))) {
            AABB b = e.getBoundingBox();
            double px = BrushRules.nearest(a.cx, b.minX, b.maxX), py = BrushRules.nearest(a.cy, b.minY, b.maxY), pz = BrushRules.nearest(a.cz, b.minZ, b.maxZ);
            if (!BrushRules.triggered(a.cx, a.cy, a.cz, a.nx, a.ny, a.nz, px, py, pz, a.radius)) continue;
            double d = (px - a.cx) * (px - a.cx) + (py - a.cy) * (py - a.cy) + (pz - a.cz) * (pz - a.cz);
            if (d < bestD) { bestD = d; best = e; }
        }
        if (best == null) return false;
        Vec3 out = new Vec3(a.cx + a.nx * 0.35, a.cy + a.ny * 0.35, a.cz + a.nz * 0.35);
        Vec3 dir = best.getBoundingBox().getCenter().subtract(out);
        if (RuneManager.beginAt(a.book, a.inst, p, a.mode, a.spec, out, dir, now, a.charge) != RuneManager.Begin.OK) return false;       // the rune caps are full: it waits
        RuneNet.sendBrush(a.level, a.id, RuneNet.PHASE_FIRED, 0, new Vec3(a.cx, a.cy, a.cz), 0f, a.radius, a.spec.pack(), 0);
        a.level.playSound(null, BlockPos.containing(a.cx, a.cy, a.cz), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.6f);
        return true;
    }

    private static void gone(Paint a, boolean playerRemoved) {
        RuneNet.sendBrush(a.level, a.id, RuneNet.PHASE_GONE, 0, new Vec3(a.cx, a.cy, a.cz), 0f, a.radius, a.spec.pack(), 0);
    }

    // ---------------------------------------------------------------- clean-up
    public static void cancelOwner(UUID owner) {
        if (PAINTS.isEmpty()) return;
        for (Iterator<Paint> it = PAINTS.iterator(); it.hasNext(); ) {
            Paint a = it.next();
            if (!a.owner.equals(owner)) continue;
            gone(a, a.player.isRemoved());
            it.remove();
        }
    }

    public static void clearAll() { PAINTS.clear(); }
}
