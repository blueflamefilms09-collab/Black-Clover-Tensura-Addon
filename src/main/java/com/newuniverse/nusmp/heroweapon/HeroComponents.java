package com.newuniverse.nusmp.heroweapon;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the stack of a hero weapon in the hand is only a PROJECTION of the bond (the bond is the truth), so what the stack must carry is little and must reach
 * other players' clients: which look to draw ({@code anime_multiverse:hero_look}: {@code base} or {@code curse}, set by the forms system when the form changes, read by the
 * renderer so that no draw call looks up a bond) and whose weapon it is ({@code anime_multiverse:hero_owner}, the player's uuid: a projection in anyone else's hands
 * dissolves). Both are persistent and network-synchronised like the arc spawn egg's character component.
 */
public final class HeroComponents {
    private HeroComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** {@code base} or {@code curse}: the mesh and glow set the renderer draws. Absent means base. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> LOOK =
            COMPONENTS.registerComponentType("hero_look", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** 4.23.0: the id of the active form ({@code base} or a form id of the weapon's forms file), set with the look; the renderer draws {@code <weapon>__<form>} from it when that mesh exists. Absent means base. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> FORM =
            COMPONENTS.registerComponentType("hero_form", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** The uuid (as text) of the player whose bond this projection belongs to. Absent on a creative-tab or command-made stack, which is adopted by its first holder. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> OWNER =
            COMPONENTS.registerComponentType("hero_owner", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** The look of a stack ({@code base} when none is set). */
    public static String look(ItemStack stack) {
        String l = stack.isEmpty() ? null : stack.get(LOOK.get());
        return l == null || l.isEmpty() ? "base" : l;
    }

    /** The active form id of a stack ({@code base} when none is set). */
    public static String form(ItemStack stack) {
        String f = stack.isEmpty() ? null : stack.get(FORM.get());
        return f == null || f.isEmpty() ? "base" : f;
    }

    /** The owner uuid text of a stack, or an empty string. */
    public static String owner(ItemStack stack) {
        String o = stack.isEmpty() ? null : stack.get(OWNER.get());
        return o == null ? "" : o;
    }
}
