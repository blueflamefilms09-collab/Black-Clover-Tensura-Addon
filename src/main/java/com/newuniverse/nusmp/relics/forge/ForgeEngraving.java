package com.newuniverse.nusmp.relics.forge;

import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the Relic Forge's third panel puts a Tensura engraving (an enchantment of the {@code #tensura:engraving} tag) from an enchanted book onto a
 * mod weapon. Engravings are plain vanilla enchantments on the stack (that is how {@code item.WeaponEngravings} and {@code relics.RelicEngravings}
 * store them, marked once with "NUEngraved"), so the stack can be edited safely from outside those classes with the ordinary enchantment API and
 * their lore marker is left alone: an engraving added here is never undone or doubled by their once-only pass. The forge counts only the engravings
 * IT adds (a small int in the custom data, "NUForgeEngr") so {@link ForgeRules#ENGRAVING_CAP} limits the table, not the weapon's lore list. All
 * checks run before anything is written, so a refusal changes nothing and the book is only consumed when every engraving on it went on.
 */
public final class ForgeEngraving {
    private ForgeEngraving() {}

    /** The tag of Tensura's engravings. */
    public static final TagKey<Enchantment> ENGRAVING_TAG = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("tensura", "engraving"));

    private static final String COUNT = "NUForgeEngr";

    /** One engraving on a book: the enchantment and the level written on it. */
    public record Entry(Holder<Enchantment> enchantment, int level) {}

    /** May this stack be engraved: one of the mod's own weapons (a Black Clover magic weapon, a relic that sits in a hand, a Djinn weapon)? */
    public static boolean isModWeapon(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        if (s.getItem() instanceof MagicWeaponItem || s.getItem() instanceof DjinnWeaponItem) return true;
        RelicDef d = Relics.defOf(s);
        return d != null && d.slot() == RelicSlot.MAIN_HAND;
    }

    /** The engravings stored on an enchanted book (only enchantments of the engraving tag count; other enchantments on the book are ignored). */
    public static List<Entry> bookEngravings(ItemStack book) {
        List<Entry> out = new ArrayList<>();
        if (book == null || book.isEmpty()) return out;
        ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null) return out;
        for (Holder<Enchantment> h : stored.keySet()) if (h.is(ENGRAVING_TAG)) out.add(new Entry(h, stored.getLevel(h)));
        return out;
    }

    /** How many engravings the forge has added to this weapon. */
    public static int forgeCount(ItemStack weapon) {
        CustomData cd = weapon.get(DataComponents.CUSTOM_DATA);
        return cd == null ? 0 : cd.copyTag().getInt(COUNT);
    }

    /**
     * Puts the book's engravings on the weapon, in place. Returns {@link ForgeRules.Msg#ENGRAVED}, or the refusal (nothing changed then). The caller
     * has checked the cap against the count; it is checked again here because the server is the only authority.
     */
    public static ForgeRules.Msg apply(ServerPlayer p, ItemStack weapon, ItemStack book) {
        if (!isModWeapon(weapon)) return ForgeRules.Msg.NOT_A_WEAPON;
        List<Entry> add = bookEngravings(book);
        if (add.isEmpty()) return ForgeRules.Msg.NOT_AN_ENGRAVING;
        ForgeRules.Msg cap = ForgeRules.engraveCap(forgeCount(weapon), add.size());
        if (cap != ForgeRules.Msg.OK) return cap;
        ItemEnchantments have = weapon.getEnchantments();
        for (int i = 0; i < add.size(); i++) {
            Holder<Enchantment> h = add.get(i).enchantment();
            boolean compatible = true;
            for (Holder<Enchantment> other : have.keySet()) if (!other.equals(h) && !Enchantment.areCompatible(h, other)) compatible = false;
            for (int j = 0; j < i; j++) if (!Enchantment.areCompatible(h, add.get(j).enchantment())) compatible = false;
            ForgeRules.Msg m = ForgeRules.engraveEntry(have.getLevel(h) > 0, compatible);
            if (m != ForgeRules.Msg.OK) return m;
        }
        for (Entry e : add) weapon.enchant(e.enchantment(), ForgeRules.engraveLevel(e.level(), e.enchantment().value().getMaxLevel()));
        int total = forgeCount(weapon) + add.size();
        CustomData.update(DataComponents.CUSTOM_DATA, weapon, (CompoundTag t) -> t.putInt(COUNT, total));
        if (!p.isCreative()) book.shrink(1);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
        Fx.burst(p.serverLevel(), FxThemes.RELIC_FORGE, p, FxTone.LIGHT, 4);
        return ForgeRules.Msg.ENGRAVED;
    }
}
