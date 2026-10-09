package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.skill.codex.AnimeSkill;
import com.newuniverse.nusmp.skill.codex.AnimeSkillCodex;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Cast dispatch (mirrors ZagredAttacks): turns the primitives of a codex skill into effects. Only the sandbox primitives exist here; a skill can
 * never do anything this class does not spell out. Damage is Riven's own and goes through the normal hurt path, so Tensura barriers, resistances
 * and anti-magic apply as they do to any mob.
 */
final class RivenAttacks {
    private RivenAttacks() {}

    static final int BLUE_VIOLET = 0xFF7A5CFF;

    static void execute(RivenBossEntity b, ServerLevel sl, LivingEntity t, AnimeSkill s, ThreatScan scan, boolean copied) {
        int tier = s.tier() + (b.phase() >= 3 ? 1 : 0);                                       // the emotional high upgrades every skill one tier
        Vec3 from = b.getEyePosition();
        for (String p : s.primitives()) {
            switch (p) {
                case "projectile" -> {
                    VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, from, t.getBoundingBox().getCenter(), BLUE_VIOLET, 14, 1.0f + 0.2f * tier);
                    sl.playSound(null, b.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.5f, 1.6f);
                }
                case "magic_damage" -> damage(b, t, scan, "magic", 3.0f * tier, true);
                case "physical_damage" -> damage(b, t, scan, "physical", 2.5f * tier, false);
                case "melee_arc" -> {
                    VfxSpawn.send(sl, VfxShape.FX_LIGHTNING_ARC, b.position().add(0, 1.2, 0), t.getBoundingBox().getCenter(), BLUE_VIOLET, 8, 0.8f);
                    sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 1.2f);
                }
                case "heal" -> {
                    b.heal(b.getMaxHealth() * 0.012f * tier);
                    VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, b, b.position(), 0xFF9CFFD0, 30, 1.2f);
                }
                case "shield" -> {
                    b.setAbsorptionAmount(Math.min(40f, b.getAbsorptionAmount() + 8f * tier));
                    sl.playSound(null, b.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1f, 1.4f);
                }
                case "blink" -> blink(b, sl, t, s);
                case "pull" -> {
                    Vec3 d = b.position().subtract(t.position());
                    if (d.lengthSqr() > 4) t.setDeltaMovement(t.getDeltaMovement().add(d.normalize().scale(1.2).add(0, 0.25, 0)));
                    t.hurtMarked = true;
                }
                case "silence" -> {                                                                // no Tensura "silence" id is assumed: weakness + a short darkness stand in
                    if (!resisted(b, t, scan)) {
                        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                        t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
                    }
                }
                case "slow" -> { if (!resisted(b, t, scan)) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50 + 10 * tier, 1)); }
                case "song_buff" -> {
                    b.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
                    b.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
                    VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, b, b.position(), BLUE_VIOLET, 40, 1.4f);
                }
                case "song_debuff" -> {
                    if (!resisted(b, t, scan)) {
                        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1));
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                    }
                }
                case "summon_construct" -> {                                                       // a story construct, v1: a ward of pages that soaks damage and shoves the nearest rusher
                    b.setAbsorptionAmount(Math.min(40f, b.getAbsorptionAmount() + 6f * tier));
                    VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, b.position().add(0, 1.2, 0), t.position(), BLUE_VIOLET, 40, 1.0f);
                    if (t.distanceToSqr(b) < 9) t.knockback(0.8, b.getX() - t.getX(), b.getZ() - t.getZ());
                }
                case "copy_codex_skill" -> {
                    if (copied) break;
                    List<AnimeSkill> pool = AnimeSkillCodex.all().stream().filter(o -> !o.has("copy_codex_skill") && o.range() >= scan.distance).toList();
                    if (!pool.isEmpty()) {
                        AnimeSkill pick = pool.get(b.getRandom().nextInt(pool.size()));
                        b.announce(sl, pick.name() + " (copied)");
                        execute(b, sl, t, pick, scan, true);
                    }
                }
                default -> { }
            }
        }
    }

    static Vec3 ground(ServerLevel level, Vec3 p, double y) {
        BlockPos base = BlockPos.containing(p.x, y, p.z);
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos b = base.offset(0, dy, 0);
            if (level.getBlockState(b).isAir() && level.getBlockState(b.below()).blocksMotion()) return new Vec3(p.x, b.getY(), p.z);
        }
        return new Vec3(p.x, y, p.z);
    }

    /** Anti-magic (or a Tensura barrier) resists the rewrite unless Riven spends 25 story charge. */
    static boolean resisted(RivenBossEntity b, LivingEntity t, ThreatScan scan) {
        if (!scan.resists.contains("anti_magic") && !scan.traits.contains("tensura")) return false;
        return !b.spendCharge(25f);
    }

    private static void damage(RivenBossEntity b, LivingEntity t, ThreatScan scan, String kind, float amount, boolean magic) {
        if (!t.isAlive() || t.distanceToSqr(b) > 34 * 34) return;
        if (magic && scan.resists.contains("anti_magic") && !b.spendCharge(25f)) amount *= 0.35f;
        var src = magic ? b.damageSources().indirectMagic(b, b) : b.damageSources().mobAttack(b);
        boolean landed = t.hurt(src, amount);
        scan.record(kind, landed);
    }

    /** Shadow Step: a short blink behind the target (or off at the cast range), leaving an afterimage that casts one Eldritch Blast. */
    private static void blink(RivenBossEntity b, ServerLevel sl, LivingEntity t, AnimeSkill s) {
        Vec3 old = b.position();
        Vec3 look = new Vec3(t.getLookAngle().x, 0, t.getLookAngle().z);
        Vec3 back = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, -1) : look.normalize().scale(-3.0);
        Vec3 dest = null;
        for (int i = 0; i < 8 && dest == null; i++) {
            Vec3 c = i == 0 ? t.position().add(back) : b.position().add((b.getRandom().nextDouble() * 2 - 1) * 8, 0, (b.getRandom().nextDouble() * 2 - 1) * 8);
            Vec3 g = ground(sl, c, t.getY());
            if (sl.noCollision(b, b.getBoundingBox().move(g.subtract(old)))) dest = g;
        }
        if (dest == null) return;
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, old.add(0, 0.1, 0), old, BLUE_VIOLET, 24, 1.2f);
        b.teleportTo(dest.x, dest.y, dest.z);
        b.lookAt(t, 360f, 360f);
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, dest.add(0, 0.1, 0), dest, BLUE_VIOLET, 24, 1.2f);
        sl.playSound(null, b.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 1.4f);
        SpellRuntime.later(sl, 20, () -> {                                                          // the afterimage casts once
            if (!b.isAlive() || !t.isAlive()) return;
            VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, old.add(0, 1.6, 0), t.getBoundingBox().getCenter(), BLUE_VIOLET, 14, 1.0f);
            t.hurt(b.damageSources().indirectMagic(b, b), 2.0f * (b.phase() >= 3 ? 2 : 1));
        });
    }
}
