package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Bone Magic after the Black Clover wiki (the table is {@link LightBoneJa}). It is still an {@link ElementBook} (the old pages and their shapes are unchanged), but its page list depends on the
 * server config {@code magic.lightBoneJa} (default on): on, the book shows the remake: Bone Spear becomes Bone Bullets and Ossuary becomes Eternal Fangs (both keep their page id, so unlocked
 * pages and mastery stay), Bone Armor, Rib Cage and Bone Rain are the old pages, and the Demon Sword Dainsleif and Spatial Rupture are appended after everything the book already had; off, the book
 * is exactly the old one. The page list is rebuilt when the switch changes ({@link #pagesVersion}).
 */
public class BoneBook extends ElementBook {
    private final List<BookPage> legacy;
    private final List<BookPage> remake;

    public BoneBook(List<BookPage> legacy) {
        super(MagicType.BONE, 0xFFE8E0C8, TensuraDamageTypes.MAGIC_GENERIC, legacy);
        this.legacy = legacy;
        List<BookPage> l = LightBoneJaKit.swap(legacy, "bone_spear", "Bone Bullets", BoneJaArts.bullets(), "out");
        l = LightBoneJaKit.swap(l, "ossuary", "Eternal Fangs", BoneJaArts.eternalFangs(), "signature");
        l = new ArrayList<>(l);
        l.add(BookPage.signature("demon_sword_dainsleif", "Demon Sword Dainsleif", BoneJaArts.dainsleif()).withCooldown(600).withAnim("sweep"));
        l.add(BookPage.zone("spatial_rupture", "Spatial Rupture", BoneJaArts.spatialRupture()).withCooldown(300).withAnim("sweep"));
        this.remake = List.copyOf(l);
    }

    @Override protected List<BookPage> familyPages() { return LightBoneJaKit.enabled() ? remake : legacy; }

    @Override protected int pagesVersion() { return LightBoneJaKit.enabled() ? 1 : 0; }
}
