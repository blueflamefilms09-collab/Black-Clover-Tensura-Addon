package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.RelicDef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Map;

/**
 * Why: the hooks the relic core calls, kept in one place so the shared files only gain a line each. {@link #extend} gives each of the six Wave 30 stones its two slot abilities when the catalog is
 * built (the old acts, passives and ids stay exactly as they were); {@link #refuse} stops a slot ability before it costs anything when the stone is not in the slot, the gate is not met or the
 * setting is off; {@link #costFactor} and {@link #cooldownFactor} are the Power and Time stones' lend to every relic act of the bearer; {@link #slotFor} sends a stone to the infinity_stone slot
 * on a right-click when the gate is met and the slot is free (otherwise it goes to the charm slot as it always did).
 */
public final class StoneKit {
    private StoneKit() {}

    /** The slot acts of a stone, in order (two each; the Mind Stone has four since Phase B: Telekinesis and Mind Link were appended). The same bodies run through a gauntlet or the scepter (see {@link CarrierKit}). */
    static java.util.List<Act> actsOf(InfinityStone s) {
        return switch (s) {
            case SPACE -> java.util.List.of(Act.of(s.actKey(1), "Portal Step", StoneActs::portalStep).cooldown(160).mana(3),
                    Act.of(s.actKey(2), "Spatial Fold", StoneActs::spatialFold).cooldown(500).mana(6));
            case MIND -> java.util.List.of(Act.of(s.actKey(1), "Psionic Lance", StoneActs::psionicLance).cooldown(300).mana(4),
                    Act.of(s.actKey(2), "Psionic Reach", StoneActs::psionicReach).cooldown(600).mana(2),
                    Act.of(s.actKey(3), "Telekinesis", MindActs::telekinesis).cooldown(300).mana(3),
                    Act.of(s.actKey(4), "Mind Link", MindActs::mindLink).cooldown(900).mana(4));
            case POWER -> java.util.List.of(Act.of(s.actKey(1), "Power Surge", StoneActs::powerSurge).cooldown(400).mana(4),
                    Act.of(s.actKey(2), "Infinite Might", StoneActs::infiniteMight).cooldown(2400).mana(10));
            case TIME -> java.util.List.of(Act.of(s.actKey(1), "Rewind", StoneActs::rewind).cooldown(1200).mana(8),
                    Act.of(s.actKey(2), "Time Lock", StoneActs::timeLock).cooldown(2400).mana(12));
            case REALITY -> java.util.List.of(Act.of(s.actKey(1), "Rewrite", StoneActs::rewrite).cooldown(900).mana(7),
                    Act.of(s.actKey(2), "False World", StoneActs::falseWorld).cooldown(1200).mana(6));
            case SOUL -> java.util.List.of(Act.of(s.actKey(1), "Soul Grasp", StoneActs::soulGrasp).cooldown(1800).mana(9),
                    Act.of(s.actKey(2), "Soulworld", StoneActs::soulworld).cooldown(2400).mana(6));
        };
    }

    /** Appends the slot acts to a stone's def (idempotent; a def that already has act 1 gets only the acts it lacks, so a Phase A def gains Telekinesis and Mind Link); a gauntlet or the scepter is extended by {@link CarrierKit}; any other def is returned as it was. Called once per def while the catalog is built. */
    public static RelicDef extend(RelicDef d) {
        if (d == null) return d;
        if (CarrierRules.allowedMask(d.id()) != 0) return CarrierKit.extend(d);
        InfinityStone s = InfinityStone.byRelicId(d.id());
        if (s == null) return d;
        for (Act a : actsOf(s)) if (d.actByKey(a.key()) == null) d = d.act(a);
        return d;
    }

    /** The reason a slot act may not run now (shown to the player, nothing is spent), or null when it may (or when the act is not a slot act). Server side; reads the slot fresh. */
    public static String refuse(ServerPlayer p, RelicDef def, Act act) {
        InfinityStone s = act == null ? null : InfinityStone.ofActKey(act.key());
        if (s == null) return null;
        if (!StoneConfig.enabled()) return "The Infinity Stone slot is switched off on this server.";
        StoneSlot.refresh(p);
        StoneSlot.State st = StoneSlot.state(p);
        if (st.stone() != s) return "Wear the " + def.cardName() + " in the Infinity Stone slot to use " + act.displayName() + ".";
        if (!st.working()) return "The stone sleeps: it needs " + StoneActs.short_(StoneConfig.epGate()) + " base maximum EP.";
        return null;
    }

    /** {@link #refuse(ServerPlayer, RelicDef, Act)} for every act: also the carrier acts (gauntlet and scepter), which read the stack they are cast from. */
    public static String refuse(ServerPlayer p, ItemStack stack, RelicDef def, Act act) {
        if (act != null && CarrierKit.isCarrierAct(act.key())) return CarrierKit.refuse(p, stack, def, act);
        return refuse(p, def, act);
    }

    /** The share of an act's cooldown that the worn stone leaves times the circuit's -15% for a carrier act of a boosted stone. */
    public static double cooldownFactor(ServerPlayer p, ItemStack stack, Act act) {
        return cooldownFactor(p) * (act != null && CarrierKit.isCarrierAct(act.key()) ? CarrierKit.circuitCooldown(stack, act) : 1.0);
    }

    /** The share of an act's cost that the worn stone leaves (the Power Stone: 0.85); 1.0 for everyone else. One map read. */
    public static double costFactor(ServerPlayer p) {
        return StoneRules.costFactor(StoneSlot.active(p));
    }

    /** The share of an act's cooldown that the worn stone leaves (Power 0.8, Time 0.75); 1.0 for everyone else. One map read. */
    public static double cooldownFactor(ServerPlayer p) {
        return StoneRules.cooldownFactor(StoneSlot.active(p));
    }

    /** The Curios slot a right-clicked relic should go to: the infinity_stone slot for a stone when the gate is met and the slot is free, else the slot the relic would use anyway. */
    public static String slotFor(ServerPlayer p, RelicDef def, String want, Map<String, IItemHandler> handlers) {
        if (def == null || InfinityStone.byRelicId(def.id()) == null || !StoneConfig.enabled() || !StoneSlot.gateOk(p)) return want;
        IItemHandler h = handlers.get(StoneRules.SLOT);
        if (h == null) return want;
        for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) return want;
        return StoneRules.SLOT;
    }
}
