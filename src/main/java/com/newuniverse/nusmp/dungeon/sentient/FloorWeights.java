package com.newuniverse.nusmp.dungeon.sentient;

import java.util.ArrayList;
import java.util.List;

/**
 * The spawn lists of the three floors that birth mobs, as plain entries with weights: the fallback when the entity-type tags
 * (data/nusmp/tags/entity_type/dungeon/born_*.json) cannot be read, and the weight of every id a modpack puts in them. A kobold is stood in
 * for by the hound dog of Tensura, which has none; the aqua slime is the same slime made at home in water; the Dark Disciple is the Zogratis
 * family's follower. Pure.
 */
public final class FloorWeights {
    private FloorWeights() {}

    /** One kind of mob and how common it is among its floor's. */
    public record Entry(String id, int weight) {}

    public static final String SLIME = "tensura:slime", DISCIPLE = "nusmp:dark_disciple";
    /** The weight of an id the table does not know (a modpack's own entry). */
    public static final int UNLISTED = 2;

    /** The built-in list of a floor (empty for a floor that births no mobs). */
    public static List<Entry> defaults(FloorKind kind) {
        List<Entry> out = new ArrayList<>();
        switch (kind) {
            case CATACOMBS -> {
                out.add(new Entry("tensura:goblin", 4));
                out.add(new Entry(SLIME, 3));
                out.add(new Entry("tensura:hound_dog", 2));
            }
            case WATERWAYS -> {
                out.add(new Entry("tensura:lizardman", 4));
                out.add(new Entry(SLIME, 3));
                out.add(new Entry("tensura:undine", 2));
            }
            case UNDERWORLD -> {
                out.add(new Entry("tensura:lesser_daemon", 3));
                out.add(new Entry(DISCIPLE, 2));
            }
            default -> { }
        }
        return out;
    }

    /** The weight of an id on a floor: the table's, or {@link #UNLISTED}. */
    public static int weight(FloorKind kind, String id) {
        for (Entry e : defaults(kind)) if (e.id().equals(id)) return e.weight();
        return UNLISTED;
    }

    /** Is the slime of this floor an aqua slime? On the Deep Waterways every slime is. */
    public static boolean aqua(FloorKind kind, String id) { return kind == FloorKind.WATERWAYS && SLIME.equals(id); }

    /** One id chosen by weight ({@code ids} and {@code weights} run in step); null if the lists are empty or no weight is above zero. */
    public static String pick(List<String> ids, List<Integer> weights, Dice dice) {
        int total = 0;
        for (int w : weights) total += Math.max(0, w);
        if (ids.isEmpty() || total <= 0) return null;
        int at = dice.nextInt(total);
        for (int i = 0; i < ids.size(); i++) {
            at -= Math.max(0, weights.get(i));
            if (at < 0) return ids.get(i);
        }
        return ids.get(ids.size() - 1);
    }
}
