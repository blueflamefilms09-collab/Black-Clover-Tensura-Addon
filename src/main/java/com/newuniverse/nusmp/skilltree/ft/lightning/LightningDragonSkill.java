package com.newuniverse.nusmp.skilltree.ft.lightning;

import com.newuniverse.nusmp.skilltree.ft.dragon.DragonSkill;

/**
 * Why: the eight active techniques of the Lightning Dragon Slayer route share what is route-wide and nothing else: the route id, the shipped numbers to fall back on, and the
 * colours of their warning dust and of the effect shapes that take a colour. One small abstract base (on top of {@link DragonSkill}, which both Dragon Slayer routes share) keeps
 * each technique class down to its own intent, and keeps every technique a plain singleton ManasCore skill, registered once in {@code LightningDragonSkills}, cast only on the
 * server, with the cast gate of the skill tree in front of it and the balance law behind every hit.
 */
public abstract class LightningDragonSkill extends DragonSkill {
    /** The colour (0xRRGGBB) of the warning dust of every lightning technique: the yellow of the route. */
    protected static final int TELL_RGB = 0xFFE14A;
    /** The same yellow as an effect colour (0xAARRGGBB), for the effect shapes that take a colour instead of using their own palette. */
    protected static final int BOLT_ARGB = 0xFFFFE14A;

    /**
     * @param nodeId the node of the Lightning route this skill serves (a constant of {@link LightningDragonNumbers})
     * @param icon   the file name under {@code textures/skill/ft/}
     */
    protected LightningDragonSkill(String nodeId, String icon) { super(LightningDragonNumbers.ROUTE, nodeId, icon, LightningDragonNumbers.TABLES); }
}
