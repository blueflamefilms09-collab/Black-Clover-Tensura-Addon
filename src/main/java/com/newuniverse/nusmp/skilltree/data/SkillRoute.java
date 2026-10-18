package com.newuniverse.nusmp.skilltree.data;

import java.util.List;

/**
 * Why: a Route is one branch of a tree that the owner's rule says is unlocked as a WHOLE by consuming one Skill Book (the mechanic of The Exiled Heavy Knight Knows How to Game
 * the System: the book does not give stats, it opens the route so that SP can be invested in it). It is one JSON file under {@code data/nusmp/skill_trees/}. It names the book that
 * opens it ({@code book}, an id, because the book item is one registered item whose component says which book it is: items cannot be registered by a datapack, routes can), the
 * class a player becomes while using it ({@code classId}: the player's active Fairy Tail magic), the nodes, and the look. Immutable and free of Minecraft types.
 *
 * @param id          the route id, a resource location string ({@code nusmp:fire_dragon_slayer}), taken from the file name
 * @param magic       the family of magic it belongs to ({@code fairy_tail}), used to group routes in the screen and by the content code; this system never touches the Black Clover grimoire
 * @param name        display name of the route
 * @param desc        what the route is about
 * @param book        the Required Skill Book id ({@code nusmp:fire_dragon_slayer_book}); a book stack carries this id
 * @param bookName    the display name of the book ("Fang of Smoldering Insanity")
 * @param bookLore    one line of lore under the book's name
 * @param color       0xRRGGBB accent of the route and its book
 * @param icon        an item id used as the route's icon
 * @param classId     the id of the class a player is while this is their active route ({@code fire_dragon_slayer})
 * @param duplicateSp the SP a second copy of the book is worth when the route is already unlocked
 * @param lootWeight  the weight of this route's book among the routes in a dungeon drop (1 is common; a higher number is more common; 0 never drops)
 * @param nodes       the nodes, parents before children
 * @param franchise   1.82.0 (Unique Skill Lines): the franchise key of a generated line ({@code tensura}, {@code solo_leveling}, ...), or an empty string for a classic route. A route with a franchise
 *                    is an "extended" route: it may use the wider canvas grid and its nodes carry a {@code kind} from the effect catalogue
 * @param guiBackground the texture drawn behind the tree canvas ({@code nusmp:textures/gui/skill_trees/<id>.png}), or an empty string for the classic look
 * @param bookTexture the item texture of the route's 3D book cover ({@code nusmp:item/skill_book_3d/<franchise>}), or an empty string
 * @param backgroundTheme the theme of the generated background ({@code magma}, {@code void}, ...), used for the fallback colour when the texture is missing, or an empty string
 */
public record SkillRoute(String id, String magic, String name, String desc, String book, String bookName, String bookLore, int color, String icon, String classId,
                         int duplicateSp, int lootWeight, List<SkillNode> nodes, String franchise, String guiBackground, String bookTexture, String backgroundTheme) {

    /**
     * Why: the 13-argument form of the record before 1.82.0. Every old call site (the loader, the tests, the hand-built routes) keeps compiling and means exactly what it meant: a classic route
     * with none of the line fields. Add the new, keep the old.
     */
    public SkillRoute(String id, String magic, String name, String desc, String book, String bookName, String bookLore, int color, String icon, String classId,
                      int duplicateSp, int lootWeight, List<SkillNode> nodes) {
        this(id, magic, name, desc, book, bookName, bookLore, color, icon, classId, duplicateSp, lootWeight, nodes, "", "", "", "");
    }

    /** True for a generated Unique Skill Line (it names a franchise); false for a classic route. */
    public boolean isLine() { return franchise != null && !franchise.isEmpty(); }

    /** The node with this id, or null. */
    public SkillNode node(String nodeId) {
        for (SkillNode n : nodes) if (n.id().equals(nodeId)) return n;
        return null;
    }

    /** The nodes without prerequisites. */
    public List<SkillNode> roots() { return nodes.stream().filter(SkillNode::isRoot).toList(); }

    /** The node that grants this skill id, or null. */
    public SkillNode bySkill(String skillId) {
        for (SkillNode n : nodes) if (n.grantsSkill() && n.skill().equals(skillId)) return n;
        return null;
    }

    /** The SP needed to buy every level of every node. */
    public int totalCost() {
        int sum = 0;
        for (SkillNode n : nodes) sum += n.totalCost(n.maxLevel());
        return sum;
    }

    /** The key under which a node's level is stored in a player's tree: {@code <route>#<node>}. */
    public static String key(String routeId, String nodeId) { return routeId + "#" + nodeId; }
}
