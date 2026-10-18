package com.newuniverse.nusmp.skilltree.shop;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;

/**
 * Why: the owner's shop is "hidden, dusty, run by an old witch", and in this mod every friendly face of the arcs is a plain record for the ONE shared NPC entity
 * ({@code nusmp:arc_npc}), so the witch needs no entity class, no renderer and no registration of her own: this record gives her a name, two colours (the plum of her robe
 * and the amber of her lantern, both lifted so her name reads on a dark chat), a place in the story (she stands in the Hub only once somebody has beaten the first boss of the
 * roster, because the shop is hidden until the dungeon has proven itself), and a magic, while her words are a dialogue file and her look is a model spec of the art kit. The
 * shop itself is not part of the NPC class either: the entity asks {@link ShopHook} and the hook recognises her by {@link #ID}, so no other character can ever open a shop.
 * The record is free of game types so it is unit tested, and {@link #registerAll()} is the one line the lead adds to {@code ArcCharacters.bootstrap()}.
 */
public final class BlackMarketWitch {
    private BlackMarketWitch() {}

    /** The character id: the roster key, the dialogue file name, the model name ({@code arc_black_market_witch}) and the id {@link ShopHook} looks for. */
    public static final String ID = "black_market_witch";

    /** The boss whose defeat brings her to the Hub: the first of the roster, so that the shop is found only once the dungeon has been survived by somebody. */
    public static final String GATE = "revchi_salik";

    /** Where she comes from: the owner's request, a hidden shop run by an old witch (the story of "The Exiled Heavy Knight Knows How to Game the System"). */
    public static final String SOURCE = "The Exiled Heavy Knight Knows How to Game the System";

    /**
     * The old witch of the Black Market. Plum for her robe and amber for her lantern; her magic is Curse Magic (an old hag's trade). The name is deliberately plain ("Old Witch"):
     * the owner's text gives her no name, and a name made up here could be taken for the original's.
     */
    public static final ArcCharacter CHARACTER = ArcCharacter.npc(ID, "Old Witch").title("Keeper of the Black Market").source(SOURCE).saga(ArcSaga.ALLIES)
            .colors(0xB878E0, 0xF0B650).magic("curse").stats(0, 0, 0, 0).after(GATE).build();

    /** Adds her to the roster. Safe to call twice (registering the same record again is a no-op). */
    public static void registerAll() {
        ArcRoster.register(CHARACTER);
    }
}
