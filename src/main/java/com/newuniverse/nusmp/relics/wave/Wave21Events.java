package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;

/**
 * Why: wave 21: the game-bus listeners of the twelve weapons. Every listener starts with a cheap test (who is hit, which flag the attacker carries) and does
 * its real work only for the wielders; the player tick is staggered by entity id and the timers (whirlwind, climb, stop, headless) are keys in the
 * player's persistent data. The one thing that lives between ticks is the Escape Rift circle (at most eight, gone after six seconds, cleared on stop).
 */
final class Wave21Events {
    private Wave21Events() {}

    private static boolean hooked;
    private static final int MAX_ZONES = 8;
    private static final List<Zone> ZONES = new ArrayList<>();
    private static final ResourceLocation WITHER_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "w21_horseman_wither");
    private static final String K_WITHER_HITS = "nusmp_w21_wither_hits", K_WITHER_UNTIL = "nusmp_w21_wither_until", K_SEASON_AT = "nusmp_w21_season_at";

    /** An open Escape Rift: the level it is in, its centre, its end. */
    record Zone(net.minecraft.resources.ResourceKey<Level> dim, Vec3 centre, long until) {}

    static void register() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave21Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave21Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave21Events::onPost);
        NeoForge.EVENT_BUS.addListener(Wave21Events::onCrit);
        NeoForge.EVENT_BUS.addListener(Wave21Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave21Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave21Events::onStopping);
    }

    private static boolean has(LivingEntity e, String flag) { return RelicFlags.has(e, flag); }

    private static String heldId(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        return s.isEmpty() ? "" : Relics.of(s).map(RelicDef::id).orElse("");
    }

    // ================================================================ the Escape Rift
    static boolean openZone(ServerPlayer p, Vec3 centre) {
        synchronized (ZONES) {
            if (ZONES.size() >= MAX_ZONES) return false;
            ZONES.add(new Zone(p.serverLevel().dimension(), centre, p.serverLevel().getGameTime() + Wave21Rules.ZONE_TICKS));
        }
        FxRelic.ring(p.serverLevel(), centre, Wave21Rules.ZONE_RADIUS + 0.4, 0xFF5AC0);
        return true;
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        if (ZONES.isEmpty()) return;
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 4 != 0) return;
        synchronized (ZONES) {
            ZONES.removeIf(z -> {
                ServerLevel l = server.getLevel(z.dim());
                if (l == null || l.getGameTime() >= z.until()) return true;
                if (server.getTickCount() % 12 == 0) FxRelic.ring(l, z.centre(), Wave21Rules.ZONE_RADIUS, 0xFF5AC0);
                AABB box = new AABB(z.centre(), z.centre()).inflate(Wave21Rules.ZONE_RADIUS, 2.0, Wave21Rules.ZONE_RADIUS);
                int n = 0;
                for (LivingEntity en : l.getEntitiesOfClass(LivingEntity.class, box)) {
                    if (n++ >= 12) break;
                    if (en.isSpectator() || (en instanceof Player pl && pl.isCreative())) continue;
                    if (!Wave21Rules.inZone(en.getX(), en.getY(), en.getZ(), z.centre().x, z.centre().y, z.centre().z)) continue;
                    scatter(l, en);
                }
                return false;
            });
        }
    }

    /** Carries an entity 30 to 60 blocks away to a spot with room to stand and ground below (a loaded chunk, no lava); tries eight places. */
    static void scatter(ServerLevel l, LivingEntity en) {
        for (int i = 0; i < 8; i++) {
            double a = l.random.nextDouble() * Math.PI * 2, d = Wave21Rules.SCATTER_MIN + l.random.nextDouble() * (Wave21Rules.SCATTER_MAX - Wave21Rules.SCATTER_MIN);
            double x = en.getX() + Math.cos(a) * d, z = en.getZ() + Math.sin(a) * d;
            if (!l.hasChunkAt(BlockPos.containing(x, en.getY(), z))) continue;
            for (int dy : new int[]{0, 1, -1, 2, -2, 4, -4, 8, -8}) {
                double y = en.getY() + dy;
                AABB box = en.getBoundingBox().move(x - en.getX(), y - en.getY(), z - en.getZ());
                if (!l.noCollision(en, box)) continue;
                BlockPos feet = BlockPos.containing(x, y, z);
                boolean ground = false, bad = false;
                for (int k = 1; k <= 3 && !ground; k++) {
                    var below = l.getBlockState(feet.below(k));
                    if (below.is(net.minecraft.world.level.block.Blocks.LAVA) || below.is(net.minecraft.world.level.block.Blocks.FIRE)) bad = true;
                    ground = !below.getCollisionShape(l, feet.below(k)).isEmpty();
                }
                if (!ground || bad || l.getFluidState(feet).is(FluidTags.LAVA)) continue;
                FxRelic.burst(l, en.position(), 0xFF5AC0);
                en.teleportTo(x, y, z);
                en.fallDistance = 0;
                FxRelic.burst(l, en.position(), 0xFFE27A);
                return;
            }
        }
    }

    // ================================================================ the player tick
    private static boolean due(ServerPlayer p, int period) { return (p.tickCount + p.getId()) % period == 0; }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        if (d.contains(Wave21Acts.K_CLIMB) && d.getLong(Wave21Acts.K_CLIMB) > now) climb(p);
        if (d.contains(Wave21Acts.K_WHIRL) && d.getLong(Wave21Acts.K_WHIRL) > now) whirl(p, now);
        if (!due(p, 20)) return;
        String id = heldId(p);
        if (!id.isEmpty()) {
            ItemStack s = p.getMainHandItem();
            switch (id) {
                case "silver_wind_mountain_glaive" -> Wave21Charges.charge(s, "sw", 20, Wave21Rules.GLAIVE_TICKS_PER_CHARGE, Wave21Rules.GLAIVE_MAX);
                case "sun_gift" -> {
                    if (p.level().isDay() && !p.level().isRaining() && p.level().canSeeSky(p.blockPosition().above()))
                        Wave21Charges.charge(s, "sun", 20, Wave21Rules.SUN_TICKS_PER_CHARGE, Wave21Rules.SUN_MAX);
                }
                case "the_last_horseman" -> Wave21Charges.charge(s, "dead", 20, Wave21Rules.DEAD_TICKS_PER_CHARGE, Wave21Rules.DEAD_MAX);
                default -> { }
            }
        }
        if (d.contains(K_WITHER_UNTIL) && d.getLong(K_WITHER_UNTIL) <= now) restoreHealth(p);
    }

    /** Spider Climb: pushing against a wall lifts you (sneak holds you still). */
    private static void climb(ServerPlayer p) {
        if (!has(p, Wave21Rules.F_SPIDER) || !p.horizontalCollision) return;
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, p.isShiftKeyDown() ? 0.0 : 0.22, v.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
    }

    /** Whirlwind: you cannot walk; every half second foes within 4 blocks are dragged in and cut. */
    private static void whirl(ServerPlayer p, long now) {
        if (!has(p, Wave21Rules.F_GLAIVE)) return;
        if (now % 4 == 0) Fx.addEffect(p, "minecraft:slowness", 8, 6);
        if (now % Wave21Rules.WHIRL_PULSE != 0) return;
        FxRelic.ring(p.serverLevel(), p.position(), Wave21Rules.WHIRL_RADIUS, 0x9CCBFF);
        for (LivingEntity t : Wave21Acts.foesNear(p, p.position(), Wave21Rules.WHIRL_RADIUS)) {
            Vec3 to = p.position().subtract(t.position());
            Vec3 pull = to.lengthSqr() < 1.0e-4 ? Vec3.ZERO : to.normalize().scale(0.35);
            t.push(pull.x, 0.1, pull.z);
            t.hurtMarked = true;
            Wave0Util.extra(p, t, 3.0f, "minecraft:player_attack");
        }
    }

    // ================================================================ damage in
    private static void onIncoming(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        LivingEntity victim = e.getEntity();
        if (Wave0Util.busy() || !(src.getEntity() instanceof ServerPlayer p) || !src.is(DamageTypes.PLAYER_ATTACK) || victim == p) return;
        String id = heldId(p);
        if (id.isEmpty()) return;
        float a = e.getAmount();
        switch (id) {
            case "stick_of_youth" -> {
                if (!has(p, Wave21Rules.F_STICK)) return;
                int form = Wave0Util.stackInt(p.getMainHandItem(), Wave21Acts.K_FORM, 0);
                e.setAmount(a * Wave21Rules.formScale(form));
                String f = Wave21Rules.formName(form);
                if (f.equals("sword")) {
                    for (LivingEntity o : p.serverLevel().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(1.2), x -> x != victim && Wave0Util.fair(p, x)))
                        Wave0Util.extra(p, o, a * 0.3f, "minecraft:player_attack");
                } else if (f.equals("axe") && victim instanceof Player vp && vp.isBlocking()) {
                    vp.stopUsingItem();
                    vp.getCooldowns().addCooldown(Items.SHIELD, 100);
                } else if (f.equals("spear")) {
                    Vec3 d = victim.position().subtract(p.position());
                    Vec3 dir = d.lengthSqr() < 1.0e-4 ? p.getLookAngle() : d.normalize();
                    Vec3 at = victim.position().add(dir.scale(Wave21Rules.SPEAR_BEHIND));
                    for (LivingEntity o : Wave21Acts.foesNear(p, at, 1.3)) if (o != victim) Wave0Util.extra(p, o, a * 0.4f, "minecraft:player_attack");
                }
            }
            case "the_librarians_maul" -> {
                if (!has(p, Wave21Rules.F_MAUL)) return;
                e.setAmount((a + Wave21Rules.libraryBonus(p.experienceLevel)) * Wave21Rules.handScale(p.getOffhandItem().isEmpty()));
            }
            case "the_legacy" -> {
                if (has(p, Wave21Rules.F_LEGACY)) e.setAmount(a * Wave21Rules.headlessScale(p.getPersistentData().getLong(Wave21Acts.K_HEADLESS) > p.level().getGameTime()));
            }
            default -> { }
        }
    }

    // ================================================================ damage out (after it landed)
    private static void onPost(LivingDamageEvent.Post e) {
        if (Wave0Util.busy()) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || !src.is(DamageTypes.PLAYER_ATTACK)) return;
        LivingEntity t = e.getEntity();
        if (t == p) return;
        String id = heldId(p);
        if (id.isEmpty()) return;
        long now = p.level().getGameTime();
        switch (id) {
            case "scimitar_of_the_seasons" -> {
                if (!has(p, Wave21Rules.F_SEASONS) || !RelicEngine.isOn(p, "scimitar_of_the_seasons", "command_word")) return;
                CompoundTag d = p.getPersistentData();
                if (now - d.getLong(K_SEASON_AT) < Wave21Rules.SEASON_GAP) return;
                d.putLong(K_SEASON_AT, now);
                Wave21Rules.Season s = Wave21Rules.season(p.level().getDayTime() / 24000L);
                Wave0Util.extra(p, t, Wave21Rules.SEASON_BONUS, Wave21Rules.seasonDamageType(s));
                switch (s) {
                    case SUMMER -> t.setRemainingFireTicks(60);
                    case AUTUMN -> Fx.addEffect(t, "minecraft:wither", 60, 0);
                    case WINTER -> {
                        t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 40, t.getTicksFrozen() + 80));
                        Fx.addEffect(t, "minecraft:slowness", 60, 1);
                    }
                    default -> Fx.addEffect(t, "minecraft:glowing", 60, 0);
                }
                FxRelic.burst(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.5, 0), Wave21Rules.seasonColour(s));
            }
            case "spider_sword" -> {
                if (!has(p, Wave21Rules.F_SPIDER)) return;
                Wave0Util.extra(p, t, 3.0f, "minecraft:magic");
                Fx.addEffect(t, "minecraft:poison", t instanceof Player ? 60 : 120, 0);
            }
            case "sun_gift" -> {
                if (has(p, Wave21Rules.F_SUN)) Wave0Util.extra(p, t, 7.0f, "tensura:holy_damage");
            }
            case "the_last_horseman" -> {
                if (!has(p, Wave21Rules.F_HORSEMAN)) return;
                Wave0Util.extra(p, t, Wave21Rules.NECROTIC_BONUS, "minecraft:wither");
                if (t.isAlive()) witherHit(t, now);
            }
            case "the_legacy" -> {
                if (has(p, Wave21Rules.F_LEGACY) && p.getPersistentData().getLong(Wave21Acts.K_HEADLESS) <= now) {
                    Wave0Util.extra(p, t, Wave21Rules.THUNDER_BONUS, "minecraft:lightning_bolt");
                    FxRelic.burst(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.5, 0), 0x78DCFF);
                }
            }
            case "time_hammer" -> {
                if (!has(p, Wave21Rules.F_TIME)) return;
                CompoundTag d = p.getPersistentData();
                if (d.getBoolean(Wave21Acts.K_SMITE_USED) || !Wave21Rules.smiteReady(now, d.getLong(Wave21Acts.K_STOP))) return;
                d.putBoolean(Wave21Acts.K_SMITE_USED, true);                     // Hammertime: a thunderous smite
                Wave0Util.extra(p, t, 12.0f, "minecraft:lightning_bolt");
                Fx.addEffect(t, "minecraft:slowness", t instanceof Player ? 40 : 80, 3);
                Vec3 push = t.position().subtract(p.position());
                if (push.lengthSqr() > 1.0e-4) {
                    Vec3 n = push.normalize().scale(1.2);
                    t.push(n.x, 0.4, n.z);
                    t.hurtMarked = true;
                }
                FxRelic.burst(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.5, 0), 0xFF9650);
                Fx.say(p, "Hammertime!");
            }
            default -> { }
        }
    }

    /** Damage dealt by the scythe reduces maximum health: 4 percent a blow up to 30, an attribute modifier that is lifted after five minutes (players) or when they heal back. */
    private static void witherHit(LivingEntity t, long now) {
        AttributeInstance max = t.getAttribute(Attributes.MAX_HEALTH);
        if (max == null) return;
        CompoundTag d = t.getPersistentData();
        int hits = d.getInt(K_WITHER_HITS) + 1;
        d.putInt(K_WITHER_HITS, hits);
        d.putLong(K_WITHER_UNTIL, now + Wave21Rules.WITHER_TICKS);
        max.removeModifier(WITHER_ID);
        max.addTransientModifier(new AttributeModifier(WITHER_ID, -Wave21Rules.witherLoss(hits), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (t.getHealth() > t.getMaxHealth()) t.setHealth(t.getMaxHealth());
    }

    private static void restoreHealth(LivingEntity t) {
        AttributeInstance max = t.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) max.removeModifier(WITHER_ID);
        t.getPersistentData().remove(K_WITHER_HITS);
        t.getPersistentData().remove(K_WITHER_UNTIL);
    }

    // ================================================================ Willbreaker: the cries on a critical hit
    private static void onCrit(CriticalHitEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !e.isCriticalHit() || !heldId(p).equals("willbreaker") || !has(p, Wave21Rules.F_WILL)) return;
        if (!(e.getTarget() instanceof LivingEntity t)) return;
        FxRelic.burst(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.5, 0), 0xD2242C);
        FxRelic.ring(p.serverLevel(), t.position(), 3.0, 0xD2242C);
        Wave21Acts.frighten(p, t, 0.0);
        for (LivingEntity o : Wave21Acts.foesNear(p, t.position(), 3.0)) if (o != t) Wave21Acts.frighten(p, o, 0.0);
    }

    // ================================================================ clean up
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restoreHealth(p);
    }

    private static void onStopping(ServerStoppingEvent e) {
        synchronized (ZONES) {
            ZONES.clear();
        }
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) restoreHealth(p);
    }
}
