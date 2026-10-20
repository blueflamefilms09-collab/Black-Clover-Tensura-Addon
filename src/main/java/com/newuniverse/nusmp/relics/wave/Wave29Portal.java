package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the Pocket Portal is two things at once: an orb that remembers the place it was "brewed" (its home, kept on the stack) and a ring on the ground that
 * carries every creature stepping into it to that home for two rounds. This class owns the home marking, the smashing, and the sweep that runs the open
 * portals; the numbers are in {@link Wave29Rules}.
 * <p>
 * Portals are held in memory only (a portal lives 12 s) and are all dropped when the owner leaves and when the server stops. The orb never edits a block,
 * so the Labyrinth's guard has nothing to refuse; home and portal must be in the same dimension.
 */
final class Wave29Portal {
    private Wave29Portal() {}

    static final String K_DIM = "w29_home_dim", K_X = "w29_home_x", K_Y = "w29_home_y", K_Z = "w29_home_z", K_ARRIVED = "arrived_until";

    /** An open portal: where, to where, whose, until when. */
    private record Open(UUID owner, ResourceKey<Level> dim, Vec3 at, Vec3 dest, long until) {}

    private static final List<Open> OPEN = new ArrayList<>();
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));

    // ================================================================ the home of an orb
    /** Where this orb was brewed, or null if it has not been marked. */
    private static Vec3 home(ItemStack stack, String[] dimOut) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag t = cd.copyTag();
        if (!t.contains(K_DIM)) return null;
        dimOut[0] = t.getString(K_DIM);
        return new Vec3(t.getDouble(K_X), t.getDouble(K_Y), t.getDouble(K_Z));
    }

    private static void mark(ItemStack stack, ServerPlayer p) {
        String dim = p.level().dimension().location().toString();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> {
            t.putString(K_DIM, dim);
            t.putDouble(K_X, p.getX());
            t.putDouble(K_Y, p.getY());
            t.putDouble(K_Z, p.getZ());
        });
    }

    // ================================================================ the act
    /**
     * Pocket Portal. Sneak while you drink to mark the spot you stand on as the circle the orb was brewed in (one orb of the stack takes the mark, the
     * others stay blank). An orb that has never been marked marks itself the first time it is used. A marked orb, drunk without sneaking, is smashed
     * where you look (up to twelve blocks): a ring of two blocks radius opens and stays two rounds; every creature that steps in is carried to within a
     * block of the home. The orb is used up when it opens a portal (marking hands back a marked orb for the blank one).
     */
    static void smash(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = c.stack();
        String[] dim = new String[1];
        Vec3 home = home(stack, dim);
        if (c.sneaking() || home == null) {
            marked(p, stack, home != null);
            return;
        }
        ServerLevel sl = p.serverLevel();
        if (!sl.dimension().location().toString().equals(dim[0])) {
            Wave29Util.keep(p, stack);
            Wave29Util.tell(p, "Pocket Portal: the circle this orb was brewed in lies in another world; the portal will not open here. The orb is kept.", ChatFormatting.GRAY);
            return;
        }
        Vec3 at = target(p);
        OPEN.removeIf(o -> o.owner().equals(p.getUUID()));
        while (OPEN.size() >= Wave29Rules.PORTAL_MAX) OPEN.remove(0);
        OPEN.add(new Open(p.getUUID(), sl.dimension(), at, home, sl.getGameTime() + Wave29Rules.PORTAL_TICKS));
        Fx.burst(sl, FxThemes.RELIC_WAVE_29, at.add(0, 0.8, 0), FxTone.LIGHT, 12, 1.2);
        sl.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
        sl.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.6f, 1.4f);
        Wave29Util.say(p, "Pocket Portal: the orb shatters and a ring of swirling light opens for twelve seconds. Whoever steps in comes out at the circle it was brewed in.");
    }

    /** Marking: the blank orb the engine is about to use up is replaced by a marked one (creative marks in place). */
    private static void marked(ServerPlayer p, ItemStack stack, boolean wasMarked) {
        if (p.isCreative()) {
            mark(stack, p);
        } else {
            ItemStack one = stack.copyWithCount(1);
            mark(one, p);
            if (!p.getInventory().add(one)) p.drop(one, false);
        }
        Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_29, p.position().add(0, 0.2, 0), FxTone.LIGHT, 8, 0.8);
        Wave29Util.say(p, wasMarked ? "Pocket Portal: the orb forgets its old circle and is brewed anew in this spot. Drink it without sneaking to smash it."
                : "Pocket Portal: a circle is drawn here and the orb remembers it. Drink it without sneaking, aimed at the ground, to smash it.");
    }

    /** Where the ring opens: the block you look at within twelve blocks, otherwise three blocks ahead. */
    private static Vec3 target(ServerPlayer p) {
        HitResult hit = p.pick(Wave29Rules.PORTAL_REACH, 1.0f, false);
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
            Vec3 n = new Vec3(b.getDirection().getStepX(), b.getDirection().getStepY(), b.getDirection().getStepZ());
            return b.getLocation().add(n.scale(0.15));
        }
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        return p.position().add(dir.scale(3.0));
    }

    // ================================================================ the sweep
    /** Server tick: every five ticks each open portal swirls and carries whoever stands in it. */
    static void tick(MinecraftServer server) {
        if (OPEN.isEmpty() || server.getTickCount() % Wave29Rules.PORTAL_SWEEP != 1) return;
        for (int i = OPEN.size() - 1; i >= 0; i--) {
            Open o = OPEN.get(i);
            ServerLevel sl = server.getLevel(o.dim());
            if (sl == null || sl.getGameTime() >= o.until()) {
                OPEN.remove(i);
                if (sl != null) Fx.burst(sl, FxThemes.RELIC_WAVE_29, o.at().add(0, 0.5, 0), FxTone.LIGHT, 6, 0.8);
                continue;
            }
            swirl(sl, o);
            carry(sl, o);
        }
    }

    private static void swirl(ServerLevel sl, Open o) {
        double a = (sl.getGameTime() % 40) / 40.0 * Math.PI * 2;
        double r = Wave29Rules.PORTAL_RADIUS;
        Fx.spray(sl, FxThemes.RELIC_WAVE_29, FxTone.LIGHT, o.at().x + Math.cos(a) * r, o.at().y + 0.4, o.at().z + Math.sin(a) * r, 2, 0.1, 0.5, 0.1, 0.02);
        Fx.spray(sl, FxThemes.RELIC_WAVE_29, FxTone.LIGHT, o.at().x - Math.cos(a) * r, o.at().y + 1.0, o.at().z - Math.sin(a) * r, 2, 0.1, 0.5, 0.1, 0.02);
        Fx.spray(sl, FxThemes.RELIC_WAVE_29, FxTone.DARK, o.at().x, o.at().y + 0.8, o.at().z, 2, 0.3, 0.6, 0.3, 0.01);
    }

    private static void carry(ServerLevel sl, Open o) {
        Vec3 c = o.at();
        double r = Wave29Rules.PORTAL_RADIUS;
        AABB box = new AABB(c.x - r, c.y - Wave29Rules.PORTAL_BELOW, c.z - r, c.x + r, c.y + Wave29Rules.PORTAL_ABOVE, c.z + r);
        long now = sl.getGameTime();
        int carried = 0;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (carried >= Wave29Rules.PORTAL_CARRY) break;
            if (!e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isPassenger() || e.isVehicle()) continue;
            if (e.getMaxHealth() >= Wave29Rules.PORTAL_MAX_HEALTH || e.getType().is(BOSSES)) continue;
            if (!Wave29Rules.inPortal(e.getX() - c.x, e.getY() - c.y, e.getZ() - c.z)) continue;
            CompoundTag d = Wave29Util.peek(e);
            if (d != null && d.getLong(K_ARRIVED) > now) continue;
            arrive(sl, e, o.dest(), now);
            carried++;
        }
    }

    /** Puts a creature within a block of the destination (the nearest free spot of a few tries; the exact spot if none is). */
    private static void arrive(ServerLevel sl, LivingEntity e, Vec3 dest, long now) {
        Vec3 spot = dest;
        for (int i = 0; i < 6; i++) {
            double ang = e.getRandom().nextDouble() * Math.PI * 2;
            double dist = i == 0 ? 0.0 : e.getRandom().nextDouble() * Wave29Rules.ARRIVAL_SPREAD;
            Vec3 try1 = dest.add(Math.cos(ang) * dist, 0, Math.sin(ang) * dist);
            if (sl.noCollision(e, e.getBoundingBox().move(try1.x - e.getX(), try1.y - e.getY(), try1.z - e.getZ()))) {
                spot = try1;
                break;
            }
        }
        Fx.burst(sl, FxThemes.RELIC_WAVE_29, e.position().add(0, e.getBbHeight() * 0.5, 0), FxTone.LIGHT, 8, 0.5);
        Wave29Util.data(e).putLong(K_ARRIVED, now + Wave29Rules.ARRIVAL_GRACE);
        if (e instanceof Mob m) m.getNavigation().stop();
        e.teleportTo(spot.x, spot.y, spot.z);
        e.fallDistance = 0;
        Fx.burst(sl, FxThemes.RELIC_WAVE_29, spot.add(0, e.getBbHeight() * 0.5, 0), FxTone.LIGHT, 10, 0.6);
        sl.playSound(null, e.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    // ================================================================ cleaning up
    static void forget(ServerPlayer p) { OPEN.removeIf(o -> o.owner().equals(p.getUUID())); }

    static void clear() { OPEN.clear(); }
}
