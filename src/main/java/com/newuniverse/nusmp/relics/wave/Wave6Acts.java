package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import com.newuniverse.nusmp.particle.fx.FxRelic;

/**
 * Wave 6: the bodies of the actives of the ten armours and robes. Charges are paid only when the effect really happened (a teleport with nowhere
 * safe to land costs nothing), and the ones that last (Slime Form, Dawnlight, Zero Gravity, Mage Armor) keep their state in the player's persistent data,
 * where {@link Wave6Events} watches it. Server side only.
 */
public final class Wave6Acts {
    private Wave6Acts() {}

    // keys in the player's persistent data
    public static final String K_SLIME = "w6_slime_until", K_SLIME_EXT = "w6_slime_ext", K_DAWN_ON = "w6_dawn_on", K_FLY_ON = "w6_fly_on",
            K_FLY_GRANTED = "w6_fly_granted", K_MAGE_UNTIL = "w6_mage_until", K_DAWN_CD = "w6_dawn_cd";

    private static final int SKY_BLUE = 0x8FD8FF, DAWN_GOLD = 0xFFE9A0, MIST_GREY = 0xA8C8D8, SLIME_GREEN = 0x6BFFB5, SHADOW_VIOLET = 0x3A2A5C, MAGE_BLUE = 0x9AA4FF;
    private static final String NEKET = Wave6Tune.ID_NEKET, COAT = Wave6Tune.ID_COAT;

    // ---------------------------------------------------------------- shared
    private static ItemStack stack(ActCtx c) {
        ItemStack s = Wave6Kit.find(c.player(), c.def().id());
        return s.isEmpty() ? c.stack() : s;
    }

    private static IntUnaryOperator roll(ServerPlayer p) { return n -> p.getRandom().nextInt(n); }

    /** The pool when it can pay {@code cost}, else null (and the player is told). */
    private static Wave6Pool.State canPay(ActCtx c, Wave6Pools kind, int cost, String what) {
        Wave6Pool.State st = Wave6Kit.pool(stack(c), kind, c.player());
        if (Wave6Pool.canSpend(st, cost)) return st;
        Fx.say(c.player(), what + ": " + st.charges() + "/" + kind.max() + " charges, it needs " + cost + ".");
        return null;
    }

    /** Pays {@code cost} from the pool and returns what is left. */
    private static int pay(ActCtx c, Wave6Pools kind, Wave6Pool.State st, int cost) {
        Wave6Pool.State left = Wave6Pool.spend(st, cost);
        Wave6Kit.put(stack(c), kind, left);
        return left.charges();
    }

    private static void left(ServerPlayer p, String what, int charges, Wave6Pools kind) {
        Fx.say(p, what + " (" + charges + "/" + kind.max() + " charges left)");
    }

    // ================================================================ Skycrystal Armor
    /** Absorb Elements (1 charge): twelve seconds of fire ward and a hardened hide, and the absorbed power rides your next blows. */
    public static void skyAbsorbElements(ActCtx c) {
        ServerPlayer p = c.player();
        Wave6Pool.State st = canPay(c, Wave6Pools.SKY, Wave6Tune.SKY_ABSORB_ELEMENTS, "Absorb Elements");
        if (st == null) return;
        Wave6Kit.give(p, "minecraft:fire_resistance", 300, 0, false);
        Wave6Kit.give(p, "minecraft:resistance", 240, 0, false);
        Wave6Kit.give(p, "minecraft:strength", 240, 0, false);
        p.setRemainingFireTicks(0);
        // the tabletop's "resistance to acid, cold, fire, lightning or thunder" becomes four Tensura resistances merged into one cast
        Fx.lendSkills(p, 240, Wave6Skills.FLAME_ATTACK_RESISTANCE, Wave6Skills.COLD_RESISTANCE, Wave6Skills.ELECTRICITY_RESISTANCE, Wave6Skills.WATER_ATTACK_RESISTANCE);
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 1.6, SKY_BLUE);
        skySpent(c, st, Wave6Tune.SKY_ABSORB_ELEMENTS, "Absorb Elements");
    }

    /** Shield (1 charge): three hearts of force for eight seconds. */
    public static void skyShield(ActCtx c) {
        ServerPlayer p = c.player();
        Wave6Pool.State st = canPay(c, Wave6Pools.SKY, Wave6Tune.SKY_SHIELD, "Shield");
        if (st == null) return;
        Fx.shield(p, 6f, 160);
        Fx.lendSkill(p, Wave6Skills.MULTILAYER_BARRIER, 200);                    // the spell's invisible plate of force, as Tensura's barrier skill
        FxRelic.burst(p.serverLevel(), p.position().add(0, 1, 0), SKY_BLUE);
        skySpent(c, st, Wave6Tune.SKY_SHIELD, "Shield");
    }

    /** Globe of Invulnerability (6 charges): for ten seconds no spell can hurt you or your allies within five blocks of where you stand. */
    public static void skyGlobe(ActCtx c) {
        ServerPlayer p = c.player();
        Wave6Pool.State st = canPay(c, Wave6Pools.SKY, Wave6Tune.SKY_GLOBE, "Globe of Invulnerability");
        if (st == null) return;
        if (!Wave6Zones.addGlobe(p, p.position(), Wave6Tune.GLOBE_TICKS)) { Fx.say(p, "Too many globes stand already."); return; }
        Fx.lendSkills(p, Wave6Tune.GLOBE_TICKS, Wave6Skills.MAGIC_NULLIFICATION, Wave6Skills.MAGIC_JAMMING);
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), Wave6Tune.GLOBE_RADIUS, SKY_BLUE);
        FxRelic.burst(p.serverLevel(), p.position().add(0, 1, 0), SKY_BLUE);
        skySpent(c, st, Wave6Tune.SKY_GLOBE, "Globe of Invulnerability: spells cannot touch those inside");
    }

    /** Invulnerability (9 charges, and a slice of your existence points): five seconds in which nothing that can be resisted gets through. */
    public static void skyInvulnerability(ActCtx c) {
        ServerPlayer p = c.player();
        Wave6Pool.State st = canPay(c, Wave6Pools.SKY, Wave6Tune.SKY_INVULN, "Invulnerability");
        if (st == null) return;
        Wave6Kit.give(p, "minecraft:resistance", Wave6Tune.INVULN_TICKS, 4, false);
        p.setRemainingFireTicks(0);
        Fx.lendSkills(p, Wave6Tune.INVULN_TICKS, Wave6Skills.PHYSICAL_ATTACK_NULLIFICATION, Wave6Skills.MAGIC_NULLIFICATION, Wave6Skills.PAIN_NULLIFICATION, Wave6Skills.PHYSICAL_ATTACK_RESISTANCE);
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 2.2, SKY_BLUE);
        FxRelic.burst(p.serverLevel(), p.position().add(0, 1, 0), 0xFFFFFF);
        skySpent(c, st, Wave6Tune.SKY_INVULN, "Invulnerability");
    }

    /** Pays a Skycrystal spell and applies "if you expend the last charge, roll a d10: on a 10 the armour regains 1d4+1". */
    private static void skySpent(ActCtx c, Wave6Pool.State st, int cost, String what) {
        ServerPlayer p = c.player();
        int left = pay(c, Wave6Pools.SKY, st, cost);
        if (left == 0) {
            int back = Wave6Tune.skyLastChargeGain(roll(p));
            if (back > 0) {
                Wave6Kit.put(stack(c), Wave6Pools.SKY, Wave6Pool.add(new Wave6Pool.State(0, st.day()), back, Wave6Pools.SKY.max()));
                Fx.say(p, what + ": the last charge burns out and the crystals flare back, +" + back + " charges!");
                return;
            }
        }
        left(p, what, left, Wave6Pools.SKY);
    }

    // ================================================================ Slime Armour
    /** Slime Form (3 a day): a minute as a small, soft, un-hittable-by-sword puddle that fits through a one-block gap; no items, no spells. Use again to end it. */
    public static void slimeForm(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        if (d.getLong(K_SLIME) > now) { Wave6Events.endSlime(p, true); return; }
        Wave6Pool.State st = canPay(c, Wave6Pools.SLIME, 1, "Slime Form");
        if (st == null) return;
        d.putLong(K_SLIME, now + Wave6Tune.SLIME_TICKS);
        d.putInt(K_SLIME_EXT, 0);
        Wave6Events.slimeMods(p, true);
        // a body of slime shrugs off blows and pain: Tensura's physical, pierce and pain resistances for the whole form
        Fx.lendSkills(p, Wave6Tune.SLIME_TICKS, Wave6Skills.PHYSICAL_ATTACK_RESISTANCE, Wave6Skills.PIERCE_RESISTANCE, Wave6Skills.PAIN_RESISTANCE);
        FxRelic.burst(p.serverLevel(), p.position().add(0, 0.6, 0), SLIME_GREEN);
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 1.4, SLIME_GREEN);
        int left = pay(c, Wave6Pools.SLIME, st, 1);
        left(p, "You and your gear melt into slime for 60 seconds. You cannot use items or cast. Use the armour again to end it", left, Wave6Pools.SLIME);
    }

    // ================================================================ Armor of the Defiant Dawn
    /** Dawnlight: sheds a bright light, shows what hides (invisible creatures glow, Darkness is burned off, you see in the dark). */
    public static void dawnlight(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = p.getPersistentData();
        boolean on = !d.getBoolean(K_DAWN_ON);
        d.putBoolean(K_DAWN_ON, on);
        if (on) FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), Wave6Tune.DAWN_FLASH_RADIUS, DAWN_GOLD);
        Fx.say(p, on ? "The armour sheds the Eternal Light." : "The light dims.");
    }

    /** Luminous Retribution, by hand (1 charge): the same flash the armour throws when you are struck. */
    public static void dawnFlashAct(ActCtx c) {
        ServerPlayer p = c.player();
        Wave6Pool.State st = canPay(c, Wave6Pools.DAWN, 1, "Luminous Retribution");
        if (st == null) return;
        int left = pay(c, Wave6Pools.DAWN, st, 1);
        flash(p);
        left(p, "Luminous Retribution", left, Wave6Pools.DAWN);
    }

    /** The flash itself: 6 radius, the close take it all and are blinded, the far take half; the undead are always caught and take half again. */
    public static void flash(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        FxRelic.ring(sl, p.position().add(0, 0.1, 0), Wave6Tune.DAWN_FLASH_RADIUS, DAWN_GOLD);
        FxRelic.ring(sl, p.position().add(0, 1.0, 0), Wave6Tune.DAWN_CLOSE, 0xFFFFFF);
        FxRelic.burst(sl, p.position().add(0, 1, 0), 0xFFF3C0);
        String light = Wave6Kit.damageType(sl, Wave6Skills.DMG_LIGHT, "minecraft:magic"), holy = Wave6Kit.damageType(sl, Wave6Skills.DMG_HOLY, light);
        for (LivingEntity t : Fx.foesAround(p, Wave6Tune.DAWN_FLASH_RADIUS)) {
            boolean undead = t.getType().is(EntityTypeTags.UNDEAD);
            double dist = p.distanceTo(t);
            Fx.damage(p, t, Wave6Tune.flashDamage(undead, dist), undead ? holy : light);          // "radiant": Tensura's light, and holy against the undead
            int blind = Wave6Tune.flashBlind(undead, dist);
            if (blind > 0) {
                Fx.addEffect(t, "minecraft:blindness", blind, 0);
                Fx.addEffect(t, Wave6Skills.FX_FLASHED_BLINDNESS, blind, 0);
            }
        }
    }

    // ================================================================ Suit of the Astral Overseer
    /** Zero Gravity Movement: switches flight on or off. Flight costs magicules (never hunger, since the 1.99.0 hunger ban) and ends when they run dry. */
    public static void zeroGravity(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = p.getPersistentData();
        boolean on = !d.getBoolean(K_FLY_ON);
        if (on && !Wave2Mana.has(p, Wave6Tune.FLIGHT_MANA_MIN)) { Fx.say(p, "Your magicules are too thin to leave the ground."); return; }
        d.putBoolean(K_FLY_ON, on);
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 1.5, 0xB070FF);
        Fx.say(p, on ? "Gravity lets go of you. Flight costs magicules." : "Gravity takes hold again.");
    }

    // ================================================================ Shadow Robes
    /** Shadow Step (2 a day): from dim light or darkness, step up to 12 blocks along your sight into another dim place; a cloud of necrotic energy stays behind. */
    public static void shadowStep(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        if (!Wave6Tune.isDim(level.getMaxLocalRawBrightness(p.blockPosition()))) { Fx.say(p, "Shadow Step needs dim light or darkness where you stand."); return; }
        Wave6Pool.State st = canPay(c, Wave6Pools.SHADOW, 1, "Shadow Step");
        if (st == null) return;
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        HitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(Wave6Tune.SHADOW_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        int max = hit.getType() == HitResult.Type.MISS ? Wave6Tune.SHADOW_RANGE : (int) Math.floor(eye.distanceTo(hit.getLocation()) - 0.5);
        int d = Wave6Tune.pickDistance(Math.min(max, Wave6Tune.SHADOW_RANGE), Wave6Tune.SHADOW_MIN, n -> standable(level, spot(eye, dir, n)));
        if (d < 0) { Fx.say(p, "No dark place to step to in sight."); return; }
        BlockPos to = spot(eye, dir, d);
        Vec3 from = p.position();
        LivingEntity dragged = nearestHunter(p);
        p.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        p.fallDistance = 0;
        if (dragged != null) {
            dragged.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            Fx.addEffect(dragged, "minecraft:slowness", 40, 1);
        }
        Fx.lendSkill(p, Wave6Skills.SHADOW_MOTION, 200);                                 // the step is Tensura's shadow motion, lent for ten seconds
        Wave6Zones.addCloud(p, from);
        FxRelic.burst(level, from.add(0, 1, 0), SHADOW_VIOLET);
        FxRelic.burst(level, p.position().add(0, 1, 0), SHADOW_VIOLET);
        int left = pay(c, Wave6Pools.SHADOW, st, 1);
        left(p, "Shadow Step: a cloud of necrotic energy waits where you stood", left, Wave6Pools.SHADOW);
    }

    private static BlockPos spot(Vec3 eye, Vec3 dir, int n) { return BlockPos.containing(eye.x + dir.x * n, eye.y + dir.y * n - 1.0, eye.z + dir.z * n); }

    /** Room for a body (feet and head clear), ground within two blocks below, and dim light. */
    private static boolean standable(ServerLevel level, BlockPos feet) {
        if (!clear(level, feet) || !clear(level, feet.above())) return false;
        if (clear(level, feet.below()) && clear(level, feet.below(2))) return false;
        return Wave6Tune.isDim(level.getMaxLocalRawBrightness(feet));
    }

    private static boolean clear(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    /** "You can bring one creature you are grappling": the one hostile mob within three blocks that is hunting you (never a player). */
    private static LivingEntity nearestHunter(ServerPlayer p) {
        LivingEntity best = null;
        double bd = Wave6Tune.DRAG_RADIUS * Wave6Tune.DRAG_RADIUS;
        AABB box = p.getBoundingBox().inflate(Wave6Tune.DRAG_RADIUS);
        for (net.minecraft.world.entity.Mob m : p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box, mm -> mm.isAlive() && mm.getTarget() == p)) {
            double dd = m.distanceToSqr(p);
            if (dd <= bd) { bd = dd; best = m; }
        }
        return best;
    }

    // ================================================================ Mistwalker Robe
    /** A Mistwalker cast: pays only if the body says it happened, wraps you in mist (two absorption hearts) and rolls the d20 when the last charge goes. */
    private static void mistCast(ActCtx c, int cost, String what, Predicate<ServerPlayer> body) {
        ServerPlayer p = c.player();
        Wave6Pool.State st = canPay(c, Wave6Pools.MIST, cost, what);
        if (st == null) return;
        if (!body.test(p)) { Fx.say(p, what + ": nowhere safe to go."); return; }
        Fx.shield(p, 2f, 160);
        FxRelic.burst(p.serverLevel(), p.position().add(0, 1, 0), MIST_GREY);
        int left = pay(c, Wave6Pools.MIST, st, cost);
        left(p, what, left, Wave6Pools.MIST);
        if (left == 0 && Wave6Tune.mistMisfire(roll(p))) mistSlip(p);
    }

    /** "On a 1 the robe teleports away": here the mist slips and flings the wearer a short way instead; the robe is never lost. */
    private static void mistSlip(ServerPlayer p) {
        Fx.say(p, "The mist slips its leash and throws you aside!");
        Wave6Kit.give(p, "minecraft:blindness", 60, 0, false);
        Wave6Kit.give(p, "minecraft:nausea", 80, 0, false);
        for (int i = 0; i < 6; i++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2;
            Vec3 at = p.position().add(Math.cos(a) * 8, 0, Math.sin(a) * 8);
            ServerLevel level = p.serverLevel();
            BlockPos feet = BlockPos.containing(at.x, p.getY(), at.z);
            if (clear(level, feet) && clear(level, feet.above()) && !clear(level, feet.below())) { p.teleportTo(at.x, feet.getY(), at.z); return; }
        }
    }

    public static void mistyStep(ActCtx c) { mistCast(c, Wave6Tune.MIST_STEP, "Misty Step", p -> Fx.teleportLook(p, Wave6Tune.MISTY_RANGE)); }

    public static void levitate(ActCtx c) {
        mistCast(c, Wave6Tune.MIST_LEVITATE, "Levitate", p -> {
            Wave6Kit.give(p, "minecraft:levitation", 40, 0, false);
            Wave6Kit.give(p, "minecraft:slow_falling", 600, 0, false);
            Fx.lendSkill(p, Wave6Skills.GRAVITY_FLIGHT, 600);                           // and the controlled float of Tensura's gravity flight for thirty seconds
            return true;
        });
    }

    public static void gaseousForm(ActCtx c) {
        mistCast(c, Wave6Tune.MIST_GASEOUS, "Gaseous Form", p -> {
            Wave6Kit.give(p, "minecraft:invisibility", 160, 0, false);
            Wave6Kit.give(p, "minecraft:resistance", 160, 2, false);
            Wave6Kit.give(p, "minecraft:slow_falling", 200, 0, false);
            Wave6Kit.give(p, "minecraft:speed", 160, 0, false);
            Fx.lendSkill(p, Wave6Skills.WIND_TRANSFORM, 160);                           // a body of vapour: Tensura's wind transformation
            return true;
        });
    }

    public static void farStep(ActCtx c) {
        mistCast(c, Wave6Tune.MIST_FAR, "Far Step", p -> {
            if (!Fx.teleportLook(p, Wave6Tune.FAR_RANGE)) return false;
            Wave6Kit.give(p, "minecraft:speed", 160, 1, false);
            Fx.lendSkill(p, Wave6Skills.SPATIAL_MOTION, 1200);                          // "you can teleport again each turn for one minute": spatial motion for one minute
            return true;
        });
    }

    /** Wind Walk (all five charges): you and your allies within eight blocks walk on the wind for 90 seconds. */
    public static void windWalk(ActCtx c) {
        mistCast(c, Wave6Tune.MIST_WIND, "Wind Walk", p -> {
            List<LivingEntity> party = new ArrayList<>();
            party.add(p);
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8), e -> e != p && e.isAlive() && Wave6Kit.isAlly(p, e))) {
                if (party.size() >= 8) break;
                party.add(e);
            }
            for (LivingEntity e : party) {
                Wave6Kit.give(e, "minecraft:slow_falling", 1800, 0, false);
                Wave6Kit.give(e, "minecraft:speed", 1800, 1, false);
                Wave6Kit.give(e, "minecraft:jump_boost", 1800, 1, false);
            }
            Fx.lendSkills(p, 1800, Wave6Skills.WIND_MANIPULATION, Wave6Skills.GRAVITY_FLIGHT);
            return true;
        });
    }

    // ================================================================ Neket's Practical Mage Robe
    public static void leftPocket(ActCtx c) { Wave6Pockets.open(c.player(), NEKET, "w6_pocket_l", 3, "Neket's Robe: left pocket"); }

    public static void rightPocket(ActCtx c) { Wave6Pockets.open(c.player(), NEKET, "w6_pocket_r", 3, "Neket's Robe: right pocket"); }

    /** Casts a stored Mage Armor on yourself (8 minutes, +4 armour). */
    public static void mageArmor(ActCtx c) {
        castMageArmor(c.player(), stack(c), true);
    }

    /** The robe's stored spell: true when one was cast. {@code asked} = the wearer asked for it (tell them why not), else it is the automatic one. */
    public static boolean castMageArmor(ServerPlayer p, ItemStack robe, boolean asked) {
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        if (d.getLong(K_MAGE_UNTIL) > now) { if (asked) Fx.say(p, "Mage Armor already shields you."); return false; }
        if (robe.isEmpty()) return false;
        Wave6Pool.State st = Wave6Kit.pool(robe, Wave6Pools.MAGE, p);
        if (!Wave6Pool.canSpend(st, 1)) { if (asked) Fx.say(p, "The robe holds no stored Mage Armor."); return false; }
        Wave6Kit.put(robe, Wave6Pools.MAGE, Wave6Pool.spend(st, 1));
        d.putLong(K_MAGE_UNTIL, now + Wave6Tune.MAGE_TICKS);
        Wave6Events.mageMods(p, true);
        Fx.lendSkill(p, Wave6Skills.MULTILAYER_BARRIER, 3600);                          // the spell's invisible mail, as Tensura's barrier skill for three minutes
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 1.3, MAGE_BLUE);
        Fx.say(p, "Mage Armor wraps you (" + (st.charges() - 1) + "/" + Wave6Pools.MAGE.max() + " stored).");
        return true;
    }

    /** Store a spell: spend mana now, keep a Mage Armor in the robe for later (up to five). */
    public static void storeSpell(ActCtx c) {
        ServerPlayer p = c.player();
        Wave6Pool.State st = Wave6Kit.pool(stack(c), Wave6Pools.MAGE, p);
        if (st.charges() >= Wave6Pools.MAGE.max()) { Fx.say(p, "The robe already holds five Mage Armors."); return; }
        Wave6Kit.put(stack(c), Wave6Pools.MAGE, Wave6Pool.add(st, 1, Wave6Pools.MAGE.max()));
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 1.0, MAGE_BLUE);
        Fx.say(p, "A Mage Armor is stored in the robe (" + (st.charges() + 1) + "/" + Wave6Pools.MAGE.max() + ").");
    }

    // ================================================================ Z'ar Sancuis
    /** Legendary Resistance, for an ally (one of the two daily uses): lifts every harmful effect from the nearest ally within 18 blocks that has any. */
    public static void zarAbsolve(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        double r = Wave6Tune.ZAR_ALLY_RANGE;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e != p && e.isAlive() && hasHarm(e) && allyOf(p, e))) {
            double dd = e.distanceToSqr(p);
            if (dd < bd && p.hasLineOfSight(e)) { bd = dd; best = e; }
        }
        if (best == null) { Fx.say(p, "No ally within sight is afflicted."); return; }
        Wave6Pool.State st = canPay(c, Wave6Pools.ZAR, 1, "Legendary Resistance");
        if (st == null) return;
        int n = 0;
        for (MobEffectInstance m : new ArrayList<>(best.getActiveEffects())) {
            if (m.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && n++ < 16) best.removeEffect(m.getEffect());
        }
        if (best instanceof ServerPlayer ally) Fx.lendSkill(ally, Wave6Skills.ABNORMAL_CONDITION_RESISTANCE, 600);    // and a minute of Tensura's resistance to the next one
        FxRelic.burst(p.serverLevel(), best.position().add(0, 1, 0), 0xC03030);
        int left = pay(c, Wave6Pools.ZAR, st, 1);
        left(p, "Your ally succeeds where they failed", left, Wave6Pools.ZAR);
    }

    private static boolean hasHarm(LivingEntity e) {
        for (MobEffectInstance m : e.getActiveEffects()) if (m.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return true;
        return false;
    }

    private static boolean allyOf(ServerPlayer p, LivingEntity e) {
        return e.isAlliedTo(p) || (e instanceof TamableAnimal t && t.isOwnedBy(p));
    }

    // ================================================================ Skitch's Smuggling Coat
    public static void deepPockets(ActCtx c) { Wave6Pockets.open(c.player(), COAT, "w6_pocket_deep", Wave6Tune.DEEP_ROWS, "Skitch's Coat: deep pockets"); }

    public static void seamPockets(ActCtx c) { Wave6Pockets.open(c.player(), COAT, "w6_pocket_seam", Wave6Tune.SEAM_ROWS, "Skitch's Coat: seam pockets"); }
}
