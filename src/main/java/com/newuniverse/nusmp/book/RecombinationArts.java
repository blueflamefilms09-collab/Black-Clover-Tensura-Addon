package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Recombination Magic (Henry Legolant, the Black Bulls' magic house), 1.93.0 rebuild: the spells of the book (ids and numbers in {@link RecombinationRules}; the four old pages are
 * re-implemented, four are appended). Canon: Henry rearranges the rooms of the magic house and, in the story, rebuilds it into the Raging Black Bull and, with the Transmutation, Mirror
 * and Poison casters, into the Ultra Giant Bull (a cockpit, cannons all over the body, a beam that arcs round corners). Everything the house becomes is a prop with a geo model: the room,
 * the drill, the Bull. The Bull replaces the iron golem the old page spawned.
 * <ul>
 *   <li>Mana Corkscrew (starter): a drill of compressed mana (a drill prop with a model) bores through a line, pushing what it hits back.</li>
 *   <li>Recombination: Bulwark: a ring of oak walls for ten seconds, four rooms standing at its corners, Absorption II.</li>
 *   <li>Recombination: Room Swap: two rooms change places: caster and target swap, a room rises at each place, the target is slowed a second.</li>
 *   <li>The Raging Black Bull: the house becomes a bull that follows, rams and fires its cannons for a minute.</li>
 *   <li>House Armour (new): the house worn as armour for twenty seconds, a share of every blow taken off.</li>
 *   <li>Room Rebuild (new): the allies in seven blocks are mended as the house mends itself.</li>
 *   <li>Ultra Giant Bull (new): the Compound spell: the Bull grows 1.6 times, fires four cannons at a time and rams harder for twenty seconds. Needs the Bull to stand.</li>
 *   <li>Plank Snare (new): boards close round a foe and hold it for three seconds (the shared control lock; a boss for a quarter).</li>
 * </ul>
 */
public final class RecombinationArts {
    private RecombinationArts() {}

    public static final int ORANGE = 0xFFFF9A3C;
    private static final String ARMOUR_UNTIL = "nusmp_house_until", ARMOUR_CUT = "nusmp_house_cut";
    private static boolean hooked;

    /** Registers the damage hook of the House Armour. Called once from RecombinationProps.init(). */
    public static synchronized void hooks() {
        if (hooked) return;
        hooked = true;
        AttributeEvents.incoming(e -> {
            LivingEntity victim = e.getEntity();
            var data = victim.getPersistentData();
            if (data.getLong(ARMOUR_UNTIL) <= victim.level().getGameTime() || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            e.setAmount(e.getAmount() * (1f - data.getFloat(ARMOUR_CUT)));
        });
    }

    // ------------------------------------------------------------------ helpers
    private static Vec3 flat(Vec3 v) {
        Vec3 f = v.multiply(1, 0, 1);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    static void dismiss(ServerPlayer p, PropKind kind) {
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96), x -> x.kind() == kind && p.getUUID().equals(x.ownerId()))) e.expire();
    }

    static MagicPropEntity bullOf(ServerPlayer p) {
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96), x -> x.kind() == PropKind.RECOMBINATION_3 && p.getUUID().equals(x.ownerId()))) return e;
        return null;
    }

    private static void armour(ServerPlayer p, int ticks, int style) {
        var data = p.getPersistentData();
        data.putLong(ARMOUR_UNTIL, p.level().getGameTime() + ticks);
        data.putFloat(ARMOUR_CUT, RecombinationRules.armourCut(BalanceLaw.epScale(p)));
    }

    private static Vec3 ground(ServerLevel level, Vec3 pos) { return TransmutationArts.groundAt(level, pos); }

    // ------------------------------------------------------------------ the four old pages
    /** Mana Corkscrew. */
    public static boolean manaCorkscrew(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.8f);
        VfxSpawn.send(level, VfxShape.RECOMBINATION_FX1, start.add(0, -0.3, 0), start.add(dir.scale(20)), ORANGE, 22, 0.9f);
        MagicPropEntity drill = MagicProps.spawn(level, PropKind.RECOMBINATION_4, start.add(dir.scale(0.8)).add(0, -0.3, 0), p.getYRot(), 1f, 14, 0, p);
        if (drill != null) drill.setXRot(p.getXRot());
        SpellRuntime.bolt(p, start, dir.scale(1.7), 0.9, 12, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 10f);
            t.knockback(0.7, -dir.x, -dir.z);
        }, null);
        return true;
    }

    /** Recombination: Bulwark. */
    public static boolean bulwark(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        BlockPos c = p.blockPosition();
        int placed = 0;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue;
            for (int y = 0; y < 2; y++) if (SpellRuntime.tempBlock(level, c.offset(dx, y, dz), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 200)) placed++;
        }
        if (placed == 0) { GrimoireBook.fail(p, "No room for walls."); return false; }
        b.castCircle(p, 0.9f);
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
            MagicProps.spawn(level, PropKind.RECOMBINATION_1, ground(level, p.position().add(sx * 3.4, 0, sz * 3.4)), (sx * sz > 0 ? 45f : -45f), 0.85f, 200, 0, p);
        VfxSpawn.send(level, VfxShape.RECOMBINATION_FX2, p.position().add(0, 0.05, 0), p.position().add(0, 1, 0), ORANGE, 70, 3.4f / 1.5f);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        return true;
    }

    /** Recombination: Room Swap. */
    public static boolean roomSwap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null) { GrimoireBook.fail(p, "No one to swap with."); return false; }
        Vec3 a = p.position(), c = t.position();
        VfxSpawn.send(level, VfxShape.RECOMBINATION_FX3, a, a.add(0, 1, 0), ORANGE, 26, 1.0f);
        VfxSpawn.send(level, VfxShape.RECOMBINATION_FX3, c, c.add(0, 1, 0), ORANGE, 26, 1.0f);
        t.teleportTo(a.x, a.y, a.z);
        p.teleportTo(c.x, c.y, c.z);
        MagicProps.spawn(level, PropKind.RECOMBINATION_1, a, p.getYRot(), 0.8f, 30, 1, p);
        MagicProps.spawn(level, PropKind.RECOMBINATION_1, c, p.getYRot() + 90f, 0.8f, 30, 1, p);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? 10 : 20, 3));
        return true;
    }

    /** The Raging Black Bull. */
    public static boolean ragingBlackBull(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        double ep = BalanceLaw.epScale(p);
        dismiss(p, PropKind.RECOMBINATION_3);
        Vec3 look = flat(p.getViewVector(1f)), at = ground(level, p.position().add(look.scale(3.2)));
        MagicPropEntity bull = MagicProps.spawn(level, PropKind.RECOMBINATION_3, at, p.getYRot(), RecombinationRules.bullScale(ep, false), RecombinationRules.summonTicks(RecombinationRules.BULL_TICKS, ep), 0, p);
        if (bull == null) { GrimoireBook.fail(p, "The house would not take form."); return false; }
        bull.getPersistentData().putFloat("nusmp_bull_dmg", (float) GrimoireBook.cover(i).damage);
        b.castCircle(p, 1.5f);
        VfxSpawn.send(level, VfxShape.RECOMBINATION_FX3, at, at.add(0, 1.5, 0), ORANGE, 34, 1.6f);
        VfxSpawn.send(level, VfxShape.RECOMBINATION_FX2, at.add(0, 0.05, 0), at.add(0, 1, 0), ORANGE, 60, 4.0f / 1.5f);
        shortArmour(p);
        return true;
    }

    private static void shortArmour(ServerPlayer p) {
        // 1.99.0: no short armour after a cast (book.ArmourPolicy: a cast never equips)
    }

    // ------------------------------------------------------------------ the four new pages
    /** House Armour. */
    public static boolean houseArmour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        armour(p, RecombinationRules.ARMOUR_TICKS, 0);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RecombinationRules.ARMOUR_TICKS, 0));
        b.castCircle(p, 1.5f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.RECOMBINATION_FX3, p, p.position().add(0, 1, 0), ORANGE, 36, 1.3f);
        VfxSpawn.send(p.serverLevel(), VfxShape.RECOMBINATION_FX2, p.position().add(0, 0.05, 0), p.position(), ORANGE, 50, 2.6f / 1.5f);
        return true;
    }

    /** Room Rebuild. */
    public static boolean roomRebuild(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        float heal = RecombinationRules.roomHeal(BalanceLaw.epScale(p));
        int n = 0;
        for (LivingEntity f : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(7, 4, 7), e -> e.isAlive() && (e == p || e.isAlliedTo(p)))) {
            if (n++ >= 8) break;
            BalanceLaw.heal(f, heal);
            f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            VfxSpawn.send(level, VfxShape.RECOMBINATION_FX3, f.position(), f.position().add(0, 1, 0), ORANGE, 20, 0.5f);
        }
        MagicProps.spawn(level, PropKind.RECOMBINATION_1, ground(level, c.add(look(p).scale(2.0))), p.getYRot(), 1.0f, 90, 0, p);
        b.castCircle(p, 1.2f);
        VfxSpawn.send(level, VfxShape.RECOMBINATION_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), ORANGE, 60, 7f / 1.5f);
        return true;
    }

    private static Vec3 look(ServerPlayer p) { return flat(p.getViewVector(1f)); }

    /** Ultra Giant Bull. */
    public static boolean ultraGiantBull(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        MagicPropEntity bull = bullOf(p);
        if (bull == null) { GrimoireBook.fail(p, "The Raging Black Bull must stand first."); return false; }
        if ((bull.param() & RecombinationRules.ULTRA_BIT) != 0) { GrimoireBook.fail(p, "The Bull is already grown."); return false; }
        long now = p.level().getGameTime();
        bull.setParam((bull.param() & 0x3FFFF) | RecombinationRules.ULTRA_BIT | (int) ((now & 0x7FF) << 19));
        bull.setSize(bull.scale() * 1.6f * 2.2f, bull.scale() * 1.6f * 3.0f);
        b.castCircle(p, 1.8f);
        VfxSpawn.send(p.serverLevel(), VfxShape.RECOMBINATION_FX3, bull.position(), bull.position().add(0, 2, 0), ORANGE, 40, 2.4f);
        VfxSpawn.send(p.serverLevel(), VfxShape.RECOMBINATION_FX2, bull.position().add(0, 0.05, 0), bull.position().add(0, 1, 0), ORANGE, 80, 7f / 1.5f);
        return true;
    }

    /** Plank Snare. */
    public static boolean plankSnare(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 22);
        if (t == null) { GrimoireBook.fail(p, "No one to snare."); return false; }
        if (!GrimoireBook.control(p)) return false;
        ServerLevel level = p.serverLevel();
        int ticks = RecombinationRules.snareTicks(BalanceLaw.isBoss(t), BalanceLaw.controlTicks(t, RecombinationRules.SNARE_TICKS));
        Vec3 at = t.position();
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
        MagicProps.spawn(level, PropKind.RECOMBINATION_2, at.add(0, 0.2, 0), 0f, 1.0f + t.getBbWidth() * 0.3f, ticks, 0, p);
        b.castCircle(p, 0.9f);
        VfxSpawn.send(level, VfxShape.RECOMBINATION_FX2, at.add(0, 0.05, 0), at.add(0, 1, 0), ORANGE, ticks + 10, 2.4f / 1.5f);
        SpellRuntime.zone(level, ticks, 4, age -> {
            if (!t.isAlive()) return;
            t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
            t.hurtMarked = true;
        });
        SpellRuntime.later(level, ticks, () -> {
            if (!t.isAlive() || !p.isAlive()) return;
            b.hurt(i, p, t, mode, 4f);
            VfxSpawn.send(level, VfxShape.RECOMBINATION_FX3, t.position(), t.position().add(0, 1, 0), ORANGE, 22, 0.8f);
        });
        return true;
    }

    public static boolean armourOn(LivingEntity e) { return e.getPersistentData().getLong(ARMOUR_UNTIL) > e.level().getGameTime(); }
}
