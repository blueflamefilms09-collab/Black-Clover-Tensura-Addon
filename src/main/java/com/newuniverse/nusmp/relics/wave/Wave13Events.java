package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * Wave 13: the damage, critical-hit and tick rules of the nine blades. Extra damage is added to the hit that is being dealt (never a second
 * {@code hurt} inside the first, which the invulnerability frames would swallow); only the Eschaton's second strike is a separate hit, six ticks
 * later, and {@link #busy} keeps it from triggering the rules again.
 */
final class Wave13Events {
    private Wave13Events() {}

    private static final ResourceLocation REACH_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w13_whip_reach");
    private static final ResourceLocation SAINT_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w13_saint_blood");
    private static final String K_CRIT = "nusmp_w13_crit", K_S2_AT = "nusmp_w13_s2_at", K_S2_T = "nusmp_w13_s2_t", K_S2_D = "nusmp_w13_s2_d", K_MAG = "nusmp_w13_mag", K_WIELD = "nusmp_w13_wield_acc";

    private static boolean busy;

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wave13Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave13Events::onCrit);
        NeoForge.EVENT_BUS.addListener(Wave13Events::onPlayerTick);
    }

    // ================================================================ damage
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (busy) return;
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer atk) || src.getDirectEntity() != atk || src.is(DamageTypeTags.IS_PROJECTILE)) return;
        String id = Wave13Util.heldId(atk);
        if (id.isEmpty()) return;
        ItemStack held = atk.getMainHandItem();
        long now = atk.level().getGameTime();
        ServerLevel level = atk.serverLevel();
        switch (id) {
            case "dream_glass" -> {
                if (!RelicFlags.has(atk, "w13_dream_glass")) return;
                var bag = Wave13Store.bag(held);
                if (bag.get("eth_until") > now) {                                    // ethereal form: worn armour does not count
                    AttributeInstance armor = victim.getAttribute(Attributes.ARMOR);
                    if (armor != null) {
                        double tough = victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                        e.setAmount((float) Wave13Rules.bypassArmour(e.getAmount(), armor.getValue(), armor.getBaseValue(), tough));
                        RukhFx.spray(level, RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 6, 0.25, 0.4, 0.25, 0.02);
                    }
                }
                CompoundTag pd = atk.getPersistentData();
                if (pd.getLong(Wave13Acts.K_DRAW) >= now) {                          // a blade drawn out of nothing
                    pd.remove(Wave13Acts.K_DRAW);
                    e.setAmount(e.getAmount() + 4.0f);
                }
            }
            case "axe_of_the_crimson_saint" -> {
                if (!RelicFlags.has(atk, "w13_saint")) return;
                int stage = Wave13Rules.stage(Wave13Store.bag(held), now);
                float add = (float) Wave13Rules.stageDamage(stage);
                if (Wave13Rules.saintCrit(stage, atk.getRandom().nextDouble())) {
                    add += e.getAmount() * 0.5f;
                    RukhFx.spray(level, RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 14, 0.3, 0.4, 0.3, 0.2);
                }
                if (add > 0) e.setAmount(e.getAmount() + add);
            }
            case "palladion" -> palladion(e, atk, victim, now);
            case "dagger_of_betrayal" -> dagger(e, atk, victim, level);
            case "humbling_longsword" -> {
                if (!RelicFlags.has(atk, "w13_humbling")) return;
                if (atk.getOffhandItem().isEmpty()) e.setAmount(e.getAmount() + 1.5f);          // versatile: two hands
                if (Wave13Rules.surrenders(victim.getHealth() - e.getAmount() * 0.6, victim.getMaxHealth())) Wave13Util.surrender(victim);
            }
            case "eschaton" -> eschaton(e, atk, victim, held, now, level);
            default -> { }
        }
    }

    private static void palladion(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, long now) {
        if (!RelicFlags.has(atk, "w13_palladion")) return;
        CompoundTag pd = atk.getPersistentData();
        if (victim instanceof AbstractVillager || victim instanceof IronGolem || (victim instanceof OwnableEntity o && o.getOwnerUUID() != null)) {
            pd.putLong(Wave13Acts.K_FALLEN, now + 6000);                                       // forged to serve the good: harming the innocent dims it
            Fx.say(atk, "Palladion dims. It was forged to serve the good.");
            return;
        }
        if (pd.getLong(Wave13Acts.K_FALLEN) > now) return;
        if (!Wave13Util.otherworldly(victim)) return;
        e.setAmount(e.getAmount() + 6.0f);                                                      // 2d6 force
        if (pd.getLong(K_CRIT) == now && pd.getLong(Wave13Acts.K_BANISH_CD) <= now) {         // a natural 20: Banishing Strike
            pd.putLong(Wave13Acts.K_BANISH_CD, now + 900);
            Wave13Util.banish(atk, victim, 100);
            Fx.say(atk, "\"Begone!\"");
        }
    }

    private static void dagger(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, ServerLevel level) {
        if (!RelicFlags.has(atk, "w13_betrayal")) return;
        Vec3 to = atk.getEyePosition().subtract(victim.getEyePosition());
        if (to.lengthSqr() > 1.0e-4 && Wave13Rules.eyeContact(victim.getViewVector(1.0f).dot(to.normalize()))) {
            e.setAmount(e.getAmount() + 4.0f);                                                  // 1d8 necrotic for the one who looks into the eyes
            RukhFx.spray(level, RukhFx.Alignment.DARK, victim.getX(), victim.getEyeY(), victim.getZ(), 4, 0.15, 0.1, 0.15, 0.02);
        }
        if (Wave13Util.considersAlly(atk, victim)) {                                           // 3d8 against one who trusted you
            float extra = 13.0f;
            if (victim instanceof net.minecraft.world.entity.player.Player) extra = (float) Math.min(extra, victim.getMaxHealth() * 0.3);
            e.setAmount(e.getAmount() + extra);
            RukhFx.spray(level, RukhFx.Alignment.DARK, victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.3, 0.5, 0.3, 0.03);
            if (!Wave13Rules.saveHolds(atk.getRandom().nextDouble())) {                        // the Constitution save failed: disadvantage for the next two rolls
                Fx.addEffect(atk, "minecraft:weakness", 160, 0);
                Fx.addEffect(atk, "minecraft:mining_fatigue", 160, 0);
                Fx.say(atk, "The betrayal sours in your hands.");
            }
        }
    }

    private static void eschaton(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, ItemStack held, long now, ServerLevel level) {
        if (!RelicFlags.has(atk, "w13_eschaton")) return;
        int tier = Wave13Rules.tier(Wave13Rules.charge(Wave13Store.bag(held), now));
        if (tier < 1) return;
        float add = 2.5f;                                                                       // 5: the spellcasting modifier replaces Strength
        if (tier >= 2) add += 1.5f;                                                             // 15: the whip, +1
        if (tier >= 4) add += 4.5f;                                                             // 50: +1d8 force, +2
        e.setAmount(e.getAmount() + add);
        if (tier >= 3) {                                                                        // 30: attack twice
            CompoundTag pd = atk.getPersistentData();
            pd.putLong(K_S2_AT, now + 6);
            pd.putUUID(K_S2_T, victim.getUUID());
            pd.putFloat(K_S2_D, e.getAmount() * 0.6f);
        }
        if (tier >= 5) {                                                                        // 75: a cantrip at the target, with advantage
            int pick = (int) ((now / 20) % 3);
            if (pick == 0) {
                victim.setRemainingFireTicks(60);
                RukhFx.spray(level, RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 12, 0.3, 0.4, 0.3, 0.03);
            } else if (pick == 1) {
                Fx.addEffect(victim, "minecraft:slowness", 60, 1);
                Fx.addEffect(victim, "tensura:frost", 60, 0);
                RukhFx.spray(level, RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 14, 0.3, 0.4, 0.3, 0.02);
            } else {
                Fx.addEffect(victim, "minecraft:weakness", 60, 0);
                RukhFx.spray(level, RukhFx.Alignment.LIGHT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 16, 0.3, 0.5, 0.3, 0.3);
            }
            e.setAmount(e.getAmount() + 2.0f);
            Fx.addEffect(victim, "tensura:magic_interference", 80, 0);                          // disadvantage on its saving throws
            Fx.addEffect(victim, "minecraft:glowing", 80, 0);                                   // advantage on the attacks
        }
    }

    // ================================================================ critical hits
    static void onCrit(CriticalHitEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !e.isCriticalHit()) return;
        if (Wave13Util.heldId(p).equals("palladion")) p.getPersistentData().putLong(K_CRIT, p.level().getGameTime());
    }

    // ================================================================ ticks
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        CompoundTag pd = p.getPersistentData();
        if (pd.contains(K_S2_AT)) secondStrike(p, pd);
        int t = p.tickCount;
        if (t % 10 != 3) return;
        long now = p.level().getGameTime();
        String held = Wave13Util.heldId(p);
        applyCurse(p, held, now);
        applyWhip(p, held, now);
        if (pd.getLong(Wave13Acts.K_AURA) > now && t % 20 == 3) pulseAura(p);
        if (held.equals("eschaton")) sampleMagicules(p, now);
        else if (pd.contains(K_MAG)) pd.remove(K_MAG);                                          // what is spent while it is sheathed does not charge it
        if (t % 20 == 3) perSecond(p, held, now);
        if (t % 10 == 3) contingency(p, now);
    }

    private static void secondStrike(ServerPlayer p, CompoundTag pd) {
        long now = p.level().getGameTime();
        long at = pd.getLong(K_S2_AT);
        if (now < at) return;
        UUID u = pd.hasUUID(K_S2_T) ? pd.getUUID(K_S2_T) : null;
        float dmg = pd.getFloat(K_S2_D);
        pd.remove(K_S2_AT);
        pd.remove(K_S2_T);
        pd.remove(K_S2_D);
        if (u == null || now - at > 40 || !Wave13Util.heldId(p).equals("eschaton")) return;
        if (p.serverLevel().getEntity(u) instanceof LivingEntity t && t.isAlive() && t.distanceToSqr(p) < 64.0) {
            t.invulnerableTime = 0;
            busy = true;
            try {
                Fx.damage(p, t, dmg, "minecraft:magic");
            } finally {
                busy = false;
            }
            RukhFx.spray(p.serverLevel(), RukhFx.Alignment.LIGHT, t.getX(), t.getY() + 1.0, t.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
        }
    }

    /** The Saint's price: each offer takes hearts from the wielder's maximum health, never below three hearts. */
    private static void applyCurse(ServerPlayer p, String held, long now) {
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return;
        double want = 0.0;
        if (held.equals("axe_of_the_crimson_saint") && RelicFlags.has(p, "w13_saint")) {
            var bag = Wave13Store.bag(p.getMainHandItem());
            if (!Wave13Rules.askedToday(bag, now)) {
                Wave13Rules.setStage(bag, now, 0);
                Fx.say(p, "\"Quante mula sant vene ad sacrifico?\" How much are you willing to sacrifice? (use: offer a heart, sneak + hold: everything)");
            }
            want = Wave13Rules.sacrificeModifier(Wave13Rules.stage(bag, now), hp.getBaseValue());
        }
        AttributeModifier cur = hp.getModifier(SAINT_ID);
        if (want >= 0.0) {
            if (cur != null) hp.removeModifier(SAINT_ID);
            return;
        }
        if (cur != null && Math.abs(cur.amount() - want) < 1.0e-6) return;
        hp.removeModifier(SAINT_ID);
        hp.addTransientModifier(new AttributeModifier(SAINT_ID, want, AttributeModifier.Operation.ADD_VALUE));
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** From 15 charges the Eschaton is a whip: three blocks more reach. */
    private static void applyWhip(ServerPlayer p, String held, long now) {
        AttributeInstance reach = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach == null) return;
        boolean want = held.equals("eschaton") && RelicFlags.has(p, "w13_eschaton") && Wave13Rules.tier(Wave13Rules.charge(Wave13Store.bag(p.getMainHandItem()), now)) >= 2;
        boolean has = reach.hasModifier(REACH_ID);
        if (want && !has) reach.addTransientModifier(new AttributeModifier(REACH_ID, 3.0, AttributeModifier.Operation.ADD_VALUE));
        else if (!want && has) reach.removeModifier(REACH_ID);
    }

    /** The Eschaton charges with the magicules the wielder spends: the Tensura storage is read and the fall since the last look is the cost. */
    private static void sampleMagicules(ServerPlayer p, long now) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        double max = EnergyHelper.getMaxMagicule(p), cur = ex.getMagicule();
        CompoundTag pd = p.getPersistentData();
        double last = pd.contains(K_MAG) ? pd.getDouble(K_MAG) : cur;
        pd.putDouble(K_MAG, cur);
        if (max <= 0 || last <= cur || !RelicFlags.has(p, "w13_eschaton")) return;
        long g = Wave13Rules.gain((last - cur) / max);
        if (g > 0) Wave13Rules.addCharge(Wave13Store.bag(p.getMainHandItem()), now, g);
    }

    private static void perSecond(ServerPlayer p, String held, long now) {
        if (!held.equals("palladion") && !held.equals("time_bender_dadao")) return;
        if (held.equals("time_bender_dadao") && RelicFlags.has(p, "w13_dadao")) {
            CompoundTag pd = p.getPersistentData();                                              // the time in hand is counted on the player and written to the blade every 30 s:
            int acc = pd.getInt(K_WIELD) + 20;                                                   // a stack write each second would replay the hand's equip animation
            if (acc < 600) {
                pd.putInt(K_WIELD, acc);
            } else {
                pd.putInt(K_WIELD, 0);
                var bag = Wave13Store.bag(p.getMainHandItem());                                  // the tiers open with the time it is carried in hand
                long before = bag.get("wield"), w = before + acc;
                bag.put("wield", w);
                if (before < Wave13Rules.TIER2_TICKS && w >= Wave13Rules.TIER2_TICKS || before < Wave13Rules.TIER3_TICKS && w >= Wave13Rules.TIER3_TICKS)
                    Fx.say(p, "The dadao opens tier " + Wave13Rules.timeTier(w) + ".");
            }
        }
        if (held.equals("palladion") && RelicFlags.has(p, "w13_palladion") && p.tickCount % 40 == 3) {   // Extraplanar Sentinel: it glows faintly near them
            if (!Wave13Util.otherworldlyNear(p, 32.0).isEmpty())
                RukhFx.spray(p.serverLevel(), RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.2, p.getZ(), 4, 0.4, 0.4, 0.4, 0.01);
        }
    }

    /** Last Line of Defense: every three seconds, allies near the wielder shed fear and get a temporary shield. */
    static void pulseAura(ServerPlayer p) {
        Vec3 c = p.position();
        boolean shield = p.tickCount % 60 < 20;
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(9.0),
                e -> e.isAlive() && e.distanceToSqr(c) <= 81.0 && Wave13Util.friend(p, e));
        int n = 0;
        for (LivingEntity a : near) {
            if (n++ >= 24) break;
            var fear = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", "fear"));
            if (fear.isPresent() && a.hasEffect(fear.get())) a.removeEffect(fear.get());
            if (shield && !a.hasEffect(MobEffects.ABSORPTION)) Fx.addEffect(a, "minecraft:absorption", 80, 2);
        }
    }

    /** Contingency Plan: the stored spell fires on the wielder when its circumstance is met, once, then the dagger rests until dawn. */
    private static void contingency(ServerPlayer p, long now) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            RelicDef d = Relics.defOf(s);
            if (d == null || !d.id().equals("contingency_plan") || !Attune.isMine(s, p)) continue;
            String text = Wave13Store.getStr(s, "fx");
            if (text.isEmpty()) continue;
            var bag = Wave13Store.bag(s);
            if (Wave13Rules.spentToday(bag, now)) continue;
            Wave13Rules.Trigger trig = Wave13Rules.triggerAt(bag.get("trig"));
            int foes = trig == Wave13Rules.Trigger.SURROUNDED ? Fx.foesAround(p, 5.0).size() : 0;
            boolean ailing = p.hasEffect(MobEffects.POISON) || p.hasEffect(MobEffects.WITHER);
            var snap = new Wave13Rules.Snapshot(p.getHealth(), p.getMaxHealth(), p.isOnFire(), p.getAirSupply(), p.fallDistance, foes, ailing, p.isCreative() || p.isSpectator());
            if (!Wave13Rules.fires(trig, snap)) return;
            for (Wave13Rules.Stored st : Wave13Rules.decode(text)) Fx.addEffect(p, st.id(), st.ticks(), st.amp());
            Wave13Store.putStr(s, "fx", "");
            Wave13Rules.markSpent(bag, now);
            ServerLevel level = p.serverLevel();
            level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.2f);
            Fx.burst(level, p.position(), 0x8FE8FF);
            Fx.say(p, "The contingency takes hold: " + trig.when() + ".");
            return;
        }
    }
}
