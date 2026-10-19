package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.ext.Ext;
import com.newuniverse.nusmp.book.ext.ExclusiveExt;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;
import java.util.Set;

/**
 * 1.93.0 nature-magic rebuild: the one place {@link WikiBooks#attribute} asks whether a wiki attribute has been rebuilt: Tree, Vine, Poison Plant, Mud and Mucus Magic get their own grimoire
 * ({@link TreeElementBook} with the pages of {@link TreeArts}, {@link VineArts}, {@link PoisonPlantArts}, {@link MudArts} and {@link MucusArts}) instead of the five generic pages of the family.
 * The first five page ids of every one of them are the old generic ids in the old order (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard), so unlocks, mastery
 * bits and cooldowns of existing saves keep meaning the same page; the new pages are appended. Plant Magic is not a wiki attribute and is rebuilt inside {@link PlantBook}.
 */
public final class TreeRebuild {
    private TreeRebuild() {}

    private static final Set<MagicType> MINE = Set.of(MagicType.TREE, MagicType.VINE, MagicType.POISON_PLANT, MagicType.MUD, MagicType.MUCUS);

    public static boolean handles(MagicType magic) { return MINE.contains(magic); }

    /** The rebuilt grimoire of the magic, or null when the magic is not one of the rebuilt ones. */
    public static GrimoireBook book(MagicType magic) {
        List<BookPage> pages = switch (magic) {
            case TREE -> TreeArts.pages();
            case VINE -> VineArts.pages();
            case POISON_PLANT -> PoisonPlantArts.pages();
            case MUD -> MudArts.pages();
            case MUCUS -> MucusArts.pages();
            default -> null;
        };
        if (pages == null) return null;
        ResourceKey<DamageType> damage = switch (magic) {
            case TREE, VINE, MUD -> TensuraDamageTypes.EARTH_ELEMENTAL;
            default -> TensuraDamageTypes.MAGIC_GENERIC;
        };
        return new TreeElementBook(magic, WikiBooks.color(magic), damage, Ext.join(pages, ExclusiveExt.pages(magic)));
    }
}
