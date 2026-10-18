package com.newuniverse.nusmp.skilltree.menu;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the registry half of "a Unique Skill Line is a Unique Skill in Tensura's menu". ONE stand-in skill ({@link LineStandInSkill}) is registered per line, at mod construction, because the ManasCore
 * skill registry freezes when the game starts and a line can therefore never be registered later (a datapack, a Skill Book or a command cannot add a registry entry). The ids come from the jar's own
 * table of lines ({@link LineIndex}) and the registry is filled in id order, so it is identical on every machine, which is what the client needs: the server and the client must register the same skills
 * in the same order, or the registry sync refuses the connection. This register is a SEPARATE {@code DeferredRegister} from {@code NUSkills.SKILLS} and is registered after it, so the numeric
 * position of every skill the mod had before is unchanged (add the new, keep the old). The class is Minecraft-registry code only; the rules (who gets which stand-in) are in {@link StandInPlan}, and
 * the teaching and forgetting is in {@link StandInGrants}. Safe to call {@link #init} twice.
 */
public final class LineStandIns {
    private LineStandIns() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);

    private static final List<LineIndex.Line> LINES = loadLines();
    private static final Map<String, DeferredHolder<ManasSkill, LineStandInSkill>> BY_ROUTE = new LinkedHashMap<>();
    private static boolean done;

    private static List<LineIndex.Line> loadLines() {
        List<LineIndex.Line> lines = LineIndex.load();
        if (lines.isEmpty()) LogUtils.getLogger().error("[nusmp] tensura menu: {}", LineIndex.problem());
        return lines;
    }

    static {
        for (LineIndex.Line l : LINES) BY_ROUTE.put(l.routeId(), SKILLS.register(l.skillPath(), () -> new LineStandInSkill(l)));
    }

    /** Registers the skills on the mod bus, the config on the container, the server-side teaching, and the in-game self-test. Called once from {@code SkillTreeSystem.init}. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        SKILLS.register(modBus);
        MenuConfig.register(container);
        StandInGrants.register();
        MenuSelfTests.register();
    }

    /** Every line the table knows, in id order. */
    public static List<LineIndex.Line> lines() { return LINES; }

    /** The route ids that have a stand-in, in id order. */
    public static Set<String> routeIds() { return Collections.unmodifiableSet(BY_ROUTE.keySet()); }

    public static int count() { return BY_ROUTE.size(); }

    /** The holder of a line's stand-in, or null when the id is not a line of the table. */
    public static DeferredHolder<ManasSkill, LineStandInSkill> holder(String routeId) { return BY_ROUTE.get(routeId); }

    /** The registry id of a line's stand-in ({@code nusmp:line_<bare>}). */
    public static ResourceLocation skillId(String routeId) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, LineIndex.skillPath(LineIndex.bare(routeId))); }

    /** The holders in id order. */
    public static List<DeferredHolder<ManasSkill, LineStandInSkill>> holders() { return Collections.unmodifiableList(new ArrayList<>(BY_ROUTE.values())); }

    /** True when the skill is one of the stand-ins. */
    public static boolean isStandIn(ManasSkill skill) { return skill instanceof LineStandInSkill; }
}
