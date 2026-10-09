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

    // ---------------------------------------------------------------- damage
    static float damage(RivenBossEntity b, AnimeSkill s, LivingEntity victim) {
        return RivenCombat.damage(b.phase(), RivenCombat.signature(s), b.getRandom().nextFloat());
    }

    private static void deal(RivenBossEntity b, AnimeSkill s, LivingEntity h, DamageSource src, boolean magic) {
        float before = h.getHealth() + h.getAbsorptionAmount();
        boolean hit = h.hurt(src, damage(b, s, h));
        float dealt = before - (h.getHealth() + h.getAbsorptionAmount());
        if (hit && dealt > 0 && b.level() instanceof ServerLevel sl) {
            Vec3 impact = h.getBoundingBox().getCenter();
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, impact, impact.add(0, 0.2, 0), RivenCombat.signature(s) ? 0xFFE0D7FF : BLUE,
                    RivenCombat.signature(s) ? 5 : 3, RivenCombat.signature(s) ? 0.75f : 0.4f);
            if (RivenCombat.signature(s)) {
                b.signatureHit();
                sl.playSound(null, h.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.0f, 1.5f);
            }
        }
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

    /** The afterimage left behind casts once: a weak eldritch blast fired from where he stood. */
    private static void afterimage(RivenBossEntity b, Vec3 at, LivingEntity target, ServerLevel sl) {
        SpellRuntime.later(sl, 14, () -> {
            if (!b.isAlive() || !target.isAlive() || at.distanceTo(target.position()) > 28) return;
            Vec3 from = at.add(0, 1.6, 0), to = target.getBoundingBox().getCenter();
            VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, from, to, VIOLET, 8, 0.9f);
            if (b.level().getEntity(target.getId()) instanceof LivingEntity t && enemy(t) && b.hasLineOfSight(t))
                t.hurt(b.damageSources().indirectMagic(b, b), (float) b.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45f);
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
