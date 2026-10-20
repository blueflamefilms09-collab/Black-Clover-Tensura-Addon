package com.newuniverse.nusmp.relics.wave;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Wave 2's game-bus handlers and the little bit of state they share with the abilities (Wave2Acts). Everything here is server side, keyed
 * by player, bounded, and forgotten on logout and server stop. The per-second work runs once a second per player, staggered by entity id,
 * and only for players who carry a wave 2 relic.
 */
final class Wave2Events {
    private Wave2Events() {}

    private static final Logger LOG = LogUtils.getLogger();

    // ---------------------------------------------------------------- the flags the relics carry (Passive.flag)
    static final String F_ANY = "w2_any";               // on every relic of the wave: the gate of the per-second work
    static final String F_HOLY = "w2_holy_chains";
    static final String F_TOME = "w2_tome";
    static final String F_PHANTOM = "w2_phantom_steps";
    static final String F_MOTH = "w2_moth_rider";
    static final String F_ECHO = "w2_echolocation";
    static final String F_MANTLE = "w2_nightstalker";
    static final String F_NINE = "w2_nine_lives";
    static final String F_COMFORT = "w2_comfort";
    static final String F_DICH = "w2_dichotomy";
    static final String F_MIMIC = "w2_cloak_mimic";
    static final String F_ALERT = "w2_alert";

    static final String CAPE = "cape_of_nine_lives";

    // ---------------------------------------------------------------- shared state
    /** Players who currently wear the relic of that flag (refreshed once a second; the hot handlers read these instead of asking each time). */
    static final Set<UUID> PHANTOM = ConcurrentHashMap.newKeySet(), MOTH = ConcurrentHashMap.newKeySet(), MIMIC = ConcurrentHashMap.newKeySet(),
            DICH = ConcurrentHashMap.newKeySet(), TRACKED = ConcurrentHashMap.newKeySet();
    /** Ace of the pact deck: victim -> game time the vulnerability ends. */
    static final Map<UUID, Long> VULN = new ConcurrentHashMap<>();
    /** Night Patrol: wearer -> game time the open wings close. */
    static final Map<UUID, Long> FLIGHT = new ConcurrentHashMap<>();
    /** Cape of Nine Lives: wearer -> game time until which a second lethal blow costs no life. */
    static final Map<UUID, Long> GRACE = new ConcurrentHashMap<>();
    /** The tome: wearer -> game time an advance petition waits until. */
    static final Map<UUID, Long> ARMED = new ConcurrentHashMap<>();
    /** The tome: wearers whose next petition rolls with advantage (a "pending review"). */
    static final Set<UUID> ADVANTAGE = ConcurrentHashMap.newKeySet();
    /** Dichotomy: wearer -> {the smite is sweetened until, the spell is sharpened until}. */
    static final Map<UUID, long[]> BOOST = new ConcurrentHashMap<>();
    /** Moth Rider: wearer -> the moth entity. */
    static final Map<UUID, UUID> MOTHS = new ConcurrentHashMap<>();
    static final Wave2Tome.Ledger FAILURES = new Wave2Tome.Ledger();
    private static final ConcurrentLinkedQueue<Runnable> LATER = new ConcurrentLinkedQueue<>();

    static final int TOME_AGE = 200;                    // a failure can be appealed for ten seconds
    static final double TOME_RANGE = Wave2Rules.blocks(30);
    static final long ARMED_TICKS = 600;                // an advance petition waits thirty seconds
    private static final String FLY_KEY = "nusmp_w2_flight", DAY_KEY = "nusmp_w2_mimic_day";
    private static final ResourceLocation CAT_CURSE = ResourceLocation.fromNamespaceAndPath("nusmp", "w2_cat_curse");
    private static final double HOLY_RADIUS = 3.0;

    // ---------------------------------------------------------------- helpers
    /** Runs the task on the next server tick (never inside an event that is still changing the thing it would change). */
    static void later(Runnable r) { LATER.add(r); }

    static String typeId(DamageSource s) { return s.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse(""); }

    static String typePath(DamageSource s) { return s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse(""); }

    static String entityId(Entity e) { return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(); }

    static boolean evil(LivingEntity t) {
        return t.getType().is(EntityTypeTags.UNDEAD) || Wave2Rules.evilPath(BuiltInRegistries.ENTITY_TYPE.getKey(t.getType()).getPath());
    }

    static boolean has(LivingEntity e, String effectId) {
        ResourceLocation rl = ResourceLocation.tryParse(effectId);
        if (rl == null) return false;
        return BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> e.hasEffect(h)).orElse(false);
    }

    static void clear(LivingEntity e, String effectId) {
        ResourceLocation rl = ResourceLocation.tryParse(effectId);
        if (rl != null) BuiltInRegistries.MOB_EFFECT.getHolder(rl).ifPresent(h -> e.removeEffect(h));
    }

    private static void warn(String what, RuntimeException ex) { LOG.warn("[nusmp] Relics wave 2: {} failed: {}", what, ex.toString()); }

    // ---------------------------------------------------------------- the per-second work
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId() + 5) % 20 != 0) return;
        try {
            second(p, p.level().getGameTime());
        } catch (RuntimeException ex) {
            warn("the per-second work", ex);                                    // a relic must never stop the tick
        }
    }

    private static void second(ServerPlayer p, long now) {
        UUID id = p.getUUID();
        Set<String> fl = Relics.loadout(p).flags();
        if (!fl.contains(F_ANY)) {
            if (TRACKED.contains(id)) forget(p);                               // it just took the last one off
            return;
        }
        TRACKED.add(id);
        mark(PHANTOM, id, fl.contains(F_PHANTOM));
        mark(MOTH, id, fl.contains(F_MOTH));
        mark(MIMIC, id, fl.contains(F_MIMIC));
        mark(DICH, id, fl.contains(F_DICH));

        long step = now / 20;
        if (fl.contains(F_HOLY)) holyAura(p);
        if (fl.contains(F_ECHO) && step % 2 == 0) echolocation(p);
        if (fl.contains(F_ALERT) && step % 2 == 1) alert(p);
        if (fl.contains(F_COMFORT)) {
            if (p.getTicksFrozen() > 0) p.setTicksFrozen(0);
            clear(p, "tensura:chill");                                         // Tensura's cold exposure finds nothing to bite
        }
        if (fl.contains(F_MANTLE)) darkStealth(p);
        if (fl.contains(F_PHANTOM)) Fx.addEffect(p, "tensura:presence_concealment", 60, 0);    // no trace: nothing notices the wearer's presence
        if (fl.contains(F_MIMIC)) hunger(p);
        catTick(p, fl.contains(F_NINE), step);

        Long end = FLIGHT.get(id);
        if (end != null) {
            if (now >= end || !fl.contains(F_MANTLE)) endFlight(p, true);
            else keepFlying(p);
        }
    }

    private static void mark(Set<UUID> set, UUID id, boolean on) {
        if (on) set.add(id);
        else set.remove(id);
    }

    /** The player no longer carries anything of the wave: drop what was kept for them. */
    private static void forget(ServerPlayer p) {
        UUID id = p.getUUID();
        PHANTOM.remove(id);
        MOTH.remove(id);
        MIMIC.remove(id);
        DICH.remove(id);
        BOOST.remove(id);
        ARMED.remove(id);
        ADVANTAGE.remove(id);
        GRACE.remove(id);
        if (FLIGHT.containsKey(id)) endFlight(p, true);
        removeCat(p);
        TRACKED.remove(id);
    }

    // ---------------------------------------------------------------- Holy Chains
    private static void holyAura(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        List<LivingEntity> near = lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(HOLY_RADIUS),
                t -> t != p && t.isAlive() && !(t instanceof Player) && !(t instanceof OwnableEntity o && o.getOwnerUUID() != null) && evil(t));
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= 6) break;
            Fx.damage(p, t, 1.0f, "minecraft:magic");
            Fx.addEffect(t, "tensura:holy_damage", 30, 0);                      // Tensura's own holy burn on top, where it exists
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, t.getX(), t.getY(0.6), t.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
        }
    }

    // ---------------------------------------------------------------- Nightstalker's Mantle
    private static void echolocation(ServerPlayer p) {
        if (has(p, "tensura:silence")) return;                                  // deafened: no echo comes back
        double r = Wave2Rules.blocks(60);
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                t -> t != p && t.isAlive() && t instanceof Enemy && t.distanceToSqr(p) <= r * r && !p.hasLineOfSight(t));
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= 10) break;
            Fx.addEffect(t, "minecraft:glowing", 50, 0);                        // seen through the wall for a moment
        }
    }

    private static void darkStealth(ServerPlayer p) {
        if (p.serverLevel().getMaxLocalRawBrightness(p.blockPosition()) < 8) Fx.addEffect(p, "tensura:presence_concealment", 60, 0);
    }

    /** Night Patrol: the open wings are the survival-mode flight, for as long as the ability allows, never in creative or spectator. */
    static boolean grantFlight(ServerPlayer p, int ticks) {
        if (p.isCreative() || p.isSpectator()) return false;
        long end = p.level().getGameTime() + ticks;
        FLIGHT.put(p.getUUID(), end);
        p.getPersistentData().putLong(FLY_KEY, end);
        p.getAbilities().mayfly = true;
        p.onUpdateAbilities();
        return true;
    }

    private static void keepFlying(ServerPlayer p) {
        if (!p.getAbilities().mayfly && !p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
        }
    }

    static void endFlight(ServerPlayer p, boolean say) {
        FLIGHT.remove(p.getUUID());
        p.getPersistentData().remove(FLY_KEY);
        if (p.isCreative() || p.isSpectator()) return;
        boolean was = p.getAbilities().mayfly;
        p.getAbilities().mayfly = false;
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        if (was) {
            Fx.addEffect(p, "minecraft:slow_falling", 300, 0);                  // the wings fold gently
            if (say) Fx.say(p, "The wings fold. You drift down.");
        }
    }

    // ---------------------------------------------------------------- Cloak Mimic, tame
    /** Alert: the cloak's eyes turn round and point out whoever is creeping up from behind. */
    private static void alert(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(10.0),
                t -> t != p && t.isAlive() && t instanceof Mob m && m instanceof Enemy && m.getTarget() == p);
        int n = 0;
        for (LivingEntity t : near) {
            Vec3 d = t.position().subtract(p.position());
            if (d.x * look.x + d.z * look.z >= 0) continue;                     // in front of you: you can see it yourself
            Fx.addEffect(t, "minecraft:glowing", 40, 0);
            if (++n >= 3) break;
        }
    }

    /** Hungry: at the turn of each day it feeds on the wearer. */
    private static void hunger(ServerPlayer p) {
        long day = p.level().getDayTime() / 24000L;
        if (!p.getPersistentData().contains(DAY_KEY)) {
            p.getPersistentData().putLong(DAY_KEY, day);                       // the first morning it is worn: no bill yet
            return;
        }
        if (day <= p.getPersistentData().getLong(DAY_KEY)) return;
        p.getPersistentData().putLong(DAY_KEY, day);
        if (Wave2Mana.take(p, Wave2Rules.MIMIC_MANA_TAX)) {                    // it prefers a caster's magicules to their dinner
            Fx.say(p, "The cloak feeds on your magicules at dawn.");
            return;
        }
        // 1.99.0, THE HUNGER BAN: the cloak never takes food points or starves its wearer (Wave2Rules.hungerTax stays as tested data and is no longer applied);
        // with no magicules to take it only weighs the wearer down for a minute
        Fx.addEffect(p, "minecraft:weakness", 1200, 0);
        Fx.addEffect(p, "minecraft:slowness", 1200, 0);
        Fx.say(p, "The cloak is hungry and takes what it can.");
    }

    // ---------------------------------------------------------------- Cape of Nine Lives
    private static void catTick(ServerPlayer p, boolean worn, long step) {
        if (!worn) {
            removeCat(p);
            return;
        }
        ItemStack cape = Wave2Data.worn(p, CAPE);
        if (cape.isEmpty()) {
            removeCat(p);
            return;
        }
        Wave2Lives lives = Wave2Data.lives(cape);
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            AttributeModifier cur = hp.getModifier(CAT_CURSE);
            double want = lives.healthPenalty();
            if (cur != null && cur.amount() != want) {
                hp.removeModifier(CAT_CURSE);
                cur = null;
            }
            if (cur == null && want < 0) hp.addTransientModifier(new AttributeModifier(CAT_CURSE, want, AttributeModifier.Operation.ADD_VALUE));
            if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        }
        if (lives.catEyes()) Fx.addEffect(p, "minecraft:night_vision", 260, 0);
        if (lives.knocksThingsOver() && step % 30 == 0 && p.getRandom().nextDouble() < lives.knockChance()) knockSomething(p);
    }

    private static void removeCat(ServerPlayer p) {
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.removeModifier(CAT_CURSE);
    }

    /** The urge to knock things over: the nearest thing lying about gets a swat. */
    private static void knockSomething(ServerPlayer p) {
        ItemEntity best = null;
        double bestD = 9.0;
        for (ItemEntity it : p.serverLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(3.0))) {
            double d = it.distanceToSqr(p);
            if (d < bestD) {
                bestD = d;
                best = it;
            }
        }
        if (best == null) return;
        best.setDeltaMovement(p.getRandom().nextGaussian() * 0.15, 0.25, p.getRandom().nextGaussian() * 0.15);
        best.hurtMarked = true;
        p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.CAT_AMBIENT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Relics.loadout(p).flags().contains(F_NINE)) return;
        try {
            if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;     // /kill and the void are not blows a cat can catch
            ItemStack cape = Wave2Data.worn(p, CAPE);
            if (cape.isEmpty()) return;
            long now = p.level().getGameTime();
            UUID id = p.getUUID();
            boolean grace = now <= GRACE.getOrDefault(id, -1L);
            Wave2Lives before = Wave2Data.lives(cape);
            if (!grace && !before.alive()) return;                             // spent
            e.setCanceled(true);
            p.setHealth(1.0f);
            p.clearFire();
            p.fallDistance = 0;
            clear(p, "minecraft:wither");
            if (grace) return;                                                // the same moment's second blow: caught for free
            Wave2Lives after = before.afterSave();
            Wave2Data.setLives(cape, after);
            GRACE.put(id, now + 40);
            Fx.addEffect(p, "minecraft:resistance", 100, 3);
            Fx.shield(p, 6f, 200);
            Fx.lendSkills(p, 600, "tensura:self_regeneration", "tensura:pain_resistance");   // the spirit licks the wounds: regeneration and no pain for half a minute
            ServerLevel lv = p.serverLevel();
            lv.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.CAT_AMBIENT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.7f);
            Fx.burst(lv, p.position().add(0, 1, 0), 0xFF4C9A);
            if (after.justSpent(before)) {
                Fx.say(p, "Meow. The ninth life is gone: the cat spirit flies off and you are a cat. (Five minutes; it will come back for a Nether Star.)");
                Fx.addEffect(p, "minecraft:weakness", 6000, 2);
                Fx.addEffect(p, "minecraft:mining_fatigue", 6000, 2);
                Fx.addEffect(p, "minecraft:speed", 6000, 1);
                Fx.lendSkill(p, "tensura:beast_transformation", 6000);           // and Tensura's beast form is yours to take for those five minutes
            } else {
                Fx.say(p, "Meow. The cat spirit caught you: " + after.left() + " of 9 lives left.");
            }
        } catch (RuntimeException ex) {
            warn("the nine lives", ex);
        }
    }

    /** Remove Curse: a golden apple lifts the cat traits (the lives that are gone stay gone). */
    public static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Relics.loadout(p).flags().contains(F_NINE)) return;
        ItemStack eaten = e.getItem();
        if (!(eaten.is(Items.GOLDEN_APPLE) || eaten.is(Items.ENCHANTED_GOLDEN_APPLE))) return;
        ItemStack cape = Wave2Data.worn(p, CAPE);
        if (cape.isEmpty()) return;
        Wave2Lives l = Wave2Data.lives(cape);
        if (l.cat() == 0) return;
        Wave2Data.setLives(cape, l.cured());
        removeCat(p);
        Fx.say(p, "The golden apple lifts the curse: the whiskers and the urge to knock things over fade.");
    }

    // ---------------------------------------------------------------- damage
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            LivingEntity victim = e.getEntity();
            DamageSource src = e.getSource();
            if (!VULN.isEmpty()) {
                Long until = VULN.get(victim.getUUID());
                if (until != null) {
                    if (until > victim.level().getGameTime()) e.setAmount(e.getAmount() * 1.3f);
                    else VULN.remove(victim.getUUID());
                }
            }
            if (victim instanceof ServerPlayer p) {
                UUID id = p.getUUID();
                if (PHANTOM.contains(id) && (src.is(DamageTypes.HOT_FLOOR) || src.is(DamageTypes.SWEET_BERRY_BUSH))) {
                    e.setCanceled(true);                                       // the steps do not touch the magma or the thorns
                    return;
                }
                if (MIMIC.contains(id) && Wave2Rules.acid(typePath(src))) e.setAmount(e.getAmount() * 0.5f);
            }
            if (!DICH.isEmpty() && src.getEntity() instanceof ServerPlayer att && DICH.contains(att.getUUID())) dichotomy(e, att);
        } catch (RuntimeException ex) {
            warn("the damage handler", ex);
        }
    }

    /** Dichotomy: a blow struck by hand sharpens the next spell, a spell sweetens the next blow; each is spent once. */
    private static void dichotomy(LivingIncomingDamageEvent e, ServerPlayer att) {
        LivingEntity victim = e.getEntity();
        if (victim == att) return;
        Wave2Rules.Hit hit = Wave2Rules.classify(typeId(e.getSource()), e.getSource().getDirectEntity() == att);
        if (hit == Wave2Rules.Hit.OTHER) return;
        long now = att.level().getGameTime();
        long[] b = BOOST.computeIfAbsent(att.getUUID(), k -> new long[2]);
        ServerLevel lv = att.serverLevel();
        if (hit == Wave2Rules.Hit.MELEE) {
            if (b[0] > now) {
                e.setAmount(e.getAmount() + (float) Wave2Rules.smiteBonus(1 + att.getRandom().nextInt(8), evil(victim)));
                b[0] = 0;
                com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, victim.getX(), victim.getY(0.6), victim.getZ(), 8, 0.25, 0.4, 0.25, 0.04);
                if (evil(victim)) Fx.addEffect(victim, "tensura:holy_damage", 40, 0);
            }
            b[1] = now + Wave2Rules.BOOST_TICKS;
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.DARK, att.getX(), att.getY(0.8), att.getZ(), 4, 0.3, 0.4, 0.3, 0.02);
        } else {
            if (b[1] > now) {
                e.setAmount(e.getAmount() * (float) Wave2Rules.SPELL_BOOST);
                b[1] = 0;
            }
            b[0] = now + Wave2Rules.BOOST_TICKS;
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(lv, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, att.getX(), att.getY(0.8), att.getZ(), 4, 0.3, 0.4, 0.3, 0.02);
        }
    }

    // ---------------------------------------------------------------- Phantom Steps
    /** The steps raise no vibration: a sculk sensor, a shrieker or the Warden hears nothing. */
    public static void onGameEvent(VanillaGameEvent e) {
        if (PHANTOM.isEmpty()) return;
        if (e.getCause() instanceof ServerPlayer p && PHANTOM.contains(p.getUUID()) && e.getVanillaEvent().is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING))
            e.setCanceled(true);
    }

    /** ... and make no sound for anyone else to hear. */
    public static void onSound(PlayLevelSoundEvent.AtEntity e) {
        if (PHANTOM.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || !PHANTOM.contains(p.getUUID())) return;
        Holder<SoundEvent> sound = e.getSound();
        if (sound == null) return;
        if (sound.unwrapKey().map(k -> k.location().getPath().endsWith(".step")).orElse(false)) e.setCanceled(true);
    }

    /** Footsteps are played at a position, not at the player: a step sound within a block and a half of a wearer is cancelled (server side). */
    public static void onStepSound(PlayLevelSoundEvent.AtPosition e) {
        if (PHANTOM.isEmpty() || e.getLevel().isClientSide()) return;
        Holder<SoundEvent> sound = e.getSound();
        if (sound == null || !sound.unwrapKey().map(k -> k.location().getPath().endsWith(".step")).orElse(false)) return;
        Vec3 at = e.getPosition();
        for (Player pl : e.getLevel().players()) {
            if (PHANTOM.contains(pl.getUUID()) && pl.distanceToSqr(at) < 2.25) {
                e.setCanceled(true);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- Cloak of the Moth Rider
    /** Fearsome Visage's pattern: one monster in five does not choose the wearer as its first target (unless the wearer hurt it first). */
    public static void onChangeTarget(LivingChangeTargetEvent e) {
        if (MOTH.isEmpty() || !(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p) || !MOTH.contains(p.getUUID())) return;
        if (!(e.getEntity() instanceof Mob m) || !(m instanceof Enemy)) return;
        if (m.getLastHurtByMob() == p || BalanceLaw.isBoss(m)) return;
        if (Wave2Rules.intimidated(m.getUUID().hashCode())) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- effects: the tome and the mimic
    /** Every harmful effect that lands on a player goes on file for the tome (ten seconds), and an advance petition is filed against it. */
    public static void onEffectAdded(MobEffectEvent.Added e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        try {
            MobEffectInstance inst = e.getEffectInstance();
            if (inst == null || inst.isAmbient()) return;
            Holder<MobEffect> holder = inst.getEffect();
            if (holder.value().getCategory() != MobEffectCategory.HARMFUL) return;
            String id = holder.unwrapKey().map(k -> k.location().toString()).orElse(null);
            if (id == null) return;
            long now = sp.level().getGameTime();
            Entity src = e.getEffectSource();
            FAILURES.record(sp.getUUID(), id, inst.getAmplifier(), inst.getDuration(), now, src == null ? null : src.getUUID());
            if (!ARMED.isEmpty()) fileAdvance(sp, now);
        } catch (RuntimeException ex) {
            warn("the failure ledger", ex);
        }
    }

    /** A wearer who filed in advance: the next failure within range (theirs or a neighbour's) is appealed on the next tick. */
    private static void fileAdvance(ServerPlayer hit, long now) {
        MinecraftServer server = hit.getServer();
        if (server == null) return;
        Iterator<Map.Entry<UUID, Long>> it = ARMED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> a = it.next();
            if (a.getValue() < now) {
                it.remove();
                continue;
            }
            ServerPlayer w = server.getPlayerList().getPlayer(a.getKey());
            if (w == null || w.level() != hit.level() || w.distanceToSqr(hit) > TOME_RANGE * TOME_RANGE) continue;
            it.remove();
            later(() -> Wave2Acts.appeal(w));
        }
    }

    /** The mimic's immunities: the corrosion of Tensura finds nothing to eat. */
    public static void onApplicable(MobEffectEvent.Applicable e) {
        if (MIMIC.isEmpty() || !(e.getEntity() instanceof ServerPlayer p) || !MIMIC.contains(p.getUUID())) return;
        MobEffectInstance inst = e.getEffectInstance();
        if (inst == null) return;
        boolean acid = inst.getEffect().unwrapKey().map(k -> k.location().getNamespace().equals("tensura") && Wave2Rules.acid(k.location().getPath())).orElse(false);
        if (acid) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    // ---------------------------------------------------------------- lifecycle
    public static void onServerTick(ServerTickEvent.Post e) {
        for (int i = 0; i < 8; i++) {
            Runnable r = LATER.poll();
            if (r == null) break;
            try {
                r.run();
            } catch (RuntimeException ex) {
                warn("a deferred task", ex);
            }
        }
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 200 == 0) {
            long now = server.overworld().getGameTime();
            FAILURES.prune(now, TOME_AGE * 2L);
            VULN.values().removeIf(t -> t < now);
            GRACE.values().removeIf(t -> t < now);
            ARMED.values().removeIf(t -> t < now);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (FLIGHT.containsKey(p.getUUID()) || p.getPersistentData().contains(FLY_KEY)) endFlight(p, false);   // the saved abilities must not keep the wings
        removeCat(p);
        UUID id = p.getUUID();
        for (Set<UUID> s : List.of(PHANTOM, MOTH, MIMIC, DICH, TRACKED, ADVANTAGE)) s.remove(id);
        for (Map<UUID, ?> m : List.of(VULN, FLIGHT, GRACE, ARMED, BOOST, MOTHS)) m.remove(id);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getPersistentData().contains(FLY_KEY)) endFlight(p, false);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getPersistentData().contains(FLY_KEY)) endFlight(p, false);
    }

    public static void onServerStopping(ServerStoppingEvent e) {
        for (Set<UUID> s : List.of(PHANTOM, MOTH, MIMIC, DICH, TRACKED, ADVANTAGE)) s.clear();
        VULN.clear();
        FLIGHT.clear();
        GRACE.clear();
        ARMED.clear();
        BOOST.clear();
        MOTHS.clear();
        FAILURES.clear();
        LATER.clear();
    }
}
