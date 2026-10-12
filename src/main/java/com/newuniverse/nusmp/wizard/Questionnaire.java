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
 *   <li>the magic attribute (one, and now and then a second when two natures are nearly tied),</li>
 *   <li>the kingdom you hail from (its grimoire suit, a buff and a debuff),</li>
 *   <li>the blood of a Tensura race mixed into yours (a buff and a debuff),</li>
 *   <li>how far your resolve takes the grimoire (3, 4 or 5 leaves; the five-leaf cover still needs the forbidden-start rule).</li>
 * </ul>
 * Option strings read "text|TOKEN:points,TOKEN:points": tokens are E:&lt;magic&gt;, K:&lt;kingdom&gt;, R:&lt;race&gt;, T (resolve).
 */
public final class Questionnaire {
    private Questionnaire() {}

    private static final String[][] NATURE = {
            {"A wildfire sweeps the field you work in. What do you do?",
                    "Charge in and beat it back|E:FLAME:2", "Run to the river and bring water|E:WATER:2", "Climb the hill and let the wind show you its path|E:WIND:2", "Dig a trench and starve it|E:EARTH:2"},
            {"Which place feels most like home?",
                    "A forge, loud and warm|E:FLAME:2", "A quiet lakeshore|E:WATER:2", "A cliff above the clouds|E:WIND:2", "A cave deep in the hills|E:EARTH:2"},
            {"A friend is trapped across a gap. How do you reach them?",
                    "Blast through whatever stands in the way|E:FLAME:2", "Swim the current beneath|E:WATER:2", "Ride the gale across|E:WIND:2", "Raise the ground to meet them|E:EARTH:2"},
            {"What do you value most in a fight?",
                    "Overwhelming power|E:FLAME:2", "Adapting to the opponent|E:WATER:2", "Speed|E:WIND:2", "Staying power|E:EARTH:2"},
            {"You may take one gift from the festival stalls.",
                    "An ember lantern|E:FLAME:2", "A silver flask of spring water|E:WATER:2", "A feather kite|E:WIND:2", "A polished river stone|E:EARTH:2"},
            {"A rival taunts you in front of the whole guild. You...",
                    "Answer in kind, loudly|E:FLAME:2", "Stay calm and let it flow past you|E:WATER:2", "Laugh and walk away|E:WIND:2", "Stand your ground and say nothing|E:EARTH:2"},
            {"What do you do with a free afternoon?",
                    "Train until you are drenched in sweat, then train more|E:FLAME:1,E:EARTH:1", "Read by the water, then nap|E:WATER:1,E:WIND:1", "Wander wherever the road goes|E:WIND:1,E:FLAME:1", "Tend a garden and watch it grow|E:EARTH:1,E:WATER:1"},
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

    private static final int[] DRAW = {3, 2, 3, 1, 1};
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
    public record Result(MagicType primary, MagicType secondary, Kingdom kingdom, Race blood, int leaves, SocialClass social) {}

    /** Reads the answers (the index of the chosen shown option for each question). Null if they do not fit the sheet. */
    public static Result result(Sheet sheet, int[] answers, List<MagicType> pool, boolean allowFive, double dualChance, RandomSource rng) {
        if (answers == null || answers.length != sheet.texts().size()) return null;
        Map<MagicType, Integer> magic = new EnumMap<>(MagicType.class);
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
                        case "E" -> magic.merge(MagicType.valueOf(p[1]), pts, Integer::sum);
                        case "K" -> kingdom.merge(Kingdom.valueOf(p[1]), pts, Integer::sum);
                        case "R" -> blood.merge(Race.valueOf(p[1]), pts, Integer::sum);
                        case "S" -> social.merge(SocialClass.valueOf(p[1]), pts, Integer::sum);
                        case "T" -> resolve += pts;
                        default -> { }
                    }
                } catch (IllegalArgumentException ignored) { }
            }
        }
        List<MagicType> ranked = rank(pool, magic, rng);
        MagicType primary = ranked.get(0), secondary = null;
        if (ranked.size() > 1) {
            int a = magic.getOrDefault(primary, 0), b = magic.getOrDefault(ranked.get(1), 0);
            if (b >= 2 && b >= 0.6 * a && rng.nextDouble() < dualChance) secondary = ranked.get(1);
        }
        Kingdom k = best(kingdom, Kingdom.values(), rng);
        Race r = best(blood, Race.values(), rng);
        if (r == Race.DEVIL) r = Race.HUMAN;
        int score = resolve + rng.nextInt(3);                                          // 0..6: practice counts, a little fortune too
        int leaves = score >= 5 ? 5 : score >= 3 ? 4 : 3;
        if (leaves >= 5 && !allowFive) leaves = 4;
        return new Result(primary, secondary, k, r, leaves, best(social, SocialClass.values(), rng));
    }

    private static List<MagicType> rank(List<MagicType> pool, Map<MagicType, Integer> score, RandomSource rng) {
        List<MagicType> l = new ArrayList<>(pool);
        Collections.shuffle(l, new java.util.Random(rng.nextLong()));                    // ties break at random
        l.sort((x, y) -> Integer.compare(score.getOrDefault(y, 0), score.getOrDefault(x, 0)));
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
