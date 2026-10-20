package com.newuniverse.nusmp.client.relics.worn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Why: "what should be drawn for this wearer" used to be spread over four places (the layer's loops, the kit layer, the Curios eye flags, the model library's null answer), and when a
 * relic did not show up nobody could say which rule had skipped it. This class is that decision, alone, with no Minecraft type in it: the relics a wearer has (one {@link Entry} each:
 * where it was found, its slot and look, whether the Curios eye is on, whether the library has a model, whether a relic renderer of its own exists) and the circumstances
 * ({@link Ctx}: slim or wide arms, whose view it is, spectator, invisible, too far, the two client options) go in; one {@link Item} per relic comes out, saying HOW it is drawn
 * ({@link Kind}) or, when it is not, WHY ({@link Why}). The two real layers ({@code RelicWornLayer} for the players, {@code KitRelicLayer} for the dungeon's player-model characters)
 * only carry the plan out, and {@code /multiverse relic debug} prints the very same plan, so the answer to "why is it not drawn" is the code that decided it.
 * <p>
 * Order and places: the relics drawn from the model library come out best grade first (ties by id, the same order every frame); a relic drawn as plain boxes or as a floating charm
 * stands after the library models of its slot ({@link Item#slotIndex}), in the order the relics were given. Pure and deterministic.
 */
public final class WornPlan {
    private WornPlan() {}

    /** Whose eyes: another player's body, the wearer's own body seen from outside (third person, inventory preview), or the wearer's own body drawn with the camera in its head. */
    public enum View { OTHER, SELF_THIRD, SELF_FIRST }

    /** Where the relic was found: a hand, the armour slots, a Curios slot, the pack of a named mob, or only in the list the server sent (the client's own look found nothing). */
    public enum Source { HAND, ARMOUR, CURIOS, PACK, SYNCED }

    /** How a relic is drawn. {@link #SKIP} means not drawn (see {@link Why}). */
    public enum Kind { LIBRARY_MODEL, PLAIN_BOX, FLOATING_CHARM, CUSTOM, SKIP }

    /** Why a relic is drawn, or is not. */
    public enum Why {
        DRAWN("drawn"),
        HELD_IN_HAND("held in a hand: the item layers draw it, it is not a worn relic"),
        NOT_A_WORN_SLOT("its slot is not worn on the body (consumable)"),
        WEARER_SPECTATOR("the wearer is a spectator"),
        WEARER_INVISIBLE("the wearer is invisible"),
        TOO_FAR("the wearer is farther than the draw range"),
        EYE_OFF("the Curios eye icon of its slot is switched off"),
        ARMOUR_OPTION("relicfx.hideArmourRelics is on"),
        VANILLA_ARMOUR("an armour relic with no worn model: the armour layer draws its painted armour"),
        FIRST_PERSON_HEAD("the camera is in the wearer's head: head pieces stay away from the view");

        public final String text;

        Why(String text) { this.text = text; }
    }

    /** What to draw for a relic the library has no model for. */
    public enum Fallback { BOXES, CHARM }

    /** Slim arms are one pixel narrower on the outer side: pieces on an arm move half a pixel toward the body. */
    public static final float SLIM_INSET = 0.5f;

    /**
     * One relic the wearer has.
     *
     * @param id       registry path of the relic
     * @param slot     {@code RelicSlot} name: MAIN_HAND, HEAD, CHEST, NECK, BACK, BELT, HANDS, CHARM, CONSUMABLE, FEET
     * @param look     {@code Look} name (for the report only)
     * @param source   where it was found
     * @param eye      the Curios eye icon of its slot: true on, false off, null not known (not a Curios slot, or Curios could not say)
     * @param custom   a relic renderer of its own exists for this id ({@code WornRenderers})
     * @param hasModel the model library can dress it
     * @param tier     its grade tier (the draw order)
     */
    public record Entry(String id, String slot, String look, Source source, Boolean eye, boolean custom, boolean hasModel, int tier) {
        public boolean isHand() { return "MAIN_HAND".equals(slot); }

        public boolean isArmourSlot() { return "HEAD".equals(slot) || "CHEST".equals(slot) || "FEET".equals(slot); }

        public boolean isCurioSlot() { return "NECK".equals(slot) || "BACK".equals(slot) || "BELT".equals(slot) || "HANDS".equals(slot) || "CHARM".equals(slot); }
    }

    /** The circumstances of one frame of one wearer. */
    public record Ctx(boolean slim, View view, boolean spectator, boolean invisible, boolean tooFar, boolean modelsOption, boolean hideArmourOption, Fallback fallback) {}

    /**
     * The answer for one relic.
     *
     * @param slotIndex for a plain box or a floating charm: its place among the slot-mates (the library models of the slot come first); 0 otherwise
     * @param index     the position of the relic in the list that was given to {@link #decide} (the caller's own list stays parallel to the plan)
     */
    public record Item(Entry entry, Kind kind, Why why, int slotIndex, int index) {
        public boolean drawn() { return kind != Kind.SKIP; }

        /** One line for the debug report: {@code "ring_of_x: LIBRARY_MODEL (drawn)"}. */
        public String describe() { return entry.id() + ": " + kind + " (" + why.text + ")"; }
    }

    /** The decision for one wearer: every relic with its answer, and the drawn ones grouped the way the layer draws them. */
    public static final class Plan {
        private final List<Item> all, library, plain, charms, custom;
        private final float armInset;

        Plan(List<Item> all, List<Item> library, List<Item> plain, List<Item> charms, List<Item> custom, float armInset) {
            this.all = Collections.unmodifiableList(all);
            this.library = Collections.unmodifiableList(library);
            this.plain = Collections.unmodifiableList(plain);
            this.charms = Collections.unmodifiableList(charms);
            this.custom = Collections.unmodifiableList(custom);
            this.armInset = armInset;
        }

        /** Every relic given, in the order given. */
        public List<Item> all() { return all; }

        /** The relics drawn from the model library, best grade first (ties by id). */
        public List<Item> library() { return library; }

        /** The relics drawn as plain boxes, in the order given. */
        public List<Item> plain() { return plain; }

        /** The relics drawn as a floating charm, in the order given. */
        public List<Item> charms() { return charms; }

        /** The relics with a renderer of their own, drawn last. */
        public List<Item> custom() { return custom; }

        /** Half a pixel for slim arms, 0 for wide ones: the sideways move toward the body of every piece anchored on an arm. */
        public float armInset() { return armInset; }

        /** How many relics will be drawn at all. */
        public int drawnCount() {
            int n = 0;
            for (Item i : all) if (i.drawn()) n++;
            return n;
        }

        public boolean isEmpty() { return drawnCount() == 0; }
    }

    /** Best grade first, ties by id: the order the library models are drawn in. */
    static int libraryOrder(Entry a, Entry b) {
        if (a.tier() != b.tier()) return Integer.compare(b.tier(), a.tier());
        return a.id().compareTo(b.id());
    }

    /** Decides what is drawn for one wearer. Never null; the plan has one item per entry, in the order given. */
    public static Plan decide(Ctx ctx, List<Entry> worn) {
        List<Item> all = new ArrayList<>(worn.size());
        Why whole = ctx.spectator() ? Why.WEARER_SPECTATOR : ctx.invisible() ? Why.WEARER_INVISIBLE : ctx.tooFar() ? Why.TOO_FAR : null;
        // pass 1: the verdict of each relic, without places
        Kind[] kinds = new Kind[worn.size()];
        Why[] whys = new Why[worn.size()];
        for (int i = 0; i < worn.size(); i++) {
            Entry e = worn.get(i);
            Kind k;
            Why y = Why.DRAWN;
            if (whole != null) { k = Kind.SKIP; y = whole; }
            else if (e.isHand()) { k = Kind.SKIP; y = Why.HELD_IN_HAND; }
            else if (!e.isArmourSlot() && !e.isCurioSlot()) { k = Kind.SKIP; y = Why.NOT_A_WORN_SLOT; }
            else if (e.isCurioSlot() && e.source() == Source.CURIOS && Boolean.FALSE.equals(e.eye())) { k = Kind.SKIP; y = Why.EYE_OFF; }
            else if (e.isArmourSlot() && ctx.hideArmourOption()) { k = Kind.SKIP; y = Why.ARMOUR_OPTION; }
            else if (e.custom()) k = Kind.CUSTOM;
            else if (ctx.view() == View.SELF_FIRST && "HEAD".equals(e.slot())) { k = Kind.SKIP; y = Why.FIRST_PERSON_HEAD; }
            else if ((ctx.modelsOption() || e.isArmourSlot()) && e.hasModel()) k = Kind.LIBRARY_MODEL;
            else if (e.isArmourSlot() && e.source() == Source.ARMOUR) { k = Kind.SKIP; y = Why.VANILLA_ARMOUR; }
            else if (ctx.fallback() == Fallback.BOXES && e.isCurioSlot() && e.source() != Source.PACK) k = Kind.PLAIN_BOX;
            else k = Kind.FLOATING_CHARM;
            kinds[i] = k;
            whys[i] = y;
        }
        // pass 2: the places among slot-mates (library models first, then boxes and charms in the order given)
        java.util.Map<String, Integer> taken = new java.util.HashMap<>();
        List<Integer> libraryAt = new ArrayList<>();
        for (int i = 0; i < worn.size(); i++) if (kinds[i] == Kind.LIBRARY_MODEL) libraryAt.add(i);
        for (int i : libraryAt) taken.merge(worn.get(i).slot(), 1, Integer::sum);
        int[] index = new int[worn.size()];
        for (int i = 0; i < worn.size(); i++) {
            if (kinds[i] != Kind.PLAIN_BOX && kinds[i] != Kind.FLOATING_CHARM) continue;
            String slot = worn.get(i).slot();
            index[i] = taken.merge(slot, 1, Integer::sum) - 1;
        }
        List<Item> library = new ArrayList<>(), plain = new ArrayList<>(), charms = new ArrayList<>(), custom = new ArrayList<>();
        for (int i = 0; i < worn.size(); i++) {
            Item it = new Item(worn.get(i), kinds[i], whys[i], index[i], i);
            all.add(it);
            switch (kinds[i]) {
                case LIBRARY_MODEL -> library.add(it);
                case PLAIN_BOX -> plain.add(it);
                case FLOATING_CHARM -> charms.add(it);
                case CUSTOM -> custom.add(it);
                default -> { }
            }
        }
        library.sort((a, b) -> libraryOrder(a.entry(), b.entry()));
        return new Plan(all, library, plain, charms, custom, ctx.slim() ? SLIM_INSET : 0f);
    }

    /** The view of a wearer, from the facts the layer knows: is it the local player, is the camera in first person, is a screen open (an inventory preview is not a first-person body). */
    public static View viewOf(boolean isLocalPlayer, boolean cameraFirstPerson, boolean screenOpen) {
        if (!isLocalPlayer) return View.OTHER;
        return cameraFirstPerson && !screenOpen ? View.SELF_FIRST : View.SELF_THIRD;
    }

    /** True for a slot name the layers can wear on the body (armour or Curios). Case-insensitive. */
    public static boolean isWornSlot(String slot) {
        if (slot == null) return false;
        return switch (slot.toUpperCase(Locale.ROOT)) {
            case "HEAD", "CHEST", "FEET", "NECK", "BACK", "BELT", "HANDS", "CHARM" -> true;
            default -> false;
        };
    }
}
