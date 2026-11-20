package com.newuniverse.nusmp.weaponpage;

import java.util.List;

/**
 * Why: what the Weapon page shows, as plain immutable records with no game types (so the checksum and the shape rules are unit-tested): the server builds a {@link View} from the game state, {@link WeaponPageNet}
 * carries it, the client page draws it. Texts are either a lang key (lower case, dots, no spaces: the client translates it) or a plain literal (a form name from data, "Lv 3"); a key nobody translated is shown as
 * written. Every list is bounded by {@link WeaponPageRules}.
 */
public final class WeaponPageData {
    private WeaponPageData() {}

    /** One mastery track: a label (key or text), one argument for the label's %s ("" for none), the level or points, the maximum, and the progress to the next step in percent. */
    public record Bar(String label, String arg, int level, int max, int pct) {}

    /** One option of the weapon: what clicking does ({@link WeaponPageRules#ACT_FORM} ...), its argument, its text, its state ({@code ST_*}) and a short note (key or text, may be empty). */
    public record Opt(int action, String arg, String label, int state, String note) {}

    /** One weapon of the player: its id ({@link WeaponPageRules#WEAPONS}), name, grade text, level, maximum, progress, the form in use, its mastery bars and its options. */
    public record Entry(String id, String name, String grade, int level, int max, int pct, String form, List<Bar> bars, List<Opt> opts) {}

    /** Everything the page shows: the index of the weapon in the hand (-1 when none of them is), and the weapons. */
    public record View(int held, List<Entry> entries) {
        public static final View EMPTY = new View(-1, List.of());

        /** A checksum of everything the page shows: equal views give equal sums on both sides, so the server can skip a sync the client already holds. */
        public int sum() {
            int h = WeaponPageRules.mix(WeaponPageRules.SEED, held);
            for (Entry e : entries) {
                h = WeaponPageRules.mix(h, e.id());
                h = WeaponPageRules.mix(h, e.name());
                h = WeaponPageRules.mix(h, e.grade());
                h = WeaponPageRules.mix(h, e.level());
                h = WeaponPageRules.mix(h, e.max());
                h = WeaponPageRules.mix(h, e.pct());
                h = WeaponPageRules.mix(h, e.form());
                for (Bar b : e.bars()) {
                    h = WeaponPageRules.mix(h, b.label());
                    h = WeaponPageRules.mix(h, b.arg());
                    h = WeaponPageRules.mix(h, b.level());
                    h = WeaponPageRules.mix(h, b.max());
                    h = WeaponPageRules.mix(h, b.pct());
                }
                for (Opt o : e.opts()) {
                    h = WeaponPageRules.mix(h, o.action());
                    h = WeaponPageRules.mix(h, o.arg());
                    h = WeaponPageRules.mix(h, o.label());
                    h = WeaponPageRules.mix(h, o.state());
                    h = WeaponPageRules.mix(h, o.note());
                }
                h = WeaponPageRules.mix(h, e.bars().size() * 31 + e.opts().size());
            }
            return WeaponPageRules.mix(h, entries.size());
        }

        /** The entry with this weapon id, or null. */
        public Entry entry(String weapon) {
            for (Entry e : entries) if (e.id().equals(weapon)) return e;
            return null;
        }

        /** The option of a weapon that a click of this action and argument would do, or null: the server only does what the page would have offered. */
        public Opt option(String weapon, int action, String arg) {
            Entry e = entry(weapon);
            if (e == null) return null;
            for (Opt o : e.opts()) if (o.action() == action && o.arg().equals(arg)) return o;
            return null;
        }

        /** True when the view obeys every bound of {@link WeaponPageRules} (the packets refuse a view that does not). */
        public boolean bounded() {
            if (entries.size() > WeaponPageRules.MAX_ENTRIES || held < -1 || held >= entries.size()) return false;
            for (Entry e : entries) {
                if (!WeaponPageRules.isWeapon(e.id()) || e.bars().size() > WeaponPageRules.MAX_BARS || e.opts().size() > WeaponPageRules.MAX_OPTIONS) return false;
                if (e.name().length() > WeaponPageRules.TEXT || e.grade().length() > WeaponPageRules.TEXT || e.form().length() > WeaponPageRules.TEXT) return false;
                for (Bar b : e.bars()) if (b.label().length() > WeaponPageRules.TEXT || b.arg().length() > WeaponPageRules.TEXT) return false;
                for (Opt o : e.opts()) {
                    if (o.arg().length() > WeaponPageRules.ARG || o.label().length() > WeaponPageRules.TEXT || o.note().length() > WeaponPageRules.TEXT) return false;
                    if (o.state() < WeaponPageRules.ST_LOCKED || o.state() > WeaponPageRules.ST_CURRENT || o.action() < 0 || o.action() >= WeaponPageRules.ACTIONS) return false;
                }
            }
            return true;
        }
    }
}
