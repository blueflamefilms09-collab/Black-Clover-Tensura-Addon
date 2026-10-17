package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.GradeTable;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.Pool;
import com.newuniverse.nusmp.relics.RelicComponents2;
import com.newuniverse.nusmp.relics.Relics;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: nothing here can be played by its author, so each part of the standard gear has a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}). They drive the same public entry points the event handlers use and fail loudly with the reason:
 * {@code gear.tables} (the shipped table loads with no complaint, and every base item, every vanilla enchantment and at least some of the Tensura engravings
 * it names exist in THIS game, which is the check a unit test cannot make), {@code gear.roll} (300 pieces of all three grades come out of the real factory:
 * each carries the component with the right grade, a name in the relic tier's colour and a lore line, no relic mark of any kind, Rare armour its resistance
 * and its resistance line, the sword or armour keeps its OWN attack damage or armour next to the new bonus, the enchantments and engravings are on the
 * stack; and the roller, run thirty thousand times at floor 40, produces all three grades), {@code gear.drop} (eight thousand zombie kills by a fake player in
 * sector 0, or anywhere in the Mega level at floor 1 when no sector exists yet, go through {@link GearDrops#process} as real {@code LivingDropsEvent}s: never more than one piece per kill, always a graded stack, the count
 * and the grade mix within six standard deviations of the table; a death no player caused, a boss, a servant drop nothing; a tagged sector Guardian drops
 * exactly one Rare piece) and {@code gear.resist} (a fake player wears and holds Rare pieces: the resistance is the sum of the right pieces, held to the
 * ceiling; spells of vanilla and of Tensura, by their REAL registry keys, are cut and a fall, a blade and a poison are not); the fifth, {@code gear.chest},
 * is {@link ChestSelfTests}. Each test skips with an OK line
 * when its precondition is a configuration choice and FAILS when the world is wrong (no Mega dimension). Everything a test touches (the fake
 * player's equipment, the tags put on the zombie) is restored in a {@code finally}.
 */
public final class GearSelfTests {
    private GearSelfTests() {}

    private static final String PLAYER = "gear_tester";

    /** Registers the tests; called once from {@code GearRegistry.init}. */
    public static void register() {
        SelfTests.register("gear.tables", GearSelfTests::tables);
        SelfTests.register("gear.roll", GearSelfTests::roll);
        SelfTests.register("gear.drop", GearSelfTests::drop);
        SelfTests.register("gear.resist", GearSelfTests::resist);
        ChestSelfTests.register();                    // 1.55.0 (chests): gear.chest
    }

    /**
     * Does the stack carry any mark of the relic system: one of the four relic components, or a relic definition? Standard gear must carry none, whoever made it
     * (the drops, the chests, the command). Lives here because this is the one class of the package allowed to import the relic classes (a unit test guards that).
     */
    static boolean carriesRelicMark(ItemStack stack) {
        return stack.has(RelicComponents2.SERIAL.get()) || stack.has(RelicComponents2.ATTUNED.get()) || stack.has(RelicComponents2.USES.get())
                || stack.has(RelicComponents2.STATE.get()) || Relics.defOf(stack) != null;
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static String firstFew(List<String> problems) {
        return problems.size() + " problem(s): " + String.join(" | ", problems.subList(0, Math.min(4, problems.size())));
    }

    // ------------------------------------------------------------------------------------------------ gear.tables
    private static SelfTests.Result tables(MinecraftServer server) {
        GearTables t = GearTablesReload.tables();
        List<String> problems = GearTablesReload.problems();
        if (!t.isUsable()) return fail("the gear tables are empty or unusable: " + (problems.isEmpty() ? "no problem was recorded" : firstFew(problems)));
        if (!problems.isEmpty()) return fail("tables.json: " + firstFew(problems));
        String source = GearTablesReload.source();
        if (!GearTablesReload.FROM_DATA.equals(source)) return fail("the tables in force came from the " + source + ", not from a data reload: the reload listener is not registered, or the data file is missing or unusable (see the log)");
        HolderLookup.RegistryLookup<Enchantment> lookup = server.overworld().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> bad = new ArrayList<>();
        Set<String> found = new TreeSet<>(), absent = new TreeSet<>();
        int items = 0, enchants = 0;
        for (GearGrade g : GearGrade.values()) {
            GradeTable gt = t.grade(g);
            for (GearSlot s : GearSlot.values()) {
                for (String id : gt.items(s)) {
                    items++;
                    ResourceLocation rl = ResourceLocation.tryParse(id);
                    Item item = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                    if (item == null || item == Items.AIR) bad.add(g.id() + "/" + s.id() + ": the item " + id + " is not registered");
                }
                for (Pool p : gt.enchantPool(s)) {
                    enchants++;
                    if (!exists(lookup, p.id())) bad.add(g.id() + "/" + s.id() + ": the enchantment " + p.id() + " does not exist");
                }
                for (Pool p : gt.engravePool(s)) (exists(lookup, p.id()) ? found : absent).add(p.id());
            }
        }
        if (!bad.isEmpty()) return fail(firstFew(bad));
        absent.removeAll(found);
        if (found.isEmpty() && !absent.isEmpty()) return fail("none of the Tensura engravings the table names exists in this game (" + absent + "): Special and Rare weapons would carry none");
        return ok("read by the data reload; " + items + " base items and " + enchants + " vanilla enchantment entries resolve; Tensura engravings found " + found.size() + ", absent " + absent);
    }

    private static boolean exists(HolderLookup.RegistryLookup<Enchantment> lookup, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && lookup.get(ResourceKey.create(Registries.ENCHANTMENT, rl)).isPresent();
    }

    // ------------------------------------------------------------------------------------------------ gear.roll
    private static SelfTests.Result roll(MinecraftServer server) {
        GearTables t = GearTablesReload.tables();
        if (!t.isUsable()) return fail("the gear tables are empty or unusable");
        RegistryAccess access = server.overworld().registryAccess();
        HolderLookup.RegistryLookup<Enchantment> lookup = access.lookupOrThrow(Registries.ENCHANTMENT);
        Random random = new Random(1055L);
        Dice dice = random::nextDouble;
        boolean engravings = GearConfig.get(GearConfig.ENGRAVINGS);
        int made = 0, rareArmour = 0, bonuses = 0, engraved = 0, durable = 0;
        for (int i = 0; i < 300; i++) {
            GearGrade grade = GearGrade.values()[i % 3];
            int floor = 1 + (i % 40);
            GearSpec spec = GearRoller.make(dice, grade, null, t).orElse(null);
            if (spec == null) return fail("floor " + floor + ": the roller made no " + grade.id() + " piece");
            ItemStack stack = GearFactory.create(spec, access);
            String what = "floor " + floor + " " + grade.id() + " " + spec.slot().id() + " (" + spec.item() + "): ";
            if (stack.isEmpty()) return fail(what + "the factory returned nothing");
            made++;
            GearInfo info = stack.get(GearComponents.GEAR.get());
            if (info == null) return fail(what + "the stack has no nusmp:gear_grade component");
            if (info.grade() != grade || !info.equals(spec.info())) return fail(what + "the component says " + info + " but the spec says " + spec.info());
            if (stack.has(RelicComponents2.SERIAL.get()) || stack.has(RelicComponents2.ATTUNED.get()) || stack.has(RelicComponents2.USES.get()) || stack.has(RelicComponents2.STATE.get()))
                return fail(what + "carries a relic component: standard gear must never be a relic");
            if (Relics.defOf(stack) != null) return fail(what + "the relic catalog recognises it as a relic");
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            TextColor want = TextColor.fromLegacyFormat(GearLook.relicGrade(grade).colour());
            if (name == null || name.getStyle().getColor() == null || !name.getStyle().getColor().equals(want)) return fail(what + "the name is not in the colour of the " + grade.id() + " relic grade");
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null || lore.lines().isEmpty() || !hasLine(lore, "line.grade")) return fail(what + "the lore has no 'Standard Gear' line");
            if (grade == GearGrade.RARE) {
                if (info.magicResistPercent() < 4 || !hasLine(lore, "line.resist")) return fail(what + "a Rare piece has no magic resistance line");
                if (spec.slot().isArmour()) {
                    if (info.magicResistPercent() < 5 || info.magicResistPercent() > 12) return fail(what + "Rare armour resists " + info.magicResistPercent() + " percent, outside 5..12");
                    rareArmour++;
                }
            } else if (info.magicResistPercent() != 0 || hasLine(lore, "line.resist")) return fail(what + "only Rare gear resists magic");
            if (spec.durabilityPercent() > 0 && stack.getMaxDamage() <= new ItemStack(stack.getItem()).getMaxDamage()) return fail(what + "the durability bonus did not raise the maximum durability");
            if (spec.durabilityPercent() > 0) durable++;
            if (spec.powerPercent() > 0 && spec.slot().hasPowerStat()) {
                String problem = checkBonus(stack, spec);
                if (problem != null) return fail(what + problem);
                bonuses++;
            }
            for (GearSpec.Ench e : spec.enchants()) if (level(stack, lookup, e.id()) < 1) return fail(what + "the enchantment " + e.id() + " is not on the stack");
            if (engravings) {
                for (GearSpec.Ench e : spec.engravings()) {
                    if (!exists(lookup, e.id())) continue;
                    if (level(stack, lookup, e.id()) < 1) return fail(what + "the engraving " + e.id() + " exists but is not on the stack");
                    engraved++;
                }
            } else {
                for (GearSpec.Ench e : spec.engravings()) if (e.id().startsWith("tensura:") && level(stack, lookup, e.id()) > 0) return fail(what + "engravings are off in the config but " + e.id() + " is on the stack");
            }
        }
        // the roller itself, many times, at floor 40: every grade the rates allow must turn up
        GearRates rates = GearConfig.rates();
        int[] seen = new int[3];
        for (int i = 0; i < 30_000; i++) GearRoller.roll(dice, 40, 0, t, rates).ifPresent(s -> seen[s.grade().ordinal()]++);
        for (GearGrade g : GearGrade.values()) {
            if (rates.base(g) > 0 && t.offers(g) && seen[g.ordinal()] == 0) return fail("30000 rolls at floor 40 produced no " + g.id() + " piece (chance " + GearRoller.chances(rates, t, 40, 0)[g.ordinal()] + ")");
        }
        return ok(made + " pieces made (" + rareArmour + " Rare armour with resistance, " + bonuses + " with a percentage bonus that kept the base stat, " + durable + " with extra durability, "
                + engraved + " engravings on the stacks); 30000 rolls at floor 40 gave Normal " + seen[0] + ", Special " + seen[1] + ", Rare " + seen[2]);
    }

    private static boolean hasLine(ItemLore lore, String suffix) {
        for (Component c : lore.lines()) {
            if (c.getContents() instanceof TranslatableContents tc && tc.getKey().equals("message.nusmp.mega.mgr." + suffix)) return true;
        }
        return false;
    }

    private static int level(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return 0;
        Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, rl));
        return h.isEmpty() ? 0 : stack.getEnchantments().getLevel(h.get());
    }

    /** The finished sword, axe or armour piece still has the base item's own attack damage or armour AND the new bonus under the slot's fixed id; null when it does. */
    private static String checkBonus(ItemStack stack, GearSpec spec) {
        Holder<Attribute> attribute = spec.slot().isArmour() ? Attributes.ARMOR : Attributes.ATTACK_DAMAGE;
        EquipmentSlotGroup group = switch (spec.slot()) {
            case HELMET -> EquipmentSlotGroup.HEAD;
            case CHESTPLATE -> EquipmentSlotGroup.CHEST;
            case LEGGINGS -> EquipmentSlotGroup.LEGS;
            case BOOTS -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.MAINHAND;
        };
        double before = GearFactory.flat(GearFactory.baseModifiers(new ItemStack(stack.getItem())), attribute, group);
        ItemAttributeModifiers after = GearFactory.baseModifiers(stack);
        double now = GearFactory.flat(after, attribute, group);
        if (before <= 0.0) return "the base item has no " + spec.slot().id() + " stat to build on (" + before + "): the test or the table is wrong";
        if (now <= before) return "the bonus is missing: the stat is " + now + " and the base item's own is " + before + " (the item's default modifiers may have been replaced)";
        int own = 0;
        for (ItemAttributeModifiers.Entry e : after.modifiers()) if (e.modifier().id().equals(GearFactory.modifierId(spec.slot()))) own++;
        if (own != 1) return "expected exactly one modifier with the id " + GearFactory.modifierId(spec.slot()) + " but found " + own;
        return null;
    }

    // ------------------------------------------------------------------------------------------------ gear.drop
    private static SelfTests.Result drop(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("no mega dimension");
        if (!GearConfig.get(GearConfig.ENABLED)) return ok("skipped: gear.enabled is false in the config");
        GearTables t = GearTablesReload.tables();
        if (!t.isUsable()) return fail("the gear tables are empty or unusable");
        // In the middle of sector 0 when it exists; otherwise anywhere in the Mega level, where the floor is 0 and counts as floor 1 (the test needs no sector).
        Sector s = MegaData.get(server).sector(0);
        BlockPos at = s != null && !s.rooms.isEmpty() ? s.rooms.get(0).center() : BlockPos.ZERO.above(100);
        int floor = MegaDungeon.floorAt(level, at);
        ServerPlayer killer = SelfTests.player(level, PLAYER, at.getX(), at.getY(), at.getZ());
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) return fail("a test zombie could not be created");
        zombie.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        try {
            GearRates rates = GearConfig.rates();
            double[] p = GearRoller.chances(rates, t, floor, 0);
            double total = GearRoller.total(p);
            int kills = 8000;
            Dice dice = new Random(77L)::nextDouble;
            DamageSource byPlayer = level.damageSources().playerAttack(killer);
            int pieces = 0;
            int[] byGrade = new int[3];
            for (int i = 0; i < kills; i++) {
                List<ItemEntity> out = new ArrayList<>();
                int n = GearDrops.process(new LivingDropsEvent(zombie, byPlayer, out, true), dice);
                if (n > 1 || out.size() != n) return fail("one kill must drop at most one piece but process() returned " + n + " and the drops hold " + out.size());
                if (n == 1) {
                    GearInfo info = out.get(0).getItem().get(GearComponents.GEAR.get());
                    if (info == null) return fail("the dropped item is not standard gear: " + out.get(0).getItem());
                    pieces++;
                    byGrade[info.grade().ordinal()]++;
                }
            }
            double sigma = Math.sqrt(kills * total * (1 - total));
            if (Math.abs(pieces - kills * total) > 6 * sigma + 5) return fail(pieces + " pieces in " + kills + " kills at floor " + floor + ", the table says " + String.format(java.util.Locale.ROOT, "%.1f", kills * total) + " +- " + String.format(java.util.Locale.ROOT, "%.1f", 6 * sigma));
            for (GearGrade g : GearGrade.values()) {
                double pg = p[g.ordinal()], sg = Math.sqrt(kills * pg * (1 - pg));
                if (Math.abs(byGrade[g.ordinal()] - kills * pg) > 6 * sg + 5) return fail(g.id() + ": " + byGrade[g.ordinal()] + " pieces in " + kills + " kills, the table says " + String.format(java.util.Locale.ROOT, "%.1f", kills * pg));
            }
            DamageSource nobody = level.damageSources().generic();
            for (int i = 0; i < 300; i++) {
                List<ItemEntity> out = new ArrayList<>();
                if (GearDrops.process(new LivingDropsEvent(zombie, nobody, out, false), dice) != 0 || !out.isEmpty()) return fail("a death no player caused still dropped gear");
            }
            for (String tag : new String[] {"nusmp_peak_boss", "nusmp_wanderer", "nusmp_servant"}) {
                zombie.addTag(tag);
                try {
                    for (int i = 0; i < 400; i++) {
                        List<ItemEntity> out = new ArrayList<>();
                        if (GearDrops.process(new LivingDropsEvent(zombie, byPlayer, out, true), dice) != 0 || !out.isEmpty()) return fail("a zombie tagged '" + tag + "' dropped standard gear");
                    }
                } finally {
                    zombie.removeTag(tag);
                }
            }
            zombie.addTag(GearDrops.GUARDIAN_TAG);
            try {
                boolean guaranteed = GearConfig.get(GearConfig.BOSS_GUARANTEED);
                for (int i = 0; i < 60; i++) {
                    List<ItemEntity> out = new ArrayList<>();
                    int n = GearDrops.process(new LivingDropsEvent(zombie, byPlayer, out, true), dice);
                    if (!guaranteed) {
                        if (n != 0) return fail("bossGuaranteed is false but the Guardian dropped gear");
                        continue;
                    }
                    GearInfo info = n == 1 && out.size() == 1 ? out.get(0).getItem().get(GearComponents.GEAR.get()) : null;
                    if (info == null || info.grade() != GearGrade.RARE) return fail("the tagged Guardian must drop exactly one Rare piece, got " + out.size() + " item(s)");
                }
            } finally {
                zombie.removeTag(GearDrops.GUARDIAN_TAG);
            }
            String dm = dungeonMonsters(level, at, byPlayer, total, dice);
            if (dm != null) return fail(dm);
            return ok(kills + " zombie kills at floor " + FloorMap.label(floor) + (floor == 0 ? " (no sector here, counted as floor 1)" : "") + ": " + pieces + " pieces (table " + String.format(java.util.Locale.ROOT, "%.1f", kills * total) + "), Normal " + byGrade[0] + " Special " + byGrade[1] + " Rare " + byGrade[2]
                    + "; never more than one per kill; no gear from a player-less death, a boss, a wanderer or a servant; the Guardian drops one Rare piece; a Dungeon Master creature that is not an Enemy pays like a zombie, a plain animal never");
        } finally {
            zombie.discard();
        }
    }

    /**
     * Tensura's monsters are tamable animals and not {@code Enemy}: a creature of the Dungeon Master (scoreboard tag {@code nusmp_dm}) must pay gear at the table's
     * rate like a zombie does, and the same animal without the tag never. A cow stands in for them (it is no Enemy and no boss). Null when all is right.
     */
    private static String dungeonMonsters(ServerLevel level, BlockPos at, DamageSource byPlayer, double chance, Dice dice) {
        Cow cow = EntityType.COW.create(level);
        if (cow == null) return "a test cow could not be created";
        cow.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        try {
            for (int i = 0; i < 400; i++) {
                List<ItemEntity> out = new ArrayList<>();
                if (GearDrops.process(new LivingDropsEvent(cow, byPlayer, out, true), dice) != 0 || !out.isEmpty()) return "an ordinary animal (no Enemy, no nusmp_dm tag) dropped standard gear";
            }
            cow.addTag(MegaDungeon.DM_TAG);
            int kills = 6000, pieces = 0;
            for (int i = 0; i < kills; i++) {
                List<ItemEntity> out = new ArrayList<>();
                int n = GearDrops.process(new LivingDropsEvent(cow, byPlayer, out, true), dice);
                if (n > 1 || out.size() != n) return "one kill of a Dungeon Master creature must drop at most one piece but process() returned " + n;
                pieces += n;
            }
            double sigma = Math.sqrt(kills * chance * (1 - chance));
            if (Math.abs(pieces - kills * chance) > 6 * sigma + 5)
                return "a Dungeon Master creature that is no Enemy gave " + pieces + " pieces in " + kills + " kills, the table says " + String.format(java.util.Locale.ROOT, "%.1f", kills * chance) + " (is MegaDungeon.isDungeonMonster consulted?)";
            return null;
        } finally {
            cow.discard();
        }
    }

    // ------------------------------------------------------------------------------------------------ gear.resist
    private static final EquipmentSlot[] POSITIONS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private static SelfTests.Result resist(MinecraftServer server) {
        int cap = GearConfig.get(GearConfig.MAX_RESIST);
        if (cap <= 0) return ok("skipped: gear.maxResist is 0 in the config (the resistance is switched off)");
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, PLAYER + "_resist", 0, 100, 0);
        Map<EquipmentSlot, ItemStack> saved = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot e : POSITIONS) saved.put(e, p.getItemBySlot(e).copy());
        RegistryAccess access = level.registryAccess();
        try {
            for (EquipmentSlot e : POSITIONS) p.setItemSlot(e, ItemStack.EMPTY);
            if (GearEvents.rawResist(p) != 0) return fail("a player wearing nothing resists " + GearEvents.rawResist(p));
            DamageSource magic = level.damageSources().magic(), fall = level.damageSources().fall();

            ItemStack chest = piece(GearSlot.CHESTPLATE, "minecraft:diamond_chestplate", 10, access);
            p.setItemSlot(EquipmentSlot.CHEST, chest);
            int want = Math.min(10, cap);
            if (GearEvents.rawResist(p) != 10) return fail("a Rare chestplate of 10 percent counts " + GearEvents.rawResist(p));
            float cut = GearEvents.reduced(p, magic, 20f, cap);
            if (Math.abs(cut - 20f * (100 - want) / 100f) > 1e-3f) return fail("magic damage 20 became " + cut + ", expected " + 20f * (100 - want) / 100f);
            if (GearEvents.reduced(p, level.damageSources().indirectMagic(null, null), 20f, cap) != cut) return fail("indirect magic is not cut like magic");
            if (GearEvents.reduced(p, fall, 20f, cap) != 20f) return fail("fall damage was cut");
            if (GearEvents.reduced(p, level.damageSources().generic(), 20f, cap) != 20f) return fail("generic damage was cut");
            if (GearEvents.reduced(p, magic, 0f, cap) != 0f) return fail("zero damage changed");

            p.setItemSlot(EquipmentSlot.MAINHAND, chest.copy());
            if (GearEvents.rawResist(p) != 10) return fail("a chestplate carried in the hand counts: the sum is " + GearEvents.rawResist(p) + ", it must stay 10");
            p.setItemSlot(EquipmentSlot.MAINHAND, piece(GearSlot.SWORD, "minecraft:diamond_sword", 6, access));
            p.setItemSlot(EquipmentSlot.OFFHAND, piece(GearSlot.SHIELD, "minecraft:shield", 5, access));
            if (GearEvents.rawResist(p) != 21) return fail("chestplate 10 + sword 6 + shield 5 should be 21, got " + GearEvents.rawResist(p));
            p.setItemSlot(EquipmentSlot.HEAD, piece(GearSlot.BOOTS, "minecraft:iron_boots", 12, access));
            if (GearEvents.rawResist(p) != 21) return fail("boots worn on the head count: the sum is " + GearEvents.rawResist(p) + ", it must stay 21");
            p.setItemSlot(EquipmentSlot.HEAD, piece(GearSlot.HELMET, "minecraft:iron_helmet", 12, access));
            p.setItemSlot(EquipmentSlot.LEGS, piece(GearSlot.LEGGINGS, "minecraft:iron_leggings", 12, access));
            p.setItemSlot(EquipmentSlot.FEET, piece(GearSlot.BOOTS, "minecraft:iron_boots", 12, access));
            int sum = 10 + 12 + 12 + 12 + 6 + 5;
            if (GearEvents.rawResist(p) != sum) return fail("the full set should add up to " + sum + ", got " + GearEvents.rawResist(p));
            int shown = Math.min(sum, cap);
            if (GearEvents.resistPercent(p, cap) != shown) return fail("the ceiling " + cap + " should hold " + sum + " to " + shown + ", got " + GearEvents.resistPercent(p, cap));
            if (Math.abs(GearEvents.reduced(p, magic, 100f, cap) - (100f - shown)) > 1e-3f) return fail("100 points of magic should become " + (100 - shown) + ", got " + GearEvents.reduced(p, magic, 100f, cap));

            String spells = realTypes(level, p, cap);
            if (spells != null) return fail(spells);
            return ok("pieces add up as armour worn plus weapon and shield held (" + sum + "), held to the ceiling " + cap + " (" + shown + "); vanilla and Tensura spells cut, fall, blade and poison not");
        } finally {
            for (EquipmentSlot e : POSITIONS) p.setItemSlot(e, saved.get(e));
        }
    }

    /** A Rare piece of the slot with exactly this resistance, made by the real factory. */
    private static ItemStack piece(GearSlot slot, String item, int resist, RegistryAccess access) {
        return GearFactory.create(new GearSpec(GearGrade.RARE, slot, item, 25, 30, List.of(), List.of(), resist), access);
    }

    /** The real Tensura damage types, from their registry keys: spells are cut, a physical skill and a poison are not. Null when all is right. */
    private static String realTypes(ServerLevel level, FakePlayer p, int cap) {
        List<ResourceKey<DamageType>> spells = List.of(TensuraDamageTypes.FIRE_ELEMENTAL, TensuraDamageTypes.LIGHT_ELEMENTAL, TensuraDamageTypes.HOLY_DAMAGE, TensuraDamageTypes.MAGIC_GENERIC);
        List<ResourceKey<DamageType>> others = List.of(TensuraDamageTypes.BULLET, TensuraDamageTypes.SUFFOCATE, TensuraDamageTypes.MAGICULE_POISON, TensuraDamageTypes.SEVERER_BLADE);
        for (ResourceKey<DamageType> k : spells) {
            DamageSource src;
            try { src = TensuraDamageTypes.getDamageSource(level, k); } catch (RuntimeException e) { return "the Tensura damage type " + k.location() + " is not registered: " + e; }
            if (!(GearEvents.reduced(p, src, 100f, cap) < 100f)) return "the Tensura spell " + k.location() + " was not cut";
        }
        for (ResourceKey<DamageType> k : others) {
            DamageSource src;
            try { src = TensuraDamageTypes.getDamageSource(level, k); } catch (RuntimeException e) { return "the Tensura damage type " + k.location() + " is not registered: " + e; }
            if (GearEvents.reduced(p, src, 100f, cap) != 100f) return "the Tensura type " + k.location() + " is not a spell but was cut";
        }
        return null;
    }
}
