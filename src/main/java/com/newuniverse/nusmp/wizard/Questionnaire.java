package com.newuniverse.nusmp.wizard;

import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.multiverse.Race;
import com.newuniverse.nusmp.multiverse.SocialClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Tower Wizard's questionnaire. Nine questions are drawn at random from the pool (three about your nature, two about your hearth,
 * three about your family's story, one about your resolve) and their answers come back in a random order. Nothing in it tells you
 * what an answer will do: every option quietly adds points to a magic attribute, a kingdom, a Tensura race's blood or your resolve,
 * and the totals decide:
 * <ul>
 *   <li>the magic attribute: any of them (see {@link MagicFlavors}); when two natures are nearly tied they fuse into one grimoire,</li>
 *   <li>the kingdom you hail from (its grimoire suit, a buff and a debuff),</li>
 *   <li>the blood of a Tensura race mixed into yours (a buff and a debuff),</li>
 *   <li>how far your resolve takes the grimoire (3, 4 or 5 leaves; the five-leaf cover still needs the forbidden-start rule).</li>
 * </ul>
 * Option strings read "text|TOKEN:points,TOKEN:points": tokens are F:&lt;flavor&gt; (the nature questions), E:&lt;magic&gt; (a magic by name, old sheets), K:&lt;kingdom&gt;, R:&lt;race&gt;, S:&lt;class&gt;, T (resolve).
 */
public final class Questionnaire {
    private Questionnaire() {}

    private static final String[][] NATURE = {
            {"A wildfire sweeps the field you work in. What do you do?",
                    "Charge in and beat it back|F:HOT:2", "Run to the river and bring water|F:FLOW:2", "Climb the hill and let the wind show you its path|F:GUST:2", "Dig a trench and starve it|F:STONE:2"},
            {"Which place feels most like home?",
                    "A forge, loud and warm|F:HOT:1,F:METAL:1", "A quiet lakeshore|F:FLOW:2", "A cliff above the clouds|F:GUST:2", "A cave deep in the hills, glittering|F:STONE:1,F:GEM:1"},
            {"You find a door in a ruin. What do you do?",
                    "Step through without a light|F:SHADE:2", "Read every carving on the frame first|F:MIND:2", "Feel for the draught and follow where it leads|F:SPACE:2", "Wait until the sun has crossed the floor|F:TIME:2"},
            {"Someone is badly hurt. What do you reach for?",
                    "Whatever grows nearby: leaf, root, moss|F:GREEN:2", "A long watch at their side until the fever breaks|F:SOUL:2", "My own arms: I carry them, whatever it takes|F:BODY:2", "A litter and a shelter, built quickly|F:MAKER:2"},
            {"A locked iron chest, and no key.",
                    "Hammer at the hinges|F:METAL:2", "Turn it over in my head until the lock gives|F:MIND:2", "Ask it politely. Sing to it, if I must|F:MAKER:2", "Slip a hand through where the wall is thin|F:SPACE:2"},
            {"Pick a gift from a jeweller's stall.",
                    "A raw, uncut gemstone|F:GEM:2", "A bright silver blade|F:BRIGHT:1,F:METAL:1", "A moth-wing brooch|F:SHADE:2", "A tiny hourglass|F:TIME:2"},
            {"What would you most like to master?",
                    "The old fighting arts|F:BODY:2", "A language nobody speaks any more|F:MIND:2", "A craft the whole village needs|F:MAKER:2", "The art of going unseen|F:SHADE:2"},
            {"A storm rolls in over the sea. You...",
                    "Stand in the open and feel the charge in the air|F:BRIGHT:1,F:GUST:1", "Walk down to the waterline|F:FLOW:2", "Tie everything down and wait it out|F:STONE:2", "Light a lantern and watch the dark|F:SHADE:1,F:HOT:1"},
            {"A friend is trapped across a gap. How do you reach them?",
                    "Blast through whatever stands in the way|F:HOT:2", "Wade or swim the current beneath|F:FLOW:2", "Ride the gale across|F:GUST:2", "Raise the ground to meet them|F:STONE:2"},
            {"In a quiet hour you most like to...",
                    "Tend a garden and watch it grow|F:GREEN:2", "Sit with old stories of the dead|F:SOUL:2", "Polish a keepsake until it shines|F:GEM:1,F:METAL:1", "Draw, paint or write whatever comes|F:MAKER:1,F:MIND:1"},
            {"What do you value most in a fight?",
                    "Overwhelming power|F:HOT:1,F:BODY:1", "Adapting to the opponent|F:FLOW:1,F:MIND:1", "Speed|F:GUST:1,F:SPACE:1", "Staying power|F:STONE:1,F:METAL:1"},
            {"A rival taunts you in front of the whole guild. You...",
                    "Answer in kind, loudly|F:HOT:2", "Stay calm and let it flow past you|F:FLOW:2", "Laugh and walk away|F:GUST:2", "Wait. Time will settle it|F:TIME:2"},
            {"You are lost in a forest at dusk.",
                    "Follow the moss and the roots|F:GREEN:2", "Climb the highest tree and watch the horizon|F:BRIGHT:1,F:GUST:1", "Sit still and listen for what is out there|F:SOUL:1,F:SHADE:1", "Mark a trail with whatever is in my pockets|F:MAKER:1,F:SPACE:1"},
            {"What do you do with a free afternoon?",
                    "Train until drenched in sweat, then train more|F:BODY:2", "Read by the water, then nap|F:FLOW:1,F:MIND:1", "Wander wherever the road goes|F:SPACE:1,F:GUST:1", "Take something apart to see how it works|F:METAL:1,F:MAKER:1"},
            {"Which of these sounds would you stop to hear?",
                    "A blacksmith's hammer|F:METAL:1,F:HOT:1", "Rain on a tin roof|F:FLOW:2", "A choir in a stone hall|F:MAKER:1,F:GEM:1", "A clock that has stopped|F:TIME:2"},
            {"A child asks you what the moon is made of.",
                    "Silver, and a little light|F:BRIGHT:2", "Cold stone, like the mountains|F:STONE:1,F:COLD:1", "Old dreams that never came true|F:MIND:1,F:SOUL:1", "Pull their sleeve: \"Come, let me show you\"|F:SPACE:2"},
            {"Winter has come early. What do you do?",
                    "Welcome it: snow is a fine thing to fight in|F:COLD:2", "Stack the hearth high and sing|F:HOT:1,F:MAKER:1", "Wrap up, watch the frost on the glass|F:COLD:1,F:GEM:1", "Go on as usual: the cold only slows the weak|F:BODY:1,F:COLD:1"},
            {"Someone you trusted lied to you.",
                    "Confront them, loud and hot|F:HOT:1,F:SOUL:1", "Say nothing, remember everything|F:MIND:1,F:TIME:1", "Cut them off, quietly and completely|F:SHADE:1,F:COLD:1", "Ask them why, and listen to the answer|F:SOUL:1,F:GREEN:1"},
            {"Pick a charm for the road.",
                    "A feather|F:GUST:1,F:GREEN:1", "A coin that is always warm|F:HOT:1,F:METAL:1", "A shard of cold glass|F:GEM:1,F:COLD:1", "A key that fits nothing yet|F:SPACE:1,F:MIND:1"},
            {"What do you want people to see in you?",
                    "A bright, clear conviction|F:BRIGHT:2", "Nothing they could take away|F:SHADE:1,F:SPACE:1", "A hard worker with steady hands|F:BODY:1,F:METAL:1", "A keeper of old, good things|F:SOUL:1,F:GREEN:1"},
    };

    private static final String[][] HEARTH = {
            {"What do you want your magic for?",
                    "To protect my home and those who need it|K:CLOVER:2", "To grow strong, whatever the cost|K:SPADE:2", "To live at peace with the mana around me|K:HEART:2", "To build, to trade, to prosper|K:DIAMOND:2"},
            {"Pick a banner to carry.",
                    "A four-leaf clover on green|K:CLOVER:2", "A black spade on silver|K:SPADE:2", "A rose heart on cream|K:HEART:2", "A gold diamond on blue|K:DIAMOND:2"},
            {"A stranger in rags asks you for coin.",
                    "Give what you can spare and ask nothing|K:CLOVER:2", "Ask what they can do for you in return|K:SPADE:2", "Share your meal and sit with them|K:HEART:2", "Offer them work, and pay fairly|K:DIAMOND:2"},
            {"Describe your ideal day.",
                    "Honest work among friends|K:CLOVER:2", "A hard fight, won|K:SPADE:2", "A walk in the woods, drawing runes in the dirt|K:HEART:2", "A busy market and a full purse|K:DIAMOND:2"},
    };

    private static final String[][] BLOOD = {
            {"An old family story tells of an ancestor who...",
                    "Lived deep in a forest, in a tree-house|R:ELF:2,R:BEASTFOLK:1", "Forged iron beneath a mountain|R:DWARF:2,R:GIANT:1", "Ran with the herds across open plains|R:BEASTFOLK:2,R:ORC:1", "Wore a crown of horns into battle|R:OGRE:2,R:DRAGONEWT:1"},
            {"Which of these would you have been proud to be called?",
                    "The Quick|R:GOBLIN:2,R:ELF:1", "The Unmoved|R:GIANT:2,R:DWARF:1", "The Scaled|R:DRAGONEWT:2,R:SLIME:1", "The Tide-born|R:MERFOLK:2,R:SLIME:1"},
            {"Your family keeps a strange heirloom. It is...",
                    "A silver fang|R:VAMPIRE:2,R:BEASTFOLK:1", "A bone flute|R:UNDEAD:2,R:DAEMON:1", "A jar that never empties|R:SLIME:2,R:MERFOLK:1", "A plain iron ring|R:HUMAN:2,R:DWARF:1"},
            {"The elders say your family has always been...",
                    "Ordinary, and proud of it|R:HUMAN:2,R:GOBLIN:1", "Wanderers who go where the night is|R:VAMPIRE:2,R:UNDEAD:1", "A little too good at bargaining with fiends|R:DAEMON:2,R:HUMAN:1", "Larger than life|R:GIANT:2,R:ORC:1,R:OGRE:1"},
            {"On a feast day your family always...",
                    "Roasts a whole boar|R:ORC:2,R:OGRE:1", "Sings by the water until dawn|R:MERFOLK:2,R:ELF:1", "Tells stories of those who walked again|R:UNDEAD:2,R:VAMPIRE:1", "Argues about whose turn it is to cook|R:HUMAN:2,R:GOBLIN:1"},
    };

    private static final String[][] RESOLVE = {
            {"How long do you practise a spell you cannot yet cast?",
                    "Until I drop|T:2", "Until I am bored|T:1", "Until someone laughs at me, then I try harder|T:2", "I ask someone to show me instead|T:1"},
            {"You fail in front of everyone. What then?",
                    "I train harder the same night|T:2", "I sulk, then I train|T:1", "I shrug and try something else|T:0", "I make a plan and come back stronger|T:2"},
    };

    private static final String[][] SOCIAL = {
            {"Which of these was your childhood?",
                    "A manor with servants and a tutor|S:ROYALTY:2,S:NOBLE:1", "An estate with a name worth keeping|S:NOBLE:2,S:ROYALTY:1", "A house in town, and a trade|S:COMMONER:2", "A field, a roof that leaked, and a crowd of brothers and sisters|S:PEASANT:2,S:COMMONER:1"},
            {"At the family table the talk was usually about...",
                    "The crown, the court and the line of succession|S:ROYALTY:2", "Land, titles and who owes whom|S:NOBLE:2", "The week's work and the market|S:COMMONER:2", "Whether the harvest will last the winter|S:PEASANT:2"},
    };

    private static final int[] DRAW = {6, 2, 2, 1, 1};     // 1.16.0: six questions about your nature now reach every magic, the rest as before
    private static final String[][][] POOLS = {NATURE, HEARTH, BLOOD, RESOLVE, SOCIAL};

    // ---------------------------------------------------------------- the long sheet (setting wizard.longQuestionnaire)
    /** Questions per section of the long sheet, in POOLS order (nature, hearth, blood, resolve, social): 12 + 4 + 4 + 6 + 4 = 30. Capped by the pool size. */
    static final int[] DRAW_LONG = {12, 4, 4, 6, 4};
    /** The section of each pool, in POOLS order. */
    private static final String[] CATS = {SheetSections.NATURE, SheetSections.HEARTH, SheetSections.BLOOD, SheetSections.RESOLVE, SheetSections.ORIGIN};
    /** The old questions first, the new ones appended: old sheets still read, and the old pools are untouched. */
    private static final String[][][] LONG_POOLS = {cat(NATURE, LongQuestions.NATURE), cat(HEARTH, LongQuestions.HEARTH), cat(BLOOD, LongQuestions.BLOOD),
            cat(RESOLVE, LongQuestions.RESOLVE), cat(SOCIAL, LongQuestions.SOCIAL)};

    private static String[][] cat(String[][] a, String[][] b) {
        String[][] r = new String[a.length + b.length][];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    /** The pool sizes of the long sheet (nature, hearth, blood, resolve, social), for tests and the doc. */
    static String[][][] longPools() { return LONG_POOLS; }

    // ---------------------------------------------------------------- one questionnaire
    /**
     * The questions as shown, kept on the server to read the answers back. {@code cats} holds the section id of each question (see {@link SheetSections}) on the long sheet and is empty on the
     * old one; {@code longMode} says which leaf and tie rules {@link #result} applies, so a sheet is always read the way it was drawn.
     */
    public record Sheet(List<String> texts, List<List<String>> shownOptions, List<List<String>> tokens, List<String> cats, boolean longMode) {
        public Sheet(List<String> texts, List<List<String>> shownOptions, List<List<String>> tokens) { this(texts, shownOptions, tokens, List.of(), false); }

        public CompoundTag toTag() {
            CompoundTag t = new CompoundTag();
            ListTag qs = new ListTag();
            boolean sections = cats.size() == texts.size();
            for (int i = 0; i < texts.size(); i++) {
                CompoundTag q = new CompoundTag();
                q.putString("T", texts.get(i));
                ListTag o = new ListTag();
                for (String s : shownOptions.get(i)) o.add(StringTag.valueOf(s));
                q.put("O", o);
                if (sections) q.putString("C", cats.get(i));                 // optional: an old sheet has none and the screen then shows no sections
                qs.add(q);
            }
            t.put("Q", qs);
            return t;
        }
    }

    public static Sheet draw(RandomSource rng) { return draw(rng, false); }

    /** {@code longSheet} false = the old 12-question sheet, exactly as it always was; true = the 30-question sheet in ordered sections. */
    public static Sheet draw(RandomSource rng, boolean longSheet) {
        String[][][] pools = longSheet ? LONG_POOLS : POOLS;
        int[] draw = longSheet ? DRAW_LONG : DRAW;
        List<String> texts = new ArrayList<>(), cats = new ArrayList<>();
        List<List<String>> shown = new ArrayList<>(), toks = new ArrayList<>();
        for (int p = 0; p < pools.length; p++) {
            List<String[]> pool = new ArrayList<>(List.of(pools[p]));
            Collections.shuffle(pool, new java.util.Random(rng.nextLong()));
            for (int i = 0; i < Math.min(draw[p], pool.size()); i++) {
                String[] q = pool.get(i);
                List<String[]> opts = new ArrayList<>();
                for (int k = 1; k < q.length; k++) opts.add(q[k].split("\\|", 2));
                Collections.shuffle(opts, new java.util.Random(rng.nextLong()));
                texts.add(q[0]);
                cats.add(CATS[p]);
                List<String> s = new ArrayList<>(), t = new ArrayList<>();
                for (String[] o : opts) { s.add(o[0]); t.add(o[1]); }
                shown.add(s);
                toks.add(t);
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) order.add(i);
        if (longSheet) {
            order.sort((a, b) -> Integer.compare(SheetSections.ORDER.indexOf(cats.get(a)), SheetSections.ORDER.indexOf(cats.get(b))));   // stable: inside a section the draw order (random) stays
        } else {
            // the questions themselves come in a shuffled order too, so the "kind" of a question never gives itself away
            Collections.shuffle(order, new java.util.Random(rng.nextLong()));
        }
        List<String> t2 = new ArrayList<>(), c2 = new ArrayList<>();
        List<List<String>> s2 = new ArrayList<>(), k2 = new ArrayList<>();
        for (int i : order) { t2.add(texts.get(i)); c2.add(cats.get(i)); s2.add(shown.get(i)); k2.add(toks.get(i)); }
        return longSheet ? new Sheet(t2, s2, k2, c2, true) : new Sheet(t2, s2, k2);
    }

    // ---------------------------------------------------------------- the result
    /**
     * What the questionnaire decided. {@code primary} is the attribute of the grimoire that is granted. When two attributes were nearly
     * tied they fuse instead of coming as two grimoires: {@code parentA} and {@code parentB} are the pair (both null for a single
     * attribute), {@code primary} is the attribute they blend into if the game has one (Fire and Water give Glass), and {@code born} says
     * they have no blend in the game: a new magic is born of the pair (see grimoire.BornMagic). {@code detail} says how clear each answer was (for the summary the wizard speaks).
     */
    public record Result(MagicType primary, MagicType parentA, MagicType parentB, boolean born, Kingdom kingdom, Race blood, int leaves, SocialClass social, Detail detail) {
        public Result(MagicType primary, MagicType parentA, MagicType parentB, boolean born, Kingdom kingdom, Race blood, int leaves, SocialClass social) {
            this(primary, parentA, parentB, born, kingdom, blood, leaves, social, null);
        }
        public boolean fused() { return parentA != null && parentB != null; }
    }

    /** {@code resolve} of {@code resolveMax} is the total of the resolve answers (not the luck); the words are clear / leaning / narrow; {@code pull} is the runner-up nature when it came close. */
    public record Detail(int resolve, int resolveMax, String natureWord, MagicType pull, String kingdomWord, String bloodWord, String socialWord) {}

    /** The total resolve of the chosen answers. */
    public static int resolveOf(Sheet sheet, int[] answers) {
        int sum = 0;
        for (int i = 0; i < answers.length; i++) sum += tokenResolve(sheet.tokens().get(i).get(answers[i]));
        return sum;
    }

    /** The best total the sheet's resolve questions allow (the best option of each question). */
    public static int resolveMax(Sheet sheet) {
        int sum = 0;
        for (List<String> opts : sheet.tokens()) {
            boolean any = false;
            int best = Integer.MIN_VALUE;
            for (String o : opts) { if (o.contains("T:")) any = true; best = Math.max(best, tokenResolve(o)); }
            if (any) sum += best;
        }
        return sum;
    }

    /** For each question the option with the most resolve (the first one on a question without resolve): the path of a player who always takes the best resolve answer. */
    public static int[] bestResolveAnswers(Sheet sheet) {
        int[] a = new int[sheet.texts().size()];
        for (int i = 0; i < a.length; i++) {
            int bestV = Integer.MIN_VALUE;
            List<String> opts = sheet.tokens().get(i);
            for (int k = 0; k < opts.size(); k++) if (tokenResolve(opts.get(k)) > bestV) { bestV = tokenResolve(opts.get(k)); a[i] = k; }
        }
        return a;
    }

    private static int tokenResolve(String optionTokens) {
        int s = 0;
        for (String tok : optionTokens.split(",")) {
            String[] p = tok.split(":");
            if (p.length >= 2 && p[0].equals("T")) s += Integer.parseInt(p[p.length - 1]);
        }
        return s;
    }

    /** Reads the answers (the index of the chosen shown option for each question). Null if they do not fit the sheet. */
    public static Result result(Sheet sheet, int[] answers, List<MagicType> pool, boolean allowFive, double dualChance, RandomSource rng) {
        if (answers == null || answers.length != sheet.texts().size()) return null;
        boolean lng = sheet.longMode();
        double eps = lng ? 1e-3 : 0;                                                   // long sheet: a tie is broken by the later answers (weight grows with the question's place), not at random
        Map<MagicType, Double> magic = new EnumMap<>(MagicType.class), lateMagic = new EnumMap<>(MagicType.class);
        Map<MagicFlavors.Flavor, Double> flavor = new EnumMap<>(MagicFlavors.Flavor.class), lateFlavor = new EnumMap<>(MagicFlavors.Flavor.class);
        Map<Kingdom, Double> kingdom = new EnumMap<>(Kingdom.class), lateKingdom = new EnumMap<>(Kingdom.class);
        Map<Race, Double> blood = new EnumMap<>(Race.class), lateBlood = new EnumMap<>(Race.class);
        Map<SocialClass, Double> social = new EnumMap<>(SocialClass.class), lateSocial = new EnumMap<>(SocialClass.class);
        int resolve = 0;
        for (int i = 0; i < answers.length; i++) {
            if (answers[i] < 0 || answers[i] >= sheet.tokens().get(i).size()) return null;
            double w = (i + 1) / (double) answers.length;
            for (String tok : sheet.tokens().get(i).get(answers[i]).split(",")) {
                String[] p = tok.split(":");
                int pts = Integer.parseInt(p[p.length - 1]);
                try {
                    switch (p[0]) {
                        case "E" -> { add(magic, MagicType.valueOf(p[1]), pts); add(lateMagic, MagicType.valueOf(p[1]), pts * w); }               // a magic by name (old sheets)
                        case "F" -> { add(flavor, MagicFlavors.Flavor.valueOf(p[1]), pts); add(lateFlavor, MagicFlavors.Flavor.valueOf(p[1]), pts * w); }   // a flavor (1.16.0)
                        case "K" -> { add(kingdom, Kingdom.valueOf(p[1]), pts); add(lateKingdom, Kingdom.valueOf(p[1]), pts * w); }
                        case "R" -> { add(blood, Race.valueOf(p[1]), pts); add(lateBlood, Race.valueOf(p[1]), pts * w); }
                        case "S" -> { add(social, SocialClass.valueOf(p[1]), pts); add(lateSocial, SocialClass.valueOf(p[1]), pts * w); }
                        case "T" -> resolve += pts;
                        default -> { }
                    }
                } catch (IllegalArgumentException ignored) { }
            }
        }
        java.util.function.ToDoubleFunction<MagicType> sc = m -> score(m, magic, flavor) + eps * score(m, lateMagic, lateFlavor);
        List<MagicType> ranked = rank(pool, sc, rng);
        MagicType primary = ranked.get(0), grimoire = primary, pa = null, pb = null, pullCand = null;
        boolean born = false;
        double top = sc.applyAsDouble(primary), second = 0;
        for (MagicType m : ranked) {                                                   // the best attribute that shares nothing with the first
            if (m == primary || MagicFlavors.shares(primary, m)) continue;
            pullCand = m;
            second = sc.applyAsDouble(m);
            if (second >= Math.max(1.5, 0.6 * top) && rng.nextDouble() < dualChance) {
                pa = primary;
                pb = m;
                MagicType blend = MagicFlavors.blendOf(MagicFlavors.main(primary), MagicFlavors.main(m));
                if (blend != null) grimoire = blend; else born = true;
            }
            break;
        }
        Kingdom k = best(kingdom, lateKingdom, eps, Kingdom.values(), rng);
        Race r = best(blood, lateBlood, eps, Race.values(), rng);
        if (r == Race.DEVIL) r = Race.HUMAN;
        int luck = rng.nextInt(lng ? LeafRoll.LUCK_SIDES : 3);                         // old sheet: practice + fortune 0..2; long sheet: the answers decide, fortune is 0 or 1
        int leaves = lng ? LeafRoll.leaves(resolve, luck) : LeafRoll.oldLeaves(resolve, luck);
        if (leaves >= 5 && !allowFive) leaves = 4;
        SocialClass sc2 = best(social, lateSocial, eps, SocialClass.values(), rng);
        MagicType pull = pa == null && pullCand != null && top > 0 && second >= 0.5 * top ? pullCand : null;
        Detail detail = new Detail(resolve, resolveMax(sheet), word(top, second), pull, marginWord(kingdom, k), marginWord(blood, r), marginWord(social, sc2));
        return new Result(grimoire, pa, pb, born, k, r, leaves, sc2, detail);
    }

    // ---------------------------------------------------------------- the summary the wizard speaks
    /** How clear a win was: the gap to the runner-up. Words only, never the numbers of options that were not chosen. */
    static String word(double top, double second) {
        double gap = top - second;
        return gap >= Math.max(2, 0.4 * top) ? "clear" : gap >= 1 ? "leaning" : "narrow";
    }

    private static <E extends Enum<E>> String marginWord(Map<E, Double> pts, E winner) {
        double top = pts.getOrDefault(winner, 0.0), second = 0;
        for (Map.Entry<E, Double> e : pts.entrySet()) if (e.getKey() != winner) second = Math.max(second, e.getValue());
        return word(top, second);
    }

    private static String shortName(MagicType m) { return m.displayName.replace(" Magic", ""); }

    /**
     * The lines the wizard shows after the last answer (long sheet only; empty when the result has no detail): one per category, the winner and how clear it was, never a number of an option
     * that was not chosen. Pure, so a test can read it.
     */
    public static List<String> summary(Result r) {
        Detail d = r.detail();
        if (d == null) return List.of();
        List<String> out = new ArrayList<>();
        if (r.fused()) {
            out.add("Nature: " + shortName(r.parentA()) + " and " + shortName(r.parentB()) + ", fused " + (r.born() ? "into a magic of their own" : "into " + shortName(r.primary())) + " (finely balanced)");
        } else {
            out.add("Nature: " + shortName(r.primary()) + " (" + d.natureWord() + ")" + (d.pull() != null ? ", with a pull towards " + shortName(d.pull()) : ""));
        }
        out.add("Hearth: " + r.kingdom().displayName.replace(" Kingdom", "") + " (" + d.kingdomWord() + ")");
        out.add("Blood: " + r.blood().displayName + " (" + d.bloodWord() + ")");
        out.add("Origin: " + r.social().displayName + " (" + d.socialWord() + ")");
        String[] n = {"", "", "", "a three-leaf", "a four-leaf", "a five-leaf"};
        out.add("Resolve: " + d.resolve() + " of " + d.resolveMax() + ", " + n[Math.max(3, Math.min(5, r.leaves()))] + " grimoire");
        return out;
    }

    private static <E> void add(Map<E, Double> m, E key, double v) { m.merge(key, v, Double::sum); }

    /** An attribute's score: the points of its flavors (shared out, so a two-flavor attribute needs both) and any points given by name, tilted by rarity. */
    private static double score(MagicType m, Map<MagicType, Double> direct, Map<MagicFlavors.Flavor, Double> flavor) {
        MagicFlavors.Flavor[] fl = MagicFlavors.flavors(m);
        double s = direct.getOrDefault(m, 0.0);
        if (fl.length > 0) {
            double f = 0;
            for (MagicFlavors.Flavor x : fl) f += flavor.getOrDefault(x, 0.0);
            s += f / Math.sqrt(fl.length);
        }
        return s * MagicFlavors.rarity(m);
    }

    private static List<MagicType> rank(List<MagicType> pool, java.util.function.ToDoubleFunction<MagicType> sc, RandomSource rng) {
        List<MagicType> l = new ArrayList<>(pool);
        Collections.shuffle(l, new java.util.Random(rng.nextLong()));                    // ties break at random
        l.sort((x, y) -> Double.compare(sc.applyAsDouble(y), sc.applyAsDouble(x)));
        return l;
    }

    /** The highest score wins; a tie is broken by the later answers on the long sheet (eps > 0), at random otherwise or when still tied. */
    private static <E extends Enum<E>> E best(Map<E, Double> score, Map<E, Double> late, double eps, E[] all, RandomSource rng) {
        List<E> l = new ArrayList<>(List.of(all));
        Collections.shuffle(l, new java.util.Random(rng.nextLong()));
        E best = l.get(0);
        for (E e : l) if (score.getOrDefault(e, 0.0) + eps * late.getOrDefault(e, 0.0) > score.getOrDefault(best, 0.0) + eps * late.getOrDefault(best, 0.0)) best = e;
        return best;
    }
}
