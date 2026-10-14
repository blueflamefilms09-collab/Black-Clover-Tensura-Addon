package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Wave 11 gameplay hooks: the damage rules of the blades (Wavebreaker's reflection, the Tripolar mood, the heated Heart of the Flame, Sorrow's Edge,
 * Flamecutter's fire), the staggered tick (the daily rolls, flight, walking on lava, cooling blades), the geas of Sorrow's Edge and Bianca's volley.
 * State lives in the stacks and the player's persistent data as absolute game times, so nothing static holds a player or a level.
 */
public final class Wave11Events {
    private Wave11Events() {}

    private static final String K_FLY = "nusmp_w11_fly", K_GEAS = "nusmp_w11_geas", K_GEAS_DAY = "nusmp_w11_geas_day", K_GEAS_DUE = "nusmp_w11_geas_due",
            K_GEAS_SAT = "nusmp_w11_geas_sat", K_EXTRA = "nusmp_w11_extra";

    static String heldId(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        if (s.isEmpty()) return "";
        RelicDef d = Relics.defOf(s);
        return d == null ? "" : d.id();
    }

    // ================================================================ damage
    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        long now = victim.level().getGameTime();

        if (victim instanceof ServerPlayer vp && e.getAmount() > 0f && src.getEntity() instanceof LivingEntity foe && foe != vp
                && Wave11Acts.cloneActive(vp) && RelicFlags.has(vp, Wave11.F_WAVE)) {
            CompoundTag pd = vp.getPersistentData();
            if (now - pd.getLong(Wave11Acts.K_REACT) >= 20) {                   // the clone's extra reaction: once a second it turns a blow aside
                pd.putLong(Wave11Acts.K_REACT, now);
                e.setAmount(e.getAmount() * 0.6f);
                pd.putUUID(Wave11Acts.K_RIPOSTE, foe.getUUID());                 // "advantage on the next attack against the attacker"
                pd.putLong(Wave11Acts.K_RIPOSTE_UNTIL, now + 100);
                ((ServerLevel) vp.level()).sendParticles(ParticleTypes.SPLASH, vp.getX(), vp.getY() + 1.0, vp.getZ(), 16, 0.4, 0.5, 0.4, 0.1);
            }
        }

        if (Wave11Acts.acting || !(src.getEntity() instanceof ServerPlayer atk)) return;
        if (!(src.getDirectEntity() == atk && !src.is(DamageTypeTags.IS_PROJECTILE))) return;
        ItemStack held = atk.getMainHandItem();
        String id = heldId(atk);
        if (id.isEmpty()) return;
        switch (id) {
            case "wavebreaker" -> {
                if (!RelicFlags.has(atk, Wave11.F_WAVE) || !Wave11Acts.cloneActive(atk)) return;
                CompoundTag ad = atk.getPersistentData();
                float amount = e.getAmount() + 2.0f;                              // the clone's 1d6 of cold
                if (ad.hasUUID(Wave11Acts.K_RIPOSTE) && ad.getUUID(Wave11Acts.K_RIPOSTE).equals(victim.getUUID()) && ad.getLong(Wave11Acts.K_RIPOSTE_UNTIL) >= now) {
                    ad.remove(Wave11Acts.K_RIPOSTE);
                    amount *= 1.25f;
                }
                e.setAmount(amount);
                Fx.addEffect(victim, "minecraft:slowness", 30, 0);
            }
            case "tripolar_sword" -> {
                if (!RelicFlags.has(atk, Wave11.F_TRIPOLAR)) return;
                if (Wave11Store.get(held, "day") != Wave11Pure.dayOf(now) + 1) return;
                int roll = (int) Wave11Store.get(held, "roll");
                e.setAmount(e.getAmount() * Wave11Pure.moodFactor(Wave11Pure.mood(roll)));
            }
            case "flamecutter" -> {
                if (!RelicFlags.has(atk, Wave11.F_FLAME)) return;
                e.setAmount(e.getAmount() + 2.0f);
                victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 60));
            }
            case "heart_of_the_flame" -> {
                if (!RelicFlags.has(atk, Wave11.F_HEART) || Wave11Store.get(held, "burn") == 0L || now - atk.getPersistentData().getLong(Wave11Acts.K_BURN_SEEN) > 60) return;
                e.setAmount(e.getAmount() + 4.0f);                                // the heated blade's 2d6 fire, shrunk
                victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), 80));
            }
            case "sorrows_edge" -> {
                if (!RelicFlags.has(atk, Wave11.F_SORROW)) return;
                e.setAmount(e.getAmount() + 2.5f);                                // 1d4 necrotic
                ((ServerLevel) victim.level()).sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1.0, victim.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
            }
            default -> { }
        }
    }

    // ================================================================ ticks
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        String held = heldId(p);
        if (held.equals("flamecutter") && RelicFlags.has(p, Wave11.F_FLAME)) lava(p);
        if (p.tickCount % 10 == 4) {
            flight(p, held.equals("windcutter") && RelicFlags.has(p, Wave11.F_WIND));
            if (Wave11Acts.cloneActive(p) && !held.equals("wavebreaker")) Wave11Acts.dismissClone(p);   // sheathing the blade dismisses the clone
        }
        if (p.tickCount % 20 != 7) return;
        long now = p.level().getGameTime();
        ItemStack s = p.getMainHandItem();
        switch (held) {
            case "tripolar_sword" -> {
                if (RelicFlags.has(p, Wave11.F_TRIPOLAR)) tripolar(p, s, now);
            }
            case "heart_of_the_flame" -> {
                if (Wave11Store.get(s, "burn") != 0L) {                                      // the heartbeat is kept on the player: a stack write each second would replay the equip animation
                    if (now - p.getPersistentData().getLong(Wave11Acts.K_BURN_SEEN) > 60) Wave11Store.put(s, "burn", 0L);   // cooled while sheathed
                    else p.getPersistentData().putLong(Wave11Acts.K_BURN_SEEN, now);
                }
            }
            case "sorrows_edge" -> {
                if (RelicFlags.has(p, Wave11.F_SORROW)) geasRoll(p, now);
            }
            case "wavebreaker" -> {
                if (RelicFlags.has(p, Wave11.F_WAVE) && p.isInWater()) Fx.addEffect(p, "minecraft:dolphins_grace", 60, 0);   // swim as fast as you walk
            }
            default -> { }
        }
        if (p.getPersistentData().contains(K_GEAS)) geasUpkeep(p, now);
    }

    // ---- Windcutter: "a flying speed equal to your walking speed, and you can hover"
    private static void flight(ServerPlayer p, boolean want) {
        if (p.isCreative() || p.isSpectator()) return;
        Abilities ab = p.getAbilities();
        CompoundTag pd = p.getPersistentData();
        if (want) {
            if (!ab.mayfly) {
                ab.mayfly = true;
                pd.putBoolean(K_FLY, true);
                p.onUpdateAbilities();
            }
        } else if (pd.getBoolean(K_FLY)) {
            pd.remove(K_FLY);
            ab.mayfly = false;
            ab.flying = false;
            p.onUpdateAbilities();
            Fx.addEffect(p, "minecraft:slow_falling", 200, 0);
        }
    }

    // ---- Flamecutter: "you can stand on and walk across molten rock as if it were solid ground"
    private static void lava(ServerPlayer p) {
        if (p.isShiftKeyDown()) return;                                           // sneaking lets you sink in
        ServerLevel l = p.serverLevel();
        Vec3 dm = p.getDeltaMovement();
        if (p.isInLava()) {
            boolean headIn = l.getFluidState(BlockPos.containing(p.getX(), p.getEyeY(), p.getZ())).is(FluidTags.LAVA);
            p.setDeltaMovement(dm.x, Math.max(dm.y, headIn ? 0.25 : 0.15), dm.z);
            p.hurtMarked = true;
            p.fallDistance = 0;
        } else if (dm.y <= 0.0) {
            BlockPos below = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
            FluidState fs = l.getFluidState(below);
            if (fs.is(FluidTags.LAVA) && below.getY() + fs.getHeight(l, below) >= p.getY() - 0.25) {
                p.setDeltaMovement(dm.x, 0.0, dm.z);
                p.setOnGround(true);
                p.fallDistance = 0;
            }
        }
    }

    // ---- Tripolar Sword: the daily d6
    private static void tripolar(ServerPlayer p, ItemStack s, long now) {
        long today = Wave11Pure.dayOf(now) + 1;                                  // stored as day + 1 so that 0 means "never rolled"
        if (Wave11Store.get(s, "day") == today) return;
        int d6 = 1 + p.getRandom().nextInt(6);
        Wave11Store.put(s, "day", today);
        Wave11Store.put(s, "roll", d6);
        switch (Wave11Pure.mood(d6)) {
            case LASHING -> {
                float lash = Wave11Pure.lash(1 + p.getRandom().nextInt(10), p.getHealth());
                if (lash > 0f) p.hurt(p.damageSources().magic(), lash);
                Fx.say(p, "The Tripolar Sword remembers its betrayal and lashes out at you (rolled " + d6 + "). It is clumsy until dawn.");
            }
            case FAVOURED -> Fx.say(p, "The Tripolar Sword sings in your hand (rolled " + d6 + "): it strikes hard until dawn.");
            default -> Fx.say(p, "The Tripolar Sword is a plain sword today (rolled " + d6 + ").");
        }
    }

    // ---- Sorrow's Edge: the geas
    private static void geasRoll(ServerPlayer p, long now) {
        CompoundTag pd = p.getPersistentData();
        long today = Wave11Pure.dayOf(now) + 1;
        if (pd.getLong(K_GEAS_DAY) == today) return;
        pd.putLong(K_GEAS_DAY, today);
        pd.remove(K_GEAS_SAT);
        int d100 = 1 + p.getRandom().nextInt(100);
        Wave11Pure.Demand d = Wave11Pure.geas(d100);
        if (d == Wave11Pure.Demand.NONE) {
            pd.remove(K_GEAS);
            Fx.say(p, "Sorrow's Edge whispers (rolled " + d100 + "), and you keep your mind.");
        } else if (d == Wave11Pure.Demand.BLOODLUST) {
            pd.putInt(K_GEAS, 1);
            pd.putLong(K_GEAS_DUE, now + Wave11Pure.BLOODLUST_TICKS);
            Fx.say(p, "Sorrow's Edge demands blood (rolled " + d100 + "): kill something within five minutes or suffer.");
        } else {
            pd.putInt(K_GEAS, 2);
            Fx.say(p, "Sorrow's Edge demands a fast (rolled " + d100 + "): eat nothing until dawn or be sickened.");
        }
    }

    private static void geasUpkeep(ServerPlayer p, long now) {
        CompoundTag pd = p.getPersistentData();
        if (pd.getLong(K_GEAS_DAY) != Wave11Pure.dayOf(now) + 1) {               // a new day: the geas is lifted until the axe is wielded again
            pd.remove(K_GEAS);
            return;
        }
        if (pd.getInt(K_GEAS) == 1 && Wave11Pure.bloodlustOverdue(now, pd.getLong(K_GEAS_DUE), pd.getBoolean(K_GEAS_SAT))) {
            pd.putBoolean(K_GEAS_SAT, true);
            float hurt = Math.max(0f, Math.min(4f, p.getHealth() - 1f));
            if (hurt > 0f) p.hurt(p.damageSources().magic(), hurt);
            Fx.addEffect(p, "minecraft:weakness", 600, 1);
            Fx.addEffect(p, "minecraft:slowness", 600, 0);
            Fx.say(p, "The spirit's thirst goes unslaked, and it punishes you.");
        }
    }

    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide() || !(e.getSource().getEntity() instanceof ServerPlayer p) || e.getEntity() == p) return;
        CompoundTag pd = p.getPersistentData();
        if (pd.getInt(K_GEAS) == 1 && !pd.getBoolean(K_GEAS_SAT)) {
            pd.putBoolean(K_GEAS_SAT, true);
            Fx.say(p, "Sorrow's Edge drinks, and is quiet for now.");
        }
    }

    static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide()) return;
        CompoundTag pd = p.getPersistentData();
        if (pd.getInt(K_GEAS) != 2 || pd.getLong(K_GEAS_DAY) != Wave11Pure.dayOf(p.level().getGameTime()) + 1) return;
        if (!e.getItem().has(DataComponents.FOOD)) return;
        Fx.addEffect(p, "minecraft:nausea", 200, 0);
        Fx.addEffect(p, "minecraft:hunger", 200, 1);
        Fx.say(p, "You broke the fast and the spirit sickens you.");
    }

    // ================================================================ Bianca: three bolts in a single attack
    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof AbstractArrow a) || a.getPersistentData().getBoolean(K_EXTRA)) return;
        if (!(a.getOwner() instanceof ServerPlayer p) || !heldId(p).equals("bianca") || !RelicFlags.has(p, Wave11.F_BIANCA)) return;
        ItemStack s = p.getMainHandItem();
        int drum = (int) Wave11Store.get(s, "drum");
        if (!Wave11Pure.canVolley(drum)) return;                                  // a part-empty drum fires one plain bolt
        Wave11Store.put(s, "drum", Wave11Pure.afterVolley(drum));
        boolean refund = a.pickup == AbstractArrow.Pickup.ALLOWED && !p.isCreative();     // an arrow that was not really spent (infinite ammo) is not refunded
        a.pickup = AbstractArrow.Pickup.DISALLOWED;
        if (refund) p.getInventory().add(new ItemStack(Items.ARROW));             // the drum fed this attack: the bolt the crossbow took comes back
        for (float deg : Wave11Pure.SPREAD) {
            Entity made = a.getType().create(a.level());
            if (!(made instanceof AbstractArrow b)) continue;
            b.setOwner(p);
            b.moveTo(a.getX(), a.getY(), a.getZ(), a.getYRot(), a.getXRot());
            b.setDeltaMovement(a.getDeltaMovement().yRot((float) Math.toRadians(deg)));
            b.setBaseDamage(a.getBaseDamage());
            b.setCritArrow(a.isCritArrow());
            b.pickup = AbstractArrow.Pickup.DISALLOWED;
            b.getPersistentData().putBoolean(K_EXTRA, true);
            a.level().addFreshEntity(b);
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave11Acts.dismissClone(p);
    }
}
