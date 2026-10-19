package com.newuniverse.nusmp.book;

import java.util.List;

/** 1.93.0 Gravity Magic: the appended pages of the book (see {@link GravityWorks}); in the book package so the page casts can stay package-private. */
public final class GravityWorksPages {
    private GravityWorksPages() {}

    public static List<BookPage> pages() {
        return List.of(
                BookPage.mid("heavy_infighting_gladiator", "Heavy Infighting: Gladiator", GravityWorks.armoured(GravityWorks::gladiator, 0)),
                BookPage.zone("evil_gods_pressure_craft", "Evil God's Pressure Craft", GravityWorks.armoured(GravityWorks::pressureCraft, 1)),
                BookPage.mid("lightened_step", "Lightened Step", GravityWorks.armoured(GravityWorks::lightenedStep, 0)).withCooldown(400),
                BookPage.mid("weighted_blows", "Weighted Blows", GravityWorks::weightedBlows).withCooldown(600));
    }
}
