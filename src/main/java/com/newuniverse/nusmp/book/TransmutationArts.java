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
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.DamageTypeTags;

import java.util.ArrayList;
import java.util.List;

/**
 * Transmutation Magic (Grey), 1.93.0 rebuild: the spells of TransmutationBook's pages (the four old ids re-implemented, four new ones appended; ids and numbers in {@link TransmutationRules}).
 * Canon: Grey takes a substance apart and puts it back as another (ground to iron, ground to sand, a spell into mana; with Gauche she turned Morris's tendrils into flowers; the Ultra Giant
 * Bull converts a whole structure into the magic house's material). Every blow scales with the caster's EP through {@code GrimoireBook.hurt}; constructs are props with their own geo models
 * (iron spikes, the Transmuted Sentinel, alchemic blades) and the armour is the Alchemist's Gilded Plate (Aura.TRANSMUTATION).
 * <ul>
 *   <li>Iron Spikes (starter): the ground ahead is turned to iron and thrust up in a line of spike props, each cutting what stands on it and weakening it.</li>
 *   <li>Magic Convert: a circle for two seconds; every enemy shot that touches it is turned into a burst of flowers and a share of the caster's mana.</li>
 *   <li>Quagmire: the aimed ground turns to sand for five seconds: everything in it sinks and is dragged.</li>
 *   <li>Grand Transmutation: every blessing on the foes is turned into its curse and taken by the caster; the five-second armour forms.</li>
 *   <li>Alchemic Blades (new): three to five blades of iron and brass circle the caster for ten seconds and lunge at the nearest foe.</li>
 *   <li>Flowering (new): foes' shots and the other casters' constructs in seven blocks become flowers; allies lose harmful effects and heal.</li>
 *   <li>Transmuted Sentinel (new): a two-block golem of iron and stone rises, follows and fights for thirty seconds.</li>
 *   <li>Alchemist's Armour (new): the Gilded Plate for twenty seconds, a share of every blow taken off ({@link TransmutationRules#armourCut}).</li>
 * </ul>
 */
public final class TransmutationArts {
    private TransmutationArts() {}

    public static final int TEAL = 0xFF7AF0D8, ROSE = 0xFFFF9AD0, SAND = 0xFFE0C080;
    private static final String ARMOUR_UNTIL = "nusmp_alch_until", ARMOUR_CUT = "nusmp_alch_cut";
    private static boolean hooked;

    /** Registers the damage hook of the armour. Called once from TransmutationProps.init(). */
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
    /** The top of the ground at this column, searched a few blocks down from a little above 'pos'. */
    static Vec3 groundAt(ServerLevel level, Vec3 pos) {
        BlockPos top = BlockPos.containing(pos.x, pos.y + 1.5, pos.z);
        for (int k = 0; k < 7; k++) {
            BlockPos b = top.below(k);
            if (!level.getBlockState(b).getCollisionShape(level, b).isEmpty()) return new Vec3(pos.x, b.getY() + 1.0, pos.z);
        }
        return pos;
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = v.multiply(1, 0, 1);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private static void dismiss(ServerPlayer p, PropKind kind) {
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96), x -> x.kind() == kind && p.getUUID().equals(x.ownerId()))) e.expire();
    }

    private static void armour(ServerPlayer p, int ticks, int style, double ep) {
        var data = p.getPersistentData();
        data.putLong(ARMOUR_UNTIL, p.level().getGameTime() + ticks);
        data.putFloat(ARMOUR_CUT, TransmutationRules.armourCut(ep));
    }

    /** The short armour a signature cast puts on (five seconds), unless a longer one is still on. */
    private static void shortArmour(ServerPlayer p) {
        // 1.99.0: no short armour after a cast (book.ArmourPolicy: a cast never equips)
    }

    // ------------------------------------------------------------------ the four old pages
    /** Transmutation: Iron Spikes. */
    public static boolean ironSpikes(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 a = p.position(), dir = flat(p.getViewVector(1f));
        float size = GrimoireBook.size(i, p);
        int n = TransmutationRules.spikeCount(size);
        b.castCircle(p, 0.7f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX2, a.add(0, 0.05, 0), a.add(0, 1, 0), b.color, 24, 1.1f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX1, p.getEyePosition().add(0, -0.3, 0), a.add(dir.scale(n)).add(0, 0.6, 0), b.color, 14, 0.9f);
        float scale = Math.max(0.7f, Math.min(1.3f, 0.75f + 0.25f * size));
        for (int k = 1; k <= n; k++) {
            Vec3 pt = a.add(dir.scale(k));
            SpellRuntime.later(level, k * 2, () -> {
                if (!p.isAlive()) return;
                Vec3 g = groundAt(level, pt);
                MagicProps.spawn(level, PropKind.TRANSMUTATION_1, g, p.getYRot(), scale, 34, 0, p);
                for (LivingEntity t : GrimoireBook.around(p, g, 1.3)) {
                    b.hurt(i, p, t, mode, 9f);
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                }
            });
        }
        return true;
    }

    /** Magic Convert. */
    public static boolean magicConvert(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1f), c = p.getEyePosition().add(look.scale(1.8));
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX2, p.position().add(0, 0.05, 0), p.position(), b.color, 44, 1.5f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX1, p.getEyePosition(), c.add(look), b.color, 10, 0.5f);
        SpellRuntime.zone(level, 40, 1, age -> {
            for (Projectile pr : WikiSpells.enemyShots(p, new AABB(c, c).inflate(2.2))) {
                VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX3, pr.position(), pr.position().add(0, 1, 0), ROSE, 22, 0.5f);
                pr.discard();
                WikiSpells.giveManaFrac(p, 0.04);
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            }
        });
        return true;
    }

    /** Transmutation: Quagmire. */
    public static boolean quagmire(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 20);
        float r = 4 * GrimoireBook.size(i, p);
        b.castCircle(p, 1f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), SAND, 100, r / 1.5f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX3, c, c.add(0, 1, 0), SAND, 24, 1.0f);
        SpellRuntime.zone(level, 100, 5, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
                t.setDeltaMovement(t.getDeltaMovement().multiply(0.6, 0, 0.6).add(0, -0.1, 0));
                t.hurtMarked = true;
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2f);
            }
        });
        return true;
    }

    /** Grand Transmutation. */
    public static boolean grandTransmutation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 1.5f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX2, p.position().add(0, 0.05, 0), p.position().add(0, 2, 0), b.color, 50, 8f / 1.5f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX3, p.position(), p.position().add(0, 1, 0), b.color, 32, 1.4f);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 8)) {
            for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
                if (!e.getEffect().value().isBeneficial()) continue;
                Holder<MobEffect> curse = WikiSpells.inverse(e.getEffect());
                t.removeEffect(e.getEffect());
                t.addEffect(new MobEffectInstance(curse, Math.min(e.getDuration(), 200), Math.min(e.getAmplifier(), 2)));
                p.addEffect(new MobEffectInstance(e.getEffect(), Math.min(e.getDuration(), 200), e.getAmplifier()));
            }
            b.hurt(i, p, t, mode, 10f);
        }
        shortArmour(p);
        return true;
    }

    // ------------------------------------------------------------------ the four new pages
    /** Alchemic Blades: the parameter of a blade prop packs its index (bits 18-21) and the count (bits 22-25); bits 0-17 are the state word of TransmutationProps. */
    public static boolean alchemicBlades(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        dismiss(p, PropKind.TRANSMUTATION_3);
        int n = TransmutationRules.bladeCount(EnergyBridge.modifier(p));
        int ticks = TransmutationRules.summonTicks(TransmutationRules.BLADE_TICKS, BalanceLaw.epScale(p));
        float size = GrimoireBook.size(i, p);
        b.castCircle(p, 1f);
        for (int k = 0; k < n; k++) {
            MagicPropEntity e = MagicProps.spawn(level, PropKind.TRANSMUTATION_3, p.position().add(0, 1.4, 0), p.getYRot(), 1f, ticks, (k << 18) | (n << 22), p);
            if (e != null) e.getPersistentData().putFloat("nusmp_blade_dmg", TransmutationRules.bladeHit(size) * (float) GrimoireBook.cover(i).damage);
        }
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), b.color, 24, 1.1f);
        return n > 0;
    }

    /** Flowering. */
    public static boolean flowering(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        double r = TransmutationRules.FLOWER_RADIUS;
        AABB box = new AABB(c, c).inflate(r, 4, r);
        int cleanses = TransmutationRules.flowerCleanses(EnergyBridge.modifier(p));
        float heal = 3f + (float) Math.min(8.0, BalanceLaw.epScale(p) - 1) * 0.5f;
        int helped = 0;
        for (LivingEntity f : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && (e == p || e.isAlliedTo(p)))) {
            if (helped++ >= 8) break;
            int stripped = 0;
            for (MobEffectInstance e : new ArrayList<>(f.getActiveEffects())) {
                if (stripped >= cleanses) break;
                if (e.getEffect().value().isBeneficial()) continue;
                f.removeEffect(e.getEffect());
                stripped++;
            }
            BalanceLaw.heal(f, heal);
            f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
            VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX3, f.position(), f.position().add(0, 1, 0), ROSE, 22, 0.5f);
        }
        int shots = 0, props = 0;
        for (Projectile pr : WikiSpells.enemyShots(p, box)) {
            if (shots++ >= 12) break;
            VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX3, pr.position(), pr.position().add(0, 1, 0), ROSE, 22, 0.4f);
            pr.discard();
        }
        for (MagicPropEntity m : level.getEntitiesOfClass(MagicPropEntity.class, box)) {         // the other casters' constructs become flowers
            LivingEntity o = m.owner();
            if (o == null || o == p || o.isAlliedTo(p)) continue;
            if (props++ >= 6) break;
            VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX3, m.position(), m.position().add(0, 1, 0), ROSE, 26, 0.8f);
            m.expire();
        }
        for (int k = 0; k < 14; k++) {                                                              // flowers on the ground for ten seconds
            double a = level.random.nextDouble() * Math.PI * 2, d = Math.sqrt(level.random.nextDouble()) * r;
            Vec3 g = groundAt(level, c.add(Math.cos(a) * d, 0, Math.sin(a) * d));
            SpellRuntime.tempBlock(level, BlockPos.containing(g.x, g.y + 0.1, g.z), (k % 3 == 0 ? Blocks.OXEYE_DAISY : k % 3 == 1 ? Blocks.POPPY : Blocks.PINK_TULIP).defaultBlockState(), 200);
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), ROSE, 60, (float) r / 1.5f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX3, c, c.add(0, 1, 0), ROSE, 34, 1.6f);
        return true;
    }

    /** Transmuted Sentinel. */
    public static boolean sentinel(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        double ep = BalanceLaw.epScale(p);
        dismiss(p, PropKind.TRANSMUTATION_2);
        Vec3 look = flat(p.getViewVector(1f)), at = groundAt(level, p.position().add(look.scale(2.2)));
        MagicPropEntity e = MagicProps.spawn(level, PropKind.TRANSMUTATION_2, at, p.getYRot() + 180f, TransmutationRules.sentinelScale(ep), TransmutationRules.summonTicks(TransmutationRules.SENTINEL_TICKS, ep), 0, p);
        if (e == null) { GrimoireBook.fail(p, "The matter would not take form."); return false; }
        e.getPersistentData().putFloat("nusmp_sentinel_dmg", 9f * (float) GrimoireBook.cover(i).damage);
        b.castCircle(p, 1.3f);
        VfxSpawn.send(level, VfxShape.TRANSMUTATION_FX2, at.add(0, 0.05, 0), at.add(0, 1, 0), b.color, 40, 2.4f / 1.5f);
        return true;
    }

    /** Alchemist's Armour. */
    public static boolean alchemistArmour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double ep = BalanceLaw.epScale(p);
        armour(p, TransmutationRules.ARMOUR_TICKS, 0, ep);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, TransmutationRules.ARMOUR_TICKS, 0));
        b.castCircle(p, 1.5f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.TRANSMUTATION_FX3, p, p.position().add(0, 1, 0), b.color, 36, 1.4f);
        VfxSpawn.send(p.serverLevel(), VfxShape.TRANSMUTATION_FX2, p.position().add(0, 0.05, 0), p.position(), b.color, 50, 2.6f / 1.5f);
        return true;
    }

    /** Is the Gilded Plate on this entity right now (server side)? */
    public static boolean armourOn(LivingEntity e) { return e.getPersistentData().getLong(ARMOUR_UNTIL) > e.level().getGameTime(); }
}
