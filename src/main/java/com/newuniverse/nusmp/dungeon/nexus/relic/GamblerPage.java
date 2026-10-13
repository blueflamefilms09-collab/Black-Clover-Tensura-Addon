package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import com.newuniverse.nusmp.dungeon.sentient.ImmuneSystem;
import com.newuniverse.nusmp.dungeon.sentient.SentientMode;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Gambler's Grimoire Page (docs/living_dungeon.md section 7). Worn in a Dungeon Relic slot and attuned (or held in the off-hand), using it
 * casts a random, overpowered spell from the curated list {@link PageSpell}: from a slot with the Use Relic key ({@link RelicNet}), from the hand
 * by using the item. The spells are direct effects, not a real grimoire page: a real page needs the caster's own
 * grimoire, the right magic and an unsealed book, and the page must work for any mage. Every cast is a second of wind-up (the page flares,
 * the outcome is still hidden), then the spell shows where it will land ({@link PageSpell#telegraph} ticks) and lands, with damage through
 * the balance law and no change to any block. The page rests for a minute after every use.
 *
 * <p>Its curse, Wild Magic Surge: every use rolls a hidden d20 ({@link WildSurge}); on a 1 there is no spell: the page backfires, a tenth of
 * the user's mana pool is lost until the day ends ({@link SurgeHandler}: it survives a relog and a death), and inside the Dungeon its immune
 * system is set on the user at once ({@code ImmuneSystem.aggro}: a Juggernaut is born from the walls). With the Sentient Dungeon's rules off
 * ({@code dungeon.sentient = false}) the 1.21.0 consequence stays instead: a {@link DungeonCollapseEvent} at the user's position. Outside the
 * Dungeon only the mana penalty and a harmless burst.
 */
final class GamblerPage {
    private GamblerPage() {}

    private static final int GOLD = 0xFFFFD36B, VIOLET = 0xFF9B6BFF, RED = 0xFFFF3030, BONE = 0xFFE8E0C8, BOLT = 0xFFBFD8FF;
    private static final int HIT_CAP = 16;

    /** The page is used. */
    static void use(ServerPlayer p, ItemStack page) {
        if (!Worn.has(p, Relic.GAMBLERS_GRIMOIRE_PAGE)) {
            p.displayClientMessage(Component.literal("The page stays blank: attune to it first (hold it in the main hand, sneak and stand still for 10 s), "
                    + "then wear it in a Dungeon Relic slot (you may wear two relics).").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        Long ready = page.get(RelicComponents.READY_AT.get());
        long readyAt = ready == null ? 0L : ready;
        if (!WildSurge.ready(now, readyAt)) {
            p.displayClientMessage(Component.literal("The page is still smouldering (" + WildSurge.secondsLeft(now, readyAt) + " s).").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        page.set(RelicComponents.READY_AT.get(), WildSurge.readyAt(now));
        p.getCooldowns().addCooldown(page.getItem(), WildSurge.COOLDOWN_TICKS);
        int roll = WildSurge.roll(p.getRandom()::nextInt);                        // hidden: nothing says what it was
        PageSpell spell = PageSpell.pick(p.getRandom()::nextInt);
        VfxSpawn.sendFollowing(level, VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 0.08, 0), GOLD, WildSurge.WINDUP_TICKS + 4, 2.0f);
        level.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.6f, 0.8f);
        p.displayClientMessage(Component.literal("The page flares in your hand...").withStyle(ChatFormatting.GOLD), true);
        SpellRuntime.later(level, WildSurge.WINDUP_TICKS, () -> {
            if (!p.isAlive() || p.isRemoved() || p.serverLevel() != level) return;
            if (WildSurge.backfires(roll)) backfire(p, level);
            else land(p, level, spell);
        });
    }

    // ---------------------------------------------------------------- the spells
    private static void land(ServerPlayer p, ServerLevel level, PageSpell s) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        Vec3 point = aim(level, p, eye, look, s.range);
        p.displayClientMessage(Component.literal(s.label + "!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        switch (s) {
            case DARK_CLOAKED_DIMENSION_SLASH -> dimensionSlash(p, level, s, eye, point);
            case GRAVITY_CRUSH -> gravityCrush(p, level, s, point);
            case SPATIAL_RIFT_BARRAGE -> riftBarrage(p, level, s, point);
            case METEOR_OF_BONES -> boneMeteor(p, level, s, point);
            case JUDGEMENT_PILLAR -> pillar(p, level, s, point);
        }
    }

    /** The first solid thing along the look, or the end of the reach. */
    private static Vec3 aim(ServerLevel level, ServerPlayer p, Vec3 eye, Vec3 look, double range) {
        Vec3 end = eye.add(look.scale(range));
        HitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return wall.getType() != HitResult.Type.MISS ? wall.getLocation() : end;
    }

    private static boolean here(ServerPlayer p, ServerLevel level) { return p.isAlive() && !p.isRemoved() && p.serverLevel() == level; }

    private static void hit(ServerPlayer p, LivingEntity t, float raw) {
        t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, raw, 0.9));
    }

    /** An enormous cut through everything along the look: a red line shows its path first, then the dimension tears. */
    private static void dimensionSlash(ServerPlayer p, ServerLevel level, PageSpell s, Vec3 eye, Vec3 point) {
        Vec3 from = eye.add(0, -0.45, 0);
        VfxSpawn.send(level, VfxShape.THREAD_LINE, from, point, RED, s.telegraph, 1.6f);
        SpellRuntime.later(level, s.telegraph, () -> {
            if (!here(p, level)) return;
            VfxSpawn.send(level, VfxShape.DARK_SLASH_DIMENSION, from, point, VIOLET, 18, 2.8f);
            level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.3f);
            int n = 0;
            for (LivingEntity t : GrimoireBook.along(p, from, point, s.size)) {
                if (n++ >= HIT_CAP) break;
                hit(p, t, s.raw);
            }
        });
    }

    /** A black hole opens where the caster looks, drags everything in for a moment, and crushes it. */
    private static void gravityCrush(ServerPlayer p, ServerLevel level, PageSpell s, Vec3 point) {
        VfxSpawn.send(level, VfxShape.MAGIC_CIRCLE, point.add(0, 0.1, 0), point.add(0, 1.0, 0), VIOLET, s.telegraph + 4, (float) s.size * 0.4f);
        VfxSpawn.send(level, VfxShape.DARK_BLACK_HOLE, point.add(0, 1.6, 0), point, VIOLET, s.telegraph, (float) s.size * 0.4f);
        SpellRuntime.zone(level, s.telegraph, 2, age -> {
            if (!here(p, level)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, point, s.size * 1.25)) {
                if (n++ >= HIT_CAP) break;
                Vec3 pull = point.subtract(t.position());
                if (pull.lengthSqr() < 1.0) continue;
                t.setDeltaMovement(t.getDeltaMovement().add(pull.normalize().scale(0.12)));
                t.hurtMarked = true;
            }
        });
        SpellRuntime.later(level, s.telegraph, () -> {
            if (!here(p, level)) return;
            VfxSpawn.send(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, point, point, VIOLET, 14, (float) s.size * 0.3f);
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, point, s.size)) {
                if (n++ >= HIT_CAP) break;
                hit(p, t, s.raw);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
        });
    }

    /** Seven rifts open round the caster, then each throws a bolt at a foe in reach, one after another. */
    private static void riftBarrage(ServerPlayer p, ServerLevel level, PageSpell s, Vec3 point) {
        int rifts = 7;
        for (int i = 0; i < rifts; i++) {
            double a = i * (Math.PI * 2.0 / rifts);
            Vec3 at = p.position().add(Math.cos(a) * 3.2, 2.0 + 0.3 * (i % 2), Math.sin(a) * 3.2);
            int delay = s.telegraph + 3 * i, index = i;
            VfxSpawn.send(level, VfxShape.SPATIAL_RIFT, at, at.add(0, 0, 1), VIOLET, delay + 8, 1.1f);
            SpellRuntime.later(level, delay, () -> {
                if (!here(p, level)) return;
                List<LivingEntity> foes = foes(p, s.range);
                Vec3 to = foes.isEmpty() ? point : foes.get(index % foes.size()).getBoundingBox().getCenter();
                VfxSpawn.send(level, VfxShape.THREAD_LINE, at, to, GOLD, 6, 1.2f);
                VfxSpawn.send(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, to, to, VIOLET, 8, 0.4f);
                level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.2f + 0.1f * index);
                if (foes.isEmpty()) return;
                hit(p, foes.get(index % foes.size()), s.raw);
            });
        }
    }

    /** Creatures in reach that are hostile, of the Dungeon's roster, or already after the caster, nearest first. */
    private static List<LivingEntity> foes(ServerPlayer p, double range) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : GrimoireBook.around(p, p.position(), range)) {
            if (t instanceof Enemy || (t instanceof Mob m && (m.getTarget() == p || Wildlife.tierOf(m) > 0))) out.add(t);
        }
        out.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
        return out;
    }

    /** A meteor of bone falls out of the dark on the place the caster looks at; the danger zone is drawn on the ground until it lands. */
    private static void boneMeteor(ServerPlayer p, ServerLevel level, PageSpell s, Vec3 point) {
        Vec3 sky = point.add((p.getRandom().nextDouble() - 0.5) * 6.0, 26.0, (p.getRandom().nextDouble() - 0.5) * 6.0);
        VfxSpawn.send(level, VfxShape.BONE_FX2, point.add(0, 0.1, 0), point, BONE, s.telegraph + 6, (float) s.size);
        VfxSpawn.send(level, VfxShape.DEMON_METEOR, sky, point, BONE, s.telegraph, 2.0f);
        SpellRuntime.later(level, s.telegraph, () -> {
            if (!here(p, level)) return;
            VfxSpawn.send(level, VfxShape.BONE_FX3, point, point, BONE, 16, 1.2f);
            level.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y + 0.5, point.z, 3, s.size * 0.3, 0.3, s.size * 0.3, 0.0);
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.7f);
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, point, s.size)) {
                if (n++ >= HIT_CAP) break;
                hit(p, t, s.raw);
                t.knockback(0.8, point.x - t.getX(), point.z - t.getZ());
            }
        });
    }

    /** A ring of light on the ground, then a pillar of lightning. */
    private static void pillar(ServerPlayer p, ServerLevel level, PageSpell s, Vec3 point) {
        VfxSpawn.send(level, VfxShape.MAGIC_CIRCLE, point.add(0, 0.1, 0), point.add(0, 1.0, 0), GOLD, s.telegraph + 2, (float) s.size * 0.5f);
        SpellRuntime.later(level, s.telegraph, () -> {
            if (!here(p, level)) return;
            VfxSpawn.send(level, VfxShape.LIGHTNING_GOD, point, point, BOLT, 22, (float) s.size);
            level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.6f, 1.0f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 1.0, point.z, 30, s.size * 0.4, 1.0, s.size * 0.4, 0.2);
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, point, s.size)) {
                if (n++ >= HIT_CAP) break;
                hit(p, t, s.raw);
            }
        });
    }

    // ---------------------------------------------------------------- Wild Magic Surge
    /**
     * A 1: no spell. A tenth of the mana pool is drained for the day, and inside the Dungeon its immune system turns on the user (the Sentient
     * Dungeon), or, with those rules off, its structure starts to come down (1.21.0).
     */
    private static void backfire(ServerPlayer p, ServerLevel level) {
        Vec3 c = p.position().add(0, 1.0, 0);
        VfxSpawn.send(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, c, c, RED, 16, 2.2f);
        VfxSpawn.send(level, VfxShape.DARK_BLACK_HOLE, c.add(0, 0.6, 0), c, RED, 16, 1.4f);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.4f);
        level.playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.6f);
        SurgeHandler.start(p);
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null) {
            ex.setMagicule(Math.min(ex.getMagicule(), EnergyHelper.getMaxMagicule(p)));
            ex.markDirty();
        }
        boolean dungeon = LivingDungeon.in(level);
        boolean hunted = dungeon && provoke(p, level);
        p.displayClientMessage(Component.literal("WILD MAGIC SURGE! The page tears itself apart: a tenth of your mana pool is drained until the day ends"
                + (hunted ? ", and the Dungeon's immune system turns on you!" : dungeon ? ", and the Dungeon shudders around you." : "."))
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }

    /**
     * What the Dungeon does about a 1: with the Sentient Dungeon's rules on, its immune system takes the user for a lethal threat now (a Juggernaut
     * is born from the walls; true if one is coming). With them off, or if that package is absent, the 1.21.0 consequence: a Treasury Collapse
     * event at the user's position. Never throws, so the page cannot break on it.
     */
    private static boolean provoke(ServerPlayer p, ServerLevel level) {
        boolean sentient;
        try {
            sentient = SentientMode.active();
        } catch (LinkageError | RuntimeException absent) {
            sentient = false;
        }
        if (!sentient) {
            NeoForge.EVENT_BUS.post(new DungeonCollapseEvent(level, p.blockPosition(), Relics.zoneAt(level, p.getBlockX(), p.getBlockZ()), p, false));
            return false;
        }
        try {
            return ImmuneSystem.aggro(p, "wild_surge");
        } catch (LinkageError | RuntimeException failed) {
            return false;
        }
    }
}
