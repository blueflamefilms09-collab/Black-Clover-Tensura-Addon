package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 19, the Spellbook of Concentration. The book holds three pages of two spells each (Tensura skills); a page is marked with a bookmark that is
 * kept on the stack. Preparing a page lends its two spells for a Minecraft day; a quick revision at rest lends another page for five minutes.
 */
final class Wave19Book {
    private Wave19Book() {}

    private static final String K_PAGE = "w19_page";

    static int page(ItemStack s) {
        return Math.floorMod(s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(K_PAGE), Wave19Rules.PAGES.length);
    }

    /** Sneak and read: the bookmark moves to the next page. */
    static void turn(ActCtx c) {
        int next = Wave19Rules.nextPage(page(c.stack()));
        CustomData.update(DataComponents.CUSTOM_DATA, c.stack(), (CompoundTag t) -> t.putInt(K_PAGE, next));
        Wave19Util.tell(c.player(), "Spellbook of Concentration: the bookmark rests on the page \"" + Wave19Rules.pageName(next) + "\".", ChatFormatting.LIGHT_PURPLE);
    }

    /**
     * Prepare Spells (once a Minecraft day, the long rest): the two spells on the marked page are prepared for a day. They are Tensura skills lent to
     * you; if the skill list lacks them, a share of magicules is poured back instead.
     */
    static void prepare(ActCtx c) {
        lend(c.player(), page(c.stack()), Wave19Rules.PREPARE_TICKS, "Prepared");
    }

    /**
     * Revise (once a day, held for five seconds, the short rest): the page after the bookmark is read over and its two spells are lent for five
     * minutes, on top of what is prepared.
     */
    static void revise(ActCtx c) {
        lend(c.player(), Wave19Rules.nextPage(page(c.stack())), Wave19Rules.REVISE_TICKS, "Revised");
    }

    private static void lend(ServerPlayer p, int page, int ticks, String verb) {
        String[] ids = Wave19Rules.page(page);
        int n = Fx.lendSkills(p, ticks, ids);
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, p.getX(), p.getY() + 1.6, p.getZ(), 40, 0.5, 0.5, 0.5, 0.9);
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_19, FxTone.LIGHT, p.getX(), p.getY() + 1.6, p.getZ(), 8, 0.4, 0.4, 0.4, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (n == 0) {
            Wave19Util.addMagicules(p, ticks >= Wave19Rules.PREPARE_TICKS ? 0.25 : 0.10);
            Wave19Util.say(p, "Spellbook of Concentration: the page \"" + Wave19Rules.pageName(page) + "\" is read, but the skills are not in this world's list; magicules flow back instead.");
        } else {
            Wave19Util.say(p, "Spellbook of Concentration: " + verb + " from \"" + Wave19Rules.pageName(page) + "\": " + n + " spell" + (n == 1 ? "" : "s") + " lent for " + (ticks / 1200) + " minutes.");
        }
    }
}
