package com.newuniverse.nusmp.skilltree.ft.fire;

import com.newuniverse.nusmp.skilltree.ft.dragon.DragonSkill;

/**
 * Why: the seven active techniques of the Fire Dragon Slayer route share what is route-wide and nothing else: the route id, the shipped numbers to fall back on, and the colour
 * of their warning dust. Keeping that in one small base (on top of {@link DragonSkill}, which both Dragon Slayer routes share) means a technique class is only its own intent:
 * what it finds, what it hits, what it shows. The class is abstract and holds no state, so every technique stays a plain singleton ManasCore skill, registered once in
 * {@code FireDragonSkills}, cast only on the server, with the cast gate of the skill tree in front of it.
 */
public abstract class FireDragonSkill extends DragonSkill {
    /** The colour (0xRRGGBB) of the warning dust of every fire technique: the orange of the route. */
    protected static final int TELL_RGB = 0xFF6A1E;
    /** The same orange as an effect colour (0xAARRGGBB), for the effect shapes that take a colour instead of using their own palette. */
    protected static final int FLAME_ARGB = 0xFFFF7A2A;

    /**
     * @param nodeId the node of the Fire route this skill serves (a constant of {@link FireDragonNumbers})
     * @param icon   the file name under {@code textures/skill/ft/}
     */
    protected FireDragonSkill(String nodeId, String icon) { super(FireDragonNumbers.ROUTE, nodeId, icon, FireDragonNumbers.TABLES); }
}
