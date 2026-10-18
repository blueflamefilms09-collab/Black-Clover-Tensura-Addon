package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.skills.evolution.EvolutionRuntime;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Why: the ONE registry class of the skills subsystem of the EliteTensuraAddon job (docs/elite/skills.md). It owns the DeferredRegister of the 21 new ManasCore / Tensura skills (same pattern as
 * {@code skill/NUSkills}: a {@code DeferredRegister<ManasSkill>} on the ManasCore skill registry key, namespace {@code nusmp}, grant in game with {@code /tensura edit <player> ability grant nusmp:<id>}),
 * the server config {@code nusmp-elite-skills-server.toml}, the game-bus handlers ({@link EliteSkillEvents}, the Gaia block guard, the evolution runtime), the reset ledger hook and the in-game
 * self-tests. {@code EliteSystem} calls {@link #init} with one line. The skills stay registered when the master switch is off (so worlds that have them still load); only their behaviour stops.
 * Tier of each: aspectual magic (Gravity Well, Kamehameha, Static Chain), extra (Ancient Soul, Iron Resolve, Meditation, Runesmith's Insight, Scavenger), intrinsic (Kaioken, Mechanical Eye, Mechanical Hands),
 * unique (Crocodile Skin, Hecate, Void Edge), ultimate (Basilisk Skin, Beelzebuth, Gaia, Hephaestus Forgefather, Ouroboros, Rapheal, Void Sovereign).
 */
public final class EliteSkills {
    private EliteSkills() {}

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);

    // ---- aspectual magic
    public static final DeferredHolder<ManasSkill, GravityWellMagic> GRAVITY_WELL = SKILLS.register("gravity_well", GravityWellMagic::new);
    public static final DeferredHolder<ManasSkill, KamehamehaMagic> KAMEHAMEHA = SKILLS.register("kamehameha", KamehamehaMagic::new);
    public static final DeferredHolder<ManasSkill, StaticChainMagic> STATIC_CHAIN = SKILLS.register("static_chain", StaticChainMagic::new);

    // ---- extra
    public static final DeferredHolder<ManasSkill, AncientSoulSkill> ANCIENT_SOUL = SKILLS.register("ancient_soul", AncientSoulSkill::new);
    public static final DeferredHolder<ManasSkill, IronResolveSkill> IRON_RESOLVE = SKILLS.register("iron_resolve", IronResolveSkill::new);
    public static final DeferredHolder<ManasSkill, MeditationSkill> MEDITATION = SKILLS.register("meditation", MeditationSkill::new);
    public static final DeferredHolder<ManasSkill, RunesmithsInsightSkill> RUNESMITHS_INSIGHT = SKILLS.register("runesmiths_insight", RunesmithsInsightSkill::new);
    public static final DeferredHolder<ManasSkill, ScavengerSkill> SCAVENGER = SKILLS.register("scavenger", ScavengerSkill::new);

    // ---- intrinsic
    public static final DeferredHolder<ManasSkill, KaiokenSkill> KAIOKEN = SKILLS.register("kaioken", KaiokenSkill::new);
    public static final DeferredHolder<ManasSkill, MechanicalEyeSkill> MECHANICAL_EYE = SKILLS.register("mechanical_eye", MechanicalEyeSkill::new);
    public static final DeferredHolder<ManasSkill, MechanicalHandsSkill> MECHANICAL_HANDS = SKILLS.register("mechanical_hands", MechanicalHandsSkill::new);

    // ---- unique
    public static final DeferredHolder<ManasSkill, CrocodileSkinUnique> CROCODILE_SKIN = SKILLS.register("crocodile_skin", CrocodileSkinUnique::new);
    public static final DeferredHolder<ManasSkill, HecateSkill> HECATE = SKILLS.register("hecate", HecateSkill::new);
    public static final DeferredHolder<ManasSkill, VoidEdgeSkill> VOID_EDGE = SKILLS.register("void_edge", VoidEdgeSkill::new);

    // ---- ultimate
    public static final DeferredHolder<ManasSkill, BasiliskSkinSkill> BASILISK_SKIN = SKILLS.register("basilisk_skin", BasiliskSkinSkill::new);
    public static final DeferredHolder<ManasSkill, BeelzebuthSkill> BEELZEBUTH = SKILLS.register("beelzebuth", BeelzebuthSkill::new);
    public static final DeferredHolder<ManasSkill, GaiaSkill> GAIA = SKILLS.register("gaia", GaiaSkill::new);
    /** The Ultimate Hephaestus; the Unique {@code nusmp:hephaestus} of the DanMachi gods is untouched. */
    public static final DeferredHolder<ManasSkill, HephaestusSkill> HEPHAESTUS_ULTIMATE = SKILLS.register("hephaestus_ultimate", HephaestusSkill::new);
    public static final DeferredHolder<ManasSkill, OuroborosSkill> OUROBOROS = SKILLS.register("ouroboros", OuroborosSkill::new);
    public static final DeferredHolder<ManasSkill, RaphealSkill> RAPHEAL = SKILLS.register("rapheal", RaphealSkill::new);
    public static final DeferredHolder<ManasSkill, VoidSovereignSkill> VOID_SOVEREIGN = SKILLS.register("void_sovereign", VoidSovereignSkill::new);

    /** Every elite skill, in the order of the inventory. */
    public static final List<DeferredHolder<ManasSkill, ? extends ManasSkill>> ALL = List.of(GRAVITY_WELL, KAMEHAMEHA, STATIC_CHAIN, ANCIENT_SOUL, IRON_RESOLVE, MEDITATION,
            RUNESMITHS_INSIGHT, SCAVENGER, KAIOKEN, MECHANICAL_EYE, MECHANICAL_HANDS, CROCODILE_SKIN, HECATE, VOID_EDGE, BASILISK_SKIN, BEELZEBUTH, GAIA, HEPHAESTUS_ULTIMATE,
            OUROBOROS, RAPHEAL, VOID_SOVEREIGN);

    private static boolean started;

    /** The one wiring line, from {@code EliteSystem}: registers the skills, the config, the handlers and the self-tests (once). */
    public static void init(IEventBus modBus, ModContainer container) {
        if (started) return;
        started = true;
        SKILLS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, EliteSkillsConfig.SPEC, "nusmp-elite-skills-server.toml");
        ResetLedger.install();
        IEventBus game = NeoForge.EVENT_BUS;
        EliteSkillEvents.register(game);
        EvolutionRuntime.register(game);
        EliteSkillsSelfTests.register();
    }
}
