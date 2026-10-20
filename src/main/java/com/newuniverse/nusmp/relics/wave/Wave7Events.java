package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 7 gameplay: the armours' abilities (act bodies, the staggered player tick, the damage / death / visibility / effect hooks).
 * Everything keys off the per-armour flags of {@link Wave7}; timers live in the player's persistent data as absolute game
 * times, so nothing here keeps a static list of players or levels. Rules live in {@link Wave7Math}.
 */
public final class Wave7Events {
    private Wave7Events() {}

    // persistent-data keys (absolute game-time stamps end in _until)
    private static final String K_PX = "nusmp_w7_px", K_PY = "nusmp_w7_py", K_PZ = "nusmp_w7_pz", K_STILL = "nusmp_w7_still", K_TRANCE = "nusmp_w7_trance";
    private static final String K_FOOD = "nusmp_w7_food", K_SAT = "nusmp_w7_sat", K_EX = "nusmp_w7_ex", K_FOOD_T = "nusmp_w7_food_t";
    private static final String K_STAR = "nusmp_w7_star_until", K_STAR_ON = "nusmp_w7_star_on";
    private static final String K_GUARD = "nusmp_w7_guard_until", K_GOLD = "nusmp_w7_gold_until";
    private static final String K_PROT = "nusmp_w7_prot_until", K_EXPO = "nusmp_w7_expo_until";
    private static final String K_HEAD = "nusmp_w7_head_until", K_WINGS = "nusmp_w7_wings_until", K_WINGS_SET = "nusmp_w7_wings_set";
    private static final String K_SOUL = "nusmp_w7_soul", K_SOUL_UNTIL = "nusmp_w7_soul_until", K_SOUL_AWAKE = "nusmp_w7_soul_awake";
    private static final String K_ARCANE = "nusmp_w7_arcane", K_VIS = "nusmp_w7_vis", K_VIS_S = "nusmp_w7_vis_s";
    private static final String K_DISPEL = "nusmp_w7_dispel_until", K_ROGUE = "nusmp_w7_rogue", K_INTB = "nusmp_w7_intb", K_STR = "nusmp_w7_str", K_DRAG = "nusmp_w7_drag";

    private static final int STAR_COLOR = 0x9EC8FF, GOLD_COLOR = 0xD4A537, FLAME_COLOR = 0xFF8A2A, LION_COLOR = 0xD9622A, SOUL_COLOR = 0x3FB4FF;
    private static final int MAX_TARGETS = 12;

    /** Guards the starscape / parry counter-blows against hitting each other forever. */
    private static boolean busy;

    // ================================================================ small helpers
    private static boolean has(LivingEntity e, String flag) { return RelicFlags.has(e, flag); }

    private static boolean on(CompoundTag d, String key, long now) { return d.getLong(key) > now; }

    /** True once per `period` ticks for this player (staggered by entity id). */
    private static boolean due(ServerPlayer p, int period) { return (p.tickCount + p.getId()) % period == 0; }

    private static void tell(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text), true); }

    private static String damagePath(DamageSource s) {
        return s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    private static String entityPath(EntityType<?> t) {
        ResourceLocation k = BuiltInRegistries.ENTITY_TYPE.getKey(t);
        return k == null ? "" : k.getPath();
    }

    /** A blow struck by a living creature in melee (not a shot, not a spell). */
    private static boolean isMelee(DamageSource s) {
        Entity direct = s.getDirectEntity();
        return direct instanceof LivingEntity && direct == s.getEntity() && !s.is(DamageTypeTags.IS_PROJECTILE)
                && !Wave7Math.isForceLike(damagePath(s));
    }

    private static boolean isAlly(ServerPlayer p, LivingEntity x) {
        if (x == p || !x.isAlive() || x.isSpectator()) return false;
        if (x instanceof TamableAnimal t && t.isOwnedBy(p)) return true;
        if (x instanceof Player o) return o.isAlliedTo(p) || (p.getTeam() == null && o.getTeam() == null);
        return x.isAlliedTo(p);
    }

    /** Allies (pets, team mates, other players without a team) around the wearer; capped. */
    private static List<LivingEntity> allies(ServerPlayer p, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> isAlly(p, x) && x.distanceToSqr(p) <= r * r)) {
            out.add(e);
            if (out.size() >= MAX_TARGETS) break;
        }
        return out;
    }

    /** The nearest of Fx.foesAround (it returns them nearest first), or null. */
    private static LivingEntity nearestFoe(ServerPlayer p, double r) {
        List<LivingEntity> foes = Fx.foesAround(p, r);
        return foes.isEmpty() ? null : foes.get(0);
    }

    // Tensura stats (0 when Tensura's storages are absent)
    private static double maxEp(LivingEntity e) { try { return EnergyHelper.getMaxEP(e); } catch (Throwable none) { return 0; } }
    private static double maxMagicule(LivingEntity e) { try { return EnergyHelper.getMaxMagicule(e); } catch (Throwable none) { return 0; } }
    private static double maxAura(LivingEntity e) { try { return EnergyHelper.getMaxAura(e); } catch (Throwable none) { return 0; } }

    /** The wearer's Rogue training (maximum aura against the gate), as the last pass stored it. */
    private static double rogue(CompoundTag d) { return d.contains(K_ROGUE) ? d.getDouble(K_ROGUE) : 1.0; }

    /** A status that is kept alive by the tick: ambient, no particles, but its icon shows. */
    private static void quiet(LivingEntity e, String id, int ticks, int amp) {
        BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).ifPresent(h -> e.addEffect(new MobEffectInstance(h, ticks, amp, true, false, true)));
    }

    // ================================================================ act bodies
    /** Starstudded Leathers: Emanate Starscape. */
    static void starscape(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        CompoundTag d = p.getPersistentData();
        d.putLong(K_STAR, l.getGameTime() + Wave7Math.STAR_TICKS);
        d.putBoolean(K_STAR_ON, true);
        double r = Wave7Math.starRadius(l.getDayTime());
        FxRelic.burst(l, p.position().add(0, 1, 0), STAR_COLOR);
        FxRelic.ring(l, p.position().add(0, 0.1, 0), r, STAR_COLOR);
        Fx.lendSkills(p, Wave7Math.STAR_TICKS, Wave7Skills.MAGIC_SENSE, Wave7Skills.THOUGHT_COMMUNICATION);   // the stars see, and carry a message to anyone you know
        Fx.say(p, "A starscape unfolds around you (" + (int) r + " blocks, 1 minute): Magic Sense and Thought Communication are lent while it lasts.");
    }

    /** Starstudded Leathers: Star Bolt (only inside a live starscape). */
    static void starBolt(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        if (!on(p.getPersistentData(), K_STAR, l.getGameTime())) { Fx.say(p, "The stars are not out: emanate a starscape first."); return; }
        LivingEntity t = nearestFoe(p, Wave7Math.starRadius(l.getDayTime()) + 3);
        if (t == null) { Fx.say(p, "No foe in the starscape."); return; }
        Fx.damage(p, t, Wave7Math.STAR_BOLT, "minecraft:magic");
        Fx.addEffect(t, "minecraft:glowing", 100, 0);
        FxRelic.burst(l, t.position().add(0, t.getBbHeight() * 0.5, 0), STAR_COLOR);
    }

    /** Coat of the Duelist: En Garde. */
    static void enGarde(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        p.getPersistentData().putLong(K_GUARD, p.level().getGameTime() + Wave7Math.GUARD_TICKS);
        Fx.lendSkills(p, Wave7Math.GUARD_TICKS + 20, Wave7Skills.DANGER_SENSE, Wave7Skills.THOUGHT_ACCELERATION);   // the fencer reads the blow before it comes
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 1.6, 0xB8BCC8);
        Fx.say(p, "En garde: the next melee blow within 6 seconds is parried (Danger Sense and Thought Acceleration are lent).");
    }

    /** Coat of the Duelist: Lunge. */
    static void lunge(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Vec3 look = p.getLookAngle(), eye = p.getEyePosition();
        LivingEntity hit = null;
        for (LivingEntity t : Fx.foesAround(p, 7.0)) {                       // nearest first: the first one in front of you
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
            double dist = to.length();
            if (dist > 0.1 && to.scale(1.0 / dist).dot(look) >= 0.75) { hit = t; break; }
        }
        Fx.dash(p, 6.0);
        if (hit != null) {
            Fx.damage(p, hit, 5f, "minecraft:player_attack");
            Fx.addEffect(p, "minecraft:speed", 60, 1);
        }
    }

    /** Suit of the Golden Shadow: Unseen. */
    static void unseen(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        double training = Wave7Math.training(maxAura(p), Wave7Math.GATE_ROGUE_AURA);
        int ticks = Wave7Math.unseenTicks(training);
        Fx.addEffect(p, "minecraft:invisibility", ticks, 0);
        quiet(p, "tensura:presence_concealment", ticks, 0);                              // Unseen merges invisibility, Presence Concealment and Shadow Motion
        Fx.lendSkill(p, Wave7Skills.SHADOW_MOTION, ticks);
        p.getPersistentData().putLong(K_GOLD, l.getGameTime() + ticks);
        FxRelic.burst(l, p.position().add(0, 1, 0), GOLD_COLOR);
        Fx.say(p, "You slip into the shadow for " + ticks / 20 + " seconds (a Rogue's training: " + Math.round(training * 100) + "%; attacking ends it with a sneak attack).");
    }

    /** Armor of the Lionheart: Bolstering Roar. */
    static void roar(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        List<LivingEntity> group = allies(p, Wave7Math.PRIDE_RADIUS);
        group.add(p);
        for (LivingEntity e : group) {
            Fx.addEffect(e, "minecraft:strength", Wave7Math.ROAR_TICKS, 0);
            Fx.addEffect(e, "minecraft:resistance", Wave7Math.ROAR_TICKS, 0);
            Fx.addEffect(e, "minecraft:speed", Wave7Math.ROAR_TICKS, 0);
            if (e instanceof ServerPlayer sp) Fx.lendSkill(sp, Wave7Skills.ABNORMAL_RESIST, Wave7Math.ROAR_TICKS);   // the "advantage on saving throws"
        }
        Fx.lendSkill(p, Wave7Skills.HERO_HAKI, Wave7Math.ROAR_TICKS);                       // the roar itself is a hero's haki
        l.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 0.8f);
        FxRelic.ring(l, p.position().add(0, 0.1, 0), Wave7Math.PRIDE_RADIUS, LION_COLOR);
        Fx.say(p, "A lion's roar: " + (group.size() - 1) + " allies are bolstered for 30 seconds.");
    }

    /** Armor of the Lionheart: Lion's Resolve (your guard moves to the ally who needs it most). */
    static void resolve(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        long now = p.level().getGameTime();
        LivingEntity pick = null;
        float best = 2f;
        for (LivingEntity a : allies(p, Wave7Math.PRIDE_RADIUS)) {
            float frac = a.getHealth() / Math.max(1f, a.getMaxHealth());
            if (frac < best) { best = frac; pick = a; }
        }
        if (pick == null) { Fx.say(p, "No ally within reach."); return; }
        pick.getPersistentData().putLong(K_PROT, now + Wave7Math.RESOLVE_TICKS);
        p.getPersistentData().putLong(K_EXPO, now + Wave7Math.RESOLVE_TICKS);
        FxRelic.ring(p.serverLevel(), pick.position().add(0, 0.1, 0), 1.4, LION_COLOR);
        if (pick instanceof ServerPlayer sp) tell(sp, p.getName().getString() + " lends you their guard.");
        Fx.say(p, "Your guard passes to " + pick.getName().getString() + " for 5 seconds; you are exposed.");
    }

    /** Armor of the Beheaded: Detach Head (again to put it back). */
    static void detachHead(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        long now = p.level().getGameTime();
        CompoundTag d = p.getPersistentData();
        if (on(d, K_HEAD, now)) { endHead(p, d); return; }
        d.putLong(K_HEAD, now + Wave7Math.HEAD_TICKS);
        FxRelic.ring(p.serverLevel(), p.position().add(0, 1.6, 0), 1.0, 0x9FD8FF);
        Fx.say(p, "You lift your head off (45 seconds): magical senses (night sight, Magic Sense), no drowning, no blinding; you cannot eat or drink.");
    }

    /** Armor of the Beheaded: Dispel Magic (the card's counter to the magical senses, turned into the wearer's own art). */
    static void dispelMagic(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        long now = l.getGameTime();
        CompoundTag d = p.getPersistentData();
        int cleansed = 0;
        for (MobEffectInstance m : new ArrayList<>(p.getActiveEffects())) {                // what is laid on you washes off
            if (m.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) { p.removeEffect(m.getEffect()); cleansed++; }
        }
        int foes = 0;
        for (LivingEntity t : Fx.foesAround(p, Wave7Math.DISPEL_RADIUS)) {                 // what is laid on them is torn off, and their magic jams
            if (foes++ >= Wave7Math.DISPEL_FOES) break;
            for (MobEffectInstance m : new ArrayList<>(t.getActiveEffects())) {
                if (m.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) t.removeEffect(m.getEffect());
            }
            Nullification.jamAll(t, BalanceLaw.isBoss(t) ? Wave7Math.JAM_SECONDS_BOSS : Wave7Math.JAM_SECONDS, Wave7Math.JAM_MISFIRE);
        }
        int broken = Nullification.shatterBarriers(l, p.position(), Wave7Math.DISPEL_RADIUS, p);
        Fx.lendSkill(p, Wave7Skills.CHANT_ANNULMENT, 200);
        // the card: Dispel Magic disables the magical senses of the headless for a minute: yours, and any other such armour in the blast
        long dark = now + Wave7Math.DISPEL_SENSES_TICKS;
        if (on(d, K_HEAD, now)) { d.putLong(K_DISPEL, dark); p.removeEffect(MobEffects.NIGHT_VISION); }
        for (ServerPlayer o : l.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(Wave7Math.DISPEL_RADIUS), x -> x != p && x.distanceToSqr(p) <= Wave7Math.DISPEL_RADIUS * Wave7Math.DISPEL_RADIUS)) {
            if (has(o, Wave7.F_HEAD)) o.getPersistentData().putLong(K_DISPEL, dark);
        }
        FxRelic.ring(l, p.position().add(0, 0.1, 0), Wave7Math.DISPEL_RADIUS, 0x9FD8FF);
        Fx.say(p, "Dispel Magic: " + cleansed + " afflictions washed off you, " + foes + " foes stripped and jammed, " + broken + " magic constructs broken."
                + (on(d, K_DISPEL, now) ? " Your own magical senses go dark for a minute." : ""));
    }

    /** False Dragon Armor: Fiery Wings. */
    static void fieryWings(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        CompoundTag d = p.getPersistentData();
        long now = l.getGameTime();
        if (on(d, K_WINGS, now)) { Fx.say(p, "Your wings are already burning."); return; }
        d.putLong(K_WINGS, now + Wave7Math.WINGS_TICKS);
        grantFlight(p, d);
        if (!p.isSpectator()) {                                        // take off at once
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, Math.max(v.y, 0.45), v.z);
            p.hurtMarked = true;
        }
        FxRelic.burst(l, p.position().add(0, 1.2, 0), FLAME_COLOR);
        Fx.say(p, "Fiery wings unfurl: you fly for 2 minutes (double-tap jump to land or take off again).");
    }

    /** Arcane Conduction Armor: Reconfigure (cycles the four modes). */
    static void reconfigure(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        CompoundTag d = p.getPersistentData();
        double pool = maxMagicule(p);
        int mode = Wave7Math.nextMode(d.getInt(K_ARCANE));
        boolean refused = !Wave7Math.modeAllowed(mode, Wave7Math.isCaster(pool));
        if (refused) mode = Wave7Math.MODE_BASE;                                           // only a spellcaster's pool can drive the other modes
        d.putInt(K_ARCANE, mode);
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 1.2, 0x58A8FF);
        Fx.say(p, "Arcane Conduction Armor: " + Wave7Math.modeName(mode) + "."
                + (refused ? " The circuits stay dark: the other modes need a spellcaster's pool of " + (long) Wave7Math.GATE_CASTER_MAGICULE + " magicules." : ""));
    }

    /** Delphyne's Cursed Armor: Tail Sweep. */
    static void tailSweep(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        for (LivingEntity t : Fx.foesAround(p, Wave7Math.TAIL_RADIUS)) {
            Fx.damage(p, t, Wave7Math.TAIL_HIT, "minecraft:player_attack");
            Fx.addEffect(t, "minecraft:slowness", 60, 1);
            Vec3 away = t.position().subtract(p.position());
            Vec3 dir = away.lengthSqr() < 1.0E-4 ? p.getLookAngle() : away.normalize();
            double push = Wave7Math.tailPush(p.getPersistentData().contains(K_STR) ? p.getPersistentData().getDouble(K_STR) : 1.0);
            t.push(dir.x * push, 0.4, dir.z * push);
            t.hurtMarked = true;
        }
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.2, 0), Wave7Math.TAIL_RADIUS, 0x8FB4C8);
    }

    // ================================================================ state changes shared by acts and ticks
    private static void grantFlight(ServerPlayer p, CompoundTag d) {
        Abilities ab = p.getAbilities();
        if (ab.mayfly) return;
        ab.mayfly = true;
        d.putBoolean(K_WINGS_SET, true);
        p.onUpdateAbilities();
    }

    /** Takes back the flight the wings gave (never touches creative, spectator or flight someone else granted). */
    private static void restoreFlight(ServerPlayer p, CompoundTag d) {
        if (!d.getBoolean(K_WINGS_SET)) return;
        d.remove(K_WINGS_SET);
        if (p.isCreative() || p.isSpectator()) return;
        Abilities ab = p.getAbilities();
        ab.mayfly = false;
        ab.flying = false;
        p.onUpdateAbilities();
    }

    private static void endWings(ServerPlayer p, CompoundTag d, boolean softLanding) {
        boolean airborne = !p.onGround();
        d.remove(K_WINGS);
        restoreFlight(p, d);
        if (softLanding && airborne) Fx.addEffect(p, "minecraft:slow_falling", Wave7Math.WINGS_FALL_TICKS, 0);
        if (softLanding) tell(p, "The wings burn out.");
    }

    private static void endHead(ServerPlayer p, CompoundTag d) {
        d.remove(K_HEAD);
        p.removeEffect(MobEffects.NIGHT_VISION);
        tell(p, "You set your head back on.");
    }

    private static void endTrance(ServerPlayer p, CompoundTag d) {
        boolean was = d.getInt(K_TRANCE) > 0;
        d.putInt(K_TRANCE, 0);
        d.putInt(K_STILL, 0);
        if (was) p.removeEffect(MobEffects.NIGHT_VISION);
    }

    private static void endStar(CompoundTag d) {
        d.remove(K_STAR);
        d.remove(K_STAR_ON);
    }

    private static void fadeSoul(ServerPlayer p, CompoundTag d) {
        float tracked = d.getFloat(K_SOUL);
        if (tracked > 0f) p.setAbsorptionAmount(Wave7Math.soulFade(tracked, p.getAbsorptionAmount()));
        d.remove(K_SOUL);
        d.remove(K_SOUL_UNTIL);
    }

    /** Everything an armour of this wave left running (death, relog is handled separately). */
    private static void clearAll(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (d.contains(K_WINGS)) endWings(p, d, false);
        restoreFlight(p, d);
        endStar(d);
        d.remove(K_HEAD);
        d.remove(K_GUARD);
        d.remove(K_GOLD);
        d.remove(K_PROT);
        d.remove(K_EXPO);
        d.remove(K_SOUL);
        d.remove(K_SOUL_UNTIL);
        d.remove(K_TRANCE);
        d.remove(K_STILL);
        d.remove(K_VIS);
        d.remove(K_VIS_S);
        d.remove(K_DISPEL);
        d.remove(K_DRAG);
    }

    // ================================================================ the player tick (every 10 ticks per player, staggered)
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive() || !due(p, Wave7Math.SAMPLE)) return;
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        boolean second = ((p.tickCount + p.getId()) / Wave7Math.SAMPLE) % 2 == 0;     // every other sample = once per second

        boolean stone = has(p, Wave7.F_STONE), star = has(p, Wave7.F_STAR), gold = has(p, Wave7.F_GOLD), lion = has(p, Wave7.F_LION);
        boolean head = has(p, Wave7.F_HEAD), dragon = has(p, Wave7.F_DRAGON), soul = has(p, Wave7.F_SOUL), arcane = has(p, Wave7.F_ARCANE);
        boolean delph = has(p, Wave7.F_DELPH);

        // ---- Stone Sentinel: stillness and half hunger
        if (stone) { sentinel(p, d, now, second); }
        else if (d.getInt(K_TRANCE) > 0 || d.getInt(K_STILL) != 0) endTrance(p, d);

        // ---- Starstudded: the starscape
        if (star) { starscapeTick(p, d, now, second); }
        else if (d.contains(K_STAR)) endStar(d);

        // ---- Golden Shadow: reveal the hidden; the cloak runs out
        if (gold) { goldTick(p, d, now, second); }
        else if (d.contains(K_GOLD) || d.contains(K_ROGUE)) { d.remove(K_GOLD); d.remove(K_ROGUE); }

        // ---- Lionheart: nobody in the pride is frightened
        if (lion && second) {
            purgeFear(p);
            for (LivingEntity a : allies(p, Wave7Math.PRIDE_RADIUS)) purgeFear(a);
        }

        // ---- Beheaded: the detached head
        long headUntil = d.getLong(K_HEAD);
        if (headUntil > 0) {
            if (!head || headUntil <= now) endHead(p, d);
            else {
                if (Wave7Math.sensesOn(true, d.getLong(K_DISPEL), now)) {                 // the head's magical senses
                    quiet(p, "minecraft:night_vision", 260, 0);
                    quiet(p, "minecraft:water_breathing", 60, 0);
                    if (second) Fx.lendSkill(p, Wave7Skills.MAGIC_SENSE, Wave7Skills.LEASE);
                } else {
                    p.removeEffect(MobEffects.NIGHT_VISION);                                  // dispelled: dark for a minute (the immunities stay)
                }
                p.removeEffect(MobEffects.BLINDNESS);
                p.removeEffect(MobEffects.CONFUSION);
                p.removeEffect(MobEffects.DARKNESS);
            }
        }

        // ---- False Dragon: the wings
        long wingsUntil = d.getLong(K_WINGS);
        if (wingsUntil > 0) {
            if (!dragon || wingsUntil <= now) endWings(p, d, true);
            else wingsTick(p, d);
        }

        // ---- Soul Armor: the borrowed hit points fade
        if (second && d.getFloat(K_SOUL) > 0f) {
            if (!soul || now >= d.getLong(K_SOUL_UNTIL)) fadeSoul(p, d);
            else d.putFloat(K_SOUL, Math.min(d.getFloat(K_SOUL), p.getAbsorptionAmount()));
        }

        // ---- Arcane Conduction: the modes, and the spellcaster's pool behind them
        if (arcane) arcaneTick(p, d, now, second);
        else if (d.contains(K_INTB)) d.remove(K_INTB);

        // ---- Delphyne: frightful presence has no hold; plate too heavy for a small existence drags
        if (delph) delphyneTick(p, d, second);
        else if (d.contains(K_DRAG) || d.contains(K_STR)) { d.remove(K_DRAG); d.remove(K_STR); }

        // ---- how visible the wearer is (read by the visibility hook)
        int mode = d.getInt(K_ARCANE);
        boolean stealthMode = arcane && mode == Wave7Math.MODE_STEALTH;
        boolean heavy = lion || soul || delph;
        int tranceLevel = stone ? d.getInt(K_TRANCE) : 0;
        double rogue = rogue(d);
        double base = Wave7Math.visibility(false, tranceLevel, star, gold, rogue, stealthMode, heavy);
        double sneak = Wave7Math.visibility(true, tranceLevel, star, gold, rogue, stealthMode, heavy);
        if (base == 1.0 && sneak == 1.0) { d.remove(K_VIS); d.remove(K_VIS_S); }
        else { d.putDouble(K_VIS, base); d.putDouble(K_VIS_S, sneak); }
    }

    private static void sentinel(ServerPlayer p, CompoundTag d, long now, boolean second) {
        // stillness: no movement, no swinging, no sprinting for a few seconds
        Vec3 at = p.position();
        boolean known = d.contains(K_PX);
        double dx = at.x - d.getDouble(K_PX), dy = at.y - d.getDouble(K_PY), dz = at.z - d.getDouble(K_PZ);
        boolean moved = !known || dx * dx + dy * dy + dz * dz > Wave7Math.MOVE_EPS_SQ;
        boolean noisy = moved || p.swinging || p.isSprinting() || p.isUsingItem() || p.hurtTime > 0;
        d.putDouble(K_PX, at.x);
        d.putDouble(K_PY, at.y);
        d.putDouble(K_PZ, at.z);
        int still = Wave7Math.stillNext(d.getInt(K_STILL), noisy);
        d.putInt(K_STILL, still);
        int level = Wave7Math.tranceLevel(still), before = d.getInt(K_TRANCE);
        if (level > 0) {
            d.putInt(K_TRANCE, level);
            if (before == 0) tell(p, "You turn to stone: nothing notices you.");
            if (level == 2 && before < 2) tell(p, "Ten minutes of stillness: you are a statue, easy to overlook.");
            quiet(p, "minecraft:night_vision", 260, 0);
            if (second) {
                Fx.lendSkill(p, Wave7Skills.MAGIC_SENSE, Wave7Skills.LEASE);               // the card's +5 to Insight, Investigation and Perception
                com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_7, FxTone.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 4, 0.35, 0.6, 0.35, 0.0);
            }
            if (level == 2) {                                                              // the statue: overlooked entirely
                quiet(p, "minecraft:invisibility", 30, 0);
                quiet(p, "tensura:presence_concealment", 30, 0);
            }
        } else if (before > 0) {
            endTrance(p, d);
        }

        // half hunger: keep only half of the exhaustion gained since the last sample
        if (!second) return;
        FoodData fd = p.getFoodData();
        float ex = fd.getExhaustionLevel(), sat = fd.getSaturationLevel();
        int food = fd.getFoodLevel();
        if (d.contains(K_FOOD) && Wave7Math.hungerSynced(d.getInt(K_FOOD), food, d.getFloat(K_SAT), sat, now - d.getLong(K_FOOD_T))) {
            float adj = Wave7Math.halfHunger(d.getFloat(K_EX), ex);
            if (adj != ex) { fd.setExhaustion(adj); ex = adj; }
        }
        d.putInt(K_FOOD, food);
        d.putFloat(K_SAT, sat);
        d.putFloat(K_EX, ex);
        d.putLong(K_FOOD_T, now);
    }

    private static void starscapeTick(ServerPlayer p, CompoundTag d, long now, boolean second) {
        if (!on(d, K_STAR, now)) {
            if (d.getBoolean(K_STAR_ON)) { endStar(d); tell(p, "The starscape fades."); }
            return;
        }
        ServerLevel l = p.serverLevel();
        double r = Wave7Math.starRadius(l.getDayTime());
        if (second) FxRelic.ring(l, p.position().add(0, 0.1, 0), r, STAR_COLOR);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_7, FxTone.LIGHT, p.getX(), p.getY() + 1.2, p.getZ(), 6, r * 0.5, 0.9, r * 0.5, 0.01);
        revealHidden(p, r);
    }

    /** Invisible creatures near the wearer are outlined (they cannot hide in a starscape or from the Golden Shadow). */
    private static void revealHidden(ServerPlayer p, double r) {
        int n = 0;
        for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                x -> x != p && x.isInvisible() && x.isAlive() && !x.isSpectator() && !isAlly(p, x))) {
            Fx.addEffect(t, "minecraft:glowing", 40, 0);
            if (++n >= MAX_TARGETS) break;
        }
    }

    private static void goldTick(ServerPlayer p, CompoundTag d, long now, boolean second) {
        revealHidden(p, 12.0);
        if (d.contains(K_GOLD) && d.getLong(K_GOLD) <= now) d.remove(K_GOLD);
        if (second || !d.contains(K_ROGUE)) d.putDouble(K_ROGUE, Wave7Math.training(maxAura(p), Wave7Math.GATE_ROGUE_AURA));   // a Rogue's training, from the aura pool
    }

    private static void arcaneTick(ServerPlayer p, CompoundTag d, long now, boolean second) {
        if (second || !d.contains(K_INTB)) {
            double pool = maxMagicule(p);
            d.putDouble(K_INTB, Wave7Math.intBonus(pool));                                 // "AC 11 + Intelligence modifier"
            if (!Wave7Math.modeAllowed(d.getInt(K_ARCANE), Wave7Math.isCaster(pool))) d.putInt(K_ARCANE, Wave7Math.MODE_BASE);
        }
        int mode = d.getInt(K_ARCANE);
        if (mode == Wave7Math.MODE_FORTIFIED) quiet(p, "minecraft:slowness", 30, 1);
        if (mode == Wave7Math.MODE_STEALTH) {                                              // "you cannot cast spells in this mode"
            if (d.getLong(ManaLaw.SEALED) < now + 30) d.putLong(ManaLaw.SEALED, now + 30);  // the grimoire shuts (its own sealing flag)
            quiet(p, "tensura:silence", 30, 0);                                            // Tensura skills and magic are jammed (magic jamming)
            if (p.isCrouching()) quiet(p, "tensura:presence_concealment", 30, 0);          // the Hide action
        }
        if (second) {
            if (mode == Wave7Math.MODE_BASE) Fx.lendSkill(p, Wave7Skills.MAGIC_RESIST, Wave7Skills.LEASE);
            else if (mode == Wave7Math.MODE_FORTIFIED) Fx.lendSkill(p, Wave7Skills.PHYSICAL_RESIST, Wave7Skills.LEASE);
            else if (mode == Wave7Math.MODE_BURST) Fx.lendSkill(p, Wave7Skills.MAGIC_AURA, Wave7Skills.LEASE);
        }
    }

    private static void delphyneTick(ServerPlayer p, CompoundTag d, boolean second) {
        if (second) {
            purgeFear(p);
            double ep = maxEp(p);
            boolean drag = Wave7Math.weighedDown(ep);
            if (drag && !d.getBoolean(K_DRAG)) tell(p, "The dragon plate is too heavy for you: it needs " + (long) Wave7Math.GATE_STRENGTH_EP + " EP.");
            d.putBoolean(K_DRAG, drag);
            d.putDouble(K_STR, Wave7Math.training(ep, Wave7Math.GATE_STRENGTH_EP));       // "Strength 15": the stronger the existence, the harder the tail
        }
        if (d.getBoolean(K_DRAG)) quiet(p, "minecraft:slowness", 30, 0);                  // the old rule: too weak for the plate, slower
    }

    private static void wingsTick(ServerPlayer p, CompoundTag d) {
        grantFlight(p, d);
        quiet(p, "minecraft:fire_resistance", 60, 0);
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 fwd = new Vec3(look.x, 0, look.z);
        fwd = fwd.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : fwd.normalize();
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        for (int i = 0; i < 6; i++) {
            double spread = 0.45 + 0.28 * i, lift = 1.0 + 0.2 * i;
            for (int side = -1; side <= 1; side += 2) {
                Vec3 at = p.position().add(right.scale(side * spread)).add(fwd.scale(-0.4)).add(0, lift, 0);
                com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_7, FxTone.LIGHT, at.x, at.y, at.z, 1, 0.04, 0.04, 0.04, 0.01);
            }
        }
    }

    /** Removes Tensura's Frightened (fear) from one creature. */
    private static void purgeFear(LivingEntity e) {
        BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", "fear")).ifPresent(h -> {
            if (e.hasEffect(h)) e.removeEffect(h);
        });
    }

    // ================================================================ damage
    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        DamageSource src = e.getSource();
        long now = victim.level().getGameTime();
        CompoundTag vd = victim.getPersistentData();

        boolean prot = vd.getLong(K_PROT) > now, expo = vd.getLong(K_EXPO) > now;      // Lion's Resolve swap
        if (prot || expo) e.setAmount(e.getAmount() * Wave7Math.resolveFactor(prot, expo));

        if (victim instanceof ServerPlayer p) victimSide(e, p, src, now);
        if (src.getEntity() instanceof ServerPlayer a && a != victim) attackerSide(e, a, victim, src, now);
    }

    private static void victimSide(LivingIncomingDamageEvent e, ServerPlayer p, DamageSource src, long now) {
        CompoundTag d = p.getPersistentData();
        boolean melee = isMelee(src), shot = src.is(DamageTypeTags.IS_PROJECTILE);
        float amt = e.getAmount();

        if (has(p, Wave7.F_DUEL)) {
            if (melee && on(d, K_GUARD, now) && src.getEntity() instanceof LivingEntity att) {   // En Garde: the blow is turned aside
                d.remove(K_GUARD);
                e.setCanceled(true);
                parry(p, att);
                return;
            }
            amt *= Wave7Math.duelistTaken(p.getOffhandItem().isEmpty(), melee);
        }
        if (shot && has(p, Wave7.F_HEAD)) amt *= Wave7Math.headlessProjectile(on(d, K_HEAD, now));
        if (has(p, Wave7.F_ARCANE)) amt *= Wave7Math.arcaneTaken(d.getInt(K_ARCANE), Wave7Math.isForceLike(damagePath(src)), d.getDouble(K_INTB));
        Entity from = src.getEntity();
        if (from != null && has(p, Wave7.F_DELPH) && Wave7Math.isDragonish(entityPath(from.getType()))) amt *= Wave7Math.DRAGON_TAKEN;
        if (amt != e.getAmount()) e.setAmount(amt);

        // the starscape burns whoever strikes the wearer from inside it
        if (!busy && (melee || shot) && from instanceof LivingEntity att && att != p && on(d, K_STAR, now) && has(p, Wave7.F_STAR)) {
            double r = Wave7Math.starRadius(p.serverLevel().getDayTime()) + 1.0;
            if (att.distanceToSqr(p) <= r * r) {
                busy = true;
                try {
                    Fx.damage(p, att, Wave7Math.starRetaliation(att instanceof Player), "minecraft:magic");
                    Fx.addEffect(att, "minecraft:glowing", 60, 0);
                } finally { busy = false; }
            }
        }
    }

    private static void parry(ServerPlayer p, LivingEntity att) {
        Vec3 away = att.position().subtract(p.position());
        Vec3 dir = away.lengthSqr() < 1.0E-4 ? p.getLookAngle() : away.normalize();
        att.push(dir.x * 0.9, 0.3, dir.z * 0.9);
        att.hurtMarked = true;
        if (!busy) {
            busy = true;
            try { Fx.damage(p, att, 5f, "minecraft:player_attack"); } finally { busy = false; }
        }
        Fx.addEffect(p, "minecraft:speed", 80, 1);
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_7, FxTone.LIGHT, att.getX(), att.getY() + att.getBbHeight() * 0.6, att.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
        tell(p, "Parried!");
    }

    private static void attackerSide(LivingIncomingDamageEvent e, ServerPlayer a, LivingEntity victim, DamageSource src, long now) {
        CompoundTag ad = a.getPersistentData();
        boolean direct = src.getDirectEntity() == a || src.is(DamageTypeTags.IS_PROJECTILE);

        if (direct && has(a, Wave7.F_GOLD)) {
            boolean cloaked = on(ad, K_GOLD, now);
            boolean mob = victim instanceof Mob;
            boolean onYou = victim instanceof Mob m && m.getTarget() == a;
            if (Wave7Math.isSneakAttack(cloaked, mob, onYou)) {
                e.setAmount(e.getAmount() + Wave7Math.sneakBonus(e.getAmount(), rogue(ad)));
                com.newuniverse.nusmp.particle.fx.Fx.spray(a.serverLevel(), FxThemes.RELIC_WAVE_7, FxTone.LIGHT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 8, 0.25, 0.25, 0.25, 0.15);
            }
            if (cloaked) {                                              // striking breaks Unseen / Fade from View
                ad.remove(K_GOLD);
                a.removeEffect(MobEffects.INVISIBILITY);
            }
        }
        if (src.getDirectEntity() == a && has(a, Wave7.F_DUEL) && a.getOffhandItem().isEmpty()) Fx.addEffect(a, "minecraft:speed", 40, 1);   // a free step after a hit
        if (has(a, Wave7.F_ARCANE) && Wave7Math.isForceLike(damagePath(src))) e.setAmount(e.getAmount() * Wave7Math.arcaneDealt(ad.getInt(K_ARCANE), true));
    }

    // ================================================================ deaths
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof ServerPlayer p) clearAll(p);
        if (!(e.getSource().getEntity() instanceof ServerPlayer k) || k == dead) return;
        if (has(k, Wave7.F_SOUL) && isLiving(dead)) drinkSoul(k, dead);
        if (has(k, Wave7.F_GOLD)) {                                       // Fade from View
            long now = k.level().getGameTime();
            Fx.addEffect(k, "minecraft:invisibility", Wave7Math.FADE_TICKS, 0);
            quiet(k, "tensura:presence_concealment", Wave7Math.FADE_TICKS, 0);
            k.getPersistentData().putLong(K_GOLD, Math.max(k.getPersistentData().getLong(K_GOLD), now + Wave7Math.FADE_TICKS));
            tell(k, "You fade from view.");
        }
    }

    /** "Living creature": not undead, not an armour stand. */
    private static boolean isLiving(LivingEntity e) {
        return !(e instanceof ArmorStand) && !e.getType().is(EntityTypeTags.UNDEAD);
    }

    private static void drinkSoul(ServerPlayer k, LivingEntity dead) {
        CompoundTag d = k.getPersistentData();
        boolean first = !d.getBoolean(K_SOUL_AWAKE);
        float gain = Wave7Math.soulRoll(k.getRandom().nextInt(4)) + (first ? 3f : 0f);
        float[] r = Wave7Math.soulApply(d.getFloat(K_SOUL), k.getAbsorptionAmount(), gain, Wave7Math.SOUL_CAP);
        d.putFloat(K_SOUL, r[0]);
        k.setAbsorptionAmount(r[1]);
        d.putLong(K_SOUL_UNTIL, k.level().getGameTime() + Wave7Math.SOUL_FADE);
        if (first) {
            d.putBoolean(K_SOUL_AWAKE, true);
            Fx.say(k, "The armour drinks its first soul and wakes.");
        }
        returnEnergy(k, Wave7Math.soulEnergyShare(first));
        com.newuniverse.nusmp.particle.fx.Fx.spray(k.serverLevel(), FxThemes.RELIC_WAVE_7, FxTone.DARK, dead.getX(), dead.getY() + dead.getBbHeight() * 0.5, dead.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
        FxRelic.burst(k.serverLevel(), k.position().add(0, 1, 0), SOUL_COLOR);
    }

    /** The escaping life energy: a share of the wearer's maximum magicules and aura comes back (Tensura's existence storage). */
    private static void returnEnergy(ServerPlayer k, float share) {
        try {
            var ex = TensuraStorages.getExistenceFrom(k);
            if (ex == null) return;
            double mm = EnergyHelper.getMaxMagicule(k), ma = EnergyHelper.getMaxAura(k);
            ex.setMagicule(Math.min(mm, ex.getMagicule() + mm * share));
            ex.setAura(Math.min(ma, ex.getAura() + ma * share));
            ex.markDirty();
        } catch (Throwable noTensura) { /* no existence storage: nothing returns */ }
    }

    public static void onWake(PlayerWakeUpEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getPersistentData().getFloat(K_SOUL) > 0f) fadeSoul(p, p.getPersistentData());
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restoreFlight(p, p.getPersistentData());     // the wings resume on the next sample if still worn
    }

    // ================================================================ senses and statuses
    /** How visible the wearer is to mobs: the number the player tick stored. */
    public static void onVisibility(LivingEvent.LivingVisibilityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag d = p.getPersistentData();
        if (!d.contains(K_VIS)) return;
        e.modifyVisibility(p.isCrouching() ? d.getDouble(K_VIS_S) : d.getDouble(K_VIS));
    }

    /** Lionheart / Delphyne cannot be frightened; the headless knight cannot be blinded, sickened or darkened. */
    public static void onEffectApplicable(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ResourceLocation id = e.getEffectInstance().getEffect().unwrapKey().map(k -> k.location()).orElse(null);
        if (id == null) return;
        boolean fear = Wave7Math.isFear(id.getNamespace(), id.getPath());
        boolean headEffect = Wave7Math.isHeadEffect(id.getNamespace(), id.getPath());
        if (!fear && !headEffect) return;
        boolean deny = fear ? (has(p, Wave7.F_LION) || has(p, Wave7.F_DELPH))
                : (has(p, Wave7.F_HEAD) && on(p.getPersistentData(), K_HEAD, p.level().getGameTime()));
        if (deny) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    /** No head, no mouth: nothing can be eaten or drunk while the head is detached. */
    public static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!on(p.getPersistentData(), K_HEAD, p.level().getGameTime())) return;
        if (Wave7Math.headlessBlocks(e.getItem().getUseAnimation().name())) {
            e.setCanceled(true);
            tell(p, "Your head is off: you cannot eat or drink.");
        }
    }

    /** The Stone Sentinel sleeps half as much: half the phantoms that would spawn near a wearer do not. */
    public static void onSpawn(FinalizeSpawnEvent e) {
        if (e.getEntity().getType() != EntityType.PHANTOM) return;
        Player near = e.getLevel().getNearestPlayer(e.getX(), e.getY(), e.getZ(), 64.0, false);
        if (near != null && has(near, Wave7.F_STONE) && e.getLevel().getRandom().nextBoolean()) e.setSpawnCancelled(true);
    }
}
