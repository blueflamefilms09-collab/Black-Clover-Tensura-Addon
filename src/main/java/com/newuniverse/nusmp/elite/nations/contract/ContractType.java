package com.newuniverse.nusmp.elite.nations.contract;

import java.util.Locale;

/**
 * Why: the kinds of contract ("contract types"). Each is CHECKABLE from a game event the server already sees, so a contract never asks the player to do something the server cannot verify: kill a mob (type or
 * tag), kill a player (a bounty), break blocks, craft an item, discover distinct biomes, hand in items. Escorts and builds are deliberately not here (they are not checkable). New kinds may only be appended.
 */
public enum ContractType {
    KILL_MOB, MINE_BLOCK, CRAFT_ITEM, EXPLORE_BIOME, DELIVER_ITEM, KILL_PLAYER;

    /** Progress comes from a game event (false: the player hands things in on the board). */
    public boolean eventDriven() { return this != DELIVER_ITEM; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static ContractType parse(String s) {
        if (s == null) return null;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
    }
}
