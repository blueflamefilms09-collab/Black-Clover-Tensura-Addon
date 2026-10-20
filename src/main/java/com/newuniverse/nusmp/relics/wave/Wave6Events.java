package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Wave 6: the game-bus side of the ten armours and robes, one listener per event, each one starting with the cheapest test (is a wearer even involved)
 * so the other thousand damage events a second cost nothing. Ongoing states (Slime Form, Dawnlight, flight, Mage Armor, the Black Veil) are checked
 * every ten ticks per player, staggered by entity id. The tabletop translation of each card is in docs/doc_wave6.md.
 */
public final class Wave6Events {
    private Wave6Events() {}

    private static final String K_SKY_CD = "w6_sky_absorb_cd", K_VOID_CD = "w6_void_cd", K_ZAR_WINDOW = "w6_zar_window", K_ZAR_GUIDE = "w6_zar_guide",
            K_VENGEANCE = "w6_zar_vengeance", K_PLOT_LUCK = "w6_plot_luck", K_FLY_SPEED = "w6_fly_speed";
    private static final int SKY_BLUE = 0x8FD8FF;
    private static final Set<String> TIME_EFFECTS = Set.of("minecraft:poison", "minecraft:wither", "minecraft:hunger", "tensura:infection", "tensura:fatal_poison", "tensura:magicule_poison");
    private static final String[] TRAVEL_SICKNESS = {"minecraft:nausea", "minecraft:blindness", "minecraft:darkness", "tensura:confusion", "tensura:insanity"};

    /** True while Timelessness rewrites an effect, so the effect-negating hooks leave its own re-adding alone. */
    private static boolean shaving;

    public static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(Wave6Events::onIncoming);
        bus.addListener(Wave6Events::onDamaged);
        bus.addListener(Wave6Events::onDeath);
        bus.addListener(Wave6Events::onEffect);
        bus.addListener(Wave6Events::onPlayerTick);
        bus.addListener(Wave6Events::onServerTick);
        bus.addListener(Wave6Events::onUseStart);
        bus.addListener(Wave6Events::onRightClick);
        bus.addListener(Wave6Events::onAttack);
        bus.addListener(Wave6Events::onLogout);
        bus.addListener(Wave6Events::onDimension);
        bus.addListener(Wave6Events::onStopping);
    }

    /** An optional hook for the grimoire cast code: "the robe stores spells cast into it" (one stored Mage Armor per cast, five at most). */
    public static void onSpellCast(ServerPlayer p) {
        if (!RelicFlags.has(p, Wave6Tune.F_NEKET)) return;
        ItemStack robe = Wave6Kit.find(p, Wave6Tune.ID_NEKET);
        if (robe.isEmpty()) return;
        Wave6Pool.State st = Wave6Kit.pool(robe, Wave6Pools.MAGE, p);
        if (st.charges() < Wave6Pools.MAGE.max()) Wave6Kit.put(robe, Wave6Pools.MAGE, Wave6Pool.add(st, 1, Wave6Pools.MAGE.max()));
    }

    // ================================================================ damage in
    private static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        DamageSource src = e.getSource();
        if (Wave6Zones.anyGlobe() && Wave6Kit.isMagic(src) && Wave6Zones.globeCovers(victim)) { e.setCanceled(true); return; }
        if (src.getEntity() instanceof LivingEntity att && att != victim) guidance(e, att, victim);
        if (victim instanceof ServerPlayer p && p.isAlive() && !p.isSpectator()) wearerHurt(e, p, src);
    }

    private static void wearerHurt(LivingIncomingDamageEvent e, ServerPlayer p, DamageSource src) {
        long now = p.level().getGameTime();
        CompoundTag d = p.getPersistentData();
        boolean magic = Wave6Kit.isMagic(src);

        if (RelicFlags.has(p, Wave6Tune.F_SLIME)) {
            if (slimed(p) && src.is(DamageTypeTags.IS_FALL)) { e.setCanceled(true); return; }      // a puddle lands softly
            if (Wave6Kit.slashing(src)) e.setAmount(e.getAmount() * (float) Wave6Tune.SLASH_FACTOR);
            else if (Wave6Kit.bludgeoning(src)) e.setAmount(e.getAmount() * (float) Wave6Tune.BLUNT_FACTOR);
        }
        if (magic && RelicFlags.has(p, Wave6Tune.F_SKY) && skyHurt(e, p, now, d)) return;
        if (magic && RelicFlags.has(p, Wave6Tune.F_ASTRAL) && voidHurt(e, p, now, d)) return;
        if (RelicFlags.has(p, Wave6Tune.F_DAWN)) dawnHurt(e, p, src, now, d);
        if (RelicFlags.has(p, Wave6Tune.F_PLOT) && plotDodge(e, p, src)) return;
        if (RelicFlags.has(p, Wave6Tune.F_NEKET) && src.getEntity() instanceof LivingEntity a && a != p && d.getLong(Wave6Acts.K_MAGE_UNTIL) <= now) {
            Wave6Acts.castMageArmor(p, Wave6Kit.find(p, Wave6Tune.ID_NEKET), false);           // the robe keeps its wearer's defences up
        }
    }

    /** Skycrystal: Spell Absorption (the spell is cancelled and its power fills the armour), else a quarter of magic damage is warded off. True if cancelled. */
    private static boolean skyHurt(LivingIncomingDamageEvent e, ServerPlayer p, long now, CompoundTag d) {
        ItemStack s = Wave6Kit.find(p, Wave6Tune.ID_SKY);
        if (s.isEmpty()) return false;
        Wave6Pool.State st = Wave6Kit.pool(s, Wave6Pools.SKY, p);
        if (st.charges() < Wave6Pools.SKY.max() && now >= d.getLong(K_SKY_CD)) {
            e.setCanceled(true);
            d.putLong(K_SKY_CD, now + Wave6Tune.SKY_ABSORB_COOLDOWN);
            int gain = Wave6Tune.absorbGain(e.getAmount());
            Wave6Pool.State after = Wave6Pool.add(st, gain, Wave6Pools.SKY.max());
            Wave6Kit.put(s, Wave6Pools.SKY, after);
            Fx.burst(p.serverLevel(), p.position().add(0, 1, 0), SKY_BLUE);
            Wave6Kit.hint(p, "Spell Absorption: the crystals drink the spell (+" + (after.charges() - st.charges()) + " charges, " + after.charges() + "/" + Wave6Pools.SKY.max() + ").");
            return true;
        }
        e.setAmount(e.getAmount() * (float) Wave6Tune.SKY_WARD_FACTOR);
        return false;
    }

    /** Astral Overseer, Into the Void: one of two daily wards throws a spell aside and charges the wearer. True if cancelled. */
    private static boolean voidHurt(LivingIncomingDamageEvent e, ServerPlayer p, long now, CompoundTag d) {
        if (now < d.getLong(K_VOID_CD)) return false;
        ItemStack s = Wave6Kit.find(p, Wave6Tune.ID_ASTRAL);
        if (s.isEmpty()) return false;
        Wave6Pool.State st = Wave6Kit.pool(s, Wave6Pools.VOID, p);
        if (!Wave6Pool.canSpend(st, 1)) return false;
        Wave6Kit.put(s, Wave6Pools.VOID, Wave6Pool.spend(st, 1));
        d.putLong(K_VOID_CD, now + Wave6Tune.ASTRAL_VOID_COOLDOWN);
        e.setCanceled(true);
        Wave6Kit.give(p, "minecraft:strength", 160, 0, false);
        Wave6Kit.give(p, "minecraft:speed", 160, 0, false);
        Fx.lendSkill(p, Wave6Skills.THOUGHT_ACCELERATION, 160);                       // "advantage on your next spell attack": thought acceleration for eight seconds
        Fx.burst(p.serverLevel(), p.position().add(0, 1, 0), 0xB070FF);
        Wave6Kit.hint(p, "Into the Void: the spell finds nothing to strike (" + (st.charges() - 1) + "/" + Wave6Pools.VOID.max() + " left today). You are charged with the void.");
        return true;
    }

    /** Defiant Dawn, Luminous Retribution: a real blow from close by makes the armour flash, once every two seconds, for one of its three charges. */
    private static void dawnHurt(LivingIncomingDamageEvent e, ServerPlayer p, DamageSource src, long now, CompoundTag d) {
        if (e.getAmount() < 4f || now < d.getLong(Wave6Acts.K_DAWN_CD)) return;
        if (!(src.getEntity() instanceof LivingEntity a) || a == p || a.distanceToSqr(p) > 64) return;
        ItemStack s = Wave6Kit.find(p, Wave6Tune.ID_DAWN);
        if (s.isEmpty()) return;
        Wave6Pool.State st = Wave6Kit.pool(s, Wave6Pools.DAWN, p);
        if (!Wave6Pool.canSpend(st, 1)) return;
        Wave6Kit.put(s, Wave6Pools.DAWN, Wave6Pool.spend(st, 1));
        d.putLong(Wave6Acts.K_DAWN_CD, now + Wave6Tune.DAWN_FLASH_COOLDOWN);
        Wave6Acts.flash(p);
        Wave6Kit.hint(p, "Luminous Retribution! (" + (st.charges() - 1) + "/" + Wave6Pools.DAWN.max() + " charges left)");
    }

    /** Armor of Plot: "when hit, negates all damage, but in a super cool way". True if the blow is cancelled. */
    private static boolean plotDodge(LivingIncomingDamageEvent e, ServerPlayer p, DamageSource src) {
        if (src.getEntity() == null || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        ItemStack s = Wave6Kit.find(p, Wave6Tune.ID_PLOT);
        if (s.isEmpty() || !Wave6Pool.canSpend(Wave6Kit.pool(s, Wave6Pools.PLOT, p), 1)) return false;
        if (p.getRandom().nextDouble() >= Wave6Tune.plotDodge(p.getHealth(), p.getMaxHealth())) return false;
        e.setCanceled(true);
        Fx.addEffect(p, Wave6Skills.FX_FATE_CHANGE, 60, 0);
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 12, 0.4, 0.5, 0.4, 0.2);
        Wave6Kit.hint(p, "The blow misses by a hair.");
        return true;
    }

    /** Z'ar Sancuis, Prophet's Guidance: an ally fighting within six blocks of the wearer hits for +3 (once every three seconds per wearer). */
    private static void guidance(LivingIncomingDamageEvent e, LivingEntity att, LivingEntity victim) {
        if (!(att instanceof ServerPlayer) && !(att instanceof TamableAnimal)) return;
        long now = att.level().getGameTime();
        List<ServerPlayer> near = att.level().getEntitiesOfClass(ServerPlayer.class, att.getBoundingBox().inflate(Wave6Tune.GUIDANCE_RADIUS),
                w -> w != att && w.isAlive() && RelicFlags.has(w, Wave6Tune.F_ZAR) && Wave6Kit.isAlly(w, att) && !Wave6Kit.isAlly(w, victim));
        for (ServerPlayer w : near) {
            CompoundTag wd = w.getPersistentData();
            if (now < wd.getLong(K_ZAR_GUIDE)) continue;
            wd.putLong(K_ZAR_GUIDE, now + Wave6Tune.GUIDANCE_COOLDOWN);
            e.setAmount(e.getAmount() + Wave6Tune.GUIDANCE_BONUS);
            Fx.addEffect(att, Wave6Skills.FX_ALLY_BOOST, 100, 0);                      // the prophet's blessing, as Tensura's ally boost
            Fx.burst(w.serverLevel(), att.position().add(0, 1, 0), 0xC03030);
            return;
        }
    }

    // ================================================================ damage dealt
    /** Z'ar Sancuis, Healing through Vengeance: a hit on a creature that is hostile to you heals you. */
    private static void onDamaged(LivingDamageEvent.Post e) {
        if (e.getNewDamage() <= 0f || !(e.getSource().getEntity() instanceof ServerPlayer p) || p == e.getEntity() || !p.isAlive()) return;
        if (!RelicFlags.has(p, Wave6Tune.F_ZAR) || p.getHealth() >= p.getMaxHealth()) return;
        LivingEntity t = e.getEntity();
        boolean hostile = t instanceof Enemy || (t instanceof Mob m && m.getTarget() == p);
        long now = p.level().getGameTime();
        CompoundTag d = p.getPersistentData();
        if (!hostile || now < d.getLong(K_VENGEANCE)) return;
        d.putLong(K_VENGEANCE, now + Wave6Tune.VENGEANCE_COOLDOWN);
        Fx.heal(p, Wave6Tune.VENGEANCE_HEAL);
    }

    // ================================================================ death
    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (RelicFlags.has(p, Wave6Tune.F_PLOT) && !e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && plotSave(p)) { e.setCanceled(true); return; }
        clearTemp(p, true);
    }

    /** Armor of Plot: a blow that would end the story is cancelled and the wearer is left on a third of the bar, for one of three plot points. */
    private static boolean plotSave(ServerPlayer p) {
        ItemStack s = Wave6Kit.find(p, Wave6Tune.ID_PLOT);
        if (s.isEmpty()) return false;
        Wave6Pool.State st = Wave6Kit.pool(s, Wave6Pools.PLOT, p);
        if (!Wave6Pool.canSpend(st, 1)) return false;
        Wave6Kit.put(s, Wave6Pools.PLOT, Wave6Pool.spend(st, 1));
        p.setHealth(Wave6Tune.plotSaveHealth(p.getMaxHealth()));
        for (MobEffectInstance m : new ArrayList<>(p.getActiveEffects())) {
            if (m.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(m.getEffect());
        }
        p.setRemainingFireTicks(0);
        Wave6Kit.give(p, "minecraft:resistance", Wave6Tune.PLOT_GRACE_TICKS, 3, false);
        Wave6Kit.give(p, "minecraft:regeneration", Wave6Tune.PLOT_GRACE_TICKS, 1, false);
        Fx.addEffect(p, Wave6Skills.FX_FATE_CHANGE, Wave6Tune.PLOT_GRACE_TICKS, 0);
        Fx.burst(p.serverLevel(), p.position().add(0, 1, 0), 0xFFD27A);
        Fx.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 3.0, 0xFFD27A);
        int left = st.charges() - 1;
        Fx.say(p, left > 0 ? "Plot Armor: the story is not over yet (" + left + "/" + Wave6Pools.PLOT.max() + " plot points left)."
                : "Plot Armor: that was the last plot point. The ending is near: Luck -3, no more saves, until a point returns at dawn.");
        return true;
    }

    // ================================================================ harmful effects in
    private static void onEffect(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || shaving) return;
        MobEffectInstance inst = e.getEffectInstance();
        if (inst == null || inst.getEffect().value().getCategory() != MobEffectCategory.HARMFUL || e.getEffectSource() == p) return;
        if (RelicFlags.has(p, Wave6Tune.F_SLIME) && Wave6Kit.restraint(inst)) { deny(e); return; }
        if (inst.getDuration() < 40) return;
        if (RelicFlags.has(p, Wave6Tune.F_ZAR) && legendaryResistance(p)) { deny(e); return; }
        if (RelicFlags.has(p, Wave6Tune.F_SKY) && p.getRandom().nextDouble() < Wave6Tune.SKY_EFFECT_SAVE) {
            deny(e);
            Wave6Kit.hint(p, "The crystals turn the curse away.");
        }
    }

    private static void deny(MobEffectEvent.Applicable e) { e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY); }

    /** Z'ar Sancuis, Legendary Resistance: a failed save becomes a success, twice a day; the second effect of the same burst (six seconds) is free. */
    private static boolean legendaryResistance(ServerPlayer p) {
        long now = p.level().getGameTime();
        CompoundTag d = p.getPersistentData();
        if (now < d.getLong(K_ZAR_WINDOW)) return true;
        ItemStack s = Wave6Kit.find(p, Wave6Tune.ID_ZAR);
        if (s.isEmpty()) return false;
        Wave6Pool.State st = Wave6Kit.pool(s, Wave6Pools.ZAR, p);
        if (!Wave6Pool.canSpend(st, 1)) return false;
        Wave6Kit.put(s, Wave6Pools.ZAR, Wave6Pool.spend(st, 1));
        d.putLong(K_ZAR_WINDOW, now + Wave6Tune.ZAR_WINDOW);
        Fx.burst(p.serverLevel(), p.position().add(0, 1, 0), 0xC03030);
        Wave6Kit.hint(p, "Legendary Resistance! (" + (st.charges() - 1) + "/" + Wave6Pools.ZAR.max() + " left today)");
        return true;
    }

    // ================================================================ ongoing states, ten ticks at a time
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        int pass = (p.tickCount + p.getId()) / 10;
        if ((p.tickCount + p.getId()) % 10 != 0) return;
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        slimeTick(p, d, now);
        mageTick(p, d, now);
        dawnTick(p, d, pass);
        flightTick(p, d);
        veilTick(p);
        plotTick(p, d, pass);
        if (pass % 10 == 0 && RelicFlags.has(p, Wave6Tune.F_SKY)) timeless(p);
    }

    // ---------------------------------------------------------------- Slime Form
    private static boolean slimed(Player p) { return p.getPersistentData().getLong(Wave6Acts.K_SLIME) > p.level().getGameTime(); }

    public static void slimeMods(ServerPlayer p, boolean on) {
        Wave6Kit.setMod(p, "minecraft:generic.scale", "slime_scale", Wave6Tune.SLIME_SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on);
        Wave6Kit.setMod(p, "minecraft:generic.knockback_resistance", "slime_kb", Wave6Tune.SLIME_KNOCKBACK, AttributeModifier.Operation.ADD_VALUE, on);
    }

    private static void slimeTick(ServerPlayer p, CompoundTag d, long now) {
        long until = d.getLong(Wave6Acts.K_SLIME);
        if (until <= 0) return;
        if (!RelicFlags.has(p, Wave6Tune.F_SLIME) || now >= until) { endSlime(p, true); return; }
        slimeMods(p, true);                                                                       // re-applied after a relog
        Wave6Kit.give(p, "minecraft:jump_boost", 40, 0, true);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(p.serverLevel(), com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 0.3, p.getZ(), 4, 0.2, 0.1, 0.2, 0.02);
    }

    /** Ends Slime Form. With {@code checkFit} it waits (up to 25 s) while there is no room to stand up again, so nobody is squeezed into a wall. */
    public static void endSlime(ServerPlayer p, boolean checkFit) {
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        if (checkFit && d.getInt(Wave6Acts.K_SLIME_EXT) < Wave6Tune.SLIME_EXTEND_MAX
                && !p.level().noCollision(p, new AABB(p.getX() - 0.3, p.getY(), p.getZ() - 0.3, p.getX() + 0.3, p.getY() + 1.8, p.getZ() + 0.3))) {
            d.putInt(Wave6Acts.K_SLIME_EXT, d.getInt(Wave6Acts.K_SLIME_EXT) + 1);
            d.putLong(Wave6Acts.K_SLIME, now + 100);
            Fx.say(p, "There is no room to stand up. The slime holds a little longer.");
            return;
        }
        d.putLong(Wave6Acts.K_SLIME, 0);
        d.remove(Wave6Acts.K_SLIME_EXT);
        slimeMods(p, false);
        Fx.burst(p.serverLevel(), p.position().add(0, 0.8, 0), 0x6BFFB5);
        Fx.say(p, "You take your own shape again.");
    }

    // ---------------------------------------------------------------- Mage Armor
    public static void mageMods(ServerPlayer p, boolean on) {
        Wave6Kit.setMod(p, "minecraft:generic.armor", "mage_armor", Wave6Tune.MAGE_ARMOR, AttributeModifier.Operation.ADD_VALUE, on);
    }

    private static void mageTick(ServerPlayer p, CompoundTag d, long now) {
        long until = d.getLong(Wave6Acts.K_MAGE_UNTIL);
        if (until <= 0) return;
        if (now >= until) { d.putLong(Wave6Acts.K_MAGE_UNTIL, 0); mageMods(p, false); Fx.say(p, "Mage Armor fades."); }
        else mageMods(p, true);
    }

    // ---------------------------------------------------------------- Dawnlight
    private static void dawnTick(ServerPlayer p, CompoundTag d, int pass) {
        if (!d.getBoolean(Wave6Acts.K_DAWN_ON)) return;
        if (!RelicFlags.has(p, Wave6Tune.F_DAWN)) { d.putBoolean(Wave6Acts.K_DAWN_ON, false); return; }
        Wave6Kit.give(p, "minecraft:night_vision", 260, 0, true);
        Wave6Kit.lift(p, "minecraft:darkness");
        if (pass % 2 != 0) return;
        Fx.lendSkills(p, 100, Wave6Skills.MAGIC_SENSE, Wave6Skills.ALL_SEEING_EYE);          // "see the unseen": magic sense and the all-seeing eye while the light shines
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(sl, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.5, 0.6, 0.5, 0.01);
        int n = 0;
        double r = Wave6Tune.DAWN_REVEAL_RADIUS;
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> x != p && x.isAlive() && x.isInvisible() && !(x instanceof Player pl && pl.isSpectator()))) {
            if (n++ >= 24) break;
            Fx.addEffect(le, "minecraft:glowing", 60, 0);
        }
    }

    // ---------------------------------------------------------------- Zero Gravity
    private static void flightTick(ServerPlayer p, CompoundTag d) {
        if (p.isCreative()) return;
        boolean worn = RelicFlags.has(p, Wave6Tune.F_ASTRAL);
        if (!worn && d.getBoolean(Wave6Acts.K_FLY_ON)) d.putBoolean(Wave6Acts.K_FLY_ON, false);
        boolean want = worn && d.getBoolean(Wave6Acts.K_FLY_ON);
        Abilities ab = p.getAbilities();
        if (want) {
            if (!Wave2Mana.has(p, Wave6Tune.FLIGHT_MANA)) {                                   // 1.99.0, the hunger ban: the suit runs on magicules, not on food
                d.putBoolean(Wave6Acts.K_FLY_ON, false);
                Fx.say(p, "Your magicules run dry and gravity takes you back.");
                revokeFlight(p);
                return;
            }
            boolean changed = false;
            if (!ab.mayfly) { ab.mayfly = true; d.putBoolean(Wave6Acts.K_FLY_GRANTED, true); changed = true; }
            if (d.getBoolean(Wave6Acts.K_FLY_GRANTED) && !d.getBoolean(K_FLY_SPEED)) { ab.setFlyingSpeed(Wave6Tune.FLY_SPEED); d.putBoolean(K_FLY_SPEED, true); changed = true; }
            if (changed) p.onUpdateAbilities();
            if (ab.flying) Wave2Mana.take(p, Wave6Tune.FLIGHT_MANA);
        } else if (d.getBoolean(Wave6Acts.K_FLY_GRANTED) || d.getBoolean(K_FLY_SPEED)) {
            revokeFlight(p);
        }
    }

    /** Takes back only what the suit granted; a soft landing is thrown in so nobody falls out of the sky. */
    private static void revokeFlight(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        Abilities ab = p.getAbilities();
        boolean changed = false;
        if (d.getBoolean(Wave6Acts.K_FLY_GRANTED) && !p.isCreative()) { ab.mayfly = false; ab.flying = false; changed = true; }
        if (d.getBoolean(K_FLY_SPEED)) { ab.setFlyingSpeed(Wave6Tune.WALK_FLY_SPEED); changed = true; }
        d.remove(Wave6Acts.K_FLY_GRANTED);
        d.remove(K_FLY_SPEED);
        if (changed) {
            p.onUpdateAbilities();
            if (!p.onGround()) Wave6Kit.give(p, "minecraft:slow_falling", 200, 0, true);
        }
    }

    // ---------------------------------------------------------------- Black Veil
    private static void veilTick(ServerPlayer p) {
        if (!p.isShiftKeyDown() || !RelicFlags.has(p, Wave6Tune.F_SHADOW)) return;
        if (!Wave6Tune.isDim(p.level().getMaxLocalRawBrightness(p.blockPosition()))) return;
        Wave6Kit.give(p, "minecraft:invisibility", 40, 0, true);
        Wave6Kit.give(p, Wave6Skills.FX_PRESENCE_CONCEALMENT, 40, 0, true);                // and Tensura's presence concealment: mobs lose your trail
    }

    // ---------------------------------------------------------------- Armor of Plot: the ending
    private static void plotTick(ServerPlayer p, CompoundTag d, int pass) {
        boolean worn = RelicFlags.has(p, Wave6Tune.F_PLOT);
        boolean luck = d.getBoolean(K_PLOT_LUCK);
        if (!worn) {
            if (luck) { Wave6Kit.setMod(p, "minecraft:generic.luck", "plot_luck", Wave6Tune.PLOT_LUCK, AttributeModifier.Operation.ADD_VALUE, false); d.putBoolean(K_PLOT_LUCK, false); }
            return;
        }
        if (pass % 2 != 0) return;
        ItemStack s = Wave6Kit.find(p, Wave6Tune.ID_PLOT);
        if (s.isEmpty()) return;
        boolean spent = Wave6Kit.pool(s, Wave6Pools.PLOT, p).charges() <= 0;
        Wave6Kit.setMod(p, "minecraft:generic.luck", "plot_luck", Wave6Tune.PLOT_LUCK, AttributeModifier.Operation.ADD_VALUE, spent);
        if (spent != luck) d.putBoolean(K_PLOT_LUCK, spent);
    }

    // ---------------------------------------------------------------- Timelessness
    /** The slow-acting afflictions (poison, wither, hunger and Tensura's infections) lose half their remaining time every five seconds. */
    private static void timeless(ServerPlayer p) {
        shaving = true;
        try {
            for (MobEffectInstance m : new ArrayList<>(p.getActiveEffects())) {
                ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(m.getEffect().value());
                if (id == null || !TIME_EFFECTS.contains(id.toString()) || m.getDuration() <= 0) continue;
                int left = Wave6Tune.timeless(m.getDuration());
                p.removeEffect(m.getEffect());
                if (left > 0) p.addEffect(new MobEffectInstance(m.getEffect(), left, m.getAmplifier(), m.isAmbient(), m.isVisible(), m.showIcon()));
            }
        } finally {
            shaving = false;
        }
    }

    // ================================================================ Slime Form: no items, no spells
    private static boolean eatsOrDrinks(ItemStack s) {
        UseAnim a = s.getUseAnimation();
        return a == UseAnim.EAT || a == UseAnim.DRINK;
    }

    private static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof ServerPlayer p && slimed(p) && !eatsOrDrinks(e.getItem())) e.setCanceled(true);
    }

    private static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (e.getEntity() instanceof ServerPlayer p && slimed(p) && !eatsOrDrinks(e.getItemStack())) e.setCanceled(true);
    }

    private static void onAttack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && slimed(p)) e.setCanceled(true);
    }

    // ================================================================ ending states
    /** Death and logout end the temporary states (Slime Form, Dawnlight, flight; on death also Mage Armor). */
    public static void clearTemp(ServerPlayer p, boolean death) {
        CompoundTag d = p.getPersistentData();
        if (d.getLong(Wave6Acts.K_SLIME) > 0) endSlime(p, false);
        d.putBoolean(Wave6Acts.K_DAWN_ON, false);
        revokeFlight(p);
        if (death) {
            d.putBoolean(Wave6Acts.K_FLY_ON, false);
            d.putLong(Wave6Acts.K_MAGE_UNTIL, 0);
            mageMods(p, false);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) clearTemp(p, false);
    }

    /** A new world ends Slime Form; and the Suit of the Astral Overseer's "Traveler of the Universe" lifts the sickness of the crossing from the wearer and nearby allies. */
    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.getPersistentData().getLong(Wave6Acts.K_SLIME) > 0) endSlime(p, false);
        if (!RelicFlags.has(p, Wave6Tune.F_ASTRAL)) return;
        for (String id : TRAVEL_SICKNESS) Wave6Kit.lift(p, id);
        Wave6Kit.give(p, "minecraft:regeneration", 100, 0, false);
        Wave6Kit.give(p, "minecraft:resistance", 200, 1, false);
        Fx.lendSkills(p, 600, Wave6Skills.SPATIAL_ATTACK_RESISTANCE, Wave6Skills.ABNORMAL_CONDITION_RESISTANCE);
        int n = 0;
        for (LivingEntity a : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8), x -> x != p && x.isAlive() && Wave6Kit.isAlly(p, x))) {
            if (n++ >= 8) break;
            for (String id : TRAVEL_SICKNESS) Wave6Kit.lift(a, id);
        }
        Fx.say(p, "Traveler of the Universe: the crossing leaves no mark on you or yours.");
    }

    private static void onServerTick(ServerTickEvent.Post e) { Wave6Zones.tick(e.getServer()); }

    private static void onStopping(ServerStoppingEvent e) {
        Wave6Zones.clear();
        shaving = false;
    }
}
