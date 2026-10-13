package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Wave 4: where the neck relics keep their small state. Charges, the Forest blessing, the Dreamer's gills and the creatures of the
 * Beast Masters Amulet live in the stack's custom data (so they travel with the amulet and survive relogs); the Pendant of Echoed
 * Souls keeps its once-a-day stamp on the wearer (so a second pendant cannot give a second life).
 */
final class Wave4Store {
    private Wave4Store() {}

    static final String K_GLASS = "w4_glass";               // + _charges / _day / _cracked
    static final String K_DREAM = "w4_dream";               // + _charges / _day
    static final String K_GILLS = "w4_gills_until";
    static final String K_SPIRIT = "w4_forest_spirit";
    static final String K_BLESSED_UNTIL = "w4_forest_until";
    static final String K_PRESAGE = "w4_presage_at";
    static final String K_BEASTS = "w4_beasts";
    static final String K_ECHO = "nusmp_w4_echo_at";

    /** A copy of the stack's custom data (never null). */
    static CompoundTag read(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    static void edit(ItemStack s, Consumer<CompoundTag> change) { CustomData.update(DataComponents.CUSTOM_DATA, s, change); }

    /** The worn stack of a relic by id, or empty. */
    static ItemStack find(LivingEntity e, String relicId) {
        for (Relics.Worn w : Relics.wornList(e)) {
            if (w.def().id().equals(relicId) && !w.stack().isEmpty()) return w.stack();
        }
        return ItemStack.EMPTY;
    }

    static Wave4Charges.State charges(CompoundTag t, String prefix, int max, long today) {
        if (!t.contains(prefix + "_charges", 3)) return Wave4Charges.fresh(max, today);
        return new Wave4Charges.State(t.getInt(prefix + "_charges"), t.getLong(prefix + "_day"), t.getBoolean(prefix + "_cracked"));
    }

    static void putCharges(CompoundTag t, String prefix, Wave4Charges.State s) {
        t.putInt(prefix + "_charges", s.charges());
        t.putLong(prefix + "_day", s.day());
        t.putBoolean(prefix + "_cracked", s.cracked());
    }

    /** The player's own kept data (survives death and relog; the same place the mod's mana system uses). */
    static CompoundTag kept(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return pd.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
