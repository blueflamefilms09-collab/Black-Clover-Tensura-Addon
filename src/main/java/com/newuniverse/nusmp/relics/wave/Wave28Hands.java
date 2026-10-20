package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the three hand relics keep small per-player states (a charge pool, a blow count, the held kind, the chosen spell); keeping their code together
 * keeps those keys in one place, and every state lives in the player's persistent data so nothing is lost with a world change.
 * <p>
 * Wave 28, the hands: the Hellfire Gauntlet (its fire blows, its awakening and the charges of its awakened powers), the Proficiency Gloves (what the
 * thing in your hand teaches the gloves) and the Gauntlet of Gamelon (four gems, three spells each).
 */
final class Wave28Hands {
    private Wave28Hands() {}

    // ================================================================ Hellfire Gauntlet
    private static final String HELL_LEFT = "hell_left", HELL_DAY = "hell_day", HELL_BLOWS = "hell_blows", HELL_AURA = "hell_aura_until", HELL_AURA_AT = "hell_aura_at";

    private static Wave28Rules.Charges hellCharges(ServerPlayer p) {
        CompoundTag d = Wave28Util.data(p);
        Wave28Rules.Charges c = new Wave28Rules.Charges(Wave28Rules.HELL_MAX_CHARGES,
                d.contains(HELL_LEFT) ? d.getInt(HELL_LEFT) : Wave28Rules.HELL_MAX_CHARGES, d.getLong(HELL_DAY));
        return c.at(Wave28Rules.dayOf(p.level().getGameTime()), Wave28Rules.HELL_MAX_CHARGES);        // "replenish each dawn": all three come back
    }

    private static void saveHell(ServerPlayer p, Wave28Rules.Charges c) {
        CompoundTag d = Wave28Util.data(p);
        d.putInt(HELL_LEFT, c.left());
        d.putLong(HELL_DAY, c.day());
    }

    static boolean hardy(ServerPlayer p) { return Wave28Rules.hardy(p.getMaxHealth()); }

    static int blows(ServerPlayer p) { return Wave28Util.data(p).getInt(HELL_BLOWS); }

    private static boolean ready(ServerPlayer p) {
        if (!hardy(p)) {
            Wave28Util.tell(p, "The gauntlet's flames shy from a body this frail (it needs " + (int) Wave28Rules.HELL_HARDY_HEALTH + " maximum health).", net.minecraft.ChatFormatting.RED);
            return false;
        }
        return true;
    }

    /** The awakened powers: the gauntlet must have drunk enough fire, and each use spends charges. False (with a line) if it cannot. */
    private static boolean awakenedSpend(ServerPlayer p, int cost) {
        if (!ready(p)) return false;
        int b = blows(p);
        if (!Wave28Rules.awakened(b)) {
            Wave28Util.tell(p, "The gauntlet sleeps: it has drunk " + b + " of " + Wave28Rules.HELL_AWAKEN_BLOWS + " burning blows.", net.minecraft.ChatFormatting.GRAY);
            return false;
        }
        Wave28Rules.Charges c = hellCharges(p);
        if (!c.canSpend(cost)) {
            Wave28Util.tell(p, "No charges left (" + c.left() + "; all three come back at dawn).", net.minecraft.ChatFormatting.RED);
            return false;
        }
        saveHell(p, c.spend(cost));
        return true;
    }

    /** One burning blow landed (called from the damage handler): the awakening counter, with a line when it wakes. */
    static void drank(ServerPlayer a) {
        CompoundTag d = Wave28Util.data(a);
        int n = d.getInt(HELL_BLOWS);
        if (n >= Wave28Rules.HELL_AWAKEN_BLOWS) return;
        d.putInt(HELL_BLOWS, n + 1);
        if (n + 1 == Wave28Rules.HELL_AWAKEN_BLOWS) {
            Wave28Util.say(a, "The Hellfire Gauntlet wakes. Three charges burn in its knuckles (they come back at dawn).", net.minecraft.ChatFormatting.GOLD);
            Wave28Util.burst(a.serverLevel(), a.position().add(0, 1.0, 0), FxTone.LIGHT, 12);
        }
    }

    /**
     * Every melee blow of the wearer (called before armour, from the damage handler): +2d4 fire damage and burning; bare-handed blows add 1d8 slashing
     * (the gauntlet's claws); a maximum die on a medium or smaller target that fails the strength contest (its existence against yours) shoves it a block.
     * Each burning blow feeds the awakening.
     */
    static void blow(ServerPlayer a, LivingEntity victim, net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) {
        if (!hardy(a) || !Wave28Util.target(a, victim)) return;
        int fire = Wave28Rules.twoD4(a.getRandom().nextInt(4), a.getRandom().nextInt(4));
        int bare = a.getMainHandItem().isEmpty() ? Wave28Rules.d8(a.getRandom().nextInt(8)) : 0;
        e.setAmount(e.getAmount() + fire + bare);
        victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 60));
        if (Wave28Rules.shoves(fire, bare) && Wave28Rules.mediumOrSmaller(victim.getBbWidth() * victim.getBbWidth() * victim.getBbHeight(),
                a.getBbWidth() * a.getBbWidth() * a.getBbHeight() * 1.0) && !Wave28Util.saves(a, victim)) {
            victim.knockback(0.9, a.getX() - victim.getX(), a.getZ() - victim.getZ());
            Wave28Util.burst(a.serverLevel(), victim.position().add(0, victim.getBbHeight() * 0.5, 0), FxTone.LIGHT, 6);
        }
        drank(a);
    }

    /** Firebolt cantrip: a bolt of fire along your look (20 blocks), 4 damage a die, one die more at levels 5, 11 and 17; it sets the target alight. */
    static void firebolt(ActCtx c) {
        ServerPlayer p = c.player();
        if (!ready(p)) return;
        Wave28Util.Aim aim = Wave28Util.aim(p, Wave28Rules.FIREBOLT_RANGE, e -> Wave28Util.foe(p, e));
        Wave28Util.trail(p.serverLevel(), p.getEyePosition().add(0, -0.3, 0), aim.end(), FxTone.LIGHT);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 1.2f);
        if (aim.hit() == null) return;
        Fx.damage(p, aim.hit(), Wave28Rules.fireboltDamage(p.experienceLevel), "minecraft:on_fire");
        aim.hit().setRemainingFireTicks(Math.max(aim.hit().getRemainingFireTicks(), 80));
        Wave28Util.burst(p.serverLevel(), aim.end(), FxTone.LIGHT, 8);
    }

    /** Fireball (awakened, 1 charge): fire bursts where you look (24 blocks), 12 damage and burning to every foe within 4 blocks. No block is touched. */
    static void fireball(ActCtx c) {
        ServerPlayer p = c.player();
        if (!awakenedSpend(p, Wave28Rules.HELL_FIREBALL)) return;
        Wave28Util.Aim aim = Wave28Util.aim(p, Wave28Rules.FIREBALL_RANGE, e -> Wave28Util.foe(p, e));
        Vec3 at = aim.end();
        ServerLevel l = p.serverLevel();
        Wave28Util.trail(l, p.getEyePosition().add(0, -0.3, 0), at, FxTone.LIGHT);
        for (LivingEntity t : Wave28Util.near(l, at, Wave28Rules.FIREBALL_RADIUS, e -> Wave28Util.foe(p, e))) {
            Fx.damage(p, t, Wave28Rules.FIREBALL_DAMAGE, "minecraft:on_fire");
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 100));
        }
        FxRelic.ring(l, at, Wave28Rules.FIREBALL_RADIUS, 0xFF6A1A);
        l.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    /** Hell-quake (awakened, 1 charge): the fist strikes the ground; every foe within 6 blocks takes force and fire damage and is knocked down (heavy slowness). */
    static void quake(ActCtx c) {
        ServerPlayer p = c.player();
        if (!awakenedSpend(p, Wave28Rules.HELL_QUAKE)) return;
        ServerLevel l = p.serverLevel();
        for (LivingEntity t : Wave28Util.near(l, p.position(), Wave28Rules.QUAKE_RADIUS, e -> Wave28Util.foe(p, e))) {
            Fx.damage(p, t, Wave28Rules.QUAKE_FORCE, "minecraft:magic");
            t.invulnerableTime = 0;                                   // the fire is a second blow of the same instant
            Fx.damage(p, t, Wave28Rules.QUAKE_FIRE, "minecraft:on_fire");
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 60));
            Wave28Util.give(t, 40, 4, "minecraft:slowness");
            Vec3 away = t.position().subtract(p.position());
            Vec3 push = new Vec3(away.x, 0, away.z);
            push = push.lengthSqr() < 1.0e-4 ? Vec3.ZERO : push.normalize().scale(0.6);
            t.setDeltaMovement(push.x, 0.35, push.z);
            t.hurtMarked = true;
        }
        FxRelic.ring(l, p.position(), Wave28Rules.QUAKE_RADIUS, 0xFF5A10);
        Wave28Util.burst(l, p.position(), FxTone.LIGHT, 12);
        l.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    /** Aura of fire (awakened, 1 charge): for one minute whoever strikes you in melee takes 7 fire damage (at most once a second). */
    static void aura(ActCtx c) {
        ServerPlayer p = c.player();
        if (!awakenedSpend(p, Wave28Rules.HELL_AURA)) return;
        Wave28Util.data(p).putLong(HELL_AURA, p.level().getGameTime() + Wave28Rules.HELL_AURA_TICKS);
        Wave28Util.give(p, Wave28Rules.HELL_AURA_TICKS, 0, "minecraft:fire_resistance");
        FxRelic.ring(p.serverLevel(), p.position(), 1.8, 0xFF7A20);
        Wave28Util.tell(p, "An aura of fire washes over you (one minute).");
    }

    /** The aura's answer (called after a blow on the wearer): the melee attacker burns. */
    static void auraAnswer(ServerPlayer v, LivingEntity attacker) {
        CompoundTag d = Wave28Util.peek(v);
        if (d == null) return;
        long now = v.level().getGameTime();
        if (now >= d.getLong(HELL_AURA) || now - d.getLong(HELL_AURA_AT) < 20 || !Wave28Util.target(v, attacker)) return;
        d.putLong(HELL_AURA_AT, now);
        attacker.hurt(v.damageSources().thorns(v), Wave28Rules.AURA_RETURN);
        attacker.setRemainingFireTicks(Math.max(attacker.getRemainingFireTicks(), 60));
        Wave28Util.burst(v.serverLevel(), attacker.position().add(0, attacker.getBbHeight() * 0.5, 0), FxTone.LIGHT, 6);
    }

    // ================================================================ Proficiency Gloves
    private static final String PROF_KIND = "prof_kind";
    private static final String ATK = "minecraft:generic.attack_damage", ARMOR = "minecraft:generic.armor", KB = "minecraft:generic.knockback_resistance",
            LUCK = "minecraft:generic.luck";

    static Wave28Rules.Held heldKind(ItemStack s) {
        if (s == null || s.isEmpty()) return Wave28Rules.Held.NOTHING;
        Item i = s.getItem();
        if (i instanceof AxeItem) return Wave28Rules.Held.AXE;
        if (i instanceof SwordItem || i instanceof TridentItem || i instanceof MaceItem) return Wave28Rules.Held.BLADE;
        if (i instanceof DiggerItem) return Wave28Rules.Held.DIGGER;
        if (i instanceof ProjectileWeaponItem) return Wave28Rules.Held.RANGED;
        if (i instanceof ShieldItem) return Wave28Rules.Held.SHIELD;
        return Wave28Rules.Held.OTHER;
    }

    private static void dropProf(ServerPlayer p) {
        Wave28Util.dropMod(p, ATK, "prof_atk");
        Wave28Util.dropMod(p, ARMOR, "prof_armor");
        Wave28Util.dropMod(p, KB, "prof_kb");
        Wave28Util.dropMod(p, LUCK, "prof_luck");
    }

    /** Once a second while the gloves are worn: the proficiency of whatever is in the main hand (a blade, an axe, a digging tool, a bow, a shield, anything else). */
    static void profPass(ServerPlayer p) {
        CompoundTag d = Wave28Util.data(p);
        Wave28Rules.Prof pr = Wave28Rules.proficiency(heldKind(p.getMainHandItem()));
        if (!pr.name().equals(d.getString(PROF_KIND))) {
            dropProf(p);
            d.putString(PROF_KIND, pr.name());
        }
        if (pr.attack() > 0) Wave28Util.ensureMod(p, ATK, "prof_atk", pr.attack(), Operation.ADD_VALUE);
        if (pr.armor() > 0) Wave28Util.ensureMod(p, ARMOR, "prof_armor", pr.armor(), Operation.ADD_VALUE);
        if (pr.knockback() > 0) Wave28Util.ensureMod(p, KB, "prof_kb", pr.knockback(), Operation.ADD_VALUE);
        if (pr.luck() > 0) Wave28Util.ensureMod(p, LUCK, "prof_luck", pr.luck(), Operation.ADD_VALUE);
        if (pr.hasteAmp() >= 0) Wave28Util.giveQuiet(p, 60, pr.hasteAmp(), "minecraft:haste");
        if (!pr.skill().isEmpty()) Fx.lendSkill(p, pr.skill(), 100);
    }

    /** For a player who no longer wears the gloves: take the borrowed proficiency off (a cheap look at one key). */
    static void profCleanup(ServerPlayer p) {
        CompoundTag d = Wave28Util.peek(p);
        if (d == null || !d.contains(PROF_KIND)) return;
        dropProf(p);
        d.remove(PROF_KIND);
    }

    // ================================================================ The Gauntlet of Gamelon
    private static final String GAM = "gam_idx";

    /** Turn the gem: the next of the twelve spells (earth, fire, water, air; three each). */
    static void gamelonTurn(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave28Util.data(p);
        int next = Wave28Rules.gamelonNext(d.getInt(GAM));
        d.putInt(GAM, next);
        Wave28Util.tell(p, "Gamelon: " + Wave28Rules.gamelonName(next));
        Wave28Util.burst(p.serverLevel(), p.position().add(0, 1.3, 0), FxTone.LIGHT, 4);
    }

    /** Cast the spell of the gem that is turned up. */
    static void gamelonCast(ActCtx c) {
        ServerPlayer p = c.player();
        int idx = Math.floorMod(Wave28Util.data(p).getInt(GAM), Wave28Rules.GAMELON_SLOTS);
        ServerLevel l = p.serverLevel();
        switch (idx) {
            case 0 -> levitateStone(p, l);
            case 1 -> {
                Wave28Util.give(p, 600, 0, "minecraft:resistance");
                Wave28Util.burst(l, p.position().add(0, 1, 0), FxTone.LIGHT, 8);
            }
            case 2 -> earthSpikes(p, l);
            case 3 -> flameBolt(p, l);
            case 4 -> {
                for (LivingEntity t : Wave28Util.near(l, p.position(), 4.0, e -> Wave28Util.foe(p, e))) {
                    t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 80));
                    Fx.damage(p, t, 2f, "minecraft:on_fire");
                    Vec3 away = t.position().subtract(p.position());
                    Vec3 push = new Vec3(away.x, 0, away.z);
                    if (push.lengthSqr() > 1.0e-4) push = push.normalize().scale(0.5);
                    t.setDeltaMovement(push.x, 0.2, push.z);
                    t.hurtMarked = true;
                }
                FxRelic.ring(l, p.position(), 4.0, 0xFF7A20);
            }
            case 5 -> {
                Wave28Util.give(p, 1200, 0, "minecraft:fire_resistance");
                FxRelic.ring(l, p.position(), 1.6, 0xFF7A20);
            }
            case 6 -> iceBridge(p, l);
            case 7 -> {
                Fx.heal(p, 6f);
                Wave28Util.give(p, 100, 0, "minecraft:regeneration");
                Wave28Util.burst(l, p.position().add(0, 1, 0), FxTone.LIGHT, 8);
            }
            case 8 -> {
                for (LivingEntity t : Wave28Util.near(l, p.position(), 5.0, e -> Wave28Util.foe(p, e))) {
                    Fx.damage(p, t, 3f, "minecraft:magic");
                    Wave28Util.give(t, 60, 1, "minecraft:slowness");
                    Vec3 away = t.position().subtract(p.position());
                    Vec3 push = new Vec3(away.x, 0, away.z);
                    if (push.lengthSqr() > 1.0e-4) push = push.normalize().scale(0.9);
                    t.setDeltaMovement(push.x, 0.25, push.z);
                    t.hurtMarked = true;
                }
                Wave28Util.give(p, 600, 0, "minecraft:water_breathing");
                FxRelic.ring(l, p.position(), 5.0, 0x4AA8FF);
            }
            case 9 -> {
                Fx.dash(p, 6.0);
                p.setDeltaMovement(p.getDeltaMovement().x, Math.max(p.getDeltaMovement().y, 0.55), p.getDeltaMovement().z);
                p.hurtMarked = true;
                Wave28Util.give(p, 140, 0, "minecraft:slow_falling");
                Wave28Util.burst(l, p.position(), FxTone.LIGHT, 8);
            }
            case 10 -> windCutter(p, l);
            default -> {
                Wave28Util.give(p, 1200, 0, "minecraft:slow_falling");
                Wave28Util.give(p, 400, 1, "minecraft:jump_boost");
                Wave28Util.burst(l, p.position().add(0, 0.5, 0), FxTone.LIGHT, 8);
            }
        }
        l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 0.9f + 0.1f * (idx % 4));
    }

    /** Levitate stone: the foe you look at (14 blocks) is lifted for three seconds, then drifts down gently. */
    private static void levitateStone(ServerPlayer p, ServerLevel l) {
        Wave28Util.Aim a = Wave28Util.aim(p, 14.0, e -> Wave28Util.foe(p, e));
        Wave28Util.trail(l, p.getEyePosition().add(0, -0.3, 0), a.end(), FxTone.LIGHT);
        if (a.hit() == null) return;
        Wave28Util.give(a.hit(), 50, 0, "minecraft:levitation");
        Wave28Util.give(a.hit(), 200, 0, "minecraft:slow_falling");
    }

    /** Earth spikes: stone rises under every foe in a cone of 5 blocks in front of you. */
    private static void earthSpikes(ServerPlayer p, ServerLevel l) {
        for (LivingEntity t : Wave28Util.cone(p, 5.0, 0.5)) {
            Fx.damage(p, t, 6f, "minecraft:stalagmite");
            Wave28Util.give(t, 60, 1, "minecraft:slowness");
            Wave28Util.burst(l, t.position(), FxTone.DARK, 6);
        }
    }

    private static void flameBolt(ServerPlayer p, ServerLevel l) {
        Wave28Util.Aim a = Wave28Util.aim(p, 18.0, e -> Wave28Util.foe(p, e));
        Wave28Util.trail(l, p.getEyePosition().add(0, -0.3, 0), a.end(), FxTone.LIGHT);
        if (a.hit() == null) return;
        Fx.damage(p, a.hit(), 5f, "minecraft:on_fire");
        a.hit().setRemainingFireTicks(Math.max(a.hit().getRemainingFireTicks(), 80));
    }

    /** Wind cutter: a blade of air along your look (8 blocks) cuts every foe near the line. */
    private static void windCutter(ServerPlayer p, ServerLevel l) {
        Vec3 look = p.getViewVector(1f);
        Wave28Util.trail(l, p.getEyePosition().add(0, -0.3, 0), p.getEyePosition().add(look.scale(8.0)), FxTone.LIGHT);
        for (LivingEntity t : Wave28Util.cone(p, 8.0, 0.93)) Fx.damage(p, t, 5f, "minecraft:player_attack");
    }

    /**
     * Ice bridge: the water surface ahead (10 long, 3 wide, at your feet's level) freezes to frosted ice that melts back by itself. Water only (over air
     * it would melt into water); never in the Dungeon, whose blocks cannot be changed; only where you may build.
     */
    private static void iceBridge(ServerPlayer p, ServerLevel l) {
        if (DungeonGuard.on(l)) {
            Wave28Util.tell(p, "The Dungeon's stone and water will not take the ice.", net.minecraft.ChatFormatting.GRAY);
            return;
        }
        Vec3 look = p.getViewVector(1f);
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        int y = BlockPos.containing(p.position()).getY() - 1;
        int placed = 0;
        BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
        for (int i = 1; i <= Wave28Rules.ICE_BRIDGE_LENGTH; i++) {
            for (int w = -(Wave28Rules.ICE_BRIDGE_WIDTH / 2); w <= Wave28Rules.ICE_BRIDGE_WIDTH / 2; w++) {
                Vec3 at = p.position().add(dir.scale(i)).add(side.scale(w));
                BlockPos pos = BlockPos.containing(at.x, y, at.z);
                BlockState s = l.getBlockState(pos);
                if (s.is(Blocks.WATER) && s.getFluidState().isSource() && s.getFluidState().is(FluidTags.WATER) && l.getBlockState(pos.above()).isAir()
                        && l.mayInteract(p, pos)) {
                    DungeonGuard.internal(() -> l.setBlock(pos, ice, 3));
                    placed++;
                }
            }
        }
        if (placed == 0) Wave28Util.tell(p, "There is no still water at your feet's level ahead.", net.minecraft.ChatFormatting.GRAY);
        else Wave28Util.trail(l, p.position(), p.position().add(dir.scale(Wave28Rules.ICE_BRIDGE_LENGTH)), FxTone.LIGHT);
    }
}
