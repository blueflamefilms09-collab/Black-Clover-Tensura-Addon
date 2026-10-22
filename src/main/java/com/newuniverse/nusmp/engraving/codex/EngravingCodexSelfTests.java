package com.newuniverse.nusmp.engraving.codex;

import com.newuniverse.nusmp.franchise.FranchiseData;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The CI self-test {@code item.engraving_codex} (server smoke test, runs on a dedicated server: no client class is touched): the item is registered and gets the unified tooltip, the book
 * builds from the running server's registries with every franchise engraving exactly once and every entry named and described, the mod's own enchantments and Tensura's engravings are
 * in it, the search and the group filter answer, the pages pack, and the command node exists. Pure reading: nothing in the world is changed.
 */
public final class EngravingCodexSelfTests {
    private EngravingCodexSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("item.engraving_codex", EngravingCodexSelfTests::run);
    }

    private static SelfTests.Result run(MinecraftServer server) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("nusmp", "engraving_codex"));
        if (item.isEmpty()) return SelfTests.Result.fail("nusmp:engraving_codex is not registered");
        ItemStack stack = new ItemStack(item.get());
        if (stack.isEmpty() || !UnifiedTooltipItems.applies(stack)) return SelfTests.Result.fail("the codex gets no unified tooltip");
        if (!"engravings".equals(EngravingCodexCommand.node().getLiteral())) return SelfTests.Result.fail("the command node is not 'engravings'");

        long t0 = System.nanoTime();
        CodexEntries.Book book = CodexGather.book(server.registryAccess(), k -> null);            // the server has no mod language file: the data fallbacks must carry the book
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        FranchiseData.Table table = FranchiseData.table();
        Set<String> keys = new HashSet<>();
        int franchise = 0, mod = 0, tensura = 0;
        for (CodexEntries.Entry e : book.entries()) {
            if (!keys.add(e.key())) return SelfTests.Result.fail("duplicate entry " + e.key());
            if (e.name() == null || e.name().isBlank()) return SelfTests.Result.fail(e.key() + " has no name");
            if (e.text() == null || e.text().isBlank()) return SelfTests.Result.fail(e.key() + " has no text");
            switch (e.kind()) {
                case FRANCHISE -> franchise++;
                case MOD -> mod++;
                case TENSURA -> tensura++;
            }
        }
        if (franchise != table.engravings().size()) return SelfTests.Result.fail("franchise entries " + franchise + " but the table has " + table.engravings().size());
        for (String id : table.engravings().keySet()) if (!keys.contains("nusmp:" + id)) return SelfTests.Result.fail(id + " is missing from the book");
        if (!keys.contains("nusmp:anti_magic") || !keys.contains("nusmp:legacy_of_the_wizard_kings")) return SelfTests.Result.fail("the mod's own enchantments are missing");
        if (tensura < 10) return SelfTests.Result.fail("only " + tensura + " Tensura engravings");
        int groupTotal = 0;
        for (CodexEntries.Group g : book.groups()) groupTotal += g.count();
        if (groupTotal != book.entries().size()) return SelfTests.Result.fail("group counts " + groupTotal + " != entries " + book.entries().size());

        // the filter answers
        int[] all = CodexFilter.select(book.entries(), null, "");
        if (all.length != book.entries().size()) return SelfTests.Result.fail("the empty search hides entries");
        int[] one = CodexFilter.select(book.entries(), null, "Falna");
        if (one.length == 0 || !book.entries().get(one[0]).key().equals("nusmp:fr_falna")) return SelfTests.Result.fail("the search for Falna does not lead with Falna");
        int[] inGroup = CodexFilter.select(book.entries(), "danmachi", "");
        if (inGroup.length != table.of("danmachi").size()) return SelfTests.Result.fail("the danmachi group has " + inGroup.length + " entries");
        int[] heights = new int[all.length];
        java.util.Arrays.fill(heights, 80);
        List<int[]> pages = CodexFilter.paginate(heights, 200, 6);
        if (pages.isEmpty() || pages.get(0)[1] < 1) return SelfTests.Result.fail("the pages do not pack");
        return SelfTests.Result.ok(book.entries().size() + " entries (" + franchise + " franchise, " + mod + " mod, " + tensura + " Tensura) in " + book.groups().size() + " groups, built in " + ms + " ms, "
                + pages.size() + " pages at 80 px");
    }
}
