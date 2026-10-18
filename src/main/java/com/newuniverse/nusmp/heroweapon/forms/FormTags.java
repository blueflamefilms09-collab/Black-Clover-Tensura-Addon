package com.newuniverse.nusmp.heroweapon.forms;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * Why: whether an item belongs to a tag is the one question the pure form arithmetic ({@link FormTree}, {@link AbsorbRules}) cannot answer without the game's item
 * registry. Both sides ask it (the server when it counts what the gem has eaten, the client when the Weapon Book draws the progress bars), so the answer lives in this
 * small class that touches nothing but the registry, and the client never has to load the server's learning code to get it.
 */
public final class FormTags {
    private FormTags() {}

    /** Tag membership from the item registry (the server and the client both have the tags). An unknown tag or item matches nothing. */
    public static final FormTree.TagMatcher MATCHER = (tagId, itemId) -> {
        ResourceLocation tag = ResourceLocation.tryParse(tagId), item = ResourceLocation.tryParse(itemId);
        if (tag == null || item == null) return false;
        return BuiltInRegistries.ITEM.getHolder(item).map(h -> h.is(TagKey.create(Registries.ITEM, tag))).orElse(false);
    };
}
