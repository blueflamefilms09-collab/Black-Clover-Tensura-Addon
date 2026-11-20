package com.newuniverse.nusmp.weaponpage;

import com.newuniverse.nusmp.greed.GreedConfig;
import com.newuniverse.nusmp.greed.GreedData;
import com.newuniverse.nusmp.greed.GreedItem;
import com.newuniverse.nusmp.greed.GreedRules;
import com.newuniverse.nusmp.greed.GreedService;
import com.newuniverse.nusmp.greed.GreedState;
import com.newuniverse.nusmp.greed.GreedStore;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.forms.HeroPageView;
import com.newuniverse.nusmp.lords.LordConfig;
import com.newuniverse.nusmp.lords.LordData;
import com.newuniverse.nusmp.lords.LordItems;
import com.newuniverse.nusmp.lords.LordRules;
import com.newuniverse.nusmp.lords.LordService;
import com.newuniverse.nusmp.lords.LordState;
import com.newuniverse.nusmp.lords.LordStore;
import com.newuniverse.nusmp.relics.Rankings;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Bar;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Entry;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Opt;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.View;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the server half of the Weapon page of the Codex. It builds what the page shows from the systems that already own the data (the hero bond and its forms, the Lord weapons' mastery, the Black Sword's
 * tier and Half-Starved mastery) and does what the page asks by calling the same server functions those systems already use (change form, learn a form, feed the gem, switch a Lord form, switch a Greed form,
 * toggle Half-Starvation). It adds no rule and no number: every refusal of those functions (ownership, the curse lock, the two second form gap, requirements) still applies. What it adds is the front door:
 * {@link #onAct} checks the sender (a real player), the weapon / action / argument shape ({@link WeaponPageRules#check}), the player's own gap between requests, that the page really offers this action to
 * this player right now ({@link View#option}, so the server never does something the page would not have shown), and only then calls the existing function. The answer is one {@link WeaponPageNet.Sync}, sent
 * only when it differs from what the player was last sent. Cost: per request, one page build (about 60 small records, one pass over 41 slots at most); no tick handler, no scheduled work; two small maps (a long
 * and an int per player) dropped at logout and at server stop.
 */
public final class WeaponPageService {
    private WeaponPageService() {}

    /** What {@link #handle} did, for the self-test and the log. */
    public enum Result { APPLIED, SENT, UNCHANGED, BAD_REQUEST, TOO_SOON, NOT_OFFERED }

    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static final Map<UUID, Integer> SHOWN = new HashMap<>();
    private static final double[] TIERS = new double[GreedRules.MAX_TIER + 1];

    static {
        for (int i = 0; i < TIERS.length; i++) TIERS[i] = GreedRules.tierThreshold(i);
    }

    /** The packet handler (server thread): a real player only. */
    public static void onAct(ServerPlayer p, WeaponPageNet.Act a) {
        if (p == null || p.isRemoved() || a == null) return;
        handle(p, a.weapon(), a.action(), a.arg());
    }

    /** The whole request, with the answer reported (the self-test calls this with a stand-in player). */
    public static Result handle(ServerPlayer p, String weapon, int action, String arg) {
        if (WeaponPageRules.check(weapon, action, arg) != WeaponPageRules.Verdict.OK) return Result.BAD_REQUEST;
        long now = p.serverLevel().getGameTime();
        Long last = LAST.get(p.getUUID());
        if (!WeaponPageRules.ready(now, last == null ? -1L : last, WeaponPageRules.gap(action))) return Result.TOO_SOON;
        LAST.put(p.getUUID(), now);
        View view = build(p);
        if (action == WeaponPageRules.ACT_OPEN) {
            boolean has = !arg.isEmpty() && Integer.toString(view.sum()).equals(arg);
            SHOWN.put(p.getUUID(), view.sum());
            if (has) return Result.UNCHANGED;
            WeaponPageNet.send(p, view);
            return Result.SENT;
        }
        WeaponPageData.Opt o = view.option(weapon, action, arg);
        boolean half = action == WeaponPageRules.ACT_HALF && view.entry("greed") != null;
        if (!half && (o == null || o.state() != WeaponPageRules.ST_READY)) {
            refuse(p);
            return Result.NOT_OFFERED;
        }
        apply(p, weapon, action, arg);
        View after = build(p);
        Integer shown = SHOWN.get(p.getUUID());
        if (shown != null && shown == after.sum()) return Result.APPLIED;
        SHOWN.put(p.getUUID(), after.sum());
        WeaponPageNet.send(p, after);
        return Result.APPLIED;
    }

    private static void refuse(ServerPlayer p) { p.displayClientMessage(Component.translatable("anime_multiverse.weaponpage.msg.refused"), true); }

    /** Does the action through the existing path of the weapon's own system; each of them checks ownership and keeps its own rules and gaps. */
    private static void apply(ServerPlayer p, String weapon, int action, String arg) {
        switch (action) {
            case WeaponPageRules.ACT_FORM -> {
                if (weapon.equals("hero")) HeroPageView.useForm(p, arg);
                else if (weapon.equals("greed")) GreedService.switchForm(p, Integer.parseInt(arg));
                else LordService.switchForm(p, LordRules.Sin.byOrdinal(WeaponPageRules.WEAPONS.indexOf(weapon) - 1), Integer.parseInt(arg));
            }
            case WeaponPageRules.ACT_LEARN -> HeroPageView.learn(p, arg);
            case WeaponPageRules.ACT_FEED -> HeroPageView.feed(p, arg);
            case WeaponPageRules.ACT_HALF -> GreedService.toggleHalf(p);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- the page
    /** Everything the page shows for this player: the hero weapon of the bond, each Lord weapon they own, the Black Sword if they own it. Empty when they have none. */
    public static View build(ServerPlayer p) {
        List<Entry> out = new ArrayList<>(WeaponPageRules.MAX_ENTRIES);
        Entry hero = HeroPageView.entry(p);
        if (hero != null) out.add(hero);
        for (LordRules.Sin sin : LordRules.Sin.values()) {
            if (!sin.isLord()) continue;
            Entry e = lord(p, sin);
            if (e != null) out.add(e);
        }
        Entry greed = greed(p);
        if (greed != null) out.add(greed);
        return new View(heldIndex(p, out), List.copyOf(out));
    }

    private static int heldIndex(ServerPlayer p, List<Entry> entries) {
        ItemStack m = p.getMainHandItem();
        String id = null;
        if (m.isEmpty()) return -1;
        if (HeroItems.kindOf(m) != null) id = "hero";
        else if (LordItems.sinOf(m) != null) id = WeaponPageRules.WEAPONS.get(1 + LordItems.sinOf(m).ordinal());
        else if (m.getItem() instanceof GreedItem) id = "greed";
        if (id == null) return -1;
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).id().equals(id)) return i;
        return -1;
    }

    private static String grade(String itemPath) {
        Rankings.Rank r = Rankings.table().of(itemPath);
        return r == null ? "" : r.label();
    }

    private static Entry lord(ServerPlayer p, LordRules.Sin sin) {
        if (!LordConfig.enabled() || !LordData.bound()) return null;
        LordData d = LordData.get();
        if (!d.isOwner(sin, p.getUUID())) return null;
        LordState st = LordStore.of(p);
        int level = st.level(sin), pct = LordRules.progress(st.xp(sin)), cur = Math.max(0, Math.min(sin.forms() - 1, d.form(sin)));
        List<Opt> opts = new ArrayList<>(sin.forms());
        for (int f = 0; f < sin.forms(); f++) {
            boolean open = LordRules.formOpen(f, level);
            int state = f == cur ? WeaponPageRules.ST_CURRENT : open ? WeaponPageRules.ST_READY : WeaponPageRules.ST_LOCKED;
            opts.add(new Opt(WeaponPageRules.ACT_FORM, Integer.toString(f), sin.formKey(f), state, open ? "" : "Lv " + LordRules.formLevel(f)));
        }
        List<Bar> bars = List.of(new Bar(sin.masteryKey(), "", level, LordRules.MAX_LEVEL, pct));
        return new Entry(WeaponPageRules.WEAPONS.get(1 + sin.ordinal()), "lords.name." + sin.id, grade(sin.baseItem()), level, LordRules.MAX_LEVEL, pct, sin.formKey(cur), bars, List.copyOf(opts));
    }

    private static Entry greed(ServerPlayer p) {
        if (!GreedConfig.enabled() || !GreedData.bound()) return null;
        GreedData d = GreedData.get();
        if (!d.isOwner(p.getUUID())) return null;
        GreedState gs = GreedStore.of(p);
        LordState ls = LordStore.of(p);
        int tier = Math.max(0, Math.min(GreedRules.MAX_TIER, d.tier())), cur = Math.max(0, Math.min(GreedRules.FORMS - 1, d.form()));
        int tierPct = WeaponPageRules.tierPct(gs.storedEp, TIERS);
        int mastery = ls.level(LordRules.Sin.GREED), masteryPct = LordRules.progress(ls.xp(LordRules.Sin.GREED));
        List<Opt> opts = new ArrayList<>(GreedRules.FORMS + 1);
        for (int f = 0; f < GreedRules.FORMS; f++) {
            boolean open = GreedRules.formOpen(f, tier);
            int state = f == cur ? WeaponPageRules.ST_CURRENT : open ? WeaponPageRules.ST_READY : WeaponPageRules.ST_LOCKED;
            opts.add(new Opt(WeaponPageRules.ACT_FORM, Integer.toString(f), GreedRules.Form.byId(f).langKey(), state, open ? "" : "Tier " + f));
        }
        opts.add(new Opt(WeaponPageRules.ACT_HALF, "", "anime_multiverse.weaponpage.opt.half", gs.half ? WeaponPageRules.ST_CURRENT : WeaponPageRules.ST_READY,
                gs.half ? "anime_multiverse.weaponpage.note.on" : "anime_multiverse.weaponpage.note.off"));
        List<Bar> bars = List.of(new Bar(LordRules.Sin.GREED.masteryKey(), "", mastery, LordRules.MAX_LEVEL, masteryPct),
                new Bar("anime_multiverse.weaponpage.bar.tier", "", tier, GreedRules.MAX_TIER, tierPct));
        return new Entry("greed", "lords.name.greed", grade(GreedRules.ID_PREFIX), tier, GreedRules.MAX_TIER, tierPct, GreedRules.Form.byId(cur).langKey(), bars, List.copyOf(opts));
    }

    // ---------------------------------------------------------------- cleanup
    public static void forget(UUID id) {
        LAST.remove(id);
        SHOWN.remove(id);
    }

    public static void clearAll() {
        LAST.clear();
        SHOWN.clear();
    }

    public static int trackedPlayers() { return LAST.size() + SHOWN.size(); }
}
