package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * Why: the third way into the treasury (after a mage's own magic and sheer force) is the tomb's Sigil, carried by the keeper of the key room. It is not a new item: a stack of vanilla echo shard,
 * named for the tomb, with the tomb's id and seed in its custom data (a vanilla data component, so nothing needs registering, nothing breaks if the mod is removed, and a Sigil of one tomb is just a
 * shard in another). It opens only the seal of the tomb it was made in, once: using it uses it up.
 */
public final class PocketSigil {
    private PocketSigil() {}

    public static final String KEY = "nusmp_pocket_sigil", SEED = "nusmp_pocket_sigil_seed";

    /** A Sigil of this tomb instance. */
    public static ItemStack make(PocketDef def, long seed) {
        ItemStack s = new ItemStack(Items.ECHO_SHARD);
        CompoundTag t = new CompoundTag();
        t.putString(KEY, def.id());
        t.putLong(SEED, seed);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.CUSTOM_NAME, Component.translatableWithFallback("pocket.nusmp." + def.id() + ".sigil", def.seal().sigil()).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(def.accent()).withItalic(false)));
        s.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatableWithFallback("message.nusmp.mega.pocket.sigil.lore1", "Cut for the seal of %s.", Component.translatableWithFallback("pocket.nusmp." + def.id() + ".name", def.name())).withStyle(ChatFormatting.GRAY),
                Component.translatableWithFallback("message.nusmp.mega.pocket.sigil.lore2", "Press it to the seal. It is spent when the seal opens.").withStyle(ChatFormatting.DARK_GRAY))));
        s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        return s;
    }

    /** Is this stack the Sigil of this very tomb? */
    public static boolean matches(ItemStack s, String pocketId, long seed) {
        if (s == null || s.isEmpty()) return false;
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        CompoundTag t = cd.copyTag();
        return pocketId.equals(t.getString(KEY)) && t.getLong(SEED) == seed;
    }
}
