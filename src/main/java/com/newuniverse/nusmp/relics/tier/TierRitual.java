package com.newuniverse.nusmp.relics.tier;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.CostMath;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.tier.TierRules.Catalyst;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The upgrade ritual: sneak and use a catalyst with the relic in the other hand (or sneak and use the relic with the catalyst in the other hand).
 * Order of things, every step before the next so nothing is half-paid: the checks (a relic, attuned if it needs that, not at the top, the right
 * catalyst in the right number, the cooldown, enough magicules and aura), then the pool is paid and the catalyst taken, then the roll. A success
 * writes the tier onto the very stack (its serial and attunement are untouched, so the Order's ledger still knows it). A failure never harms the
 * relic: it sets a cooldown and, from the Unique jump up, costs the catalysts as well.
 */
public final class TierRitual {
    private TierRitual() {}

    private static final String ROOT = "nusmp_tier", UNTIL = "until";
    private static volatile Set<String> catalystIds;

    public static void register(IEventBus gameBus) { gameBus.addListener(EventPriority.HIGH, TierRitual::onUse); }

    private static Set<String> catalystIds() {
        Set<String> s = catalystIds;
        if (s == null) {
            s = new HashSet<>();
            for (int g = 1; g <= TierRules.MAX_TIER; g++) for (Catalyst c : TierRules.catalystsTo(g)) s.add(c.itemId());
            catalystIds = s;
        }
        return s;
    }

    private static String idOf(ItemStack s) { return s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()).toString(); }

    private static boolean isCatalyst(ItemStack s) { return !s.isEmpty() && catalystIds().contains(idOf(s)); }

    private static boolean ritualRelic(ItemStack s) {
        RelicDef d = Relics.defOf(s);
        return d != null && d.slot() != RelicSlot.CONSUMABLE;
    }

    /** For the Relic Forge's slot rules: is this stack one of the accepted catalyst items (of any tier jump)? */
    public static boolean acceptsCatalyst(ItemStack s) { return isCatalyst(s); }

    /** For the Relic Forge's slot rules: may this stack go through the ritual (a relic that is not a consumable)? */
    public static boolean acceptsRelic(ItemStack s) { return ritualRelic(s); }

    /** Both sides: cancels the click so the relic's own act does not fire; only the server performs the ritual. */
    static void onUse(PlayerInteractEvent.RightClickItem e) {
        Player p = e.getEntity();
        if (!p.isShiftKeyDown()) return;
        InteractionHand other = e.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack used = e.getItemStack(), second = p.getItemInHand(other);
        ItemStack relic, catalyst;
        if (isCatalyst(used) && ritualRelic(second)) { catalyst = used; relic = second; }
        else if (ritualRelic(used) && isCatalyst(second)) { relic = used; catalyst = second; }
        else return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.CONSUME);
        if (e.getLevel().isClientSide() || !(p instanceof ServerPlayer sp)) return;
        attempt(sp, relic, catalyst);
    }

    /** Runs the ritual once. Returns true if the tier went up. */
    public static boolean attempt(ServerPlayer p, ItemStack relic, ItemStack catalyst) {
        return run(p, relic, List.of(catalyst)).success();
    }

    /** Why a ritual ended the way it did (the Relic Forge shows a line per reason; the sneak-use flow only needs the messages). */
    public enum Reason { SUCCESS, NOT_A_RELIC, COOLDOWN, UNATTUNED, AT_TOP, WRONG_CATALYST, NEED_COUNT, NO_ENERGY, FAILED_KEPT, FAILED_LOST }

    /** The result of one ritual: the reason, whether the tier went up, and the grade ordinal that was aimed for (-1 if none). */
    public record Result(Reason reason, boolean success, int target) {}

    /**
     * The Relic Forge's way in (1.41.0): the same ritual, same checks, same cost and roll, but the catalysts come from several menu slots and the answer
     * is a {@link Result}. {@code relic} is edited in place (the tier is written onto that very stack), the catalyst stacks are shrunk in place.
     */
    public static Result attemptFromForge(ServerPlayer p, ItemStack relic, List<ItemStack> catalystSlots) {
        return run(p, relic, catalystSlots == null ? List.<ItemStack>of() : catalystSlots);
    }

    /** Ticks left of the player's ritual cooldown (0 when ready); the forge shows it as a refusal before the player clicks. */
    public static long cooldownLeft(Player p) {
        CompoundTag r = root(p, false);
        long until = r == null ? 0L : r.getLong(UNTIL);
        return Math.max(0L, until - p.level().getGameTime());
    }

    /**
     * The one body of the ritual. Several catalyst stacks are allowed: the first accepted catalyst item present among them is the one used, and its
     * stacks are added together for the count (a single stack, the sneak-use case, behaves exactly as before).
     */
    private static Result run(ServerPlayer p, ItemStack relic, List<ItemStack> catalysts) {
        RelicDef def = Relics.defOf(relic);
        if (def == null) return new Result(Reason.NOT_A_RELIC, false, -1);
        long now = p.level().getGameTime();
        long until = root(p, false) == null ? 0L : root(p, false).getLong(UNTIL);
        if (now < until) {
            say(p, Component.translatable("nusmp.tier.cooldown", (until - now + 19) / 20).withStyle(ChatFormatting.GRAY));
            return new Result(Reason.COOLDOWN, false, -1);
        }
        if (!Attune.works(relic, def, p)) {
            say(p, Component.translatable("nusmp.tier.unattuned").withStyle(ChatFormatting.LIGHT_PURPLE));
            return new Result(Reason.UNATTUNED, false, -1);
        }
        Grade eff = Tiers.effectiveGrade(relic, def);
        int target = TierRules.nextTarget(eff.ordinal());
        if (target < 0) {
            say(p, Component.translatable("nusmp.tier.top").withStyle(ChatFormatting.GOLD));
            return new Result(Reason.AT_TOP, false, -1);
        }
        Grade to = Grade.values()[target];
        Catalyst need = null;
        List<ItemStack> matched = new ArrayList<>();
        for (Catalyst c : TierRules.catalystsTo(target)) {
            for (ItemStack s : catalysts) if (!s.isEmpty() && idOf(s).equals(c.itemId())) matched.add(s);
            if (!matched.isEmpty()) { need = c; break; }
        }
        if (need == null) {
            say(p, Component.translatable("nusmp.tier.wrong_catalyst", to.label(), catalystList(target)).withStyle(ChatFormatting.RED));
            return new Result(Reason.WRONG_CATALYST, false, target);
        }
        int have = 0;
        for (ItemStack s : matched) have += s.getCount();
        if (!p.isCreative() && have < need.count()) {
            say(p, Component.translatable("nusmp.tier.need_count", need.count(), matched.get(0).getHoverName()).withStyle(ChatFormatting.RED));
            return new Result(Reason.NEED_COUNT, false, target);
        }
        if (!payEnergy(p, TierRules.costPercent(target))) {
            say(p, Component.translatable("nusmp.tier.no_energy", Math.round(TierRules.costPercent(target))).withStyle(ChatFormatting.RED));
            return new Result(Reason.NO_ENERGY, false, target);
        }
        boolean win = TierRules.succeeds(target, p.getRandom().nextDouble());
        boolean lose = win || TierRules.losesCatalystOnFail(target);
        if (lose && !p.isCreative()) {
            int left = need.count();
            for (ItemStack s : matched) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        if (win) {
            Tiers.setTier(relic, target);
            com.newuniverse.nusmp.quests.QuestHooks.relicTier(p, to);
            Relics.invalidate(p);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.4f);
            Fx.ring(p.serverLevel(), p.position(), 1.8, to.rgb());
            Fx.burst(p.serverLevel(), p.position(), to.rgb());
            setCooldown(p, now + TierRules.SUCCESS_COOLDOWN_TICKS);
            say(p, Component.translatable("nusmp.tier.success", relic.getHoverName(), to.label()).withStyle(to.colour()));
            return new Result(Reason.SUCCESS, true, target);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 0.6f, 0.7f);
        setCooldown(p, now + TierRules.failCooldownTicks(target));
        say(p, Component.translatable(lose ? "nusmp.tier.fail_lost" : "nusmp.tier.fail_kept", relic.getHoverName()).withStyle(ChatFormatting.RED));
        return new Result(lose ? Reason.FAILED_LOST : Reason.FAILED_KEPT, false, target);
    }

    /** "2x Low Magisteel Ingot / ..." for the failure message: the registry paths, which are readable enough and need no client lookup. */
    private static String catalystList(int target) {
        StringBuilder b = new StringBuilder();
        for (Catalyst c : TierRules.catalystsTo(target)) {
            if (b.length() > 0) b.append(" / ");
            b.append(c.count()).append("x ").append(c.itemId());
        }
        return b.toString();
    }

    private static void say(ServerPlayer p, Component c) { p.displayClientMessage(c, true); }

    /** Pays {@code percent} of the maximum magicule pool, magicules first, then aura (the way relic acts pay). No Tensura data or creative: free. */
    static boolean payEnergy(ServerPlayer p, double percent) {
        if (p.isCreative() || percent <= 0) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        double cost = EnergyHelper.getMaxMagicule(p) * percent / 100.0 * NUConfig.DM_COST_MULT.get();
        CostMath.Pooled c = CostMath.spendPooled(ex.getMagicule(), ex.getAura(), cost);
        if (!c.ok()) return false;
        ex.setMagicule(c.first());
        ex.setAura(c.second());
        ex.markDirty();
        return true;
    }

    // ---------------------------------------------------------------- the player's cooldown (survives relog and death)
    private static CompoundTag root(Player p, boolean create) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) {
            if (!create) return null;
            pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT, 10)) {
            if (!create) return null;
            persisted.put(ROOT, new CompoundTag());
        }
        return persisted.getCompound(ROOT);
    }

    private static void setCooldown(Player p, long untilTick) { root(p, true).putLong(UNTIL, untilTick); }
}
