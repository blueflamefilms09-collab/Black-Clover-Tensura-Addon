package com.newuniverse.nusmp.elite.records.rule;

import java.util.List;

/**
 * Why: custom stats are free-form names in the data files, but a typo in a title's {@code "xp_bonus_pecent"} would silently give nothing. This is the closed list of the names the mod's systems read
 * (the data test fails on a name that is not here), with who reads each. Implemented by the records system itself: {@link #XP_BONUS_PERCENT}, {@link #KILL_HEAL}, {@link #BOSS_DAMAGE_PERCENT}. The
 * others are numbers other subsystems read through {@code RecordEvents.customStat(uuid, name)} (the forge for forge quality, the economy for coins, the nations and war system, the Walpurgis council
 * for the vote weight); until they read them a title that grants one is a flavour line. Adding a name here is how a new subsystem opens a new bonus. Pure.
 */
public final class StatNames {
    private StatNames() {}

    /** Extra experience from kills, in percent (records system). */
    public static final String XP_BONUS_PERCENT = "xp_bonus_percent";
    /** Hit points healed when the player kills something (records system). */
    public static final String KILL_HEAL = "kill_heal";
    /** Damage dealt to bosses (the {@code c:bosses} entity tag), in percent (records system). */
    public static final String BOSS_DAMAGE_PERCENT = "boss_damage_percent";
    /** Extra quality chance of the forge, in percent points (read by elite/forge). */
    public static final String FORGE_QUALITY_BONUS = "forge_quality_bonus";
    /** Extra Tensura coins from quests and sales, in percent (read by elite/hub). */
    public static final String COIN_BONUS_PERCENT = "coin_bonus_percent";
    /** Extra EP gained, in percent (read by whoever grants EP). */
    public static final String EP_GAIN_PERCENT = "ep_gain_percent";
    /** Damage dealt in a war, in percent (read by elite/nations). */
    public static final String WAR_DAMAGE_PERCENT = "war_damage_percent";
    /** Damage dealt to calamities, in percent (read by elite/world). */
    public static final String CALAMITY_DAMAGE_PERCENT = "calamity_damage_percent";
    /** Extra votes in the Walpurgis council (read by elite/council). */
    public static final String COUNCIL_VOTE_WEIGHT = "council_vote_weight";

    public static final List<String> ALL = List.of(XP_BONUS_PERCENT, KILL_HEAL, BOSS_DAMAGE_PERCENT, FORGE_QUALITY_BONUS, COIN_BONUS_PERCENT, EP_GAIN_PERCENT,
            WAR_DAMAGE_PERCENT, CALAMITY_DAMAGE_PERCENT, COUNCIL_VOTE_WEIGHT);
}
