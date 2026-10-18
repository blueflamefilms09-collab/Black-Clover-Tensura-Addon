package com.newuniverse.nusmp.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Why: the skills used to print hard-coded English ("Not enough magicules", "Recalled 3 summon(s)."). Every message, mode name and chant
 * line now goes through these three factories, which build a translatable component with an English fallback, so a missing lang entry
 * still reads correctly and tools/skills/gen_lang_fragment.py can find every key by scanning for these calls. Keep the key and the fallback
 * as plain string literals at the call site (the generator reads them from the source).
 */
public final class SkillMsg {
    private SkillMsg() {}

    /** A message: key under {@code nusmp.skill.msg.}; {@code %s} in the fallback is filled by the args. */
    public static MutableComponent t(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("nusmp.skill.msg." + key, fallback, args);
    }

    /** A mode name: key under {@code nusmp.skill.mode.} (the pre-existing mode keys keep their names). */
    public static MutableComponent mode(String key, String fallback) {
        return Component.translatableWithFallback("nusmp.skill.mode." + key, fallback);
    }

    /** One line of a chant: {@code nusmp.skill.chant.<spell>.<line>} (line starts at 1). */
    public static MutableComponent chant(String spell, int line, String fallback) {
        return Component.translatableWithFallback("nusmp.skill.chant." + spell + "." + line, fallback);
    }
}
