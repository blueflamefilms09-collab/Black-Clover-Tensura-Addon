package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.worn.WornAssets;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
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
        }
        List<ItemStack> sent = WornSync.lastSent(target.getUUID());
        out.add(t("sent", "Worn list last sent to clients: %s", sent.isEmpty() ? "-" : String.join(", ", WornSync.keysOf(sent))));
        return new Report(out, compact);
    }

    private static String yes(boolean b) { return b ? "yes" : "NO"; }
}
