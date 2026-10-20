package com.newuniverse.nusmp.audio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: the owner's rule is that dungeon mobs must not play the generic vanilla zombie / skeleton sounds; they get a bespoke voice drawn from the fantasy tropes the players
 * know (goblins cackle in a creepy, evil, high-pitched laugh; a lich speaks in echoing, spectral whispers; a dragon rumbles; a slime squelches). This class is the CATALOGUE,
 * pure data and pure rules with no Minecraft type so JUnit can pin it: the families (25), the eight cues each ({@link Cue}), the pitch range and volume of every cue, the event ids
 * ({@code mob.<family>.<cue>}), and the rules that choose a family: for a Foundry mob (from its faction and the words of its id), for a vanilla or modded entity type, for a boss
 * look, and for the vanilla voice sounds that the remap ({@link MobSoundRemap}) turns into a family's cue when a vanilla mob stands in the dungeon. {@link ModSounds} registers one
 * {@code SoundEvent} per (family, cue) from this table; {@code tools/gen_mob_sounds.py} writes the .ogg files and the entries of {@code sounds.json}; {@code SoundsJsonTest} keeps the
 * three in step. To add a family or a cue see {@code docs/audio/mob_audio.md}.
 */
public final class MobSoundSet {
    private MobSoundSet() {}

    /** The family an unknown mob gets: a plain gruff humanoid. */
    public static final String DEFAULT_FAMILY = "orc";

    /**
     * One family of mobs and how it sounds. {@code pitch} is the usual playback pitch of its sounds and {@code spread} how far a random pitch may stray from it (feet and pain
     * stray further, a death less); {@code volume} scales every cue of the family.
     */
    public record Family(String id, String label, String trope, float volume, float pitch, float spread) {
        public String event(Cue cue) { return eventId(id, cue); }

        public float pitchMin(Cue cue) { return clampPitch(pitch - spread * widen(cue)); }

        public float pitchMax(Cue cue) { return clampPitch(pitch + spread * widen(cue)); }

        /** The volume of a cue: the cue's own loudness (idle sounds and steps are quieter) times the family's. */
        public float volume(Cue cue) { return volume * cueVolume(cue); }
    }

    private static float widen(Cue cue) {
        return switch (cue) {
            case STEP -> 2.0f;
            case HURT -> 1.3f;
            case DEATH -> 0.6f;
            default -> 1.0f;
        };
    }

    private static float cueVolume(Cue cue) {
        return switch (cue) {
            case AMBIENT -> 0.7f;
            case STEP -> 0.5f;
            case CAST -> 0.9f;
            default -> 1.0f;
        };
    }

    private static float clampPitch(float p) { return Math.max(0.5f, Math.min(2.0f, p)); }

    private static final Map<String, Family> FAMILIES = new LinkedHashMap<>();

    private static void family(String id, String label, String trope, float volume, float pitch, float spread) {
        FAMILIES.put(id, new Family(id, label, trope, volume, pitch, spread));
    }

    static {
        family("goblin", "Goblin", "a creepy, evil, high-pitched laugh and giggling mutters", 0.9f, 1.0f, 0.06f);
        family("orc", "Orc", "gruff barks, bellows and a guffawing laugh", 1.0f, 1.0f, 0.05f);
        family("kobold", "Kobold", "yaps, hisses and a yapping laugh", 0.9f, 1.0f, 0.06f);
        family("skeleton", "Skeleton", "bone rattles, a chattering jaw and a clacking laugh", 0.95f, 1.0f, 0.05f);
        family("ghoul", "Ghoul", "wet groans, rasping moans and hungry chuckles", 1.0f, 1.0f, 0.05f);
        family("lich", "Lich", "echoing, spectral whispers and a whispered laugh", 1.0f, 1.0f, 0.04f);
        family("wraith", "Wraith", "airy moans and wails", 1.0f, 1.0f, 0.05f);
        family("armor", "Living armour", "metal clanks, creaks and a hollow helm-laugh", 1.0f, 1.0f, 0.05f);
        family("golem", "Golem", "stone grinding, cracks and heavy thuds", 1.1f, 1.0f, 0.04f);
        family("dragon", "Dragon", "deep rumbling roars and a low chuckle", 1.2f, 1.0f, 0.04f);
        family("drake", "Drake", "screeching hisses and a rasping cackle", 1.0f, 1.0f, 0.05f);
        family("slime", "Slime", "squelches, bubbles and giggling blips", 0.9f, 1.0f, 0.06f);
        family("demon", "Demon", "infernal growls and a slow, dark laugh", 1.0f, 1.0f, 0.04f);
        family("imp", "Imp", "raspy, gibbering cackles", 0.9f, 1.0f, 0.06f);
        family("harpy", "Harpy", "shrill screeches and a shrieking laugh", 1.0f, 1.0f, 0.06f);
        family("troll", "Troll", "heavy grumbles, smashes and guffaws", 1.0f, 1.0f, 0.05f);
        family("ogre", "Ogre", "bellows, pounding and a hearty laugh", 1.05f, 1.0f, 0.05f);
        family("beast", "Beast", "growls, snarls, yelps and howls", 1.0f, 1.0f, 0.06f);
        family("insect", "Insect", "chitters, clicks and buzzing", 0.9f, 1.0f, 0.06f);
        family("elemental", "Elemental", "crackling fire, surges and a crackling laugh", 1.0f, 1.0f, 0.05f);
        family("aquatic", "Drowned", "gurgles, bubbling croaks and a wet laugh", 0.95f, 1.0f, 0.05f);
        family("cultist", "Cultist", "hooded chanting and mad laughter", 1.0f, 1.0f, 0.05f);
        family("vampire", "Vampire", "silky murmurs, hisses and a soft chuckle", 0.95f, 1.0f, 0.04f);
        family("celestial", "Celestial", "choirs, bell-like chimes and a ringing laugh", 1.0f, 1.0f, 0.04f);
        family("npc", "Villager", "friendly hums and short exclamations", 0.9f, 1.0f, 0.05f);
    }

    /** Every family, in the order of the catalogue. */
    public static List<Family> families() { return List.copyOf(FAMILIES.values()); }

    public static Set<String> ids() { return java.util.Collections.unmodifiableSet(FAMILIES.keySet()); }

    public static boolean has(String id) { return id != null && FAMILIES.containsKey(id); }

    /** The family with this id, or the default one when there is none. */
    public static Family family(String id) { return FAMILIES.getOrDefault(id == null ? "" : id, FAMILIES.get(DEFAULT_FAMILY)); }

    /** The sound event path of a cue of a family: {@code mob.goblin.taunt}. */
    public static String eventId(String family, Cue cue) { return "mob." + family + "." + cue.id(); }

    /** The subtitle key of a cue of a family (the text is in {@code tools/lang/mob_audio.json}). */
    public static String subtitleKey(String family, Cue cue) { return "subtitles.nusmp.mob." + family + "." + cue.id(); }

    /** A random pitch for a cue, {@code r01} being a random number in [0, 1). */
    public static float pitch(String family, Cue cue, double r01) {
        Family f = family(family);
        float lo = f.pitchMin(cue), hi = f.pitchMax(cue);
        return (float) (lo + (hi - lo) * Math.max(0.0, Math.min(1.0, r01)));
    }

    /**
     * The pitch factor of a creature's size: a big one (the SCALE attribute above 1) sounds lower, a small one higher, by the 0.4th power of the scale (scale 2 -> 0.76, scale 0.7 -> 1.15),
     * for scales between 0.6 and 2.5.
     */
    public static float sizeShift(float scale) {
        double s = Math.max(0.6, Math.min(2.5, Double.isNaN(scale) ? 1.0 : scale));
        return (float) Math.pow(s, -0.4);
    }

    // ================================================================================================================================ choosing a family
    private static final Map<String, String> FACTION = Map.ofEntries(
            Map.entry("undead", "ghoul"), Map.entry("cultist", "cultist"), Map.entry("beast", "beast"), Map.entry("demon", "demon"), Map.entry("slime", "slime"),
            Map.entry("construct", "armor"), Map.entry("insect", "insect"), Map.entry("ogre", "ogre"), Map.entry("elemental", "elemental"), Map.entry("aquatic", "aquatic"),
            Map.entry("goblin", "goblin"));

    /** The word of a mob's id or name -> the family it names. The first word of an id that is in the table wins ("bone_archer_of_the_barrow" is a skeleton). */
    private static final Map<String, String> WORDS = new LinkedHashMap<>();

    private static void words(String family, String... ws) {
        for (String w : ws) WORDS.putIfAbsent(w, family);
    }

    static {
        words("kobold", "kobold");
        words("goblin", "goblin", "gremlin");
        words("orc", "orc", "hobgoblin", "uruk", "bugbear", "orcish");
        words("skeleton", "skeleton", "bone", "skull", "rattler", "skeletal");
        words("ghoul", "ghoul", "zombie", "mummy", "husk", "corpse", "revenant", "wight", "ghast", "undead");
        words("lich", "lich", "necromancer", "acererak", "vecna");
        words("wraith", "wraith", "ghost", "banshee", "specter", "spectre", "phantom", "shade", "poltergeist", "spirit", "beholder");
        words("armor", "armor", "armour", "knight", "sentinel", "helm", "helmet");
        words("golem", "golem", "gargoyle", "statue", "colossus", "automaton");
        words("dragon", "dragon", "wyrm", "tarrasque", "bahamut", "tiamat", "dracolich");
        words("drake", "drake", "wyvern", "wyrmling", "basilisk", "lizard", "lizardfolk");
        words("slime", "slime", "ooze", "pudding", "jelly", "mimic");
        words("demon", "demon", "devil", "balor", "fiend", "archdevil", "succubus", "incubus", "hellhound", "asmodeus", "orcus", "demogorgon", "zariel");
        words("imp", "imp", "quasit", "homunculus", "sprite", "pixie", "bat");
        words("harpy", "harpy", "siren", "hawk", "owl", "sparrow", "sparrowhawk", "crow", "roc", "thunderbird", "griffon", "griffin", "wren", "raven", "eagle", "phoenix", "falcon", "vulture");
        words("troll", "troll");
        words("ogre", "ogre", "giant", "ettin", "cyclops", "minotaur");
        words("beast", "wolf", "hound", "dog", "bear", "lion", "boar", "beast", "werewolf", "direwolf", "panther", "tiger", "stag", "hart", "wolfhound", "fox", "cat");
        words("insect", "spider", "ant", "beetle", "wasp", "insect", "scorpion", "centipede", "mantis", "fly", "swarm", "lolth", "moth", "bee");
        words("elemental", "elemental", "salamander", "wisp", "flame", "ember", "fire", "ifrit", "djinn", "genie");
        words("aquatic", "kraken", "aboleth", "drowned", "merfolk", "sahuagin", "naga", "mermaid", "siren", "leviathan", "frog", "toad", "fish", "crab", "eel", "shark");
        words("cultist", "cultist", "priest", "acolyte", "witch", "shaman", "warlock", "cleric");
        words("vampire", "vampire", "strahd", "nosferatu", "assassin");
        words("wraith", "reaper", "shinigami", "soul");
        words("celestial", "angel", "archangel", "solar", "seraph", "aasimar", "deva", "planetar", "unicorn");
        words("npc", "villager", "merchant", "innkeeper", "shopkeeper", "peasant", "bard", "healer", "cook", "scribe", "archivist", "keeper", "chancellor", "sage", "trainee", "student", "novice", "page",
                "cantor", "chorister", "piper", "jester", "runner", "wayfarer", "ensign", "tavern", "nun", "elf", "sniper", "marksman");
        words("orc", "monk", "brawler", "abbot", "swordsman", "duelist", "bladewarden", "captain", "marshal", "lieutenant", "commander", "cavalier", "scout", "slayer", "sellsword", "soldier", "warrior",
                "gladiator", "champion", "hunter");
    }

    /** The family named by the first known word of {@code text} (split on anything that is not a letter or a digit), or null. */
    public static String forName(String text) {
        if (text == null) return null;
        for (String w : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            String f = WORDS.get(w);
            if (f != null) return f;
        }
        return null;
    }

    /**
     * The family of a Foundry mob: the first known word of its id decides ("kobold_trapsmith", "bone_archer_of_the_barrow"), else its faction ({@code undead} -> ghoul,
     * {@code construct} -> armour, {@code cultist} -> cultist, ...), else the default family.
     */
    public static String forFoundry(String faction, String id) {
        String byName = forName(id);
        if (byName != null) return byName;
        String byFaction = faction == null ? null : FACTION.get(faction);
        return byFaction != null ? byFaction : DEFAULT_FAMILY;
    }

    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("minecraft:zombie", "ghoul"), Map.entry("minecraft:husk", "ghoul"), Map.entry("minecraft:zombie_villager", "ghoul"), Map.entry("minecraft:zombified_piglin", "ghoul"),
            Map.entry("minecraft:drowned", "aquatic"), Map.entry("minecraft:guardian", "aquatic"), Map.entry("minecraft:elder_guardian", "aquatic"),
            Map.entry("minecraft:skeleton", "skeleton"), Map.entry("minecraft:stray", "skeleton"), Map.entry("minecraft:wither_skeleton", "skeleton"), Map.entry("minecraft:bogged", "skeleton"),
            Map.entry("minecraft:spider", "insect"), Map.entry("minecraft:cave_spider", "insect"), Map.entry("minecraft:silverfish", "insect"), Map.entry("minecraft:endermite", "insect"),
            Map.entry("minecraft:slime", "slime"), Map.entry("minecraft:magma_cube", "slime"),
            Map.entry("minecraft:blaze", "elemental"), Map.entry("minecraft:ghast", "wraith"), Map.entry("minecraft:phantom", "wraith"), Map.entry("minecraft:vex", "imp"),
            Map.entry("minecraft:pillager", "orc"), Map.entry("minecraft:vindicator", "orc"), Map.entry("minecraft:piglin", "orc"), Map.entry("minecraft:piglin_brute", "orc"),
            Map.entry("minecraft:evoker", "cultist"), Map.entry("minecraft:illusioner", "cultist"), Map.entry("minecraft:witch", "cultist"),
            Map.entry("minecraft:ravager", "ogre"), Map.entry("minecraft:hoglin", "beast"), Map.entry("minecraft:zoglin", "beast"), Map.entry("minecraft:wolf", "beast"),
            Map.entry("minecraft:iron_golem", "golem"), Map.entry("minecraft:villager", "npc"), Map.entry("minecraft:wandering_trader", "npc"),
            Map.entry("nusmp:dungeon_slime", "slime"), Map.entry("nusmp:living_armor", "armor"), Map.entry("nusmp:dungeon_direwolf", "beast"), Map.entry("nusmp:basilisk", "drake"),
            Map.entry("nusmp:floor_warden", "armor"), Map.entry("nusmp:labyrinth_guardian", "armor"));

    /** The family of an entity type id such as {@code minecraft:zombie}, or null when none is known (the Foundry mob, whose family depends on which mob it is, answers null here). */
    public static String forEntityType(String typeId) { return typeId == null ? null : TYPES.get(typeId); }

    /** The family of an entity: a Foundry mob by its faction and id, anything else by its type. Null when unknown. */
    public static String forEntity(String typeId, String faction, String detail) {
        if ("nusmp:foundry_mob".equals(typeId)) return forFoundry(faction, detail);
        return forEntityType(typeId);
    }

    private static final Map<String, String> LOOKS = Map.ofEntries(
            Map.entry("lich", "lich"), Map.entry("dracolich", "dragon"), Map.entry("tarrasque", "dragon"), Map.entry("asmodeus", "demon"), Map.entry("kraken", "aquatic"),
            Map.entry("lich_king", "lich"), Map.entry("lolth", "insect"), Map.entry("orcus", "demon"), Map.entry("vecna", "lich"), Map.entry("solar", "celestial"),
            Map.entry("beholder", "wraith"), Map.entry("zariel", "demon"), Map.entry("acererak", "lich"), Map.entry("elder_brain", "lich"), Map.entry("strahd", "vampire"),
            Map.entry("demogorgon", "demon"), Map.entry("bahamut", "dragon"), Map.entry("tiamat", "dragon"));

    /**
     * The family of a Decennial boss: the explicit {@code sound_set} of its profile if it names a known family, else the family of its look ({@code lich}, {@code kraken}), else the
     * first known word of its name or of the creatures it fuses ({@code Umbral Catacomb Lich-Djinn} is a lich), else by its side (holy: celestial, evil: demon), else living armour.
     */
    public static String forBoss(String explicit, String look, String name, List<String> creatures, String side) {
        if (explicit != null && has(explicit)) return explicit;
        if (look != null && !look.isEmpty()) {
            String l = LOOKS.get(look);
            if (l != null) return l;
            String byWord = forName(look);
            if (byWord != null) return byWord;
        }
        String byName = forName(name);
        if (byName != null) return byName;
        if (creatures != null) {
            for (String c : creatures) {
                String f = forName(c);
                if (f != null) return f;
            }
        }
        if ("evil".equals(side)) return "demon";
        if ("holy".equals(side)) return "celestial";
        return "armor";
    }

    // ================================================================================================================================ the vanilla voices
    /** A vanilla mob voice sound heard in the dungeon: which vanilla mob made it and what it expresses. */
    public record Voice(String mob, Cue cue) {}

    private static final Set<String> VOICE_MOBS = Set.of("zombie", "zombie_villager", "husk", "drowned", "skeleton", "stray", "wither_skeleton", "bogged", "spider", "cave_spider", "silverfish",
            "endermite", "slime", "magma_cube", "blaze", "ghast", "vex", "phantom", "pillager", "vindicator", "evoker", "illusioner", "witch", "ravager", "piglin", "piglin_brute", "zombified_piglin",
            "hoglin", "zoglin", "wolf", "iron_golem", "guardian", "elder_guardian");

    private static final Map<String, Cue> VOICE_CUES = Map.ofEntries(
            Map.entry("ambient", Cue.AMBIENT), Map.entry("hurt", Cue.HURT), Map.entry("hurt_on_fire", Cue.HURT), Map.entry("hurt_drown", Cue.HURT), Map.entry("hurt_freeze", Cue.HURT),
            Map.entry("hurt_baby", Cue.HURT), Map.entry("hurt_land", Cue.HURT), Map.entry("hit", Cue.HURT), Map.entry("damage", Cue.HURT), Map.entry("hurt_water", Cue.HURT),
            Map.entry("death", Cue.DEATH), Map.entry("death_small", Cue.DEATH), Map.entry("death_baby", Cue.DEATH), Map.entry("death_water", Cue.DEATH),
            Map.entry("step", Cue.STEP), Map.entry("step_baby", Cue.STEP), Map.entry("squish", Cue.STEP), Map.entry("squish_small", Cue.STEP), Map.entry("jump", Cue.STEP),
            Map.entry("angry", Cue.AGGRO), Map.entry("growl", Cue.AGGRO), Map.entry("roar", Cue.AGGRO), Map.entry("ambient_water", Cue.AMBIENT),
            Map.entry("attack", Cue.ATTACK), Map.entry("attack_strong", Cue.ATTACK), Map.entry("bite", Cue.ATTACK), Map.entry("swoop", Cue.ATTACK),
            Map.entry("celebrate", Cue.TAUNT), Map.entry("howl", Cue.TAUNT), Map.entry("curse", Cue.TAUNT), Map.entry("converted", Cue.CAST),
            Map.entry("cast_spell", Cue.CAST), Map.entry("prepare_attack", Cue.CAST), Map.entry("prepare_summon", Cue.CAST), Map.entry("prepare_wololo", Cue.CAST), Map.entry("charge", Cue.CAST));

    /**
     * Reads a vanilla sound path ({@code entity.zombie.ambient}, {@code entity.wolf.howl}, {@code entity.evoker.cast_spell}) as the voice of one of the stock hostile mobs, or null when
     * it is not one (a bow's twang, a door, an item, a mob that is not in the list).
     */
    public static Voice parseVanillaVoice(String path) {
        if (path == null || !path.startsWith("entity.")) return null;
        String rest = path.substring("entity.".length());
        int dot = rest.indexOf('.');
        if (dot <= 0) return null;
        String mob = rest.substring(0, dot);
        if (!VOICE_MOBS.contains(mob)) return null;
        Cue cue = VOICE_CUES.get(rest.substring(dot + 1));
        return cue == null ? null : new Voice(mob, cue);
    }

    /** Every event id of the catalogue (family x cue), in order: what {@link ModSounds} registers and what {@code sounds.json} must hold. */
    public static List<String> allEventIds() {
        List<String> out = new ArrayList<>();
        for (Family f : FAMILIES.values()) for (Cue c : Cue.values()) out.add(f.event(c));
        return out;
    }
}
