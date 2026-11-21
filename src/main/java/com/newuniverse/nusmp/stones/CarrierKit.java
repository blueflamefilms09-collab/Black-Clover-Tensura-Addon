package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Why: the hooks that turn the Infinity Gauntlet, the new Nano Gauntlet and Loki's Scepter into stone carriers, kept in one place so the shared files only gain a line each. {@link #defs} makes the two new
 * relics; {@link #extend} appends the carrier acts to a gauntlet (socket, unsocket, every socketed stone's acts, the two combos, the Snap mode and the Snap) or to the scepter (socket, unsocket, the Mind
 * Stone's four acts, the blast and the barrier), leaving every old act in place; {@link #refuse} stops a carrier act before it costs anything (setting off, gate not met, stone not socketed, hand held,
 * Snap not ready); {@link #circuitCooldown} gives the comics circuit's -15% cooldown. Server side; nothing here touches the world.
 */
public final class CarrierKit {
    private CarrierKit() {}

    private static final String[] RESTRAINTS = {"tensura:paralysis", "tensura:petrification", "tensura:webbed", "tensura:stun"};

    /** The two relics Phase B adds: the Nano Gauntlet (hands) and Loki's Scepter (a weapon). */
    public static void defs(Consumer<RelicDef> out) {
        out.accept(RelicDef.of(CarrierRules.NANO_ID, "The Nano Gauntlet", Grade.GENESIS, RelicSlot.HANDS).attune().vessel()
                .palette(0x2A2E3C, 0xC8362E, 0xFFB43A).look(Look.GAUNTLETS)
                .desc("Stark's nanotech copy of the Infinity Gauntlet: it fits any hand and holds all six stones. Socketing stings twice as hard and a Snap through it leaves a burned arm for an hour. Needs the gate of 1 billion base EP.")
                .act(Act.of("nano_status", "Read the Nano Gauntlet", CarrierActs::status).cooldown(20)));
        out.accept(RelicDef.of(CarrierRules.SCEPTER_ID, "Loki's Scepter", Grade.GOD, RelicSlot.MAIN_HAND).vessel()
                .palette(0xC9A24A, 0x3A8BFF, 0xFFD93A).look(Look.STAFF).melee(8.0, 1.6)
                .engrave("magic_weapon", "severance@2", "magic_interference")
                .desc("A golden spear-staff with a blade, the conduit of the Mind Stone. Socket the Mind Stone to blast, ward and think with it. It never takes a will.")
                .act(Act.of("scepter_read", "Read the Scepter", CarrierActs::status).cooldown(20)));
    }

    /** Adds the relic ids of the stones socketed in this stack (a gauntlet or the scepter) to the set; nothing for any other stack. The Wave 30 gauntlet reads "the stones you carry" through it. */
    public static void addSocketed(ItemStack s, java.util.Set<String> out) {
        if (s == null || s.isEmpty() || !s.has(net.minecraft.core.component.DataComponents.CONTAINER)) return;
        int mask = CarrierData.mask(s);
        if (mask == 0) return;
        for (InfinityStone st : InfinityStone.values()) if (CarrierRules.has(mask, st)) out.add(st.relicId());
    }

    public static boolean isCarrierAct(String key) {
        return key != null && (key.startsWith("cs_") || key.startsWith("sc_"));
    }

    /** Appends the carrier acts of a gauntlet or the scepter (idempotent). Called by {@link StoneKit#extend}. */
    static RelicDef extend(RelicDef d) {
        boolean gauntlet = CarrierRules.isGauntlet(d.id()), scepter = CarrierRules.SCEPTER_ID.equals(d.id());
        if (!gauntlet && !scepter) return d;
        if (d.actByKey("cs_socket") != null) return d;
        d = d.act(Act.of("cs_socket", "Socket a Stone", CarrierActs::socket).cooldown(40))
                .act(Act.of("cs_unsocket", "Unsocket the Stones", CarrierActs::unsocket).cooldown(100));
        for (InfinityStone s : InfinityStone.values()) {
            if (scepter && s != InfinityStone.MIND) continue;
            for (Act a : StoneKit.actsOf(s)) {
                InfinityStone st = s;
                Act mine = Act.of(CarrierRules.carrierKey(s, indexOf(s, a)), a.displayName(), ctx -> CarrierActs.cast(ctx, st, a.body()))
                        .cooldown(a.cooldownTicks()).mana(a.manaPercent());
                d = d.act(mine);
            }
        }
        if (gauntlet) {
            d = d.act(Act.of("cs_combo_mend", "Combo: Mend", CarrierActs::comboMend).cooldown(2400).mana(10))
                    .act(Act.of("cs_combo_rift", "Combo: Rift", CarrierActs::comboRift).cooldown(1200).mana(8))
                    .act(Act.of("cs_snap_mode", "Choose the Snap", SnapActs::chooseMode).cooldown(10))
                    .act(Act.of("cs_snap", "The Snap", SnapActs::snap).hold(100).cooldown(200).epPercent(8.0));
        } else {
            d = d.act(Act.of("sc_blast", "Scepter Blast", ScepterActs::blast).cooldown(120).mana(2))
                    .act(Act.of("sc_barrier", "Scepter Barrier", ScepterActs::barrier).cooldown(600).mana(4));
        }
        return d;
    }

    private static int indexOf(InfinityStone s, Act slotAct) {
        String k = slotAct.key();
        return Integer.parseInt(k.substring(k.lastIndexOf('_') + 1));
    }

    // ---------------------------------------------------------------- refusals (nothing is paid for a refused act)
    /** The reason a carrier act may not run now, or null. Reads the stack the act is cast from. */
    static String refuse(ServerPlayer p, ItemStack stack, RelicDef def, Act act) {
        String key = act.key();
        boolean scepterAct = key.startsWith("sc_") || CarrierRules.SCEPTER_ID.equals(def.id());
        if (!StoneConfig.enabled()) return "The Infinity Stones are switched off on this server.";
        if (scepterAct ? !StoneConfig.scepter() : !StoneConfig.carriers()) return (scepterAct ? "Loki's Scepter" : "The gauntlet's sockets") + " is switched off on this server.";
        int mask = CarrierData.mask(stack);
        boolean gate = StoneSlot.gateOk(p);
        String needGate = "The stones burn: you need " + StoneActs.short_(StoneConfig.epGate()) + " base maximum EP to use them.";
        switch (key) {
            case "cs_socket":
                if (!gate) return needGate;
                return CarrierActs.nextSocketable(p, stack) == null ? "You carry no stone that fits an empty socket here." : null;
            case "cs_unsocket":
                return mask == 0 ? "Nothing is socketed." : null;
            case "cs_snap_mode":
                return StoneConfig.snap() ? null : "The Snap is switched off on this server.";
            case "cs_snap": {
                if (!CarrierRules.isGauntlet(def.id())) return "Only a gauntlet can snap.";
                long now = p.level().getGameTime();
                String r = CarrierRules.snapRefusal(mask, StoneConfig.snap(), gate, restrained(p), now, SnapActs.readyAt(p));
                if (r != null) return r;
                if (CarrierData.mode(stack) == CarrierRules.SnapMode.ERASE && !StoneConfig.snapErase()) return "The Erase Snap is switched off on this server; choose another.";
                return null;
            }
            case "cs_combo_mend":
                if (!StoneConfig.combos()) return "Combo casts are switched off on this server.";
                return !CarrierRules.mendReady(mask) ? "Mend needs the Soul, Time and Reality stones socketed." : stoneUse(p, gate, needGate);
            case "cs_combo_rift":
                if (!StoneConfig.combos()) return "Combo casts are switched off on this server.";
                return !CarrierRules.riftReady(mask) ? "Rift needs the Space and Power stones socketed." : stoneUse(p, gate, needGate);
            case "sc_blast":
            case "sc_barrier":
                return !CarrierRules.has(mask, InfinityStone.MIND) ? "Socket the Mind Stone in the scepter first." : stoneUse(p, gate, needGate);
            default:
                break;
        }
        InfinityStone s = CarrierRules.carrierStone(key);
        if (s == null) return null;
        if (!CarrierRules.has(mask, s)) return "The " + s.id() + " stone is not socketed here.";
        return stoneUse(p, gate, needGate);
    }

    private static String stoneUse(ServerPlayer p, boolean gate, String needGate) {
        if (!gate) return needGate;
        if (restrained(p)) return "Your hand is held: the stones will not answer.";
        return null;
    }

    /** A stun, bind, paralysis or petrification blocks every use (canon: restraining the hand blocks the gauntlet). */
    static boolean restrained(ServerPlayer p) {
        for (String id : RESTRAINTS) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) continue;
            Holder<MobEffect> h = BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(x -> (Holder<MobEffect>) x).orElse(null);
            if (h != null && p.hasEffect(h)) return true;
        }
        return false;
    }

    /** The comics circuit's -15% cooldown for a carrier act of a stone whose predecessor is socketed beside it (1.0 otherwise). */
    static double circuitCooldown(ItemStack stack, Act act) {
        InfinityStone s = CarrierRules.carrierStone(act.key());
        if (s == null) return 1.0;
        return CarrierRules.cooldownFactor(CarrierData.mask(stack), s, StoneConfig.synergy());
    }
}
