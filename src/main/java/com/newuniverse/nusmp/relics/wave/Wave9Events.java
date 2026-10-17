package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wave 9: the game-bus rules of the ten weapons: bonuses on a blow, what a weapon does to the one it strikes, the frost patches, the rebelling
 * gladiators, the Exile's curse, Hunger's gnawing and the Viper's compass. Everything per tick is staggered (a held-weapon check every fourth
 * tick, the rest every 20 or 40) and bounded (twelve frost patches, a handful of gladiators, eight creatures per patch).
 */
final class Wave9Events {
    private Wave9Events() {}

    // ---------------------------------------------------------------- persistent-data keys (player or arrow or victim)
    static final String K_ARMED = "nusmp_w9_frost_armed", K_FROST = "nusmp_w9_frost_bolt", K_SURFACE = "nusmp_w9_surface_bolt";
    static final String K_DEFLECT = "nusmp_w9_deflect", K_CHARGE = "nusmp_w9_charge", K_AGATHYS = "nusmp_w9_agathys", K_EYE = "nusmp_w9_eye_at";
    static final String K_FED = "nusmp_w9_fed", K_GNAW = "nusmp_w9_gnaw", K_DEVOUR = "nusmp_w9_devour";
    static final String K_LAST_UUID = "nusmp_w9_last", K_LAST_AT = "nusmp_w9_last_at";
    static final String K_MARK_UUID = "nusmp_w9_mark", K_MARK_UNTIL = "nusmp_w9_mark_until", K_MARK_X = "nusmp_w9_mx", K_MARK_Y = "nusmp_w9_my",
            K_MARK_Z = "nusmp_w9_mz", K_MARK_DIM = "nusmp_w9_mdim";
    static final String K_DRAIN_UNTIL = "nusmp_w9_drain_until", K_RESTRAIN = "nusmp_w9_restrain", K_EXILE = "nusmp_w9_exile_told";
    /** On a hostile gladiator: the game time at which it fades; a gladiator that loads after that time is removed (it was unloaded when its time ran out). */
    static final String K_ROGUE = "nusmp_w9_rogue_until", K_REFLECTED = "nusmp_w9_reflected";

    private static final ResourceLocation DRAIN_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w9_null_drain");
    private static final ResourceLocation EXILE_SPEED = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w9_exile_stride");
    private static final String OWNER_TAG = "nusmp_relic_owner";

    /** True while one of these weapons deals its own ability damage, so its on-hit bonuses do not fire on it again. */
    static boolean busy;

    // ---------------------------------------------------------------- state held for a while
    private record Patch(ResourceKey<Level> dim, Vec3 at, long until, UUID owner) {}

    private static final List<Patch> PATCHES = new ArrayList<>();

    /** The gladiators of one player that will turn on them at {@code at}. */
    private static final class Rebel {
        final List<UUID> gladiators;
        final long at;
        boolean warned;

        Rebel(List<UUID> gladiators, long at) {
            this.gladiators = gladiators;
            this.at = at;
        }
    }

    private static final Map<UUID, Rebel> REBELS = new HashMap<>();
    private static final Map<UUID, Long> ROGUES = new HashMap<>();
    /** Mobs whose maximum health the Null Blade has lowered, with the game time at which it comes back (players are handled on their own tick). At most 200. */
    private static final Map<UUID, Long> DRAINED = new HashMap<>();

    static void markRebels(UUID player, List<UUID> gladiators, long at) { REBELS.put(player, new Rebel(new ArrayList<>(gladiators), at)); }

    static void clear() {
        PATCHES.clear();
        REBELS.clear();
        ROGUES.clear();
        DRAINED.clear();
    }

    // ---------------------------------------------------------------- small helpers
    /** The relic id in the main hand if it works for this player (attuned when it needs that), else "". */
    static String held(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        if (s.isEmpty()) return "";
        RelicDef d = Relics.defOf(s);
        return d != null && Attune.works(s, d, p) ? d.pairBaseId() : "";      // 1.29.0: either half of a pair counts as the pair
    }

    static boolean awakened(ItemStack stack) { return Wave9Rules.awakened(Wave9Store.get(stack, "kills")); }

    /** The foe this player struck last, if it was within the window (ticks) and still lives. */
    static LivingEntity lastHit(ServerPlayer p, int window) {
        CompoundTag d = p.getPersistentData();
        if (!d.hasUUID(K_LAST_UUID) || p.level().getGameTime() - d.getLong(K_LAST_AT) > window) return null;
        Entity e = p.serverLevel().getEntity(d.getUUID(K_LAST_UUID));
        return e instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    private static void remember(ServerPlayer p, LivingEntity victim) {
        CompoundTag d = p.getPersistentData();
        d.putUUID(K_LAST_UUID, victim.getUUID());
        d.putLong(K_LAST_AT, p.level().getGameTime());
    }

    private static String pathOf(DamageSource src) { return src.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse(""); }

    private static boolean isCold(DamageSource src) {
        String s = pathOf(src);
        return src.is(DamageTypeTags.IS_FREEZING) || s.contains("frost") || s.contains("cold") || s.contains("ice") || s.contains("freez");
    }

    private static boolean isNecrotic(DamageSource src) {
        String s = pathOf(src);
        return s.contains("wither") || s.contains("darkness") || s.contains("necro");
    }

    private static Entity findAny(MinecraftServer server, UUID id) {
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    // ================================================================ arrows: Hyperborean Crossbow
    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (e.getEntity() instanceof Mob rogue && rogue.getPersistentData().contains(K_ROGUE) && rogue.getPersistentData().getLong(K_ROGUE) <= e.getLevel().getGameTime()) {
            e.setCanceled(true);                                                               // a hostile gladiator whose three minutes ran out while it was unloaded
            return;
        }
        if (!(e.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer sp) || !held(sp).equals("hyperborean_crossbow")) return;
        CompoundTag t = arrow.getPersistentData();
        if (t.getBoolean(K_FROST)) return;
        t.putBoolean(K_FROST, true);
        arrow.setBaseDamage(arrow.getBaseDamage() + 1.0);                                      // "+1 to attack and damage rolls"
        CompoundTag d = sp.getPersistentData();
        long now = sp.level().getGameTime();
        if (d.getLong(K_ARMED) >= now) {                                                      // "you can choose to shoot at a surface"
            d.remove(K_ARMED);
            t.putBoolean(K_SURFACE, true);
        }
    }

    static void onImpact(ProjectileImpactEvent e) {
        Projectile proj = e.getProjectile();
        if (proj.level().isClientSide()) return;
        HitResult hit = e.getRayTraceResult();
        // Twin Swords: a parrying player turns a missile back on the one who sent it
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof ServerPlayer vp && vp.getPersistentData().getLong(K_DEFLECT) >= vp.level().getGameTime()
                && held(vp).equals("twin_swords_of_deflection")
                && (proj.getOwner() != vp || proj.getPersistentData().getInt(K_REFLECTED) > 0)) {
            e.setCanceled(true);                                                               // a missile already turned back is never let through to the parrier either
            int turned = proj.getPersistentData().getInt(K_REFLECTED);
            if (turned >= 2) {                                                                 // turned back twice already (two wielders trading it): it falls
                proj.discard();
                return;
            }
            proj.getPersistentData().putInt(K_REFLECTED, turned + 1);
            proj.setOwner(vp);
            proj.setDeltaMovement(proj.getDeltaMovement().scale(-0.8));
            proj.hurtMarked = true;
            RukhFx.spray(((ServerLevel) vp.level()), RukhFx.Alignment.LIGHT, proj.getX(), proj.getY(), proj.getZ(), 8, 0.2, 0.2, 0.2, 0.2);
            return;
        }
        if (!proj.getPersistentData().getBoolean(K_SURFACE) || !(proj.getOwner() instanceof ServerPlayer owner)) return;
        proj.getPersistentData().remove(K_SURFACE);
        freezeAt((ServerLevel) proj.level(), owner, hit.getLocation());
    }

    /** Flash Freeze on a surface: water in reach turns to frosted ice, and for a minute the circle slows whatever crosses it. */
    private static void freezeAt(ServerLevel level, ServerPlayer owner, Vec3 at) {
        int mine = 0;
        for (Patch pt : PATCHES) if (pt.owner().equals(owner.getUUID())) mine++;
        for (Iterator<Patch> it = PATCHES.iterator(); mine >= 3 && it.hasNext(); ) {              // three patches each: the oldest of the shooter's own goes first
            if (it.next().owner().equals(owner.getUUID())) {
                it.remove();
                mine--;
            }
        }
        while (!PATCHES.isEmpty() && PATCHES.size() >= Wave9Rules.FROST_PATCH_CAP) PATCHES.remove(0);
        PATCHES.add(new Patch(level.dimension(), at, level.getGameTime() + Wave9Rules.FROST_PATCH_TICKS, owner.getUUID()));
        Fx.ring(level, at, Wave9Rules.FROST_PATCH_RADIUS, 0x9FE8FF);
        RukhFx.spray(level, RukhFx.Alignment.LIGHT, at.x, at.y + 0.3, at.z, 30, 1.2, 0.3, 1.2, 0.02);
        if (Labyrinth.in(level)) return;                                                   // the Dungeon's blocks are not for turning into ice
        int r = (int) Math.ceil(Wave9Rules.FROST_PATCH_RADIUS);
        BlockPos c = BlockPos.containing(at);
        int changed = 0;
        for (int dx = -r; dx <= r && changed < 14; dx++) {
            for (int dz = -r; dz <= r && changed < 14; dz++) {
                if (dx * dx + dz * dz > Wave9Rules.FROST_PATCH_RADIUS * Wave9Rules.FROST_PATCH_RADIUS) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos bp = c.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(bp);
                    if (st.is(Blocks.WATER) && level.getFluidState(bp).isSource() && level.getBlockState(bp.above()).isAir() && level.mayInteract(owner, bp)) {
                        level.setBlockAndUpdate(bp, Blocks.FROSTED_ICE.defaultBlockState());
                        changed++;
                        break;
                    }
                }
            }
        }
    }

    // ================================================================ damage
    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide() || busy || com.newuniverse.nusmp.relics.pair.PairHooks.striking()) return;
        DamageSource src = e.getSource();
        long now = victim.level().getGameTime();
        Entity direct = src.getDirectEntity();

        // Flash Freeze: a bolt from the Hyperborean Crossbow
        if (direct != null && direct.getPersistentData().getBoolean(K_FROST)) {
            int ticks = Wave9Rules.controlTicks(Wave9Rules.FROST_SLOW_TICKS, victim.getMaxHealth());
            Fx.addEffect(victim, "minecraft:slowness", ticks, Wave9Rules.FROST_SLOW_AMP);       // "movement speed is halved"
            Fx.addEffect(victim, "tensura:frost", ticks, 0);
            victim.setTicksFrozen(Math.min(victim.getTicksRequiredToFreeze() + 40, victim.getTicksFrozen() + 60));
            RukhFx.spray(((ServerLevel) victim.level()), RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 12, 0.3, 0.4, 0.3, 0.03);
        }

        if (victim instanceof ServerPlayer vp) incomingOnWielder(e, vp, src, now);

        if (!(src.getEntity() instanceof ServerPlayer atk)) return;
        String id = held(atk);
        if (id.isEmpty()) return;
        String path = pathOf(src);
        if (id.equals("hyperborean_crossbow") && com.newuniverse.nusmp.relics.wave.Wave1Rules.isSpellPath(path)) {
            e.setAmount(e.getAmount() * 1.10f);                                                // "this crossbow is also a spellcasting focus"
            return;
        }
        boolean melee = direct == atk && !src.is(DamageTypeTags.IS_PROJECTILE);
        if (!melee) return;
        ServerLevel level = (ServerLevel) victim.level();
        switch (id) {
            case "astralaria" -> {                                                              // 3d6 radiant, as a flat blow of light
                e.setAmount(e.getAmount() + 4.5f);
                RukhFx.spray(level, RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 14, 0.3, 0.5, 0.3, 0.05);
                RukhFx.spray(level, RukhFx.Alignment.DARK, atk.getX(), atk.getY() + 1.2, atk.getZ(), 10, 0.5, 0.5, 0.5, 0.5);
            }
            case "null_blade" -> nullStrike(e, atk, victim, level, now);
            case "viper_whip" -> {                                                              // 1d4 + 1d6 poison
                e.setAmount(e.getAmount() + 2.5f);
                Fx.addEffect(victim, "minecraft:poison", 100, 0);
                remember(atk, victim);
            }
            case "twin_swords_of_deflection" -> {                                              // both blades land (the off-hand blow is the second blade now); a crit on 19-20
                float amount = e.getAmount();
                if (Wave9Rules.twinCrit(atk.getRandom().nextDouble())) {
                    amount *= 1.5f;
                    RukhFx.spray(level, RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 14, 0.3, 0.4, 0.3, 0.2);
                }
                e.setAmount(amount);
            }
            case "hunger" -> hungerStrike(e, atk, victim, level, now);
            case "thornblight" -> {
                remember(atk, victim);
                price(atk, victim);
            }
            default -> { }
        }
    }

    /** The curse of Thornblight (and of the thrown axe): every blow that lands costs the wielder one point, but never the last two hearts. */
    static void price(ServerPlayer atk, LivingEntity victim) {
        if (!atk.isCreative() && Wave9Rules.thornPrice(atk.getHealth())) atk.hurt(atk.damageSources().thorns(victim), 1.0f);
    }

    private static void incomingOnWielder(LivingIncomingDamageEvent e, ServerPlayer vp, DamageSource src, long now) {
        CompoundTag d = vp.getPersistentData();
        // Twin Swords: the parry
        if (d.getLong(K_DEFLECT) >= now && held(vp).equals("twin_swords_of_deflection")) {
            if (src.is(DamageTypeTags.IS_PROJECTILE)) {
                e.setCanceled(true);
                return;
            }
            e.setAmount(e.getAmount() * 0.4f);
            if (src.getEntity() instanceof LivingEntity foe && foe != vp && foe.distanceToSqr(vp) < 25.0) {
                RukhFx.spray(((ServerLevel) vp.level()), RukhFx.Alignment.LIGHT, vp.getX(), vp.getY() + 1.1, vp.getZ(), 1, 0, 0, 0, 0);
                Wave9Acts.quiet(() -> Fx.damage(vp, foe, 2.5f, "minecraft:player_attack"));      // the riposte
            }
        }
        // Armor of Agathys: whatever strikes you in melee takes cold
        if (d.getLong(K_AGATHYS) >= now && src.getEntity() instanceof LivingEntity foe && foe != vp && src.getDirectEntity() == foe) {
            Wave9Acts.quiet(() -> {
                Fx.damage(vp, foe, 3f, "minecraft:freeze");
                Fx.addEffect(foe, "tensura:frost", 60, 0);
            });
        }
        String id = held(vp);
        if (!id.equals("null_blade")) return;
        ItemStack blade = vp.getMainHandItem();
        // the blade drinks hostile magic: a charge, up to three
        if (src.getEntity() != null && src.getEntity() != vp && com.newuniverse.nusmp.relics.wave.Wave1Rules.isSpellPath(pathOf(src))) {
            int c = d.getInt(K_CHARGE);
            if (c < Wave9Rules.MAX_CHARGES) {
                d.putInt(K_CHARGE, c + 1);
                vp.displayClientMessage(Component.literal("Null Blade: charge " + (c + 1) + "/" + Wave9Rules.MAX_CHARGES), true);
            }
        }
        if (awakened(blade) && (isCold(src) || isNecrotic(src))) e.setAmount(e.getAmount() * 0.5f);   // "resistance to cold and necrotic damage"
        // the eye in the cross: it locks onto what strikes you from within five blocks
        if (src.getEntity() instanceof LivingEntity foe && foe != vp && foe.distanceToSqr(vp) <= Wave9Rules.EYE_RANGE * Wave9Rules.EYE_RANGE && foe.getBoundingBox().getSize() > 0) {
            CompoundTag ft = foe.getPersistentData();
            if (ft.getLong(K_RESTRAIN) >= now) {                                               // struck again while restrained: frozen
                ft.remove(K_RESTRAIN);
                int ticks = Wave9Rules.controlTicks(Wave9Rules.FROZEN_TICKS, foe.getMaxHealth());
                Fx.addEffect(foe, "minecraft:slowness", ticks, 6);
                Fx.addEffect(foe, "tensura:paralysis", ticks / 2, 0);
                Fx.addEffect(foe, "tensura:frost", ticks, 1);
                RukhFx.spray(((ServerLevel) foe.level()), RukhFx.Alignment.LIGHT, foe.getX(), foe.getY() + 1.0, foe.getZ(), 30, 0.4, 0.8, 0.4, 0.04);
            } else if (d.getLong(K_EYE) <= now) {
                d.putLong(K_EYE, now + Wave9Rules.EYE_COOLDOWN);
                int ticks = Wave9Rules.controlTicks(Wave9Rules.RESTRAIN_TICKS, foe.getMaxHealth());
                ft.putLong(K_RESTRAIN, now + Wave9Rules.FREEZE_WINDOW);
                Fx.addEffect(foe, "minecraft:slowness", ticks, 4);
                Fx.addEffect(foe, "tensura:frost", ticks, 0);
                RukhFx.spray(((ServerLevel) foe.level()), RukhFx.Alignment.LIGHT, foe.getX(), foe.getY() + 1.0, foe.getZ(), 16, 0.3, 0.6, 0.3, 0.03);
            }
        }
    }

    private static void nullStrike(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, ServerLevel level, long now) {
        e.setAmount(e.getAmount() + 3.0f);                                                     // the extra d6 of cold; the blade deals cold in place of slashing
        Fx.addEffect(victim, "tensura:frost", 60, 0);
        RukhFx.spray(level, RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.3, 0.5, 0.3, 0.03);
        AttributeInstance mh = victim.getAttribute(Attributes.MAX_HEALTH);
        if (mh == null) return;
        boolean player = victim instanceof Player;
        CompoundTag vt = victim.getPersistentData();
        AttributeModifier old = mh.getModifier(DRAIN_ID);
        double already = old == null || vt.getLong(K_DRAIN_UNTIL) < now ? 0.0 : -old.amount();
        double take = Wave9Rules.drain(already, victim.getMaxHealth() + already, player, 1 + atk.getRandom().nextInt(6));
        if (take <= 0.0) return;
        mh.removeModifier(DRAIN_ID);
        mh.addTransientModifier(new AttributeModifier(DRAIN_ID, -(already + take), AttributeModifier.Operation.ADD_VALUE));
        long until = now + Wave9Rules.drainTicks(player);
        vt.putLong(K_DRAIN_UNTIL, until);
        if (!player) trackDrain(level.getServer(), victim.getUUID(), until);
        double abs = atk.getAbsorptionAmount();
        double gain = Wave9Rules.tempFromDrain(take, abs);
        if (gain > 0.0) tempHp(atk, abs + gain);                                               // temporary hit points equal to what was taken
    }

    /** Temporary hit points that fade: the absorption effect carries them for a minute (and takes them with it), then the exact amount is set. */
    private static void tempHp(ServerPlayer p, double total) {
        float t = (float) Math.max(0.0, total);
        Fx.shield(p, t, 1200);
        p.setAbsorptionAmount(t);
    }

    private static void trackDrain(MinecraftServer server, UUID victim, long until) {
        if (server == null) return;
        if (DRAINED.size() >= 200 && !DRAINED.containsKey(victim)) {                           // the cap: the oldest entry gets its health back at once
            UUID first = DRAINED.keySet().iterator().next();
            releaseDrain(server, first);
            DRAINED.remove(first);
        }
        DRAINED.put(victim, until);
    }

    private static void releaseDrain(MinecraftServer server, UUID id) {
        if (findAny(server, id) instanceof LivingEntity le) {
            AttributeInstance mh = le.getAttribute(Attributes.MAX_HEALTH);
            if (mh != null) mh.removeModifier(DRAIN_ID);
        }
    }

    /** Gives the drained mobs their maximum health back when the time is up (a mob that is gone needs nothing: the modifier is not saved). */
    private static void tickDrained(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> it = DRAINED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> en = it.next();
            Entity x = findAny(server, en.getKey());
            if (!(x instanceof LivingEntity le) || !le.isAlive()) {
                it.remove();
                continue;
            }
            if (le.level().getGameTime() >= en.getValue()) {
                AttributeInstance mh = le.getAttribute(Attributes.MAX_HEALTH);
                if (mh != null) mh.removeModifier(DRAIN_ID);
                it.remove();
            }
        }
    }

    private static void hungerStrike(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, ServerLevel level, long now) {
        e.setAmount(e.getAmount() + 4.0f);                                                     // 1d12 acid
        Fx.addEffect(victim, "tensura:corrosion", 80, 0);
        CompoundTag d = atk.getPersistentData();
        d.putLong(K_FED, now);
        RukhFx.spray(level, RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
        if (d.getLong(K_DEVOUR) >= now) {                                                      // Devouring: the blood becomes temporary health
            d.remove(K_DEVOUR);
            double gain = Wave9Rules.devourTemp(e.getAmount(), victim instanceof Player);
            if (gain > 0.0) tempHp(atk, Math.min(atk.getAbsorptionAmount() + gain, 16.0));
            RukhFx.spray(level, RukhFx.Alignment.DARK, atk.getX(), atk.getY() + 1.4, atk.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
    }

    // ================================================================ deaths: the Null Blade counts its lives
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide() || !(e.getSource().getEntity() instanceof ServerPlayer p) || e.getEntity() == p) return;
        String id = held(p);
        if (id.equals("hunger")) p.getPersistentData().putLong(K_FED, p.level().getGameTime());
        if (!id.equals("null_blade")) return;
        ItemStack blade = p.getMainHandItem();
        int before = Wave9Store.get(blade, "kills");
        Wave9Store.put(blade, "kills", before + 1);
        if (!Wave9Rules.awakened(before) && Wave9Rules.awakened(before + 1)) {
            Fx.say(p, "Twenty lives taken. The Null Blade awakens: cold and necrotic harm is halved, and Armor of Agathys answers your call.");
            Fx.burst((ServerLevel) p.level(), p.position(), 0x9FE8FF);
        }
    }

    // ================================================================ the held-weapon tick
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 4 != 1) return;
        long now = p.level().getGameTime();
        CompoundTag d = p.getPersistentData();
        if (p.tickCount % 40 == 1) {
            tickMark(p, d, now);
            if (p.tickCount % 80 == 1) dropDrainOnPlayer(p, now);
        }
        ItemStack stack = p.getMainHandItem();
        String id = held(p);
        boolean exile = id.equals("axe_of_the_exile");
        if (!exile && p.tickCount % 20 == 1) {                                                // the offhand carries the curse too
            ItemStack off = p.getOffhandItem();
            RelicDef od = off.isEmpty() ? null : Relics.defOf(off);
            exile = od != null && od.id().equals("axe_of_the_exile");
        }
        exileStride(p, exile);
        if (exile && p.tickCount % Wave9Rules.EXILE_EVERY == 1) exileCurse(p, d);
        if (id.isEmpty()) {
            if (!stack.isEmpty() && p.tickCount % 20 == 1) strayNullBlade(p, stack);
            return;
        }
        switch (id) {
            case "astralaria" -> {
                if (RelicEngine.isOn(p, "astralaria", "footprints") && p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.002) {
                    ServerLevel l = (ServerLevel) p.level();
                    RukhFx.spray(l, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 0.05, p.getZ(), 1, 0.18, 0.0, 0.18, 0.0);
                    RukhFx.spray(l, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 0.08, p.getZ(), 3, 0.2, 0.0, 0.2, 0.0);
                }
                if (p.tickCount % 20 == 1) revealInvisible(p);
            }
            case "null_blade" -> {
                if (p.tickCount % 20 == 1) dread(p);
            }
            case "hunger" -> {
                if (p.tickCount % 20 == 1) {
                    if (!d.contains(K_FED)) d.putLong(K_FED, now);                              // a newly drawn Hunger starts fed
                    double hurt = Wave9Rules.gnaw(now, d.getLong(K_FED), d.getLong(K_GNAW), p.getHealth());
                    if (hurt > 0.0 && !p.isCreative()) {
                        d.putLong(K_GNAW, now);
                        p.hurt(p.damageSources().magic(), (float) hurt);
                        p.causeFoodExhaustion(1.0f);
                        p.displayClientMessage(Component.literal("Hunger gnaws at you. Feed it.").withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
                    }
                }
            }
            default -> { }
        }
    }

    /** Truesight: anything invisible within 16 blocks is shown with a glow (at most 10 creatures). */
    private static void revealInvisible(ServerPlayer p) {
        List<LivingEntity> seen = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16), x -> x != p && x.isInvisible() && x.isAlive());
        int n = 0;
        for (LivingEntity x : seen) {
            if (n++ >= 10) break;
            Fx.addEffect(x, "minecraft:glowing", 60, 0);
        }
    }

    /** "Simply being within eyesight of this foul instrument creates a sense of dread": lesser creatures within five blocks (up to eight) flinch with fear. */
    private static void dread(ServerPlayer p) {
        int n = 0;
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(5), x -> x.isAlive() && x.getMaxHealth() < Wave9Rules.BOSS_HEALTH
                && !x.getPersistentData().hasUUID(OWNER_TAG) && !(x instanceof OwnableEntity oe && p.getUUID().equals(oe.getOwnerUUID())))) {
            if (n++ >= 8) break;
            Fx.addEffect(m, "tensura:fear", 40, 0);
        }
    }

    /** The Null Blade "calls to those who follow the path of a warlock": whoever holds it without being its attuned owner drops it, and takes cold for it. */
    private static void strayNullBlade(ServerPlayer p, ItemStack stack) {
        RelicDef d = Relics.defOf(stack);
        if (d == null || !d.id().equals("null_blade") || p.isCreative()) return;
        java.util.UUID owner = Attune.owner(stack);
        if (owner == null || owner.equals(p.getUUID())) return;                                 // unbound: the attunement ritual may begin
        p.drop(stack.copy(), true);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        float cold = Math.min(3.0f, p.getHealth() - 4.0f);                                      // never lethal: it leaves the last two hearts alone
        if (cold > 0.0f) p.hurt(p.damageSources().freeze(), cold);
        p.displayClientMessage(Component.literal("The Null Blade turns in your hand: it is not yours to hold.").withStyle(net.minecraft.ChatFormatting.AQUA), true);
    }

    private static void dropDrainOnPlayer(ServerPlayer p, long now) {
        AttributeInstance mh = p.getAttribute(Attributes.MAX_HEALTH);
        if (mh != null && mh.hasModifier(DRAIN_ID) && p.getPersistentData().getLong(K_DRAIN_UNTIL) < now) mh.removeModifier(DRAIN_ID);
    }

    // ---------------------------------------------------------------- Axe of the Exile
    private static BlockPos home(ServerPlayer p) {
        BlockPos spawn = p.getRespawnPosition();
        return spawn != null ? spawn : p.getServer().overworld().getSharedSpawnPos();
    }

    private static ResourceKey<Level> homeDim(ServerPlayer p) {
        return p.getRespawnPosition() != null ? p.getRespawnDimension() : Level.OVERWORLD;
    }

    private static boolean inExile(ServerPlayer p) {
        if (p.level().dimension() != homeDim(p)) return false;
        BlockPos h = home(p);
        return Wave9Rules.inExile(p.getX() - h.getX(), p.getZ() - h.getZ());
    }

    private static void exileCurse(ServerPlayer p, CompoundTag d) {
        if (p.isCreative() || !inExile(p)) return;
        p.causeFoodExhaustion(Wave9Rules.exileExhaustion());                                   // "for every hour within the forbidden place, one point of exhaustion"
        if (!d.getBoolean(K_EXILE)) {
            d.putBoolean(K_EXILE, true);
            Fx.say(p, "The Exile's curse: within " + (int) Wave9Rules.HOME_RADIUS + " blocks of your bed (or the world spawn) this axe wears you out. Put it away, or walk far from home.");
        }
    }

    /** Far from home the axe lends its bearer a lighter step: +10% speed while held beyond the circle. */
    private static void exileStride(ServerPlayer p, boolean holding) {
        if (p.tickCount % 20 != 1) return;
        AttributeInstance sp = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp == null) return;
        boolean want = holding && !inExile(p);
        boolean has = sp.hasModifier(EXILE_SPEED);
        if (want && !has) sp.addTransientModifier(new AttributeModifier(EXILE_SPEED, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        else if (!want && has) sp.removeModifier(EXILE_SPEED);
    }

    // ---------------------------------------------------------------- Viper Whip: the compass
    private static void tickMark(ServerPlayer p, CompoundTag d, long now) {
        if (!d.contains(K_MARK_UNTIL)) return;
        if (now > d.getLong(K_MARK_UNTIL) || !d.hasUUID(K_MARK_UUID)) {
            d.remove(K_MARK_UNTIL);
            return;
        }
        Entity t = findAny(p.getServer(), d.getUUID(K_MARK_UUID));
        boolean seen = t instanceof LivingEntity;
        double x = d.getDouble(K_MARK_X), y = d.getDouble(K_MARK_Y), z = d.getDouble(K_MARK_Z);
        String dim = d.getString(K_MARK_DIM);
        if (seen) {
            x = t.getX();
            y = t.getY();
            z = t.getZ();
            dim = t.level().dimension().location().toString();
            d.putDouble(K_MARK_X, x);
            d.putDouble(K_MARK_Y, y);
            d.putDouble(K_MARK_Z, z);
            d.putString(K_MARK_DIM, dim);
        }
        boolean dead = seen && !t.isAlive();
        String state = dead ? "fallen" : seen ? "alive" : "last known";
        String text;
        if (!dim.equals(p.level().dimension().location().toString())) {
            text = "Viper bite: in another world (" + dim + "), " + state;
        } else {
            double dx = x - p.getX(), dz = z - p.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz + (y - p.getY()) * (y - p.getY()));
            text = dist > Wave9Rules.MARK_RANGE ? "Viper bite: beyond a thousand metres" : "Viper bite: " + Wave9Rules.compass(dx, dz) + ", " + (int) dist + " m, " + state;
        }
        p.displayClientMessage(Component.literal(text).withStyle(net.minecraft.ChatFormatting.GREEN), true);
        if (dead) d.remove(K_MARK_UNTIL);
    }

    // ================================================================ server tick: frost patches, gladiators
    static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int tc = server.getTickCount();
        if (tc % 10 == 4 && !PATCHES.isEmpty()) tickPatches(server);
        if (tc % 40 == 7 && !DRAINED.isEmpty()) tickDrained(server);
        if (tc % 20 == 9 && (!REBELS.isEmpty() || !ROGUES.isEmpty())) {
            tickRebels(server);
            tickRogues(server);
        }
    }

    private static void tickPatches(MinecraftServer server) {
        Iterator<Patch> it = PATCHES.iterator();
        while (it.hasNext()) {
            Patch pt = it.next();
            ServerLevel l = server.getLevel(pt.dim());
            if (l == null || l.getGameTime() >= pt.until()) {
                it.remove();
                continue;
            }
            double r = Wave9Rules.FROST_PATCH_RADIUS;
            RukhFx.spray(l, RukhFx.Alignment.LIGHT, pt.at().x, pt.at().y + 0.2, pt.at().z, 5, r * 0.5, 0.1, r * 0.5, 0.01);
            AABB box = new AABB(pt.at().x - r, pt.at().y - 1.0, pt.at().z - r, pt.at().x + r, pt.at().y + 2.0, pt.at().z + r);
            int n = 0;
            for (LivingEntity x : l.getEntitiesOfClass(LivingEntity.class, box, v -> v.isAlive() && !v.isSpectator() && !v.getUUID().equals(pt.owner())
                    && !(v instanceof Player pl && pl.isCreative()) && !v.getPersistentData().hasUUID(OWNER_TAG)
                    && !(v instanceof OwnableEntity oe && pt.owner().equals(oe.getOwnerUUID())))) {
                if (n++ >= 8) break;
                if (x.position().distanceToSqr(pt.at().x, x.getY(), pt.at().z) > r * r) continue;
                Fx.addEffect(x, "minecraft:slowness", 30, 1);
                Fx.addEffect(x, "tensura:frost", 30, 0);
            }
        }
    }

    /** Caesar's Gladius: ten calls in a hundred, the gladiators turn: a warning five seconds before, then three hostile gladiators for three minutes. */
    private static void tickRebels(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Rebel>> it = REBELS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Rebel> en = it.next();
            Rebel r = en.getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(en.getKey());
            if (owner == null || !owner.isAlive()) {
                it.remove();
                continue;
            }
            long now = owner.level().getGameTime();
            if (!r.warned && now >= r.at - 100) {
                r.warned = true;
                Fx.say(owner, "The gladiators mutter among themselves: \"The Republic must be saved...\"");
            }
            if (now < r.at) continue;
            it.remove();
            for (UUID g : r.gladiators) {
                Entity ge = findAny(server, g);
                if (ge != null) ge.discard();
            }
            turnRogue(owner, now);
        }
    }

    private static void turnRogue(ServerPlayer owner, long now) {
        ServerLevel l = owner.serverLevel();
        Fx.say(owner, "\"Sic semper tyrannis!\" The gladiators turn on you. They fade in three minutes, or when they fall.");
        for (int i = 0; i < Wave9Rules.GLADIATORS; i++) {
            double a = owner.getRandom().nextDouble() * Math.PI * 2;
            Vindicator v = EntityType.VINDICATOR.create(l);
            if (v == null) continue;
            v.moveTo(owner.getX() + Math.cos(a) * 3.0, owner.getY(), owner.getZ() + Math.sin(a) * 3.0, owner.getYRot() + 180f, 0f);
            v.finalizeSpawn(l, l.getCurrentDifficultyAt(owner.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            v.setCustomName(Component.literal("Gladiator of the Republic"));
            v.getPersistentData().putLong(K_ROGUE, now + Wave9Rules.ROGUE_LIFE);                  // not persistent: if it is left behind it despawns like any raider
            v.setTarget(owner);
            if (l.addFreshEntity(v)) ROGUES.put(v.getUUID(), now + Wave9Rules.ROGUE_LIFE);
            Fx.burst(l, v.position(), 0x8B2E2E);
        }
    }

    private static void tickRogues(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> it = ROGUES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> en = it.next();
            Entity v = findAny(server, en.getKey());
            if (!(v instanceof Mob m) || !m.isAlive()) {
                it.remove();
                continue;
            }
            if (m.level().getGameTime() >= en.getValue()) {
                Fx.burst((ServerLevel) m.level(), m.position(), 0x8888AA);
                m.discard();
                it.remove();
            }
        }
    }

    static void onStopping(ServerStoppingEvent e) { clear(); }
}
