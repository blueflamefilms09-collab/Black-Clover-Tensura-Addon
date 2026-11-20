package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import com.newuniverse.nusmp.relics.Rankings;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Bar;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Entry;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Opt;
import com.newuniverse.nusmp.weaponpage.WeaponPageRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the Weapon page of the Codex shows the hero (Holy / Seven Star / Vassal) weapon of the bond the way the Weapon Book does, and lets the player use the same three upgrade actions of the existing forms system
 * (change form, learn a form, feed the gem), but it must read the curse series and the form tree, which are package-private to the forms system; so this small class lives here. It only READS the bond and calls
 * the existing server paths ({@link HeroForms#trySetActive}, {@link FormLearning#tryLearn}, {@link HeroAbsorb#feed}), which keep every rule they had (curse lock, technique lock, requirements, the absorb rate
 * limit): nothing is added to what a player can do. Cost: one pass over the weapon's forms (a few dozen) and one pass over the 41 inventory slots, only when a page is built (an open or a click).
 */
public final class HeroPageView {
    private HeroPageView() {}

    /** The feed options the page offers at most (one per missing item the player carries). */
    private static final int MAX_FEED = 6;

    /** The page entry of the player's bond, or null when they have no hero weapon. */
    public static Entry entry(ServerPlayer p) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) return null;
        HeroKind kind = b.kind();
        List<HeroForm> forms = HeroForms.of(kind);
        HeroForm active = HeroForms.active(b);
        Set<String> learned = new HashSet<>(b.unlocked());
        learned.add(HeroForm.BASE_ID);
        Set<String> series = CurseLink.series(b);
        Map<String, Integer> absorbed = b.absorbedMap();

        List<Bar> bars = new ArrayList<>();
        bars.add(new Bar("anime_multiverse.weaponpage.bar.level", "", b.level(), HeroLevels.MAX, (int) Math.round(HeroLevels.progress(b.exp()) * 100.0)));
        if (active != null && active.mastery() > 0) {
            int have = Math.min(b.mastery(active.id()), active.mastery());
            bars.add(new Bar("anime_multiverse.weaponpage.bar.form", cut(active.name()), have, active.mastery(), have * 100 / active.mastery()));
        }
        for (HeroForm f : forms) {                                              // the other learned forms that have started to fill (own key: a client with moreMastery off hides these); bounded by MAX_BARS
            if (bars.size() >= WeaponPageRules.MAX_BARS) break;
            if (f == active || f.mastery() <= 0 || !learned.contains(f.id()) || b.mastery(f.id()) <= 0) continue;
            int have = Math.min(b.mastery(f.id()), f.mastery());
            bars.add(new Bar("anime_multiverse.weaponpage.bar.formmore", cut(f.name()), have, f.mastery(), have * 100 / f.mastery()));
        }

        List<Opt> opts = new ArrayList<>();
        Set<String> wanted = new HashSet<>();                                   // item requirements of the forms that could be learned next that the gem still lacks
        for (HeroForm f : forms) {
            if (opts.size() >= WeaponPageRules.MAX_OPTIONS - MAX_FEED) break;
            FormTree.Status st = FormTree.status(f, b.form(), learned, absorbed, FormTags.MATCHER, series);
            String name = cut(f.name());
            switch (st) {
                case ACTIVE -> opts.add(new Opt(WeaponPageRules.ACT_FORM, f.id(), name, WeaponPageRules.ST_CURRENT, ""));
                case LEARNED -> opts.add(new Opt(WeaponPageRules.ACT_FORM, f.id(), name, WeaponPageRules.ST_READY, ""));
                case AVAILABLE -> opts.add(new Opt(WeaponPageRules.ACT_LEARN, f.id(), name, WeaponPageRules.ST_READY, "anime_multiverse.weaponpage.note.learn"));
                case SEALED -> opts.add(new Opt(WeaponPageRules.ACT_FORM, f.id(), name, WeaponPageRules.ST_LOCKED, "anime_multiverse.weaponpage.note.sealed"));
                default -> {
                    opts.add(new Opt(WeaponPageRules.ACT_FORM, f.id(), name, WeaponPageRules.ST_LOCKED, "anime_multiverse.weaponpage.note.locked"));
                    if (FormTree.parentsLearned(f, learned) && !FormTree.sealed(f, series)) {
                        for (HeroForm.Requirement r : f.requires()) {
                            if (!r.isTag() && !FormTree.met(r, absorbed, FormTags.MATCHER) && WeaponPageRules.isItemId(r.item())) wanted.add(r.item());
                        }
                    }
                }
            }
        }
        if (!wanted.isEmpty()) feedOptions(p, wanted, opts);

        Rankings.Rank rank = Rankings.table().of(kind.itemId());
        String grade = rank == null ? "" : rank.label();
        int pct = (int) Math.round(HeroLevels.progress(b.exp()) * 100.0);
        return new Entry("hero", cut(kind.display()), grade, b.level(), HeroLevels.MAX, pct, active == null ? "" : cut(active.name()), List.copyOf(bars), List.copyOf(opts));
    }

    /** One feed option for each wanted item the player carries (main inventory and off hand, like {@link HeroAbsorb#feed}), at most {@link #MAX_FEED}. */
    private static void feedOptions(ServerPlayer p, Set<String> wanted, List<Opt> out) {
        Map<String, Integer> have = new HashMap<>();
        Inventory inv = p.getInventory();
        for (ItemStack s : inv.items) count(s, wanted, have);
        for (ItemStack s : inv.offhand) count(s, wanted, have);
        List<String> ids = new ArrayList<>(have.keySet());
        java.util.Collections.sort(ids);                                       // a stable order, so the page checksum does not flicker
        int room = Math.min(MAX_FEED, WeaponPageRules.MAX_OPTIONS - out.size());
        for (int i = 0; i < ids.size() && i < room; i++) out.add(new Opt(WeaponPageRules.ACT_FEED, ids.get(i), ids.get(i), WeaponPageRules.ST_READY, "x" + have.get(ids.get(i))));
    }

    private static void count(ItemStack s, Set<String> wanted, Map<String, Integer> have) {
        if (s.isEmpty()) return;
        String id = HeroAbsorb.idOf(s);
        if (wanted.contains(id)) have.merge(id, s.getCount(), Integer::sum);
    }

    private static String cut(String s) { return s.length() <= WeaponPageRules.TEXT ? s : s.substring(0, WeaponPageRules.TEXT); }

    // ---------------------------------------------------------------- the three actions (each is the existing path of the forms system)
    /** Change to a learned form; the reason of a refusal is shown on the action bar by the existing path. */
    public static void useForm(ServerPlayer p, String formId) { HeroForms.setActive(p, formId); }

    /** Learn one form that is available now; a refusal is shown on the action bar. */
    public static void learn(ServerPlayer p, String formId) {
        Component why = FormLearning.tryLearn(p, formId);
        if (why != null) FormsFeedback.bar(p, why);
    }

    /** Feed the gem with one item from the inventory (one stack at a time, only what the forms still need unless the setting says otherwise). Returns how many were taken. */
    public static int feed(ServerPlayer p, String itemId) { return HeroAbsorb.feed(p, itemId, false); }
}
