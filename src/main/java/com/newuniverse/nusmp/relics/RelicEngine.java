package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.tier.ActPower;
import com.newuniverse.nusmp.relics.tier.TierRules;
import com.newuniverse.nusmp.relics.tier.Tiers;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
 * The server half of the relics: once a second per player (staggered by entity id) it brings the player's attribute modifiers, ambient effects and
 * grade EP lift in line with what they wear; it cuts damage by the resistances they wear; and it runs the active abilities with every check in one
 * place ({@link #activate}): attunement, sneaking, hold time, cooldown, uses per day, mana, existence points. Costs and cooldowns are settled before
 * the ability's own code runs.
 * <p>
 * Where the state lives: cooldowns and toggles in the player's persistent data (they survive relog and death), the per-day counters on the stack
 * ({@code relic_uses}), attunement on the stack ({@code relic_attuned}).
 */
public final class RelicEngine {
    private RelicEngine() {}

    private static final String ROOT = "nusmp_relics2", CD = "cd", ON = "on";
    private static final String MOD_NS = "nusmp";
    /** Per player: the attribute modifiers the engine put on them (id -> attribute), so the ones that no longer apply are taken off. */
    private static final Map<UUID, Map<ResourceLocation, Holder<Attribute>>> APPLIED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_PACKET = new ConcurrentHashMap<>();
    /** The attunement ritual of an adopted item (whose own use() the engine does not run): first and last tick of an unbroken hold. */
    private static final Map<UUID, long[]> RITUAL = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Holder<Attribute>>> ATTRS = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> IDS = new ConcurrentHashMap<>();

    /** Ticks a worn relic's lent skill lasts without being renewed. */
    private static final int SKILL_LEASE = 60;

    public enum Outcome { NONE, HOLD, DONE, REFUSED }

    // ---------------------------------------------------------------- registration
    /** Game-bus listeners of the engine. */
    public static void register(IEventBus bus) {
        bus.addListener(RelicEngine::onPlayerTick);
        bus.addListener(RelicEngine::onIncomingDamage);
        bus.addListener(RelicEngine::onEquipmentChange);
        bus.addListener(RelicEngine::onLogout);
        bus.addListener(RelicEngine::onLogin);
        bus.addListener(RelicEngine::onRightClickItem);
        bus.addListener(RelicEngine::onRespawn);
        bus.addListener(RelicEngine::onServerTick);
        bus.addListener(RelicEngine::onServerStopping);
        bus.addListener(RelicEngine::onChangeTarget);
        bus.addListener(RelicEngine::onSummonLoad);
    }

    /** A summon that comes back from disk (a crash, a chunk that held it) is not tracked any more: it goes, so none stays forever. */
    private static void onSummonLoad(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !e.loadedFromDisk()) return;
        CompoundTag d = e.getEntity().getPersistentData();
        if (d.hasUUID(Fx.OWNER) && d.contains(Fx.UNTIL)) e.setCanceled(true);
    }

    /**
     * An ally called by {@link Fx#summon} never picks its owner, or another ally of the same owner, as a target: a Vex or a bat has goals of its own that
     * go for the nearest player, and the sweep in {@link RelicSummons} only corrects them every ten ticks.
     */
    private static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity target = e.getNewAboutToBeSetTarget();
        if (target == null) return;
        CompoundTag mine = e.getEntity().getPersistentData();
        if (!mine.hasUUID(Fx.OWNER)) return;
        UUID owner = mine.getUUID(Fx.OWNER);
        if (target.getUUID().equals(owner)) {
            e.setCanceled(true);
            return;
        }
        CompoundTag theirs = target.getPersistentData();
        if (theirs.hasUUID(Fx.OWNER) && owner.equals(theirs.getUUID(Fx.OWNER))) e.setCanceled(true);
    }

    private static void onServerTick(ServerTickEvent.Post e) { RelicSummons.tick(e.getServer()); }

    private static void onServerStopping(ServerStoppingEvent e) {
        APPLIED.clear();
        LAST_PACKET.clear();
        RITUAL.clear();
        RelicSummons.discardAll(e.getServer());
        RelicSummons.clear();
        Relics.clearCaches();
    }

    private static void onEquipmentChange(LivingEquipmentChangeEvent e) { Relics.invalidate(e.getEntity()); }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        APPLIED.remove(id);
        LAST_PACKET.remove(id);
        RITUAL.remove(id);
        Relics.invalidate(e.getEntity());
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) syncCooldowns(p);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        APPLIED.remove(e.getEntity().getUUID());                                       // transient modifiers did not survive the new body
        Relics.invalidate(e.getEntity());
        if (e.getEntity() instanceof ServerPlayer p) syncCooldowns(p);
    }

    // ---------------------------------------------------------------- the once-a-second pass
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        if (p.isUsingItem()) holdRitual(p);
        int phase = (p.tickCount + p.getId()) % 20;
        if (phase == 0) pass(p);
        else if (phase == 9) RelicEngravings.engraveInventory(p);
    }

    /** Brings the player's modifiers and ambient effects in line with what works for them now. */
    static void pass(ServerPlayer p) {
        Relics.Loadout lo = Relics.loadoutFresh(p);
        Map<ResourceLocation, Want> want = new LinkedHashMap<>();
        if (!p.isSpectator()) {
            for (Relics.Worn w : lo.active()) {
                int i = 0;
                for (Passive ps : w.def().passives()) {
                    if (ps.kind() == Passive.Kind.ATTR) {
                        boolean offOnly = w.def().slot().isHand() && w.def().look() != Look.SHIELD && w.stack() == p.getOffhandItem() && w.stack() != p.getMainHandItem();
                        Holder<Attribute> h = offOnly ? null : attribute(ps.key());       // a blade in the off-hand adds no attack bonus (its flags still count for set bonuses)
                        if (h != null) want.put(ResourceLocation.fromNamespaceAndPath(MOD_NS, "relic/" + w.def().id() + "/" + i), new Want(h, ps.amount(), ps.operation()));
                    } else if (ps.kind() == Passive.Kind.EFFECT) {
                        ambient(p, ps);
                    } else if (ps.kind() == Passive.Kind.SKILL) {
                        Fx.lendSkill(p, ps.key(), SKILL_LEASE);                         // a short lease, renewed every second while the relic is worn
                    }
                    i++;
                }
            }
            if (lo.epBonus() > 0) {                                                    // the grade lifts the existence the wearer has
                for (String a : new String[]{"tensura:max_magicule", "tensura:max_aura"}) {
                    Holder<Attribute> h = attribute(a);
                    if (h != null) want.put(ResourceLocation.fromNamespaceAndPath(MOD_NS, "relic_ep/" + a.substring(a.indexOf(':') + 1)),
                            new Want(h, lo.epBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            }
        }
        apply(p, want);
    }

    private record Want(Holder<Attribute> attr, double amount, AttributeModifier.Operation op) {}

    private static void apply(ServerPlayer p, Map<ResourceLocation, Want> want) {
        Map<ResourceLocation, Holder<Attribute>> old = APPLIED.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        Iterator<Map.Entry<ResourceLocation, Holder<Attribute>>> it = old.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, Holder<Attribute>> e = it.next();
            if (want.containsKey(e.getKey())) continue;
            AttributeInstance inst = p.getAttribute(e.getValue());
            if (inst != null) inst.removeModifier(e.getKey());
            it.remove();
        }
        for (Map.Entry<ResourceLocation, Want> e : want.entrySet()) {
            Want w = e.getValue();
            AttributeInstance inst = p.getAttribute(w.attr());
            if (inst == null) continue;
            AttributeModifier cur = inst.getModifier(e.getKey());
            if (cur == null || cur.amount() != w.amount() || cur.operation() != w.op()) {
                if (cur != null) inst.removeModifier(e.getKey());
                inst.addTransientModifier(new AttributeModifier(e.getKey(), w.amount(), w.op()));
            }
            old.put(e.getKey(), w.attr());
        }
        if (old.isEmpty()) APPLIED.remove(p.getUUID());
    }

    private static void ambient(ServerPlayer p, Passive ps) {
        Optional<? extends Holder<net.minecraft.world.effect.MobEffect>> h = Fx.effect(ps.key());
        if (h.isEmpty()) return;
        int ticks = ps.key().endsWith("night_vision") ? 260 : 80;                      // night vision flickers under ten seconds
        p.addEffect(new MobEffectInstance(h.get(), ticks, ps.amp(), true, false));
    }

    private static Holder<Attribute> attribute(String id) {
        return ATTRS.computeIfAbsent(id, k -> {
            ResourceLocation rl = Fx.id(k);
            return rl == null ? Optional.<Holder<Attribute>>empty() : BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h);
        }).orElse(null);
    }

    // ---------------------------------------------------------------- resistances
    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Relics.Loadout lo = Relics.loadout(p);
        if (lo.resists().isEmpty()) return;
        DamageSource src = e.getSource();
        double keep = 1.0;
        for (Relics.Resist r : lo.resists()) if (matches(src, r.id())) keep *= 1.0 - r.fraction();
        if (keep < 1.0) e.setAmount((float) (e.getAmount() * Math.max(0.1, keep)));
    }

    /** Does the source belong to this damage type id or damage type tag? */
    static boolean matches(DamageSource src, String id) {
        ResourceLocation rl = IDS.computeIfAbsent(id, k -> Fx.id(k) == null ? ResourceLocation.withDefaultNamespace("none") : Fx.id(k));
        return src.is(TagKey.create(Registries.DAMAGE_TYPE, rl)) || src.is(ResourceKey.<DamageType>create(Registries.DAMAGE_TYPE, rl));
    }

    // ---------------------------------------------------------------- using a relic
    /**
     * Right-click on a held relic. A relic that needs attunement and is not yours starts the five-second ritual (sneak, hold use); otherwise the first
     * act fires (the second, if you sneak and it has one), or a held act begins charging.
     */
    public static Outcome useHeld(ServerPlayer p, ItemStack stack, RelicDef def, InteractionHand hand) {
        if (def.needsAttunement() && !Attune.isMine(stack, p)) {
            if (p.isShiftKeyDown()) {
                p.startUsingItem(hand);
                return Outcome.HOLD;
            }
            tell(p, def.cardName() + " needs attunement: sneak and hold use for 5 seconds.", ChatFormatting.LIGHT_PURPLE);
            return Outcome.REFUSED;
        }
        if (def.acts().isEmpty()) return Outcome.NONE;
        Act act = pickAct(def, p.isShiftKeyDown());
        if (act.holdTicks() > 0) {
            p.startUsingItem(hand);
            return Outcome.HOLD;
        }
        return activate(p, stack, def, act, p.isShiftKeyDown(), 0) ? Outcome.DONE : Outcome.REFUSED;
    }

    /** The act a right-click uses: the first, or the second while sneaking if there is one. */
    public static Act pickAct(RelicDef def, boolean sneaking) {
        List<Act> acts = def.acts();
        return sneaking && acts.size() > 1 ? acts.get(1) : acts.get(0);
    }

    /** While a held relic is being used: the attunement ritual's clock (server). */
    public static void attuneTick(ServerPlayer p, ItemStack stack, RelicDef def, int heldTicks) {
        if (!p.isShiftKeyDown()) {
            p.stopUsingItem();
            tell(p, "Attunement broken: keep sneaking while you hold use.", ChatFormatting.GRAY);
            return;
        }
        if (heldTicks % 10 == 0 && heldTicks < AttuneRules.HOLD_TICKS) {
            int pct = AttuneRules.percent(heldTicks);
            p.displayClientMessage(Component.literal("Attuning " + def.cardName() + "  " + bar(pct) + "  " + pct + "%").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        if (heldTicks >= AttuneRules.HOLD_TICKS) {
            p.stopUsingItem();
            finishAttune(p, stack, def);
        }
    }

    private static void finishAttune(ServerPlayer p, ItemStack stack, RelicDef def) {
        if (Attune.bind(p, stack)) {
            ServerLevel level = p.serverLevel();
            level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 0.9f);
            Fx.ring(level, p.position(), 1.6, def.paletteGlow());
            tell(p, def.cardName() + " is attuned to you (" + Attune.carried(p, null) + " of " + AttuneRules.CAP + ").", ChatFormatting.GOLD);
        } else {
            tell(p, "You can keep only " + AttuneRules.CAP + " relics attuned. Let one go first (/multiverse relic unattune).", ChatFormatting.RED);
        }
    }

    /**
     * A relic the mod already had (an adopted def) keeps its own item class, which knows nothing of attunement. While it needs attunement and is not
     * yours, sneaking and clicking runs the ritual instead of the item's own sneak technique: keep sneaking and the button down (the game repeats the
     * click every few ticks) for five seconds. A click without sneaking is left alone, so the old shots, guards and first techniques never stop working.
     */
    private static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack stack = e.getItemStack();
        RelicDef def = Relics.defOf(stack);
        if (def == null || !def.isAdopted() || !def.needsAttunement() || def.look() == Look.SHIELD || Attune.isMine(stack, p)) return;   // a shield is held, not clicked: see holdRitual
        UUID id = p.getUUID();
        if (!p.isShiftKeyDown()) {
            RITUAL.remove(id);                                                         // a plain click is the old item's own business: its shot, guard or technique still works
            return;
        }
        e.setCanceled(true);                                                           // sneaking with an unattuned remake is the ritual (its old sneak technique waits until it is yours)
        e.setCancellationResult(InteractionResult.FAIL);
        long now = p.tickCount;
        long[] r = RITUAL.get(id);
        if (r == null || now - r[1] > 8) r = new long[]{now, now};
        else r[1] = now;
        RITUAL.put(id, r);
        int held = (int) (r[1] - r[0]);
        if (held >= AttuneRules.HOLD_TICKS) {
            RITUAL.remove(id);
            finishAttune(p, stack, def);
        } else {
            int pct = AttuneRules.percent(held);
            p.displayClientMessage(Component.literal("Attuning " + def.cardName() + "  " + bar(pct) + "  " + pct + "%").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /**
     * The one remake that is held rather than clicked: the Magebane Shield blocks while the button is down, on the client too, so the game sends no
     * repeated clicks for the other ritual to count. Its attunement counts the ticks of the hold instead, while the player sneaks and blocks; it still blocks as before.
     */
    private static void holdRitual(ServerPlayer p) {
        ItemStack stack = p.getUseItem();
        RelicDef def = Relics.defOf(stack);
        if (def == null || !def.isAdopted() || !def.needsAttunement() || def.look() != Look.SHIELD || !p.isShiftKeyDown() || Attune.isMine(stack, p)) return;
        int held = p.getTicksUsingItem();
        if (held >= AttuneRules.HOLD_TICKS) {
            p.stopUsingItem();
            finishAttune(p, stack, def);
        } else if (held > 0 && held % 10 == 0) {
            int pct = AttuneRules.percent(held);
            p.displayClientMessage(Component.literal("Attuning " + def.cardName() + "  " + bar(pct) + "  " + pct + "%").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    private static String bar(int pct) {
        int n = pct / 10;
        return "[" + "|".repeat(n) + ".".repeat(10 - n) + "]";
    }

    /** A held act is released (the use button let go): fires it with the charge it got, if the relic is yours. */
    public static void released(ServerPlayer p, ItemStack stack, RelicDef def, int chargedTicks) {
        if (def.acts().isEmpty() || (def.needsAttunement() && !Attune.isMine(stack, p))) return;
        Act act = pickAct(def, p.isShiftKeyDown());
        if (act.holdTicks() <= 0) return;
        activate(p, stack, def, act, p.isShiftKeyDown(), chargedTicks);
    }

    /** The server end of the Use Worn Relic key. */
    static void onActPacket(ServerPlayer p, RelicActPayload pl) {
        if (!p.isAlive() || p.isSpectator() || pl.relicId().length() > 64 || pl.actKey().length() > 64) return;
        long now = p.level().getGameTime();
        Long last = LAST_PACKET.put(p.getUUID(), now);
        if (last != null && now - last < 3) return;                                      // a held key cannot flood the server
        for (Relics.Worn w : Relics.loadoutFresh(p).active()) {
            if (!w.def().id().equals(pl.relicId())) continue;
            Act act = w.def().actByKey(pl.actKey());
            if (act != null) activate(p, w.stack(), w.def(), act, p.isShiftKeyDown(), Mth.clamp(pl.chargeTicks(), 0, 1200));
            return;
        }
        tell(p, "You do not wear that relic, or it does not answer you.", ChatFormatting.GRAY);
    }

    /**
     * Fires an act after every check. Returns true if it ran. Costs are paid and the cooldown and use counters are set before the body runs, so a
     * relic's code never has to guard them.
     */
    public static boolean activate(ServerPlayer p, ItemStack stack, RelicDef def, Act act, boolean sneaking, int chargeTicks) {
        if (p == null || stack == null || def == null || act == null || !p.isAlive() || p.isSpectator()) return false;
        if (RelicNullification.isNullified(p)) { tell(p, "Your relics are de-attuned for a moment: they do not answer.", ChatFormatting.GRAY); return false; }     // 1.45.0
        if (!Attune.works(stack, def, p)) { tell(p, def.cardName() + " is not attuned to you.", ChatFormatting.LIGHT_PURPLE); return false; }
        if (act.sneakRequired() && !sneaking) { tell(p, act.displayName() + " needs you to sneak.", ChatFormatting.GRAY); return false; }
        if (act.holdTicks() > 0 && chargeTicks < act.holdTicks()) {
            tell(p, act.displayName() + ": hold for " + UseRules.span(act.holdTicks()) + ".", ChatFormatting.GRAY);
            return false;
        }
        long now = p.level().getGameTime();
        long readyAt = cooldownReadyAt(p, def, act);
        if (!UseRules.ready(now, readyAt)) {
            tell(p, act.displayName() + ": ready in " + UseRules.span(UseRules.remaining(now, readyAt)) + ".", ChatFormatting.GRAY);
            return false;
        }
        if (act.usesPerDay() > 0 && usesLeft(stack, act, now) <= 0) {
            tell(p, act.displayName() + ": no uses left today (they return at dawn).", ChatFormatting.GRAY);
            return false;
        }
        if (!pay(p, act, Tiers.multiplier(stack, def, TierRules.Stat.COST))) return false;          // 1.29.0: a raised relic spends less
        if (act.toggles()) flip(p, def, act);
        if (act.usesPerDay() > 0) recordUse(stack, act, now);
        if (act.cooldownTicks() > 0) {
            int cd = Math.max(1, (int) Math.round(act.cooldownTicks() * Tiers.multiplier(stack, def, TierRules.Stat.COOLDOWN)));     // 1.29.0: a raised relic recovers sooner
            long until = UseRules.readyAt(now, cd);
            setCooldown(p, def, act, until);
            if (def.acts().size() == 1 && !def.isAdopted() && !def.isRanged() && def.slot().isHand()) p.getCooldowns().addCooldown(stack.getItem(), cd);
        }
        p.displayClientMessage(Component.literal(act.displayName()).withStyle(def.grade().colour()), true);
        double previousPower = ActPower.begin(Tiers.multiplier(stack, def, TierRules.Stat.ACT_POWER));      // 1.29.0: Fx damage inside this act is scaled by the tier
        try {
            act.body().run(new ActCtx(p, stack, def, sneaking, chargeTicks));
        } catch (RuntimeException | LinkageError failed) {
            org.slf4j.LoggerFactory.getLogger("nusmp").error("[nusmp] relic {} act {} failed", def.id(), act.key(), failed);
        } finally {
            ActPower.end(previousPower);
        }
        return true;
    }

    // ---------------------------------------------------------------- costs
    /**
     * Costs run on base Tensura only: mana is magicules (then aura for what they cannot cover, like the Kotodama words), existence points are the
     * magicule and aura pools both (never below 10% of either). No pool or meter of the relics' own.
     */
    private static boolean pay(ServerPlayer p, Act act, double costFactor) {
        if (p.isCreative() || (act.manaPercent() <= 0 && act.epPercentCost() <= 0)) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;                                                    // no Tensura data: nothing to spend
        double maxM = EnergyHelper.getMaxMagicule(p), maxA = EnergyHelper.getMaxAura(p);
        double mag = ex.getMagicule(), aura = ex.getAura();
        double ep = act.epPercentCost() * costFactor;
        if (ep > 0) {
            CostMath.Spend m = CostMath.spend(mag, maxM, ep, CostMath.EP_FLOOR);
            CostMath.Spend a = CostMath.spend(aura, maxA, ep, CostMath.EP_FLOOR);
            if (!m.ok() || !a.ok()) {
                tell(p, act.displayName() + ": not enough existence to spend (it would leave you under 10%).", ChatFormatting.RED);
                return false;
            }
            mag = m.after();
            aura = a.after();
        }
        if (act.manaPercent() > 0) {
            CostMath.Pooled c = CostMath.spendPooled(mag, aura, maxM * act.manaPercent() * costFactor / 100.0 * NUConfig.DM_COST_MULT.get());
            if (!c.ok()) {
                tell(p, act.displayName() + ": not enough mana.", ChatFormatting.RED);
                return false;
            }
            mag = c.first();
            aura = c.second();
        }
        ex.setMagicule(mag);
        ex.setAura(aura);
        ex.markDirty();
        return true;
    }

    // ---------------------------------------------------------------- cooldowns, toggles, uses
    private static CompoundTag data(Player p, boolean create) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            root.put(ROOT, new CompoundTag());
        }
        return root.getCompound(ROOT);
    }

    private static CompoundTag sub(Player p, String name, boolean create) {
        CompoundTag d = data(p, create);
        if (d == null) return null;
        if (!d.contains(name, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            d.put(name, new CompoundTag());
        }
        return d.getCompound(name);
    }

    private static String key(RelicDef def, Act act) { return def.pairBaseId() + "/" + act.key(); }      // 1.29.0: both halves of a pair share their cooldowns

    /** The game time this act is ready again (0 if never used). */
    public static long cooldownReadyAt(Player p, RelicDef def, Act act) {
        CompoundTag cd = sub(p, CD, false);
        return cd == null ? 0L : cd.getLong(key(def, act));
    }

    private static void setCooldown(ServerPlayer p, RelicDef def, Act act, long readyAt) {
        sub(p, CD, true).putLong(key(def, act), readyAt);
        PacketDistributor.sendToPlayer(p, new RelicCooldownPayload(def.id(), act.key(), readyAt));
        if (def.isPairHalf()) {                                                       // the HUD of the other half shows the same wait
            String other = def.pairPartnerId();
            if (other != null) PacketDistributor.sendToPlayer(p, new RelicCooldownPayload(other, act.key(), readyAt));
        }
    }

    /** Tells the client every cooldown still running (after login or a respawn). */
    static void syncCooldowns(ServerPlayer p) {
        CompoundTag cd = sub(p, CD, false);
        if (cd == null) return;
        long now = p.level().getGameTime();
        List<String> done = new ArrayList<>();
        for (String k : cd.getAllKeys()) {
            long until = cd.getLong(k);
            int slash = k.indexOf('/');
            if (until <= now || slash <= 0) { done.add(k); continue; }
            PacketDistributor.sendToPlayer(p, new RelicCooldownPayload(k.substring(0, slash), k.substring(slash + 1), until));
            RelicDef pd = Relics.byId(k.substring(0, slash));
            if (pd != null && pd.hasPair()) PacketDistributor.sendToPlayer(p, new RelicCooldownPayload(pd.pairPartnerId(), k.substring(slash + 1), until));      // 1.29.0: the left half's HUD
        }
        for (String k : done) cd.remove(k);
    }

    /** Is the toggle ability switched on for this player? */
    public static boolean isOn(Player p, String relicId, String actKey) {
        CompoundTag on = sub(p, ON, false);
        RelicDef d = Relics.byId(relicId);
        return on != null && on.getBoolean((d == null ? relicId : d.pairBaseId()) + "/" + actKey);
    }

    private static void flip(ServerPlayer p, RelicDef def, Act act) {
        CompoundTag on = sub(p, ON, true);
        String k = key(def, act);
        if (on.getBoolean(k)) on.remove(k);
        else on.putBoolean(k, true);
    }

    /** Uses of this act left today on this stack. */
    public static int usesLeft(ItemStack stack, Act act, long now) {
        CompoundTag all = stack.get(RelicComponents2.USES.get());
        CompoundTag c = all == null ? new CompoundTag() : all.getCompound(act.key());
        return UseRules.usesLeft(c.getLong("d"), c.getInt("n"), UseRules.day(now), act.usesPerDay());
    }

    private static void recordUse(ItemStack stack, Act act, long now) {
        CompoundTag old = stack.get(RelicComponents2.USES.get());
        CompoundTag all = old == null ? new CompoundTag() : old.copy();
        CompoundTag c = all.getCompound(act.key());
        long today = UseRules.day(now);
        int n = UseRules.usedAfter(c.getLong("d"), c.getInt("n"), today);
        CompoundTag fresh = new CompoundTag();
        fresh.putLong("d", today);
        fresh.putInt("n", n);
        all.put(act.key(), fresh);
        stack.set(RelicComponents2.USES.get(), all);
    }

    // ---------------------------------------------------------------- what a player carries
    /** Every stack the player carries that could be a relic: the whole inventory (hands and armour included) and the Curios slots. */
    static List<ItemStack> carriedStacks(Player p) {
        List<ItemStack> out = new ArrayList<>();
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) out.add(s);
        }
        out.addAll(CuriosLink.equipped(p));
        return out;
    }

    static void tell(ServerPlayer p, String text, ChatFormatting colour) { p.displayClientMessage(Component.literal(text).withStyle(colour), true); }
}
