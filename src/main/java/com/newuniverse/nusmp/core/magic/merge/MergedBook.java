package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.GrimoireSummon;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/**
 * The Unique skill that casts one merged magic. There are {@link MergeLimits#MAX_MERGED} of them (nusmp:merged_00 .. merged_63), registered
 * once at startup; the merged magic of a world is assigned to a slot when it is created, so the registry never changes while worlds do.
 * Everything about the book is read from the slot's definition: its name, its pages (the curated union of the parents', see
 * {@link MergedPages}), its colour, the magic it counts as for hooks (the dominant parent's). It is a {@link GrimoireBook} so the whole
 * casting loop, the page list, cooldowns, mastery and the UI treat it like any other grimoire; it differs in that it needs no grimoire
 * of its own (any summoned grimoire of the caster is the key) and has no universal pages.
 */
public final class MergedBook extends GrimoireBook {
    public static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/merged.png");

    private final int slot;

    public MergedBook(int slot) {
        super(MagicType.FLAME, 0xFFB8D8FF, ICON);          // the constants are placeholders: magicNow() / magicId() / colorNow() answer from the definition
        this.slot = slot;
    }

    public int slot() { return slot; }

    /** The definition this slot casts now, or null. */
    public MergedDef def() { return MergedRegistry.bySlot(slot); }

    @Override protected List<BookPage> familyPages() { return MergedPages.pages(slot); }

    @Override protected int pagesVersion() { return MergedPages.version(slot); }

    @Override
    public ResourceKey<DamageType> damageType() {
        GrimoireBook dominant = MergedPages.parentBook(slot, 0);
        return dominant != null ? dominant.damageType() : TensuraDamageTypes.MAGIC_GENERIC;
    }

    // ---------------------------------------------------------------- what the book calls itself
    @Override public MagicType magicNow() {
        MergedDef d = def();
        MagicType t = d == null ? null : MagicType.lookup(d.dominant());
        return t != null ? t : magic;
    }

    @Override public String magicId() { MergedDef d = def(); return d == null ? "merged:unbound_" + slot : d.id(); }

    @Override public String magicLabel() { MergedDef d = def(); return d == null ? "Merged magic" : d.name(); }

    @Override public int colorNow() { MergedDef d = def(); return d == null ? color : MergedRegistry.blend(d).primary; }

    @Override
    public MutableComponent getName() {
        MergedDef d = def();
        return d == null ? Component.translatable("nusmp.merge.unbound", slot + 1) : Component.literal(d.name());
    }

    @Override
    public MutableComponent getSkillDescription() {
        MergedDef d = def();
        if (d == null) return Component.translatable("nusmp.merge.unbound.description");
        StringBuilder sb = new StringBuilder();
        for (String p : d.parents()) sb.append(sb.length() == 0 ? "" : ", ").append(com.newuniverse.nusmp.core.magic.profile.MagicProfiles.of(p).displayName());
        return Component.translatable("nusmp.merge.description", d.name(), sb.toString());
    }

    // ---------------------------------------------------------------- rules that differ from a normal book
    /** The parents' grimoires are the keys: any summoned grimoire of the caster opens a merged book's pages. */
    @Override protected boolean holdingBook(ServerPlayer p) { return GrimoireSummon.isFloating(p); }

    @Override protected boolean givesGrimoireItem() { return false; }
    @Override protected boolean universalPages() { return false; }
    @Override protected MagicType summonMagic() { return null; }
}
