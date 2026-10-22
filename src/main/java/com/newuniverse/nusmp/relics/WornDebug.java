package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.worn.WornAssets;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
import com.newuniverse.nusmp.client.relics.worn.WornVisibility;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.item.CuriosProbe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the server half of {@code /multiverse relic debug [player]}. For one wearer it prints, per relic: was it found worn (and where: a hand, the armour slots, a Curios slot, a mob's
 * pack), its slot and look, the model it resolves to in the jar and whether the file loads, the body part ("bone") each piece sits on, whether the atlas texture is in the jar, and
 * what {@link WornPlan} decides for it (drawn how, or why not). The Curios read is walked step by step ({@link CuriosProbe}) so a failing reflection names its step. The operator's
 * own client then adds its half (what the layer there found and decided, whether the layer was reached at all). Read only.
 */
public final class WornDebug {
    private WornDebug() {}

    /** The report: lines for the operator, and the compact "id|SLOT|WHERE" list sent to the operator's client. */
    public record Report(List<Component> lines, List<String> compact) {}

    private static Component t(String key, String fallback, Object... args) { return Component.translatableWithFallback("message.nusmp.relic_debug." + key, fallback, args); }

    private static Component lit(String s) { return Component.literal(s); }

    /** Builds the server's report for this wearer. */
    public static Report build(LivingEntity target) {
        List<Component> out = new ArrayList<>();
        List<String> compact = new ArrayList<>();
        out.add(t("title", "Worn relics of %s (server view)", target.getName().getString()));
        Relics.invalidate(target);                                                                   // a fresh look, not the ten-tick one

        CuriosProbe.Report probe = CuriosProbe.probe(target);
        out.add(t("curios", "Curios read: %s", probe.ok() ? t("ok", "every step passed") : t("broken", "A STEP FAILED, see below")));
        for (String l : probe.lines()) out.add(lit("  " + l));

        boolean atlas = WornAssets.exists(WornAssets.ATLAS), shimmer = WornAssets.exists(WornAssets.SHIMMER);
        out.add(t("textures", "Worn texture atlas in the jar: %s, shimmer: %s; catalog: %s", yes(atlas), yes(shimmer),
                WornAssets.catalogProblem() == null ? t("ok", "every step passed") : WornAssets.catalogProblem()));

        List<Relics.Located> located = Relics.locatedList(target);
        List<ItemStack> raw = CuriosLink.equipped(target);
        if (!raw.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ItemStack s : raw) names.add(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath() + (Relics.defOf(s) == null ? " (not a relic)" : ""));
            out.add(t("raw", "Items in Curios slots: %s", String.join(", ", names)));
        }
        if (located.isEmpty()) out.add(t("none", "No worn relic was found on this wearer (hand relics, armour relics, Curios slots, a mob's pack). Items that are not relics are not drawn by this layer."));

        List<WornPlan.Entry> entries = new ArrayList<>(located.size());
        java.util.Set<String> sentKeys = new java.util.HashSet<>(WornSync.keysOf(WornSync.lastSent(target.getUUID())));
        List<CuriosLink.RenderFlag> flags = CuriosLink.flags(target);
        for (Relics.Located l : located) {
            RelicDef d = l.def();
            Boolean eye = null;
            if (l.where() == Relics.Where.CURIOS) for (CuriosLink.RenderFlag f : flags) if (f.stack() == l.stack() || ItemStack.matches(f.stack(), l.stack())) { eye = f.render(); break; }
            WornAssets.Dress dress = d.slot().isHand() ? null : WornAssets.dress(d.id(), d.slot().name(), d.look().name());
            entries.add(new WornPlan.Entry(d.id(), d.slot().name(), d.look().name(), WornPlan.Source.valueOf(l.where().name()), eye,
                    WornAssets.CUSTOM_RENDERERS.contains(d.id()), dress != null && dress.loads(), d.grade().tier()));
            compact.add(d.id() + "|" + d.slot().name() + "|" + l.where().name());
        }
        WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(false, WornPlan.View.SELF_THIRD, false, false, false, true, false, WornPlan.Fallback.BOXES), entries);
        java.util.Map<String, net.neoforged.neoforge.items.IItemHandler> slotTypes = CuriosLink.slotHandlers(target);      // relics_wear: the slot types this wearer's Curios inventory really has
        RelicWear.Data wearData = wearData();
        out.add(t("wear_slots", "Curios slot types of this wearer: %s", CuriosLink.available() ? (slotTypes.isEmpty() ? "NONE (the inventory has no slot type: no slot is given to this entity)" : String.join(", ", slotTypes.keySet())) : "Curios is not available"));
        if (CuriosLink.available() && !slotTypes.isEmpty()) {
            List<String> missing = new ArrayList<>();
            for (String slot : RelicWear.RELIC_SLOTS) if (!slotTypes.containsKey(slot)) missing.add(slot);
            if (!missing.isEmpty()) out.add(t("wear_missing", "RELIC SLOT TYPES THE WEARER DOES NOT HAVE: %s (relics of these slots cannot be put on)", String.join(", ", missing)));
        }
        int n = 0;
        for (int i = 0; i < located.size(); i++) {
            Relics.Located l = located.get(i);
            RelicDef d = l.def();
            WornPlan.Item it = plan.all().get(i);
            n++;
            out.add(t("relic", "%s. %s (%s): slot %s, look %s, found in: %s%s", n, d.id(), d.grade().label(), d.slot().name(), d.look().name(), l.where().name().toLowerCase(),
                    d.slot().isCurio() ? ", Curios slot \"" + d.slot().curiosSlot() + "\"" : ""));
            if (d.slot().isHand()) {
                out.add(lit("     " + it.why().text));
                continue;
            }
            if (WornAssets.CUSTOM_RENDERERS.contains(d.id())) out.add(t("custom", "     own renderer: yes (it draws itself from the posed model; the library is not used)"));
            WornAssets.Dress dress = WornAssets.dress(d.id(), d.slot().name(), d.look().name());
            out.add(t("catalog", "     catalog route: %s", dress.source()));
            for (String line : dress.lines()) out.add(lit("     " + line));
            out.add(t("plan", "     decision: %s", it.describe()));
            out.add(wearLine(l, d, it, slotTypes, wearData, sentKeys));
        }
        List<ItemStack> sent = WornSync.lastSent(target.getUUID());
        out.add(t("sent", "Worn list last sent to clients: %s", sent.isEmpty() ? "-" : String.join(", ", WornSync.keysOf(sent))));
        List<String> causes = WornOnce.causes();
        if (!causes.isEmpty()) {
            out.add(t("causes", "Causes noted on this machine since the game started (each once):"));
            for (String c : causes) out.add(lit("  " + c));
        }
        return new Report(out, compact);
    }

    private static volatile RelicWear.Data wearData;

    /** The mod's own slot, entity and tag files, read once from the jar (empty data if they cannot be read; the report then says every slot is undefined). */
    private static RelicWear.Data wearData() {
        RelicWear.Data d = wearData;
        if (d != null) return d;
        try {
            d = RelicWear.read(RelicWear.classPath(WornDebug.class.getClassLoader()));
        } catch (java.io.IOException | RuntimeException unreadable) {
            d = new RelicWear.Data(java.util.Map.of(), java.util.Map.of(), java.util.Set.of());
        }
        wearData = d;
        return d;
    }

    /**
     * relics_wear: one line per worn relic with the facts behind "can it be put on and is it drawn": its slot, whether it is equipped, whether the slot type is defined by the mod's data and
     * really present in the wearer's Curios inventory, whether the item is in the slot's tag (the mod's file and the game's own tag), whether the server's list sent to clients holds it, and
     * whether it is visible with the default options (nothing switched off, third person, near) or the one reason it is not ({@link WornVisibility}).
     */
    private static Component wearLine(Relics.Located l, RelicDef d, WornPlan.Item it, java.util.Map<String, net.neoforged.neoforge.items.IItemHandler> slotTypes, RelicWear.Data data, java.util.Set<String> sent) {
        boolean armour = d.slot().isArmor();
        String itemId = BuiltInRegistries.ITEM.getKey(l.stack().getItem()).toString();
        boolean equipped = armour ? l.where() == Relics.Where.ARMOUR : l.where() == Relics.Where.CURIOS || l.where() == Relics.Where.PACK;
        String slot = armour ? "armour " + d.slot().name().toLowerCase() : d.slot().curiosSlot();
        String defined, tag;
        if (armour) {
            defined = "n/a (real armour)";
            tag = "n/a";
        } else {
            String id = d.slot().curiosSlot();
            defined = "mod file " + yes(data.defines(id)) + ", in this wearer's inventory " + (CuriosLink.available() ? yes(slotTypes.containsKey(id)) : "n/a (no Curios)");
            boolean live = l.stack().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios", id)));
            java.util.Set<String> file = data.tags().get(id);
            tag = "game tag " + yes(live) + ", mod file " + yes(file != null && file.contains(itemId));
        }
        WornVisibility.Result v = WornVisibility.decide(armour ? WornVisibility.Source.ARMOUR : WornVisibility.Source.CURIOS, d.slot().name(), it.entry().eye(), it.entry().custom(),
                false, true, WornPlan.View.SELF_THIRD, false, false, false);
        return t("wear", "     wear: slot %s, equipped %s, slot defined: %s, tag: %s, sent to clients %s, visible by default %s%s", slot, yes(equipped), defined, tag,
                armour ? "n/a (vanilla syncs armour)" : yes(sent.contains(itemId)), yes(v.visible()), v.visible() ? "" : " (hidden: " + v.reason().text + ")");
    }

    private static String yes(boolean b) { return b ? "yes" : "NO"; }
}
