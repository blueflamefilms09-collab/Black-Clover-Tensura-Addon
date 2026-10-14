package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Wave 16: the game-bus side of the seventeen charms, one listener per event, each starting with the cheapest test (is a wearer even involved).
 * <ul>
 *   <li>Damage in: the Sigil's fire focus and Reprisal, the god's armed blows, the Dryad's weakness to fire, the Sunscreen against the sun's burn.</li>
 *   <li>Death: the Wallet pays for a kill; an innocent's death marks the killer for the Handgrenade; the ring's curse and the creatures end with the wearer.</li>
 *   <li>Player tick, once a second: the Sunscreen, the Dryad's band, the Satchel's vanishing objects.</li>
 *   <li>Server tick: the bats, the Weaverlings and the chalk wards.</li>
 *   <li>Use block: a sealed door or container stays shut for everyone but its owner.</li>
 *   <li>Logout, dimension change, server stop: the temporary states are dropped.</li>
 * </ul>
 * {@link #onSpellCast} is the optional hook of the grimoire cast code that lets the Brooch of Po'eur refund a spell. The Dryad's ring is kept on the
 * finger by Curios' own "can unequip" event, joined by reflection so the build does not depend on it.
 */
public final class Wave16Events {
    private Wave16Events() {}

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("nusmp");
    /** True while the Sigil's own fireball is in flight, so a second Sigil does not answer it with another. */
    private static boolean inReprisal;

    static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(Wave16Events::onIncoming);
        bus.addListener(Wave16Events::onDeath);
        bus.addListener(Wave16Events::onPlayerTick);
        bus.addListener(Wave16Events::onServerTick);
        bus.addListener(Wave16Events::onUseBlock);
        bus.addListener(Wave16Events::onChangeTarget);
        bus.addListener(Wave16Events::onLogout);
        bus.addListener(Wave16Events::onDimension);
        bus.addListener(Wave16Events::onStopping);
        bindCurio();
    }

    // ---------------------------------------------------------------- Brooch of Po'eur
    /** A grimoire spell was cast and cost {@code costPercent} of the caster's maximum magicules: the gems may have kept the price. */
    public static void onSpellCast(ServerPlayer p, double costPercent) {
        if (p == null || !RelicFlags.has(p, Wave16Tune.F_POEUR) || Wave16Rules.poeurDc(costPercent) == 0) return;
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        if (now < d.getLong(Wave16Tune.K_POEUR)) return;
        int roll = p.getRandom().nextInt(20) + 1;
        if (!Wave16Rules.freeCast(costPercent, roll, Wave16Tune.POEUR_BONUS)) {
            Wave6Kit.hint(p, "Po'eur: the gems stay dull (" + roll + " + " + Wave16Tune.POEUR_BONUS + " against " + Wave16Rules.poeurDc(costPercent) + ")");
            return;
        }
        d.putLong(Wave16Tune.K_POEUR, now + Wave16Tune.POEUR_COOLDOWN);
        Wave16Kit.give(p, costPercent / 100.0, 0.0);
        Fx.burst(p.serverLevel(), p.position(), 0xBFEFFF);
        Wave6Kit.hint(p, "Po'eur: the spell costs nothing (" + roll + " + " + Wave16Tune.POEUR_BONUS + " against " + Wave16Rules.poeurDc(costPercent) + ")");
    }

    /** Has this player lately shed innocent blood (the Handgrenade will not trust them)? */
    static boolean tainted(ServerPlayer p) { return p.getPersistentData().getLong(Wave16Tune.K_TAINT) > p.level().getGameTime(); }

    // ================================================================ damage in
    private static boolean isFire(DamageSource src) {
        if (src.is(DamageTypeTags.IS_FIRE)) return true;
        var key = src.typeHolder().unwrapKey();
        if (key.isEmpty() || !"tensura".equals(key.get().location().getNamespace())) return false;
        String path = key.get().location().getPath();
        return path.contains("flame") || path.contains("fire") || path.contains("heat");
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        DamageSource src = e.getSource();
        if (src.getEntity() instanceof ServerPlayer att && att != victim) attackerHits(e, att, src);
        if (victim instanceof ServerPlayer p && p.isAlive() && !p.isSpectator()) wearerHurt(e, p, src);
    }

    private static void attackerHits(LivingIncomingDamageEvent e, ServerPlayer att, DamageSource src) {
        CompoundTag d = att.getPersistentData();
        if (RelicFlags.has(att, Wave16Tune.F_FLAME) && isFire(src)) e.setAmount((float) (e.getAmount() * Wave16Tune.FLAME_FOCUS));
        if (!RelicFlags.has(att, Wave16Tune.F_GOD)) return;
        long now = att.level().getGameTime();
        boolean melee = src.getDirectEntity() == att;
        if (melee && d.getLong(Wave16Tune.K_GOD_SPIRIT) > now) {
            e.setAmount(e.getAmount() + d.getInt(Wave16Tune.K_GOD_SPIRIT_ROLL));
            d.putLong(Wave16Tune.K_GOD_SPIRIT, 0L);
            Fx.burst(att.serverLevel(), e.getEntity().position(), 0xFFE0A0);
        }
        if (melee && d.getLong(Wave16Tune.K_GOD_AVATAR) > now) e.setAmount(e.getAmount() + att.getRandom().nextInt(10) + 1);
    }

    private static void wearerHurt(LivingIncomingDamageEvent e, ServerPlayer p, DamageSource src) {
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        if (RelicFlags.has(p, Wave16Tune.F_DRYAD) && src.is(DamageTypeTags.IS_FIRE)) e.setAmount((float) (e.getAmount() * Wave16Tune.DRYAD_FIRE));
        if (d.getLong(Wave16Tune.K_SUN) > now && src.is(DamageTypes.ON_FIRE) && p.level().isDay() && p.level().canSeeSky(p.blockPosition())) {
            e.setAmount((float) (e.getAmount() * Wave16Tune.SUN_BURN_FACTOR));
        }
        if (d.getLong(Wave16Tune.K_GOD_REVENGE) > now && src.getEntity() instanceof LivingEntity a && a != p && a.isAlive()) {
            d.putLong(Wave16Tune.K_GOD_REVENGE, 0L);
            int dmg = 0;
            for (int i = 0; i < 3; i++) dmg += p.getRandom().nextInt(10) + 1;
            Fx.damage(p, a, dmg, "minecraft:magic");
            Fx.burst(p.serverLevel(), a.position(), 0xFF3030);
        }
        if (!inReprisal && RelicFlags.has(p, Wave16Tune.F_FLAME)) reprisal(e, p, src, d, now);
    }

    /** Reprisal: a spell aimed at you is halved by one to three of the Sigil's charges, and a fireball goes back to whoever cast it. */
    private static void reprisal(LivingIncomingDamageEvent e, ServerPlayer p, DamageSource src, CompoundTag d, long now) {
        if (!(src.getEntity() instanceof LivingEntity att) || att == p || !att.isAlive()) return;
        if (!Wave6Kit.isMagic(src) && !isFire(src)) return;
        if (now < d.getLong(Wave16Tune.K_REPRISAL) || !Wave16Rules.sunUp(p.level().getDayTime())) return;
        ItemStack sigil = Wave6Kit.find(p, Wave16Tune.ID_SIGIL);
        if (sigil.isEmpty()) return;
        int cost = Wave16Rules.reprisalCost(e.getAmount());
        if (!Wave16Kit.spend(sigil, p, "sigil", Wave16Tune.SIGIL_MAX, () -> Wave16Rules.sigilGain(n -> p.getRandom().nextInt(n)), cost)) return;
        d.putLong(Wave16Tune.K_REPRISAL, now + Wave16Tune.REPRISAL_COOLDOWN);
        e.setAmount(e.getAmount() / 2f);
        inReprisal = true;
        try {
            Fx.damage(p, att, Wave16Rules.reprisalDamage(cost), "minecraft:fireball");
            att.setRemainingFireTicks(80);
        } finally {
            inReprisal = false;
        }
        Fx.burst(p.serverLevel(), att.position(), 0xFF8A2A);
        Wave6Kit.hint(p, "Reprisal! The spell is halved and a fireball goes back (" + cost + " charge" + (cost > 1 ? "s" : "") + ")");
    }

    // ================================================================ death
    private static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim instanceof ServerPlayer dead) {
            CompoundTag d = dead.getPersistentData();
            d.putBoolean(Wave16Tune.K_DRYAD_BOUND, false);
            d.putLong(Wave16Tune.K_DRYAD_OFF, 0L);
            d.putLong(Wave16Tune.K_GOD_SPIRIT, 0L);
            d.putLong(Wave16Tune.K_GOD_REVENGE, 0L);
            d.putLong(Wave16Tune.K_GOD_AVATAR, 0L);
            Wave16Swarm.ownerGone(dead);
        }
        if (e.getSource().getEntity() instanceof ServerPlayer killer && killer != victim) {
            if (Wave16Kit.innocent(victim)) killer.getPersistentData().putLong(Wave16Tune.K_TAINT, killer.level().getGameTime() + Wave16Tune.TAINT_TICKS);
            if (RelicFlags.has(killer, Wave16Tune.F_WALLET)) Wave16Wallet.reward(killer, victim);
        }
    }

    // ================================================================ once a second per player
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        if ((p.tickCount + p.getId()) % 20 != 3) return;
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        if (d.getLong(Wave16Tune.K_SUN) > now) {
            for (String id : new String[]{"minecraft:blindness", "minecraft:darkness", "tensura:flashed_blindness"}) Wave6Kit.lift(p, id);
        }
        if (RelicFlags.has(p, Wave16Tune.F_DRYAD) || d.getBoolean(Wave16Tune.K_DRYAD_BOUND)) dryadPass(p, d, now);
        if ((p.tickCount / 20) % 5 == 0 && RelicFlags.has(p, Wave16Tune.F_SATCHEL)) Wave16Satchel.sweep(p);
    }

    /** The ring: worn, it binds and the wearer takes the sun in; off, the vines drag at them until the ring is back, they die, or three days pass. */
    private static void dryadPass(ServerPlayer p, CompoundTag d, long now) {
        if (Relics.worn(p, Wave16Tune.ID_DRYAD)) {
            d.putBoolean(Wave16Tune.K_DRYAD_BOUND, true);
            d.putLong(Wave16Tune.K_DRYAD_OFF, 0L);
            if ((p.tickCount / 20) % 5 == 0 && p.level().isDay() && p.level().canSeeSky(p.blockPosition())) {
                var food = p.getFoodData();
                food.setSaturation(Math.min((float) food.getFoodLevel(), food.getSaturationLevel() + 0.6f));
                if (p.getHealth() < p.getMaxHealth()) p.heal(0.5f);
            }
            return;
        }
        if (!d.getBoolean(Wave16Tune.K_DRYAD_BOUND)) return;
        long off = d.getLong(Wave16Tune.K_DRYAD_OFF);
        if (off == 0L) {
            d.putLong(Wave16Tune.K_DRYAD_OFF, now);
        } else if (Wave16Rules.vinesLetGo(off, now)) {
            d.putBoolean(Wave16Tune.K_DRYAD_BOUND, false);
            d.putLong(Wave16Tune.K_DRYAD_OFF, 0L);
            Wave6Kit.hint(p, "The vines wither away at last.");
        } else {
            Fx.addEffect(p, "minecraft:slowness", 60, 0);
            Fx.addEffect(p, "minecraft:weakness", 60, 0);
            if ((p.tickCount / 20) % 10 == 0) Wave6Kit.hint(p, "The vines drag at you: the ring wants its finger back.");
        }
    }

    /** Curios' "can this be taken off" event, joined by name: the Dryad's ring stays on the finger of the living. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bindCurio() {
        try {
            Class<?> evt = Class.forName("top.theillusivec4.curios.api.event.CurioCanUnequipEvent");
            if (!Event.class.isAssignableFrom(evt)) return;
            Method getStack = evt.getMethod("getStack"), set = null;
            for (Method m : evt.getMethods()) if (m.getName().equals("setUnequipResult") && m.getParameterCount() == 1) set = m;
            if (set == null || !set.getParameterTypes()[0].isEnum()) return;
            Object deny = null;
            for (Object k : set.getParameterTypes()[0].getEnumConstants()) if (k.toString().equals("FALSE") || k.toString().equals("DENY")) deny = k;
            if (deny == null) return;
            final Method setter = set;
            final Object no = deny;
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, (Class) evt, (Consumer) ev -> {
                try {
                    if (!(getStack.invoke(ev) instanceof ItemStack s)) return;
                    RelicDef def = Relics.defOf(s);
                    if (def == null || !Wave16Tune.ID_DRYAD.equals(def.id())) return;
                    if (ev instanceof LivingEvent le && !le.getEntity().isAlive()) return;                     // the dead may be freed
                    setter.invoke(ev, no);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // the ring is then held only by the vines' drag
                }
            });
        } catch (ClassNotFoundException none) {
            // Curios is not installed: no relic in a Curios slot works anyway
        } catch (ReflectiveOperationException | RuntimeException | LinkageError odd) {
            LOG.debug("[nusmp] relics: the Dryad's band could not join Curios' unequip event", odd);
        }
    }

    // ================================================================ server tick, seals, lifetime
    private static void onServerTick(ServerTickEvent.Post e) {
        Wave16Swarm.tickBats(e.getServer());
        Wave16Swarm.tickWeavers(e.getServer());
        Wave16Chalk.tickWards(e.getServer());
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || !Wave16Chalk.anySeals() || e.getEntity().isCreative()) return;
        if (!Wave16Chalk.sealedAgainst(e.getLevel(), e.getPos(), e.getEntity())) return;
        e.setCanceled(true);
        if (e.getEntity() instanceof ServerPlayer p) Wave6Kit.hint(p, "A chalk rune holds it shut.");
    }

    /** Snuggles and the Weaverlings keep to their owner's quarrels: a player the owner may not harm is never their target. */
    private static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof Player target)) return;
        LivingEntity me = e.getEntity();
        CompoundTag tag = me.getPersistentData();
        if (!tag.getBoolean(Wave16Tune.ALLY) || !tag.hasUUID(Wave16Tune.OWNER) || !(me.level() instanceof ServerLevel lv)) return;
        ServerPlayer owner = lv.getServer().getPlayerList().getPlayer(tag.getUUID(Wave16Tune.OWNER));
        if (owner == null || target.isCreative() || target.isSpectator() || !owner.canHarmPlayer(target)) e.setCanceled(true);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave16Swarm.ownerGone(p);
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave16Swarm.ownerGone(p);
    }

    private static void onStopping(ServerStoppingEvent e) {
        Wave16Swarm.clear(e.getServer());
        Wave16Chalk.clear();
        inReprisal = false;
    }
}
