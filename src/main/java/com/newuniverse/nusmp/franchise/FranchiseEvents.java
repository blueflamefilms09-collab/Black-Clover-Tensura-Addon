package com.newuniverse.nusmp.franchise;

import com.newuniverse.nusmp.franchise.FranchiseData.Effect;
import com.newuniverse.nusmp.franchise.FranchiseData.Engraving;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the franchise engravings do (server side only; the clients only see names and a tooltip line). The decisions and numbers are {@link FranchiseRules}, the table of effects
 * {@link FranchiseData}; this class gathers the facts of a hit, applies the result and keeps the per-player state (keyed by the player's UUID, dropped on logout and when the server
 * stops). Nothing here touches the food bar, nothing deals a share of the target's maximum health, every bonus is a capped share of the hit that was going to land, and every proc
 * has a cooldown. Procs show the franchise's particle theme through {@code Fx} (so they go through the particle budgets).
 * <p>
 * Where an engraving counts: scope {@code main} = the item is in the main hand, {@code held} = in either hand, {@code any} = held, worn as armour or worn as a Curio (the worn
 * set is re-read once a second). Melee kinds count for the player's melee hits ({@code player_attack}); {@code spell_power} for what a spell hits within its window of a cast
 * ({@link #onSpellCast}, called by the grimoire after a finished cast).
 */
public final class FranchiseEvents {
    private FranchiseEvents() {}

    /** One effect of one engraving the player has right now, with the engraving's level. */
    record Active(Engraving eng, Effect fx, int level) {
        String key() { return eng.id() + ":" + fx.kind(); }
    }

    /** The per-player state. */
    static final class State {
        final Map<String, Long> readyAt = new HashMap<>();
        int killStacks, combo, charge;
        long lastKill = -1_000_000L, lastHit = -1_000_000L, lastChargeHit = -1_000_000L, counterUntil, lastCast = -1L, refundWindow;
        double refunded;
        final Map<UUID, Long> struck = new LinkedHashMap<>();
        Map<String, Integer> worn = Map.of();
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
    private static final int MAX_STRUCK = 16;

    private static State state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), k -> new State()); }

    private static boolean ready(State st, String key, long now) { return FranchiseRules.ready(now, st.readyAt.getOrDefault(key, 0L)); }

    private static void arm(State st, String key, long now, int cooldown) { st.readyAt.put(key, now + Math.max(0, cooldown)); }

    // ---------------------------------------------------------------- what a player has
    private static boolean scopeAllows(String scope, int rank) {
        return switch (scope) {
            case "main" -> rank == 0;
            case "held" -> rank <= 1;
            default -> rank <= 2;
        };
    }

    /** Adds the engravings of one stack to {id -> level}; rank 0 main hand, 1 off hand, 2 worn. */
    private static void add(Map<String, Integer> out, ItemStack s, int rank) {
        if (s.isEmpty()) return;
        ItemEnchantments ench = s.getEnchantments();
        if (ench.isEmpty()) return;
        for (Object2IntMap.Entry<Holder<Enchantment>> en : ench.entrySet()) {
            Optional<ResourceKey<Enchantment>> k = en.getKey().unwrapKey();
            if (k.isEmpty()) continue;
            ResourceLocation id = k.get().location();
            if (!"nusmp".equals(id.getNamespace()) || !id.getPath().startsWith("fr_")) continue;
            Engraving e = FranchiseData.engraving(id.getPath());
            if (e == null || !scopeAllows(e.scope(), rank)) continue;
            out.merge(id.getPath(), Math.max(1, Math.min(FranchiseRules.MAX_LEVEL, en.getIntValue())), Math::max);
        }
    }

    private static List<Active> actives(ServerPlayer p, State st) {
        Map<String, Integer> lv = new LinkedHashMap<>();
        add(lv, p.getMainHandItem(), 0);
        add(lv, p.getOffhandItem(), 1);
        if (st != null) for (Map.Entry<String, Integer> w : st.worn.entrySet()) lv.merge(w.getKey(), w.getValue(), Math::max);
        if (lv.isEmpty()) return List.of();
        List<Active> out = new ArrayList<>(4);
        for (Map.Entry<String, Integer> en : lv.entrySet()) {
            Engraving e = FranchiseData.engraving(en.getKey());
            if (e != null) for (Effect fx : e.effects()) out.add(new Active(e, fx, en.getValue()));
        }
        return out;
    }

    /** Armour and Curios, re-read once a second. */
    private static Map<String, Integer> readWorn(ServerPlayer p) {
        Map<String, Integer> lv = new LinkedHashMap<>();
        for (ItemStack s : p.getArmorSlots()) add(lv, s, 2);
        try {
            if (com.newuniverse.nusmp.item.CuriosLink.available()) for (ItemStack s : com.newuniverse.nusmp.item.CuriosLink.equipped(p)) add(lv, s, 2);
        } catch (RuntimeException | LinkageError failed) {
            // Curios is optional: armour and hands still count
        }
        return lv.isEmpty() ? Map.of() : lv;
    }

    // ---------------------------------------------------------------- helpers
    private static void puff(LivingEntity at, Engraving e) {
        try {
            if (!(at.level() instanceof ServerLevel level)) return;
            FranchiseData.Franchise f = FranchiseData.table().franchise(e.franchise());
            if (f != null) Fx.send(level, ThemeKey.of(f.theme()), at.getX(), at.getY() + at.getBbHeight() * 0.6, at.getZ(), 4, 0.35);
        } catch (RuntimeException | LinkageError ignored) {
            // looks only: never let a particle break a hit
        }
    }

    private static final Map<String, Holder<MobEffect>> EFFECTS = new ConcurrentHashMap<>();

    private static Holder<MobEffect> effect(String id) {
        if (!FranchiseRules.effectAllowed(id)) return null;
        Holder<MobEffect> h = EFFECTS.get(id);
        if (h == null) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            Optional<Holder.Reference<MobEffect>> o = rl == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(rl);
            if (o.isEmpty()) return null;
            h = o.get();
            EFFECTS.put(id, h);
        }
        return h;
    }

    private static void quiet(LivingEntity e, Holder<MobEffect> h, int dur, int amp) { e.addEffect(new MobEffectInstance(h, dur, amp, true, false, false)); }

    private static boolean protectedFoe(LivingEntity v) {
        return v.hasEffect(MobEffects.DAMAGE_RESISTANCE) || v.hasEffect(MobEffects.ABSORPTION) || v.hasEffect(MobEffects.REGENERATION) || v.getAbsorptionAmount() > 0f;
    }

    private static boolean holdsShield(Player p) {
        return p.getMainHandItem().getUseAnimation() == UseAnim.BLOCK || p.getOffhandItem().getUseAnimation() == UseAnim.BLOCK;
    }

    // ---------------------------------------------------------------- damage
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (!FranchiseConfig.enabled()) return;
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || e.getAmount() <= 0f) return;
        boolean attackerIsPlayer = src.getEntity() instanceof ServerPlayer;
        if (!(victim instanceof ServerPlayer) && !attackerIsPlayer) return;
        long now = victim.level().getGameTime();
        if (victim instanceof ServerPlayer vp && defend(vp, src, e, now)) return;
        if (src.getEntity() instanceof ServerPlayer ap && ap != victim) attack(ap, victim, src, e, now);
    }

    /** The victim's side; true when the hit was dodged (cancelled). */
    private static boolean defend(ServerPlayer vp, DamageSource src, LivingIncomingDamageEvent e, long now) {
        State st = STATES.get(vp.getUUID());
        List<Active> act = actives(vp, st);
        if (act.isEmpty()) return false;
        if (st == null) st = state(vp);
        double guard = 0.0;
        boolean blocking = vp.isBlocking() || holdsShield(vp);
        boolean magic = src.is(DamageTypeTags.WITCH_RESISTANT_TO) || src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC);
        boolean projectile = src.is(DamageTypeTags.IS_PROJECTILE), fall = src.is(DamageTypeTags.IS_FALL), explosion = src.is(DamageTypeTags.IS_EXPLOSION), fire = src.is(DamageTypeTags.IS_FIRE);
        boolean melee = src.is(DamageTypes.MOB_ATTACK) || src.is(DamageTypes.PLAYER_ATTACK) || src.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
        for (Active a : act) {
            Effect fx = a.fx();
            switch (fx.kind()) {
                case "dodge" -> {
                    String key = a.key();
                    if (ready(st, key, now) && FranchiseRules.roll(vp.getRandom().nextDouble(), FranchiseRules.chance(fx.d("chance", 0), a.level()))) {
                        arm(st, key, now, fx.i("cd", 400));
                        e.setCanceled(true);
                        puff(vp, a.eng());
                        return true;
                    }
                }
                case "shield_guard" -> { if (blocking) guard += FranchiseRules.scaled(fx.d("pct", 0), a.level()); }
                case "ward" -> { if (FranchiseRules.wardApplies(fx.s("tag", ""), magic, projectile, fall, melee, explosion, fire)) guard += FranchiseRules.scaled(fx.d("pct", 0), a.level()); }
                default -> { }
            }
        }
        if (guard > 0.0) e.setAmount(FranchiseRules.applyGuard(e.getAmount(), guard));
        return false;
    }

    /** The attacker's side: every bonus together is one capped share of the hit. */
    private static void attack(ServerPlayer ap, LivingEntity victim, DamageSource src, LivingIncomingDamageEvent e, long now) {
        State st = STATES.get(ap.getUUID());
        List<Active> act = actives(ap, st);
        if (act.isEmpty()) return;
        if (st == null) st = state(ap);
        boolean melee = src.is(DamageTypes.PLAYER_ATTACK);
        boolean spellHit = !melee && st.lastCast >= 0;
        FranchiseRules.Facts facts = null;
        double sum = 0.0;
        for (Active a : act) {
            Effect fx = a.fx();
            int lv = a.level();
            String kind = fx.kind();
            if (kind.equals("spell_power")) {
                if (spellHit && FranchiseRules.spellWindow(now, st.lastCast, fx.i("window", 60))) sum += FranchiseRules.scaled(fx.d("pct", 0), lv);
                continue;
            }
            if (!melee) continue;
            if (facts == null) facts = facts(ap, victim);
            switch (kind) {
                case "hit_bonus" -> { if (FranchiseRules.condMet(fx.s("cond", ""), facts)) sum += FranchiseRules.scaled(fx.d("pct", 0), lv); }
                case "anti_ward" -> { if (protectedFoe(victim)) sum += FranchiseRules.scaled(fx.d("pct", 0), lv); }
                case "first_strike" -> {
                    Long last = st.struck.get(victim.getUUID());
                    if (last == null || now - last > fx.i("window", 200)) sum += FranchiseRules.scaled(fx.d("pct", 0), lv);
                }
                case "kill_stacks" -> {
                    int n = FranchiseRules.stacksLeft(st.killStacks, st.lastKill, now, fx.i("decay", 1200));
                    sum += FranchiseRules.stackBonus(n, fx.i("max", 5), FranchiseRules.scaled(fx.d("pct", 0), lv));
                }
                case "combo" -> {
                    int max = fx.i("max", 4);
                    st.combo = FranchiseRules.comboNext(st.combo, st.lastHit, now, fx.i("window", 60), max + 1);
                    st.lastHit = now;
                    sum += (st.combo - 1) * FranchiseRules.scaled(fx.d("pct", 0), lv);
                }
                case "charge_hit" -> {
                    if (FranchiseRules.chargeStale(st.lastChargeHit, now)) st.charge = 0;
                    int hits = fx.i("hits", 8);
                    if (FranchiseRules.chargeFires(st.charge, hits)) {
                        sum += FranchiseRules.scaled(fx.d("pct", 0), lv);
                        puff(victim, a.eng());
                    }
                    st.charge = FranchiseRules.chargeNext(st.charge, hits);
                    st.lastChargeHit = now;
                }
                case "counter" -> {
                    if (now <= st.counterUntil) {
                        sum += FranchiseRules.scaled(fx.d("pct", 0), lv);
                        st.counterUntil = 0L;
                    }
                }
                case "blood_price" -> {
                    if (FranchiseRules.priceOk(ap.getHealth(), fx.d("cost", 1.0), fx.d("min_hp", 8.0))) {
                        ap.setHealth((float) (ap.getHealth() - fx.d("cost", 1.0)));                 // a flat price, set directly: it can never kill
                        sum += FranchiseRules.scaled(fx.d("pct", 0), lv);
                    }
                }
                case "chance_roll" -> sum += chanceRoll(ap, st, a, now);
                default -> { }
            }
        }
        if (sum > 0.0) e.setAmount(FranchiseRules.applyAttack(e.getAmount(), sum, victim instanceof Player, FranchiseConfig.attackCap()));
    }

    /** A chance_roll: on a hit, with a cooldown, one of the bonuses the engraving lists. Returns the extra share for this hit. */
    private static double chanceRoll(ServerPlayer ap, State st, Active a, long now) {
        Effect fx = a.fx();
        String key = a.key();
        if (!ready(st, key, now)) return 0.0;
        if (!FranchiseRules.roll(ap.getRandom().nextDouble(), FranchiseRules.chance(fx.d("chance", 0), a.level()))) return 0.0;
        List<String> outs = new ArrayList<>(4);
        for (String o : new String[]{"bonus", "heal", "haste", "mana"}) if (fx.d(o, 0) > 0) outs.add(o);
        if (outs.isEmpty()) return 0.0;
        arm(st, key, now, fx.i("cd", 100));
        puff(ap, a.eng());
        String pick = outs.get(FranchiseRules.pick(ap.getRandom().nextDouble(), outs.size()));
        switch (pick) {
            case "bonus" -> { return FranchiseRules.scaled(fx.d("bonus", 0), a.level()); }
            case "heal" -> ap.heal(FranchiseRules.healCap(fx.d("heal", 0), a.level()));
            case "haste" -> quiet(ap, MobEffects.MOVEMENT_SPEED, Math.min(100, fx.i("haste", 60)), 0);
            case "mana" -> FranchiseMana.refund(ap, FranchiseRules.refund(FranchiseMana.max(ap), fx.d("mana", 0), a.level(), 0.0));
            default -> { }
        }
        return 0.0;
    }

    private static FranchiseRules.Facts facts(ServerPlayer ap, LivingEntity victim) {
        boolean day = ap.level().isDay();
        return new FranchiseRules.Facts(day, victim.getHealth() / Math.max(1.0f, victim.getMaxHealth()), ap.getHealth() / Math.max(1.0f, ap.getMaxHealth()),
                victim.getType().is(EntityTypeTags.UNDEAD), victim.getArmorValue(), victim.getMaxHealth(), ap.isSprinting(), ap.fallDistance > 0.5f && !ap.onGround(),
                ap.isShiftKeyDown(), ap.isInWaterOrRain());
    }

    /** After the damage landed: lifesteal, debuffs, the memory of who was struck, the armed counter and the second wind. */
    public static void onPost(LivingDamageEvent.Post e) {
        if (!FranchiseConfig.enabled()) return;
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide() || e.getNewDamage() <= 0f) return;
        long now = victim.level().getGameTime();
        DamageSource src = e.getSource();
        if (victim instanceof ServerPlayer vp) {
            State st = STATES.get(vp.getUUID());
            List<Active> act = actives(vp, st);
            if (!act.isEmpty()) {
                if (st == null) st = state(vp);
                for (Active a : act) {
                    Effect fx = a.fx();
                    if (fx.kind().equals("counter")) st.counterUntil = Math.max(st.counterUntil, now + fx.i("window", 60));
                    else if (fx.kind().equals("second_wind") && ready(st, a.key(), now) && FranchiseRules.windFires(vp.getHealth(), vp.getMaxHealth(), fx.d("threshold", 0.25))) {
                        arm(st, a.key(), now, fx.i("cd", 3600));
                        vp.heal(FranchiseRules.healCap(fx.d("heal", 0), a.level()));
                        puff(vp, a.eng());
                    }
                }
            }
        }
        if (!src.is(DamageTypes.PLAYER_ATTACK) || !(src.getEntity() instanceof ServerPlayer ap) || ap == victim) return;
        State st = STATES.get(ap.getUUID());
        List<Active> act = actives(ap, st);
        if (act.isEmpty()) return;
        if (st == null) st = state(ap);
        boolean tracks = false;
        for (Active a : act) {
            Effect fx = a.fx();
            switch (fx.kind()) {
                case "lifesteal" -> {
                    if (ready(st, a.key(), now)) {
                        float heal = FranchiseRules.steal(e.getNewDamage(), fx.d("pct", 0), fx.d("cap", 1.5), a.level());
                        if (heal > 0f) {
                            arm(st, a.key(), now, fx.i("cd", 20));
                            ap.heal(heal);
                        }
                    }
                }
                case "debuff" -> {
                    Holder<MobEffect> h = effect(fx.s("effect", ""));
                    if (h != null && ready(st, a.key(), now) && FranchiseRules.roll(ap.getRandom().nextDouble(), FranchiseRules.chance(fx.d("chance", 0), a.level()))) {
                        arm(st, a.key(), now, fx.i("cd", 100));
                        quiet(victim, h, Math.min(100, fx.i("dur", 40)), fx.i("amp", 0));
                        puff(victim, a.eng());
                    }
                }
                case "first_strike" -> tracks = true;
                default -> { }
            }
        }
        if (tracks) {
            st.struck.put(victim.getUUID(), now);
            if (st.struck.size() > MAX_STRUCK) {
                Iterator<UUID> it = st.struck.keySet().iterator();
                it.next();
                it.remove();                                                                  // the oldest memory goes
            }
        }
    }

    // ---------------------------------------------------------------- kills
    public static void onDeath(LivingDeathEvent e) {
        if (!FranchiseConfig.enabled()) return;
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide() || dead instanceof Player || dead.getType().getCategory() != MobCategory.MONSTER) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer ap)) return;
        State st = STATES.get(ap.getUUID());
        List<Active> act = actives(ap, st);
        if (act.isEmpty()) return;
        if (st == null) st = state(ap);
        long now = dead.level().getGameTime();
        for (Active a : act) {
            Effect fx = a.fx();
            switch (fx.kind()) {
                case "kill_stacks" -> {
                    st.killStacks = FranchiseRules.addStack(FranchiseRules.stacksLeft(st.killStacks, st.lastKill, now, fx.i("decay", 1200)), fx.i("max", 5));
                    st.lastKill = now;
                }
                case "kill_heal" -> {
                    if (ready(st, a.key(), now)) {
                        arm(st, a.key(), now, fx.i("cd", 60));
                        ap.heal(FranchiseRules.healCap(fx.d("heal", 0), a.level()));
                    }
                }
                case "kill_haste" -> {
                    if (ready(st, a.key(), now)) {
                        arm(st, a.key(), now, fx.i("cd", 200));
                        quiet(ap, MobEffects.MOVEMENT_SPEED, Math.min(100, fx.i("dur", 80)), 0);
                    }
                }
                case "kill_ally" -> {
                    if (FranchiseConfig.allies() && ready(st, a.key(), now) && FranchiseRules.roll(ap.getRandom().nextDouble(), FranchiseRules.chance(fx.d("chance", 0), a.level()))) {
                        arm(st, a.key(), now, fx.i("cd", 900));
                        try {
                            if (com.newuniverse.nusmp.relics.Fx.summon(ap, fx.s("entity", "minecraft:skeleton"), Math.min(600, fx.i("life", 400))) != null) puff(ap, a.eng());
                        } catch (RuntimeException | LinkageError failed) {
                            // an ally that cannot be made is simply not made
                        }
                    }
                }
                default -> { }
            }
        }
    }

    // ---------------------------------------------------------------- spells
    /** Called by the grimoire after a finished cast: opens the spell window and returns a little mana. Never throws. */
    public static void onSpellCast(ServerPlayer p) {
        try {
            if (!FranchiseConfig.enabled()) return;
            State st = STATES.get(p.getUUID());
            List<Active> act = actives(p, st);
            if (act.isEmpty()) return;
            if (st == null) st = state(p);
            long now = p.level().getGameTime();
            st.lastCast = now;
            for (Active a : act) {
                if (!a.fx().kind().equals("spell_refund") || !ready(st, a.key(), now)) continue;
                if (now - st.refundWindow > 1200) {
                    st.refundWindow = now;
                    st.refunded = 0.0;
                }
                double give = FranchiseRules.refund(FranchiseMana.max(p), a.fx().d("pct", 0), a.level(), st.refunded);
                if (give <= 0.0) continue;
                arm(st, a.key(), now, a.fx().i("cd", 100));
                st.refunded += give;
                FranchiseMana.refund(p, give);
                puff(p, a.eng());
            }
        } catch (RuntimeException | LinkageError failed) {
            // a cast is never broken by an engraving
        }
    }

    // ---------------------------------------------------------------- once a second
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_TICK = com.newuniverse.nusmp.perf.PerfProbe.section("s.franchise.tick");

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            tick(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_TICK, t0);
        }
    }

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 11 || !FranchiseConfig.enabled()) return;
        State st = STATES.get(p.getUUID());
        Map<String, Integer> worn = readWorn(p);
        if (st == null) {
            if (worn.isEmpty() && !hasHandEngraving(p)) return;
            st = state(p);
        }
        st.worn = worn;
        long now = p.level().getGameTime();
        if (st.killStacks > 0 && FranchiseRules.stacksLeft(st.killStacks, st.lastKill, now, 2400) == 0) st.killStacks = 0;
        List<Active> act = actives(p, st);
        for (Active a : act) {
            Effect fx = a.fx();
            if (fx.kind().equals("mana_trickle") && ready(st, a.key(), now)) {
                arm(st, a.key(), now, 100);
                FranchiseMana.refund(p, FranchiseRules.trickle(FranchiseMana.max(p), fx.d("pct", 0), a.level()));
            } else if (fx.kind().equals("ally_regen") && FranchiseConfig.aura() && ready(st, a.key(), now)) {
                arm(st, a.key(), now, fx.i("interval", 40));
                regenAround(p, fx);
            }
        }
    }

    private static boolean hasHandEngraving(ServerPlayer p) {
        Map<String, Integer> m = new HashMap<>();
        add(m, p.getMainHandItem(), 0);
        add(m, p.getOffhandItem(), 1);
        return !m.isEmpty();
    }

    private static void regenAround(ServerPlayer p, Effect fx) {
        double r = Math.max(2.0, Math.min(12.0, fx.d("radius", 8)));
        int dur = FranchiseRules.regenDuration(fx.i("dur", 80));
        AABB box = p.getBoundingBox().inflate(r);
        for (ServerPlayer o : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, box)) {
            if (o == p || !o.isAlive() || o.isSpectator() || o.distanceToSqr(p) > r * r) continue;
            boolean friendly = p.isAlliedTo(o) || (p.getTeam() == null && o.getTeam() == null);
            if (friendly) quiet(o, MobEffects.REGENERATION, dur, 0);
        }
    }

    // ---------------------------------------------------------------- clean-up
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { STATES.remove(e.getEntity().getUUID()); }

    public static void onServerStopping(ServerStoppingEvent e) {
        STATES.clear();
        EFFECTS.clear();
    }

    /** For the self-test: drops the state of a stand-in player. */
    static void forget(UUID id) { STATES.remove(id); }
}
