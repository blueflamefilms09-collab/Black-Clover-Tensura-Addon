package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wave 10: the game-bus listeners of the ten weapons. Every listener starts with a cheap test (who is hit, which flag the attacker carries) and does its
 * real work only for the wielders; the player tick is staggered by entity id and nothing scans the world per tick. The one thing that lives between
 * ticks is the Wall of Water (at most one per player, at most 32 in all, gone after eight seconds, cleared on stop).
 */
final class Wave10Events {
    private Wave10Events() {}

    private static boolean hooked;
    private static final int MAX_WALLS = 32;
    private static final Map<UUID, Wall> WALLS = new ConcurrentHashMap<>();
    private static final String K_INSULT = "nusmp_w10_insult_at";

    /** A standing wall of water: where, which way it faces, when it falls. */
    record Wall(ResourceKey<Level> dim, Vec3 centre, double dx, double dz, long until) {}

    static void register() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(Wave10Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave10Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Wave10Events::onIncomingLate);
        NeoForge.EVENT_BUS.addListener(Wave10Events::onPost);
        NeoForge.EVENT_BUS.addListener(Wave10Events::onJoin);
        NeoForge.EVENT_BUS.addListener(Wave10Events::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave10Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave10Events::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave10Events::onStopping);
    }

    private static boolean has(LivingEntity e, String flag) { return RelicFlags.has(e, flag); }

    private static boolean due(ServerPlayer p, int period) { return (p.tickCount + p.getId()) % period == 0; }

    // ================================================================ Krakeblade: the domain of the kraken
    /** In the water or the rain, or with a pool within four blocks (the card's 15 feet). */
    static boolean nearWater(ServerPlayer p) {
        if (p.isInWaterOrRain()) return true;
        ServerLevel l = p.serverLevel();
        BlockPos o = p.blockPosition();
        for (int dx = -4; dx <= 4; dx += 4) {
            for (int dz = -4; dz <= 4; dz += 4) {
                for (int dy = -1; dy <= 0; dy++) if (l.getFluidState(o.offset(dx, dy, dz)).is(FluidTags.WATER)) return true;
            }
        }
        return false;
    }

    /** Wall of Water (called by the act): one wall per player. */
    static void raiseWall(ServerPlayer p, Vec3 centre, double dx, double dz) {
        if (!WALLS.containsKey(p.getUUID()) && WALLS.size() >= MAX_WALLS) return;
        WALLS.put(p.getUUID(), new Wall(p.serverLevel().dimension(), centre, dx, dz, p.serverLevel().getGameTime() + Wave10Rules.WALL_TICKS));
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        if (WALLS.isEmpty()) return;
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 4 != 0) return;
        for (Map.Entry<UUID, Wall> en : WALLS.entrySet()) {
            Wall w = en.getValue();
            ServerLevel l = server.getLevel(w.dim());
            ServerPlayer owner = server.getPlayerList().getPlayer(en.getKey());
            if (l == null || owner == null || !owner.isAlive() || l.getGameTime() >= w.until()) {
                WALLS.remove(en.getKey());
                continue;
            }
            tickWall(l, owner, w);
        }
    }

    private static void tickWall(ServerLevel l, ServerPlayer owner, Wall w) {
        double px = -w.dz(), pz = w.dx();                                            // along the wall
        for (int i = 0; i < 6; i++) {
            double s = (l.random.nextDouble() * 2 - 1) * Wave10Rules.WALL_HALF_WIDTH;
            double y = w.centre().y + l.random.nextDouble() * Wave10Rules.WALL_HEIGHT;
            l.sendParticles(ParticleTypes.FALLING_WATER, w.centre().x + px * s, y, w.centre().z + pz * s, 2, 0.05, 0.2, 0.05, 0.0);
            l.sendParticles(ParticleTypes.SPLASH, w.centre().x + px * s, y, w.centre().z + pz * s, 2, 0.1, 0.2, 0.1, 0.0);
        }
        double r = Wave10Rules.WALL_HALF_WIDTH + 1.0;
        AABB box = new AABB(w.centre().x - r, w.centre().y - 1, w.centre().z - r, w.centre().x + r, w.centre().y + Wave10Rules.WALL_HEIGHT + 1, w.centre().z + r);
        int handled = 0;
        for (Entity en : l.getEntitiesOfClass(Entity.class, box)) {
            if (handled >= 24) break;
            if (!Wave10Rules.inWall(en.getX(), en.getY() + en.getBbHeight() * 0.5, en.getZ(), w.centre().x, w.centre().y, w.centre().z, w.dx(), w.dz())) continue;
            if (en instanceof Projectile pr) {
                if (pr.getOwner() != owner) { pr.discard(); handled++; }
            } else if (en instanceof LivingEntity t && Wave0Util.fair(owner, t)) {
                int side = Wave10Rules.wallSide(t.getX(), t.getZ(), w.centre().x, w.centre().z, w.dx(), w.dz());
                if (side == 0) side = -1;
                t.setDeltaMovement(t.getDeltaMovement().add(w.dx() * side * 0.45, 0.1, w.dz() * side * 0.45));
                t.hurtMarked = true;
                Fx.addEffect(t, "minecraft:slowness", 40, 1);
                handled++;
            }
        }
    }

    // ================================================================ the player tick
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || !p.isAlive()) return;
        // Junglethorn Whip: Junglestride, every tick while pressed against a tree
        if (p.horizontalCollision && !p.onGround() && !p.isInWater() && !p.isFallFlying() && !p.isPassenger() && !p.getAbilities().flying && has(p, Wave10.F_JUNGLE)) climb(p);
        if (due(p, 20)) {
            if (has(p, Wave10.F_KRAKE) && nearWater(p)) kraken(p);
            if (has(p, Wave10.F_RUNE) && RelicEngine.isOn(p, "rune_splitter", "light")) runeLight(p);
        }
        if (due(p, Wave10Rules.FLAIL_GAP) && has(p, Wave10.F_STEGO)) flail(p);
    }

    private static void climb(ServerPlayer p) {
        BlockPos front = p.blockPosition().relative(p.getDirection());
        boolean plant = false;
        for (int dy = 0; dy <= 1 && !plant; dy++) {
            BlockState s = p.level().getBlockState(front.above(dy));
            plant = s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(Blocks.VINE) || s.is(Blocks.BAMBOO) || s.is(BlockTags.CLIMBABLE);
        }
        if (!plant) return;
        Vec3 dm = p.getDeltaMovement();
        p.setDeltaMovement(dm.x, p.isShiftKeyDown() ? 0.0 : Math.max(dm.y, 0.2), dm.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
    }

    private static void kraken(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 hand = p.position().add(p.getLookAngle().scale(0.8)).add(0, 1.1, 0);
        l.sendParticles(Wave10Acts.dust(0x3FE0C8, 1.0f), hand.x, hand.y, hand.z, 6, 0.4, 0.5, 0.4, 0.0);
        l.sendParticles(ParticleTypes.GLOW, hand.x, hand.y, hand.z, 2, 0.3, 0.3, 0.3, 0.01);
    }

    private static void runeLight(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        int col = Wave10Rules.lightColour(p.getPersistentData().getInt(Wave10Acts.K_LIGHT_COL));
        Fx.addEffect(p, "minecraft:night_vision", 260, 0);
        Vec3 c = p.position().add(0, 1.2, 0);
        l.sendParticles(Wave10Acts.dust(col, 1.4f), c.x, c.y, c.z, 10, 0.6, 0.6, 0.6, 0.0);
        l.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 2, 0.5, 0.5, 0.5, 0.01);
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, Wave10Rules.LIGHT_RADIUS)) {
            if (n++ >= 12) break;
            Fx.addEffect(t, "minecraft:glowing", 60, 0);
        }
    }

    /** Stegosaurus Greatsword: the weapon lashes out on its own at whatever stands behind you. */
    private static void flail(ServerPlayer p) {
        boolean whip = RelicEngine.isOn(p, "stegosaurus_greatsword", "command_word");
        double reach = whip ? Wave10Rules.FLAIL_REACH_WHIP : Wave10Rules.FLAIL_REACH_SWORD;
        Vec3 look = p.getLookAngle();
        int left = Wave10Rules.flailTargets(whip);
        ServerLevel l = p.serverLevel();
        for (LivingEntity t : Fx.foesAround(p, reach)) {
            if (left <= 0) break;
            Vec3 to = t.position().subtract(p.position());
            if (!Wave10Rules.inRear(look.x, look.z, to.x, to.z)) continue;
            Wave0Util.extra(p, t, Wave10Rules.flailDamage(whip), "minecraft:player_attack");
            l.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 1, 0, 0, 0, 0);
            l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.7f);
            left--;
        }
    }

    // ================================================================ damage
    private static void onIncoming(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        LivingEntity victim = e.getEntity();
        if (!Wave0Util.busy() && src.getEntity() instanceof ServerPlayer p && src.is(DamageTypes.PLAYER_ATTACK) && victim != p) {
            if (has(p, Wave10.F_KRAKE) && nearWater(p)) e.setAmount(e.getAmount() * 1.2f);
            if (has(p, Wave10.F_STEGO)) {
                Vec3 look = p.getLookAngle(), to = victim.position().subtract(p.position());
                if (Wave10Rules.inFront(look.x, look.z, to.x, to.z)) e.setAmount(e.getAmount() * Wave10Rules.FRONT_SCALE);
            }
        }
    }

    /**
     * Jade Xiphos: runs last (after every shield, resistance and cancel), so the blade only breaks under a blow that really lands. Never for a creative
     * or spectating player, a blow the player is invulnerable to, or a fall into the void (only blast, thunder and sound count).
     */
    private static void onIncomingLate(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer v) || e.isCanceled() || v.isCreative() || v.isSpectator() || !v.isAlive()) return;
        DamageSource src = e.getSource();
        if (v.isInvulnerableTo(src) || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !has(v, Wave10.F_JADE)) return;
        boolean force = src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_LIGHTNING) || src.is(DamageTypes.SONIC_BOOM);
        if (!Wave10Rules.shatters(e.getAmount(), force)) return;
        ItemStack hand = Relics.defOf(v.getMainHandItem()) != null && "jade_xiphos".equals(Relics.defOf(v.getMainHandItem()).id()) ? v.getMainHandItem() : v.getOffhandItem();
        shatterXiphos(v, hand, false);
    }

    private static void onPost(LivingDamageEvent.Post e) {
        if (Wave0Util.busy()) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p)) return;
        LivingEntity t = e.getEntity();
        if (t == p) return;
        boolean melee = src.is(DamageTypes.PLAYER_ATTACK);
        if (melee && has(p, Wave10.F_SIDRA)) insult(p, t);
        if (melee && has(p, Wave10.F_KRAKE) && p.isInWaterOrRain()) Fx.addEffect(t, "minecraft:slowness", 40, 0);   // tentacles
        if (src.getDirectEntity() instanceof AbstractArrow && has(p, Wave10.F_NEEDLER)) Wave0Util.extra(p, t, 2.0f, "minecraft:magic");   // needles that find the gap
    }

    /** The sword swears at the creature it just hit: a Wisdom save against DC 13 or it is insulted (1d4 psychic and a moment of weakness). */
    static void insult(ServerPlayer p, LivingEntity t) {
        if (!Wave0Util.targetable(p, t)) return;
        long now = p.level().getGameTime();
        if (now - t.getPersistentData().getLong(K_INSULT) < 20) return;
        t.getPersistentData().putLong(K_INSULT, now);
        if (Wave0Util.saves(t, Wave10Rules.WIS_DC, 0)) return;
        ServerLevel l = p.serverLevel();
        if (mayShout(p, now)) {
            String line = Wave10Rules.insult(t.getName().getString(), p.getRandom().nextInt(64));
            Component msg = Component.literal("<Sidras' swearing sword> " + line).withStyle(ChatFormatting.LIGHT_PURPLE);
            for (ServerPlayer q : l.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(24.0))) q.sendSystemMessage(msg);
        }
        Wave0Util.extra(p, t, Wave10Rules.psychic(p.getRandom().nextInt(4)), "minecraft:magic");
        Fx.addEffect(t, "minecraft:weakness", 80, 0);
        l.sendParticles(ParticleTypes.ANGRY_VILLAGER, t.getX(), t.getY() + t.getBbHeight() + 0.3, t.getZ(), 3, 0.3, 0.1, 0.3, 0.0);
    }

    private static final String K_SHOUT_AT = "nusmp_w10_shout_at", K_SHOUT_N = "nusmp_w10_shout_n";

    /** Chat spam guard: one wielder gets at most 3 lines in a tick (the Barrage), then none for a second. The damage and the weakness are not limited by it. */
    private static boolean mayShout(ServerPlayer p, long now) {
        var d = p.getPersistentData();
        long last = d.getLong(K_SHOUT_AT);
        if (now == last) {
            int n = d.getInt(K_SHOUT_N);
            if (n >= 3) return false;
            d.putInt(K_SHOUT_N, n + 1);
            return true;
        }
        if (last != 0 && now > last && now - last < 20) return false;
        d.putLong(K_SHOUT_AT, now);
        d.putInt(K_SHOUT_N, 1);
        return true;
    }

    // ================================================================ Jade Xiphos
    /** The blade vibrates itself to pieces: the sword is gone for good and the shards fly into every foe within five blocks. */
    static void shatterXiphos(ServerPlayer p, ItemStack stack, boolean deliberate) {
        RelicDef d = Relics.defOf(stack);
        if (d == null || !"jade_xiphos".equals(d.id())) return;
        ServerLevel l = p.serverLevel();
        stack.shrink(1);
        Relics.invalidate(p);
        Vec3 c = p.position().add(0, 1.0, 0);
        l.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.LIME_STAINED_GLASS_PANE)), c.x, c.y, c.z, 40, 0.8, 0.6, 0.8, 0.2);
        l.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.4f, 0.8f);
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 5.0)) {
            if (n++ >= 8) break;
            int sum = 0;
            for (int i = 0; i < 6; i++) sum += p.getRandom().nextInt(10);
            Wave0Util.extra(p, t, Wave10Rules.shardDamage(sum + 6, t.distanceToSqr(p) < 6.25), "minecraft:thorns");
        }
        Fx.say(p, deliberate ? "You sing the blade to pieces: it is destroyed beyond repair." : "The blade shudders and bursts: it is destroyed beyond repair.");
    }

    // ================================================================ Junglethorn Whip: Speak with Animals
    private static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p) || !(e.getEntity() instanceof Animal a) || a instanceof Enemy) return;
        if (a.getLastHurtByMob() == p || !has(p, Wave10.F_JUNGLE)) return;
        e.setCanceled(true);                                                         // the wild animals know the whip's wielder: they leave you be unless you hurt them
    }

    /** The raised servant of the Dead Blade does not outlive a save: a creature that comes back from disk with the mark is removed (the core's sweep only knows the live ones). */
    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk() && !e.getLevel().isClientSide() && e.getEntity().getPersistentData().getBoolean(Wave10Acts.K_RAISED)) e.getEntity().discard();
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { WALLS.remove(e.getEntity().getUUID()); }

    private static void onStopping(ServerStoppingEvent e) { WALLS.clear(); }
}
