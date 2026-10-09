package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.skill.codex.AnimeSkill;
import com.newuniverse.nusmp.skill.codex.AnimeSkillCodex;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a codex skill: the sandbox primitives of {@link com.newuniverse.nusmp.skill.codex.SkillSandbox} mapped onto this mod's own
 * damage, effects and VFX. A skill is delivery (projectile / melee arc, or just the target), then effects on whoever it reached,
 * then self effects (blink, heal, shield, songs, constructs). Damage goes through the normal damage events, so anti-magic, Tensura
 * barriers and immunities decide what lands, and a hit that does nothing teaches the brain to stop using that kind.
 */
final class RivenAttacks {
    private RivenAttacks() {}

    static final int VIOLET = 0xFF8A6AFF, BLUE = 0xFF5AA8FF;

    static void execute(RivenBossEntity b, AnimeSkill s, LivingEntity target, ServerLevel sl, int depth) {
        if (target == null || !target.isAlive()) target = b.getTarget();
        List<String> prim = s.primitives();
        hook(b, s, target, sl);
        if (specialAttack(b, s, target, sl)) return;
        if (prim.contains("blink")) blink(b, s, target, sl);
        if (prim.contains("song_buff")) songBuff(b, sl);
        if (prim.contains("song_debuff")) songDebuff(b, sl);
        if (prim.contains("heal")) heal(b, sl);
        if (prim.contains("shield")) shield(b, sl);
        if (prim.contains("summon_construct")) summon(b, s, sl);
        if (prim.contains("copy_codex_skill") && depth < 1) copy(b, s, target, sl, depth);

        boolean ranged = prim.contains("projectile"), arc = prim.contains("melee_arc");
        boolean effectful = prim.contains("magic_damage") || prim.contains("physical_damage") || prim.contains("pull")
                || prim.contains("slow") || prim.contains("silence");
        if (!effectful || target == null) return;
        List<LivingEntity> hits = new ArrayList<>();
        int delay = 0;
        switch (s.nativeId()) {
            case "page_tear" -> hits.addAll(cone(b, 6.0, 0.35));
            case "severance_aria" -> hits.addAll(along(b, b.getEyePosition(), target.getBoundingBox().getCenter(), 14));
            case "island_fall" -> hits.addAll(sl.getEntitiesOfClass(Player.class,
                    target.getBoundingBox().inflate(4, 2, 4), RivenAttacks::enemy));
            case "maw_of_the_rift" -> hits.addAll(sl.getEntitiesOfClass(Player.class,
                    b.getBoundingBox().inflate(3, 1.5, 3), RivenAttacks::enemy));
            case "final_page" -> hits.addAll(cone(b, 10.0, 0.55));
            default -> { }
        }
        if (ranged && hits.isEmpty()) {
            Vec3 from = b.getEyePosition(), to = target.getBoundingBox().getCenter();
            hits.addAll(along(b, from, to, s.range()));
            delay = Math.max(1, (int) (from.distanceTo(to) / 2.5));
            VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, from, to, VIOLET, delay + 6, 1.1f);
        }
        if (arc && hits.isEmpty()) {
            hits.addAll(arc(b, 4.2));
            Vec3 c = b.position().add(b.getLookAngle().multiply(1, 0, 1).scale(1.8)).add(0, 1, 0);
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, c, c.add(0, 1, 0), BLUE, 12, 0.7f);
        }
        if (s.nativeId().equals("page_tear")) {
            Vec3 a = b.getEyePosition(), c = target.getBoundingBox().getCenter();
            VfxSpawn.send(sl, VfxShape.WIND_SLASH, a, c, 0xFF9A83FF, 10, 1.7f);
            VfxSpawn.send(sl, VfxShape.THREAD_LINE, a, c, 0xFFD9D2FF, 8, 0.2f);
        } else if (s.nativeId().equals("severance_aria") || s.nativeId().equals("final_page")) {
            Vec3 a = b.position().add(0, 1.1, 0), c = target.getBoundingBox().getCenter();
            VfxSpawn.send(sl, VfxShape.SLASH_WAVE, a, c, s.nativeId().equals("final_page") ? 0xFFF1E9FF : 0xFFB18CFF,
                    s.nativeId().equals("final_page") ? 8 : 10, s.nativeId().equals("final_page") ? 2.4f : 1.7f);
        }
        if (!ranged && !arc && hits.isEmpty() && b.distanceTo(target) <= s.range() * 1.5) hits.add(target);
        if (s.nativeId().equals("island_fall") || s.nativeId().equals("maw_of_the_rift")) {
            Vec3 mark = s.nativeId().equals("island_fall") ? target.position() : b.position();
            if (s.nativeId().equals("island_fall")) {
                VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, mark, mark.add(0, 1, 0), 0xFF7357B8, 24, 3.0f);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, mark.x, mark.y + 0.15, mark.z, 48, 1.3, 0.08, 1.3, 0.01);
            } else {
                VfxSpawn.send(sl, VfxShape.RIVEN_VOID_GATE, b.position(), b.position().add(0, 0.08, 0), 0xFF766DFF, 25, 3f);
                VfxSpawn.send(sl, VfxShape.SHADOW_POOL, mark, mark.add(0, 0.1, 0), 0xFF321544, 20, 3.0f);
                VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, mark, mark.add(0, 0.1, 0), 0xFFB088FF, 10, 2.0f);
            }
        }
        final LivingEntity primary = target;
        Runnable apply = () -> {
            for (LivingEntity h : hits) {
                if (!h.isAlive() || !b.isAlive()) continue;
                if (prim.contains("pull")) pull(b, h);
                if (prim.contains("slow") && b.rewriteAllowed(h)) h.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                if (prim.contains("silence") && h instanceof ServerPlayer p && b.rewriteAllowed(p))
                    p.getPersistentData().putLong("nusmp_sealed_until", p.level().getGameTime() + 60);
                if (prim.contains("magic_damage")) deal(b, s, h, b.damageSources().indirectMagic(b, b), true);
                if (prim.contains("physical_damage")) deal(b, s, h, b.damageSources().mobAttack(b), false);
            }
            if (hits.isEmpty() && primary != null && (prim.contains("magic_damage") || prim.contains("physical_damage"))) b.brain().bounced(s, b.tickCount);
        };
        if (delay > 0) SpellRuntime.later(sl, delay, apply); else apply.run();
    }

    private static boolean specialAttack(RivenBossEntity b, AnimeSkill s, LivingEntity target, ServerLevel sl) {
        switch (s.nativeId()) {
            case "eldritch_verse" -> {
                if (target == null) return true;
                Vec3 origin = b.getEyePosition();
                for (int bolt = 0; bolt < 3; bolt++) {
                    int index = bolt;
                    SpellRuntime.later(sl, RivenCombat.eldritchBoltDelay(index), () -> {
                        if (!b.isAlive() || !target.isAlive()) return;
                        Vec3 from = b.getEyePosition();
                        Vec3 to = target.getBoundingBox().getCenter();
                        VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, from, to, VIOLET, 6, 0.8f);
                        if (b.hasLineOfSight(target) && from.distanceTo(to) <= 22)
                            applyDirect(b, s, target, b.damageSources().indirectMagic(b, b), true,
                                    RivenCombat.damage(b.phase(), false, b.getRandom().nextFloat()));
                    });
                }
                VfxSpawn.send(sl, VfxShape.DARK_SLASH_AVIDYA, origin, target.getBoundingBox().getCenter(), VIOLET, 16, 0.45f);
                return true;
            }
            case "hexblade_waltz" -> {
                if (target == null) return true;
                for (int swing = 0; swing < 3; swing++) {
                    int index = swing;
                    SpellRuntime.later(sl, RivenCombat.hexbladeSwingDelay(index), () -> {
                        if (!b.isAlive() || !target.isAlive() || b.distanceTo(target) > 6.5 || !b.hasLineOfSight(target)) return;
                        VfxShape slash = index == 2 ? VfxShape.DARK_SLASH_DIMENSION : VfxShape.DARK_SLASH_AVIDYA;
                        VfxSpawn.send(sl, slash, b.getEyePosition(), target.getBoundingBox().getCenter(), 0xFFB18CFF, 8, 0.7f);
                        boolean hit = applyDirect(b, s, target, b.damageSources().mobAttack(b), false, RivenCombat.damage(b.phase(), true, 0));
                        if (index == 2 && hit) {
                            boolean popped = popMultilayerBarrier(target);
                            VfxSpawn.send(sl, VfxShape.BARRIER_FX3, target.getBoundingBox().getCenter(),
                                    target.getBoundingBox().getCenter().add(0, 0.2, 0), 0xFFD8C8FF, 8, popped ? 1.35f : 0.8f);
                        }
                    });
                }
                return true;
            }
            case "bull_ward" -> {
                b.startBullWard(600);
                b.setAbsorptionAmount(Math.max(b.getAbsorptionAmount(), 40f));
                b.guardFor(600);
                VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, b, b.position().add(0, 0.05, 0), BLUE, 600, 1.6f);
                return true;
            }
            case "soul_note" -> {
                if (target instanceof ServerPlayer p) {
                    b.startSoulNote(p);
                    VfxSpawn.send(sl, VfxShape.THREAD_LINE, b.getBoundingBox().getCenter(), p.getBoundingBox().getCenter(), VIOLET, 100, 0.3f);
                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Soul Note: damage you deal to Riven will ring back in five seconds.")
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
                }
                return true;
            }
            case "legion_knight" -> {
                if (!b.canManifest()) return true;
                Vec3 old = b.position();
                Vec3 soldierAt = target.position().add(target.getLookAngle().multiply(-2, 0, -2));
                StoryConstructEntity knight = StoryConstructEntity.spawn(sl, b, StoryConstructEntity.CLONE, soldierAt);
                if (knight == null) return true;
                VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, target.position(), target.position().add(0, 1.5, 0), 0xFFFFD66E, 20, 1.1f);
                SpellRuntime.later(sl, 12, () -> {
                    if (!b.isAlive() || !knight.isAlive() || !target.isAlive()) return;
                    Vec3 knightPos = knight.position();
                    b.teleportTo(knightPos.x, knightPos.y, knightPos.z);
                    knight.teleportTo(old.x, old.y, old.z);
                    b.getLookControl().setLookAt(target, 60, 60);
                    if (enemy(target) && b.hasLineOfSight(target)) applyDirect(b, s, target, b.damageSources().mobAttack(b), false,
                            RivenCombat.damage(b.phase(), false, b.getRandom().nextFloat()));
                });
                return true;
            }
            case "discord" -> {
                if (target instanceof ServerPlayer p) {
                    p.getPersistentData().putLong("nusmp_sealed_until", p.level().getGameTime() + 40);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    applyDirect(b, s, p, b.damageSources().indirectMagic(b, b), true, 42f);
                }
                return true;
            }
            case "gold_ring" -> {
                Vec3 center = Vec3.atBottomCenterOf(b.arenaPosition());
                b.startNullWindowForAttack("magic", 60, sl, "Gold Ring: magic is null for three seconds. Leave the ring or strike physically.");
                VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, center, center.add(0, 0.1, 0), 0xFFFFD66E, 60, 16f);
                VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, center, center.add(0, 0.1, 0), 0xFF8265B5, 60, 16f);
                SpellRuntime.later(sl, 20, () -> {
                    for (ServerPlayer p : sl.players()) {
                        if (!enemy(p) || p.distanceToSqr(center) > 16 * 16 || !p.hasLineOfSight(b)) continue;
                        VfxSpawn.send(sl, VfxShape.SLASH_WAVE, center.add(0, 1, 0), p.getBoundingBox().getCenter(), 0xFFFFE3A1, 8, 2.0f);
                        applyDirect(b, s, p, b.damageSources().indirectMagic(b, b), true, RivenCombat.damage(b.phase(), true, 0));
                    }
                });
                return true;
            }
            case "two_moons" -> {
                Vec3 mark = target.position();
                for (int moon = 0; moon < 2; moon++) {
                    int which = moon;
                    SpellRuntime.later(sl, moon == 0 ? 0 : 10, () -> {
                        if (!b.isAlive()) return;
                        Vec3 strike = mark.add(which == 0 ? -2.5 : 2.5, 0, 0);
                        Vec3 sky = strike.add(0, 18, 0);
                        VfxSpawn.send(sl, VfxShape.LIGHTNING_GOD, sky, strike, 0xFFB5A5FF, 8, 0.8f);
                        for (ServerPlayer p : sl.players()) if (enemy(p) && p.distanceToSqr(strike) <= 3.5 * 3.5 && p.hasLineOfSight(b))
                            applyDirect(b, s, p, b.damageSources().indirectMagic(b, b), true, RivenCombat.damage(b.phase(), true, 0));
                    });
                }
                return true;
            }
            case "doom_gate" -> {
                for (ServerPlayer p : sl.players()) {
                    if (!enemy(p) || p.distanceToSqr(b) > 16 * 16) continue;
                    Vec3 delta = b.position().subtract(p.position());
                    double distance = delta.length();
                    if (distance > 6) {
                        p.setDeltaMovement(p.getDeltaMovement().add(delta.normalize().scale(Math.min(1.0, (distance - 6) * 0.12)).add(0, 0.2, 0)));
                        p.hurtMarked = true;
                    }
                }
                VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, b.position(), b.position().add(0, 1.5, 0), VIOLET, 16, 3f);
                SpellRuntime.later(sl, 12, () -> {
                    if (!b.isAlive()) return;
                    VfxSpawn.send(sl, VfxShape.WIND_SLASH, b.getEyePosition(), b.getEyePosition().add(b.getLookAngle().scale(6)), 0xFFB99CFF, 8, 2f);
                    for (ServerPlayer p : sl.players()) if (enemy(p) && p.distanceToSqr(b) <= 6 * 6 && b.hasLineOfSight(p))
                        applyDirect(b, s, p, b.damageSources().indirectMagic(b, b), true, RivenCombat.damage(b.phase(), true, 0));
                });
                return true;
            }
            case "unwritten_ending" -> {
                b.beginUnwrittenEnding(sl);
                return true;
            }
            case "audience_collapse" -> {
                if (!b.startAudienceCollapse()) return true;
                int players = (int) sl.players().stream().filter(p -> enemy(p) && p.distanceToSqr(b) < 48 * 48).count();
                float damage = RivenCombat.audienceDamage(players);
                VfxSpawn.send(sl, VfxShape.FX_SPIRIT_NOVA, b.position(), b.position().add(0, 0.1, 0), 1, 12, 4f);
                for (ServerPlayer p : sl.players()) {
                    if (!enemy(p) || p.distanceToSqr(b) >= 48 * 48) continue;
                    VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, p.position(), p.position().add(0, 0.1, 0), 0xFFB088FF, 8, 1.2f);
                    applyDirect(b, s, p, b.damageSources().indirectMagic(b, b), true, damage);
                }
                return true;
            }
            case "crown_break" -> {
                Vec3[] aims = new Vec3[4];
                for (int i = 0; i < aims.length; i++) aims[i] = target.getBoundingBox().getCenter();
                for (int i = 0; i < aims.length; i++) {
                    Vec3 aim = aims[i];
                    SpellRuntime.later(sl, i * 4, () -> {
                        Vec3 start = b.position().add((b.getRandom().nextDouble() - 0.5) * 1.5, 2.6, (b.getRandom().nextDouble() - 0.5) * 1.5);
                        VfxSpawn.send(sl, VfxShape.EARTH_SPIKES, start, aim, 0xFFFFD66E, 8, 0.65f);
                        for (ServerPlayer p : sl.players()) {
                            if (!enemy(p) || p.getBoundingBox().getCenter().distanceToSqr(aim) > 1.5 * 1.5) continue;
                            Vec3 now = p.getBoundingBox().getCenter();
                            if (sl.clip(new net.minecraft.world.level.ClipContext(start, now,
                                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                    net.minecraft.world.level.ClipContext.Fluid.NONE, b)).getType() != net.minecraft.world.phys.HitResult.Type.BLOCK)
                                applyDirect(b, s, p, b.damageSources().mobAttack(b), false, RivenCombat.damage(b.phase(), false, b.getRandom().nextFloat()));
                        }
                    });
                }
                return true;
            }
            case "rewrite_round" -> {
                if (!b.canRewriteRound()) return true;
                b.startRewriteStartup();
                AnimeSkill reprise = b.lastCastSkill();
                long startupEnds = b.rewriteStartupEndsAt();
                VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, b, b.position().add(0, 1, 0), 0xFF39224F, 12, 1.5f);
                SpellRuntime.later(sl, 12, () -> {
                    if (!b.isAlive() || !target.isAlive() || b.rewriteStartupEndsAt() != startupEnds || b.isStaggeredNow()) {
                        b.cancelRewriteStartup();
                        return;
                    }
                    b.markRewriteRoundUsed();
                    if (reprise != null && !reprise.nativeId().equals("rewrite_round"))
                        execute(b, reprise, target, sl, 1);
                    else
                        applyDirect(b, s, target, b.damageSources().indirectMagic(b, b), true,
                                RivenCombat.damage(b.phase(), false, b.getRandom().nextFloat()));
                });
                return true;
            }
            default -> { return false; }
        }
    }

    static void paySoulNote(RivenBossEntity b, ServerLevel sl) {
        java.util.UUID id = b.soulNoteTarget();
        float amount = b.takeSoulNoteDamage();
        if (id == null) return;
        if (sl.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer p && p.isAlive()) {
            float payout = amount;
            if (payout > 0) {
                VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, p.getBoundingBox().getCenter(), b.getBoundingBox().getCenter(), VIOLET, 12, 0.8f);
                p.hurt(b.damageSources().indirectMagic(b, b), payout);
            }
        }
    }

    static void bullWardBreak(RivenBossEntity b, ServerLevel sl) {
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, b.position(), b.position().add(0, 0.1, 0), BLUE, 10, 4.0f);
        for (ServerPlayer p : sl.players()) {
            if (!enemy(p) || p.distanceToSqr(b) > 5 * 5) continue;
            applyDirect(b, null, p, b.damageSources().mobAttack(b), false, RivenCombat.damage(b.phase(), false, b.getRandom().nextFloat()));
        }
    }

    private static boolean applyDirect(RivenBossEntity b, AnimeSkill skill, LivingEntity victim, DamageSource source, boolean magic, float damage) {
        if (!victim.isAlive()) return false;
        boolean hit = victim.hurt(source, damage);
        if (hit && skill != null && RivenCombat.signature(skill)) b.signatureHit();
        if (hit) {
            Vec3 at = victim.getBoundingBox().getCenter();
            VfxSpawn.send((ServerLevel) b.level(), VfxShape.LIGHT_FLARE, at, at.add(0, 0.2, 0), 0xFFE0D7FF, 4, 0.7f);
            VfxSpawn.send((ServerLevel) b.level(), VfxShape.BARRIER_FX3, at, at.add(0, 0.2, 0), 0xFFE0D7FF, 5,
                    skill != null && RivenCombat.signature(skill) ? 1.35f : 0.75f);
            if (skill != null && RivenCombat.signature(skill)) {
                VfxSpawn.send((ServerLevel) b.level(), VfxShape.DARK_SLASH_DIMENSION, b.getEyePosition(), at, 0xFF8F76FF, 10, 0.55f);
            }
        }
        return hit;
    }

    private static boolean popMultilayerBarrier(LivingEntity target) {
        var attribute = target.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MULTILAYER_BARRIER);
        if (attribute == null || attribute.getValue() <= 0) return false;
        for (String path : List.of("multilayer_barrier", "ally_multilayer_barrier")) {
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tensura", path);
            var modifier = attribute.getModifier(id);
            if (modifier == null || modifier.amount() <= 0) continue;
            if (modifier.amount() <= 1) attribute.removeModifier(modifier.id());
            else attribute.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    modifier.id(), modifier.amount() - 1, modifier.operation()));
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- damage
    static float damage(RivenBossEntity b, AnimeSkill s, LivingEntity victim) {
        return RivenCombat.damage(b.phase(), RivenCombat.signature(s), b.getRandom().nextFloat());
    }

    private static void deal(RivenBossEntity b, AnimeSkill s, LivingEntity h, DamageSource src, boolean magic) {
        float before = h.getHealth() + h.getAbsorptionAmount();
        applyDirect(b, s, h, src, magic, damage(b, s, h));
        float dealt = before - (h.getHealth() + h.getAbsorptionAmount());
        if (dealt > 0 && RivenCombat.signature(s) && b.level() instanceof ServerLevel sl)
            sl.playSound(null, h.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.0f, 1.5f);
        if (dealt < 0.5f) b.brain().bounced(s, b.tickCount);
    }

    /** Players the bolt from a to b passes through: the target and anyone standing in the line. */
    private static List<LivingEntity> along(RivenBossEntity b, Vec3 a, Vec3 c, double range) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 ab = c.subtract(a);
        double len = Math.max(1e-6, ab.lengthSqr());
        if (ab.length() > range * 1.3) return out;
        for (Player p : b.level().getEntitiesOfClass(Player.class, new AABB(a, c).inflate(1.5), e -> enemy(e))) {
            Vec3 m = p.getBoundingBox().getCenter();
            double t = Math.max(0, Math.min(1, m.subtract(a).dot(ab) / len));
            if (a.add(ab.scale(t)).distanceTo(m) <= 1.1 + p.getBbWidth() / 2 && b.hasLineOfSight(p)) out.add(p);
        }
        return out;
    }

    private static List<LivingEntity> arc(RivenBossEntity b, double reach) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 look = b.getLookAngle().multiply(1, 0, 1).normalize();
        for (Player p : b.level().getEntitiesOfClass(Player.class, b.getBoundingBox().inflate(reach, 1.5, reach), RivenAttacks::enemy)) {
            Vec3 d = p.position().subtract(b.position()).multiply(1, 0, 1);
            if (d.length() <= reach && (d.length() < 1.2 || d.normalize().dot(look) > 0.35)) out.add(p);
        }

        return out;
    }

    private static List<LivingEntity> cone(RivenBossEntity b, double reach, double halfWidth) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 look = b.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0e-5) return out;
        look = look.normalize();
        for (Player p : b.level().getEntitiesOfClass(Player.class, b.getBoundingBox().inflate(reach, 2, reach), RivenAttacks::enemy)) {
            Vec3 delta = p.position().subtract(b.position()).multiply(1, 0, 1);
            double distance = delta.length();
            if (distance > 0.1 && distance <= reach && delta.normalize().dot(look) >= 1.0 - halfWidth)
                out.add(p);
        }
        return out;
    }

    static boolean enemy(LivingEntity e) {
        return e instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    private static void pull(RivenBossEntity b, LivingEntity h) {
        Vec3 d = b.position().subtract(h.position()).normalize().scale(1.1);
        h.setDeltaMovement(h.getDeltaMovement().add(d.x, 0.25, d.z));
        h.hurtMarked = true;
    }

    // ---------------------------------------------------------------- self and area effects
    private static void blink(RivenBossEntity b, AnimeSkill s, LivingEntity target, ServerLevel sl) {
        Vec3 old = b.position();
        if (target == null) return;
        boolean closing = s.has("melee_arc");
        Vec3 away = old.subtract(target.position()).multiply(1, 0, 1);
        Vec3 dir = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
        for (int i = 0; i < 8; i++) {
            double ang = (i - 3.5) * 0.35 + (b.getRandom().nextDouble() - 0.5) * 0.3;
            Vec3 d = dir.yRot((float) ang);
            Vec3 at = closing ? target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(2.0)) : target.position().add(d.scale(7.5));
            if (!sl.noCollision(b, b.getBoundingBox().move(at.subtract(b.position())))) continue;
            VfxSpawn.send(sl, VfxShape.MIRROR_PANE, old.add(0, 1, 0), old.add(0, 2, 0), VIOLET, 18, 1.0f);
            b.teleportTo(at.x, at.y, at.z);
            sl.playSound(null, b.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 1.4f);
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), BLUE, 12, 0.6f);
            afterimage(b, old, target, sl);
            return;
        }
    }

    /** The afterimage repeats Page Tear from where he blinked, at half signature damage. */
    private static void afterimage(RivenBossEntity b, Vec3 at, LivingEntity target, ServerLevel sl) {
        SpellRuntime.later(sl, 14, () -> {
            if (!b.isAlive() || !target.isAlive() || at.distanceTo(target.position()) > 28) return;
            Vec3 from = at.add(0, 1.6, 0), to = target.getBoundingBox().getCenter();
            VfxSpawn.send(sl, VfxShape.DARK_SLASH_AVIDYA, from, to, VIOLET, 10, 0.9f);
            VfxSpawn.send(sl, VfxShape.WIND_SLASH, from, to, VIOLET, 8, 1.1f);
            Vec3 look = to.subtract(from).multiply(1, 0, 1).normalize();
            for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(at, at.add(look.scale(6))).inflate(2, 1.5, 2), RivenAttacks::enemy)) {
                Vec3 delta = p.position().subtract(at).multiply(1, 0, 1);
                if (delta.length() > 6 || delta.lengthSqr() < 1e-5 || delta.normalize().dot(look) < 0.65
                        || sl.clip(new net.minecraft.world.level.ClipContext(from, p.getBoundingBox().getCenter(),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, b))
                        .getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) continue;
                applyDirect(b, null, p, b.damageSources().indirectMagic(b, b), true,
                        RivenCombat.damage(b.phase(), true, 0) * 0.5f);
            }
        });
    }

    private static void songBuff(RivenBossEntity b, ServerLevel sl) {
        b.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
        b.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        b.markSongBuff(120);
        for (StoryConstructEntity c : sl.getEntitiesOfClass(StoryConstructEntity.class, b.getBoundingBox().inflate(16), e -> e.ownedBy(b.getUUID()))) {
            c.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
            c.heal(6f);
        }
        notes(b, sl, 1.0f);
    }

    private static void songDebuff(RivenBossEntity b, ServerLevel sl) {
        for (Player p : sl.getEntitiesOfClass(Player.class, b.getBoundingBox().inflate(22), RivenAttacks::enemy)) {
            if (!b.rewriteAllowed(p)) continue;
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
        notes(b, sl, 0.6f);
    }

    private static void notes(RivenBossEntity b, ServerLevel sl, float pitch) {
        sl.sendParticles(ParticleTypes.NOTE, b.getX(), b.getY() + 2.1, b.getZ(), 12, 1.2, 0.5, 1.2, 1.0);
        for (int i = 0; i < 4; i++) {
            float p = pitch + i * 0.18f;
            SpellRuntime.later(sl, i * 5, () -> sl.playSound(null, b.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.HOSTILE, 2.0f, p));
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 2.0f, pitch);
    }

    private static void heal(RivenBossEntity b, ServerLevel sl) {
        b.heal(b.getMaxHealth() * 0.04f);
        for (StoryConstructEntity c : sl.getEntitiesOfClass(StoryConstructEntity.class, b.getBoundingBox().inflate(16), e -> e.ownedBy(b.getUUID()))) c.heal(8f);
        sl.sendParticles(ParticleTypes.HEART, b.getX(), b.getY() + 1.8, b.getZ(), 6, 0.5, 0.4, 0.5, 0.0);
    }

    private static void shield(RivenBossEntity b, ServerLevel sl) {
        b.setAbsorptionAmount(Math.max(b.getAbsorptionAmount(), 10f));
        b.guardFor(60);
        VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, b, b.position().add(0, 0.05, 0), BLUE, 40, 1.0f);
    }

    private static void summon(RivenBossEntity b, AnimeSkill s, ServerLevel sl) {
        if (!b.canManifest()) return;
        int kind = switch (s.constructKind()) { case "shield" -> StoryConstructEntity.SHIELD; case "clone" -> StoryConstructEntity.CLONE; default -> StoryConstructEntity.WEAPON; };
        Vec3 look = b.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 at = b.position().add(look.scale(2.0)).add(look.yRot(1.57f).scale((b.getRandom().nextDouble() - 0.5) * 3));
        StoryConstructEntity.spawn(sl, b, kind, at);
    }

    private static void copy(RivenBossEntity b, AnimeSkill s, LivingEntity target, ServerLevel sl, int depth) {
        List<AnimeSkill> pool = new ArrayList<>();
        for (AnimeSkill o : AnimeSkillCodex.all()) if (o != s && o.tier() <= s.tier() + 1 && !o.has("copy_codex_skill") && o.lethal()) pool.add(o);
        if (pool.isEmpty()) return;
        AnimeSkill pick = pool.get(b.getRandom().nextInt(pool.size()));
        b.say(sl, "\"" + pick.name() + "\" — borrowed from " + pick.anime().replace('_', ' ') + ".");
        execute(b, pick, target, sl, depth + 1);
    }

    /** First-party pre-hooks the codex can name. */
    private static void hook(RivenBossEntity b, AnimeSkill s, LivingEntity target, ServerLevel sl) {
        switch (s.nativeId()) {
            case "hex" -> { if (target != null) b.hex(target, 200); }
            case "soul_bond" -> {
                if (target instanceof Player p && b.rewriteAllowed(p)) {
                    b.bond(p, 160);
                    VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, b.getBoundingBox().getCenter(), p.getBoundingBox().getCenter(), VIOLET, 40, 0.8f);
                } else if (target != null) b.say(sl, "It won't take... they don't believe.");
            }
            case "inspire" -> {
                b.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
                b.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
                b.addStory(10);
                notes(b, sl, 1.2f);
            }
            case "page_flip" -> {
                List<AnimeSkill> pages = new ArrayList<>();
                for (AnimeSkill o : AnimeSkillCodex.all()) if ("black_clover".equals(o.anime()) && o.lethal() && !"page_flip".equals(o.nativeId())) pages.add(o);
                if (!pages.isEmpty() && target != null) {
                    AnimeSkill pick = pages.get(b.getRandom().nextInt(pages.size()));
                    sl.playSound(null, b.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 1.5f, 1.0f);
                    execute(b, pick, target, sl, 1);
                }
            }
            default -> { }
        }
    }
}
