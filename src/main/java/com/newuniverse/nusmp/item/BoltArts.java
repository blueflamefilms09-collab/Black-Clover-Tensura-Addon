package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * 1.3.0: the two crossbows of the "Magic Item Compendium I" sheet. Both load themselves (no ammunition): a right-click looses one
 * hitscan bolt of force, drawn as a lightning streak from the muzzle. Force damage is magic damage, so armour does not turn it,
 * and it goes through the balance law like every other weapon technique. Reached from {@link ReferenceWeapons}.
 * <ul>
 *   <li><b>Bolt Thrower</b> (steel and brass, crackling blue): Force Bolt, short cooldown. Every bolt leaves a Thrower's Mark
 *       (up to three, 8 s): a marked foe takes 20% more from each mark already on it. Sneak + right-click: Triple Volley, three
 *       bolts in quick succession, converging on the foe in the crosshair. A melee hit discharges the marks; the full swing is a
 *       muzzle bash that leaves a fresh mark.</li>
 *   <li><b>Crossbow of Swift Death</b> (blackened wood, red notches): every bolt may crit, likelier the more wounded the foe. A crit is
 *       Swift Death: a foe under 15% health is executed (a boss takes a heavy blow instead, capped like every other control) and
 *       the foe is struck with fear and weakness. A kill by a crit etches another notch on the frame: each notch is +1% crit chance
 *       (up to +20%) and a little more crit damage. Sneak + right-click: Death Mark, the next three bolts always crit.</li>
 * </ul>
 */
final class BoltArts {
    private BoltArts() {}

    static final double RANGE = 48;
    static final int MARK_MAX = 3, MARK_TICKS = 160, DEATH_MARKS = 3;
    private static final String MARKS = "nusmp_bt_marks", MARK_UNTIL = "nusmp_bt_mark_until";
    private static final String NOTCHES = "Notches", DEATH_MARK = "DeathMark";

    private static final int CYAN = 0xFF7FE6FF, WHITE = 0xFFEAFBFF, CRIMSON = 0xFFD0183A, DEEP = 0xFF6A0C26, PALE_RED = 0xFFFFC8D0;

    static boolean isBolt(Kind k) { return k == Kind.BOLT_THROWER || k == Kind.CROSSBOW_OF_SWIFT_DEATH; }

    // ---------------------------------------------------------------- state
    /** Thrower's Marks on a foe (0 once they lapse). */
    static int marks(LivingEntity t) {
        CompoundTag d = t.getPersistentData();
        return t.level().getGameTime() < d.getLong(MARK_UNTIL) ? Mth.clamp(d.getInt(MARKS), 0, MARK_MAX) : 0;
    }

    private static void setMarks(LivingEntity t, int n) {
        CompoundTag d = t.getPersistentData();
        d.putInt(MARKS, Mth.clamp(n, 0, MARK_MAX));
        d.putLong(MARK_UNTIL, t.level().getGameTime() + MARK_TICKS);
    }

    static int notches(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : Mth.clamp(d.copyTag().getInt(NOTCHES), 0, 999);
    }

    private static void etch(ItemStack s) {
        if (s.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, s, tag -> tag.putInt(NOTCHES, Math.min(999, tag.getInt(NOTCHES) + 1)));
    }

    static int deathMark(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : Mth.clamp(d.copyTag().getInt(DEATH_MARK), 0, DEATH_MARKS);
    }

    private static void setDeathMark(ItemStack s, int n) {
        int v = Mth.clamp(n, 0, DEATH_MARKS);
        CustomData.update(DataComponents.CUSTOM_DATA, s, tag -> tag.putInt(DEATH_MARK, v));
    }

    /** Chance that a Crossbow of Swift Death bolt crits: it rises with the foe's missing health, and with the notches on the frame. */
    static float critChance(LivingEntity t, int notches) {
        float missing = 1f - Mth.clamp(t.getHealth() / Math.max(1f, t.getMaxHealth()), 0f, 1f);
        return Mth.clamp(0.10f + 0.55f * missing + 0.01f * Math.min(notches, 20), 0f, 0.95f);
    }

    // ---------------------------------------------------------------- shooting
    private record Shot(LivingEntity target, Vec3 end) {}

    /** A hitscan from {@code from} along {@code dir}: the first foe within reach, or where the bolt ends (a wall or the range). */
    private static Shot trace(ServerPlayer p, Vec3 from, Vec3 dir, double range) {
        ServerLevel sl = p.serverLevel();
        Vec3 end = from.add(dir.scale(range));
        HitResult wall = sl.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(sl, p, from, end, new AABB(from, end).inflate(1.0),
                e -> e instanceof LivingEntity le && le.isAlive() && !le.isSpectator() && e != p && !e.isAlliedTo(p), 0.35f);
        if (hit != null && hit.getEntity() instanceof LivingEntity le) return new Shot(le, hit.getLocation());
        return new Shot(null, end);
    }

    /** Where the bolt leaves the weapon: in front of the hand, {@code side} blocks to the right. */
    private static Vec3 muzzle(ServerPlayer p, double side) {
        Vec3 look = p.getViewVector(1f);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : right.normalize();
        return p.getEyePosition().add(look.scale(1.1)).add(right.scale(0.22 + side)).add(0, -0.22, 0);
    }

    /** Force damage: magic damage that armour does not turn, with the hurt timer reset so quick bolts all land. */
    private static boolean force(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p)) return false;
        t.invulnerableTime = 0;
        return t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, raw, 0.6));
    }

    private static void melee(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p)) return;
        t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, t, raw, 0.6));
    }

    // ---------------------------------------------------------------- Bolt Thrower
    /** One Force Bolt: a streak of lightning to the hit, a mark left on the foe. */
    private static void thrower(ServerPlayer p, Vec3 dir, float raw, double side) {
        ServerLevel sl = p.serverLevel();
        Shot s = trace(p, p.getEyePosition(), dir, RANGE);
        Vec3 mz = muzzle(p, side);
        VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, mz, s.end(), CYAN, 8, 0.8f);
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, mz, s.end(), WHITE, 6, 1.2f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mz, mz, CYAN, 6, 0.5f);
        sl.playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.1f, 1.45f);
        LivingEntity t = s.target();
        if (t == null) {
            VfxSpawn.send(sl, VfxShape.FX_LIGHTNING_ARC, s.end().add(0, 0.7, 0), s.end(), CYAN, 6, 0.5f);
            return;
        }
        int marks = marks(t);
        force(p, t, raw * (1f + 0.2f * marks));
        setMarks(t, marks + 1);
        Vec3 mid = t.getBoundingBox().getCenter();
        VfxSpawn.send(sl, VfxShape.FX_LIGHTNING_ARC, mid.add(0, 1.0, 0), mid, CYAN, 8, 0.8f + 0.2f * marks);
        if (marks > 0) VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, WHITE, 8, 0.8f + 0.3f * marks);
        sl.playSound(null, t.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.35f + 0.15f * marks, 1.9f);
        if (marks >= MARK_MAX) {                                                  // a fully charged foe arcs to the next one
            LivingEntity next = null;
            for (LivingEntity o : GrimoireBook.around(p, t.position(), 5)) {
                if (o == t) continue;
                if (next == null || o.distanceToSqr(t) < next.distanceToSqr(t)) next = o;
            }
            if (next != null) {
                force(p, next, raw * 0.5f);
                setMarks(next, marks(next) + 1);
                VfxSpawn.send(sl, VfxShape.FX_LIGHTNING_ARC, mid, next.getBoundingBox().getCenter(), CYAN, 8, 1.1f);
            }
        }
    }

    // ---------------------------------------------------------------- Crossbow of Swift Death
    /** One Notched Bolt: may crit; a crit is Swift Death. */
    private static void swift(ServerPlayer p, ItemStack stack, Vec3 dir, float raw, double side) {
        ServerLevel sl = p.serverLevel();
        Shot s = trace(p, p.getEyePosition(), dir, RANGE);
        Vec3 mz = muzzle(p, side);
        VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, mz, s.end(), CRIMSON, 8, 0.7f);
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, mz, s.end(), PALE_RED, 6, 1.1f);
        sl.playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2f, 0.75f);
        LivingEntity t = s.target();
        if (t == null) return;
        int mark = deathMark(stack), notches = notches(stack);
        boolean crit = mark > 0 || p.getRandom().nextFloat() < critChance(t, notches);
        if (mark > 0) setDeathMark(stack, mark - 1);
        Vec3 mid = t.getBoundingBox().getCenter();
        if (!crit) {
            force(p, t, raw);
            VfxSpawn.send(sl, VfxShape.BLOOD_FX1, mid, t.position(), DEEP, 8, 0.5f);
            return;
        }
        force(p, t, raw * (1.6f + 0.01f * Math.min(notches, 50)));
        sl.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.3f, 0.8f);
        Vec3 look = p.getViewVector(1f);
        VfxSpawn.send(sl, VfxShape.WIND_SLASH, mid.subtract(look.scale(1.4)).add(0, 0.8, 0), mid.add(look.scale(1.4)).subtract(0, 0.8, 0), CRIMSON, 10, 1.3f);
        VfxSpawn.send(sl, VfxShape.BLOOD_FX1, mid, t.position(), CRIMSON, 12, 0.9f);
        if (t.isAlive()) swiftDeath(p, t, mid);
        if (!t.isAlive()) {                                                       // the crit killed it: another notch on the frame
            etch(stack);
            p.displayClientMessage(Component.literal("A notch is etched: " + notches(stack)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        }
    }

    /** The crit's second blow: execute a foe under 15% health, a heavy bonus on a boss, then fear. */
    private static void swiftDeath(ServerPlayer p, LivingEntity t, Vec3 mid) {
        ServerLevel sl = p.serverLevel();
        if (BalanceLaw.isBoss(t)) {
            force(p, t, 20f);
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, mid, t.position(), PALE_RED, 16, 1.4f);
            p.displayClientMessage(Component.literal("Swift Death!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        } else if (t.getHealth() <= t.getMaxHealth() * 0.15f) {
            float lethal = t instanceof Player ? BalanceLaw.cap(t, t.getHealth() + 1f) : t.getHealth() * 2f + 20f;
            t.invulnerableTime = 0;
            t.hurt(p.damageSources().indirectMagic(p, p), lethal);
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, mid, t.position(), 0xFFFFFFFF, 20, 1.8f);
            VfxSpawn.send(sl, VfxShape.WIND_SLASH, mid.add(-1.6, 1.4, 0), mid.add(1.6, -0.6, 0), 0xFFFFFFFF, 12, 1.6f);
            sl.playSound(null, t.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.9f, 0.7f);
            p.displayClientMessage(Component.literal("Swift Death!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        }
        if (t.isAlive()) fright(p, t);
    }

    /** Fear and weakness: mobs drop their target and bolt away, players are slowed; bosses are only weakened (capped control). */
    private static void fright(ServerPlayer p, LivingEntity t) {
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        if (BalanceLaw.isBoss(t) || !BalanceLaw.beginControl(p)) return;
        int ticks = BalanceLaw.controlTicks(t, 50);
        if (t instanceof Mob mob) {
            mob.setTarget(null);
            Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) {
                Vec3 to = t.position().add(away.normalize().scale(10));
                mob.getNavigation().moveTo(to.x, to.y, to.z, 1.4);
            }
        }
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, t instanceof Player ? 1 : 0));
        VfxSpawn.send(p.serverLevel(), VfxShape.KOTO_SHATTER, t.getBoundingBox().getCenter(), t.position(), DEEP, 14, 0.8f);
    }

    // ---------------------------------------------------------------- right-click technique
    static boolean technique(ServerPlayer p, Kind k, ItemStack stack) {
        Vec3 look = p.getViewVector(1f);
        switch (k) {
            case BOLT_THROWER -> thrower(p, look, 7f, 0);
            case CROSSBOW_OF_SWIFT_DEATH -> swift(p, stack, look, 9f, 0);
            default -> { return false; }
        }
        return true;
    }

    // ---------------------------------------------------------------- the second technique
    static WeaponArts.Alt alt(Kind k) {
        return switch (k) {
            case BOLT_THROWER -> new WeaponArts.Alt("Triple Volley", "Three bolts in quick succession, converging on the foe in your crosshair: each leaves a mark and the next one hits harder.", 400);
            case CROSSBOW_OF_SWIFT_DEATH -> new WeaponArts.Alt("Death Mark", "The next three bolts always crit, and Swift Death can claim whatever they strike. The foe you aim at is lit up.", 600);
            default -> null;
        };
    }

    static boolean cast(ServerPlayer p, Kind k, ItemStack stack) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        switch (k) {
            case BOLT_THROWER -> {
                LivingEntity lock = trace(p, eye, look, RANGE).target();
                for (int i = 0; i < 3; i++) {
                    final int n = i;
                    SpellRuntime.later(sl, n * 3, () -> {
                        if (!p.isAlive()) return;
                        Vec3 e2 = p.getEyePosition(), l2 = p.getViewVector(1f);
                        Vec3 dir = lock != null && lock.isAlive() ? lock.getBoundingBox().getCenter().subtract(e2).normalize() : l2.yRot((n - 1) * 0.1f);
                        thrower(p, dir, 6f, (n - 1) * 0.4);
                    });
                }
            }
            case CROSSBOW_OF_SWIFT_DEATH -> {
                if (stack.isEmpty()) return false;
                setDeathMark(stack, DEATH_MARKS);
                LivingEntity prey = trace(p, eye, look, RANGE).target();
                if (prey != null) prey.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, p, p.position(), CRIMSON, 40, 1.0f);
                VfxSpawn.send(sl, VfxShape.BLOOD_FX2, p.position().add(0, 1.0, 0), p.position(), CRIMSON, 24, 0.9f);
                sl.playSound(null, p.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.7f, 0.5f);
            }
            default -> { return false; }
        }
        return true;
    }

    // ---------------------------------------------------------------- melee
    /** Bolt Thrower: a melee hit discharges every mark on the foe as a burst of lightning. */
    static void onHit(ServerPlayer p, ItemStack stack, LivingEntity t, Kind k) {
        if (k != Kind.BOLT_THROWER) return;
        int marks = marks(t);
        if (marks <= 0) return;
        setMarks(t, 0);
        t.getPersistentData().putLong(MARK_UNTIL, 0);
        force(p, t, 2.5f * marks);
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter();
        VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, mid.add(0, 4, 0), mid, CYAN, 8, 0.9f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, WHITE, 10, 1.0f + 0.4f * marks);
        sl.playSound(null, t.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    /** The signature strike on a full swing: a bash with the weapon's frame. */
    static void fullSwing(ServerPlayer p, LivingEntity t, Kind k) {
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter();
        switch (k) {
            case BOLT_THROWER -> {                                                   // a muzzle bash that spits a spark of force
                melee(p, t, 4f);
                setMarks(t, marks(t) + 1);
                if (!BalanceLaw.isBoss(t)) t.knockback(0.8, p.getX() - t.getX(), p.getZ() - t.getZ());
                VfxSpawn.send(sl, VfxShape.FX_LIGHTNING_ARC, mid.add(0, 1.2, 0), mid, CYAN, 8, 0.9f);
                VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, WHITE, 8, 0.9f);
            }
            case CROSSBOW_OF_SWIFT_DEATH -> {                                        // a butt-stroke that may frighten
                melee(p, t, 4f);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                if (!BalanceLaw.isBoss(t)) t.knockback(0.7, p.getX() - t.getX(), p.getZ() - t.getZ());
                if (p.getRandom().nextFloat() < critChance(t, notches(ReferenceWeapons.held(p, k)))) {
                    fright(p, t);
                    VfxSpawn.send(sl, VfxShape.BLOOD_FX1, mid, t.position(), CRIMSON, 10, 0.7f);
                }
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- tooltip
    static void tooltip(ItemStack stack, Kind k, List<Component> tip) {
        if (k == Kind.BOLT_THROWER) {
            tip.add(Component.literal("  No ammunition: the bolts are force, and armour does not turn them").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Thrower's Mark: every bolt marks the foe (up to 3, 8 s); each mark on it makes the next bolt hit 20% harder, and a fully marked foe arcs to a neighbour. A melee hit discharges the marks.").withStyle(ChatFormatting.AQUA));
            tip.add(Component.literal("Dwarven steel and brass, a bolt channel humming with captured storm: it throws bolts of pure magical force.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else if (k == Kind.CROSSBOW_OF_SWIFT_DEATH) {
            int notches = notches(stack), mark = deathMark(stack);
            tip.add(Component.literal("  No ammunition: the bolts are force, and armour does not turn them").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Swift Death: a crit (10% at full health, up to 65% on a dying foe) executes a foe under 15% health, strikes a boss with a heavy blow, and frightens and weakens what it does not kill.").withStyle(ChatFormatting.DARK_RED));
            tip.add(Component.literal("Notches on the frame: " + notches + " (each kill by a crit etches another: +1% crit chance, up to +20%)").withStyle(ChatFormatting.RED));
            if (mark > 0) tip.add(Component.literal("Death Mark: the next " + mark + " bolt" + (mark == 1 ? "" : "s") + " always crit").withStyle(ChatFormatting.DARK_PURPLE));
            tip.add(Component.literal("A chilling series of notches above the trigger, one for every creature it has taken in a single breath.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
