package com.newuniverse.nusmp.skilltree.unique;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the registry half of "a Skill Book teaches a Unique skill". ONE {@link LineUniqueSkill} is registered per book, at mod construction, because the ManasCore skill registry freezes when the game starts and a
 * skill can therefore never be registered later (a datapack, a book or a command cannot add a registry entry). The ids come from the jar's own table of books ({@link LineIndex}, the file
 * {@code assets/nusmp/skill_books.json}) and the registry is filled in id order, so it is identical on every machine, which is what the client needs: the server and the client must register the same skills in
 * the same order, or the registry sync refuses the connection. The ids are the ones the inert stand-ins of 1.92 used ({@code nusmp:line_<id>}), so a player who already held a stand-in now holds the real skill.
 * The register is a SEPARATE {@code DeferredRegister} from {@code NUSkills.SKILLS}, so the numeric position of every skill the mod had before is unchanged. It also registers the inert {@link LegacySkill}
 * placeholders for the ids of the removed magics ({@link LegacyIds}), so an old save that names one still loads. Safe to call {@link #init} twice.
 */
public final class UniqueSkills {
    private UniqueSkills() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);

    private static final List<LineIndex.Line> LINES = LineIndex.shipped();
    private static final Map<String, DeferredHolder<ManasSkill, LineUniqueSkill>> BY_BARE = new LinkedHashMap<>();
    private static boolean done;

    static {
        if (LINES.isEmpty()) LOG.error("[nusmp] skill books: {}", LineIndex.problem());
        for (LineIndex.Line l : LINES) BY_BARE.put(l.bare(), SKILLS.register(l.skillPath(), () -> new LineUniqueSkill(l)));
        for (String path : LegacyIds.REMOVED_TECHNIQUES) SKILLS.register(path, LegacySkill::new);
        for (String bare : LegacyIds.REMOVED_LINES) SKILLS.register(LineIndex.skillPath(bare), LegacySkill::new);
    }

    /** Registers the skills on the mod bus. Called once from {@code SkillBookSystem.init}. */
    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        SKILLS.register(modBus);
    }

    /** Every book the table knows, in id order. */
    public static List<LineIndex.Line> lines() { return LINES; }

    /** The line of a bare id ({@code lady_luck}) or route id, or null. */
    public static LineIndex.Line line(String bareOrRouteId) { return LineIndex.byBare(LINES, bareOrRouteId); }

    /** The line whose book id is this ({@code nusmp:lady_luck_book}), or null. */
    public static LineIndex.Line lineOfBook(String bookId) { return LineIndex.byBook(LINES, bookId); }

    /** The bare ids that have a skill, in id order. */
    public static List<String> bares() { return Collections.unmodifiableList(new ArrayList<>(BY_BARE.keySet())); }

    public static int count() { return BY_BARE.size(); }

    /** The holder of a line's skill, or null when the id is not a book of the table. */
    public static DeferredHolder<ManasSkill, LineUniqueSkill> holder(String bareOrRouteId) { return bareOrRouteId == null ? null : BY_BARE.get(LineIndex.bare(bareOrRouteId)); }

    /** The registry id of a line's skill ({@code nusmp:line_<bare>}). */
    public static ResourceLocation skillId(String bareOrRouteId) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, LineIndex.skillPath(LineIndex.bare(bareOrRouteId))); }

    /** The holders in id order. */
    public static List<DeferredHolder<ManasSkill, LineUniqueSkill>> holders() { return Collections.unmodifiableList(new ArrayList<>(BY_BARE.values())); }
}
