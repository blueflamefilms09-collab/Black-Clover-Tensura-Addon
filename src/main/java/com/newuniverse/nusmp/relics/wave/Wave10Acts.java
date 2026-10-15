package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Wave 10: the act bodies of the ten weapons (the server side of every right-click, sneak-click and worn-relic key). The core has already checked
 * attunement, paid mana or existence points and set the cooldown and the daily use counter before a body runs. The numbers are in
 * {@link Wave10Rules}; the long-lived things (the wall, the light, the flail, the climb) are in {@link Wave10Events}.
 */
final class Wave10Acts {
    private Wave10Acts() {}

    static final String K_AXE_AT = "nusmp_w10_axe_at", K_AXE_X = "nusmp_w10_axe_x", K_AXE_Y = "nusmp_w10_axe_y", K_AXE_Z = "nusmp_w10_axe_z",
            K_LIGHT_COL = "nusmp_w10_light_col", K_RAISED = "nusmp_w10_raised";
    private static final int WATER = 0x4FD8E8, PINK = 0xFF3FD0, JADE = 0x3FE08A, GOLDEN = 0xFFE27A, THORN = 0x7BC24A, PURPLE = 0xA060FF;

    // ================================================================ aiming
    /** What a look along the player's sight found: the creature (or null) and where the line ended (a block, the creature, or the full range). */
    record Aim(LivingEntity target, Vec3 end) {}

    /**
     * The nearest creature along the look direction within {@code range} (blocks stop the line), among those {@code ok} accepts. {@code yawDeg} and
     * {@code pitchDeg} bend the line (the needles' fan).
     */
    static Aim aim(ServerPlayer p, double range, double yawDeg, double pitchDeg, Predicate<LivingEntity> ok) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        float yaw = p.getYRot() + (float) yawDeg, pitch = Mth.clamp(p.getXRot() + (float) pitchDeg, -90f, 90f);
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        Vec3 far = eye.add(look.scale(range));
        BlockHitResult blocks = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (blocks.getType() != HitResult.Type.MISS) far = blocks.getLocation();
        AABB box = new AABB(eye, far).inflate(1.0);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        Vec3 bestAt = far;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive() && !x.isSpectator() && ok.test(x))) {
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

    static Aim aim(ServerPlayer p, double range, Predicate<LivingEntity> ok) { return aim(p, range, 0, 0, ok); }

    /** A line of coloured dust from one point to another (at most 48 puffs). */
    static void trail(ServerLevel l, Vec3 from, Vec3 to, int rgb, double step) {
        double len = from.distanceTo(to);
        if (len < 0.1) return;
        int n = Mth.clamp((int) (len / Math.max(0.2, step)), 1, 48);
        Vec3 d = to.subtract(from).scale(1.0 / n);
        var dust = align(rgb);
        for (int i = 0; i <= n; i++) RukhFx.spray(l, dust, from.x + d.x * i, from.y + d.y * i, from.z + d.z * i, 1, 0.02, 0.02, 0.02, 0.0);
    }

    static net.minecraft.core.particles.SimpleParticleType dust(int rgb, float size) {
        return align(rgb).type();                    // Rukh replaces vanilla dust: the colour only picks the alignment
    }

    /** Rukh alignment for a legacy dust colour: dark colours (relative luminance below 0.33) become dark Rukh, the rest light. */
    static com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment align(int rgb) {
        double lum = (0.2126 * (rgb >> 16 & 255) + 0.7152 * (rgb >> 8 & 255) + 0.0722 * (rgb & 255)) / 255.0;
        return com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.of(lum < 0.33);
    }

    private static void waterBurst(ServerLevel l, Vec3 at) {
        RukhFx.spray(l, RukhFx.Alignment.LIGHT, at.x, at.y, at.z, 40, 0.5, 0.4, 0.5, 0.2);
        RukhFx.spray(l, RukhFx.Alignment.LIGHT, at.x, at.y, at.z, 16, 0.4, 0.4, 0.4, 0.05);
        l.playSound(null, BlockPos.containing(at), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.9f, 1.2f);
    }

    private static boolean ally(ServerPlayer p, LivingEntity x) { return !Wave0Util.fair(p, x); }

    private static String nameOf(LivingEntity e) { return e.getName().getString(); }

    // ================================================================ Krakeblade
    /** Wall of Water: a standing wall four blocks ahead that shoves foes back, slows them and swallows missiles for eight seconds. */
    static void wallOfWater(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Vec3 look = p.getLookAngle();
        double dx = look.x, dz = look.z;
        double len = Math.hypot(dx, dz);
        if (len < 1.0e-3) { dx = 0; dz = 1; len = 1; }
        dx /= len;
        dz /= len;
        Vec3 c = new Vec3(p.getX() + dx * Wave10Rules.WALL_DISTANCE, p.getY(), p.getZ() + dz * Wave10Rules.WALL_DISTANCE);
        Wave10Events.raiseWall(p, c, dx, dz);
        waterBurst(p.serverLevel(), c.add(0, 1, 0));
        Fx.say(p, "A wall of water rises ahead of you (8 seconds).");
    }

    // ================================================================ Unicorn Dagger
    /** Poke an ally (or yourself, when no one is in reach): 1 damage that cannot kill, and a random d8 buff. */
    static void poke(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Aim a = aim(p, 4.5, x -> ally(p, x));
        LivingEntity t = a.target() != null ? a.target() : p;
        if (Wave10Rules.pokeHurts(t.getHealth())) t.hurt(p.damageSources().magic(), 1.0f);
        int roll = 1 + p.getRandom().nextInt(Wave10Rules.buffCount());
        Wave10Rules.Buff b = Wave10Rules.buff(roll);
        giveBuff(t, b);
        ServerLevel l = p.serverLevel();
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.6, 0);
        RukhFx.spray(l, RukhFx.Alignment.LIGHT, at.x, at.y, at.z, 10, 0.4, 0.4, 0.4, 0.02);
        Fx.burst(l, at, 0xFFB8F0);
        l.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.4f);
        Fx.say(p, (t == p ? "No one in reach: the horn pokes you. " : "You poke " + nameOf(t) + ". ") + "The d8 shows " + roll + ": "
                + b.effect().substring(b.effect().indexOf(':') + 1).replace('_', ' ') + ".");
    }

    private static void giveBuff(LivingEntity t, Wave10Rules.Buff b) {
        ResourceLocation id = ResourceLocation.tryParse(b.effect());
        if (id == null || BuiltInRegistries.MOB_EFFECT.getHolder(id).isEmpty()) id = ResourceLocation.withDefaultNamespace("luck");   // an absent Tensura effect: luck
        BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(h -> t.addEffect(new MobEffectInstance(h, b.ticks(), b.amp())));
    }

    // ================================================================ Rune Splitter
    /** Light: the axe sheds a bright light (night sight for you, a glow on hostile creatures near you) until you put it out or let go of it. */
    static void light(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        CompoundTag d = p.getPersistentData();
        if (ctx.isOn("light")) {
            int col = (d.getInt(K_LIGHT_COL) + 1) % Wave10Rules.LIGHT_COLOURS.length;
            d.putInt(K_LIGHT_COL, col);
            Fx.burst(p.serverLevel(), p.position().add(0, 1, 0), Wave10Rules.lightColour(col));
            Fx.say(p, "The Rune Splitter shines (it lights hostile creatures within " + (int) Wave10Rules.LIGHT_RADIUS + " blocks and gives you night sight). Use it again to put it out.");
        } else {
            Fx.say(p, "The light goes out.");
        }
    }

    /** Dispel Magic: the creature in your sights (or you, if none) loses the magic on it. */
    static void dispelMagic(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Aim a = aim(p, 24.0, x -> true);
        ServerLevel l = p.serverLevel();
        LivingEntity t = a.target();
        if (t == null) {
            int n = 0;
            while (Wave0Util.cleanseOne(p) && n++ < 8) { /* every affliction on you */ }
            Fx.burst(l, p.position().add(0, 1, 0), GOLDEN);
            Fx.say(p, "No target: the axe dispels the harmful magic on you (" + n + ").");
            return;
        }
        boolean infernal = Wave10Rules.infernalPath(pathOf(t));
        int ended = 0, kept = 0;
        for (MobEffectInstance i : new ArrayList<>(t.getActiveEffects())) {
            boolean good = i.getEffect().value().isBeneficial();
            boolean friend = ally(p, t);
            if (friend == good) continue;                                                    // foes lose their boons; friends lose their afflictions
            if (Wave10Rules.dispelled(i.getAmplifier(), infernal, p.getRandom().nextDouble())) { t.removeEffect(i.getEffect()); ended++; }
            else kept++;
        }
        if (t instanceof Mob m && !ally(p, t) && m.getPersistentData().getLong("nusmp_sealed_until") > 0) m.getPersistentData().putLong("nusmp_sealed_until", 0);
        trail(l, p.getEyePosition().add(0, -0.2, 0), a.end(), GOLDEN, 0.5);
        Fx.burst(l, t.position().add(0, t.getBbHeight() * 0.5, 0), GOLDEN);
        l.playSound(null, t.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        Fx.say(p, "Dispel Magic on " + nameOf(t) + ": " + ended + " ended" + (kept > 0 ? ", " + kept + " held (" + (infernal ? "infernal: harder to strip" : "too strong") + ")" : "") + ".");
    }

    private static String pathOf(LivingEntity e) {
        ResourceLocation k = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return k == null ? "" : k.getPath();
    }

    // ================================================================ Junglethorn Whip
    /** Vine Swing: the whip catches a tree, a vine or any solid anchor in sight and swings you towards it. */
    static void vineSwing(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition(), far = eye.add(p.getLookAngle().scale(Wave10Rules.SWING_RANGE));
        BlockHitResult hit = l.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.MISS) { Fx.say(p, "Nothing in reach to catch."); return; }
        BlockState s = l.getBlockState(hit.getBlockPos());
        boolean plant = s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(Blocks.VINE) || s.is(Blocks.JUNGLE_LOG) || s.is(BlockTags.CLIMBABLE);
        Vec3 at = hit.getLocation();
        double dist = at.distanceTo(eye);
        double speed = Wave10Rules.swingSpeed(dist) * (plant ? 1.0 : 0.75);
        Vec3 dir = at.subtract(eye).normalize();
        p.setDeltaMovement(dir.x * speed, dir.y * speed + 0.35, dir.z * speed);
        p.hurtMarked = true;
        p.fallDistance = 0;
        trail(l, p.position().add(0, 1.2, 0), at, THORN, 0.4);
        l.playSound(null, p.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.9f, 0.8f);
        if (!plant) Fx.say(p, "No tree to swing from, so the whip only half catches: a weaker swing.");
    }

    /** Thorny Disarm: the whip tears a weapon from a creature's hand and pulls it to yours (players are only weakened; no one is disarmed in PvP). */
    static void thornyDisarm(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Aim a = aim(p, Wave10Rules.DISARM_RANGE, x -> Wave0Util.fair(p, x));
        LivingEntity t = a.target();
        trail(l, p.getEyePosition().add(0, -0.3, 0), a.end(), THORN, 0.4);
        if (t == null) { Fx.say(p, "The whip cracks at nothing."); return; }
        if (t instanceof Player || com.newuniverse.nusmp.balance.BalanceLaw.isBoss(t)) {
            Fx.addEffect(t, "minecraft:weakness", 160, 0);
            Fx.say(p, nameOf(t) + " is stung and weakened (players and bosses keep what they hold).");
            return;
        }
        ItemStack held = t.getItemBySlot(EquipmentSlot.MAINHAND);
        if (held.isEmpty()) {
            Fx.say(p, nameOf(t) + " has nothing in hand to take.");
            return;
        }
        ItemStack taken = held.copy();
        t.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (!p.getInventory().add(taken)) p.drop(taken, false);
        Fx.addEffect(t, "minecraft:slowness", 60, 0);
        Fx.burst(l, t.position().add(0, 1, 0), THORN);
        l.playSound(null, t.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.5f);
        Fx.say(p, "You snatch " + taken.getHoverName().getString() + " from " + nameOf(t) + ".");
    }

    /** Thorn Whip: a lash of thorns that pulls a creature up to five blocks towards you and cuts it. */
    static void thornWhip(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Aim a = aim(p, Wave10Rules.THORN_RANGE, x -> Wave0Util.fair(p, x));
        trail(l, p.getEyePosition().add(0, -0.3, 0), a.end(), THORN, 0.4);
        LivingEntity t = a.target();
        if (t == null) { Fx.say(p, "The thorn lash finds nothing."); return; }
        Fx.damage(p, t, 4.0f, "minecraft:player_attack");
        Vec3 to = p.position().subtract(t.position());
        double d = to.length();
        if (d > 2.0) {
            Vec3 pull = to.normalize().scale(Math.min(5.0, d - 1.5) * 0.28);
            t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.25, pull.z));
            t.hurtMarked = true;
        }
        Fx.addEffect(t, "minecraft:poison", 60, 0);
    }

    // ================================================================ Needler
    /** Needle Volley: four needles in a fan, loading ignored, no strength added (a fixed damage each, hit by line). */
    static void needleVolley(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.15, 0);
        int hits = 0;
        List<LivingEntity> struck = new ArrayList<>();
        for (int i = 0; i < Wave10Rules.NEEDLES; i++) {
            double[] off = Wave10Rules.needleOffset(i, Wave10Rules.NEEDLES);
            Aim a = aim(p, Wave10Rules.NEEDLE_RANGE, off[0], off[1], x -> Wave0Util.fair(p, x));
            trail(l, from, a.end(), PURPLE, 0.8);
            if (a.target() != null) {
                if (a.target() instanceof Player && struck.contains(a.target())) continue;   // PvP: one needle per player per volley (the fan cannot stack on one body)
                struck.add(a.target());
                if (!(a.target() instanceof Player)) a.target().invulnerableTime = 0;   // each needle is its own hit on a creature
                Fx.damageHeavy(p, a.target(), Wave10Rules.NEEDLE_DAMAGE, "minecraft:arrow");
                RukhFx.spray(l, RukhFx.Alignment.LIGHT, a.end().x, a.end().y, a.end().z, 4, 0.1, 0.1, 0.1, 0.1);
                hits++;
            }
        }
        l.playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.7f);
        Fx.say(p, "Needle Volley: " + Wave10Rules.NEEDLES + " needles, " + hits + " hit.");
    }

    // ================================================================ Sidras' swearing sword
    /** Barrage of Insults: the sword hurls abuse at every foe within six blocks (each takes a Wisdom save, DC 13). */
    static void barrage(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 6.0)) {
            if (n++ >= 6) break;
            Wave10Events.insult(p, t);
        }
        if (n == 0) Fx.say(p, "\"Come out, come out, you cowardly... uh... whatever!\" (no one in earshot)");
    }

    // ================================================================ Stegosaurus Greatsword
    /** Command Word: the blade clatters apart into a whip (and back). */
    static void commandWord(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        boolean whip = ctx.isOn("command_word");
        ServerLevel l = p.serverLevel();
        l.playSound(null, p.blockPosition(), SoundEvents.SKELETON_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.6f);
        RukhFx.spray(l, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
        Fx.say(p, whip ? "\"Clatter!\" The spine falls apart into a whip: it lashes up to five blocks behind you, two foes at a time."
                : "\"Clatter!\" The vertebrae snap back into a greatsword: it lashes three blocks behind you, one foe at a time.");
    }

    // ================================================================ Jade Xiphos
    /** Ringing Note: you flick the blade: a high glassy note stings foes within five blocks (psychic, a little) and shows the blade the way to break. */
    static void ringingNote(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 1.8f);
        Fx.ring(l, p.position(), 5.0, JADE);
        Fx.aoe(p, 5.0, 3.0f, "minecraft:sonic_boom", "minecraft:slowness", 40, 0);
        Fx.say(p, "A high, clear note rings from the blade. (Sneak and hold to shatter it on purpose.)");
    }

    /** Shatter: the blade vibrates itself to pieces; the shards fly out and the sword is gone for good. */
    static void shatter(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Wave10Events.shatterXiphos(p, ctx.stack(), true);
    }

    // ================================================================ Returning Hand Axe
    /** Hurl: the axe flies at the first creature in sight within 18 blocks and turns to water; it can be called back (sneak) for a second blow. */
    static void hurl(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Aim a = aim(p, 18.0, x -> Wave0Util.fair(p, x));
        Vec3 from = p.getEyePosition().add(0, -0.3, 0);
        trail(l, from, a.end(), WATER, 0.4);
        waterBurst(l, a.end());
        if (a.target() != null) Fx.damage(p, a.target(), 6.0f, "minecraft:thrown");
        CompoundTag d = p.getPersistentData();
        d.putLong(K_AXE_AT, l.getGameTime());
        d.putDouble(K_AXE_X, a.end().x);
        d.putDouble(K_AXE_Y, a.end().y);
        d.putDouble(K_AXE_Z, a.end().z);
        Fx.say(p, a.target() != null ? "The axe strikes " + nameOf(a.target()) + " and breaks into water. Sneak-use to call it back." : "The axe lands and breaks into water. Sneak-use to call it back.");
    }

    /** Recall: the water gathers and bursts back into your hand, cutting every creature on the way back. */
    static void recall(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        CompoundTag d = p.getPersistentData();
        if (!d.contains(K_AXE_AT) || l.getGameTime() - d.getLong(K_AXE_AT) > Wave10Rules.AXE_AWAY_TICKS) {
            Fx.say(p, "The axe is already in your hand (it has to have been thrown in the last 8 seconds to be recalled).");
            return;
        }
        Vec3 a = new Vec3(d.getDouble(K_AXE_X), d.getDouble(K_AXE_Y), d.getDouble(K_AXE_Z));
        Vec3 b = p.getEyePosition();
        d.remove(K_AXE_AT);
        if (a.distanceToSqr(b) > 48.0 * 48.0) {                                          // teleported or changed dimension since the throw: the water has dissolved
            Fx.say(p, "The axe's water has drifted too far to gather again.");
            return;
        }
        trail(l, a, b, WATER, 0.4);
        waterBurst(l, b.add(p.getLookAngle().scale(0.8)));
        int n = 0;
        List<LivingEntity> near = l.getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(2.0), x -> Wave0Util.fair(p, x));
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(a)));
        for (LivingEntity t : near) {
            if (Wave10Rules.distToSegment(t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), a.x, a.y, a.z, b.x, b.y, b.z) > 1.6) continue;
            if (n++ >= 8) break;
            Fx.damage(p, t, 4.0f, "minecraft:thrown");
            Fx.addEffect(t, "minecraft:slowness", 40, 0);
        }
        Fx.say(p, "The axe bursts back into your hand" + (n > 0 ? ", cutting " + n + " on the way." : "."));
    }

    // ================================================================ Dead Blade
    /** Finger of Death: after a held breath, the blade points and the life drains out of one creature (a save halves it); a humanoid it kills rises as your servant. */
    static void fingerOfDeath(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Aim a = aim(p, 24.0, x -> Wave0Util.fair(p, x));
        trail(l, p.getEyePosition().add(0, -0.3, 0), a.end(), PINK, 0.35);
        LivingEntity t = a.target();
        if (t == null) { Fx.say(p, "The finger of death points at nothing."); return; }
        boolean saved = Wave0Util.saves(t, Wave10Rules.FINGER_DC, 0);
        float dmg = Wave10Rules.fingerDamage(t.getMaxHealth(), saved);
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.5, 0);
        for (int i = 0; i < 12; i++) RukhFx.spray(l, align(PINK), t.getX(), t.getY() + i * 0.25, t.getZ(), 3, 0.2, 0.05, 0.2, 0.0);
        Fx.burst(l, at, PINK);
        l.playSound(null, t.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.8f);
        boolean humanoid = Wave10Rules.humanoidPath(pathOf(t));
        Fx.damageHeavy(p, t, dmg, "minecraft:magic");
        Fx.say(p, "Finger of Death on " + nameOf(t) + (saved ? " (it resists: half)." : "."));
        if (!t.isAlive() && humanoid && !(t instanceof Player)) {
            LivingEntity z = Fx.summon(p, "minecraft:zombie", 1200);
            if (z != null) {
                z.moveTo(t.getX(), t.getY(), t.getZ(), t.getYRot(), 0f);
                z.getPersistentData().putBoolean(K_RAISED, true);
                Fx.say(p, "It rises as your servant for one minute.");
            }
        }
    }
}
