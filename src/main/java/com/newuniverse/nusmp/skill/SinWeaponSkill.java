package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.sinweapons.SinRules;
import com.newuniverse.nusmp.sinweapons.SinSkillService;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * The skill of one Berserk of Gluttony weapon (Ultimate): Lord of Slothful Weight (Black Axe of Sloth), Lord of Green-Eyed Envy (Black Gunblade of Envy), Lord of Empty Vanity (Black Spear of Vanity), Lord of the Twin
 * Rings (Black Chakrams; the chakram sin is unnamed in canon, so the name is a placeholder) and Seifort's Holy Judgement (Seifort Holy Sword). One class for all five, given the {@link SinRules.Kind}, set up like
 * Lord of Gluttonous Greed: the weapon is built with the skill. It is given once, when the weapon first binds to a carrier ({@code sinweapons.SinSkillService}), and stays. Four modes switched with the skill system's mode
 * key: Technique (the weapon in the main hand fires its right-click technique), Manifest / Store (the bound weapon in the main hand goes back into the skill, else a fresh bound one is forged) and, appended,
 * the weapon's two extra techniques (docs/sin_weapons_abilities.md; each has its own cooldown; the weapon also has a passive on plain hits). All the work is
 * {@code SinSkillService}, on the server; the names are design names, not canon.
 */
public class SinWeaponSkill extends NUSkill {
    private static final String[] IDS = {"technique", "manifest", "ability_a", "ability_b"};

    private final SinRules.Kind kind;

    public SinWeaponSkill(SinRules.Kind kind) { super(SkillType.ULTIMATE, skillText(kind), true); this.kind = kind; }
    @Override public UltimateClass ultimateClass() { return UltimateClass.LORD; }      // docs/ultimate_classes.md

    /** Name and description of each skill, as plain literals (tools/skills/gen_lang_fragment.py reads them from here). The id is {@code SinSkillRules.skillId}. */
    static SkillText skillText(SinRules.Kind k) {
        return switch (k) {
            case SLOTH -> SkillText.of("lord_of_slothful_weight", "Lord of Slothful Weight",
                    "Ultimate. The skill of the Black Axe of Sloth, a Weapon of Mortal Sin. Modes: Technique (Dead Weight, the right click of the axe in your main hand) and Manifest / Store (the axe is built out of the skill: hold it to store it back, otherwise a bound one is forged). Two more techniques follow as modes 3 and 4, each with its own cooldown: Stillness and Crushing Gravity. Passive: Sinking Weight, on every plain hit of its owner's axe.");
            case ENVY -> SkillText.of("lord_of_green_eyed_envy", "Lord of Green-Eyed Envy",
                    "Ultimate. The skill of the Black Gunblade of Envy, a Weapon of Mortal Sin. Modes: Technique (Green-Eyed Shot, the right click of the gunblade in your main hand) and Manifest / Store (the gunblade is built out of the skill: hold it to store it back, otherwise a bound one is forged). Two more techniques follow as modes 3 and 4, each with its own cooldown: Green Eyes and Stolen Life. Passive: Glaring Eyes, on every plain hit of its owner's gunblade.");
            case VANITY -> SkillText.of("lord_of_empty_vanity", "Lord of Empty Vanity",
                    "Ultimate. The skill of the Black Spear of Vanity, a Weapon of Mortal Sin. Modes: Technique (Vain Thrust, the right click of the spear in your main hand) and Manifest / Store (the spear is built out of the skill: hold it to store it back, otherwise a bound one is forged). Two more techniques follow as modes 3 and 4, each with its own cooldown: Reflected Thrust and Narcissus. Passive: Vain Shine, on the plain hits of its owner's spear.");
            case CHAKRAMS -> SkillText.of("lord_of_the_twin_rings", "Lord of the Twin Rings",
                    "Ultimate. The skill of the Black Chakrams, a Weapon of Mortal Sin (the sin of the rings is unnamed in canon: the name is a placeholder). Modes: Technique (Spinning Ring, the right click of the chakrams in your main hand) and Manifest / Store (the chakrams are built out of the skill: hold them to store them back, otherwise a bound pair is forged). Two more techniques follow as modes 3 and 4, each with its own cooldown: Thrown Rings and Binding Embrace. Passive: Hungry Rings, on the plain hits of its owner's chakrams.");
            case HOLY -> SkillText.of("seifort_holy_judgement", "Seifort's Holy Judgement",
                    "Ultimate. The skill of the holy sword Seifort. Modes: Technique (Grand Cross, the right click of the sword in your main hand) and Manifest / Store (the sword is built out of the skill: hold it to store it back, otherwise a bound one is forged). Two more techniques follow as modes 3 and 4, each with its own cooldown: Purge and Holy Ground. Passive: Smiting Light, on every plain hit of its owner's sword against the undead.");
        };
    }

    public SinRules.Kind sinKind() { return kind; }

    @Override public int getModes(ManasSkillInstance instance) { return IDS.length; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return Math.floorMod(mode + (reverse ? -1 : 1), IDS.length);
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) { return "sinskill." + kind.id + "." + IDS[Math.floorMod(mode, IDS.length)]; }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        int m = Math.floorMod(mode, IDS.length);
        if (m >= 2) {                                                                          // the two extra techniques of the weapon (docs/sin_weapons_abilities.md)
            SinRules.Ability a = SinRules.ability(kind, m - 1);
            return Component.translatableWithFallback(a.key(), a.english);
        }
        return m == 0 ? SkillMsg.mode("sinskill.technique", "Technique") : SkillMsg.mode("sinskill.manifest", "Manifest / Store");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        int m = Math.floorMod(mode, IDS.length);
        if (m == 0) SinSkillService.technique(p, kind);
        else if (m == 1) SinSkillService.manifest(p, kind);
        else SinSkillService.ability(p, kind, m - 1);
    }
}
