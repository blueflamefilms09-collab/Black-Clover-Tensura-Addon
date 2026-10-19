package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.97.0: what the engraving "Legacy of the Wizard Kings" does (docs/elsdocia_legacy.md). Every distinct Black Clover magic among Elsdocia's owners gives
 * it ONE ability, kept on the stack for good ({@link ElsdociaLineage#learned}). The ability is the signature page of that magic's grimoire, cast through
 * the page's own code (the same {@code BookPage.Cast} the grimoire uses, guarded by {@link SafeCast}) with the sword in place of a summoned grimoire:
 * <ul>
 *   <li>Shift + right-click cycles the active ability: Legacy Release, Legacy Gate, then the learned magics in the order they were learned. With no
 *       learned magic yet shift + right-click is still just Legacy Gate, as before.</li>
 *   <li>Right-click casts the active ability.</li>
 *   <li>Cost: a share of the page's cost from the Aura pool (magicules if the Aura is short), {@link EnergyBridge#aura}; refunded if the page did nothing.
 *       Damage and size come from the page itself, so they scale with the wielder's EP like the grimoire's. Cooldown: per magic on the wielder.</li>
 * </ul>
 */
public final class ElsdociaArts {
    private ElsdociaArts() {}

    private static final String K_CD = "nusmp_elsdocia_cd_";

    /** The signature page of a magic: its grimoire, the page index (family pages only) and the page; null parts if the magic has no ported book. */
    public record Signature(GrimoireBook book, int mode, BookPage page) {}

    public static Signature signature(MagicType m) {
        GrimoireBook book = NUSkills.bookFor(m);
        if (book == null) return new Signature(null, -1, null);
        int n = book.familyCount();
        double[] cost = new double[n];
        boolean[] ok = new boolean[n];
        for (int i = 0; i < n; i++) {
            BookPage p = book.page(i);
            cost[i] = p == null ? 0 : p.costPercent();
            ok[i] = p != null && p.weaponTag() == null && p.costPercent() > 0;
        }
        int at = ElsdociaLineage.pickSignature(cost, ok);
        return at < 0 ? new Signature(book, -1, null) : new Signature(book, at, book.page(at));
    }

    /** The name shown for a learned magic: "Flame Magic: Crimson Lance". */
    public static String abilityName(String magic) {
        MagicType m = MagicType.lookup(magic);
        if (m == null) return magic;
        Signature s = signature(m);
        return m.displayName + (s.page() == null ? "" : ": " + s.page().name());
    }

    /** The name of the active slot. */
    public static String slotName(ElsdociaLineage l, int slot) {
        if (slot == 0) return "Legacy Release";
        if (slot == 1) return "Legacy Gate";
        String m = l.learnedAt(slot);
        return m == null ? "?" : abilityName(m);
    }

    // ---------------------------------------------------------------- use
    /**
     * Right-click with Elsdocia (server). Returns true if this handled the click. With no learned magic it does nothing at all (the old behaviour
     * stands: the technique, and shift for the Legacy Gate). Once something is learned, shift cycles and the click casts the active slot.
     */
    static boolean use(ServerPlayer p, ItemStack stack, MagicWeaponItem weapon) {
        ElsdociaEvents.Result r = ElsdociaEvents.settle(p, stack);                      // claim on first use
        if (r == ElsdociaEvents.Result.BLOCKED) return true;
        ElsdociaLineage l = ElsdociaComponents.get(stack);
        if (l.learned().isEmpty()) return false;
        if (p.isShiftKeyDown()) {
            ElsdociaLineage next = l.cycled();
            ElsdociaComponents.set(stack, next);
            int slot = next.activeSlot();
            p.displayClientMessage(Component.literal("Elsdocia: " + slotName(next, slot) + " (" + (slot + 1) + "/" + next.slots() + ")")
                    .withStyle(slot >= ElsdociaRules.BASE_SLOTS ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            return true;
        }
        int slot = l.activeSlot();
        if (slot == 1) { WeaponArts.tryAlt(p, MagicWeaponItem.Kind.ELSDOCIA); return true; }
        long now = p.level().getGameTime();
        if (slot == 0) {                                                                // Legacy Release on its own cooldown, so the item is never locked and shift can still cycle
            long ready = p.getPersistentData().getLong(K_CD + "RELEASE");
            if (now < ready) { GrimoireBook.fail(p, "Legacy Release is gathering again (" + (ready - now + 19) / 20 + "s)."); return true; }
            if (!weapon.technique(p, stack)) return true;
            p.getPersistentData().putLong(K_CD + "RELEASE", now + weapon.cooldownFor(p));
            p.displayClientMessage(Component.literal(MagicWeaponItem.Kind.ELSDOCIA.ability + "!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            return true;
        }
        cast(p, l.learnedAt(slot));
        p.getCooldowns().addCooldown(weapon, ElsdociaRules.USE_LOCK_TICKS);
        return true;
    }

    // ---------------------------------------------------------------- the learned ability
    /** Casts the signature of {@code magicName}; true if it was cast (cost taken, cooldown started). */
    public static boolean cast(ServerPlayer p, String magicName) {
        MagicType m = MagicType.lookup(magicName);
        if (m == null) { GrimoireBook.fail(p, "Elsdocia no longer knows that magic."); return false; }
        long now = p.level().getGameTime();
        long ready = p.getPersistentData().getLong(K_CD + m.name());
        if (now < ready) { GrimoireBook.fail(p, abilityName(magicName) + " is gathering again (" + (ready - now + 19) / 20 + "s)."); return false; }
        if (SafeCast.blocked(m, now)) { GrimoireBook.fail(p, m.displayName + " is recovering (" + (SafeCast.ticksLeft(m.name(), now) + 19) / 20 + "s)."); return false; }
        Signature sig = signature(m);
        double pct = sig.page() == null ? 20 : sig.page().costPercent();
        double floor = sig.page() == null ? 600 : sig.page().costFloor();
        int cooldown = sig.page() == null || sig.page().cooldown() <= 0 ? 400 : sig.page().cooldown();
        double[] pools = pools(p);
        if (!EnergyBridge.aura(p, pct / 100.0 * ElsdociaRules.COST_SHARE, floor * ElsdociaRules.FLOOR_SHARE)) {
            GrimoireBook.fail(p, "Not enough aura or magicules for " + abilityName(magicName) + ".");
            return false;
        }
        boolean done;
        if (sig.page() == null) {
            done = SafeCast.run(m.name(), now, p, () -> MagicProfiles.of(m).fallbackCast(p));
        } else {
            ManasSkillInstance inst = sig.book().createDefaultInstance();
            done = SafeCast.run(m.name(), now, p, () -> sig.page().cast().cast(sig.book(), inst, p, sig.mode()));
        }
        if (!done) { restore(p, pools); return false; }
        int ticks = (int) Math.round(cooldown * ElsdociaRules.COOLDOWN_SHARE * com.newuniverse.nusmp.NUGameRules.spellCooldown(p.level()));
        p.getPersistentData().putLong(K_CD + m.name(), now + (p.isCreative() ? ticks / 4 : ticks));
        p.displayClientMessage(Component.literal(abilityName(magicName) + "!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        return true;
    }

    private static double[] pools(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        return ex == null ? null : new double[]{ex.getAura(), ex.getMagicule()};
    }

    private static void restore(ServerPlayer p, double[] pools) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || pools == null || p.isCreative()) return;
        ex.setAura(pools[0]);
        ex.setMagicule(pools[1]);
        ex.markDirty();
    }

    // ---------------------------------------------------------------- tooltip
    /** Lines for the Elsdocia tooltip: grade, damage, owners, learned abilities. Works on the client (the component is synced). */
    public static void tooltip(ItemStack stack, List<Component> tip) {
        ElsdociaLineage l = ElsdociaComponents.get(stack);
        tip.add(Component.literal(com.newuniverse.nusmp.relics.Grade.GENESIS.label()).withStyle(com.newuniverse.nusmp.relics.Grade.GENESIS.colour(), ChatFormatting.BOLD));
        tip.add(Component.literal("Base damage: " + l.damage() + " (+" + ElsdociaRules.GRAND_STEP_DAMAGE + " for each new Grand Magic owner)").withStyle(ChatFormatting.GOLD));
        ElsdociaLineage.Owner cur = l.current();
        if (cur == null) {
            tip.add(Component.literal("No owner yet: the first hand to hold it claims it").withStyle(ChatFormatting.GRAY));
        } else {
            tip.add(Component.literal("Owner: " + cur.name() + (cur.magic().isEmpty() ? "" : " (" + magicName(cur.magic()) + ")")).withStyle(ChatFormatting.AQUA));
            ElsdociaLineage.Owner last = l.last();
            if (last != null) tip.add(Component.literal("It will not return to " + last.name()).withStyle(ChatFormatting.DARK_AQUA));
            tip.add(Component.literal("Owners so far: " + l.owners().size()).withStyle(ChatFormatting.DARK_GRAY));
        }
        tip.add(Component.literal("Legacy of the Wizard Kings: " + l.learned().size() + " of " + MagicType.values().length + " magics").withStyle(ChatFormatting.LIGHT_PURPLE));
        List<String> shown = new ArrayList<>();
        for (int i = 0; i < l.learned().size() && i < 10; i++) {
            int slot = ElsdociaRules.BASE_SLOTS + i;
            shown.add((slot == l.activeSlot() ? "> " : "  ") + abilityName(l.learned().get(i)));
        }
        for (String s : shown) tip.add(Component.literal(s).withStyle(s.startsWith(">") ? ChatFormatting.WHITE : ChatFormatting.GRAY));
        if (l.learned().size() > 10) tip.add(Component.literal("  ... and " + (l.learned().size() - 10) + " more").withStyle(ChatFormatting.DARK_GRAY));
        if (!l.learned().isEmpty()) {
            tip.add(Component.literal("Sneak + right-click: change ability. Active: " + slotName(l, l.activeSlot())).withStyle(ChatFormatting.GOLD));
        }
    }

    static String magicName(String magic) {
        MagicType m = MagicType.lookup(magic);
        return m == null ? magic : m.displayName;
    }
}
