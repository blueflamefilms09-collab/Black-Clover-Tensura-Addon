package com.newuniverse.nusmp.skill;

/**
 * Why: every rebuilt skill has a display name and a description, and the owner wants all text in lang keys with an English fallback in
 * code. This tiny Minecraft-free record is the single place a skill declares both, so the key names are derived (never typed twice) and
 * tools/skills/gen_lang_fragment.py can read the declarations straight from the source to produce tools/skills/lang_fragment.json.
 */
public record SkillText(String id, String name, String description) {
    public static SkillText of(String id, String name, String description) {
        return new SkillText(id, name, description);
    }

    /** The key the skill's display name is under (unchanged from the first release, so existing lang files keep working). */
    public String nameKey() { return "nusmp.skill." + id; }

    public String descriptionKey() { return nameKey() + ".description"; }
}
