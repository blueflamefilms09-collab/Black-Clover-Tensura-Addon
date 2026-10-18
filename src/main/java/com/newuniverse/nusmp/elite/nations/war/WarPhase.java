package com.newuniverse.nusmp.elite.nations.war;

import java.util.Locale;

/**
 * Why: the phases of a war ("war phases"). DECLARED (a short notice, nobody may fight) -> PREPARATION (both sides get ready, still no fighting) -> ACTIVE (members of the two nations may fight, score counts)
 * -> CEASEFIRE (a truce window after the war ended or a ceasefire was agreed; no fighting, no new war between the pair) -> ENDED (archived into the history). A war that is withdrawn before it starts goes
 * straight to ENDED. New phases may only be appended.
 */
public enum WarPhase {
    DECLARED, PREPARATION, ACTIVE, CEASEFIRE, ENDED;

    /** The war still counts as "at war" (hostile relations): declared, preparing or fighting. */
    public boolean hostile() { return this == DECLARED || this == PREPARATION || this == ACTIVE; }

    public boolean fighting() { return this == ACTIVE; }

    public String id() { return name().toLowerCase(Locale.ROOT); }
}
