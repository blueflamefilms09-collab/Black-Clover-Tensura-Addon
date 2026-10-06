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
 * Charmy's own canon grimoire has both. The starter, Sleeping Sheep Strike, is shared. The roll lives on the skill ("Half").
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

    /** "COTTON", "FOOD" or "BOTH" (rolled on first tick). */
    public static String half(ManasSkillInstance i) { return i.getOrCreateTag().getString("Half"); }

    @Override
    protected void tickBook(ManasSkillInstance i, ServerPlayer p) {
        super.tickBook(i, p);
        if (!half(i).isEmpty()) return;
        String h = CharacterSpells.characterOf(p) == CanonBook.CHARMY ? "BOTH" : p.getRandom().nextBoolean() ? "COTTON" : "FOOD";
        i.getOrCreateTag().putString("Half", h);
        i.markDirty();
        p.sendSystemMessage(Component.literal(switch (h) {
            case "BOTH" -> "Your grimoire holds both halves: Cotton and Food.";
            case "FOOD" -> "Your grimoire leans to Food Magic: its pages will feed on magic.";
            default -> "Your grimoire leans to Cotton Magic: its pages are soft and woolly.";
        }).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public String pageBlock(ServerPlayer p, ManasSkillInstance inst, int mode) {
        String base = super.pageBlock(p, inst, mode);
        if (base != null) return base;
        BookPage page = page(mode);
        if (page == null) return null;
        String h = half(inst);
        if (COTTON_PAGES.contains(page.id()) && !(h.equals("COTTON") || h.equals("BOTH"))) return "a grimoire that leans to Cotton";
        if (FOOD_PAGES.contains(page.id()) && !(h.equals("FOOD") || h.equals("BOTH"))) return "a grimoire that leans to Food";
        return null;
    }
}
