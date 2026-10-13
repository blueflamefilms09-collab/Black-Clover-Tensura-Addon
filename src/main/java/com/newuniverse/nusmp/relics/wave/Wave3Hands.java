package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Element;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Gauntlet;
import com.newuniverse.nusmp.relics.wave.Wave3Rules.Thermo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;

/**
 * Wave 3, the hand relics: Claws of Greed (stealth and a daily snatch), the Gloves of Instant Cleanliness (a snap that scours), the Elemental Hand
 * Wraps (a chosen element on bare fists), the Thermo-Gloves (fire and frost, a touch of each, a cloud of steam) and the Gauntlets of Eldritch
 * Ferocity (force beams, and the price of a second one).
 */
final class Wave3Hands {
    private Wave3Hands() {}

    private static final String K_SEIZE = "seize", K_WRAPS = "wraps_el", K_THERMO = "thermo", K_BL = "bl_until";
    private static final String BL_MOD = "bl_maxhp";

    /** True while our own extra damage is being dealt, so that it never counts as a fist landing. */
    private static boolean busy;

    // ================================================================ a fist lands
    /** Called for every melee blow a player lands: the claws' snatch, then the extra damage of wraps and Thermo-Gloves (bare fists only). */
    static void onHit(ServerPlayer p, LivingEntity t) {
        if (busy || t == p) return;
        if (!RelicFlags.has(p, Wave3Rules.F_ANY)) return;
        if (RelicFlags.has(p, Wave3Rules.F_CLAWS)) seize(p, t);
        if (!p.getMainHandItem().isEmpty() || !Wave3Util.target(p, t)) return;
        boolean wraps = RelicFlags.has(p, Wave3Rules.F_WRAPS), thermo = RelicFlags.has(p, Wave3Rules.F_THERMO);
        if (!wraps && !thermo) return;
        busy = true;
        try {
            if (wraps) {
                Element el = element(p);
                if (el.active()) elemental(p, t, el, Wave3Rules.WRAPS_EXTRA);
            }
            if (thermo) thermalHit(p, t, mode(p), Wave3Rules.THERMO_EXTRA);
        } finally {
            busy = false;
        }
    }

    // ================================================================ Claws of Greed
    /** Seize: once a day a blow that lands snatches what the creature holds in its hands into yours (a free hand needed; not from players or bosses). */
    private static void seize(ServerPlayer p, LivingEntity t) {
        if (!(t instanceof Mob m) || !Wave3Util.softMob(p, m)) return;
        if (!p.getMainHandItem().isEmpty() && !p.getOffhandItem().isEmpty()) return;
        EquipmentSlot slot = !m.getMainHandItem().isEmpty() ? EquipmentSlot.MAINHAND : !m.getOffhandItem().isEmpty() ? EquipmentSlot.OFFHAND : null;
        if (slot == null) return;
        ServerLevel sl = p.serverLevel();
        long today = Wave3Util.today(sl);
        CompoundTag d = Wave3Util.data(p);
        if (!Wave3Rules.dailyReady(d.getLong(K_SEIZE), today)) return;
        d.putLong(K_SEIZE, Wave3Rules.dailyMark(today));
        ItemStack stolen = m.getItemBySlot(slot).copy();
        m.setItemSlot(slot, ItemStack.EMPTY);
        if (!p.getInventory().add(stolen)) p.drop(stolen, false);
        Vec3 at = m.getBoundingBox().getCenter();
        sl.sendParticles(ParticleTypes.PORTAL, at.x, at.y, at.z, 24, 0.3, 0.3, 0.3, 0.6);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.7f);
        Wave3Util.tell(p, "Seize: the claws snatch " + stolen.getHoverName().getString() + " from " + m.getName().getString() + ".", ChatFormatting.GOLD);
    }

    // ================================================================ Gloves of Instant Cleanliness
    /** Snap: within three blocks everything spotless: you and your friends lose the nausea, hunger, blindness, poison and sickness of dirt, fires and cobwebs go out. Curses stay. */
    static void snap(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        for (LivingEntity e : Wave3Util.friendsAround(p, 3.0)) {
            cleanse(e);
            e.clearFire();
            sl.sendParticles(ParticleTypes.BUBBLE_POP, e.getX(), e.getY() + 0.9, e.getZ(), 10, 0.35, 0.5, 0.35, 0.02);
        }
        BlockPos at = p.blockPosition();
        int cleared = 0;
        for (int dx = -3; dx <= 3 && cleared < 24; dx++) {
            for (int dy = -2; dy <= 3 && cleared < 24; dy++) {
                for (int dz = -3; dz <= 3 && cleared < 24; dz++) {
                    if (dx * dx + dy * dy + dz * dz > 10) continue;
                    BlockPos bp = at.offset(dx, dy, dz);
                    BlockState st = sl.getBlockState(bp);
                    if ((st.is(Blocks.COBWEB) || st.is(Blocks.FIRE)) && sl.mayInteract(p, bp)) {
                        sl.removeBlock(bp, false);
                        cleared++;
                    }
                }
            }
        }
        sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 16, 1.4, 0.8, 1.4, 0.04);
        sl.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.1f, 1.9f);
        Wave3Util.tell(p, cleared > 0 ? "Snap: spotless (" + cleared + " cobwebs and flames swept away)." : "Snap: spotless.", ChatFormatting.AQUA);
    }

    /** What dirt can do: not magic, not curses. */
    static void cleanse(LivingEntity e) {
        Wave3Util.remove(e, "minecraft:nausea", "minecraft:hunger", "minecraft:blindness", "minecraft:poison", "tensura:infection");
    }

    /** The clean hands keep their wearer free of hunger and nausea (every few seconds). */
    static void cleanPass(ServerPlayer p) { Wave3Util.remove(p, "minecraft:hunger", "minecraft:nausea"); }

    // ================================================================ Elemental Hand Wraps
    static Element element(ServerPlayer p) {
        CompoundTag d = Wave3Util.peek(p);
        return d == null ? Element.OFF : Element.byOrdinal(d.getInt(K_WRAPS));
    }

    /** Elemental Infusion: turn the wraps to the next element (fire, cold, lightning, thunder, acid, then none). */
    static void infusion(ActCtx c) {
        ServerPlayer p = c.player();
        Element next = element(p).next();
        Wave3Util.data(p).putInt(K_WRAPS, next.ordinal());
        ServerLevel sl = p.serverLevel();
        sl.sendParticles(colour(next) == 0 ? ParticleTypes.CLOUD : Wave3Util.dust(colour(next), 1.4f), p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.4, 0.5, 0.4, 0.04);
        Wave3Util.tell(p, next.active() ? "Elemental Infusion: your fists burn with " + next.label + "." : "Elemental Infusion: the wraps are quiet.", ChatFormatting.LIGHT_PURPLE);
    }

    private static int colour(Element el) {
        return switch (el) {
            case FIRE -> 0xFF7A1E;
            case COLD -> 0x9DE8F0;
            case LIGHTNING -> 0xFFF05A;
            case THUNDER -> 0x6F7FD8;
            case ACID -> 0x58B848;
            default -> 0;
        };
    }

    /** The extra blow of the chosen element, each with its own rider. {@code amount} is before the balance law. */
    private static void elemental(ServerPlayer p, LivingEntity t, Element el, float amount) {
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter();
        t.invulnerableTime = 0;                                                         // so the extra damage is not swallowed by the pause after the punch
        switch (el) {
            case FIRE -> {
                Fx.damage(p, t, amount, "minecraft:in_fire");
                t.igniteForSeconds(4);
            }
            case COLD -> {
                Fx.damage(p, t, amount, "minecraft:freeze");
                Wave3Util.give(t, 60, 1, "minecraft:slowness");
                t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 40, t.getTicksFrozen() + 60));
            }
            case LIGHTNING -> {
                Fx.damage(p, t, amount, "minecraft:lightning_bolt");
                List<LivingEntity> others = Wave3Util.near(sl, mid, 4.0, x -> x != t && Wave3Util.target(p, x));
                if (!others.isEmpty()) {
                    LivingEntity o = others.get(0);
                    o.invulnerableTime = 0;
                    Fx.damage(p, o, amount * 0.6f, "minecraft:lightning_bolt");
                    Wave3Util.trail(sl, mid, o.getBoundingBox().getCenter(), Wave3Util.dust(0xFFF05A, 0.9f));
                }
            }
            case THUNDER -> {
                Fx.damage(p, t, amount, "minecraft:sonic_boom");
                t.knockback(0.7, p.getX() - t.getX(), p.getZ() - t.getZ());
                sl.sendParticles(ParticleTypes.SONIC_BOOM, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
            }
            case ACID -> {
                Fx.damage(p, t, amount, "minecraft:magic");
                Wave3Util.give(t, 80, 0, "tensura:corrosion", "minecraft:poison");
            }
            default -> { return; }
        }
        sl.sendParticles(el == Element.COLD ? ParticleTypes.SNOWFLAKE : Wave3Util.dust(colour(el), 1.3f), mid.x, mid.y, mid.z, 10, 0.3, 0.4, 0.3, 0.05);
    }

    // ================================================================ Thermo-Gloves
    static Thermo mode(ServerPlayer p) {
        CompoundTag d = Wave3Util.peek(p);
        return d == null ? Thermo.FIRE : Thermo.byOrdinal(d.getInt(K_THERMO));
    }

    /** Choose which glove strikes: fire (the red one) or cold (the blue one). */
    static void thermoMode(ActCtx c) {
        ServerPlayer p = c.player();
        Thermo next = mode(p).next();
        Wave3Util.data(p).putInt(K_THERMO, next.ordinal());
        Wave3Util.tell(p, "Thermo-Gloves: the " + (next == Thermo.FIRE ? "red" : "blue") + " glove leads; your fists carry " + next.label + ".", next == Thermo.FIRE ? ChatFormatting.RED : ChatFormatting.AQUA);
    }

    private static void thermalHit(ServerPlayer p, LivingEntity t, Thermo mode, float amount) {
        t.invulnerableTime = 0;
        if (mode == Thermo.FIRE) {
            Fx.damage(p, t, amount, "minecraft:in_fire");
            t.igniteForSeconds(3);
        } else {
            Fx.damage(p, t, amount, "minecraft:freeze");
            Wave3Util.give(t, 40, 0, "minecraft:slowness");
            t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 20, t.getTicksFrozen() + 40));
        }
        Vec3 mid = t.getBoundingBox().getCenter();
        p.serverLevel().sendParticles(mode == Thermo.FIRE ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, mid.x, mid.y, mid.z, 6, 0.3, 0.4, 0.3, 0.03);
    }

    /** The red glove's touch: a flame on the flammable thing you look at (as a flint and steel would), or on the creature in front of you. */
    static void ignite(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Wave3Util.Aim near = Wave3Util.aim(p, 3.5);
        if (near.hit() != null) {
            near.hit().igniteForSeconds(4);
            sl.sendParticles(ParticleTypes.FLAME, near.hit().getX(), near.hit().getEyeY(), near.hit().getZ(), 12, 0.3, 0.3, 0.3, 0.04);
            return;
        }
        HitResult h = p.pick(5.0, 1f, false);
        if (!(h instanceof BlockHitResult b) || h.getType() != HitResult.Type.BLOCK) {
            Wave3Util.tell(p, "Nothing to ignite within reach.", ChatFormatting.GRAY);
            return;
        }
        BlockPos pos = b.getBlockPos();
        if (!sl.mayInteract(p, pos)) return;
        BlockState st = sl.getBlockState(pos);
        if (CampfireBlock.canLight(st) || CandleBlock.canLight(st) || CandleCakeBlock.canLight(st)) {
            sl.setBlock(pos, st.setValue(BlockStateProperties.LIT, Boolean.TRUE), 11);
            sl.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }
        BlockPos at = pos.relative(b.getDirection());
        if (sl.mayInteract(p, at) && BaseFireBlock.canBePlacedAt(sl, at, p.getDirection())) {
            sl.playSound(null, at, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            sl.setBlock(at, BaseFireBlock.getState(sl, at), 11);
        } else {
            Wave3Util.tell(p, "Nothing there will take a flame.", ChatFormatting.GRAY);
        }
    }

    /** The blue glove's touch: the water you look at, a block-cube of it (three by three), turns to frosted ice that thaws by itself (no creature is frozen in). */
    static void freeze(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        HitResult h = p.pick(6.0, 1f, true);
        if (!(h instanceof BlockHitResult b) || h.getType() != HitResult.Type.BLOCK || !sl.getFluidState(b.getBlockPos()).is(FluidTags.WATER)) {
            Wave3Util.tell(p, "Look at water within six blocks.", ChatFormatting.GRAY);
            return;
        }
        BlockPos centre = b.getBlockPos();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos bp = centre.offset(dx, dy, dz);
                    if (sl.getBlockState(bp) != water || !ice.canSurvive(sl, bp) || !sl.isUnobstructed(ice, bp, CollisionContext.empty()) || !sl.mayInteract(p, bp)) continue;
                    sl.setBlockAndUpdate(bp, ice);
                    sl.scheduleTick(bp, Blocks.FROSTED_ICE, Mth.nextInt(sl.getRandom(), 60, 120));
                    n++;
                }
            }
        }
        sl.sendParticles(ParticleTypes.SNOWFLAKE, centre.getX() + 0.5, centre.getY() + 0.8, centre.getZ() + 0.5, 20, 0.8, 0.4, 0.8, 0.03);
        sl.playSound(null, centre, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.8f);
        Wave3Util.tell(p, n > 0 ? "The water freezes (" + n + " blocks); it thaws in a while." : "The water would not freeze there.", ChatFormatting.AQUA);
    }

    /** Steam Cloud (twice a day): clap the gloves; a cloud of steam bursts round you. Foes at your side are scalded (2), thrown back and made to fall; the cloud dims and slows your foes for fifteen seconds. */
    static void steam(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Vec3 at = p.position();
        Wave3Field.steam(p, at, 6.0, 300);
        for (LivingEntity e : Wave3Util.near(sl, at, 3.0, x -> Wave3Util.target(p, x))) {
            Fx.damage(p, e, 2.0f, "minecraft:in_fire");
            e.knockback(0.9, at.x - e.getX(), at.z - e.getZ());
            Wave3Util.give(e, 40, 3, "minecraft:slowness");
        }
        sl.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 1.0, at.z, 60, 2.5, 0.9, 2.5, 0.06);
        sl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2f, 0.6f);
    }

    // ================================================================ Gauntlets of Eldritch Ferocity
    private static void beam(ServerPlayer p, LivingEntity t, float dmg, boolean twin) {
        ServerLevel sl = p.serverLevel();
        Vec3 to = t.getBoundingBox().getCenter();
        Wave3Util.trail(sl, p.getEyePosition().add(0, -0.3, 0), to, Wave3Util.dust(twin ? 0x52F2E0 : 0x1FA698, 1.1f));
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, to.x, to.y, to.z, 8, 0.2, 0.25, 0.2, 0.03);
        Fx.damage(p, t, dmg, "minecraft:magic");
    }

    /** Eldritch Blast: one beam of force (2 to 4, never under 2: a 1 counts as a 2) at the creature you look at, within 32 blocks. */
    static void blast(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Wave3Util.Aim a = Wave3Util.aim(p, 32.0);
        if (a.hit() == null) {
            Wave3Util.trail(sl, p.getEyePosition().add(0, -0.3, 0), a.end(), Wave3Util.dust(0x1FA698, 0.8f));
            return;
        }
        beam(p, a.hit(), Gauntlet.beam(1 + sl.getRandom().nextInt(4), false), false);
        sl.playSound(null, p.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.5f, 1.8f);
    }

    /**
     * Eldritch Ferocity: a second beam (up to 3 each, 6 in all), the first at your target and the second at another foe near it, or the same one. The
     * extra beam is not free: on a 1 or 2 of a d20 it turns on you, a bite of necrotic damage that cannot kill and lowers your maximum health for ten
     * minutes or until you sleep.
     */
    static void twin(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Wave3Util.Aim a = Wave3Util.aim(p, 32.0);
        if (a.hit() == null) {
            Wave3Util.trail(sl, p.getEyePosition().add(0, -0.3, 0), a.end(), Wave3Util.dust(0x1FA698, 0.8f));
            Wave3Util.tell(p, "Eldritch Ferocity: no creature in sight.", ChatFormatting.GRAY);
            return;
        }
        LivingEntity first = a.hit();
        LivingEntity second = first;
        for (LivingEntity o : Wave3Util.near(sl, first.position(), 8.0, x -> x != first && Wave3Util.target(p, x))) {
            second = o;
            break;
        }
        beam(p, first, Gauntlet.beam(1 + sl.getRandom().nextInt(4), true), true);
        beam(p, second, Gauntlet.beam(1 + sl.getRandom().nextInt(4), true), true);
        sl.playSound(null, p.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.6f, 1.4f);
        if (Gauntlet.backlash(1 + sl.getRandom().nextInt(20))) backlash(p);
    }

    private static void backlash(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        var r = sl.getRandom();
        float dmg = Gauntlet.backlashDamage(1 + r.nextInt(4), 1 + r.nextInt(4), 1 + r.nextInt(4), p.getHealth());
        if (dmg > 0f) p.hurt(p.damageSources().magic(), dmg);
        float pen = Gauntlet.maxHealthPenalty(dmg, (float) p.getAttributeBaseValue(Attributes.MAX_HEALTH));
        if (pen > 0f) {
            Wave3Util.setMod(p, Attributes.MAX_HEALTH, BL_MOD, -pen, Operation.ADD_VALUE);
            Wave3Util.data(p).putLong(K_BL, sl.getGameTime() + Gauntlet.BACKLASH_TICKS);
        }
        sl.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.4, 0.6, 0.4, 0.05);
        Wave3Util.say(p, "Gauntlets of Eldritch Ferocity: the extra beam turns on you" + (pen > 0f ? " (your maximum health is down " + (int) pen + " until you rest)." : "."));
    }

    /** Once a second: the backlash wears off after ten minutes. */
    static void gauntletPass(ServerPlayer p) {
        CompoundTag d = Wave3Util.peek(p);
        if (d == null) return;
        long until = d.getLong(K_BL);
        if (until > 0 && p.serverLevel().getGameTime() >= until) lift(p, d);
    }

    /** Sleeping heals the price of the gauntlets. */
    static void rested(ServerPlayer p) {
        CompoundTag d = Wave3Util.peek(p);
        if (d != null && d.getLong(K_BL) > 0) lift(p, d);
    }

    private static void lift(ServerPlayer p, CompoundTag d) {
        d.putLong(K_BL, 0);
        Wave3Util.dropMod(p, Attributes.MAX_HEALTH, BL_MOD);
    }
}
