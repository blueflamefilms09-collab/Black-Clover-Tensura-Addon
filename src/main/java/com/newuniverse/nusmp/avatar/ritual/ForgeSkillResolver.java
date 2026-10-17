package com.newuniverse.nusmp.avatar.ritual;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkills;
import com.newuniverse.nusmp.boss.rules.BossSkillRules;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the skill list in the data files is only a list of NAMES of skills; whether each one exists in the running game, and whether it is a Unique or an
 * Ultimate skill, only the game's skill registry knows (Tensura 2.0.1.3 for instance registers no Ultimate skill of its own, and the owner's brief wants only
 * real ids). So the server asks the registry: an id that is not registered is left out of the forge, an id whose real category (read through the boss rules'
 * {@link BossSkillRules#categoryOf}, the same reading the rest of the mod trusts) is Unique or Ultimate goes into THAT slot whatever the file hinted, and an id of
 * any other category (a Common or Extra skill that someone put in a data pack) is left out, because the forge binds only Special skills. If the category
 * cannot be read at all, the file's hint stands. The validator and the state packet both ask this class, so what the screen offers and what the server accepts
 * can never disagree. Cheap (one registry lookup per skill), so nothing is cached.
 */
public final class ForgeSkillResolver {
    private ForgeSkillResolver() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> LOGGED = new HashSet<>();

    /** The skill as the game sees it (kind taken from the registry), or null when it cannot be offered. Never throws. */
    public static ForgeSkill resolve(ForgeSkill data) {
        if (data == null) return null;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(data.id());
            ManasSkill skill = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
            if (skill == null) {
                once(data.id() + ":missing", "[nusmp] Soul Forge skill {} is not registered in this game: left out", data.id());
                return null;
            }
            String category = BossSkillRules.categoryOf(skill);
            ForgeSkill.Kind kind = "unique".equals(category) ? ForgeSkill.Kind.UNIQUE : "ultimate".equals(category) ? ForgeSkill.Kind.ULTIMATE
                    : "unknown".equals(category) ? data.kind() : null;
            if (kind == null) {
                once(data.id() + ":category", "[nusmp] Soul Forge skill {} is a '{}' skill, not Unique or Ultimate: left out", data.id(), category);
                return null;
            }
            if (kind != data.kind()) once(data.id() + ":kind", "[nusmp] Soul Forge skill {} is {} in the registry (the data said {}): the registry wins", data.id(), kind, data.kind());
            return kind == data.kind() ? data : data.withKind(kind);
        } catch (Throwable noSkills) {
            return null;
        }
    }

    /** The shipped (or datapack) skill with this id, resolved; null when it is not offered. */
    public static ForgeSkill lookup(String id) {
        return id == null ? null : resolve(ForgeSkills.byId(id.toLowerCase(Locale.ROOT)));
    }

    /** Every skill of the data lists that the game can offer, in file order. */
    public static List<ForgeSkill> available() {
        List<ForgeSkill> out = new ArrayList<>();
        for (ForgeSkill s : ForgeSkills.all()) {
            ForgeSkill r = resolve(s);
            if (r != null) out.add(r);
        }
        return out;
    }

    private static void once(String key, String msg, Object... args) {
        if (LOGGED.add(key)) LOG.info(msg, args);
    }

    /** Forgets what was logged (called when the server stops, so a new world logs again). */
    public static void forgetLogged() { LOGGED.clear(); }
}
