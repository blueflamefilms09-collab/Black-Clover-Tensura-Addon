package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUSMP;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NUSkills {
    public static final DeferredRegister<ManasSkill> SKILLS =
            DeferredRegister.create(SkillAPI.getSkillRegistryKey(), NUSMP.MODID);

    /** Grant in game with: /tensura edit <player> ability grant nusmp:summoner */
    public static final DeferredHolder<ManasSkill, SummonerSkill> SUMMONER =
            SKILLS.register("summoner", SummonerSkill::new);

    /** Grant in game with: /tensura edit <player> ability grant nusmp:faith */
    public static final DeferredHolder<ManasSkill, FaithSkill> FAITH =
            SKILLS.register("faith", FaithSkill::new);

    // ---- DanMachi (Is It Wrong to Pick Up Girls in a Dungeon?) ----
    public static final DeferredHolder<ManasSkill, FireboltSkill> FIREBOLT =
            SKILLS.register("firebolt", FireboltSkill::new);
    public static final DeferredHolder<ManasSkill, ArgonautSkill> ARGONAUT =
            SKILLS.register("argonaut", ArgonautSkill::new);
    public static final DeferredHolder<ManasSkill, LiarisFreeseSkill> LIARIS_FREESE =
            SKILLS.register("liaris_freese", LiarisFreeseSkill::new);
    public static final DeferredHolder<ManasSkill, AirielSkill> AIRIEL =
            SKILLS.register("airiel", AirielSkill::new);

    // ---- DanMachi gods (Unique) ----
    public static final DeferredHolder<ManasSkill, HestiaSkill> HESTIA = SKILLS.register("hestia", HestiaSkill::new);
    public static final DeferredHolder<ManasSkill, FreyaSkill> FREYA = SKILLS.register("freya", FreyaSkill::new);
    public static final DeferredHolder<ManasSkill, HephaestusSkill> HEPHAESTUS = SKILLS.register("hephaestus", HephaestusSkill::new);
    public static final DeferredHolder<ManasSkill, LokiSkill> LOKI = SKILLS.register("loki", LokiSkill::new);

    // ---- DanMachi chanted magics ----
    public static final DeferredHolder<ManasSkill, WynnFimbulvetrSkill> WYNN_FIMBULVETR = SKILLS.register("wynn_fimbulvetr", WynnFimbulvetrSkill::new);
    public static final DeferredHolder<ManasSkill, ReaLaevateinnSkill> REA_LAEVATEINN = SKILLS.register("rea_laevateinn", ReaLaevateinnSkill::new);
    public static final DeferredHolder<ManasSkill, ArcsRaySkill> ARCS_RAY = SKILLS.register("arcs_ray", ArcsRaySkill::new);
    public static final DeferredHolder<ManasSkill, FutsunomitamaSkill> FUTSUNOMITAMA = SKILLS.register("futsunomitama", FutsunomitamaSkill::new);
    public static final DeferredHolder<ManasSkill, UchideNoKozuchiSkill> UCHIDE_NO_KOZUCHI = SKILLS.register("uchide_no_kozuchi", UchideNoKozuchiSkill::new);

    // ---- v0.4: toggle / held / passive skills ----
    public static final DeferredHolder<ManasSkill, HellWalkerSkill> HELL_WALKER = SKILLS.register("hell_walker", HellWalkerSkill::new);
    public static final DeferredHolder<ManasSkill, HawkEyeSkill> HAWK_EYE = SKILLS.register("hawk_eye", HawkEyeSkill::new);
    public static final DeferredHolder<ManasSkill, SpiritCardSkill> SPIRIT_CARD = SKILLS.register("spirit_card", SpiritCardSkill::new);
    public static final DeferredHolder<ManasSkill, UnbreakableWillSkill> UNBREAKABLE_WILL = SKILLS.register("unbreakable_will", UnbreakableWillSkill::new);
    public static final DeferredHolder<ManasSkill, HatiSkill> HATI = SKILLS.register("hati", HatiSkill::new);

    // ---- Ultimate evolutions (granted automatically on mastery, see SkillEvolution) ----
    public static final DeferredHolder<ManasSkill, LordOfSummonsSkill> LORD_OF_SUMMONS = SKILLS.register("lord_of_summons", LordOfSummonsSkill::new);
    public static final DeferredHolder<ManasSkill, GoddessOfFaithSkill> GODDESS_OF_FAITH = SKILLS.register("goddess_of_faith", GoddessOfFaithSkill::new);
    public static final DeferredHolder<ManasSkill, HerosArgonautSkill> HEROS_ARGONAUT = SKILLS.register("heros_argonaut", HerosArgonautSkill::new);
    public static final DeferredHolder<ManasSkill, HellConquerorSkill> HELL_CONQUEROR = SKILLS.register("hell_conqueror", HellConquerorSkill::new);
    public static final DeferredHolder<ManasSkill, ImmortalWillSkill> IMMORTAL_WILL = SKILLS.register("immortal_will", ImmortalWillSkill::new);

    // ---- Fire Force / Medaka Box / JJK / JoJo / Railgun (Unique -> Ultimate) ----
    public static final DeferredHolder<ManasSkill, DevilsFootprintsSkill> DEVILS_FOOTPRINTS = SKILLS.register("devils_footprints", DevilsFootprintsSkill::new);
    public static final DeferredHolder<ManasSkill, AdollaBurstSkill> ADOLLA_BURST = SKILLS.register("adolla_burst", AdollaBurstSkill::new);
    public static final DeferredHolder<ManasSkill, BookMakerSkill> BOOK_MAKER = SKILLS.register("book_maker", BookMakerSkill::new);
    public static final DeferredHolder<ManasSkill, AllFictionSkill> ALL_FICTION = SKILLS.register("all_fiction", AllFictionSkill::new);
    public static final DeferredHolder<ManasSkill, SixEyesSkill> SIX_EYES = SKILLS.register("six_eyes", SixEyesSkill::new);
    public static final DeferredHolder<ManasSkill, LimitlessSkill> LIMITLESS = SKILLS.register("limitless", LimitlessSkill::new);
    public static final DeferredHolder<ManasSkill, GoldExperienceSkill> GOLD_EXPERIENCE = SKILLS.register("gold_experience", GoldExperienceSkill::new);
    public static final DeferredHolder<ManasSkill, GoldExperienceRequiemSkill> GOLD_EXPERIENCE_REQUIEM = SKILLS.register("gold_experience_requiem", GoldExperienceRequiemSkill::new);
    public static final DeferredHolder<ManasSkill, VectorManipulationSkill> VECTOR_MANIPULATION = SKILLS.register("vector_manipulation", VectorManipulationSkill::new);
    public static final DeferredHolder<ManasSkill, BlackWingsSkill> BLACK_WINGS = SKILLS.register("black_wings", BlackWingsSkill::new);

    // ---- Standalone god-tier Ultimates (admin/event rewards) ----
    public static final DeferredHolder<ManasSkill, InstantDeathSkill> INSTANT_DEATH = SKILLS.register("instant_death", InstantDeathSkill::new);
    public static final DeferredHolder<ManasSkill, TheAlmightySkill> THE_ALMIGHTY = SKILLS.register("the_almighty", TheAlmightySkill::new);
    public static final DeferredHolder<ManasSkill, TheVisionarySkill> THE_VISIONARY = SKILLS.register("the_visionary", TheVisionarySkill::new);

    // ---- Black Clover ----
    /** Granted automatically with a grimoire. Magic, leaves and devil are stored on the skill. */
    public static final DeferredHolder<ManasSkill, GrimoireMagicSkill> GRIMOIRE_MAGIC = SKILLS.register("grimoire_magic", () -> new GrimoireMagicSkill()); // legacy
    /** One skill per magic type (nusmp:grimoire_flame, nusmp:grimoire_water, ...) so each has its own name and icon. */
    public static final java.util.Map<com.newuniverse.nusmp.blackclover.MagicType, DeferredHolder<ManasSkill, GrimoireMagicSkill>> GRIMOIRES = new java.util.EnumMap<>(com.newuniverse.nusmp.blackclover.MagicType.class);
    static {
        for (com.newuniverse.nusmp.blackclover.MagicType m : com.newuniverse.nusmp.blackclover.MagicType.values()) {
            GRIMOIRES.put(m, SKILLS.register("grimoire_" + m.name().toLowerCase(), () -> new GrimoireMagicSkill(m)));
        }
    }

    // ---- 0.61: Riven Remake's exclusive, boss-bound passives (no skill item, no plunder, no page drop) ----
    public static final java.util.Map<String, DeferredHolder<ManasSkill, BossBoundSkill>> RIVEN_PASSIVES = new java.util.LinkedHashMap<>();
    static {
        for (String id : com.newuniverse.nusmp.entity.riven.RivenPassives.EXCLUSIVE) RIVEN_PASSIVES.put(id, SKILLS.register("riven_" + id, BossBoundSkill::new));
    }

    // ---- Grimoire pages: Time ----
    public static final DeferredHolder<ManasSkill, TimePages.ChronoStasis> PAGE_CHRONO_STASIS = SKILLS.register("page_chrono_stasis", () -> new TimePages.ChronoStasis());
    public static final DeferredHolder<ManasSkill, TimePages.ChronoStasisGrigora> PAGE_CHRONO_STASIS_GRIGORA = SKILLS.register("page_chrono_stasis_grigora", () -> new TimePages.ChronoStasisGrigora());
    public static final DeferredHolder<ManasSkill, TimePages.TimeAcceleration> PAGE_TIME_ACCELERATION = SKILLS.register("page_time_acceleration", () -> new TimePages.TimeAcceleration());
    public static final DeferredHolder<ManasSkill, TimePages.TimeReversal> PAGE_TIME_REVERSAL = SKILLS.register("page_time_reversal", () -> new TimePages.TimeReversal());
    public static final DeferredHolder<ManasSkill, TimePages.StolenTime> PAGE_STOLEN_TIME = SKILLS.register("page_stolen_time", () -> new TimePages.StolenTime());

    // ---- Grimoire BOOKS: Tensura UNIQUE skills, one per family; pages are modes ----
    private static DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> book(String id,
            java.util.function.Supplier<com.newuniverse.nusmp.book.GrimoireBook> s) { return SKILLS.register(id, s); }

    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_TIME = book("book_time", com.newuniverse.nusmp.book.TimeBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_FIRE = book("book_fire", com.newuniverse.nusmp.book.FireBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_WATER = book("book_water", com.newuniverse.nusmp.book.WaterBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_WIND = book("book_wind", com.newuniverse.nusmp.book.WindBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_EARTH = book("book_earth", com.newuniverse.nusmp.book.EarthBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_LIGHT = book("book_light", com.newuniverse.nusmp.book.LightBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_DARK = book("book_dark", com.newuniverse.nusmp.book.DarkBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_SPATIAL = book("book_spatial", com.newuniverse.nusmp.book.SpatialBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_LIGHTNING = book("book_lightning", com.newuniverse.nusmp.book.LightningBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_STEEL = book("book_steel", com.newuniverse.nusmp.book.SteelBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_MIRROR = book("book_mirror", com.newuniverse.nusmp.book.MirrorBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_THREAD = book("book_thread", com.newuniverse.nusmp.book.ThreadBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_PLANT = book("book_plant", com.newuniverse.nusmp.book.PlantBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_SEALING = book("book_sealing", com.newuniverse.nusmp.book.SealingBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_GRAVITY = book("book_gravity", com.newuniverse.nusmp.book.GravityBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_ICE = book("book_ice", com.newuniverse.nusmp.book.IceBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_MERCURY = book("book_mercury", com.newuniverse.nusmp.book.MercuryBook::new);

    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_SWORD = book("book_sword", com.newuniverse.nusmp.book.ElementBooks::sword);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_EXPLOSION = book("book_explosion", com.newuniverse.nusmp.book.ElementBooks::explosion);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_MAGMA = book("book_magma", com.newuniverse.nusmp.book.ElementBooks::magma);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_MIST = book("book_mist", com.newuniverse.nusmp.book.ElementBooks::mist);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_STAR = book("book_star", com.newuniverse.nusmp.book.ElementBooks::star);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_STORM = book("book_storm", com.newuniverse.nusmp.book.ElementBooks::storm);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_SAND = book("book_sand", com.newuniverse.nusmp.book.ElementBooks::sand);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_SHADOW = book("book_shadow", com.newuniverse.nusmp.book.ElementBooks::shadow);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_POISON = book("book_poison", com.newuniverse.nusmp.book.ElementBooks::poison);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_REINFORCEMENT = book("book_reinforcement", com.newuniverse.nusmp.book.ElementBooks::reinforcement);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BEAST = book("book_beast", com.newuniverse.nusmp.book.ElementBooks::beast);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BONE = book("book_bone", com.newuniverse.nusmp.book.ElementBooks::bone);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BLOOD = book("book_blood", com.newuniverse.nusmp.book.ElementBooks::blood);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_CREATION = book("book_creation", com.newuniverse.nusmp.book.ElementBooks::creation);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_COPY = book("book_copy", com.newuniverse.nusmp.book.ElementBooks::copyBook);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_ILLUSION = book("book_illusion", com.newuniverse.nusmp.book.ElementBooks::illusion);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_DREAM = book("book_dream", com.newuniverse.nusmp.book.ElementBooks::dream);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_ANTI_MAGIC = book("book_anti_magic", com.newuniverse.nusmp.book.ElementBooks::antiMagic);
    // 0.34: attributes from the wiki
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_TRANSMUTATION = book("book_transmutation", com.newuniverse.nusmp.book.WikiBooks::transmutation);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_ASH = book("book_ash", com.newuniverse.nusmp.book.AshBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_COTTON = book("book_cotton", com.newuniverse.nusmp.book.CottonBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_RECOMBINATION = book("book_recombination", com.newuniverse.nusmp.book.WikiBooks::recombination);
    // 0.41: Painting Magic (Rill Boismortier, Lira)
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_PAINTING = book("book_painting", com.newuniverse.nusmp.book.PaintingBook::create);
    // 0.45: extended attributes
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_WORLD_TREE = book("book_world_tree", com.newuniverse.nusmp.book.WorldTreeBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_DICE = book("book_dice", com.newuniverse.nusmp.book.DiceBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_SLASH = book("book_slash", com.newuniverse.nusmp.book.SlashBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_COMPASS = book("book_compass", com.newuniverse.nusmp.book.CompassBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_GAME = book("book_game", com.newuniverse.nusmp.book.GameBook::new);   // 0.49
    // 0.53: the Black Clover Magic and VFX expansion
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_DEMON_BEAST = book("book_demon_beast", com.newuniverse.nusmp.book.DemonBeastBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BODY = book("book_body", com.newuniverse.nusmp.book.BodyBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_EYE = book("book_eye", com.newuniverse.nusmp.book.EyeBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_EYEBALL = book("book_eyeball", com.newuniverse.nusmp.book.EyeballBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_CURSE = book("book_curse", com.newuniverse.nusmp.book.CurseBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_CURSE_WARDING = book("book_curse_warding", com.newuniverse.nusmp.book.CurseWardingBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_DEMON_FIRE = book("book_demon_fire", com.newuniverse.nusmp.book.DemonFireBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_DEMON_ICE = book("book_demon_ice", com.newuniverse.nusmp.book.DemonIceBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_DEMON_LIGHT = book("book_demon_light", com.newuniverse.nusmp.book.DemonLightBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_DEMON_WATER = book("book_demon_water", com.newuniverse.nusmp.book.DemonWaterBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BARRIER = book("book_barrier", com.newuniverse.nusmp.book.BarrierBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_KEY = book("book_key", com.newuniverse.nusmp.book.KeyBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_CHAIN = book("book_chain", com.newuniverse.nusmp.book.ChainBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BUTOH = book("book_butoh", com.newuniverse.nusmp.book.ButohBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BRIAR = book("book_briar", com.newuniverse.nusmp.book.BriarBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_CHERRY_BLOSSOM = book("book_cherry_blossom", com.newuniverse.nusmp.book.CherryBlossomBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_FUNGUS = book("book_fungus", com.newuniverse.nusmp.book.FungusBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_FOOD = book("book_food", com.newuniverse.nusmp.book.FoodBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_CRYSTAL = book("book_crystal", com.newuniverse.nusmp.book.CrystalBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_CORUNDUM = book("book_corundum", com.newuniverse.nusmp.book.CorundumBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BRONZE = book("book_bronze", com.newuniverse.nusmp.book.BronzeBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_COPPER = book("book_copper", com.newuniverse.nusmp.book.CopperBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_IRON = book("book_iron", com.newuniverse.nusmp.book.IronBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BLACK_OIL = book("book_black_oil", com.newuniverse.nusmp.book.BlackOilBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_GEL = book("book_gel", com.newuniverse.nusmp.book.GelBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_GLASS = book("book_glass", com.newuniverse.nusmp.book.GlassBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_BUBBLE = book("book_bubble", com.newuniverse.nusmp.book.BubbleBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_ICE_WEDGE = book("book_ice_wedge", com.newuniverse.nusmp.book.IceWedgeBook::new);
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_LEGION = book("book_legion", com.newuniverse.nusmp.book.LegionBook::new);
    // 0.47: Kotodama (Word Soul) Magic - creative only
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_KOTODAMA = book("book_kotodama", com.newuniverse.nusmp.book.KotodamaBook::new);
    /** Anti-Magic Spirit Lord: awakened by mastering the Anti-Magic grimoire (or /nusmp grimoire awaken_anti). */
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.antimagic.AntiMagicLordSkill> ANTI_MAGIC_LORD =
            SKILLS.register("anti_magic_spirit_lord", () -> new com.newuniverse.nusmp.antimagic.AntiMagicLordSkill());
    /** Spirit Lord (Unique): the non-grimoire path (multiverse.SpiritLordSkill). */
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.multiverse.SpiritLordSkill> SPIRIT_LORD =
            SKILLS.register("spirit_lord", () -> new com.newuniverse.nusmp.multiverse.SpiritLordSkill());
    /** Forbidden Magic: written by a devil contract (not a grimoire family). */
    public static final DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook> BOOK_FORBIDDEN = book("forbidden_magic", com.newuniverse.nusmp.book.ForbiddenBook::new);

    public static final java.util.List<DeferredHolder<ManasSkill, com.newuniverse.nusmp.book.GrimoireBook>> BOOKS = java.util.List.of(
            BOOK_TIME, BOOK_FIRE, BOOK_WATER, BOOK_WIND, BOOK_EARTH, BOOK_LIGHT, BOOK_DARK, BOOK_SPATIAL, BOOK_LIGHTNING,
            BOOK_STEEL, BOOK_MIRROR, BOOK_THREAD, BOOK_PLANT, BOOK_SEALING, BOOK_GRAVITY, BOOK_ICE, BOOK_MERCURY,
            BOOK_SWORD, BOOK_EXPLOSION, BOOK_MAGMA, BOOK_MIST, BOOK_STAR, BOOK_STORM, BOOK_SAND, BOOK_SHADOW, BOOK_POISON,
            BOOK_REINFORCEMENT, BOOK_BEAST, BOOK_BONE, BOOK_BLOOD, BOOK_CREATION, BOOK_COPY, BOOK_ILLUSION, BOOK_DREAM, BOOK_ANTI_MAGIC,
            BOOK_TRANSMUTATION, BOOK_ASH, BOOK_COTTON, BOOK_RECOMBINATION, BOOK_PAINTING,
            BOOK_WORLD_TREE, BOOK_DICE, BOOK_SLASH, BOOK_COMPASS, BOOK_KOTODAMA, BOOK_GAME,
            BOOK_DEMON_BEAST, BOOK_BODY, BOOK_EYE, BOOK_EYEBALL, BOOK_CURSE, BOOK_CURSE_WARDING,
            BOOK_DEMON_FIRE, BOOK_DEMON_ICE, BOOK_DEMON_LIGHT, BOOK_DEMON_WATER, BOOK_BARRIER, BOOK_KEY,
            BOOK_CHAIN, BOOK_BUTOH, BOOK_BRIAR, BOOK_CHERRY_BLOSSOM, BOOK_FUNGUS, BOOK_FOOD,
            BOOK_CRYSTAL, BOOK_CORUNDUM, BOOK_BRONZE, BOOK_COPPER, BOOK_IRON, BOOK_BLACK_OIL,
            BOOK_GEL, BOOK_GLASS, BOOK_BUBBLE, BOOK_ICE_WEDGE, BOOK_LEGION);

    /** The Unique book for a magic type, or null if that family isn't ported yet. */
    public static com.newuniverse.nusmp.book.GrimoireBook bookFor(com.newuniverse.nusmp.blackclover.MagicType m) {
        for (var h : BOOKS) if (h.get().magic == m) return h.get();
        return null;
    }

    /** What a new grimoire teaches: the Unique book if ported, else the legacy skill. Never both. */
    public static ManasSkill grimoireSkillFor(com.newuniverse.nusmp.blackclover.MagicType m) {
        var book = bookFor(m);
        return book != null ? book : grimoireSkill(m);
    }

    public static GrimoireMagicSkill grimoireSkill(com.newuniverse.nusmp.blackclover.MagicType m) { return GRIMOIRES.get(m).get(); }

    /** Every grimoire skill id (legacy + per-magic), used to find or remove a player's grimoire magic. */
    public static java.util.List<net.minecraft.resources.ResourceLocation> allGrimoireSkillIds() {
        java.util.List<net.minecraft.resources.ResourceLocation> ids = new java.util.ArrayList<>();
        for (var h : BOOKS) ids.add(h.getId());
        ids.add(GRIMOIRE_MAGIC.getId());
        for (var h : GRIMOIRES.values()) ids.add(h.getId());
        return ids;
    }

    private NUSkills() {}
}
