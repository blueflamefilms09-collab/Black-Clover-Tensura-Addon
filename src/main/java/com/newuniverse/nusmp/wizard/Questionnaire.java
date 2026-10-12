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

    // ---------------------------------------------------------------- one questionnaire
    /** The questions as shown (random order), kept on the server to read the answers back. */
    public record Sheet(List<String> texts, List<List<String>> shownOptions, List<List<String>> tokens) {
        public CompoundTag toTag() {
            CompoundTag t = new CompoundTag();
            ListTag qs = new ListTag();
            for (int i = 0; i < texts.size(); i++) {
                CompoundTag q = new CompoundTag();
                q.putString("T", texts.get(i));
                ListTag o = new ListTag();
                for (String s : shownOptions.get(i)) o.add(StringTag.valueOf(s));
                q.put("O", o);
                qs.add(q);
            }
            t.put("Q", qs);
            return t;
        }
    }

    public static Sheet draw(RandomSource rng) {
        List<String> texts = new ArrayList<>();
        List<List<String>> shown = new ArrayList<>(), toks = new ArrayList<>();
        for (int p = 0; p < POOLS.length; p++) {
            List<String[]> pool = new ArrayList<>(List.of(POOLS[p]));
            Collections.shuffle(pool, new java.util.Random(rng.nextLong()));
            for (int i = 0; i < Math.min(DRAW[p], pool.size()); i++) {
                String[] q = pool.get(i);
                List<String[]> opts = new ArrayList<>();
                for (int k = 1; k < q.length; k++) opts.add(q[k].split("\\|", 2));
                Collections.shuffle(opts, new java.util.Random(rng.nextLong()));
                texts.add(q[0]);
                List<String> s = new ArrayList<>(), t = new ArrayList<>();
                for (String[] o : opts) { s.add(o[0]); t.add(o[1]); }
                shown.add(s);
                toks.add(t);
            }
        }
        // the questions themselves come in a shuffled order too, so the "kind" of a question never gives itself away
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) order.add(i);
        Collections.shuffle(order, new java.util.Random(rng.nextLong()));
        List<String> t2 = new ArrayList<>();
        List<List<String>> s2 = new ArrayList<>(), k2 = new ArrayList<>();
        for (int i : order) { t2.add(texts.get(i)); s2.add(shown.get(i)); k2.add(toks.get(i)); }
        return new Sheet(t2, s2, k2);
    }

    // ---------------------------------------------------------------- the result
    /**
     * What the questionnaire decided. {@code primary} is the attribute of the grimoire that is granted. When two attributes were nearly
     * tied they fuse instead of coming as two grimoires: {@code parentA} and {@code parentB} are the pair (both null for a single
     * attribute), {@code primary} is the attribute they blend into if the game has one (Fire and Water give Glass), and {@code born} says
     * they have no blend in the game: a new magic is born of the pair (see grimoire.BornMagic).
     */
    public record Result(MagicType primary, MagicType parentA, MagicType parentB, boolean born, Kingdom kingdom, Race blood, int leaves, SocialClass social) {
        public boolean fused() { return parentA != null && parentB != null; }
    }

    /** Reads the answers (the index of the chosen shown option for each question). Null if they do not fit the sheet. */
    public static Result result(Sheet sheet, int[] answers, List<MagicType> pool, boolean allowFive, double dualChance, RandomSource rng) {
        if (answers == null || answers.length != sheet.texts().size()) return null;
        Map<MagicType, Integer> magic = new EnumMap<>(MagicType.class);
        Map<MagicFlavors.Flavor, Integer> flavor = new EnumMap<>(MagicFlavors.Flavor.class);
        Map<Kingdom, Integer> kingdom = new EnumMap<>(Kingdom.class);
        Map<Race, Integer> blood = new EnumMap<>(Race.class);
        Map<SocialClass, Integer> social = new EnumMap<>(SocialClass.class);
        int resolve = 0;
        for (int i = 0; i < answers.length; i++) {
            if (answers[i] < 0 || answers[i] >= sheet.tokens().get(i).size()) return null;
            for (String tok : sheet.tokens().get(i).get(answers[i]).split(",")) {
                String[] p = tok.split(":");
                int pts = Integer.parseInt(p[p.length - 1]);
                try {
                    switch (p[0]) {
                        case "E" -> magic.merge(MagicType.valueOf(p[1]), pts, Integer::sum);                       // a magic by name (old sheets)
                        case "F" -> flavor.merge(MagicFlavors.Flavor.valueOf(p[1]), pts, Integer::sum);          // a flavor (1.16.0)
                        case "K" -> kingdom.merge(Kingdom.valueOf(p[1]), pts, Integer::sum);
                        case "R" -> blood.merge(Race.valueOf(p[1]), pts, Integer::sum);
                        case "S" -> social.merge(SocialClass.valueOf(p[1]), pts, Integer::sum);
                        case "T" -> resolve += pts;
                        default -> { }
                    }
                } catch (IllegalArgumentException ignored) { }
            }
        }
        List<MagicType> ranked = rank(pool, magic, flavor, rng);
        MagicType primary = ranked.get(0), grimoire = primary, pa = null, pb = null;
        boolean born = false;
        double top = score(primary, magic, flavor);
        for (MagicType m : ranked) {                                                   // the best attribute that shares nothing with the first
            if (m == primary || MagicFlavors.shares(primary, m)) continue;
            if (score(m, magic, flavor) >= Math.max(1.5, 0.6 * top) && rng.nextDouble() < dualChance) {
                pa = primary;
                pb = m;
                MagicType blend = MagicFlavors.blendOf(MagicFlavors.main(primary), MagicFlavors.main(m));
                if (blend != null) grimoire = blend; else born = true;
            }
            break;
        }
        Kingdom k = best(kingdom, Kingdom.values(), rng);
        Race r = best(blood, Race.values(), rng);
        if (r == Race.DEVIL) r = Race.HUMAN;
        int score = resolve + rng.nextInt(3);                                          // 0..6: practice counts, a little fortune too
        int leaves = score >= 5 ? 5 : score >= 3 ? 4 : 3;
        if (leaves >= 5 && !allowFive) leaves = 4;
        return new Result(grimoire, pa, pb, born, k, r, leaves, best(social, SocialClass.values(), rng));
    }

    /** An attribute's score: the points of its flavors (shared out, so a two-flavor attribute needs both) and any points given by name, tilted by rarity. */
    private static double score(MagicType m, Map<MagicType, Integer> direct, Map<MagicFlavors.Flavor, Integer> flavor) {
        MagicFlavors.Flavor[] fl = MagicFlavors.flavors(m);
        double s = direct.getOrDefault(m, 0);
        if (fl.length > 0) {
            double f = 0;
            for (MagicFlavors.Flavor x : fl) f += flavor.getOrDefault(x, 0);
            s += f / Math.sqrt(fl.length);
        }
        return s * MagicFlavors.rarity(m);
    }

    private static List<MagicType> rank(List<MagicType> pool, Map<MagicType, Integer> direct, Map<MagicFlavors.Flavor, Integer> flavor, RandomSource rng) {
        List<MagicType> l = new ArrayList<>(pool);
        Collections.shuffle(l, new java.util.Random(rng.nextLong()));                    // ties break at random
        l.sort((x, y) -> Double.compare(score(y, direct, flavor), score(x, direct, flavor)));
        return l;
    }

    private static <E extends Enum<E>> E best(Map<E, Integer> score, E[] all, RandomSource rng) {
        List<E> l = new ArrayList<>(List.of(all));
        Collections.shuffle(l, new java.util.Random(rng.nextLong()));
        E best = l.get(0);
        for (E e : l) if (score.getOrDefault(e, 0) > score.getOrDefault(best, 0)) best = e;
        return best;
    }
}
