package com.newuniverse.nusmp.heroweapon.skill;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: the lang texts keep the base numbers ("lasts 8 seconds"), which is what the tests pin and what a server with {@code durationScale} 1.0 plays. When the setting
 * lengthens a technique, the Weapon Book shows the real number: this table says, per skill or passive key ({@code skill.shield.cover}, {@code passive.staff.wide_prism}),
 * which base lengths in the text were scaled, and {@link #apply} rewrites exactly those numbers (and no other number in the text, so a wind-up or window that stays is
 * never touched). Pure, client display only. Keys not listed keep their text as it is.
 */
public final class DurText {
    private DurText() {}

    private static final Pattern NUM = Pattern.compile("(\\d+(?:\\.\\d+)?)( ?)(seconds|second|s)\\b");

    /** key to the base lengths in ticks that the text names and the code scales. */
    private static final Map<String, int[]> RULES = Map.ofEntries(
            Map.entry("skill.shield.air_strike", new int[]{160}), Map.entry("skill.shield.second_shield", new int[]{200}),
            Map.entry("skill.shield.cover", new int[]{160}), Map.entry("skill.shield.provoke", new int[]{80}),
            Map.entry("skill.shield.herb_mend", new int[]{100}), Map.entry("skill.shield.soul_aegis", new int[]{240}),
            Map.entry("skill.shield.undead_control", new int[]{120}), Map.entry("skill.shield.absolute_defence", new int[]{100}),
            Map.entry("skill.shield.rage_roar", new int[]{120}),
            Map.entry("skill.sword.afterimage", new int[]{60}), Map.entry("skill.sword.float_blades", new int[]{160}),
            Map.entry("skill.sword.gold_rebellion", new int[]{120}),
            Map.entry("skill.spear.vault", new int[]{60}), Map.entry("skill.spear.filo_rush", new int[]{50}),
            Map.entry("skill.staff.magic_prism", new int[]{160}), Map.entry("skill.staff.float_mirror", new int[]{80}),
            Map.entry("skill.staff.fenrir_rod", new int[]{160}), Map.entry("skill.staff.analyze", new int[]{200}),
            Map.entry("passive.staff.wide_prism", new int[]{80}), Map.entry("passive.staff.deep_mirror", new int[]{40}),
            Map.entry("passive.staff.star_mark", new int[]{100}),
            Map.entry("skill.fan.wind_dance", new int[]{80}), Map.entry("skill.fan.mist_veil", new int[]{80}),
            Map.entry("skill.fan.snow_plum", new int[]{160}),
            Map.entry("skill.mirror.glass_shield", new int[]{120}), Map.entry("skill.mirror.stardust_mirror", new int[]{160}),
            Map.entry("skill.mirror.release_rebound", new int[]{80, 120}), Map.entry("skill.mirror.movement_mirror", new int[]{240}),
            Map.entry("skill.mirror.covet", new int[]{200}),
            Map.entry("skill.instrument.march", new int[]{200}), Map.entry("skill.instrument.war_song", new int[]{160}),
            Map.entry("skill.carriage.boost", new int[]{50}), Map.entry("skill.carriage.cabin_barrier", new int[]{100}),
            Map.entry("skill.carriage.haikuikku", new int[]{160}),
            Map.entry("skill.chain.chain_defence", new int[]{120}), Map.entry("skill.katana.double_sword", new int[]{120}),
            Map.entry("skill.gauntlet.iron_guard", new int[]{60}), Map.entry("passive.projectile.mark_prey", new int[]{160}));

    /** The keys the table covers (for the table test). */
    public static Set<String> keys() { return RULES.keySet(); }

    /** {@code text} with the listed base lengths of {@code key} shown as scaled by {@code factor}; other text and other keys are returned as they are. */
    public static String apply(String key, String text, double factor) {
        int[] bases = RULES.get(key);
        if (bases == null || text == null || Dur.clampFactor(factor) == 1.0) return text;
        Matcher m = NUM.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            int ticks = (int) Math.round(Double.parseDouble(m.group(1)) * 20);
            String rep = m.group();
            for (int b : bases) {
                if (b == ticks) {
                    int scaled = Dur.scale(b, factor);
                    String unit = m.group(3);
                    if (unit.startsWith("second") && scaled != 20) unit = "seconds";
                    rep = Dur.secs(scaled) + m.group(2) + unit;
                    break;
                }
            }
            m.appendReplacement(out, Matcher.quoteReplacement(rep));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** The same with the factor now in force. */
    public static String apply(String key, String text) { return apply(key, text, Dur.factor()); }
}
