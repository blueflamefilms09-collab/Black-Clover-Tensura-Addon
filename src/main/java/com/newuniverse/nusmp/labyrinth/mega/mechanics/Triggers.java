package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: WHEN a mechanic fires. Eighteen trigger kinds (the catalogue lists them): once on entering, on every entry, on a timer, on proximity, when looked at, after kills, when a
 * player is low, when an item is used, when a prop is used, when someone is hurt, sprints, stands still, sneaks, jumps, when the boss is wounded, when enough players are
 * together, when someone dies, when someone lingers in the dark. Polled kinds are evaluated every tick from a small per-player memory kept by {@link #observe}; event kinds are
 * raised by the Director's event handlers through the on... methods, which only mark the instance ({@code pendingFire}) so the effects still run inside the instance's own tick,
 * in one place. A trigger with a cooldown fires at most once per cooldown. Arguments are read through the catalogue so a mechanic need only name what it changes.
 */
public final class Triggers {
    private Triggers() {}

    /** What the trigger remembers of one player. */
    static final class PM {
        int sprint, idle, sneak, dark, jumps;
        boolean wasOnGround = true;
        double lx = Double.NaN, ly, lz;
        int light = 15;
    }

    /** What the trigger remembers of the instance. */
    static final class Mem {
        long cooldownUntil;
        long nextTimer = -1;
        boolean bossFired;
        final Map<UUID, PM> pm = new HashMap<>();
    }

    private static double num(Instance in, String k) {
        OpCatalog cat = MechanicLibrary.catalog();
        OpCatalog.KindSpec ks = cat == null ? null : cat.trigger(in.def.trigger().kind());
        OpCatalog.ArgSpec a = ks == null ? null : ks.arg(k);
        double d = a != null && a.def() instanceof Double dd ? dd : 0.0;
        return in.def.trigger().args().num(k, d, in.floor);
    }

    private static String str(Instance in, String k) {
        OpCatalog cat = MechanicLibrary.catalog();
        OpCatalog.KindSpec ks = cat == null ? null : cat.trigger(in.def.trigger().kind());
        OpCatalog.ArgSpec a = ks == null ? null : ks.arg(k);
        return in.def.trigger().args().str(k, a != null && a.def() instanceof String s ? s : "");
    }

    private static boolean ready(Instance in, long now) { return now >= in.trig.cooldownUntil; }

    private static void armCooldown(Instance in, long now) {
        if (in.def.trigger().args().has("cooldown") || hasCooldown(in)) in.trig.cooldownUntil = now + (long) num(in, "cooldown");
    }

    private static boolean hasCooldown(Instance in) {
        OpCatalog cat = MechanicLibrary.catalog();
        OpCatalog.KindSpec ks = cat == null ? null : cat.trigger(in.def.trigger().kind());
        return ks != null && ks.arg("cooldown") != null;
    }

    // ---------------------------------------------------------------- the per-player memory
    /** Updates what the polled triggers and the "quiet" counter need; called every tick before {@link #poll}. */
    static void observe(Instance in, long now) {
        Mem m = in.trig;
        m.pm.keySet().removeIf(id -> in.inside.stream().noneMatch(p -> p.getUUID().equals(id)));
        for (ServerPlayer p : in.inside) {
            PM x = m.pm.computeIfAbsent(p.getUUID(), k -> new PM());
            x.sprint = p.isSprinting() ? x.sprint + 1 : 0;
            x.sneak = p.isShiftKeyDown() ? x.sneak + 1 : 0;
            boolean moved = Double.isNaN(x.lx) || p.distanceToSqr(x.lx, x.ly, x.lz) > 0.0009;
            x.idle = moved ? 0 : x.idle + 1;
            x.lx = p.getX();
            x.ly = p.getY();
            x.lz = p.getZ();
            boolean ground = p.onGround();
            boolean jumped = x.wasOnGround && !ground && p.getDeltaMovement().y > 0.2;
            if (jumped) { x.jumps++; in.lastNoiseAt = now; }
            if (p.isSprinting()) in.lastNoiseAt = now;
            x.wasOnGround = ground;
            x.light = in.level.getMaxLocalRawBrightness(p.blockPosition());
        }
    }

    // ---------------------------------------------------------------- polled kinds
    /** Should the mechanic fire now? */
    static boolean poll(Instance in, long now) {
        if (in.inside.isEmpty()) return false;
        Mem m = in.trig;
        String kind = in.def.trigger().kind();
        switch (kind) {
            case "floor_enter":
                return !in.fired;
            case "area_enter": {
                if (!ready(in, now)) return false;
                for (ServerPlayer p : in.inside) {
                    if (!in.lastInside.contains(p.getUUID())) { armCooldown(in, now); return true; }
                }
                return false;
            }
            case "timer": {
                if (m.nextTimer < 0) m.nextTimer = now + (long) num(in, "every");
                if (now < m.nextTimer) return false;
                m.nextTimer = now + (long) Math.max(20, num(in, "every"));
                return true;
            }
            case "proximity": {
                if (!ready(in, now)) return false;
                double r = num(in, "r");
                for (ServerPlayer p : in.inside) {
                    if (p.distanceToSqr(in.anchor.getX() + 0.5, in.anchor.getY() + 0.5, in.anchor.getZ() + 0.5) <= r * r) { armCooldown(in, now); return true; }
                }
                return false;
            }
            case "sight": {
                if (!ready(in, now)) return false;
                double range = num(in, "range");
                Vec3 target = new Vec3(in.anchor.getX() + 0.5, in.anchor.getY() + 1.0, in.anchor.getZ() + 0.5);
                for (ServerPlayer p : in.inside) {
                    Vec3 eye = p.getEyePosition();
                    Vec3 to = target.subtract(eye);
                    double dist = to.length();
                    if (dist > range || dist < 1.0) continue;
                    if (p.getLookAngle().dot(to.scale(1.0 / dist)) < 0.96) continue;
                    HitResult hit = in.level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                    if (hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceTo(target) < 1.5) { armCooldown(in, now); return true; }
                }
                return false;
            }
            case "hp_below": {
                if (!ready(in, now)) return false;
                double frac = num(in, "frac");
                for (ServerPlayer p : in.inside) {
                    if (p.getHealth() < p.getMaxHealth() * frac) { armCooldown(in, now); return true; }
                }
                return false;
            }
            case "sprint": return counted(in, now, m, PMField.SPRINT, (int) num(in, "ticks"));
            case "idle": return counted(in, now, m, PMField.IDLE, (int) num(in, "ticks"));
            case "sneak": return counted(in, now, m, PMField.SNEAK, (int) num(in, "ticks"));
            case "dark": {
                int ticks = (int) num(in, "ticks");
                double light = num(in, "light");
                if (!ready(in, now)) return false;
                for (ServerPlayer p : in.inside) {
                    PM x = m.pm.get(p.getUUID());
                    if (x == null) continue;
                    x.dark = x.light <= light ? x.dark + 1 : 0;
                    if (x.dark >= ticks) { x.dark = 0; armCooldown(in, now); return true; }
                }
                return false;
            }
            case "jump": {
                if (!ready(in, now)) return false;
                int n = (int) num(in, "n");
                for (ServerPlayer p : in.inside) {
                    PM x = m.pm.get(p.getUUID());
                    if (x != null && x.jumps >= n) { x.jumps = 0; armCooldown(in, now); return true; }
                }
                return false;
            }
            case "party_size": {
                if (!ready(in, now)) return false;
                if (in.inside.size() >= (int) num(in, "n")) { armCooldown(in, now); return true; }
                return false;
            }
            case "boss_hp": {
                if (m.bossFired) return false;
                double frac = num(in, "frac");
                List<LabyrinthGuardianEntity> bosses = in.level.getEntitiesOfClass(LabyrinthGuardianEntity.class, in.area.aabb(), b -> b.isAlive());
                for (LabyrinthGuardianEntity b : bosses) {
                    if (b.getHealth() < b.getMaxHealth() * frac) { m.bossFired = true; return true; }
                }
                return false;
            }
            default:
                return false;                       // kill, item_use, interact, damage_taken, death: raised by events
        }
    }

    private enum PMField { SPRINT, IDLE, SNEAK }

    private static boolean counted(Instance in, long now, Mem m, PMField f, int ticks) {
        if (!ready(in, now)) return false;
        for (ServerPlayer p : in.inside) {
            PM x = m.pm.get(p.getUUID());
            if (x == null) continue;
            int have = f == PMField.SPRINT ? x.sprint : f == PMField.IDLE ? x.idle : x.sneak;
            if (have >= ticks) {
                if (f == PMField.SPRINT) x.sprint = 0; else if (f == PMField.IDLE) x.idle = 0; else x.sneak = 0;
                armCooldown(in, now);
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- event kinds
    static void onKill(Instance in) {
        if (!"kill".equals(in.def.trigger().kind()) || in.state != Instance.State.ARMED) return;
        if (in.kills >= (int) num(in, "n")) {
            in.kills = 0;
            in.pendingFire = true;
        }
    }

    static void onDamage(Instance in, ServerPlayer p, float amount) {
        if (!"damage_taken".equals(in.def.trigger().kind()) || in.state != Instance.State.ARMED) return;
        long now = in.clock();
        if (amount >= num(in, "min") && ready(in, now)) {
            armCooldown(in, now);
            in.pendingFire = true;
        }
    }

    static void onDeath(Instance in, ServerPlayer p) {
        if (!"death".equals(in.def.trigger().kind()) || in.state != Instance.State.ARMED) return;
        long now = in.clock();
        if (ready(in, now)) {
            armCooldown(in, now);
            in.pendingFire = true;
        }
    }

    static void onItemUse(Instance in, ServerPlayer p, ItemStack stack) {
        if (!"item_use".equals(in.def.trigger().kind()) || in.state != Instance.State.ARMED) return;
        long now = in.clock();
        if (ready(in, now) && itemMatches(stack, str(in, "item"))) {
            armCooldown(in, now);
            in.pendingFire = true;
        }
    }

    static void onUse(Instance in, ServerPlayer p, Prop prop) {
        if (!"interact".equals(in.def.trigger().kind()) || in.state != Instance.State.ARMED) return;
        long now = in.clock();
        if (ready(in, now)) {
            armCooldown(in, now);
            in.pendingFire = true;
        }
    }

    /** The item filters of the {@code item_use} trigger: any, food, light, weapon, magic (anything with a magicule or enchantment glint), or an item id. */
    public static boolean itemMatches(ItemStack s, String filter) {
        if (s == null || s.isEmpty()) return false;
        switch (filter) {
            case "any": return true;
            case "food": return s.has(net.minecraft.core.component.DataComponents.FOOD);
            case "light": return s.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().getLightEmission() >= 7;
            case "weapon": return s.getItem() instanceof SwordItem || s.getItem() instanceof AxeItem || s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem || s.getItem() instanceof TridentItem;
            case "magic": return s.isEnchanted() || BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals("tensura");
            default: return BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(filter);
        }
    }

    /** Is the player holding something that gives light? Used by the hold_light counter. */
    public static boolean holdsLight(Player p) {
        return itemMatches(p.getMainHandItem(), "light") || itemMatches(p.getOffhandItem(), "light");
    }
}
