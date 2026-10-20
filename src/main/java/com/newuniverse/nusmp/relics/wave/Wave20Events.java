package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the damage, death and tick rules of the twelve weapons of wave 20. Extra damage is added to the hit that is being dealt (never a second
 * {@code hurt} inside the first, which the invulnerability frames would swallow); the area and counter blows that are separate hits run with {@link #busy}
 * set, so they cannot trigger the rules again. Every timer lives in the player's persistent data; nothing here is static state.
 */
final class Wave20Events {
    private Wave20Events() {}

    private static boolean busy;

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wave20Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave20Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave20Events::onPlayerTick);
    }

    // ================================================================ damage
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (busy) return;
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        if (victim instanceof ServerPlayer vp) {
            if (defend(e, vp, src)) return;
        }
        if (!(src.getEntity() instanceof ServerPlayer atk) || src.getDirectEntity() != atk || src.is(DamageTypeTags.IS_PROJECTILE)) return;
        String id = Wave20Util.heldId(atk);
        if (id.isEmpty()) return;
        ServerLevel level = atk.serverLevel();
        long now = level.getGameTime();
        switch (id) {
            case "demon_shovel" -> {
                if (!RelicFlags.has(atk, "w20_shovel")) return;
                e.setAmount(e.getAmount() + 3.5f);                                               // 1d6 necrotic
                Fx.addEffect(victim, "minecraft:wither", 40, 0);
                com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_20, FxTone.DARK, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 5, 0.25, 0.3, 0.25, 0.02);
            }
            case "dragoons_halberd" -> halberd(e, atk, victim, level);
            case "efreetis_kusarigama" -> {
                if (!RelicFlags.has(atk, "w20_kusarigama")) return;
                e.setAmount(e.getAmount() + 2.0f);                                               // +2 fire
                victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 40));
            }
            case "ilmare_gin_the_starlight_silver" -> {
                if (!RelicFlags.has(atk, "w20_gin")) return;
                if (atk.getOffhandItem().isEmpty()) e.setAmount(e.getAmount() + 1.5f);          // versatile: two hands
            }
            case "katana_of_carnage" -> carnage(e, atk, victim, level, now);
            case "mjolnir" -> {
                if (!RelicFlags.has(atk, "w20_mjolnir")) return;
                if (victim.isInWaterOrRain()) {                                                  // a wet foe conducts the hammer's lightning
                    e.setAmount(e.getAmount() + 3.0f);
                    com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_20, FxTone.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 6, 0.3, 0.5, 0.3, 0.2);
                }
            }
            case "phoenix_flame" -> {
                if (!RelicFlags.has(atk, "w20_phoenix") || !atk.getPersistentData().getBoolean(Wave20Acts.K_LIT)) return;
                if (victim.fireImmune()) return;
                e.setAmount(e.getAmount() + Wave20Rules.BLAZE_DAMAGE);                          // 2d6 fire while ablaze
                victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 100));
                com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_20, FxTone.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 8, 0.3, 0.5, 0.3, 0.08);
            }
            case "primal_chaos" -> {
                if (!RelicFlags.has(atk, "w20_chaos")) return;
                float add = Wave20Rules.RADIANT_DAMAGE;                                          // 1d4 radiant
                if (Wave20Util.isVoid(victim)) {
                    add += Wave20Rules.VOID_BONUS;                                               // forged to destroy the creatures of the void
                    Fx.addEffect(victim, "tensura:spatial_blockade", 40, 0);
                }
                e.setAmount(e.getAmount() + add);
                com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_20, FxTone.LIGHT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 4, 0.2, 0.3, 0.2, 0.05);
            }
            case "lightworkers_scepter" -> {
                if (!RelicFlags.has(atk, "w20_lightworker")) return;
                if (victim.getType().is(EntityTypeTags.UNDEAD)) {
                    e.setAmount(e.getAmount() + 4.0f);                                           // radiant against the undead
                    com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_20, FxTone.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 6, 0.3, 0.5, 0.3, 0.05);
                }
            }
            default -> { }
        }
    }

    /** The wielder is the one being hit: deflection, the hovering halberd, the Scepter's ward and soulgate. True if the blow was cancelled. */
    private static boolean defend(LivingIncomingDamageEvent e, ServerPlayer vp, DamageSource src) {
        ServerLevel level = vp.serverLevel();
        long now = level.getGameTime();
        CompoundTag pd = vp.getPersistentData();
        LivingEntity attacker = src.getEntity() instanceof LivingEntity a && src.getDirectEntity() == a && !src.is(DamageTypeTags.IS_PROJECTILE) ? a : null;
        if (attacker != null && attacker != vp) {
            if (RelicFlags.has(vp, "w20_shogun") && Wave20Util.heldId(vp).equals("katana_of_the_crimson_shogun")
                    && Wave20Rules.deflects(vp.getRandom().nextDouble(), now, pd.getLong("nusmp_w20_deflect_at"))) {
                pd.putLong("nusmp_w20_deflect_at", now);
                e.setCanceled(true);                                                             // the blow is turned aside...
                busy = true;
                try {
                    Fx.damage(vp, attacker, Wave20Rules.COUNTER_DAMAGE, "minecraft:player_attack");    // ...and answered as a normal attack
                } finally {
                    busy = false;
                }
                com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_20, vp.position().add(0, 1.2, 0), FxTone.LIGHT, 8);
                Fx.say(vp, "Deflection: the folded steel turns the blow and answers it.");
                return true;
            }
            if (pd.getLong(Wave20Acts.K_HOVER) > now && Wave20Rules.hoverStrikes(now, pd.getLong(Wave20Acts.K_HOVER), pd.getLong("nusmp_w20_hover_at"), vp.distanceTo(attacker)) && carries(vp, "overprotective_halberd")) {
                pd.putLong("nusmp_w20_hover_at", now);
                busy = true;
                try {
                    Fx.damage(vp, attacker, 8.0f, "minecraft:player_attack");                   // the halberd attacks as if you were wielding it
                } finally {
                    busy = false;
                }
                com.newuniverse.nusmp.particle.fx.Fx.stream(level, FxThemes.RELIC_WAVE_20, vp.position().add(0, 1.5, 0), attacker.position().add(0, 1.0, 0), FxTone.LIGHT, 6);
                level.playSound(null, vp.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f);
            }
        }
        if (RelicFlags.has(vp, "w20_lightworker")) {
            var bag = Wave20Store.playerBag(pd);
            if (Wave20Rules.soulgateSaves(vp.getHealth(), e.getAmount(), Wave20Rules.spentToday(bag, "soulgate", now) > 0)) {
                Wave20Rules.spend(bag, "soulgate", now, 1);
                e.setCanceled(true);                                                             // Soulgate: out of the blow's way, onto the plane of life
                vp.setHealth(Wave20Rules.SOULGATE_HEALTH);
                Fx.addEffect(vp, "minecraft:resistance", 80, 4);
                Fx.addEffect(vp, "minecraft:regeneration", 120, 2);
                FxRelic.ring(level, vp.position(), 3.0, 0xEAF8FF);
                Fx.say(vp, "Soulgate: the voice pulls you through the plane of life, and the killing blow finds no one. Once a day.");
                return true;
            }
            DamageSource s = e.getSource();
            boolean magical = s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC) || s.is(DamageTypes.WITHER) || s.is(DamageTypes.DRAGON_BREATH) || s.is(DamageTypes.SONIC_BOOM);
            if (magical) {
                int gain = Wave20Rules.wardGain(Wave20Rules.spentToday(bag, "ward", now));
                if (gain > 0) {
                    Wave20Rules.spend(bag, "ward", now, gain);
                    Fx.heal(vp, gain);
                }
            }
        }
        return false;
    }

    private static boolean carries(ServerPlayer p, String relicId) {
        for (ItemStack s : p.getInventory().items) if (!s.isEmpty() && Relics.of(s).map(RelicDef::id).orElse("").equals(relicId)) return true;
        return false;
    }

    private static void halberd(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, ServerLevel level) {
        if (!RelicFlags.has(atk, "w20_dragoon")) return;
        if (Wave20Util.isDraconic(victim)) e.setAmount(e.getAmount() + Wave20Rules.DRAGON_BONUS);
        if (!Wave20Rules.isDropAttack(atk.fallDistance, atk.onGround())) return;
        Wave20Rules.Element el = Wave20Rules.Element.of(Wave20Store.bag(atk.getMainHandItem()).get("elem"));
        busy = true;
        try {
            for (LivingEntity t : Wave20Util.foesNear(atk, victim.position(), Wave20Rules.DROP_RADIUS)) {
                Fx.damage(atk, t, Wave20Rules.DROP_DAMAGE, el.damageType());
                switch (el) {
                    case FIRE -> t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 60));
                    case FROST -> {
                        Fx.addEffect(t, "tensura:frost", 60, 0);
                        Fx.addEffect(t, "minecraft:slowness", 60, 1);
                    }
                    case LIGHTNING -> Fx.addEffect(t, "tensura:paralysis", 20, 0);
                    case WIND -> {
                        t.setDeltaMovement(t.getDeltaMovement().add(0, 0.5, 0));
                        t.hurtMarked = true;
                    }
                }
            }
        } finally {
            busy = false;
        }
        FxRelic.ring(level, victim.position(), Wave20Rules.DROP_RADIUS, el == Wave20Rules.Element.FROST ? 0xBFE8FF : el == Wave20Rules.Element.FIRE ? 0xFF8A30 : 0xFFF0A0);
    }

    private static void carnage(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, ServerLevel level, long now) {
        if (!RelicFlags.has(atk, "w20_carnage")) return;
        CompoundTag pd = atk.getPersistentData();
        int stacks = Wave20Rules.carnageNow(pd.getInt(Wave20Acts.K_CARN_N), pd.getLong(Wave20Acts.K_CARN_AT), now);
        float add = Wave20Rules.carnageBonus(stacks);
        if (Wave20Rules.severs(atk.getRandom().nextDouble())) {                                  // of sharpness: a severing cut
            add += e.getAmount() * 0.4f;
            Fx.addEffect(victim, "minecraft:poison", 80, 1);
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_20, FxTone.DARK, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
        }
        if (add > 0) e.setAmount(e.getAmount() + add);
    }

    // ================================================================ kills
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide()) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer killer) || !Wave20Util.heldId(killer).equals("katana_of_carnage") || !RelicFlags.has(killer, "w20_carnage")) return;
        CompoundTag pd = killer.getPersistentData();
        long now = killer.level().getGameTime();
        int stacks = Wave20Rules.carnageAfterKill(pd.getInt(Wave20Acts.K_CARN_N), pd.getLong(Wave20Acts.K_CARN_AT), now);
        pd.putInt(Wave20Acts.K_CARN_N, stacks);
        pd.putLong(Wave20Acts.K_CARN_AT, now);
        com.newuniverse.nusmp.particle.fx.Fx.burst(killer.serverLevel(), FxThemes.RELIC_WAVE_20, e.getEntity(), FxTone.DARK, 6);
        if (stacks >= Wave20Rules.CARNAGE_MAX) Fx.heal(killer, 2.0f);
        Fx.say(killer, "Carnage x" + stacks + ".");
    }

    // ================================================================ ticks
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int t = p.tickCount;
        if (t % 10 != 3) return;
        CompoundTag pd = p.getPersistentData();
        long now = p.level().getGameTime();
        String held = Wave20Util.heldId(p);
        if (pd.contains(Wave20Acts.K_GRAP_UNTIL)) grapple(p, pd, held, now);
        if (pd.getBoolean(Wave20Acts.K_LIT)) {                                                   // the flames last until the command word, a drop or a stow
            if (!held.equals("phoenix_flame")) {
                pd.putBoolean(Wave20Acts.K_LIT, false);
                Fx.say(p, "The phoenix's flames die as the halberd leaves your hand.");
            } else {
                Fx.addEffect(p, "minecraft:night_vision", 260, 0);
                com.newuniverse.nusmp.particle.fx.Fx.aura(p.serverLevel(), FxThemes.RELIC_WAVE_20, p, FxTone.LIGHT);
            }
        }
        if (pd.getLong(Wave20Acts.K_HOVER) > now && t % 20 == 3) com.newuniverse.nusmp.particle.fx.Fx.aura(p.serverLevel(), FxThemes.RELIC_WAVE_20, p, FxTone.LIGHT);
        if (RelicFlags.has(p, "w20_lightworker")) {                                              // resistance to psychic damage: the mind's afflictions slide off
            for (String id : new String[]{"tensura:fear", "tensura:confusion", "tensura:insanity", "tensura:hypnosis"}) Wave20Util.removeEffect(p, id);
        }
        if (pd.getLong(Wave20Acts.K_AURA) > now && t % 20 == 3) pulseAura(p);
    }

    /** Pulls a grappled foe towards the wielder; ends the grapple when it is over, out of reach, or the kusarigama is no longer held. */
    private static void grapple(ServerPlayer p, CompoundTag pd, String held, long now) {
        ServerLevel level = p.serverLevel();
        LivingEntity t = pd.hasUUID(Wave20Acts.K_GRAP_T) && level.getEntity(pd.getUUID(Wave20Acts.K_GRAP_T)) instanceof LivingEntity le ? le : null;
        boolean over = t == null || !t.isAlive() || Wave20Rules.grappleOver(now, pd.getLong(Wave20Acts.K_GRAP_UNTIL)) || !held.equals("efreetis_kusarigama") || t.distanceTo(p) > 14.0f;
        if (over) {
            pd.remove(Wave20Acts.K_GRAP_UNTIL);
            pd.remove(Wave20Acts.K_GRAP_NEXT);
            pd.remove(Wave20Acts.K_GRAP_T);
            return;
        }
        boolean big = Wave20Util.isBig(t);
        Fx.addEffect(t, "minecraft:slowness", 30, big ? 1 : 5);
        if (!big) pull(p, t);
        com.newuniverse.nusmp.particle.fx.Fx.stream(level, FxThemes.RELIC_WAVE_20, p, t, FxTone.LIGHT, 3);
        if (Wave20Rules.grappleDamageDue(now, pd.getLong(Wave20Acts.K_GRAP_NEXT))) {
            pd.putLong(Wave20Acts.K_GRAP_NEXT, now + Wave20Rules.GRAPPLE_STEP);
            Fx.damage(p, t, Wave20Rules.GRAPPLE_DAMAGE, "minecraft:in_fire");
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 30));
        }
    }

    /** Draws the foe towards the wielder until it is three blocks away. */
    static void pull(ServerPlayer p, LivingEntity t) {
        if (Wave20Util.isBig(t)) return;
        Vec3 to = p.position().subtract(t.position());
        double d = to.length();
        if (d <= 3.0) return;
        Vec3 dir = to.scale(1.0 / d);
        t.setDeltaMovement(dir.x * 0.6, Math.max(t.getDeltaMovement().y, 0.1), dir.z * 0.6);
        t.hurtMarked = true;
        if (t instanceof Mob m) m.getNavigation().stop();
    }

    /** One pulse of the Scepter's Soothing Presence: friends within six blocks mend and calm, hostile mobs lose their anger. */
    static void pulseAura(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6.0), x -> x.isAlive() && !x.isSpectator());
        int n = 0;
        for (LivingEntity t : near) {
            if (n++ >= 24) break;
            if (Wave20Util.friend(p, t)) {
                Fx.addEffect(t, "minecraft:regeneration", 60, 0);
                Wave20Util.removeEffect(t, "tensura:fear");
            } else if (t instanceof Mob m && !Wave20Util.isBig(m) && m.getTarget() != null) {
                m.setTarget(null);
                Fx.addEffect(m, "minecraft:weakness", 60, 0);
            }
        }
        com.newuniverse.nusmp.particle.fx.Fx.aura(level, FxThemes.RELIC_WAVE_20, p, FxTone.LIGHT);
    }
}
