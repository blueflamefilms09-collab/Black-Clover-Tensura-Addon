package com.newuniverse.nusmp.skilltree.item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Why: the 3.26.0 look needs the rarity of a Skill Book STACK, and the rule that decides it (the optional data component {@code nusmp:skill_book_rarity}, which wins when present) is built
 * by the rarity work, not by the look. This reader does not import that work: it finds the component type by its id in the data component registry ONCE (a map lookup, cached), reads it
 * with a cast guarded by {@link BookLook#rankOfValue} (null = absent, a text or a number = its rank) and falls back to the shipped table's rank of the book id. So the look works with the
 * rarity work merged or not: without the component type registered it is exactly the table. Common code (no client class); the client property reads it per frame, which costs one registry
 * lookup the first time and one component read after that, with no allocation.
 */
public final class BookLookStack {
    private BookLookStack() {}

    /** The id of the optional component the rarity work registers. */
    public static final ResourceLocation COMPONENT_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "skill_book_rarity");

    private static volatile DataComponentType<?> type;
    private static volatile boolean resolved;

    private static DataComponentType<?> type() {
        if (!resolved) {
            DataComponentType<?> t = BuiltInRegistries.DATA_COMPONENT_TYPE.get(COMPONENT_ID);   // null when the id is not registered (no rarity work in this build)
            type = t;
            resolved = true;
        }
        return type;
    }

    /** The rank held by the stack's own component, or -1 when the stack has none (or the component type does not exist). */
    @SuppressWarnings("unchecked")
    public static int rankOfStack(ItemStack stack) {
        DataComponentType<?> t = type();
        if (t == null || stack == null || stack.isEmpty()) return -1;
        return BookLook.rankOfValue(stack.get((DataComponentType<Object>) t));
    }

    /** The value of the item property {@code nusmp:rarity}: the stack's own component first, else the table's rank of the book id (common for an unknown book). */
    public static float rarityValue(ItemStack stack, String bookId) {
        int r = rankOfStack(stack);
        return r >= 0 ? BookLook.rarityStep(r) : BookLook.rarityValue(bookId);
    }

    /** The rank of a stack for the drop shimmer and other readers: the component, else the table, else 0. */
    public static int rank(ItemStack stack, String bookId) {
        int r = rankOfStack(stack);
        return r >= 0 ? r : BookLook.index().rankOf(bookId);
    }
}
