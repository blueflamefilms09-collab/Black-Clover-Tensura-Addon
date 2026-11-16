package com.newuniverse.nusmp.labyrinth.mega.cores;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Kit;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.saiyan.SaiyanState;
import com.newuniverse.nusmp.saiyan.SaiyanStore;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the game side of the four armour families. Every handler returns at once for a player who wears no Boss Core piece (four slot reads and an instanceof each), so the cost for everybody else is
 * nil. Server side only (no client class); per-player state is keyed by UUID and dropped at logout.
 * <ul>
 *   <li><b>Attributes</b> ({@code ItemAttributeModifierEvent}): an Aegis piece adds maximum health, a Striker piece adds attack damage in its own slot group, both from the server config.</li>
 *   <li><b>Aegis</b> ({@code LivingIncomingDamageEvent}): less damage from damage types of other mods (a registry namespace that is not minecraft, plus the owner's list), capped.</li>
 *   <li><b>Striker</b>: two pieces raise the wearer's hits; four pieces add a second damage type after the hit ({@code LivingDamageEvent.Post}: a share of the damage dealt, again as magic damage that
 *       bypasses armour), guarded against recursion and bounded per player per tick.</li>
 *   <li><b>Wellspring</b> (once a second per player, spread by id): magicule, aura or Ki back as a share of the maximum; Nihility shortens skill cooldowns and, with four pieces, wipes a harmful effect.</li>
 *   <li><b>Ascendant</b> ({@code LivingDeathEvent}): a kill adds a share of the victim's EP to the wearer's base EP (capped by the wearer's own EP); Telesma and Malice are a stub ({@link QuenchedBridge}).</li>
 * </ul>
 * Worst case: one 20-tick pass over the online players that wear a Wellspring (at most one skill list walk each, a few dozen skills); at most {@code maxSecondTypeHitsPerTick} extra hits per player per tick;
 * no entity, no packet, no chunk, no world scan.
 */
public final class BossCoreEvents {
    private BossCoreEvents() {}

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final PerfProbe.Section TICK_SECTION = PerfProbe.section("s.bosscores.tick");

    /** Striker: the second hit is in progress (server thread only, so one flag is enough). */
    private static boolean inside;
    /** Striker: per player, the game tick and how many second-type hits were dealt in it. */
    private static final Map<UUID, long[]> EXTRA = new HashMap<>();

    public static void register(IEventBus bus) {
        bus.addListener(BossCoreEvents::onAttributes);
        bus.addListener(BossCoreEvents::onIncoming);
        bus.addListener(BossCoreEvents::onDamaged);
        bus.addListener(BossCoreEvents::onPlayerTick);
        bus.addListener(BossCoreEvents::onDeath);
        bus.addListener(BossCoreEvents::onLogout);
        bus.addListener(BossCoreEvents::onStopping);
    }

    // ---------------------------------------------------------------- what is worn
    /** How many pieces of each kit the player wears (index = {@link Kit#ordinal()}). */
    static int[] worn(Player p) {
        int[] n = new int[BossCoreRules.KITS];
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = p.getItemBySlot(s);
            if (!st.isEmpty() && st.getItem() instanceof BossCoreArmorItem a) n[a.kit().ordinal()]++;
        }
        return n;
    }

    private static boolean any(int[] n) {
        for (int v : n) if (v > 0) return true;
        return false;
    }

    // ---------------------------------------------------------------- attributes
    static void onAttributes(ItemAttributeModifierEvent e) {
        if (!(e.getItemStack().getItem() instanceof BossCoreArmorItem a)) return;
        if (!BossCoreConfig.enabled()) return;
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(a.getEquipmentSlot());
        if (a.kit().family() == BossCoreRules.Family.AEGIS) {
            double hp = BossCoreConfig.aegisHealth();
            if (hp > 0.0) e.addModifier(Attributes.MAX_HEALTH, new AttributeModifier(a.modifierId(), hp, AttributeModifier.Operation.ADD_VALUE), group);
        } else if (a.kit().family() == BossCoreRules.Family.STRIKER) {
            double atk = BossCoreConfig.strikerAttack();
            if (atk > 0.0) e.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(a.modifierId(), atk, AttributeModifier.Operation.ADD_VALUE), group);
        }
    }

    // ---------------------------------------------------------------- aegis and the first Striker bonus
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (!BossCoreConfig.enabled()) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || e.getAmount() <= 0f) return;
        if (e.getEntity() instanceof ServerPlayer victim) aegis(e, victim, src);
        if (!inside && src.getEntity() instanceof ServerPlayer hitter && e.getEntity() != hitter) striker(e, hitter);
    }

    private static void aegis(LivingIncomingDamageEvent e, ServerPlayer victim, DamageSource src) {
        int[] n = worn(victim);
        int pieces = n[Kit.AEGIS.ordinal()];
        if (pieces == 0) return;
        ResourceLocation type = src.typeHolder().unwrapKey().map(k -> k.location()).orElse(null);
        if (type == null) return;
        boolean counts = (BossCoreConfig.aegisOtherMods() && BossCoreRules.foreignNamespace(type.getNamespace())) || BossCoreConfig.extraResisted(type.toString());
        if (!counts) return;
        int tier = BossCoreRules.cappedTiers(n, BossCoreConfig.maxTiers())[Kit.AEGIS.ordinal()];
        double pct = BossCoreRules.resistPercent(pieces, tier, BossCoreConfig.aegisPer(), BossCoreConfig.aegisSet2(), BossCoreConfig.aegisSet4(), BossCoreConfig.aegisCap());
        if (pct > 0.0) e.setAmount((float) (e.getAmount() * (1.0 - pct / 100.0)));
    }

    private static void striker(LivingIncomingDamageEvent e, ServerPlayer hitter) {
        int[] n = worn(hitter);
        if (n[Kit.STRIKER.ordinal()] < 2) return;
        int tier = BossCoreRules.cappedTiers(n, BossCoreConfig.maxTiers())[Kit.STRIKER.ordinal()];
        if (tier >= 1) e.setAmount((float) (e.getAmount() * (1.0 + BossCoreConfig.strikerSet2() / 100.0)));
    }

    /** The second damage type: after the hit landed, a share of what it did is dealt again as magic damage (armour is bypassed), credited to the wearer. */
    static void onDamaged(LivingDamageEvent.Post e) {
        if (inside || !BossCoreConfig.enabled() || e.getNewDamage() <= 0f) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer hitter) || e.getEntity() == hitter) return;
        LivingEntity victim = e.getEntity();
        if (!victim.isAlive() || victim.level().isClientSide) return;
        int[] n = worn(hitter);
        if (n[Kit.STRIKER.ordinal()] < 4) return;
        if (BossCoreRules.cappedTiers(n, BossCoreConfig.maxTiers())[Kit.STRIKER.ordinal()] < 2) return;
        float extra = BossCoreRules.extraShare(e.getNewDamage(), BossCoreConfig.strikerShare());
        if (extra <= 0f || !budget(hitter, hitter.level().getGameTime())) return;
        int frames = victim.invulnerableTime;
        inside = true;
        try {
            victim.invulnerableTime = 0;                                  // the first hit set the invulnerability frames: the second type must land too
            victim.hurt(hitter.damageSources().indirectMagic(hitter, hitter), extra);
        } finally {
            victim.invulnerableTime = frames;
            inside = false;
        }
    }

    private static boolean budget(ServerPlayer p, long now) {
        long[] r = EXTRA.computeIfAbsent(p.getUUID(), k -> new long[2]);
        if (r[0] != now) {
            r[0] = now;
            r[1] = 0;
        }
        if (r[1] >= BossCoreConfig.extraHitsPerTick()) return false;
        r[1]++;
        return true;
    }

    // ---------------------------------------------------------------- the Wellspring, once a second
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 20 != 0 || p.isSpectator() || !p.isAlive() || !BossCoreConfig.enabled()) return;
        long t0 = PerfProbe.begin();
        try {
            SafeCast.tick("cores.wellspring", () -> wellspring(p));
        } finally {
            PerfProbe.end(TICK_SECTION, t0);
        }
    }

    static void wellspring(ServerPlayer p) {
        int[] n = worn(p);
        if (!any(n)) return;
        int[] tiers = BossCoreRules.cappedTiers(n, BossCoreConfig.maxTiers());
        for (Kit k : Kit.values()) {
            if (k.family() != BossCoreRules.Family.WELLSPRING || n[k.ordinal()] == 0) continue;
            int pieces = n[k.ordinal()], tier = tiers[k.ordinal()];
            double pct = BossCoreRules.regenPercent(pieces, tier, BossCoreConfig.regenPer(), BossCoreConfig.tier2(), BossCoreConfig.tier4(), BossCoreConfig.regenCap());
            switch (k.resource()) {
                case AURA:
                    refill(p, true, pct);
                    break;
                case KI:
                    SaiyanState s = SaiyanStore.peek(p);
                    refill(p, false, s != null && s.saiyan ? Math.min(BossCoreConfig.regenCap() * BossCoreConfig.kiSaiyan(), pct * BossCoreConfig.kiSaiyan()) : pct);
                    break;
                case NIHILITY:
                    nihility(p, pieces, tier);
                    break;
                default:
                    refill(p, false, pct);
                    break;
            }
        }
    }

    /** Gives back {@code pct} percent of the maximum of the aura (or the magicule, which is also the Ki of this mod), never past the maximum. */
    private static void refill(ServerPlayer p, boolean aura, double pct) {
        if (pct <= 0.0) return;
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return;
            double max = aura ? EnergyHelper.getMaxAura(p) : EnergyHelper.getMaxMagicule(p);
            double cur = aura ? ex.getAura() : ex.getMagicule();
            if (max <= 0.0 || cur >= max) return;
            double next = Math.min(max, cur + max * pct / 100.0);
            if (aura) ex.setAura(next);
            else ex.setMagicule(next);
            ex.markDirty();
        } catch (RuntimeException | LinkageError noTensuraData) {
            // a stand-in or a player without Tensura data has nothing to refill
        }
    }

    /** Nihility: no pool exists, so the Wellspring of Nihility means freedom from what holds a caster back: every active cooldown falls faster, and the full set wipes one harmful effect each second. */
    private static void nihility(ServerPlayer p, int pieces, int tier) {
        double pct = Math.min(50.0, BossCoreConfig.nihilityCooldown() * pieces * BossCoreRules.tierMult(tier, BossCoreConfig.tier2(), BossCoreConfig.tier4()));
        if (pct > 0.0) {
            try {
                Skills skills = SkillAPI.getSkillsFrom(p);
                if (skills != null) {
                    boolean changedAny = false;
                    for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
                        int modes = Math.max(1, i.getModes());
                        boolean changed = false;
                        for (int m = 0; m < modes; m++) {
                            int cd = i.getCoolDown(m);
                            if (cd > 0) {
                                i.setCoolDown(BossCoreRules.reduceCooldown(cd, pct), m);
                                changed = true;
                            }
                        }
                        if (changed) {
                            i.markDirty();
                            changedAny = true;
                        }
                    }
                    if (changedAny) skills.markDirty();
                }
            } catch (RuntimeException | LinkageError noSkills) {
                // no skill data: nothing to shorten
            }
        }
        if (tier >= 2 && BossCoreConfig.nihilityCleanse()) {
            for (MobEffectInstance m : new ArrayList<>(p.getActiveEffects())) {
                if (m.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    p.removeEffect(m.getEffect());
                    break;
                }
            }
        }
    }

    // ---------------------------------------------------------------- the Ascendant
    static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        if (victim instanceof ServerPlayer vp) forget(vp.getUUID());
        if (!BossCoreConfig.enabled() || !(e.getSource().getEntity() instanceof ServerPlayer killer) || killer == victim) return;
        if (victim instanceof ServerPlayer && !BossCoreConfig.epFromPlayers()) return;
        int[] n = worn(killer);
        if (n[Kit.ASCENDANT.ordinal()] == 0) return;
        SafeCast.tick("cores.ascendant", () -> ascend(killer, victim, n));
    }

    private static void ascend(ServerPlayer killer, LivingEntity victim, int[] n) {
        int pieces = n[Kit.ASCENDANT.ordinal()];
        int tier = BossCoreRules.cappedTiers(n, BossCoreConfig.maxTiers())[Kit.ASCENDANT.ordinal()];
        double gain = BossCoreRules.epBonus(TensuraBridge.ep(victim), pieces, tier, BossCoreConfig.epShare(), BossCoreConfig.tier2(), BossCoreConfig.tier4(),
                TensuraBridge.baseMaxEp(killer), BossCoreConfig.epCap());
        if (gain <= 0.0) return;
        TensuraBridge.addBaseEp(killer, gain);
        QuenchedBridge.onAscendantGain(killer, gain);
    }

    // ---------------------------------------------------------------- clean-up
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { forget(e.getEntity().getUUID()); }

    static void onStopping(ServerStoppingEvent e) { EXTRA.clear(); QuenchedBridge.clear(); }

    static void forget(UUID id) { EXTRA.remove(id); QuenchedBridge.forget(id); }

    /** For the self-test: how many players the second-type table holds. */
    static int tracked() { return EXTRA.size(); }

    /** For the self-test: the second-type hits this player dealt in the tick {@code now}. */
    static int extraHits(UUID id, long now) {
        long[] r = EXTRA.get(id);
        return r == null || r[0] != now ? 0 : (int) r[1];
    }
}
