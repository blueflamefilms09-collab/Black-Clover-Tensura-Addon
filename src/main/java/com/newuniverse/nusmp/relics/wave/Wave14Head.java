package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Breath;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Habitat;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Pool;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

/**
 * Wave 14, the head relics: the three dragon circlets (a breath each, and a trait), the Tiny Duck Crown (a flock at dawn), the Unicorn Crown and
 * its dark twin (charges of healing or a void beam), the Circlet of the Phoenix (feather fall, and a fire that raises the dead wearer once), the
 * Crown of the Conjurer (six charges of fey) and the Diadem of Fabulous Truthiness (one truth a day). Numbers live in {@link Wave14Rules}; per-player
 * values are kept in {@link Wave14Util#data}.
 */
final class Wave14Head {
    private Wave14Head() {}

    private static final String K_SIEGE = "siege_until", K_DUCK_DAY = "duck_day", K_DUCK_POOL = "duck_pool", K_HAIR = "hair", K_PHX = "phx_ready",
            K_CJ_EXTRA = "cj_extra", K_CJ_SWIFT = "cj_swift", K_DIADEM = "diadem_day";
    private static final String P_UNI = "uc", P_DARK = "dk", P_CONJ = "cj";
    private static final String[] CIRCLETS = {"gold_circlet", "silver_circlet", "steel_circlet"};
    private static final String[][] FEY = {
            {"tensura:sylphide", "minecraft:vex"}, {"tensura:undine", "minecraft:vex"},
            {"tensura:salamander", "minecraft:iron_golem"}, {"tensura:unicorn", "tensura:pegasus", "minecraft:iron_golem"}};
    private static final Map<UUID, List<UUID>> FLOCKS = new HashMap<>();

    private static IntUnaryOperator rng(ServerPlayer p) { return bound -> p.getRandom().nextInt(bound); }

    // ================================================================ the dragon circlets
    /** How many of the three dragon circlets the player carries anywhere (worn, in the pack, in the off hand), each counted once. */
    static int combined(ServerPlayer p) {
        boolean[] has = new boolean[CIRCLETS.length];
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            RelicDef d = Relics.defOf(inv.getItem(i));
            if (d == null) continue;
            for (int k = 0; k < CIRCLETS.length; k++) if (CIRCLETS[k].equals(d.id())) has[k] = true;
        }
        return Wave14Rules.circletsCombined(has[0], has[1], has[2]);
    }

    /** The breath: a cone of the circlet's element in front of you, 9 blocks long. Every other circlet you carry adds a quarter; all three add the other two stings. */
    static void breath(ActCtx c, Breath b) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        int n = combined(p);
        float dmg = Wave14Rules.breathDamage(n);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        int hits = 0;
        for (LivingEntity e : Fx.foesAround(p, Wave14Rules.BREATH_RANGE)) {
            Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
            if (!Wave14Rules.inCone(d.x, d.y, d.z, look.x, look.y, look.z, Wave14Rules.BREATH_COS_HALF, Wave14Rules.BREATH_RANGE)) continue;
            hits++;
            Fx.damageHeavy(p, e, dmg, b.damageType);
            sting(p, e, b, d);
            if (Wave14Rules.trinity(n)) for (Breath o : Breath.values()) if (o != b) sting(p, e, o, d);
        }
        for (int i = 1; i <= 9; i++) {                                                 // the cone drawn in the air
            for (int k = 0; k < 5; k++) {
                double spread = i * 0.12;
                Vec3 at = eye.add(look.scale(i)).add((p.getRandom().nextDouble() - 0.5) * spread * 2, (p.getRandom().nextDouble() - 0.5) * spread * 2,
                        (p.getRandom().nextDouble() - 0.5) * spread * 2);
                sl.sendParticles(Wave14Util.dust(b.rgb, 1.6f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
            Vec3 at = eye.add(look.scale(i));
            sl.sendParticles(b == Breath.FIRE ? ParticleTypes.FLAME : b == Breath.COLD ? ParticleTypes.SNOWFLAKE : ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, 0.15 * i * 0.3, 0.15 * i * 0.3, 0.15 * i * 0.3, 0.02);
        }
        sl.playSound(null, p.blockPosition(), b == Breath.THUNDER ? SoundEvents.LIGHTNING_BOLT_IMPACT : b == Breath.COLD ? SoundEvents.PLAYER_HURT_FREEZE : SoundEvents.DRAGON_FIREBALL_EXPLODE,
                SoundSource.PLAYERS, 1.0f, 0.8f);
        if (b == Breath.FIRE) igniteBlocks(p, eye, look);
        Wave14Util.tell(p, (hits == 0 ? "Your breath finds nothing." : "Your " + b.label + " breath strikes " + hits + (hits == 1 ? " foe." : " foes.")) + (n > 1 ? " (" + n + " circlets)" : ""), ChatFormatting.GOLD);
    }

    /** The element's own sting on a creature the breath hit. */
    private static void sting(ServerPlayer p, LivingEntity e, Breath b, Vec3 away) {
        switch (b) {
            case FIRE -> {
                if (!e.fireImmune()) e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), Wave14Rules.BREATH_BURN_TICKS));
            }
            case COLD -> {
                Wave14Util.give(e, 80, 1, "tensura:frost", "minecraft:slowness");
                e.setTicksFrozen(e.getTicksRequiredToFreeze() + 20);
            }
            default -> {
                Vec3 n = away.lengthSqr() < 1.0e-6 ? p.getLookAngle() : away.normalize();
                e.push(n.x * 1.1, 0.35, n.z * 1.1);
                e.hurtMarked = true;
                Wave14Util.give(e, 80, 0, "minecraft:nausea");
            }
        }
    }

    /** Dragon Fire: the breath lights the first flammable thing in its way (up to the first solid block), at most one fire. */
    private static void igniteBlocks(ServerPlayer p, Vec3 eye, Vec3 look) {
        ServerLevel sl = p.serverLevel();
        if (!sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        for (double d = 1.0; d <= Wave14Rules.BREATH_RANGE; d += 1.0) {
            BlockPos at = BlockPos.containing(eye.add(look.scale(d)));
            BlockState st = sl.getBlockState(at);
            if (st.isAir()) continue;
            if (burns(st)) {
                for (Direction dir : Direction.values()) {
                    BlockPos n = at.relative(dir);
                    if (!sl.isEmptyBlock(n) || !sl.mayInteract(p, n)) continue;
                    BlockState fire = BaseFireBlock.getState(sl, n);
                    if (fire.canSurvive(sl, n)) {
                        sl.setBlockAndUpdate(n, fire);
                        return;
                    }
                }
            }
            return;                                                                    // the breath stops at the first solid thing
        }
    }

    /** Will this block catch fire (leaves, wool, planks, burnable logs, wooden stairs, slabs and fences, flowers, saplings, beds, hay, bookshelves)? */
    private static boolean burns(BlockState st) {
        return st.is(BlockTags.LEAVES) || st.is(BlockTags.WOOL) || st.is(BlockTags.WOOL_CARPETS) || st.is(BlockTags.PLANKS) || st.is(BlockTags.LOGS_THAT_BURN)
                || st.is(BlockTags.WOODEN_STAIRS) || st.is(BlockTags.WOODEN_SLABS) || st.is(BlockTags.WOODEN_FENCES) || st.is(BlockTags.FLOWERS)
                || st.is(BlockTags.SAPLINGS) || st.is(BlockTags.BEDS) || st.is(Blocks.HAY_BLOCK) || st.is(Blocks.BOOKSHELF);
    }

    /** Dragon Fire, as the damage lands: a creature that was burnt by the wearer's fire goes on burning for five seconds. */
    static void dragonFire(ServerPlayer p, LivingEntity victim, DamageSource src) {
        if (victim == p || victim.fireImmune() || !src.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return;
        victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), Wave14Rules.BREATH_BURN_TICKS));
    }

    /** Winter's Fury: at half health or less, a melee blow carries an extra d8 of cold (held to 6 against a player); returns the extra, 0 if it does not apply. */
    static float wintersFury(ServerPlayer p, LivingEntity victim) {
        if (!Wave14Rules.wintersFury(p.getHealth(), p.getMaxHealth())) return 0f;
        float extra = Wave14Rules.furyExtra(1 + p.getRandom().nextInt(8), victim instanceof net.minecraft.world.entity.player.Player);
        Wave14Util.give(victim, 40, 0, "minecraft:slowness");
        p.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        return extra;
    }

    // ---------------------------------------------------------------- Siege Monster
    /** Siege Monster: three minutes as a Large creature (half again as tall, +4 damage, twice as fast at breaking blocks); a blow of ten or more breaks it. */
    static void siege(ActCtx c) {
        ServerPlayer p = c.player();
        Wave14Util.data(p).putLong(K_SIEGE, p.level().getGameTime() + Wave14Rules.SIEGE_TICKS);
        siegeApply(p);
        Fx.burst(p.serverLevel(), p.position(), 0x8A93A6);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0f, 0.5f);
        Wave14Util.say(p, "Siege Monster: you swell to a Large creature for three minutes. A single blow of 10 or more breaks your concentration.");
    }

    private static void siegeApply(ServerPlayer p) {
        Wave14Util.setMod(p, "minecraft:generic.scale", "siege_scale", Wave14Rules.SIEGE_SCALE, Operation.ADD_MULTIPLIED_BASE);
        Wave14Util.setMod(p, "minecraft:generic.attack_damage", "siege_damage", Wave14Rules.SIEGE_DAMAGE, Operation.ADD_VALUE);
        Wave14Util.setMod(p, "minecraft:player.block_break_speed", "siege_break", Wave14Rules.SIEGE_BREAK_SPEED, Operation.ADD_MULTIPLIED_BASE);
    }

    private static void siegeDrop(ServerPlayer p) {
        Wave14Util.dropMod(p, "minecraft:generic.scale", "siege_scale");
        Wave14Util.dropMod(p, "minecraft:generic.attack_damage", "siege_damage");
        Wave14Util.dropMod(p, "minecraft:player.block_break_speed", "siege_break");
    }

    /** Once a second for everyone: keeps Siege Monster on (after a relog) or ends it (time up, circlet off). */
    static void siegePass(ServerPlayer p) {
        CompoundTag d = Wave14Util.peek(p);
        if (d == null) return;
        long until = d.getLong(K_SIEGE);
        if (until == 0L) {
            if (Wave14Util.hasMod(p, "minecraft:generic.scale", "siege_scale")) siegeDrop(p);
            return;
        }
        long now = p.level().getGameTime();
        if (now >= until || !RelicFlags.has(p, Wave14Rules.F_STEEL)) {
            d.remove(K_SIEGE);
            siegeDrop(p);
            Wave14Util.tell(p, "You shrink back to your size.", ChatFormatting.GRAY);
        } else siegeApply(p);
    }

    /** A hard blow while enlarged breaks the concentration. */
    static void siegeHit(ServerPlayer p, float amount) {
        CompoundTag d = Wave14Util.peek(p);
        if (d == null || d.getLong(K_SIEGE) == 0L || !Wave14Rules.siegeBreaks(amount)) return;
        d.remove(K_SIEGE);
        siegeDrop(p);
        Wave14Util.tell(p, "The blow breaks your concentration: you shrink back.", ChatFormatting.RED);
    }

    // ================================================================ Tiny Duck Crown
    /** Once a second for a wearer: the dawn roll, and the flock following. */
    static void duckPass(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        CompoundTag d = Wave14Util.data(p);
        long today = Wave14Util.today(sl);
        long dayTime = sl.getDayTime();
        if (Wave14Rules.dailyReady(d.getLong(K_DUCK_DAY), today)) {
            d.putLong(K_DUCK_DAY, Wave14Rules.dailyMark(today));
            int flock = 0;
            if (!Wave14Rules.followOver(dayTime) && Wave14Rules.ducksCome(p.getRandom().nextInt(100))) {
                flock = Wave14Rules.flockSize(habitat(p), rng(p));
                gather(p, flock);
                Wave14Util.say(p, "The Tiny Duck Crown: " + flock + " ducks (chickens, here) come to you and follow you until midnight.");
            } else if (!Wave14Rules.followOver(dayTime)) {
                Wave14Util.tell(p, "A quiet morning: no ducks today.", ChatFormatting.GRAY);
            }
            d.putInt(K_DUCK_POOL, flock);
        }
        List<UUID> flock = FLOCKS.get(p.getUUID());
        if (flock == null) return;
        if (Wave14Rules.followOver(dayTime) || p.isSpectator()) {
            FLOCKS.remove(p.getUUID());
            return;
        }
        flock.removeIf(id -> !follow(sl, id, p));
        if (flock.isEmpty()) FLOCKS.remove(p.getUUID());
    }

    private static Habitat habitat(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        if (sl.dimension() != net.minecraft.world.level.Level.OVERWORLD) return Habitat.HIGHLY_UNLIKELY;
        var biome = sl.getBiome(p.blockPosition());
        if (biome.is(BiomeTags.IS_MOUNTAIN)) return Habitat.UNLIKELY;
        return Habitat.NATURAL;
    }

    /** The chickens within 32 blocks, nearest first, as many as the flock is large (at most 24), come to follow the wearer. */
    private static void gather(ServerPlayer p, int flock) {
        List<LivingEntity> found = Wave14Util.near(p.serverLevel(), p.position(), 32.0, e -> e instanceof Chicken);
        List<UUID> ids = new ArrayList<>();
        for (LivingEntity e : found) {
            if (ids.size() >= Math.min(flock, Wave14Rules.DUCK_FOLLOW_CAP)) break;
            ids.add(e.getUUID());
        }
        if (ids.isEmpty()) FLOCKS.remove(p.getUUID());
        else FLOCKS.put(p.getUUID(), ids);
    }

    private static boolean follow(ServerLevel sl, UUID id, ServerPlayer p) {
        Entity e = sl.getEntity(id);
        if (!(e instanceof Chicken c) || !c.isAlive()) return false;
        if (c.distanceToSqr(p) > 14.0) c.getNavigation().moveTo(p, 1.15);
        return true;
    }

    /** King of Ducks: five ducks of the day's flock assist you (a fortunate hour and a quick hand); they leave after fifteen seconds. */
    static void kingOfDucks(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave14Util.data(p);
        int pool = d.getInt(K_DUCK_POOL);
        if (Wave14Rules.assistsLeft(pool) <= 0) {
            Wave14Util.tell(p, "No ducks are left to help you today.", ChatFormatting.GRAY);
            return;
        }
        d.putInt(K_DUCK_POOL, Wave14Rules.flockAfterAssist(pool));
        for (int i = 0; i < Wave14Rules.DUCKS_PER_ASSIST; i++) Fx.summon(p, "minecraft:chicken", 300);
        Wave14Util.give(p, 3600, 1, "minecraft:luck");
        Wave14Util.give(p, 1200, 0, "minecraft:haste");
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CHICKEN_AMBIENT, SoundSource.PLAYERS, 1.2f, 1.3f);
        Wave14Util.say(p, "Bird! Five ducks assist you: luck and a quick hand. " + Wave14Rules.assistsLeft(d.getInt(K_DUCK_POOL)) + " more such calls today.");
    }

    static void forget(ServerPlayer p) { FLOCKS.remove(p.getUUID()); }

    static void clearAll() { FLOCKS.clear(); }

    // ================================================================ Unicorn Crown and Dark Unicorn Crown
    /** Healing Touch: spend a charge and touch a creature (the one you look at within reach, else yourself): 2d8+2 health, and poisons and diseases are gone. */
    static void healingTouch(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave14Util.data(p);
        long today = Wave14Util.today(p.serverLevel());
        Pool pool = Wave14Util.pool(d, P_UNI, Wave14Rules.UNICORN_CHARGES, today, () -> Wave14Rules.regainD3(rng(p)));
        if (!pool.canSpend(1)) {
            Wave14Util.store(d, P_UNI, pool);
            Wave14Util.tell(p, "The Unicorn Crown has no charges left (1d3 return at dawn).", ChatFormatting.GRAY);
            return;
        }
        Wave14Util.store(d, P_UNI, pool.spend(1));
        LivingEntity t = Wave14Util.touched(p);
        int amount = Wave14Rules.healingTouch(rng(p));
        if (t instanceof ServerPlayer tp) Fx.heal(tp, amount);
        else t.heal(amount);
        Wave14Util.remove(t, "minecraft:poison", "minecraft:hunger", "minecraft:nausea", "tensura:infection", "tensura:fatal_poison", "tensura:magicule_poison");
        p.serverLevel().sendParticles(Wave14Util.dust(0xFF9AD5, 1.4f), t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 18, 0.4, 0.5, 0.4, 0.02);
        p.serverLevel().sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + t.getBbHeight() + 0.2, t.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        Wave14Util.say(p, "Healing Touch: " + (t == p ? "you recover " : t.getName().getString() + " recovers ") + amount + " health and is cleansed. " + Wave14Util.pool(d, P_UNI, Wave14Rules.UNICORN_CHARGES, today, () -> 0).have() + " charges left.");
    }

    /** Detect Evil and Good, at will: every hostile creature within 24 blocks glows for ten seconds. */
    static void detectEvil(ActCtx c) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity e : Fx.foesAround(p, 24.0)) {
            Wave14Util.give(e, 200, 0, "minecraft:glowing");
            n++;
        }
        p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 24, 1.5, 0.8, 1.5, 0.05);
        Wave14Util.tell(p, n == 0 ? "You sense no evil near." : "You sense " + n + (n == 1 ? " hostile presence." : " hostile presences."), ChatFormatting.LIGHT_PURPLE);
    }

    /** Druidcraft: the crop or sapling you look at (within 5 blocks) grows as if fed bone meal. */
    static void druidcraft(ActCtx c) {
        ServerPlayer p = c.player();
        HitResult hr = p.pick(5.0, 1.0f, false);
        if (hr instanceof BlockHitResult b && hr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = b.getBlockPos();
            ServerLevel sl = p.serverLevel();
            BlockState st = sl.getBlockState(pos);
            if (sl.mayInteract(p, pos) && st.getBlock() instanceof BonemealableBlock bb && bb.isValidBonemealTarget(sl, pos, st)) {
                if (bb.isBonemealSuccess(sl, sl.random, pos, st)) bb.performBonemeal(sl, sl.random, pos, st);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.4, 0.3, 0.4, 0.0);
                return;
            }
        }
        p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
        Wave14Util.tell(p, "Druidcraft: nothing here wants to grow.", ChatFormatting.GRAY);
    }

    /** Pass without Trace: you and your friends within 8 blocks walk unnoticed for a minute (Tensura's presence concealment). */
    static void passWithoutTrace(ActCtx c) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity e : Wave14Util.friendsAround(p, 8.0)) {
            if (Wave14Util.give(e, 1200, 0, "tensura:presence_concealment", "minecraft:invisibility")) n++;
        }
        p.serverLevel().sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.5, p.getZ(), 20, 0.8, 0.4, 0.8, 0.01);
        Wave14Util.tell(p, "Pass without Trace: " + n + (n == 1 ? " walks" : " walk") + " unnoticed for a minute.", ChatFormatting.GRAY);
    }

    /** Change the colour of your hair (the crown shimmers in it): eight colours in turn. */
    static void hair(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave14Util.data(p);
        int h = Wave14Rules.nextHair(d.getInt(K_HAIR));
        d.putInt(K_HAIR, h);
        Fx.burst(p.serverLevel(), p.position().add(0, 0.8, 0), Wave14Rules.HAIR[h]);
        Wave14Util.tell(p, "Your hair shimmers a new colour.", ChatFormatting.LIGHT_PURPLE);
    }

    /** Once a second for a wearer of the Unicorn Crown: a few motes of the hair colour round the head. */
    static void hairGlow(ServerPlayer p) {
        if (p.isInvisible() || p.isSpectator()) return;
        CompoundTag d = Wave14Util.peek(p);
        int h = d == null ? 0 : Math.floorMod(d.getInt(K_HAIR), Wave14Rules.HAIR.length);
        double y = p.getEyeY() + 0.35;
        for (int i = 0; i < 3; i++) {
            double a = p.tickCount * 0.1 + i * 2.09;
            p.serverLevel().sendParticles(Wave14Util.dust(Wave14Rules.HAIR[h], 0.9f), p.getX() + Math.cos(a) * 0.45, y, p.getZ() + Math.sin(a) * 0.45, 1, 0, 0, 0, 0);
        }
    }

    /** Void Beam: spend a charge (they return at midnight) and send a beam of dark energy at a creature within 36 blocks: 4d6 necrotic, half on a saving throw. */
    static void voidBeam(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave14Util.data(p);
        long midnight = Wave14Rules.midnightDayOf(p.serverLevel().getDayTime());
        Pool pool = Wave14Util.pool(d, P_DARK, Wave14Rules.DARK_CHARGES, midnight, () -> Wave14Rules.regainD3(rng(p)));
        if (!pool.canSpend(1)) {
            Wave14Util.store(d, P_DARK, pool);
            Wave14Util.tell(p, "The Dark Unicorn Crown has no charges left (1d3 return at midnight).", ChatFormatting.GRAY);
            return;
        }
        Wave14Util.Aim a = Wave14Util.aim(p, Wave14Rules.VOID_RANGE);
        ServerLevel sl = p.serverLevel();
        if (a.hit() == null) {
            Wave14Util.store(d, P_DARK, pool);
            Wave14Util.tell(p, "No creature in sight within 36 blocks: the charge stays.", ChatFormatting.GRAY);
            return;
        }
        Wave14Util.store(d, P_DARK, pool.spend(1));
        boolean saved = p.getRandom().nextDouble() < Wave14Rules.VOID_SAVE_CHANCE;
        float dmg = Wave14Rules.voidBeam(rng(p), saved);
        Vec3 to = a.hit().getBoundingBox().getCenter();
        Wave14Util.trail(sl, p.getEyePosition().add(0, -0.2, 0), to, Wave14Util.dust(0x3A0A1A, 1.5f));
        Wave14Util.trail(sl, p.getEyePosition().add(0, -0.2, 0), to, Wave14Util.dust(0xB0182E, 0.9f));
        sl.sendParticles(ParticleTypes.SQUID_INK, to.x, to.y, to.z, 12, 0.3, 0.3, 0.3, 0.04);
        Fx.damage(p, a.hit(), dmg, "minecraft:wither");
        if (!saved) Wave14Util.give(a.hit(), 40, 0, "minecraft:wither");
        sl.playSound(null, p.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 1.4f);
        Wave14Util.tell(p, "Void Beam: " + (int) dmg + " necrotic" + (saved ? " (half: it dodged)" : "") + ". " + (pool.have() - 1) + " charges left.", ChatFormatting.DARK_RED);
    }

    /** Thaumaturgy: a booming, ominous voice: weak hostile creatures within 8 blocks flinch in fear for three seconds. */
    static void thaumaturgy(ActCtx c) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity e : Fx.foesAround(p, 8.0)) {
            if (!Wave14Util.softMob(p, e) || Wave14Util.saves(p, e)) continue;
            Wave14Util.give(e, 60, 0, "tensura:fear", "minecraft:weakness");
            n++;
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.6f);
        Fx.ring(p.serverLevel(), p.position(), 3.0, 0x6A0F26);
        Wave14Util.tell(p, n == 0 ? "Your voice booms; nothing flinches." : n + " creatures flinch from your voice.", ChatFormatting.DARK_RED);
    }

    /**
     * The evil deed (the Unicorn Crown): killing a villager, a trader, a golem or a tamed pet ends the attunement in a flash of rainbow light. The crown stays
     * in your hands (nothing is lost) but it will not answer you again until you attune it anew.
     */
    static void evilDeed(ServerPlayer killer, LivingEntity victim) {
        boolean innocent = victim instanceof net.minecraft.world.entity.npc.AbstractVillager || victim instanceof net.minecraft.world.entity.animal.IronGolem
                || (victim instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame()) || isTensuraFey(victim);
        if (!innocent) return;
        release(killer, "unicorn_crown", "The Unicorn Crown: an evil deed. Your attunement ends in a flash of rainbow light.", 0xFFFFFF);
    }

    private static boolean isTensuraFey(LivingEntity e) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return "tensura".equals(key.getNamespace()) && ("unicorn".equals(key.getPath()) || "pegasus".equals(key.getPath()) || "pegacorn".equals(key.getPath()));
    }

    /** The Dark Unicorn Crown: aiding a good unicorn (feeding a horse, here) ends the attunement in a vortex of shadow. */
    static void aidedUnicorn(ServerPlayer p) {
        release(p, "dark_unicorn_crown", "The Dark Unicorn Crown: you aided a good unicorn. Your attunement ends in a vortex of shadow.", 0x3A0A1A);
    }

    private static void release(ServerPlayer p, String relicId, String message, int colour) {
        for (Relics.Worn w : Relics.wornList(p)) {
            if (!w.def().id().equals(relicId)) continue;
            Attune.release(p, w.stack());
            Fx.burst(p.serverLevel(), p.position(), colour);
            Wave14Util.say(p, message);
            return;
        }
    }

    // ================================================================ Circlet of the Phoenix
    /** Feather Fall, as often as you like: twenty seconds of slow falling (and the circlet already takes the sting out of every fall). */
    static void featherFall(ActCtx c) {
        Wave14Util.give(c.player(), 400, 0, "minecraft:slow_falling");
        c.player().serverLevel().sendParticles(ParticleTypes.FLAME, c.player().getX(), c.player().getY() + 0.2, c.player().getZ(), 8, 0.3, 0.1, 0.3, 0.01);
    }

    /**
     * Up in Flames (hidden): when a blow would kill the wearer, the circlet bursts: foes within 3 blocks take 2d10 fire (half if they shrug it off), the wearer
     * rises at full health, ashen and weak for a minute, and the circlet will not do it again for three days. Equipment is never lost. Returns true if it fired.
     */
    static boolean phoenixRise(ServerPlayer p, DamageSource src, LivingDeathEvent e) {
        if (src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        CompoundTag d = Wave14Util.data(p);
        long now = p.level().getGameTime();
        if (now < d.getLong(K_PHX)) return false;
        d.putLong(K_PHX, now + 72000L);
        e.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        p.clearFire();
        Wave14Util.giveQuiet(p, 100, 4, "minecraft:resistance");
        Wave14Util.giveQuiet(p, 400, 0, "minecraft:fire_resistance");
        Wave14Util.give(p, 1200, 1, "minecraft:weakness");
        Wave14Util.give(p, 600, 0, "minecraft:glowing");
        ServerLevel sl = p.serverLevel();
        for (LivingEntity f : Fx.foesAround(p, 3.0)) {
            float dmg = Wave14Rules.dice(rng(p), 2, 10);
            if (p.getRandom().nextBoolean()) dmg /= 2f;
            Fx.damageHeavy(p, f, dmg, "minecraft:in_fire");
            if (!f.fireImmune()) f.setRemainingFireTicks(100);
        }
        sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 80, 1.2, 0.8, 1.2, 0.12);
        sl.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 1.0, p.getZ(), 20, 1.0, 0.6, 1.0, 0.0);
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
        Fx.ring(sl, p.position(), 3.0, 0xFF7A1C);
        Wave14Util.say(p, "Up in Flames: you erupt in fire and rise again, ashen and weak. The circlet is spent for three days.");
        return true;
    }

    // ================================================================ Crown of the Conjurer
    /** Conjure Fey: spend 1 charge, plus more for a stronger creature (set with Set Strength), plus 2 to skip the three-second circle (Swift Casting). */
    static void conjure(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave14Util.data(p);
        long today = Wave14Util.today(p.serverLevel());
        int extra = Wave14Rules.clamp(d.getInt(K_CJ_EXTRA), 0, 3);
        boolean swift = d.getBoolean(K_CJ_SWIFT);
        int cost = Wave14Rules.conjureCost(extra, swift);
        Pool pool = Wave14Util.pool(d, P_CONJ, Wave14Rules.CONJURER_CHARGES, today, () -> Wave14Rules.regainConjurer(rng(p)));
        if (!pool.canSpend(cost)) {
            Wave14Util.store(d, P_CONJ, pool);
            Wave14Util.tell(p, "Conjure Fey needs " + cost + " charges; the crown has " + pool.have() + ".", ChatFormatting.GRAY);
            return;
        }
        Wave14Util.store(d, P_CONJ, pool.spend(cost));
        int power = Wave14Rules.conjurePower(extra);
        if (swift) finishConjure(p, power);
        else {
            Fx.ring(p.serverLevel(), p.position(), 2.5, 0x6FE08A);
            Wave14Zones.later(p, Wave14Rules.CONJURE_RITUAL, Wave14Zones.KIND_CONJURE, power);
            Wave14Util.tell(p, "The circle is drawn: the fey comes in three seconds.", ChatFormatting.GREEN);
        }
    }

    /** The conjuring is done: the fey creature of this strength steps out of the circle and fights for you. */
    static void finishConjure(ServerPlayer p, int power) {
        if (!p.isAlive()) return;
        String[] ladder = FEY[Wave14Rules.clamp(power, 1, 4) - 1];
        LivingEntity fey = null;
        for (String id : ladder) {
            fey = Fx.summon(p, id, Wave14Rules.conjureLife(power));
            if (fey != null) break;
        }
        Fx.ring(p.serverLevel(), p.position(), 2.0, 0x6FE08A);
        Wave14Util.tell(p, fey == null ? "The circle holds nothing: the fey did not come." : "A fey creature answers (strength " + power + ").", ChatFormatting.GREEN);
    }

    /** Set Strength: choose how many extra charges the next conjuring spends (0 to 3); each makes the creature stronger. */
    static void conjureStrength(ActCtx c) {
        CompoundTag d = Wave14Util.data(c.player());
        int extra = (Wave14Rules.clamp(d.getInt(K_CJ_EXTRA), 0, 3) + 1) % 4;
        d.putInt(K_CJ_EXTRA, extra);
        Wave14Util.tell(c.player(), "Conjure Fey will spend " + Wave14Rules.conjureCost(extra, d.getBoolean(K_CJ_SWIFT)) + " charges for a creature of strength " + Wave14Rules.conjurePower(extra) + ".", ChatFormatting.GREEN);
    }

    /** Swift Casting: switch the quick casting on or off (+2 charges, no circle to wait for). */
    static void conjureSwift(ActCtx c) {
        CompoundTag d = Wave14Util.data(c.player());
        boolean on = !d.getBoolean(K_CJ_SWIFT);
        d.putBoolean(K_CJ_SWIFT, on);
        Wave14Util.tell(c.player(), on ? "Swift Casting on: +2 charges, no circle." : "Swift Casting off.", ChatFormatting.GREEN);
    }

    // ================================================================ Diadem of Fabulous Truthiness
    /** Zone of Truth on one creature (once a day): it glows, its disguises fall away, and you read its true measure; you also see truly for two minutes. */
    static void truth(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave14Util.data(p);
        long today = Wave14Util.today(p.serverLevel());
        if (!Wave14Rules.dailyReady(d.getLong(K_DIADEM), today)) {
            Wave14Util.tell(p, "The diadem has spoken its truth for today.", ChatFormatting.GRAY);
            return;
        }
        Wave14Util.Aim a = Wave14Util.aim(p, 24.0);
        LivingEntity t = a.hit();
        if (t == null) {
            Wave14Util.tell(p, "No creature in sight within 24 blocks.", ChatFormatting.GRAY);
            return;
        }
        d.putLong(K_DIADEM, Wave14Rules.dailyMark(today));
        Wave14Util.remove(t, "minecraft:invisibility", "tensura:presence_concealment");
        Wave14Util.give(t, 600, 0, "minecraft:glowing");
        if (t instanceof Mob m && m.getTarget() == p) Wave14Util.give(t, 100, 0, "tensura:confusion", "minecraft:nausea");
        Fx.lendSkill(p, "tensura:eye_of_truth", 2400);
        double ep;
        try {
            ep = EnergyHelper.getMaxEP(t);
        } catch (RuntimeException | LinkageError none) {
            ep = 0.0;
        }
        p.serverLevel().sendParticles(Wave14Util.dust(0x5CF0A0, 1.3f), t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 20, 0.4, 0.5, 0.4, 0.02);
        Wave14Util.say(p, "Zone of Truth: " + t.getName().getString() + " has " + Math.round(t.getHealth()) + " of " + Math.round(t.getMaxHealth()) + " health and an existence of "
                + Math.round(ep) + (t instanceof Mob m && m.getTarget() == p ? ", and it means you harm." : "."));
    }
}
