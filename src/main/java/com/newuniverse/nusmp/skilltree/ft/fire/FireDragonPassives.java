package com.newuniverse.nusmp.skilltree.ft.fire;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

/**
 * Why: the two passives of the Fire route (Eating Flame and Blazing Scales) are not spells with a key, so they cannot live in a ManasCore skill's cast; they are things that
 * happen TO the player, so they belong where the game reports such things: the damage-taken event (fire that would hurt is eaten; a melee blow that lands is answered with
 * flame) and a once-a-second player tick (standing in or near flame feeds the player; hostile creatures next to the player catch fire). Both are NeoForge game-bus listeners,
 * server-side only, registered once from {@code FireDragonSkills}, and each starts with the skill-tree gate ({@code SkillTreeApi.canCast}: route unlocked, node bought, class
 * active) so a refunded or foreign node does nothing. Everything numeric comes from the route's tables by node level, so the SP invested changes the effect. The two decisions
 * are public static methods taking plain arguments ({@link #eat}, {@link #scorch}) so the in-game self-test drives them directly with a stand-in player: a fake player cannot be
 * hurt (the game makes it invulnerable), so the self-test cannot prove a passive by hurting it. A recoil hit never sets off another recoil (a guard), the benefit of eating
 * is limited to once a second so rapid ticks of fire cannot farm magicules, and nothing here ever throws: an exception in an event handler would reach the whole server.
 */
public final class FireDragonPassives {
    private FireDragonPassives() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String EAT_UNTIL = "nusmp_ft_eat_until";
    private static final String SCORCH_UNTIL = "nusmp_ft_scorch_until";
    private static boolean registered;
    private static boolean recoiling;

    /** Starts listening to the damage and the player tick. Idempotent. */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(FireDragonPassives::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(FireDragonPassives::onPlayerTick);
    }

    private static double num(String node, String key, int level) { return DragonKit.value(FireDragonNumbers.TABLES, node, key, level); }

    private static int levelOf(ServerPlayer p, String node) { return Math.max(1, SkillTreeApi.nodeLevel(p, FireDragonNumbers.ROUTE, node)); }

    private static boolean has(ServerPlayer p, String node) { return SkillTreeApi.canCast(p, FireDragonNumbers.ROUTE, node); }

    // ------------------------------------------------------------------ events
    /** A player is about to take damage: fire may be eaten, a melee blow may be answered. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            if (eat(p, e.getSource(), e.getAmount())) {
                e.setCanceled(true);
                return;
            }
            scorch(p, e.getSource());
        } catch (Throwable t) {
            LOG.warn("[nusmp] Fire Dragon passive failed on damage: {}", t.toString());
        }
    }

    /** Once a second per player: the flame-eating bite and the aura. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !p.isAlive()) return;
            feed(p);
            warm(p);
        } catch (Throwable t) {
            LOG.warn("[nusmp] Fire Dragon passive failed on tick: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------ Eating Flame
    /** True when {@code src} is fire in the sense of this passive: the game's fire damage and Tensura's fire magic and flame breath. */
    static boolean isFire(DamageSource src) {
        return src.is(DamageTypeTags.IS_FIRE) || src.is(TensuraDamageTypes.FIRE_ELEMENTAL) || src.is(TensuraDamageTypes.FLAME_BREATH);
    }

    /**
     * Eating Flame, on damage: fire that would hurt {@code p} feeds them instead. Returns true when the damage must be cancelled (the caller cancels the event); the heal and the
     * magicule refill are applied here, at most once a second. Flames the player made themselves are not eaten (canon), and lava only at the last level.
     */
    public static boolean eat(ServerPlayer p, DamageSource src, float amount) {
        if (!isFire(src) || src.getEntity() == p || !has(p, FireDragonNumbers.FLAME_EATER)) return false;
        int lv = levelOf(p, FireDragonNumbers.FLAME_EATER);
        if (src.is(DamageTypes.LAVA) && num(FireDragonNumbers.FLAME_EATER, "eats_lava", lv) < 1.0) return false;
        CompoundTag d = p.getPersistentData();
        long now = p.serverLevel().getGameTime();
        if (now >= d.getLong(EAT_UNTIL)) {
            d.putLong(EAT_UNTIL, now + 20L);
            AbilityKit.heal(p, (float) (amount * num(FireDragonNumbers.FLAME_EATER, "eat_heal_ratio", lv)));
            DragonKit.gainMagicule(p, num(FireDragonNumbers.FLAME_EATER, "eat_recharge_pct", lv) / 100.0);
            Vec3 at = p.position().add(0.0, 0.8, 0.0);
            AbilityKit.fx(p.serverLevel(), VfxShape.FIRE_BURST, at, at, 0, 10, 0.35f);
        }
        if (src.is(DamageTypes.ON_FIRE) || src.is(DamageTypes.IN_FIRE)) p.clearFire();
        return true;
    }

    /** Eating Flame, once a second: a player who burns, or stands beside a flame, is fed a little. */
    static void feed(ServerPlayer p) {
        if (!has(p, FireDragonNumbers.FLAME_EATER)) return;
        int lv = levelOf(p, FireDragonNumbers.FLAME_EATER);
        boolean lava = num(FireDragonNumbers.FLAME_EATER, "eats_lava", lv) >= 1.0;
        boolean burning = p.isOnFire();
        if (!burning && !nearFlame(p, num(FireDragonNumbers.FLAME_EATER, "radius", lv), lava)) return;
        if (burning) p.clearFire();
        if (p.getHealth() < p.getMaxHealth()) AbilityKit.heal(p, (float) (p.getMaxHealth() * num(FireDragonNumbers.FLAME_EATER, "heal_pct", lv) / 100.0));
        DragonKit.gainMagicule(p, num(FireDragonNumbers.FLAME_EATER, "recharge_pct", lv) / 100.0);
        Vec3 at = p.position().add(0.0, 0.8, 0.0);
        AbilityKit.fx(p.serverLevel(), VfxShape.FIRE_BURST, at, at, 0, 10, 0.3f);
    }

    /** Whether a flame (fire, a lit campfire, a torch, and at the last level lava) is within {@code radius} blocks of the player. Reads blocks only; changes none. */
    static boolean nearFlame(ServerPlayer p, double radius, boolean lava) {
        ServerLevel level = p.serverLevel();
        BlockPos c = p.blockPosition();
        int r = Math.max(1, (int) Math.ceil(radius));
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -1, -r), c.offset(r, 2, r))) {
            BlockState s = level.getBlockState(pos);
            if (s.is(BlockTags.FIRE) || s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH)) return true;
            if (s.is(BlockTags.CAMPFIRES) && s.hasProperty(CampfireBlock.LIT) && s.getValue(CampfireBlock.LIT)) return true;
            if (lava && s.getFluidState().is(FluidTags.LAVA)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ Blazing Scales
    /**
     * Blazing Scales, on damage: whoever strikes {@code p} in melee is set alight and takes a small recoil through the balance law, at most once per attacker per the
     * node's interval. Projectiles and spells do not trigger it (only a blow from the attacker's own body), and a recoil never triggers another recoil.
     */
    public static void scorch(ServerPlayer p, DamageSource src) {
        if (recoiling) return;
        LivingEntity attacker = src.getEntity() instanceof LivingEntity a ? a : null;
        if (attacker == null || attacker == p || src.getDirectEntity() != attacker || attacker.isAlliedTo(p)) return;
        if (!has(p, FireDragonNumbers.BLAZING_SCALES)) return;
        int lv = levelOf(p, FireDragonNumbers.BLAZING_SCALES);
        CompoundTag d = attacker.getPersistentData();
        long now = p.serverLevel().getGameTime();
        if (now < d.getLong(SCORCH_UNTIL)) return;
        d.putLong(SCORCH_UNTIL, now + Math.max(20L, Math.round(num(FireDragonNumbers.BLAZING_SCALES, "icd", lv))));
        recoiling = true;
        try {
            attacker.igniteForSeconds((float) num(FireDragonNumbers.BLAZING_SCALES, "burn_seconds", lv));
            AbilityKit.hit(p, attacker, (float) num(FireDragonNumbers.BLAZING_SCALES, "recoil", lv), 0.0);
            Vec3 c = DragonKit.center(attacker);
            AbilityKit.fx(p.serverLevel(), VfxShape.FIRE_BURST, c, c, 0, 12, 0.4f);
        } finally {
            recoiling = false;
        }
    }

    /** Blazing Scales, once a second: hostile creatures beside the player catch fire. Only the hostile: animals, villagers and pets are never set alight by it. */
    static void warm(ServerPlayer p) {
        if (!has(p, FireDragonNumbers.BLAZING_SCALES)) return;
        int lv = levelOf(p, FireDragonNumbers.BLAZING_SCALES);
        float seconds = (float) num(FireDragonNumbers.BLAZING_SCALES, "aura_burn_seconds", lv);
        for (LivingEntity t : AbilityKit.around(p, p.position(), num(FireDragonNumbers.BLAZING_SCALES, "aura_radius", lv))) {
            if (t instanceof Enemy) t.igniteForSeconds(seconds);
        }
    }
}
