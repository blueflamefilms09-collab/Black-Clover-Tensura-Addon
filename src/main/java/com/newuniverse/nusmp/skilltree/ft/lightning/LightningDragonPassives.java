package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

/**
 * Why: the two passives of the Lightning route (Lightning Body and Eating Lightning) are things that happen TO the player, so they belong where the game reports such things, not
 * in a skill's cast: the damage-taken event (lightning that would hurt is eaten; now and then a blow passes through the body) and a once-a-second player tick (a sprinting player
 * is quickened; a player under a thunderstorm in the open is fed). Both are NeoForge game-bus listeners, server-side only, registered once from
 * {@code LightningDragonSkills}, each starting with the skill-tree gate ({@code SkillTreeApi.canCast}: route unlocked, node bought, class active) so a
 * refunded or foreign node does nothing; every number comes from the route's tables by node level, so the SP invested changes the effect. The decisions are public static methods
 * taking plain arguments ({@link #eat}, {@link #evade}) so the in-game self-test drives them with a stand-in player (the game makes a fake player invulnerable, so the real damage
 * event can never reach it); the evade takes its dice roll as an argument so the test can force both outcomes. A blow is only avoided when it comes from something (an attacker),
 * never from fall, drowning, starvation or the void, and at most once per interval, so it cannot make the player untouchable. Nothing here throws out of a handler.
 */
public final class LightningDragonPassives {
    private LightningDragonPassives() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String EAT_UNTIL = "nusmp_ft_thunder_eat_until";
    private static final String EVADE_UNTIL = "nusmp_ft_evade_until";
    private static boolean registered;

    /** Starts listening to the damage and the player tick. Idempotent. */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(LightningDragonPassives::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(LightningDragonPassives::onPlayerTick);
    }

    private static double num(String node, String key, int level) { return DragonKit.value(LightningDragonNumbers.TABLES, node, key, level); }

    private static int levelOf(ServerPlayer p, String node) { return Math.max(1, SkillTreeApi.nodeLevel(p, LightningDragonNumbers.ROUTE, node)); }

    private static boolean has(ServerPlayer p, String node) { return SkillTreeApi.canCast(p, LightningDragonNumbers.ROUTE, node); }

    // ------------------------------------------------------------------ events
    /** A player is about to take damage: lightning may be eaten, a blow may pass through the body. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            if (eat(p, e.getSource(), e.getAmount()) || evade(p, e.getSource(), p.getRandom().nextDouble() * 100.0)) e.setCanceled(true);
        } catch (Throwable t) {
            LOG.warn("[nusmp] Lightning Dragon passive failed on damage: {}", t.toString());
        }
    }

    /** Once a second per player: sprinting is quickened, a thunderstorm feeds. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !p.isAlive()) return;
            quicken(p);
            feed(p);
        } catch (Throwable t) {
            LOG.warn("[nusmp] Lightning Dragon passive failed on tick: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------ Eating Lightning
    /** True when {@code src} is lightning in the sense of this passive: the game's lightning and Tensura's lightning magic and thunder breath. */
    static boolean isLightning(DamageSource src) {
        return src.is(DamageTypeTags.IS_LIGHTNING) || src.is(TensuraDamageTypes.LIGHTNING_ELEMENTAL) || src.is(TensuraDamageTypes.LIGHTNING) || src.is(TensuraDamageTypes.THUNDER_BREATH);
    }

    /**
     * Eating Lightning, on damage: lightning that would hurt {@code p} feeds them instead. Returns true when the damage must be cancelled; the heal and the magicule refill are
     * applied here, at most once a second. Lightning the player made themselves is not eaten (canon).
     */
    public static boolean eat(ServerPlayer p, DamageSource src, float amount) {
        if (!isLightning(src) || src.getEntity() == p || !has(p, LightningDragonNumbers.THUNDER_EATER)) return false;
        int lv = levelOf(p, LightningDragonNumbers.THUNDER_EATER);
        CompoundTag d = p.getPersistentData();
        long now = p.serverLevel().getGameTime();
        if (now >= d.getLong(EAT_UNTIL)) {
            d.putLong(EAT_UNTIL, now + 20L);
            AbilityKit.heal(p, (float) (amount * num(LightningDragonNumbers.THUNDER_EATER, "eat_heal_ratio", lv)));
            DragonKit.gainMagicule(p, num(LightningDragonNumbers.THUNDER_EATER, "eat_recharge_pct", lv) / 100.0);
            Vec3 at = p.position().add(0.0, 0.9, 0.0);
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.LIGHTNING_FIEND, p, at, 0, 12, 0.9f);
        }
        return true;
    }

    /** Eating Lightning, once a second: a player standing in the open under a thunderstorm is fed a little. */
    static void feed(ServerPlayer p) {
        if (!has(p, LightningDragonNumbers.THUNDER_EATER)) return;
        ServerLevel level = p.serverLevel();
        if (!level.isThundering() || !level.canSeeSky(p.blockPosition())) return;
        int lv = levelOf(p, LightningDragonNumbers.THUNDER_EATER);
        if (p.getHealth() < p.getMaxHealth()) AbilityKit.heal(p, (float) (p.getMaxHealth() * num(LightningDragonNumbers.THUNDER_EATER, "heal_pct", lv) / 100.0));
        DragonKit.gainMagicule(p, num(LightningDragonNumbers.THUNDER_EATER, "recharge_pct", lv) / 100.0);
        Vec3 at = p.position().add(0.0, 0.9, 0.0);
        VfxSpawn.sendFollowing(level, VfxShape.LIGHTNING_FIEND, p, at, 0, 14, 0.8f);
    }

    // ------------------------------------------------------------------ Lightning Body
    /**
     * Lightning Body, on damage: a blow from an attacker may pass through the body. {@code roll} is a number in [0, 100) (the caller rolls it; the self-test forces it): the blow is
     * avoided when it is under the node's chance, and at most once per the node's interval. Returns true when the damage must be cancelled.
     */
    public static boolean evade(ServerPlayer p, DamageSource src, double roll) {
        if (src.getEntity() == null || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !has(p, LightningDragonNumbers.LIGHTNING_BODY)) return false;
        int lv = levelOf(p, LightningDragonNumbers.LIGHTNING_BODY);
        if (roll >= num(LightningDragonNumbers.LIGHTNING_BODY, "evade_pct", lv)) return false;
        CompoundTag d = p.getPersistentData();
        long now = p.serverLevel().getGameTime();
        if (now < d.getLong(EVADE_UNTIL)) return false;
        d.putLong(EVADE_UNTIL, now + Math.max(20L, Math.round(num(LightningDragonNumbers.LIGHTNING_BODY, "evade_icd", lv))));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.LIGHTNING_FIEND, p, p.position().add(0.0, 0.9, 0.0), 0, 10, 1.1f);
        return true;
    }

    /** Lightning Body, once a second: a sprinting player is quickened for the next two seconds (a vanilla Speed effect with no icon and no particles). */
    static void quicken(ServerPlayer p) {
        if (!p.isSprinting() || !has(p, LightningDragonNumbers.LIGHTNING_BODY)) return;
        int amp = (int) Math.round(num(LightningDragonNumbers.LIGHTNING_BODY, "speed_amp", levelOf(p, LightningDragonNumbers.LIGHTNING_BODY)));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, amp, true, false, false));
    }
}
