package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.MercuryArts;
import com.newuniverse.nusmp.book.MercuryRules;

import java.util.List;

/**
 * 1.93.0 Mercury Magic rebuild (it was the 0.53 audit stub): the pages APPENDED after the four of MercuryBook (Silver Blade, Silver Guardian, Silver Rain, Silver Eagle keep their ids, order
 * and mastery bits). Canon: the wiki's Silver Bullet, Silver Spear, Silver Shining Regal Spear and Silver Star of Execution. Ids are save keys: never rename them.
 */
public final class MercuryExt {
    private MercuryExt() {}

    private static final List<BookPage> PAGES = List.of(
            BookPage.starter(MercuryRules.BULLET, "Mercury Magic: Silver Bullet", MercuryArts::silverBullet),
            BookPage.starter(MercuryRules.SPEAR, "Mercury Magic: Silver Spear", MercuryArts::silverSpear),
            BookPage.signature(MercuryRules.REGAL, "Mercury Magic: Silver Shining Regal Spear", MercuryArts::regalSpear).withCooldown(600).withAnim("cast_release_thrust"),
            BookPage.signature(MercuryRules.STAR, "Mercury Creation Magic: Silver Star of Execution", MercuryArts::starOfExecution).withCooldown(1200).withAnim("signature"));

    public static List<BookPage> pages() { return PAGES; }
}
