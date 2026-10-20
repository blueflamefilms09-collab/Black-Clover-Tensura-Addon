package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: wave 23's act bodies, the server side of every sneak-click and worn-relic key of the bows, the lute, the lyre, the ring and the horn. The core has
 * already checked attunement, paid the mana, set the cooldown and the daily use counter before a body runs. The numbers are in {@link Wave23Rules}; the
 * rules that need the game bus (arrows that land, spells swallowed, the horn's tally) are in {@link Wave23Events}. Nothing here edits a block: the
 * Dragon's Sight only looks at them, so the acts are safe in the Labyrinth.
 */
final class Wave23Acts {
    private Wave23Acts() {}

    // ---------------------------------------------------------------- persistent-data keys (all on the player unless said otherwise)
    static final String K_SENTENCE = "nusmp_w23_sentence_until", K_PROT_USED = "nusmp_w23_prot_used", K_PROT_REST = "nusmp_w23_prot_rest",
            K_RHYTHM = "nusmp_w23_rhythm", K_CHORD_DAY = "nusmp_w23_chord_day" /* on the target */, K_LYRE = "nusmp_w23_lyre_charges",
            K_HORN_UNTIL = "nusmp_w23_horn_until", K_HORN_KILLS = "nusmp_w23_horn_kills", K_CHARISMA = "nusmp_w23_charisma",
            K_BOON = "nusmp_w23_boon", K_BOON_DAY = "nusmp_w23_boon_day";

    private static final int GOLD = 0xFFD070, RED = 0xC02838, TEAL = 0x4FD8D0, PINK = 0xE04888, VIOLET = 0x8A60E0, WHITE = 0xF4F0FF;

    // ================================================================ shared helpers
    static void note(ServerPlayer p, String text, ChatFormatting colour) { Wave0Util.note(p, text, colour); }

    static void spark(ServerLevel l, Vec3 at, int rgb, int n) { com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_23, FxRelic.tone(rgb), at.x, at.y, at.z, n, 0.3, 0.4, 0.3, 0.05); }

    /** A line of Rukh from one point to another (one call: the particles fly along it). */
    static void beam(ServerLevel l, Vec3 from, Vec3 to, int rgb) {
        if (from.distanceToSqr(to) < 0.04) return;
        com.newuniverse.nusmp.particle.fx.Fx.stream(l, FxThemes.RELIC_WAVE_23, from, to, FxRelic.tone(rgb), 6);
        spark(l, to, rgb, 8);
    }

    static boolean fiendOrUndead(LivingEntity e) {
        if (e.getType().is(EntityTypeTags.UNDEAD)) return true;
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return key != null && Wave23Rules.isFiendOrUndead(key.getPath());
    }

    /** What a look along the player's sight found: the creature (or null) and where the line ended. */
    record Aim(LivingEntity target, Vec3 end) {}

    /** The nearest creature along the look direction within {@code range} (blocks stop the line unless {@code throughWalls}). */
    static Aim aim(ServerPlayer p, double range, boolean throughWalls, Predicate<LivingEntity> ok) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 far = eye.add(p.getLookAngle().scale(range));
        if (!throughWalls) {
            BlockHitResult blocks = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (blocks.getType() != HitResult.Type.MISS) far = blocks.getLocation();
        }
        AABB box = new AABB(eye, far).inflate(1.0);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        Vec3 bestAt = far;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive() && !x.isSpectator() && !(x instanceof ArmorStand) && ok.test(x))) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, far);
            if (hit.isEmpty()) continue;
            double d = hit.get().distanceToSqr(eye);
            if (d < bestD) {
                bestD = d;
                best = e;
                bestAt = hit.get();
            }
        }
        return new Aim(best, best == null ? far : bestAt);
    }

    /** True for an ally of the player: another player they may not harm, a pet, a summon of theirs, a team mate. */
    static boolean ally(ServerPlayer p, LivingEntity x) {
        if (x == p || !x.isAlive() || x.isSpectator() || x instanceof ArmorStand) return false;
        if (x instanceof Player o) return !o.isCreative() && (x.isAlliedTo(p) || !p.canHarmPlayer(o));
        if (x instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return true;
        if (x.getPersistentData().hasUUID("nusmp_relic_owner") && p.getUUID().equals(x.getPersistentData().getUUID("nusmp_relic_owner"))) return true;
        return x.isAlliedTo(p);
    }

    private static Vec3 centre(LivingEntity e) { return e.position().add(0, e.getBbHeight() * 0.6, 0); }

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.25, 0); }

    // ================================================================ Divine Mercy
    /** The Sentence: the next arrow that lands on a creature of 100 hit points or fewer sentences it (once a day). */
    static void divineSentence(ActCtx c) {
        ServerPlayer p = c.player();
        p.getPersistentData().putLong(K_SENTENCE, p.level().getGameTime() + Wave23Rules.SENTENCE_ARMED_TICKS);
        note(p, "Divine Mercy: your next arrow carries the Sentence (30 s).", ChatFormatting.GOLD);
        spark(p.serverLevel(), p.position().add(0, 1.4, 0), GOLD, 12);
        com.newuniverse.nusmp.particle.fx.Fx.gather(p.serverLevel(), FxThemes.RELIC_WAVE_23, p, FxTone.LIGHT, 10);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.4f);
    }

    // ================================================================ Embrasure
    /** "Protect me!": a magical shield round the bow for one round (6 s): resistance and a little absorption. Two uses, then a short rest (four minutes). */
    static void protectMe(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        Wave23Rules.Charges ch = new Wave23Rules.Charges(d.getInt(K_PROT_USED), d.getLong(K_PROT_REST));
        if (!ch.canUse(now)) {
            note(p, "Embrasure needs a short rest: " + (ch.restLeft(now) / 20) + " s.", ChatFormatting.GRAY);
            return;
        }
        Wave23Rules.Charges after = ch.use(now);
        d.putInt(K_PROT_USED, after.used());
        d.putLong(K_PROT_REST, after.restAt());
        Fx.addEffect(p, "minecraft:resistance", Wave23Rules.PROTECT_TICKS, 0);
        Fx.shield(p, Wave23Rules.PROTECT_ABSORB, Wave23Rules.PROTECT_TICKS);
        Fx.addEffect(p, "minecraft:speed", Wave23Rules.PROTECT_TICKS, 0);
        Fx.say(p, "Protect me!  (" + (Wave23Rules.PROTECT_USES - after.used()) + " of " + Wave23Rules.PROTECT_USES + " left before a rest)");
        com.newuniverse.nusmp.particle.fx.Fx.aura(p.serverLevel(), FxThemes.RELIC_WAVE_23, p, FxTone.LIGHT);
        spark(p.serverLevel(), p.position().add(0, 1.2, 0), 0x8AE070, 14);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    // ================================================================ The Dragon's Spirit
    /** Dragon's Sight: an ethereal arrow to the point you look at. Within six blocks of it everything is revealed (Glowing, invisible things included) and traps are marked. */
    static void dragonsSight(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        HitResult hit = p.pick(Wave23Rules.SIGHT_RANGE, 1.0f, false);
        Vec3 at = hit.getLocation();
        beam(level, hand(p), at, TEAL);
        double r = Wave23Rules.SIGHT_RADIUS;
        int seen = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r), x -> x != p && x.isAlive() && !(x instanceof ArmorStand) && x.position().distanceToSqr(at) <= r * r)) {
            if (e instanceof Player o && (o.isCreative() || o.isSpectator())) continue;
            Fx.addEffect(e, "minecraft:glowing", Wave23Rules.SIGHT_TICKS, 0);
            if (e.isInvisible()) e.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
            if (++seen >= 24) break;
        }
        int traps = 0;
        BlockPos centre = BlockPos.containing(at);
        int ri = (int) Math.ceil(r);
        for (BlockPos bp : BlockPos.betweenClosed(centre.offset(-ri, -ri, -ri), centre.offset(ri, ri, ri))) {
            if (bp.distToCenterSqr(at) > r * r) continue;
            var key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(bp).getBlock());
            if (key == null || !Wave23Rules.isTrapBlock(key.getPath())) continue;
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_23, FxTone.DARK, bp.getX() + 0.5, bp.getY() + 0.8, bp.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
            if (++traps >= 24) break;
        }
        Fx.addEffect(p, "minecraft:night_vision", Wave23Rules.SIGHT_TICKS, 0);          // the truesight itself
        Fx.addEffect(p, "minecraft:haste", Wave23Rules.SIGHT_TICKS, 0);                 // "advantage on disarming" = quick hands
        Fx.lendSkill(p, "tensura:eye_of_truth", Wave23Rules.SIGHT_TICKS);
        note(p, "Dragon's Sight: " + seen + " creature(s) revealed, " + traps + " trap(s) marked.", ChatFormatting.AQUA);
    }

    /** Dragon's Claw: three ethereal arrows in the time of one, each at half strength, at up to three foes in sight (fewer foes take more than one). */
    static void dragonsClaw(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> seen = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(Wave23Rules.CLAW_RANGE), x -> Wave0Util.targetable(p, x))) {
            Vec3 to = centre(e).subtract(eye);
            double len = to.length();
            if (len < 0.5 || len > Wave23Rules.CLAW_RANGE || to.scale(1.0 / len).dot(look) < 0.82 || !p.hasLineOfSight(e)) continue;
            seen.add(e);
        }
        if (seen.isEmpty()) {
            note(p, "Dragon's Claw: no target in sight.", ChatFormatting.GRAY);
            return;
        }
        seen.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        for (int i = 0; i < Wave23Rules.CLAW_TARGETS; i++) {
            LivingEntity t = seen.get(i % seen.size());
            if (!t.isAlive()) continue;
            beam(level, hand(p).add(look.cross(new Vec3(0, 1, 0)).scale((i - 1) * 0.25)), centre(t), TEAL);
            t.invulnerableTime = 0;                                                     // three arrows at one foe all land
            Fx.damage(p, t, Wave23Rules.CLAW_DAMAGE, "minecraft:arrow");
        }
        level.playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.6f);
    }

    /** Dragonstrike: the spirit of the dragon flies 20 blocks along your sight, one block wide, through walls. 4d6 force (half on a save). */
    static void dragonstrike(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        Vec3 from = hand(p), look = p.getLookAngle();
        Vec3 to = from.add(look.scale(Wave23Rules.STRIKE_LENGTH));
        for (int i = 0; i <= 8; i++) {
            Vec3 at = from.add(look.scale(Wave23Rules.STRIKE_LENGTH * i / 8.0));
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_23, FxTone.LIGHT, at.x, at.y, at.z, 6, 0.25, 0.25, 0.25, 0.04);
        }
        com.newuniverse.nusmp.particle.fx.Fx.stream(level, FxThemes.RELIC_WAVE_23, from, to, FxTone.LIGHT, 6);
        int hit = 0;
        AABB box = new AABB(from, to).inflate(2.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> Wave0Util.fair(p, x))) {
            if (hit >= 24) break;
            Optional<Vec3> in = e.getBoundingBox().inflate(Wave23Rules.STRIKE_HALF_WIDTH).clip(from, to);
            if (in.isEmpty() && !e.getBoundingBox().inflate(Wave23Rules.STRIKE_HALF_WIDTH).contains(from)) continue;
            boolean saved = Wave23Rules.strikeSaved(p.getRandom().nextDouble());
            Fx.damage(p, e, Wave23Rules.strikeDamage(saved), "minecraft:magic");
            spark(level, centre(e), TEAL, 10);
            hit++;
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.6f, 1.5f);
        note(p, "Dragonstrike: the spirit passes through " + hit + " foe(s).", ChatFormatting.AQUA);
    }

    // ================================================================ The Reaver
    /** Barbed Arrow: a chained arrow (10 blocks, up to 20 at a disadvantage). The target is dragged to the bow and impaled if it is Large or smaller and fails its save. */
    static void barbedArrow(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        Aim a = aim(p, Wave23Rules.BARB_LONG, false, x -> Wave0Util.targetable(p, x));
        Vec3 from = hand(p);
        if (a.target() == null) {
            beam(level, from, a.end(), RED);
            note(p, "The barbed arrow finds nothing.", ChatFormatting.GRAY);
            return;
        }
        LivingEntity t = a.target();
        double dist = Math.sqrt(t.distanceToSqr(p));
        boolean impaled = Wave23Rules.pulled(t.getBbWidth(), t.getBbHeight(), t.getMaxHealth(), p.getRandom().nextDouble());
        beam(level, from, a.end(), RED);
        Fx.damage(p, t, Wave23Rules.barbDamage(dist, impaled), "minecraft:arrow");
        Fx.addEffect(t, "minecraft:slowness", Wave23Rules.BARB_CHAIN_TICKS, 1);
        if (impaled && t.isAlive()) {
            Vec3 dest = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(Wave23Rules.BARB_STOP));
            Vec3 pull = dest.subtract(t.position());
            Vec3 v = pull.scale(0.35);
            double cap = 2.2;
            if (v.length() > cap) v = v.normalize().scale(cap);
            t.setDeltaMovement(v.x, Math.max(0.25, v.y + 0.2), v.z);
            t.hurtMarked = true;
            t.fallDistance = 0;
            if (t instanceof Mob m) m.setTarget(p);
            spark(level, centre(t), RED, 14);
            note(p, "The Reaver drags " + t.getName().getString() + " onto its spike.", ChatFormatting.DARK_RED);
        } else {
            note(p, t.getName().getString() + " tears the chain loose and is not dragged in.", ChatFormatting.GRAY);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    // ================================================================ Warlute
    /** Chord of Valor: the creature in your sight (not you; once a day each) gains Luck, Strength and Speed for a minute; one step stronger if you struck with the lute in the last round. */
    static void chordOfValor(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        Aim a = aim(p, Wave23Rules.CHORD_RANGE, true, x -> ally(p, x));
        LivingEntity t = a.target();
        if (t == null) {
            note(p, "Chord of Valor: look at an ally (not yourself).", ChatFormatting.GRAY);
            return;
        }
        CompoundTag td = t.getPersistentData();
        long today = Wave23Rules.dayOf(now);
        if (!Wave23Rules.chordAllowed(td.getLong(K_CHORD_DAY), today)) {
            note(p, t.getName().getString() + " has had a chord today.", ChatFormatting.GRAY);
            return;
        }
        td.putLong(K_CHORD_DAY, today + 1);
        boolean beat = Wave23Rules.rhythm(p.getPersistentData().getLong(K_RHYTHM), now);
        int amp = Wave23Rules.chordAmp(beat);
        Fx.addEffect(t, "minecraft:luck", Wave23Rules.CHORD_TICKS, amp + 1);
        Fx.addEffect(t, "minecraft:strength", Wave23Rules.CHORD_TICKS, amp);
        Fx.addEffect(t, "minecraft:speed", Wave23Rules.CHORD_TICKS, amp);
        Fx.addEffect(t, "tensura:inspiration", Wave23Rules.CHORD_TICKS, amp);
        ServerLevel level = p.serverLevel();
        beam(level, hand(p), centre(t), GOLD);
        com.newuniverse.nusmp.particle.fx.Fx.gather(level, FxThemes.RELIC_WAVE_23, t, FxTone.LIGHT, 10);
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_GUITAR.value(), SoundSource.PLAYERS, 1.2f, beat ? 1.6f : 1.2f);
        note(p, "Chord of Valor" + (beat ? " (on the beat)" : "") + ": " + t.getName().getString() + " is inspired.", ChatFormatting.GOLD);
    }

    // ================================================================ Hero's Shield
    /** The shield flies to the first creature in sight (6 blocks, up to 18 at a disadvantage) for 2d6 plus your strength, and is in your hand again at once. */
    static void shieldThrow(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        Aim a = aim(p, Wave23Rules.THROW_LONG, false, x -> Wave0Util.targetable(p, x));
        beam(level, hand(p), a.end(), 0x6FD8FF);
        LivingEntity t = a.target();
        if (t == null) {
            note(p, "The shield finds nothing and returns.", ChatFormatting.GRAY);
            return;
        }
        double dist = Math.sqrt(t.distanceToSqr(p));
        Fx.damage(p, t, Wave23Rules.throwDamage(dist), "minecraft:player_attack");
        Vec3 push = t.position().subtract(p.position()).multiply(1, 0, 1);
        if (push.lengthSqr() > 1.0e-4) {
            Vec3 v = push.normalize().scale(0.5);
            t.setDeltaMovement(t.getDeltaMovement().add(v.x, 0.15, v.z));
            t.hurtMarked = true;
        }
        spark(level, centre(t), 0x6FD8FF, 10);
        level.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    // ================================================================ Bewitching Lyre
    static int lyreCharges(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        return d.contains(K_LYRE) ? d.getInt(K_LYRE) : Wave23Rules.LYRE_CHARGES;
    }

    /** The Enchantment: foes within ten blocks save against the song or lose their will to fight. Sneak to spend a charge: then none of them can save. */
    static void bewitch(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        int charges = lyreCharges(p);
        boolean spend = c.sneaking() && charges > 0;
        if (spend) p.getPersistentData().putInt(K_LYRE, Wave23Rules.lyreAfter(charges, true));
        List<LivingEntity> foes = Fx.foesAround(p, Wave23Rules.LYRE_RADIUS);
        int fell = 0;
        for (LivingEntity f : foes) {
            boolean boss = Wave23Rules.isBoss(f.getMaxHealth());
            if (!Wave23Rules.bewitched(spend, boss, p.getRandom().nextDouble())) continue;
            int ticks = boss ? Wave23Rules.BEWITCH_TICKS / 2 : Wave23Rules.BEWITCH_TICKS;
            Fx.addEffect(f, "minecraft:slowness", ticks, 1);
            Fx.addEffect(f, "minecraft:weakness", ticks, 1);
            Fx.addEffect(f, "tensura:fear", ticks, 0);
            if (f instanceof Mob m) {
                m.setTarget(null);
                m.setLastHurtByMob(null);
            }
            spark(level, centre(f), PINK, 8);
            fell++;
        }
        com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_23, p.position().add(0, 1.2, 0), FxTone.LIGHT, 12, 1.2);
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.4f, 0.8f);
        note(p, "Bewitching Lyre: " + fell + " of " + foes.size() + " fell under the song" + (spend ? " (charge spent, " + lyreCharges(p) + " left)" : "") + ".", ChatFormatting.LIGHT_PURPLE);
    }

    /** A performance before a willing audience of twelve: a good one brings every charge back. */
    static void perform(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        double r = Wave23Rules.AUDIENCE_RADIUS;
        int audience = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> x != p && x.isAlive() && !x.isSpectator() && !(x instanceof ArmorStand)
                && !(x instanceof Enemy) && x.distanceToSqr(p) <= r * r && !(x instanceof Player o && o.isCreative()))) {
            if (e instanceof Mob m && m.getTarget() == p) continue;                  // someone who is hunting you is not a willing listener
            audience++;
        }
        boolean ok = Wave23Rules.performed(audience, p.getRandom().nextDouble());
        com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_23, p.position().add(0, 1.2, 0), FxTone.LIGHT, 12, 1.5);
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.4f, 1.0f);
        if (ok) {
            p.getPersistentData().putInt(K_LYRE, Wave23Rules.LYRE_CHARGES);
            note(p, "The audience of " + audience + " cheers: the lyre regains all " + Wave23Rules.LYRE_CHARGES + " charges.", ChatFormatting.LIGHT_PURPLE);
        } else if (!Wave23Rules.audienceEnough(audience)) {
            note(p, "Only " + audience + " willing listener(s): twelve are needed.", ChatFormatting.GRAY);
        } else {
            note(p, "The performance falls flat (" + audience + " listeners). Try again.", ChatFormatting.GRAY);
        }
    }

    // ================================================================ Eye of the Deep One
    static int boon(ServerPlayer p) { return p.getPersistentData().getInt(K_BOON); }

    /** Eldritch Blast: a beam of dark force, 20 blocks, 5 damage (and 2 necrotic more under the Twisted Blast boon). */
    static void eldritchBlast(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        Aim a = aim(p, Wave23Rules.BLAST_RANGE, false, x -> Wave0Util.targetable(p, x));
        beam(level, hand(p), a.end(), VIOLET);
        LivingEntity t = a.target();
        if (t == null) return;
        Fx.damage(p, t, Wave23Rules.BLAST_DAMAGE, "minecraft:magic");
        if (boon(p) == Wave23Rules.BOON_BLAST) {
            t.invulnerableTime = 0;
            Fx.damage(p, t, Wave23Rules.BLAST_BONUS, "minecraft:wither");
        }
        level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.4f, 1.8f);
    }

    /** Strength of the Old One: tentacles seize the creature in reach, holding it fast for four seconds. A boss is held for half as long. */
    static void grapple(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        Aim a = aim(p, Wave23Rules.GRAPPLE_RANGE, false, x -> Wave0Util.targetable(p, x));
        LivingEntity t = a.target();
        if (t == null) {
            note(p, "Nothing within reach of the tentacles.", ChatFormatting.GRAY);
            return;
        }
        int ticks = Wave23Rules.isBoss(t.getMaxHealth()) ? Wave23Rules.GRAPPLE_TICKS / 2 : Wave23Rules.GRAPPLE_TICKS;
        Fx.addEffect(t, "minecraft:slowness", ticks, 6);
        Fx.addEffect(t, "minecraft:weakness", ticks, 1);
        Fx.damage(p, t, 3.0f, "minecraft:player_attack");
        beam(level, hand(p), centre(t), 0x2FA060);
        com.newuniverse.nusmp.particle.fx.Fx.gather(level, FxThemes.RELIC_WAVE_23, t, FxTone.DARK, 10);
        note(p, "The tentacles close on " + t.getName().getString() + ".", ChatFormatting.DARK_GREEN);
    }

    // ================================================================ Horn of Leroy
    /** Alright Chums, Let's Do This: every ally within 100 blocks charges with you for six hours of a day (5 minutes). Win the battle and your charisma grows; lose it and you get a chicken. */
    static void blowHorn(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        CompoundTag d = p.getPersistentData();
        long now = level.getGameTime();
        d.putLong(K_HORN_UNTIL, now + Wave23Rules.HORN_TICKS);
        d.putInt(K_HORN_KILLS, 0);
        double r = Wave23Rules.HORN_RADIUS;
        int rallied = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> ally(p, x) && x.distanceToSqr(p) <= r * r)) {
            if (rallied >= 40) break;
            Fx.addEffect(e, "minecraft:strength", Wave23Rules.HORN_TICKS, 0);
            Fx.addEffect(e, "minecraft:speed", Wave23Rules.HORN_TICKS, 0);
            Fx.addEffect(e, "minecraft:resistance", Wave23Rules.HORN_TICKS, 0);
            Fx.addEffect(e, "tensura:inspiration", Wave23Rules.HORN_TICKS, 0);
            if (e instanceof Mob m && !(e instanceof Player) && m.getTarget() == null) {
                List<LivingEntity> foes = Fx.foesAround(p, 24);
                if (!foes.isEmpty()) m.setTarget(foes.get(0));
            }
            com.newuniverse.nusmp.particle.fx.Fx.aura(level, FxThemes.RELIC_WAVE_23, e, FxTone.LIGHT);
            rallied++;
        }
        Fx.addEffect(p, "minecraft:strength", Wave23Rules.HORN_TICKS, 1);
        Fx.addEffect(p, "minecraft:speed", Wave23Rules.HORN_TICKS, 0);
        Fx.addEffect(p, "minecraft:luck", Wave23Rules.HORN_TICKS, 4);                      // "+10 on persuasion checks"
        Fx.addEffect(p, "tensura:inspiration", Wave23Rules.HORN_TICKS, 1);
        com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_23, p.position().add(0, 1.4, 0), FxTone.LIGHT, 12, 2.0);
        com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_23, p.position().add(0, 1.0, 0), FxTone.DARK, 12, 1.5);
        level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 3.0f, 1.0f);
        Fx.say(p, "LEEEEROOOOY JENKINS!  " + rallied + " fighter(s) follow you. Fell " + Wave23Rules.HORN_KILLS + " hostile creatures in five minutes to win the day.");
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("The horn crumbles in your hands; it will be whole again in five days.").withStyle(ChatFormatting.GRAY), false);
    }

    /** The battle is over: a victory raises charisma for good, a defeat is a bucket of chicken. */
    static void endBattle(ServerPlayer p, boolean timedOut) {
        CompoundTag d = p.getPersistentData();
        int kills = d.getInt(K_HORN_KILLS);
        d.remove(K_HORN_UNTIL);
        d.remove(K_HORN_KILLS);
        if (timedOut && Wave23Rules.victory(kills)) {
            d.putInt(K_CHARISMA, Wave23Rules.charismaAfterVictory(d.getInt(K_CHARISMA)));
            Fx.say(p, "Victory! Leroy lives on in you: +1 Charisma (" + d.getInt(K_CHARISMA) + " of " + Wave23Rules.CHARISMA_CAP + "; luck +" + Wave23Rules.charismaLuck(d.getInt(K_CHARISMA)) + ").");
            com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_23, p.position().add(0, 1.4, 0), FxTone.LIGHT, 12, 1.5);
        } else {
            Fx.say(p, "The battle was lost (" + kills + " felled). A bucket of chicken appears in your hands.");
            ItemStack bucket = new ItemStack(Items.COOKED_CHICKEN, 8);
            if (!p.getInventory().add(bucket)) p.drop(bucket, false);
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!p.getInventory().add(empty)) p.drop(empty, false);
        }
    }
}
