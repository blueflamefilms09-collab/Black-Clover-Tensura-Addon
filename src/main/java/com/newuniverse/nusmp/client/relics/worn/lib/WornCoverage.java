package com.newuniverse.nusmp.client.relics.worn.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * The data-driven check behind "no worn relic is invisible": every relic that is worn on the body (a Curios slot or a piece of armour) must resolve, through the
 * catalog, to models whose files exist (its own entry, the entry of its slot and look, or the default look of its slot), and the layer has a last resort for a relic
 * that still resolves to nothing (the plain boxes of a Curios slot, the floating charm of {@link WornLibrary#fallbackCharm}). Pure: no Minecraft types, so a unit test and the
 * server self-test {@code relic.worn_render} both run it.
 */
public final class WornCoverage {
    private WornCoverage() {}

    /** One relic as the check sees it: registry path, slot name ("NECK" ...), look name ("AMULET" ...). */
    public record Entry(String id, String slot, String look) {}

    /** Slots a relic is worn in on the body; the others (hand, consumable) are held or used up and need no worn model. */
    public static boolean isWornSlot(String slot) {
        if (slot == null) return false;
        return switch (slot.toUpperCase(Locale.ROOT)) {
            case "HEAD", "CHEST", "NECK", "BACK", "BELT", "HANDS", "CHARM", "FEET" -> true;
            default -> false;
        };
    }

    /** Result of a check: the problems (empty = fine) and how many relics resolved by each route. */
    public record Report(List<String> problems, int worn, int signature, int look, int defaultLook) {
        public boolean ok() { return problems.isEmpty(); }

        public String summary() {
            return worn + " worn relics: " + signature + " own models, " + look + " by slot and look, " + defaultLook + " by the default look of the slot, " + problems.size() + " problems";
        }
    }

    /** Checks every worn relic against the catalog; {@code modelExists} answers whether the model file of a name is there. */
    public static Report check(List<Entry> relics, WornCatalog cat, Predicate<String> modelExists) {
        List<String> problems = new ArrayList<>();
        int worn = 0, sig = 0, look = 0, def = 0;
        for (Entry e : relics) {
            if (!isWornSlot(e.slot())) continue;
            worn++;
            WornCatalog.Source src = cat.sourceOf(e.id(), e.slot(), e.look());
            if (src == WornCatalog.Source.NONE) {
                problems.add(e.id() + ": no worn model for " + e.slot() + "/" + e.look() + " and the slot has no default look");
                continue;
            }
            if (src == WornCatalog.Source.SIGNATURE) sig++;
            else if (src == WornCatalog.Source.LOOK) look++;
            else def++;
            boolean any = false;
            for (WornCatalog.Piece p : cat.piecesFor(e.id(), e.slot(), e.look())) {
                if (WornCatalog.NONE.equals(p.model())) continue;
                if (modelExists.test(p.model())) any = true;
                else problems.add(e.id() + ": model file " + p.model() + " is missing");
            }
            if (!any) problems.add(e.id() + ": none of its worn models can be loaded");
        }
        return new Report(List.copyOf(problems), worn, sig, look, def);
    }
}
