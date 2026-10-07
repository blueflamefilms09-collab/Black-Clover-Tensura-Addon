package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.grimoire.CanonBook;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * Cotton / Food Magic (Charmy Pappitson, 0.34): a dual-attribute grimoire. When it is first bound it rolls which side it leans to -
 * Cotton (the sheep: Sheep Cook, Sheep Bondage, Cotton Cloud) or Food (the glutton: Glutton's Banquet, Gourmet's Bite), 50 / 50.
 * Charmy's own canon grimoire has both. The starter, Sleeping Sheep Strike, is shared. The roll lives on the skill ("Half") and is
 * settled the first time a page or the book needs it (0.48 fix).
 */
public class CottonBook extends ElementBook {
    static final Set<String> COTTON_PAGES = Set.of("sheep_cook", "sheep_bondage", "cotton_cloud");
    static final Set<String> FOOD_PAGES = Set.of("gluttons_banquet", "gourmets_bite");

    public CottonBook() {
        super(MagicType.COTTON, 0xFFFFF4FA, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("sleeping_sheep_strike", "Sleeping Sheep Strike", WikiSpells::sleepingSheepStrike),
                mid("sheep_cook", "Sheep Cook", WikiSpells::sheepCook).withCooldown(900),
                mid("sheep_bondage", "Sheep Bondage", WikiSpells::sheepBondage),
                zone("cotton_cloud", "Cotton Cloud", WikiSpells::cottonCloud),
                mid("gourmets_bite", "Gourmet's Bite", WikiSpells::gourmetsBite),
                signature("gluttons_banquet", "Glutton's Banquet", WikiSpells::gluttonsBanquet)));
    }

    /** "COTTON", "FOOD" or "BOTH" ("" until rolled; see {@link #resolveHalf}). */
    public static String half(ManasSkillInstance i) { return i.getOrCreateTag().getString("Half"); }

    /** Charmy's own grimoire: the canon mark on the bound grimoire, wherever it is (slot, hands or inventory). */
    static boolean isCharmy(ServerPlayer p) {
        if (CharacterSpells.characterOf(p) == CanonBook.CHARMY) return true;
        for (var s : p.getInventory().items)
            if (com.newuniverse.nusmp.blackclover.GrimoireItem.isOwnedBy(s, p.getUUID())
                    && CanonBook.byId(com.newuniverse.nusmp.blackclover.GrimoireItem.data(s).getString("Canon")) == CanonBook.CHARMY) return true;
        return false;
    }

    /**
     * 0.48 fix: the half is settled the moment anything needs it (it used to wait for a skill tick that may never come, which sealed
     * every Cotton and Food page with "A page stirs..."). Charmy's grimoire is always BOTH, also when it was rolled before the canon
     * mark arrived; a change clears the page reminders so the pages announce themselves again.
     */
    static String resolveHalf(ManasSkillInstance i, ServerPlayer p) {
        String h = half(i);
        boolean charmy = isCharmy(p);
        if (charmy && !h.equals("BOTH")) h = "BOTH";
        else if (!h.isEmpty()) return h;
        else h = p.getRandom().nextBoolean() ? "COTTON" : "FOOD";
        i.getOrCreateTag().putString("Half", h);
        i.getOrCreateTag().remove("GateTold");
        i.markDirty();
        p.sendSystemMessage(Component.literal(switch (h) {
            case "BOTH" -> "Your grimoire holds both halves: Cotton and Food.";
            case "FOOD" -> "Your grimoire leans to Food Magic: its pages will feed on magic.";
            default -> "Your grimoire leans to Cotton Magic: its pages are soft and woolly.";
        }).withStyle(ChatFormatting.LIGHT_PURPLE));
        return h;
    }

    @Override
    protected void tickBook(ManasSkillInstance i, ServerPlayer p) {
        super.tickBook(i, p);
        resolveHalf(i, p);
    }

    @Override
    public String pageBlock(ServerPlayer p, ManasSkillInstance inst, int mode) {
        String base = super.pageBlock(p, inst, mode);
        if (base != null) return base;
        BookPage page = page(mode);
        if (page == null) return null;
        String h = resolveHalf(inst, p);
        if (COTTON_PAGES.contains(page.id()) && !(h.equals("COTTON") || h.equals("BOTH"))) return "a grimoire that leans to Cotton";
        if (FOOD_PAGES.contains(page.id()) && !(h.equals("FOOD") || h.equals("BOTH"))) return "a grimoire that leans to Food";
        return null;
    }
}
