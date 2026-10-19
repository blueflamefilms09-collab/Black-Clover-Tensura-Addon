package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every word the puzzles say is a lang key with an English fallback in code ({@code Component.translatableWithFallback}), so the messages read fine before the lang fragment
 * ({@code tools/lang/puzzles_v2.json}, merged into {@code en_us.json} by the lead) is merged. This is the one place the English lives: the world layer asks {@link #resolve} for the key of
 * a rule's line and {@link #en} for its text, the JUnit test demands that every line a rule can say exists here, and {@link #json} writes the flat fragment so the file and the code can never
 * drift apart. Keys are relative to {@code message.nusmp.mega.puzzle.}: {@code <puzzle>.name}, {@code <puzzle>.hint}, {@code <puzzle>.<line>}, {@code common.<line>}, {@code theme.<theme id>}
 * (the per-theme flavour, with the puzzle's name as its argument), {@code riddle.<id>.q / a0..a2}, {@code item.<token>.q}, {@code colour.<name>}, {@code compound.<n>}. Pure.
 */
public final class PuzzleText {
    private PuzzleText() {}

    public static final String PREFIX = "message.nusmp.mega.puzzle.";
    private static final Map<String, String> EN = new LinkedHashMap<>();

    private static void p(String key, String text) { EN.put(key, text); }

    static {
        // ---------------------------------------------------------------- names and hints
        p("plate_sequence.name", "Tread of Colours");
        p("plate_sequence.hint", "Read the coloured tiles on the wall side, then step on the plates of the same colours in that order. A wrong plate puts the lights out.");
        p("rotating_pillars.name", "The Turning Stones");
        p("rotating_pillars.hint", "Right-click the stones to turn them until every one faces the arrow on the compass stone. Meshing gears drag a neighbour round too.");
        p("beam_mirrors.name", "The Beam and the Crystal");
        p("beam_mirrors.hint", "A beam leaves the golden block heading east. Right-click the mirrors to turn them until the beam lights the red crystal.");
        p("weight_scales.name", "The Scales of the Deep");
        p("weight_scales.hint", "The height of a stone is its weight. Right-click a stone to put it on the blue pan, on the red pan, or off the scale. Use every stone and make the pans balance.");
        p("simon_levers.name", "The Echoing Levers");
        p("simon_levers.hint", "Watch and listen to the levers, then press them in the same order. Each round adds one more.");
        p("memory_tiles.name", "The Floor of Pairs");
        p("memory_tiles.hint", "Step on a tile to see its symbol, then find its twin. Matching tiles stay lit. Too many mistakes and the floor shuffles itself.");
        p("colour_mix.name", "The Painter's Pillars");
        p("colour_mix.hint", "Each lamp mixes its two pillars: red and blue make purple, red and yellow orange, blue and yellow green. Make every lamp match the colour above it. Right-click a pillar to change its colour.");
        p("lights_out.name", "The Grid of Lights");
        p("lights_out.hint", "Right-click a tile to flip it and the four tiles beside it. Light every tile.");
        p("sliding_block.name", "The Sliding Floor");
        p("sliding_block.hint", "Right-click a tile in the hole's row or column to slide the line toward the hole. Put the numbers in order, like the picture beside the grid.");
        p("rune_copy.name", "The Copyist's Stones");
        p("rune_copy.hint", "Copy the master runes onto your stones: right-click a stone to change its rune (crouch to go back), then touch the seal stone. Deeper down the master fades; the oracle stone shows it again.");
        p("riddle_door.name", "The Sphinx's Door");
        p("riddle_door.hint", "Answer the sphinx's riddles by clicking an answer in chat. A wrong answer costs you and brings a different riddle. Right-click the sphinx to hear the riddle again.");
        p("riddle_offering.name", "The Altars of Riddles");
        p("riddle_offering.hint", "Each altar speaks a riddle whose answer is a thing. Take that thing from the matching bowl (right-click it) and offer it to the altar with a right click.");
        p("sundial.name", "The Sundial Without a Sun");
        p("sundial.hint", "A shadow creeps round the ring of hours. Right-click the gnomon in the moment the shadow lies on the gold stone.");
        p("water_level.name", "The Measuring Tanks");
        p("water_level.hint", "Click a tank to pick it, then the spring to fill it, the drain to empty it, or another tank to pour into it. Bring exactly as much water as the glass column shows into one tank.");
        p("torch_order.name", "The Braziers of Order");
        p("torch_order.hint", "Right-click the lectern: it recites which flame must wake before which. Light the braziers in an order that keeps every rule, or they all go out.");
        p("gravity_maze.name", "The Tilting Hall");
        p("gravity_maze.hint", "Tip the room with the arrow pillars. The lit marble slides until a wall stops it. Drop it into the gold hole.");
        p("shadow_cast.name", "The Shadow Wall");
        p("shadow_cast.hint", "Right-click the pillars to turn them until the shadow row on the wall matches the wanted row.");
        p("melody_notes.name", "The Silent Choir");
        p("melody_notes.hint", "Strike the bell to hear the tune. Play it back on the note pillars (light is low, dark is high), then touch the stave stone.");
        p("pipe_connect.name", "The Pipe Floor");
        p("pipe_connect.hint", "Right-click a pipe tile to turn it. Join the golden inlet to the drain on the far side; wet pipes show where the water goes.");
        p("key_and_lock.name", "The Chain of Keys");
        p("key_and_lock.hint", "Take a key from its pedestal (right-click) and carry it to the lock of the same colour. Opening a lock frees the next key.");
        p("escort_spirit.name", "The Lost Spirit");
        p("escort_spirit.hint", "Stand on the tile ahead of the pale spirit to light its way. Keep ahead of the creeping shade, and stand on the shade's tile to push it back.");
        p("sentry_stealth.name", "The Watching Statues");
        p("sentry_stealth.hint", "Cross the lanes to the far row. A lane is watched while its statue and tiles glow red: wait until it looks away. Amber is a warning.");
        p("pendulum_crossing.name", "The Swinging Blades");
        p("pendulum_crossing.hint", "Cross the lanes to the far row. A blade sweeps each lane: red is where it is, amber where it goes next. Step where it is not.");
        p("pit_jump.name", "The Coloured Floor");
        p("pit_jump.hint", "The column of colours beside the field names the safe colour of each row. Step only on tiles of that colour, from one end of the field to the other; any other tile gives way.");
        p("number_lock.name", "The Code Lock");
        p("number_lock.hint", "Right-click the plaques scattered about the room: each states a fact about the code. Set the dials (crouch to turn back) and touch the lock stone.");
        p("constellation.name", "The Star Thread");
        p("constellation.hint", "Right-click the stars to draw one closed figure through all of them without ever crossing your own thread. Click the last star again to take a thread back, and close the figure on the first star.");
        p("mimic_chests.name", "The Chests That Lie");
        p("mimic_chests.hint", "Exactly one inscription tells the truth. Right-click a chest to read its inscription, and open it with a crouching right click. Only one chest is real.");
        p("elemental_mix.name", "The Elemental Pedestals");
        p("elemental_mix.hint", "Take shards from the four crystals. Each pedestal wants a compound of two elements (steam is fire and water ...). Offer two shards with a right click; crouch to empty a pedestal.");
        p("chain_reaction.name", "The Chain of Crystals");
        p("chain_reaction.hint", "A right click adds a charge to a crystal. A crystal with as much charge as it has neighbours bursts and charges them in turn. Burst the golden core crystal before the clicks run out.");

        // ---------------------------------------------------------------- the lines the puzzles say
        p("plate_sequence.wrong", "A wrong plate: the lights go out.");
        p("common.fade", "The pattern fades from the floor.");
        p("simon_levers.watch", "Watch first.");
        p("simon_levers.again", "The levers sound again.");
        p("simon_levers.round", "Round %s.");
        p("simon_levers.wrong", "A wrong lever. Listen again.");
        p("memory_tiles.shuffle", "Too many mistakes: the floor shuffles itself.");
        p("sliding_block.stuck", "Only a tile in the hole's row or column can slide.");
        p("rune_copy.close", "%s of %s runes are right.");
        p("rune_copy.rest", "The oracle is resting.");
        p("rune_copy.mismatch", "The seal rejects the copy.");
        p("riddle_door.right", "Right. %s answered.");
        p("riddle_door.wrong", "Wrong. The sphinx asks another.");
        p("riddle_door.ask", "Riddle %s:");
        p("riddle_offering.refused", "The altar refuses it.");
        p("sundial.miss", "Too soon or too late.");
        p("water_level.pickFirst", "Pick a tank first.");
        p("torch_order.recite", "The lectern recites %s rules of the flames:");
        p("torch_order.rule", "The %s flame must wake before the %s one.");
        p("torch_order.gutter", "A flame woke out of turn: every brazier goes out.");
        p("melody_notes.offkey", "Off key. The notes fall silent.");
        p("key_and_lock.sealed", "This pedestal is sealed until the lock before it opens.");
        p("key_and_lock.needKey", "The lock wants a key of its colour.");
        p("key_and_lock.bite", "The lock bites: wrong key.");
        p("escort_spirit.caught", "The shade caught the spirit and it flees back to the start.");
        p("sentry_stealth.spotted", "A statue saw you: back to the start row.");
        p("pendulum_crossing.spotted", "The blade caught you: back to the start row.");
        p("pit_jump.pit", "The tile gives way.");
        p("number_lock.dial", "Dial %s shows %s.");
        p("number_lock.digitsRight", "%s of %s digits are in place.");
        p("number_lock.wrongCode", "The lock stays shut.");
        p("number_lock.clue.0", "The digits add up to %s.");
        p("number_lock.clue.1", "Digit %s is %s.");
        p("number_lock.clue.2e", "Digit %s is even.");
        p("number_lock.clue.2o", "Digit %s is odd.");
        p("number_lock.clue.3", "Digit %s is greater than digit %s.");
        p("number_lock.clue.4", "No two digits are alike.");
        p("number_lock.clue.5", "Digit %s is twice digit %s.");
        p("number_lock.clue.6", "Digits %s and %s differ by %s.");
        p("constellation.snap", "The thread snaps where it would cross itself.");
        p("constellation.linked", "That star is already on the thread.");
        p("mimic_chests.inscr.in", "Chest %s says: the treasure is in chest %s.");
        p("mimic_chests.inscr.not", "Chest %s says: the treasure is not in chest %s.");
        p("mimic_chests.mimic", "A mimic! The lid bites.");
        p("mimic_chests.mimic.name", "Mimic");
        p("elemental_mix.wants", "This pedestal wants %s (%s of 2 shards given).");
        p("elemental_mix.backlash", "Wrong pair: the elements backlash.");

        // ---------------------------------------------------------------- common
        p("common.enter", "Puzzle: %s");
        p("common.hintLine", "%s");
        p("common.solved", "The mechanism opens.");
        p("common.solvedTitle", "Solved");
        p("common.flawless", "Not one mistake: the mechanism pays double.");
        p("common.cooling", "The mechanism is cooling down for a moment.");
        p("common.jam", "The mechanism jams from too much fiddling.");
        p("common.restarted", "The mechanism starts over.");
        p("common.tooMany", "Too many mistakes: the mechanism starts over.");
        p("common.progress", "Mistakes so far: %s.");
        p("common.nudge", "Stuck? Right-click the lectern for the hint.");
        p("common.pity", "A faint glow shows where to try next.");
        p("common.bypassWarn", "The mechanism is winding down: it lets you go in %s seconds.");
        p("common.bypass", "The mechanism grinds to a halt. Nothing is locked, but it pays nothing.");
        p("common.dismissAsk", "Crouch and right-click the lectern again to give up on this puzzle.");
        p("common.dismissed", "You give up the puzzle. The way stays open, and it pays nothing.");
        p("common.lectern", "Lectern of the puzzle");
        p("common.stale", "That question has passed.");
        p("common.pickHint", "Click an answer:");
        p("common.pickNone", "There is no question open.");
        p("common.penalty.hurt", "A shock runs through you.");
        p("common.penalty.dark", "The light dims round you.");
        p("common.penalty.drain", "Something drains your strength.");
        p("common.penalty.slow", "Your limbs grow heavy.");
        p("common.penalty.hunger", "A hollow hunger takes you.");
        p("common.penalty.ambush", "Something stirs in the dark.");
        p("common.adminOn", "Puzzle %s started here.");
        p("common.adminNo", "No room for that puzzle here.");

        // ---------------------------------------------------------------- the admin command
        p("cmd.clear", "Ended %s live puzzles.");
        p("cmd.reset", "Cleared and given-up puzzles may start again.");
        p("cmd.list.head", "Puzzle rooms: %s puzzles, at most %s a floor, switched %s.");
        p("cmd.list.line", "  %s (%s): weight %s, tiers %s to %s");
        p("cmd.nosuch", "No such puzzle '%s'.");
        p("cmd.info", "Family %s, %s cells at tier 1 for one delver.");
        p("cmd.player", "Only a player can do that.");
        p("cmd.apply.no", "Cannot start it here: %s.");
        p("cmd.apply.ok", "Started %s here (#%s). Crouch and right-click its lectern twice to give up.");
        p("cmd.status.head", "%s live puzzles.");
        p("cmd.plan.none", "You are not in a sector.");
        p("cmd.plan.head", "Sector %s (theme %s): %s planned.");
        p("cmd.verify.ok", "All %s puzzles pass: solvable by their own hint, palettes and texts complete.");

        // ---------------------------------------------------------------- the per-theme flavour (the argument is the puzzle's name)
        p("theme.default", "An old mechanism wakes as you enter: %s.");
        p("theme.abyss", "Something far below turns its attention to you: %s.");
        p("theme.astral", "The stars left a lesson in the floor: %s.");
        p("theme.blood", "The stone is warm and wet, and it remembers a game: %s.");
        p("theme.celestial_court", "The court sets a trial of wit before judgement: %s.");
        p("theme.crystal_geode", "The crystals hum a test through the rock: %s.");
        p("theme.cursed_archive", "A librarian's ghost slides a card across the table: %s.");
        p("theme.drowned_crypt", "Water drips in time with an old lock: %s.");
        p("theme.dwarven_forgehold", "The smiths built a mechanism here and never took it down: %s.");
        p("theme.feywild_grove", "The fey love a game, and they have made one: %s.");
        p("theme.flame", "Heat shimmers over a device of fire and stone: %s.");
        p("theme.frost", "Frost has crept into the gears but they still turn: %s.");
        p("theme.fungal_underdark", "Pale spores drift over a device someone left to rot: %s.");
        p("theme.gravity", "The floor leans a little, as if the puzzle itself were heavy: %s.");
        p("theme.haunted_manor", "A parlour game from a house that never ended: %s.");
        p("theme.holy_church", "A catechism carved in stone invites you to answer: %s.");
        p("theme.infernal_foundry", "The foundry's overseers set a test for new hands: %s.");
        p("theme.mountain_monastery", "The monks leave a koan in the stone: %s.");
        p("theme.necropolis", "The dead keep score, and a game is laid out for the living: %s.");
        p("theme.radiant", "The light itself is the lock, and you are the key: %s.");
        p("theme.shadow", "The dark offers a trade, and a game: %s.");
        p("theme.sky_citadel", "The winds sing a measure above the clouds: %s.");
        p("theme.spatial", "Space folds round a small puzzle: %s.");
        p("theme.spider_warrens", "Webs hold a mechanism as a spider holds a fly: %s.");
        p("theme.storm", "Thunder counts out the rules: %s.");
        p("theme.sunken_temple", "The tide worked this lock before the temple sank: %s.");
        p("theme.vampire_castle", "An old lord sets a guest a courtesy: %s.");
        p("theme.verdant", "Roots and vines have grown a puzzle round the stone: %s.");

        // ---------------------------------------------------------------- colours and compounds
        String[] colours = {"red", "blue", "green", "yellow", "purple", "orange", "cyan", "white"};
        for (String c : colours) p("colour." + c, c);
        String[] compounds = {"steam", "mud", "lava", "dust", "storm", "lightning"};
        for (int i = 0; i < compounds.length; i++) p("compound." + i, compounds[i]);

        // ---------------------------------------------------------------- the names of the carried tokens
        for (String t : Tokens.ITEMS) p("token." + t, "Offering: " + Character.toUpperCase(t.charAt(0)) + t.substring(1));
        p("token.fire", "Shard of Fire");
        p("token.water", "Shard of Water");
        p("token.earth", "Shard of Earth");
        p("token.air", "Shard of Air");
        p("token.key0", "Red Key");
        p("token.key1", "Blue Key");
        p("token.key2", "Green Key");
        p("token.key3", "Yellow Key");

        // ---------------------------------------------------------------- the choice riddles (a0 is the right answer)
        riddle("echo", "I speak without a mouth and answer when you call. What am I?", "An echo", "A statue", "A shadow");
        riddle("footsteps", "The more of me you take, the more you leave behind. What am I?", "Footsteps", "Gold", "Rope");
        riddle("map", "I have cities but no houses, mountains but no trees, and water but no fish. What am I?", "A map", "A dream", "A mirror");
        riddle("piano", "What has keys but cannot open a lock?", "A piano", "A chest", "A ring");
        riddle("towel", "What gets wetter the more it dries?", "A towel", "A sponge", "A cloud");
        riddle("fire", "Feed me and I live; give me water and I die. What am I?", "Fire", "A dragon", "A river");
        riddle("coffin", "The one who makes me does not need me; the one who buys me does not use me; the one who uses me never knows it. What am I?", "A coffin", "A crown", "A sword");
        riddle("river", "What runs but never walks, has a mouth but never talks, and has a bed but never sleeps?", "A river", "A wolf", "A road");
        riddle("ton", "Forward I am heavy, backward I am not. What am I?", "The word \"ton\"", "An anchor", "A stone");
        riddle("silence", "What breaks the moment you speak its name?", "Silence", "A promise", "A mirror");
        riddle("cloud", "I fly without wings, I cry without eyes, and wherever I go, darkness follows. What am I?", "A cloud", "A bat", "A candle");
        riddle("flame", "I am not alive, but I grow; I have no lungs, but I need air. What am I?", "A flame", "A seed", "A stone");
        riddle("future", "What is always in front of you but can never be seen?", "The future", "A wall", "The air");
        riddle("clock", "What has hands but cannot clap?", "A clock", "A statue", "A ghost");
        riddle("umbrella", "What goes up when the rain comes down?", "An umbrella", "A flood", "Smoke");
        riddle("darkness", "The more of me there is, the less you see. What am I?", "Darkness", "A window", "A lamp");

        // ---------------------------------------------------------------- the item riddles (the answer is a thing)
        item("coal", "Born in the dark of the deep, black as night, I give my warmth when you burn me.");
        item("bone", "The dead leave me behind, and wolves would sit for me.");
        item("gold", "Kings wear me and fools chase me, and I never rust.");
        item("lapis", "Deep blue as the night sky, scholars grind me into ink and dye.");
        item("amethyst", "Purple shards that ring like a bell when struck, grown in a hollow of stone.");
        item("redstone", "A red dust that wakes sleeping wires.");
        item("iron", "I am forged into the blade and the shield, and I rust if left out in the rain.");
        item("quartz", "White and hard, grown in the red lands of fire, I am the stone of the Nether.");
        item("emerald", "Green and bright, I buy bread from villagers.");
        item("diamond", "The hardest gem, found at the deepest layers, bright as ice.");
        item("copper", "An orange metal that turns green as it ages.");
    }

    private static void riddle(String id, String q, String a0, String a1, String a2) {
        p("riddle." + id + ".q", q);
        p("riddle." + id + ".a0", a0);
        p("riddle." + id + ".a1", a1);
        p("riddle." + id + ".a2", a2);
    }

    private static void item(String token, String q) { p("item." + token + ".q", q); }

    // ---------------------------------------------------------------- lookups
    /** The relative key of a line a rule says: "<puzzle>.<key>", else "common.<key>", else the key itself if it is a full relative key, else null. */
    public static String resolve(String puzzleId, String key) {
        if (EN.containsKey(puzzleId + "." + key)) return puzzleId + "." + key;
        if (EN.containsKey("common." + key)) return "common." + key;
        if (EN.containsKey(key)) return key;
        return null;
    }

    /** The English text of a relative key, or null. */
    public static String en(String rel) { return EN.get(rel); }

    /** The full lang key of a relative key. */
    public static String langKey(String rel) { return PREFIX + rel; }

    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }

    /** The flat lang fragment (full key -> English), one entry per line, in a stable order. */
    public static String json() {
        List<String> keys = new ArrayList<>(EN.keySet());
        Collections.sort(keys);
        StringBuilder b = new StringBuilder("{\n");
        for (int i = 0; i < keys.size(); i++) {
            b.append("  \"").append(escape(PREFIX + keys.get(i))).append("\": \"").append(escape(EN.get(keys.get(i)))).append('"').append(i + 1 < keys.size() ? "," : "").append('\n');
        }
        return b.append("}\n").toString();
    }

    private static String escape(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                default -> b.append(c);
            }
        }
        return b.toString();
    }
}
