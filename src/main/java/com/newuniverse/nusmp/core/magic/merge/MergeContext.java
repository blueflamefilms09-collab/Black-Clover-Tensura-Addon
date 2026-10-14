package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.ElementBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.book.WikiBooks;
import com.newuniverse.nusmp.core.magic.profile.MagicProfile;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * "A merged magic is casting right now". While one of its pages runs (the parent's own cast code, called through
 * {@link MergedPages}), this context is set, and three central hooks read it:
 * <ul>
 *   <li>{@code vfx.VfxSpawn.send(level, payload)} calls {@link #adapt}: the effect is redrawn in the parents' shared palette, and the
 *       other parents' own effects of the same kind (cast / field / burst) and the composite layer join it, so the effect is a blend of
 *       the parents' shapes and not one parent's with a tint;</li>
 *   <li>{@code book.GrimoireBook.hurtAs} calls {@link #onHit}: every hit also carries the other parent's hit effect;</li>
 *   <li>{@code book.SpellRuntime} wraps its bolts, zones and delayed tasks with {@link #wrap}, so a bolt that ends a second later is
 *       still part of the merged cast.</li>
 * </ul>
 * Server thread only (all casting runs there). Outside a merged cast every method is a null check and returns.
 */
public final class MergeContext {
    private MergeContext() {}

    /** The running merged cast: the definition, its blend and which parent's page is running. */
    private record Scope(MergedBlend blend, int parent) {}

    private static Scope current;
    private static boolean busy;
    private static long stamp;
    private static int sent;

    /** At most this many companion effects per tick (a volley of bolts must not multiply into a flood). */
    private static final int MAX_COMPANIONS_PER_TICK = 10;

    public static boolean active() { return current != null; }

    /** The merged magic casting now, or null. */
    public static MergedDef currentDef() { Scope s = current; return s == null ? null : s.blend().def; }

    // ---------------------------------------------------------------- entering and leaving
    /** Runs a parent's page cast as part of the merged magic {@code def}; the context is restored afterwards whatever happens. */
    public static boolean cast(MergedDef def, int parent, BooleanSupplier body) {
        Scope previous = current;
        current = new Scope(MergedRegistry.blend(def), parent);
        try {
            return body.getAsBoolean();
        } finally {
            current = previous;
        }
    }

    private static void run(Scope s, Runnable body) {
        Scope previous = current;
        current = s;
        try { body.run(); } finally { current = previous; }
    }

    /** Re-enters the running merged cast when a bolt hits / ends later. Returns the argument itself outside a merged cast. */
    public static SpellRuntime.OnHit wrap(SpellRuntime.OnHit hit) {
        Scope s = current;
        if (s == null || hit == null) return hit;
        return (bolt, target) -> run(s, () -> hit.hit(bolt, target));
    }

    public static SpellRuntime.OnEnd wrap(SpellRuntime.OnEnd end) {
        Scope s = current;
        if (s == null || end == null) return end;
        return (bolt, at) -> run(s, () -> end.end(bolt, at));
    }

    public static IntConsumer wrap(IntConsumer step) {
        Scope s = current;
        if (s == null || step == null) return step;
        return age -> run(s, () -> step.accept(age));
    }

    public static Runnable wrap(Runnable task) {
        Scope s = current;
        if (s == null || task == null) return task;
        return () -> run(s, task);
    }

    // ---------------------------------------------------------------- the second hit effect
    /** Called after every spell hit of the book: a merged cast also applies the other parent's hit effect. */
    public static void onHit(ServerPlayer caster, LivingEntity target) {
        Scope s = current;
        if (s == null || caster == null || target == null || target == caster) return;
        try {
            MergedBlend b = s.blend();
            int rider = s.parent() == 0 ? b.hitSecondaryParent : 0;       // the dominant parent's page carries the secondary; any other parent's page carries the dominant's
            if (rider < 0 || rider == s.parent()) return;
            MagicType t = MagicType.lookup(b.def.parents().get(rider)), own = MagicType.lookup(b.def.parents().get(s.parent()));
            if (t == null || (own != null && own.hit == t.hit)) return;
            ElementBook.Rider r = WikiBooks.rider(t);
            r.apply(target, caster);
        } catch (RuntimeException e) {
            SafeCast.fault("merge-rider", e);
        }
    }

    // ---------------------------------------------------------------- effects
    /** 0 = cast, 1 = field, 2 = burst, -1 = something else. */
    private static int categoryOf(MagicProfile parent, VfxShape shape) {
        if (shape == parent.castShape()) return 0;
        if (shape == parent.fieldShape()) return 1;
        if (shape == parent.burstShape()) return 2;
        return -1;
    }

    /** The wiki layers read WHICH wiki magic it is from the exact palette colour of the payload (WikiMagicLayer.styleForColor): a tinted colour would draw every wiki parent as Air. */
    private static boolean keepsColor(VfxShape shape) {
        return shape == VfxShape.WIKI_MAGIC_CAST || shape == VfxShape.WIKI_MAGIC_FIELD || shape == VfxShape.WIKI_MAGIC_BURST;
    }

    private static VfxShape shapeOf(MagicProfile profile, int category) {
        return category == 0 ? profile.castShape() : category == 1 ? profile.fieldShape() : profile.burstShape();
    }

    /**
     * The hook in {@code VfxSpawn.send}: returns the payload to send. Outside a merged cast, or while this method is sending its own
     * companions, it returns the payload unchanged. Inside one it recolours the effect to the casting parent's tint of the shared
     * palette and, if the effect is one of that parent's three (cast / field / burst), sends every other parent's effect of the same
     * kind plus the composite layer at the same place.
     */
    public static VfxPayload adapt(ServerLevel level, VfxPayload payload) {
        Scope s = current;
        if (s == null || busy || payload == null) return payload;
        try {
            MergedBlend b = s.blend();
            VfxShape shape = payload.shapeType();
            if (shape == VfxShape.MERGED_CAST || shape == VfxShape.MERGED_FIELD || shape == VfxShape.MERGED_BURST || shape == VfxShape.MERGED_AURA) return payload;
            VfxPayload out = new VfxPayload(payload.shape(), payload.from(), payload.to(), keepsColor(shape) ? payload.color() : b.tinted(s.parent()), payload.duration(), payload.power(),
                    payload.followEntity(), payload.seed());
            int cat = categoryOf(MagicProfiles.of(b.def.parents().get(s.parent())), shape);
            if (cat >= 0) companions(level, s, b, cat, payload);
            return out;
        } catch (RuntimeException e) {
            SafeCast.fault("merge-adapt", e);
            return payload;
        }
    }

    private static void companions(ServerLevel level, Scope s, MergedBlend b, int cat, VfxPayload p) {
        long now = level.getGameTime();
        if (now != stamp) { stamp = now; sent = 0; }
        if (sent >= MAX_COMPANIONS_PER_TICK) return;
        busy = true;
        try {
            int n = b.size();
            for (int j = 0; j < n; j++) {
                if (j == s.parent()) continue;
                VfxShape sh = shapeOf(MagicProfiles.of(b.def.parents().get(j)), cat);
                float size = ColorBlend.sizeFor(b.weights[j], n);
                VfxSpawn.send(level, new VfxPayload(sh.ordinal(), p.from(), p.to(), keepsColor(sh) ? b.colors[j] : b.tinted(j), p.duration(), p.power() * size, p.followEntity(),
                        p.seed() ^ (0x9E3779B97F4A7C15L * (j + 1))));
                sent++;
            }
            VfxShape comp = cat == 0 ? VfxShape.MERGED_CAST : cat == 1 ? VfxShape.MERGED_FIELD : VfxShape.MERGED_BURST;
            VfxSpawn.send(level, new VfxPayload(comp.ordinal(), p.from(), p.to(), b.primary, p.duration(), p.power(), p.followEntity(),
                    MergedKeys.seed(b.def.id(), p.seed())));
            sent++;
        } finally {
            busy = false;
        }
    }

    // ---------------------------------------------------------------- the stance
    /** Ticks of the temporary armour layer and stance circle after a merged cast. */
    public static final int STANCE_TICKS = 100;

    /** At the start of a merged cast: the stance circle under the caster (every parent's colour) and the temporary armour layer. */
    public static void stance(ServerPlayer player, MergedDef def) {
        try {
            MergedBlend b = MergedRegistry.blend(def);
            ServerLevel level = player.serverLevel();
            boolean was = busy;
            busy = true;                                 // the stance is not a parent's effect: it is not recoloured or doubled
            try {
                VfxSpawn.send(level, new VfxPayload(VfxShape.MERGED_AURA.ordinal(), player.position(), player.position().add(0, 0.05, 0), b.primary, STANCE_TICKS, 1.0f,
                        player.getId(), MergedKeys.seed(def.id(), level.random.nextLong())));
            } finally {
                busy = was;
            }
            PlayerAuras.set(player, Aura.MERGED, STANCE_TICKS, def.slot());
            MagicProfile profile = MagicProfiles.of(def.id());
            for (int i = 0; i < b.size(); i++)                       // the merged profile hands out the parents' particles in turn
                level.sendParticles(profile.particle(), player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.45, 0.6, 0.45, 0.02);
        } catch (RuntimeException e) {
            SafeCast.fault("merge-stance", e);
        }
    }
}
