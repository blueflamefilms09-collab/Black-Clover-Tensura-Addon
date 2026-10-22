package com.newuniverse.nusmp.spiritcontrol;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the GRIMOIRE route of the spirit control plane. Six pages are appended at the very end of every grimoire (after everything else, so no page id, mode number or save moves): "Summon / Release
 * Spirit" and "Spirit Combat Mode" (toggles that act on the key press, like Summon Grimoire), and up to four ability pages (named after the abilities of the spirit that answers: the Space spirit's portal,
 * gravity pulse, spatial cut and waypoint; the Light spirit's beam, pulse and seraph; the Darkness spirit's Shadow Step and demon; a powered form for the others). They go through the ordinary page flow: hold and
 * release to cast, the page's mana price and cooldown, the grimoire must be summoned. A page is shown only while a spirit of the book's magic answers the player ({@link PageGate}); otherwise it is a
 * hidden "?" page that the page switch skips, like the late-game pages.
 *
 * <p>The page names come from the book's skill instance: once a second (and after every action) the server writes a small {@code SpiritUi} tag (the spirit, whether it is out, the combat flag, the ability
 * labels) into the instance, and only when it changed, so the clients can name the pages without asking anyone. The price of an ability page is the spirit's own (a spirit that charges its own mana, the Space
 * spirit, asks nothing extra); a refused ability gives the price back.</p>
 */
public final class SpiritControlPages {
    private SpiritControlPages() {}

    private static final String UI = "SpiritUi";
    private static final String SLOT_KEY = "nusmp_spirit_slot";

    /** The six pages, in book order (appended last by the grimoire). */
    public static List<BookPage> pages() {
        List<BookPage> l = new ArrayList<>();
        l.add(new BookPage(PageGate.CALL, "Summon Spirit", "Spirit Call", 0, 0, 0, (b, i, p, m) -> true));
        l.add(new BookPage(PageGate.COMBAT, "Spirit Combat Mode", "Spirit Combat", 0, 0, 0, (b, i, p, m) -> true));
        for (int k = 0; k < PageGate.MAX_ABILITIES; k++) {
            final int idx = k;
            l.add(new BookPage(PageGate.abilityId(k), "Spirit Ability " + (k + 1), "Spirit Art", 0, 0, 0, (b, i, p, m) -> ability(b, i, p, m, idx)));
        }
        return l;
    }

    public static boolean isPage(String id) { return PageGate.isPage(id); }

    public static boolean isInstant(String id) { return PageGate.isInstant(id); }

    public static boolean isAbilityPage(String id) { return PageGate.abilityIndex(id) >= 0; }

    // ================================================================================================ who answers this book
    private static List<SpiritHandle> answering(GrimoireBook book, ServerPlayer p) { return SpiritControl.eligibleFor(p, book.magicNow().name()); }

    /** The spirit whose abilities the pages show: the first that has abilities, else the first. */
    static SpiritHandle lead(List<SpiritHandle> l) {
        for (SpiritHandle h : l) if (!h.abilities().isEmpty()) return h;
        return l.isEmpty() ? null : l.get(0);
    }

    /** The spirit the call page toggles: the first that is out (so it can be released), else the lead. */
    static SpiritHandle callTarget(ServerPlayer p, List<SpiritHandle> l) {
        for (SpiritHandle h : l) if (h.isOut(p)) return h;
        return lead(l);
    }

    // ================================================================================================ usable / cost / label (called by the grimoire)
    /** Can this page be selected and cast now: live for a server player, from the instance's tag otherwise (the client names the pages from it). */
    public static boolean usable(GrimoireBook book, ManasSkillInstance inst, LivingEntity e, String id) {
        if (!SpiritControlConfig.enabled()) return false;
        if (e instanceof ServerPlayer p) {
            List<SpiritHandle> l = answering(book, p);
            SpiritHandle h = lead(l);
            return PageGate.visible(id, l.size(), h == null ? 0 : h.abilities().size());
        }
        if (inst == null) return false;
        CompoundTag ui = inst.getOrCreateTag().getCompound(UI);
        if (ui.isEmpty()) return false;
        int a = PageGate.abilityIndex(id);
        return a < 0 || !ui.getString("a" + (a + 1)).isEmpty();
    }

    /** {percent, floor} of the magicule price of an ability page for this caster: the lead spirit's price for that ability, else free. */
    public static double[] cost(GrimoireBook book, LivingEntity e, String id) {
        int a = PageGate.abilityIndex(id);
        if (a < 0 || !(e instanceof ServerPlayer p)) return new double[]{0, 0};
        SpiritHandle h = lead(answering(book, p));
        if (h == null || a >= h.abilities().size()) return new double[]{0, 0};
        double pct = h.abilities().get(a).pageCostPercent();
        return new double[]{pct, pct > 0 ? 100 : 0};
    }

    /** The name of a spirit page from the instance's tag. */
    public static String label(ManasSkillInstance inst, BookPage page) {
        CompoundTag ui = inst == null ? new CompoundTag() : inst.getOrCreateTag().getCompound(UI);
        String id = page.id();
        if (PageGate.CALL.equals(id)) return ui.isEmpty() ? page.name() : PageGate.callLabel(ui.getString("name"), ui.getBoolean("out"));
        if (PageGate.COMBAT.equals(id)) return ui.isEmpty() ? page.name() : PageGate.combatLabel(ui.getBoolean("combat"));
        int a = PageGate.abilityIndex(id);
        String s = a < 0 ? "" : ui.getString("a" + (a + 1));
        return s.isEmpty() ? page.name() : s;
    }

    // ================================================================================================ the tag
    /** Writes the SpiritUi tag of one book's instance for this player; only a change marks the instance dirty (one sync packet). */
    public static void sync(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p) {
        if (book.magicNow() == null || SpiritControl.REG.forMagic(book.magicNow().name()).isEmpty()) return;     // a book whose magic has no spirit: nothing to name, nothing to check
        CompoundTag tag = inst.getOrCreateTag();
        List<SpiritHandle> l = answering(book, p);
        SpiritHandle h = lead(l);
        if (h == null) {
            if (tag.contains(UI)) { tag.remove(UI); inst.markDirty(); }
            return;
        }
        CompoundTag ui = new CompoundTag();
        ui.putString("id", h.id());
        ui.putString("name", h.display());
        ui.putBoolean("out", callTarget(p, l).isOut(p));
        ui.putBoolean("combat", SpiritCombat.isOn(p.getUUID()));
        for (int i = 0; i < h.abilities().size(); i++) ui.putString("a" + (i + 1), h.abilities().get(i).label());
        if (!ui.equals(tag.getCompound(UI))) { tag.put(UI, ui); inst.markDirty(); }
    }

    /** After an action: rewrites the tag of every grimoire the player has, so the page names are right at once. */
    public static void refresh(ServerPlayer p) {
        var skills = SkillAPI.getSkillsFrom(p);
        for (var h : com.newuniverse.nusmp.skill.NUSkills.BOOKS)
            skills.getSkill(h.getId()).ifPresent(i -> { if (i.getSkill() instanceof GrimoireBook b) sync(b, i, p); });
    }

    // ================================================================================================ the actions
    /** The waypoint slot (0-based) the portal and waypoint pages use for this player; the command with a slot sets it. */
    public static void rememberSlot(ServerPlayer p, int slot) { p.getPersistentData().putInt(SLOT_KEY, Math.max(0, Math.min(SpiritControl.MAX_SLOT - 1, slot))); }

    /** The key press of one of the two toggle pages. */
    public static void press(ServerPlayer p, GrimoireBook book, ManasSkillInstance inst, String id) {
        List<SpiritHandle> l = answering(book, p);
        if (l.isEmpty()) { GrimoireBook.fail(p, "No spirit answers you."); return; }
        if (PageGate.CALL.equals(id)) SpiritControl.toggle(p, callTarget(p, l));
        else SpiritControl.toggleCombat(p);
        sync(book, inst, p);
    }

    /** The cast of an ability page; true when the spirit really did it. */
    private static boolean ability(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int mode, int index) {
        List<SpiritHandle> l = answering(book, p);
        SpiritHandle h = lead(l);
        if (h == null || index >= h.abilities().size()) { GrimoireBook.fail(p, "No spirit answers you."); return false; }
        SpiritAbilityDef d = h.abilities().get(index);
        int slot = 0;
        if (d.takesSlot()) {
            int count = Math.max(1, h.slots());
            slot = Math.floorMod(p.getPersistentData().getInt(SLOT_KEY), count);
            if (p.isShiftKeyDown()) {                                                  // sneak: pick the next waypoint slot instead of casting (the old cycle key)
                slot = (slot + 1) % count;
                p.getPersistentData().putInt(SLOT_KEY, slot);
                p.displayClientMessage(Component.translatableWithFallback("nusmp.spirit_control.slot", "Waypoint slot %s selected.", slot + 1).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                refund(book, inst, p, mode);
                return false;
            }
        }
        SpiritControl.Verdict v = SpiritControl.ability(p, h, d.id(), slot);
        if (v != SpiritControl.Verdict.DONE) { refund(book, inst, p, mode); return false; }
        if (d.pageCooldownTicks() > 0) inst.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(d.pageCooldownTicks()), mode);
        sync(book, inst, p);
        return true;
    }

    /** Gives the page's price back (the grimoire took it before the cast; a refused ability must not cost). */
    private static void refund(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int mode) {
        try {
            double cost = book.getMagiculeCost(p, inst, mode);
            if (cost > 0) EnergyHelper.gainMagicule(p, cost, EnergyHelper.GainType.NORMAL);
        } catch (Throwable ignored) { /* no Tensura data: nothing was taken */ }
    }
}
