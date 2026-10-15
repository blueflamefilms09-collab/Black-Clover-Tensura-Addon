package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.djinn.Bonds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the six necklaces are the largest group of the wave and share the same wearer-side bookkeeping (a draw, a form timer, a charge pool, a hum
 * clock); putting them in one class keeps the per-player keys together and keeps the event handlers free of card logic.
 * <p>
 * Wave 28, the neck: the Chromatic Collar (a daily draw of one resistance), the Collar of the Penitent Dragon (a radiant breath), the Eye of the
 * Aboleth (charm), Fenrir's Collar (the hybrid form and the moon), the Sunheart Medallion (ten charges of sunlight) and the Witcher's Medallion (the
 * hum of danger).
 */
final class Wave28Neck {
    private Wave28Neck() {}

    private static final String ARMOR = "minecraft:generic.armor", ATK = "minecraft:generic.attack_damage", SPEED = "minecraft:generic.movement_speed",
            KB = "minecraft:generic.knockback_resistance", HP = "minecraft:generic.max_health";

    // ================================================================ Chromatic Collar
    private static final String C_ROLL = "chroma_roll", C_UNTIL = "chroma_until", C_WARD = "chroma_ward", C_CHOICE = "chroma_choice", C_MOD = "chroma_mod";
    private static final String[] WARD_NAMES = {"acid", "poison", "fire", "lightning", "cold"};

    /** Dragon's Resistance: the d6. Lasts until the next dawn's worth of time has gone (a day) or the next draw; the collar must stay on. */
    static void chromaDraw(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave28Util.data(p);
        int roll = Wave28Rules.chromaRoll(p.getRandom().nextInt(6));
        Wave28Rules.Ward w = Wave28Rules.wardOf(roll, d.contains(C_CHOICE) ? d.getInt(C_CHOICE) : 1);
        d.putInt(C_ROLL, roll);
        d.putInt(C_WARD, w.ordinal());
        d.putLong(C_UNTIL, p.level().getGameTime() + Wave28Rules.DAY);
        Wave28Util.dropMod(p, ARMOR, "chroma_armor");
        chromaPass(p);
        Wave28Util.say(p, "Dragon's Resistance: you rolled " + roll + ": " + WARD_NAMES[w.ordinal()] + " resistance"
                + (roll >= 6 ? " and +1 armour" : "") + " until the next dawn.", net.minecraft.ChatFormatting.AQUA);
        Wave28Util.burst(p.serverLevel(), p.position().add(0, 1.4, 0), RukhFx.Alignment.LIGHT, 8);
    }

    /** The resistance a 6 lets you choose: acid, poison, fire, lightning, cold in turn. */
    static void chromaChoose(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave28Util.data(p);
        int next = (d.contains(C_CHOICE) ? d.getInt(C_CHOICE) : 1) % 5 + 1;
        d.putInt(C_CHOICE, next);
        Wave28Util.tell(p, "Chromatic Collar: a roll of 6 will give " + WARD_NAMES[next - 1] + " resistance.");
    }

    /** Once a second while the collar is worn: the armour point of a 6, the poison and acid wards' cures and loans. */
    static void chromaPass(ServerPlayer p) {
        CompoundTag d = Wave28Util.peek(p);
        if (d == null) return;
        long now = p.level().getGameTime();
        if (!Wave28Rules.chromaActive(now, d.getLong(C_UNTIL))) {
            chromaCleanup(p);
            return;
        }
        if (Wave28Rules.chromaArmor(d.getInt(C_ROLL)) > 0) {
            Wave28Util.ensureMod(p, ARMOR, "chroma_armor", 1.0, Operation.ADD_VALUE);
            d.putBoolean(C_MOD, true);
        }
        Wave28Rules.Ward w = Wave28Rules.Ward.values()[Math.max(0, Math.min(4, d.getInt(C_WARD)))];
        if (w == Wave28Rules.Ward.POISON) {
            Wave28Util.remove(p, "minecraft:poison");
            Fx.lendSkill(p, "tensura:poison_resistance", 100);
        } else if (w == Wave28Rules.Ward.ACID) {
            Wave28Util.remove(p, "tensura:corrosion");
            Fx.lendSkill(p, "tensura:corrosion_resistance", 100);
        }
    }

    /** The collar is off or the day is over: the armour point goes. */
    static void chromaCleanup(ServerPlayer p) {
        CompoundTag d = Wave28Util.peek(p);
        if (d == null || !d.getBoolean(C_MOD)) return;
        Wave28Util.dropMod(p, ARMOR, "chroma_armor");
        d.remove(C_MOD);
    }

    /** The share of a blow that gets through the drawn resistance: 1.0 if none applies. */
    static float chromaKeep(ServerPlayer v, DamageSource src) {
        CompoundTag d = Wave28Util.peek(v);
        if (d == null || !Wave28Rules.chromaActive(v.level().getGameTime(), d.getLong(C_UNTIL))) return 1f;
        Wave28Rules.Ward w = Wave28Rules.Ward.values()[Math.max(0, Math.min(4, d.getInt(C_WARD)))];
        boolean hit = switch (w) {
            case FIRE -> src.is(DamageTypeTags.IS_FIRE);
            case LIGHTNING -> src.is(DamageTypeTags.IS_LIGHTNING);
            case COLD -> src.is(DamageTypeTags.IS_FREEZING);
            case POISON -> src.is(DamageTypes.MAGIC);
            case ACID -> src.is(DamageTypes.MOB_PROJECTILE) && src.getDirectEntity() != null && src.getDirectEntity().getType().toString().contains("spit");
        };
        return hit ? (float) (1.0 - Wave28Rules.CHROMA_REDUCTION) : 1f;
    }

    // ================================================================ Collar of the Penitent Dragon
    /** Radiant breath as a bonus action: a cone of light 6 blocks long; undead feel it more. */
    static void breath(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        for (int i = 1; i <= 6; i++) RukhFx.spray(l, RukhFx.Alignment.LIGHT, eye.x + look.x * i, eye.y - 0.2 + look.y * i, eye.z + look.z * i, 3, 0.25 + 0.08 * i, 0.25 + 0.08 * i, 0.25 + 0.08 * i, 0.02);
        for (LivingEntity t : Wave28Util.cone(p, Wave28Rules.BREATH_RANGE, Wave28Rules.BREATH_CONE)) {
            float dmg = Wave28Rules.BREATH_DAMAGE + (t.getType().is(EntityTypeTags.UNDEAD) ? Wave28Rules.BREATH_UNDEAD_BONUS : 0f);
            Fx.damage(p, t, dmg, "tensura:holy_damage");
        }
        l.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    /** A creature of dragon kind does not pick a penitent as its target (unless the penitent hurt it). */
    static boolean dragonRecognises(Mob m, LivingEntity target) {
        if (!(target instanceof ServerPlayer p) || m.getLastHurtByMob() == p) return false;
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType());
        return key != null && Wave28Rules.dragonKin(key.getPath(), m.getMaxHealth());
    }

    // ================================================================ Eye of the Aboleth
    static final String CHARM_OWNER = "w28_charm_owner", CHARM_UNTIL = "w28_charm_until";

    /** Charm: the creature you look at within 10 blocks, unless its existence is more than twice yours (the Wisdom save). A player is dazed, not commanded. */
    static void charm(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        Wave28Util.Aim a = Wave28Util.aim(p, Wave28Rules.CHARM_RANGE, e -> Wave28Util.target(p, e));
        Wave28Util.trail(l, p.getEyePosition().add(0, -0.1, 0), a.end(), RukhFx.Alignment.DARK);
        LivingEntity t = a.hit();
        if (t == null) {
            Wave28Util.tell(p, "The eye finds no creature within ten blocks.", net.minecraft.ChatFormatting.GRAY);
            return;
        }
        if (!Wave28Rules.charmable(t.getMaxHealth()) || Wave28Util.saves(p, t)) {
            Wave28Util.tell(p, t.getName().getString() + " throws off the gaze.", net.minecraft.ChatFormatting.GRAY);
            return;
        }
        Wave28Util.burst(l, t.position().add(0, t.getBbHeight() * 0.6, 0), RukhFx.Alignment.DARK, 8);
        if (t instanceof Player) {
            Wave28Util.give(t, Wave28Rules.PLAYER_CHARM_TICKS, 1, "minecraft:weakness");
            Wave28Util.give(t, Wave28Rules.PLAYER_CHARM_TICKS, 0, "minecraft:slowness");
        } else if (t instanceof Mob m) {
            m.getPersistentData().putUUID(CHARM_OWNER, p.getUUID());
            m.getPersistentData().putLong(CHARM_UNTIL, l.getGameTime() + Wave28Rules.CHARM_TICKS);
            LivingEntity other = null;
            for (LivingEntity e : Wave28Util.near(l, m.position(), 12.0, e -> e != m && e != p && e instanceof Enemy && !e.getPersistentData().contains(CHARM_OWNER))) {
                other = e;
                break;
            }
            m.setTarget(other);
        }
        Wave28Util.tell(p, t.getName().getString() + " is charmed (one minute).");
    }

    /** A charmed creature does not pick its charmer as a target while the charm holds. */
    static boolean charmedAgainst(Mob m, LivingEntity target) {
        CompoundTag d = m.getPersistentData();
        return target != null && d.hasUUID(CHARM_OWNER) && d.getUUID(CHARM_OWNER).equals(target.getUUID()) && m.level().getGameTime() < d.getLong(CHARM_UNTIL);
    }

    // ================================================================ Fenrir's Collar
    private static final String F_UNTIL = "fen_until", F_CTRL = "fen_ctrl", F_VIGIL = "fen_vigil";

    private static boolean inForm(ServerPlayer p, CompoundTag d) { return d != null && p.level().getGameTime() < d.getLong(F_UNTIL); }

    private static boolean controlled(ServerPlayer p) {
        CompoundTag d = Wave28Util.peek(p);
        long vigil = d != null && d.contains(F_VIGIL) ? d.getLong(F_VIGIL) : -1L;
        ServerLevel l = p.serverLevel();
        return Wave28Rules.controlled(l.getMoonPhase(), l.isNight(), vigil, Wave28Rules.dayOf(l.getDayTime()));
    }

    private static void formMods(ServerPlayer p) {
        Wave28Util.ensureMod(p, ATK, "fen_atk", 4.0, Operation.ADD_VALUE);
        Wave28Util.ensureMod(p, SPEED, "fen_speed", 0.2, Operation.ADD_MULTIPLIED_BASE);
        Wave28Util.ensureMod(p, KB, "fen_kb", 0.3, Operation.ADD_VALUE);
        Wave28Util.ensureMod(p, HP, "fen_hp", 6.0, Operation.ADD_VALUE);
    }

    private static void dropFormMods(ServerPlayer p) {
        Wave28Util.dropMod(p, ATK, "fen_atk");
        Wave28Util.dropMod(p, SPEED, "fen_speed");
        Wave28Util.dropMod(p, KB, "fen_kb");
        Wave28Util.dropMod(p, HP, "fen_hp");
    }

    /** Metamorphose into the cursed hybrid form: five minutes if you hold the wolf (full moon night, or the vigil of that moon), 90 seconds if you do not. */
    static void metamorphose(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave28Util.data(p);
        if (inForm(p, d)) {
            Wave28Util.tell(p, "You are already the wolf.", net.minecraft.ChatFormatting.GRAY);
            return;
        }
        boolean ctrl = controlled(p);
        int ticks = Wave28Rules.formTicks(ctrl);
        d.putLong(F_UNTIL, p.level().getGameTime() + ticks);
        d.putBoolean(F_CTRL, ctrl);
        formMods(p);
        Fx.lendSkills(p, ticks, "tensura:godwolf_sense", "tensura:strengthen_body");
        Wave28Util.give(p, ticks, 0, "minecraft:night_vision");
        Wave28Util.burst(p.serverLevel(), p.position().add(0, 1.0, 0), RukhFx.Alignment.DARK, 12);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.0f, 0.8f);
        Wave28Util.say(p, ctrl ? "The moon holds the wolf: you keep your mind for " + ticks / 1200 + " minutes."
                : "The wolf is not yours tonight: the form will slip in " + ticks / 20 + " seconds and the curse will take its price.", net.minecraft.ChatFormatting.DARK_RED);
    }

    /** Shed the form at will (no price if it was held by the moon; the price of the curse if it was not and the time has not run out is waived). */
    static void shed(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave28Util.peek(p);
        if (!inForm(p, d)) {
            Wave28Util.tell(p, "You are not in the hybrid form.", net.minecraft.ChatFormatting.GRAY);
            return;
        }
        endForm(p, false);
    }

    private static void endForm(ServerPlayer p, boolean price) {
        CompoundTag d = Wave28Util.data(p);
        dropFormMods(p);
        d.putLong(F_UNTIL, 0L);
        if (price) {
            Wave28Util.give(p, Wave28Rules.FORM_PRICE_TICKS, 0, "minecraft:weakness");
            Wave28Util.give(p, 200, 0, "minecraft:hunger");
            Wave28Util.say(p, "The wolf lets go and takes its price: you are weak and hungry.", net.minecraft.ChatFormatting.DARK_RED);
        } else {
            Wave28Util.tell(p, "You are yourself again.");
        }
        Wave28Util.burst(p.serverLevel(), p.position().add(0, 1.0, 0), RukhFx.Alignment.DARK, 6);
    }

    /** Moon vigil: a night under the open sky at the full moon. The collar then holds the wolf for you until the next full moon. */
    static void vigil(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        if (!(l.isNight() && l.getMoonPhase() == 0 && l.canSeeSky(p.blockPosition()))) {
            Wave28Util.tell(p, "The vigil needs the full moon, at night, under the open sky.", net.minecraft.ChatFormatting.GRAY);
            return;
        }
        Wave28Util.data(p).putLong(F_VIGIL, Wave28Rules.cycleOf(Wave28Rules.dayOf(l.getDayTime())));
        Fx.heal(p, 6f);
        Wave28Util.give(p, 200, 0, "minecraft:regeneration");
        Wave28Util.say(p, "You keep the vigil. Until the next full moon you hold the wolf.", net.minecraft.ChatFormatting.AQUA);
        Wave28Util.burst(l, p.position().add(0, 1.6, 0), RukhFx.Alignment.LIGHT, 10);
    }

    /** Once a second: keeps the form's modifiers and night eyes while it lasts and the collar is worn; ends it (with the price if the moon did not hold it). */
    static void fenrirPass(ServerPlayer p, boolean worn) {
        CompoundTag d = Wave28Util.peek(p);
        if (d == null || !d.contains(F_UNTIL) || d.getLong(F_UNTIL) <= 0L) return;
        if (inForm(p, d)) {
            if (!worn) {
                endForm(p, false);
                return;
            }
            formMods(p);
            Wave28Util.giveQuiet(p, 60, 0, "minecraft:night_vision");
        } else {
            endForm(p, !d.getBoolean(F_CTRL) && worn);
        }
    }

    // ================================================================ Sunheart Medallion
    private static final String S_LEFT = "sun_left", S_DAY = "sun_day", S_DAWN = "sun_dawn", S_BURN = "sun_burn_at", WARD = "w28_ward";

    private static Wave28Rules.Charges sun(ServerPlayer p) {
        CompoundTag d = Wave28Util.data(p);
        Wave28Rules.Charges c = new Wave28Rules.Charges(Wave28Rules.SUN_MAX, d.contains(S_LEFT) ? d.getInt(S_LEFT) : Wave28Rules.SUN_MAX, d.getLong(S_DAY));
        return c.at(Wave28Rules.dayOf(p.level().getGameTime()), 0);
    }

    private static void saveSun(ServerPlayer p, Wave28Rules.Charges c) {
        CompoundTag d = Wave28Util.data(p);
        d.putInt(S_LEFT, c.left());
        d.putLong(S_DAY, c.day());
    }

    private static boolean spendSun(ServerPlayer p, int cost) {
        Wave28Rules.Charges c = sun(p);
        if (!c.canSpend(cost)) {
            Wave28Util.tell(p, "The medallion holds " + c.left() + " of " + Wave28Rules.SUN_MAX + " charges (this needs " + cost + "). Greet the dawn to fill it.", net.minecraft.ChatFormatting.RED);
            return false;
        }
        saveSun(p, c.spend(cost));
        Wave28Util.tell(p, "Sunheart: " + (c.left() - cost) + " charges left.");
        return true;
    }

    /** On command: Sacred Flame. A mote of radiant fire at the foe you look at (14 blocks): 6 damage, a little more against the undead. */
    static void sacredFlame(ActCtx c) {
        ServerPlayer p = c.player();
        Wave28Util.Aim a = Wave28Util.aim(p, 14.0, e -> Wave28Util.foe(p, e));
        Wave28Util.trail(p.serverLevel(), p.getEyePosition().add(0, -0.3, 0), a.end(), RukhFx.Alignment.LIGHT);
        if (a.hit() == null) return;
        float dmg = Wave28Rules.SACRED_FLAME + (a.hit().getType().is(EntityTypeTags.UNDEAD) ? Wave28Rules.SACRED_UNDEAD_BONUS : 0f);
        Fx.damage(p, a.hit(), dmg, "tensura:holy_damage");
        a.hit().setRemainingFireTicks(Math.max(a.hit().getRemainingFireTicks(), 40));
        Wave28Util.burst(p.serverLevel(), a.end(), RukhFx.Alignment.LIGHT, 8);
    }

    /** Bathed in dawn's first light (open sky, once a day): the medallion takes 1d8+3 charges back, up to ten. */
    static void greetDawn(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        CompoundTag d = Wave28Util.data(p);
        long today = Wave28Rules.dayOf(l.getGameTime());
        if (!Wave28Rules.inDawn(l.getDayTime()) || !l.canSeeSky(p.blockPosition())) {
            Wave28Util.tell(p, "The medallion only drinks dawn's first light, under the open sky.", net.minecraft.ChatFormatting.GRAY);
            return;
        }
        if (!Wave28Rules.canGreetDawn(d.getLong(S_DAWN) - 1, today)) {
            Wave28Util.tell(p, "It has already drunk this dawn.", net.minecraft.ChatFormatting.GRAY);
            return;
        }
        Wave28Rules.Charges now = sun(p);
        int gain = Wave28Rules.dawnRegain(p.getRandom().nextInt(8));
        saveSun(p, now.add(gain));
        d.putLong(S_DAWN, today + 1);
        Wave28Util.say(p, "The medallion drinks the dawn: +" + gain + " charges (" + Math.min(Wave28Rules.SUN_MAX, now.left() + gain) + " of " + Wave28Rules.SUN_MAX + ").", net.minecraft.ChatFormatting.GOLD);
        Wave28Util.burst(l, p.position().add(0, 1.5, 0), RukhFx.Alignment.LIGHT, 12);
    }

    private static List<LivingEntity> friends(ServerPlayer p, double r) { return Wave28Util.near(p.serverLevel(), p.position(), r, e -> Wave28Util.friend(p, e)); }

    private static void cleanse(LivingEntity e) {
        List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> bad = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(i.getEffect());
        for (var h : bad) e.removeEffect(h);
        if (e instanceof ServerPlayer sp) Wave28Potions.reattach(sp);
    }

    private static void healFriend(ServerPlayer p, LivingEntity e, float amount) {
        if (e instanceof ServerPlayer sp) Fx.heal(sp, amount);
        else e.heal(amount);
        Wave28Util.burst(p.serverLevel(), e.position().add(0, e.getBbHeight() * 0.6, 0), RukhFx.Alignment.LIGHT, 5);
    }

    /** Aura of Vitality (1 charge): you and friends within 10 blocks get Regeneration II for 20 seconds. */
    static void vitality(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendSun(p, Wave28Rules.COST_VITALITY)) return;
        for (LivingEntity e : friends(p, 10.0)) Wave28Util.give(e, 400, 1, "minecraft:regeneration");
        Fx.ring(p.serverLevel(), p.position(), 5.0, 0xFFD060);
    }

    /** Fire Shield (2 charges): five minutes of fire resistance and a shield of absorption. */
    static void fireShield(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendSun(p, Wave28Rules.COST_SHIELD)) return;
        Wave28Util.give(p, 6000, 0, "minecraft:fire_resistance");
        Fx.shield(p, 8f, 6000);
        Fx.ring(p.serverLevel(), p.position(), 1.8, 0xFF9A30);
    }

    /** Mass Cure Wounds (3 charges): you and friends within 8 blocks heal 10 hit points. */
    static void massCure(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendSun(p, Wave28Rules.COST_MASS_CURE)) return;
        for (LivingEntity e : friends(p, 8.0)) healFriend(p, e, 10f);
        Fx.ring(p.serverLevel(), p.position(), 8.0, 0xFFE080);
    }

    /** Greater Restoration (5 charges): every harmful effect leaves you and friends within 6 blocks. */
    static void restoration(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendSun(p, Wave28Rules.COST_RESTORATION)) return;
        for (LivingEntity e : friends(p, 6.0)) cleanse(e);
        Fx.ring(p.serverLevel(), p.position(), 6.0, 0xFFF0B0);
    }

    private static LivingEntity healTarget(ServerPlayer p) {
        Wave28Util.Aim a = Wave28Util.aim(p, 6.0, e -> Wave28Util.friend(p, e) && e != p);
        return a.hit() != null ? a.hit() : p;
    }

    /** Heal (6 charges): 20 hit points to you or the friend you look at (6 blocks), and every harmful effect off them. */
    static void heal(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendSun(p, Wave28Rules.COST_HEAL)) return;
        LivingEntity t = healTarget(p);
        healFriend(p, t, 20f);
        cleanse(t);
    }

    /** Regenerate (7 charges): 10 hit points now and Regeneration III for a minute on you or the friend you look at; wither and other rot leave them. */
    static void regenerate(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendSun(p, Wave28Rules.COST_REGENERATE)) return;
        LivingEntity t = healTarget(p);
        healFriend(p, t, 10f);
        Wave28Util.give(t, 1200, 2, "minecraft:regeneration");
        cleanse(t);
    }

    /**
     * True Resurrection (once only): the amulet shatters into sunlight. You and friends within 16 blocks are healed to full and carry a ward for ten
     * minutes: the next blow that would kill each of them leaves them on half their health instead (once). The medallion is lost for ever.
     */
    static void resurrection(ActCtx c) {
        ServerPlayer p = c.player();
        long until = p.level().getGameTime() + Wave28Rules.SUN_WARD_TICKS;
        for (LivingEntity e : friends(p, 16.0)) {
            e.getPersistentData().putLong(WARD, until);
            e.heal(e.getMaxHealth());
            Wave28Util.burst(p.serverLevel(), e.position().add(0, e.getBbHeight() * 0.6, 0), RukhFx.Alignment.LIGHT, 12);
            if (e instanceof ServerPlayer sp) Wave28Potions.reattach(sp);
        }
        Fx.ring(p.serverLevel(), p.position(), 16.0, 0xFFE9A0);
        Wave28Util.say(p, "The Sunheart Medallion shatters into brilliant sunlight, lost for ever. Ten minutes of its ward remain on you and your friends.", net.minecraft.ChatFormatting.GOLD);
        c.stack().shrink(1);
    }

    /** Is a killing blow about to land on someone who carries the resurrection ward? */
    static boolean warded(LivingEntity v, float health, float damage) {
        return damage >= health && v.level().getGameTime() < v.getPersistentData().getLong(WARD);
    }

    /** The ward is spent: it is gone, and the bearer is brought up to half health (the blow itself was set to nothing by the caller). */
    static void spendWard(LivingEntity v) {
        v.getPersistentData().remove(WARD);
        if (v.getHealth() < v.getMaxHealth() * 0.5f) v.setHealth(v.getMaxHealth() * 0.5f);
        if (v.level() instanceof ServerLevel l) Wave28Util.burst(l, v.position().add(0, v.getBbHeight() * 0.6, 0), RukhFx.Alignment.LIGHT, 12);
    }

    /** Once a second while worn: disease cannot take hold (Tensura infection and the sickness of rotten food); an evil (devil-bound) wearer burns a little each round. */
    static void sunPass(ServerPlayer p) {
        Wave28Util.remove(p, "tensura:infection");
        if (!Bonds.holds(p, Bonds.Kind.DEVIL)) return;
        CompoundTag d = Wave28Util.data(p);
        long now = p.level().getGameTime();
        if (now < d.getLong(S_BURN)) return;
        float burn = Wave28Rules.evilBurn(p.getHealth());
        if (burn <= 0f) return;
        d.putLong(S_BURN, now + Wave28Rules.EVIL_BURN_GAP);
        p.hurt(p.damageSources().onFire(), burn);
        Wave28Util.burst(p.serverLevel(), p.position().add(0, 1.2, 0), RukhFx.Alignment.DARK, 6);
        Wave28Util.tell(p, "The medallion burns the evil in you.", net.minecraft.ChatFormatting.RED);
    }

    // ================================================================ Witcher's Medallion
    private static final String W_AT = "wit_at", W_N = "wit_n", W_STORM = "wit_storm";

    /** Once a second while worn: the medallion hums for monsters within 20 blocks (more often as they grow in number), for a great presence and for a storm. */
    static void witcherPass(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        double r = Wave28Rules.WITCHER_RANGE;
        Vec3 c = p.position();
        List<Mob> mobs = l.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r), m -> m instanceof Enemy && m.isAlive() && m.distanceToSqr(c) <= r * r);
        CompoundTag d = Wave28Util.data(p);
        long now = l.getGameTime();
        int n = mobs.size();
        int before = d.getInt(W_N);
        d.putInt(W_N, n);
        if (n > 0) {
            Mob nearest = mobs.get(0);
            boolean great = false;
            for (Mob m : mobs) {
                if (m.distanceToSqr(c) < nearest.distanceToSqr(c)) nearest = m;
                if (m.getMaxHealth() >= 150f) great = true;
            }
            if (n > before || now - d.getLong(W_AT) >= Wave28Rules.WITCHER_HUM_GAP) {
                d.putLong(W_AT, now);
                String where = Wave28Rules.compass(nearest.getX() - p.getX(), nearest.getZ() - p.getZ(), p.getYRot());
                int dist = (int) Math.round(Math.sqrt(nearest.distanceToSqr(c)));
                Wave28Util.tell(p, "The medallion hums" + (great ? " hard: a great presence" : "") + ": " + n + (n == 1 ? " monster" : " monsters")
                        + ", the nearest about " + dist + " blocks " + where + ".", great ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.YELLOW);
                l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.35f + 0.15f * Wave28Rules.humLevel(n), 1.2f);
            }
        }
        if (l.isThundering() && l.canSeeSky(p.blockPosition()) && now - d.getLong(W_STORM) >= 600) {
            d.putLong(W_STORM, now);
            Wave28Util.tell(p, "The medallion trembles: lightning in the air.", net.minecraft.ChatFormatting.AQUA);
        }
    }

    /** Witcher senses: for fifteen seconds the monsters within 20 blocks glow for you, and you see in the dark. */
    static void senses(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        Wave28Util.give(p, Wave28Rules.SENSE_TICKS, 0, "minecraft:night_vision");
        Fx.lendSkill(p, "tensura:magic_sense", Wave28Rules.SENSE_TICKS);
        int n = 0;
        for (LivingEntity e : Wave28Util.near(l, p.position(), Wave28Rules.WITCHER_RANGE, e -> e instanceof Enemy)) {
            Wave28Util.give(e, Wave28Rules.SENSE_TICKS, 0, "minecraft:glowing");
            n++;
        }
        Wave28Util.burst(l, p.position().add(0, 1.6, 0), RukhFx.Alignment.DARK, 6);
        Wave28Util.tell(p, "Witcher senses: " + n + (n == 1 ? " monster" : " monsters") + " lit for fifteen seconds.");
    }
}
