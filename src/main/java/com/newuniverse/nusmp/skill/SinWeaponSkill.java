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
    private static final String[] IDS = {"technique", "manifest", "ability_a", "ability_b", "decree", "form"};      // the last two (appended) only for the four Lord weapons, see lordModes()

    private final SinRules.Kind kind;

    public SinWeaponSkill(SinRules.Kind kind) { super(SkillType.ULTIMATE, skillText(kind), true); this.kind = kind; }
    @Override public UltimateClass ultimateClass() { return UltimateClass.LORD; }      // docs/ultimate_classes.md

    /** Name and description of each skill, as plain literals (tools/skills/gen_lang_fragment.py reads them from here). The id is {@code SinSkillRules.skillId}. */
    static SkillText skillText(SinRules.Kind k) {
        return switch (k) {
            case SLOTH -> SkillText.of("lord_of_slothful_weight", "Lord of Slothful Weight",
                    "Ultimate. The skill of the Black Axe of Sloth, a Weapon of Mortal Sin. Modes: Technique (the art of the form in your hand, the right click of the axe), Manifest / Store (the axe is built out of the skill: hold it to store it back, otherwise a bound one is forged), two more techniques as modes 3 and 4 (Stillness and Crushing Gravity), the Lord's Decree as mode 5 (Abyssal Stillness, opens at level 8 of the Mastery of Stillness) and Switch form as mode 6 (the Titan's Cleaver opens at level 3, the Chained Anchor at level 6). Passive: Sinking Weight, on the plain hits of its owner's axe. Drawback: the Weight of Sloth, explained when the axe binds and in its tooltip.");
            case ENVY -> SkillText.of("lord_of_green_eyed_envy", "Lord of Green-Eyed Envy",
                    "Ultimate. The skill of the Black Gunblade of Envy, a Weapon of Mortal Sin. Modes: Technique (the art of the form in your hand, the right click of the gunblade), Manifest / Store (the gunblade is built out of the skill: hold it to store it back, otherwise a bound one is forged), two more techniques as modes 3 and 4 (Green Eyes and Stolen Life), the Lord's Decree as mode 5 (Covet All, opens at level 8 of the Mastery of Covetousness) and Switch form as mode 6 (the Coveted Rifle opens at level 3, the Serpent Coil at level 6). Passive: Glaring Eyes, on the plain hits of its owner's gunblade. Drawback: the Hollow of Envy, explained when the gunblade binds and in its tooltip.");
            case VANITY -> SkillText.of("lord_of_empty_vanity", "Lord of Empty Vanity",
                    "Ultimate. The skill of the Black Spear of Vanity, a Weapon of Mortal Sin. Modes: Technique (the art of the form in your hand, the right click of the spear), Manifest / Store (the spear is built out of the skill: hold it to store it back, otherwise a bound one is forged), two more techniques as modes 3 and 4 (Reflected Thrust and Narcissus), the Lord's Decree as mode 5 (Hall of Mirrors, opens at level 8 of the Mastery of Reflection) and Switch form as mode 6 (the Mirror Glaive opens at level 3, the Peacock Halberd at level 6). Passive: Vain Shine, on the plain hits of its owner's spear. Drawback: the Cracked Mirror, explained when the spear binds and in its tooltip.");
            case CHAKRAMS -> SkillText.of("lord_of_the_twin_rings", "Lord of the Twin Rings",
                    "Ultimate. The skill of the Black Chakrams, a Weapon of Mortal Sin (the sin of the rings is unnamed in canon: the name is a placeholder). Modes: Technique (Spinning Ring, the right click of the chakrams in your main hand) and Manifest / Store (the chakrams are built out of the skill: hold them to store them back, otherwise a bound pair is forged). Two more techniques follow as modes 3 and 4, each with its own cooldown: Thrown Rings and Binding Embrace. Passive: Hungry Rings, on the plain hits of its owner's chakrams.");
            case HOLY -> SkillText.of("seifort_holy_judgement", "Seifort's Holy Judgement",
                    "Ultimate. The skill of the holy sword Seifort. Modes: Technique (the art of the form in your hand, the right click of the sword), Manifest / Store (the sword is built out of the skill: hold it to store it back, otherwise a bound one is forged), two more techniques as modes 3 and 4 (Purge and Holy Ground), the Lord's Decree as mode 5 (Holy Judgement, opens at level 8 of the Mastery of Devotion) and Switch form as mode 6 (Seifort, Radiant opens at level 3, Seifort, Sent Forth at level 6). Passive: Smiting Light, on the plain hits of its owner's sword. Drawback: Penance, explained when the sword binds and in its tooltip.");
        };
    }

    public SinRules.Kind sinKind() { return kind; }

    /** Six modes for the four Lord weapons (the Lord's decree and the form switch were appended), four for the Chakrams. */
    private int count() { return kind == SinRules.Kind.CHAKRAMS ? 4 : IDS.length; }

    private com.newuniverse.nusmp.lords.LordRules.Sin lord() { return com.newuniverse.nusmp.lords.LordItems.fromKind(kind); }

    @Override public int getModes(ManasSkillInstance instance) { return count(); }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return Math.floorMod(mode + (reverse ? -1 : 1), count());
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) { return "sinskill." + kind.id + "." + IDS[Math.floorMod(mode, count())]; }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        int m = Math.floorMod(mode, count());
        if (m == 4) return Component.translatable(lord().ultimateKey());                      // the Lord's decree
        if (m == 5) return SkillMsg.mode("lords.mode.form", "Switch form");
        if (m >= 2) {                                                                          // the two extra techniques of the weapon (docs/sin_weapons_abilities.md)
            SinRules.Ability a = SinRules.ability(kind, m - 1);
            return Component.translatableWithFallback(a.key(), a.english);
        }
        return m == 0 ? SkillMsg.mode("sinskill.technique", "Technique") : SkillMsg.mode("sinskill.manifest", "Manifest / Store");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        int m = Math.floorMod(mode, count());
        com.newuniverse.nusmp.lords.LordRules.Sin lord = lord();
        if (m == 4 && lord != null) com.newuniverse.nusmp.lords.LordService.decree(p, lord);
        else if (m == 5 && lord != null) com.newuniverse.nusmp.lords.LordService.cycleForm(p, false);
        else if (m == 0 && lord != null && p.getMainHandItem().getItem() instanceof com.newuniverse.nusmp.lords.LordItem li && li.sin() == lord) li.fire(p);   // a new form's art, with the right click's gates
        else if (m == 0) SinSkillService.technique(p, kind);
        else if (m == 1) SinSkillService.manifest(p, kind);
        else SinSkillService.ability(p, kind, m - 1);
    }
}
