package com.newuniverse.nusmp.dragonballs;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.multiverse.KnightRank;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.skill.GrimoireMagicSkill;
import com.newuniverse.nusmp.skill.GrimoirePageSkill;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.skill.ShishouUltimateSkill;
import com.newuniverse.nusmp.skill.SinWeaponSkill;
import com.newuniverse.nusmp.skill.WeaponSkill;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.GameMasterBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The live lists the overpowered wishes pick from, read from the REGISTRIES at run time (never copied): every race of the Tensura race registry (all addons that add races show up, and so does this mod's Saiyan),
 * every skill of the skill registry of every loaded mod (the tier is Tensura's own {@code Skill.SkillType}, see {@link SkillRules}), every item of the item registry that is not an operator item, every relic
 * of this mod up to God grade, and the Magic Knight ranks. Each list is built lazily on first use and kept; {@link #invalidate} drops them when a server starts or stops and on {@code /reload}, so there is no per-tick work
 * and no scan unless a wish or a tab completion asks. Worst case: one pass over a registry (a few thousand entries) per invalidation. All calls are guarded: a registry that is not ready answers an empty list.
 */
public final class WishCatalog {
    private WishCatalog() {}

    /** The mod's own race, a flag on the Magic Knight profile ({@code MultiverseProfile#setDevil}), offered next to the registry races. Not a registry id. */
    public static final String DEVIL_ID = "anime_multiverse:devil";

    private static volatile List<WishNames.Entry> races, skills, items, relicsAll;
    private static volatile boolean installed;
    /** Cuts of the lists by ceiling / grade and the command spellings of a list, kept until {@link #invalidate} so a tab completion costs no rebuild. */
    private static final Map<Object, List<WishNames.Entry>> CUTS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<List<WishNames.Entry>, List<String>> COMMAND_NAMES = java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    /** Called once from {@link DbBoot}: fills the runtime suppliers of {@link WishRules} and hooks the invalidation events. */
    static synchronized void install() {
        if (installed) return;
        installed = true;
        WishRules.races = WishCatalog::races;
        WishRules.skills = WishCatalog::allSkills;
        WishRules.items = WishCatalog::items;
        WishRules.relics = () -> relics(true);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> invalidate());
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> invalidate());
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener((ResourceManagerReloadListener) rm -> invalidate()));
    }

    public static void invalidate() {
        races = null;
        skills = null;
        items = null;
        relicsAll = null;
        CUTS.clear();
        COMMAND_NAMES.clear();
    }

    /** The one-word command spellings of a list of this class ({@code demon_lord}), computed once per list. */
    public static List<String> commandNames(List<WishNames.Entry> list) { return COMMAND_NAMES.computeIfAbsent(list, WishNames::commandNames); }

    // ================================================================ races
    /** Every race of the live race registry, plus the mod's Devil. */
    public static List<WishNames.Entry> races() {
        List<WishNames.Entry> r = races;
        if (r != null) return r;
        List<String> ids = new ArrayList<>();
        Map<String, String> tr = new HashMap<>();
        try {
            for (ResourceLocation id : RaceAPI.getRaceRegistry().getIds()) {
                ids.add(id.toString());
                ManasRace race = RaceAPI.getRaceRegistry().get(id);
                if (race != null) tr.put(id.toString(), race.getName().getString());
            }
        } catch (RuntimeException | LinkageError e) {
            // the race registry is not ready: an empty list, tried again next time
            return List.of();
        }
        ids.sort(String::compareTo);
        if (!ids.contains(DEVIL_ID)) ids.add(DEVIL_ID);
        r = List.copyOf(WishNames.build(ids, tr, null));
        races = r;
        return r;
    }

    // ================================================================ skills
    /** True when a registered skill may be offered by a wish at all (not a tier question): it has a Tensura tier, is not a stub and is not managed by one of this mod's grant services. */
    static boolean teachableKind(ResourceLocation id, ManasSkill sk) {
        if (sk == null || sk instanceof GrimoireBook || sk instanceof GrimoirePageSkill || sk instanceof GrimoireMagicSkill || sk instanceof WeaponSkill || sk instanceof SinWeaponSkill || sk instanceof ShishouUltimateSkill) return false;
        if (SkillRules.blockedId(id.toString()) || SkillRules.stubClass(sk.getClass().getSimpleName())) return false;
        return skillTier(sk) != null;
    }

    /** The tier of a live skill: Tensura's {@code Skill.SkillType} through the pure {@link SkillRules#tierOf}. A skill that is not a Tensura {@code Skill} has none. */
    public static SkillRules.Tier skillTier(ManasSkill sk) { return sk instanceof Skill s ? SkillRules.tierOf(s.getType().getSerializedName()) : null; }

    /** Every skill of every loaded mod that a wish could teach at the highest ceiling; the tag of an entry is its tier name. */
    public static List<WishNames.Entry> allSkills() {
        List<WishNames.Entry> r = skills;
        if (r != null) return r;
        List<String> ids = new ArrayList<>();
        Map<String, String> tr = new HashMap<>(), tag = new HashMap<>();
        Set<String> grimoire = new HashSet<>();
        try {
            for (ResourceLocation g : NUSkills.allGrimoireSkillIds()) grimoire.add(g.toString());
            for (ResourceLocation id : SkillAPI.getSkillRegistry().getIds()) {
                ManasSkill sk = SkillAPI.getSkillRegistry().get(id);
                if (grimoire.contains(id.toString()) || !teachableKind(id, sk)) continue;
                ids.add(id.toString());
                tr.put(id.toString(), sk.getName().getString());
                tag.put(id.toString(), skillTier(sk).name());
            }
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
        ids.sort(String::compareTo);
        r = List.copyOf(WishNames.build(ids, tr, tag));
        skills = r;
        return r;
    }

    /** The skills this dragon may teach: the live list cut at the tier ceiling of the set. */
    public static List<WishNames.Entry> skillsFor(BallSet set) {
        SkillRules.Tier ceil = DbConfig.skillCeiling(set);
        List<WishNames.Entry> all = allSkills();
        if (all.isEmpty()) return all;
        return CUTS.computeIfAbsent("skills:" + ceil, k -> {
            List<WishNames.Entry> out = new ArrayList<>();
            for (WishNames.Entry e : all) if (SkillRules.allowed(SkillRules.Tier.byName(e.tag()), ceil)) out.add(e);
            return List.copyOf(out);
        });
    }

    /** Registry counts per tier of the live skill list (CI log line). */
    public static Map<SkillRules.Tier, Integer> skillCounts() {
        Map<SkillRules.Tier, Integer> m = new java.util.EnumMap<>(SkillRules.Tier.class);
        for (WishNames.Entry e : allSkills()) {
            SkillRules.Tier t = SkillRules.Tier.byName(e.tag());
            if (t != null) m.merge(t, 1, Integer::sum);
        }
        return m;
    }

    // ================================================================ items
    private static final TagKey<Item> HIDDEN = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:hidden_from_recipe_viewers"));

    /** Runtime half of the item blocklist: the pure id rules, operator blocks (game master), the hidden-from-viewers tag and this mod's relics (they have the weapon wish). */
    static boolean itemBlocked(ResourceLocation id, Item it) {
        if (WishRules.itemBlocked(id.toString())) return true;
        if (it instanceof BlockItem bi && bi.getBlock() instanceof GameMasterBlock) return true;
        if (it.builtInRegistryHolder().is(HIDDEN)) return true;
        return id.getNamespace().equals("anime_multiverse") && Relics.byId(id.getPath()) != null;
    }

    /** Every item of the item registry that an item wish may give. */
    public static List<WishNames.Entry> items() {
        List<WishNames.Entry> r = items;
        if (r != null) return r;
        List<String> ids = new ArrayList<>();
        Map<String, String> tr = new HashMap<>();
        try {
            for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                Item it = BuiltInRegistries.ITEM.get(id);
                if (itemBlocked(id, it)) continue;
                ids.add(id.toString());
                tr.put(id.toString(), it.getDescription().getString());
            }
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
        ids.sort(String::compareTo);
        r = List.copyOf(WishNames.build(ids, tr, null));
        items = r;
        return r;
    }

    // ================================================================ relics (the weapon wish)
    /** The relics of this mod a weapon wish may give: every grade up to God, and Genesis and EX-Singularity only when {@code genesis}. Names are the relic's card name too. */
    public static List<WishNames.Entry> relics(boolean genesis) {
        List<WishNames.Entry> all = relicsAll;
        if (all == null) {
            List<String> ids = new ArrayList<>();
            Map<String, String> tr = new HashMap<>(), tag = new HashMap<>();
            try {
                for (RelicDef d : Relics.all()) {
                    ids.add(d.id());
                    tr.put(d.id(), d.cardName());
                    tag.put(d.id(), String.valueOf(d.grade().ordinal()));
                }
            } catch (RuntimeException | LinkageError e) {
                return List.of();
            }
            all = List.copyOf(WishNames.build(ids, tr, tag));
            relicsAll = all;
        }
        if (all.isEmpty()) return all;
        List<WishNames.Entry> src = all;
        return CUTS.computeIfAbsent("relics:" + genesis, k -> {
            List<WishNames.Entry> out = new ArrayList<>();
            for (WishNames.Entry e : src) if (WishRules.relicGradeAllowed(Integer.parseInt(e.tag()), genesis)) out.add(e);
            return List.copyOf(out);
        });
    }

    // ================================================================ ranks
    /** The rank words a wish understands: the Magic Knight ranks and "rank_class" (class 1 is the best). The Wizard King is not offered: there is one seat on the whole server. */
    public static List<WishNames.Entry> ranks() {
        List<String> ids = new ArrayList<>();
        for (KnightRank r : KnightRank.values()) {
            if (r == KnightRank.WIZARD_KING) continue;
            ids.add(r.name().toLowerCase(java.util.Locale.ROOT));
            if (r.hasClasses) for (int c = 1; c <= 5; c++) ids.add(r.name().toLowerCase(java.util.Locale.ROOT) + "_" + c);
        }
        return CUTS.computeIfAbsent("ranks", k -> {
            List<WishNames.Entry> out = new ArrayList<>();
            for (String id : ids) out.add(new WishNames.Entry(id, WishNames.titleOf(id)));
            return List.copyOf(out);
        });
    }

    /** The step of a typed rank ("senior", "senior 3", "grand"), or -1. A rank without a class means its best class (1). */
    public static int rankStep(String text) {
        String[] w = DbRules.norm(text).split(" ");
        KnightRank rank = null;
        int cls = 1;
        for (String x : w) {
            KnightRank k = KnightRank.byName(x);
            if (k != null && rank == null) rank = k;
            else if (x.length() == 1 && Character.isDigit(x.charAt(0))) cls = Math.max(1, Math.min(5, x.charAt(0) - '0'));
        }
        if (rank == null || rank == KnightRank.WIZARD_KING) return -1;
        return KnightRank.step(rank, rank.hasClasses ? cls : 0);
    }
}
