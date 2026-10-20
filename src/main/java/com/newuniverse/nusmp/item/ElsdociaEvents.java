package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.97.0: Elsdocia's lineage in the world (docs/elsdocia_legacy.md). Three hooks and one rule:
 * <ul>
 *   <li><b>Damage</b>: {@link ItemAttributeModifierEvent} sets the attack damage of every Elsdocia stack from its lineage (400, then +25 per pass to a
 *       new Grand Magic rank owner). The generic Genesis range of relics/GradeDamage is not applied to it ({@link ElsdociaRules#isLegacyException}).</li>
 *   <li><b>Possession</b>: "passed" means the stack is found in ANOTHER player's inventory (or on their cursor): a trade, a chest, a dropped item picked up,
 *       a death drop. The player tick looks every 5 ticks; the first player to hold it claims it (no pass); a different player becomes the owner
 *       and, if Grand Magic rank or above and never an owner before, adds +25.</li>
 *   <li><b>No way back</b>: the LAST owner (the one just before the current one) cannot pick it up from the ground ({@link ItemEntityPickupEvent.Pre}
 *       is cancelled); if it still reaches their inventory (a chest, a trade) the next look drops it at their feet. Earlier owners may take it again.</li>
 * </ul>
 */
public final class ElsdociaEvents {
    private ElsdociaEvents() {}

    public static final String LEFT_YOU = "Elsdocia has left you and will not return to your hand";
    private static final String K_MSG = "nusmp_elsdocia_msg";
    private static final int SCAN_EVERY = 5;
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section SCAN_SECTION = com.newuniverse.nusmp.perf.PerfProbe.section("s.elsdocia.scan");

    public enum Result { CLAIMED, SAME, PASSED, BLOCKED }

    public static void registerContent(IEventBus modBus) { ElsdociaComponents.COMPONENTS.register(modBus); }

    public static void registerGame(IEventBus gameBus) {
        gameBus.addListener(ElsdociaEvents::onAttributes);
        gameBus.addListener(ElsdociaEvents::onPickup);
        gameBus.addListener(ElsdociaEvents::onPlayerTick);
        ElsdociaSelfTests.register();
    }

    // ---------------------------------------------------------------- damage
    static void onAttributes(ItemAttributeModifierEvent e) {
        ItemStack stack = e.getItemStack();
        if (!ElsdociaRules.isLegacyException(stack)) return;
        double amount = ElsdociaRules.attributeAmount(ElsdociaComponents.get(stack).bonuses());
        AttributeModifier old = null;
        for (ItemAttributeModifiers.Entry en : new ArrayList<>(e.getModifiers()))
            if (en.attribute().value() == Attributes.ATTACK_DAMAGE.value() && en.modifier().operation() == AttributeModifier.Operation.ADD_VALUE
                    && en.slot() == EquipmentSlotGroup.MAINHAND) { old = en.modifier(); break; }
        if (old == null) return;                                                         // the sword always carries one; nothing to replace otherwise
        e.replaceModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(old.id(), amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    // ---------------------------------------------------------------- possession
    static void onPickup(ItemEntityPickupEvent.Pre e) {
        ItemStack s = e.getItemEntity().getItem();
        if (!ElsdociaRules.isLegacyException(s)) return;
        if (ElsdociaComponents.get(s).verdict(e.getPlayer().getUUID()) != ElsdociaLineage.Verdict.BLOCKED) return;
        e.setCanPickup(TriState.FALSE);
        if (e.getPlayer() instanceof ServerPlayer p) leftYou(p);
    }

    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || p.tickCount % com.newuniverse.nusmp.perf.PerfSettings.entityScanInterval() != 0) return;      // 1.99.0: SCAN_EVERY (5) is the default of entityScanInterval
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        scan(p);
        com.newuniverse.nusmp.perf.PerfProbe.end(SCAN_SECTION, t0);
    }

    /** Looks at everything the player carries and settles each Elsdocia found. */
    public static void scan(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (ElsdociaRules.isLegacyException(s)) settle(p, s);
        }
        ItemStack carried = p.containerMenu == null ? ItemStack.EMPTY : p.containerMenu.getCarried();
        if (ElsdociaRules.isLegacyException(carried)) settle(p, carried);
    }

    /** The player's current Black Clover magic type name ("" = none yet) and Magic Knight rank step. */
    public static String magicOf(ServerPlayer p) {
        return GrimoirePages.grimoireOf(p).map(i -> {
            MagicType m = GrimoirePages.magicOf(i);
            return m == null ? "" : m.name();
        }).orElse("");
    }

    /** Settles one stack found with this player: claims, passes, or (the last owner) puts it out of their hands. */
    public static Result settle(ServerPlayer p, ItemStack stack) {
        return settle(p, stack, magicOf(p), MultiverseProfile.step(p));
    }

    /** The same with the magic and rank given (the self-test and admin command use it). */
    public static Result settle(ServerPlayer p, ItemStack stack, String magic, int rank) {
        ElsdociaLineage before = ElsdociaComponents.get(stack);
        ElsdociaLineage.Verdict v = before.verdict(p.getUUID());
        if (v == ElsdociaLineage.Verdict.BLOCKED) {
            ItemStack copy = stack.copy();
            stack.setCount(0);
            p.drop(copy, false);
            leftYou(p);
            return Result.BLOCKED;
        }
        ElsdociaLineage after = before.take(p.getUUID(), p.getGameProfile().getName(), magic, rank);
        if (!after.equals(before)) ElsdociaComponents.set(stack, after);
        if (v == ElsdociaLineage.Verdict.SAME) {
            int learnedBefore = before.learned().size(), learnedAfter = after.learned().size();
            if (learnedAfter > learnedBefore) learnedMessage(p, after.learned().get(learnedAfter - 1));
            return Result.SAME;
        }
        if (v == ElsdociaLineage.Verdict.CLAIM) {
            p.sendSystemMessage(Component.literal("Elsdocia answers to you. Its legacy is yours to carry, and to pass on.").withStyle(ChatFormatting.AQUA));
            if (!after.learned().isEmpty()) learnedMessage(p, after.learned().get(after.learned().size() - 1));
            return Result.CLAIMED;
        }
        boolean bonus = after.bonuses() > before.bonuses();
        p.sendSystemMessage(Component.literal("Elsdocia passes to your hand" + (bonus ? ": a Grand Magic hand, +" + ElsdociaRules.GRAND_STEP_DAMAGE + " damage (" + after.damage() + ")." : "."))
                .withStyle(bonus ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        if (after.learned().size() > before.learned().size()) learnedMessage(p, after.learned().get(after.learned().size() - 1));
        return Result.PASSED;
    }

    private static void learnedMessage(ServerPlayer p, String magic) {
        MagicType m = MagicType.lookup(magic);
        p.sendSystemMessage(Component.literal("Legacy of the Wizard Kings: Elsdocia learns " + (m == null ? magic : m.displayName) + ".").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** The refusal message, at most once a second. */
    static void leftYou(ServerPlayer p) {
        long now = p.level().getGameTime();
        if (now - p.getPersistentData().getLong(K_MSG) < 20 && now >= p.getPersistentData().getLong(K_MSG)) return;
        p.getPersistentData().putLong(K_MSG, now);
        p.displayClientMessage(Component.literal(LEFT_YOU).withStyle(ChatFormatting.RED), true);
    }

    /** Every Elsdocia the player carries, for the command. */
    static List<ItemStack> carried(ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (ElsdociaRules.isLegacyException(inv.getItem(i))) out.add(inv.getItem(i));
        return out;
    }
}
