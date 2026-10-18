import com.newuniverse.nusmp.client.skilltree.layout.ButtonPlacement;
import com.newuniverse.nusmp.client.skilltree.layout.NodeState;
import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.skilltree.layout.ScreenLayout;
import com.newuniverse.nusmp.client.skilltree.layout.TreeLayout;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.SimpleTreeState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the preview tool (preview_skilltree.py) draws a mock of the Skill Tree screen with Pillow, and a mock that re-implements the layout numbers by hand drifts from the Java the
 * day somebody changes a constant. This program is the cure: it runs the REAL pure layout classes of the screen (ScreenLayout, TreeLayout, NodeState with the core's TreeRules, ButtonPlacement, which have no
 * Minecraft imports) on a route described on standard input and prints every rectangle, node position, node state and routed line as JSON, so the Python side only has to draw
 * what Java computed. It lives under tools/ (not in the mod's source set) because it is a developer tool, and it has no dependencies: the JSON is written by hand.
 * <p>
 * Input, one directive per line:
 * <pre>
 *   size W H                      the GUI size
 *   label PIXELS                  the width of the menu button's label (the preview's font width)
 *   route ID RRGGBB UNLOCKED SP   the route id, its colour, 1 or 0, the player's SP
 *   node ID X Y COST MAX PARENTS COSTS    PARENTS is "-" or "a:3,b:2"; COSTS is "-" or "2,3,4"
 *   level NODEID LEVEL            a node level the player has
 * </pre>
 */
public final class LayoutDump {
    private LayoutDump() {}

    public static void main(String[] args) throws IOException {
        int w = 427, h = 240, label = 45;
        String routeId = "nusmp:sample", colour = "E8541C";
        boolean unlocked = true;
        int sp = 0;
        List<SkillNode> nodes = new ArrayList<>();
        Map<String, Integer> levelsById = new HashMap<>();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        for (String line; (line = in.readLine()) != null; ) {
            String[] p = line.trim().split("\\s+");
            if (p.length == 0 || p[0].isEmpty() || p[0].startsWith("#")) continue;
            switch (p[0]) {
                case "size" -> { w = Integer.parseInt(p[1]); h = Integer.parseInt(p[2]); }
                case "label" -> label = Integer.parseInt(p[1]);
                case "route" -> { routeId = p[1]; colour = p[2]; unlocked = p[3].equals("1"); sp = Integer.parseInt(p[4]); }
                case "node" -> {
                    List<SkillNode.Parent> parents = new ArrayList<>();
                    if (!p[6].equals("-")) {
                        for (String part : p[6].split(",")) {
                            int c = part.indexOf(':');
                            parents.add(c < 0 ? new SkillNode.Parent(part, 1) : new SkillNode.Parent(part.substring(0, c), Integer.parseInt(part.substring(c + 1))));
                        }
                    }
                    List<Integer> costs = new ArrayList<>();
                    if (p.length > 7 && !p[7].equals("-")) for (String c : p[7].split(",")) costs.add(Integer.parseInt(c));
                    nodes.add(new SkillNode(p[1], p[1], "", parents, Integer.parseInt(p[4]), costs, Integer.parseInt(p[5]), "", List.of(), Map.of(), Integer.parseInt(p[2]),
                            Integer.parseInt(p[3]), ""));
                }
                case "level" -> levelsById.put(p[1], Integer.parseInt(p[2]));
                default -> throw new IllegalArgumentException("unknown directive: " + line);
            }
        }
        SkillRoute route = new SkillRoute(routeId, "fairy_tail", routeId, "", routeId + "_book", "Book", "", Integer.parseInt(colour, 16), "minecraft:book", "sample", 3, 1, nodes);
        SimpleTreeState player = new SimpleTreeState().withSp(sp);                      // the core's in-memory player: the same rules the server runs answer for it
        if (unlocked) player.unlock(routeId);
        for (Map.Entry<String, Integer> e : levelsById.entrySet()) player.setLevel(routeId, e.getKey(), e.getValue());

        ScreenLayout lay = ScreenLayout.of(w, h);
        TreeLayout tl = TreeLayout.of(nodes);
        float[] view = tl.initialView(lay.canvas.w(), lay.canvas.h());
        float[] fit = tl.fitView(lay.canvas.w(), lay.canvas.h());

        StringBuilder o = new StringBuilder();
        o.append("{\n  \"screen\": [").append(w).append(", ").append(h).append("],\n  \"layout\": {");
        String[] names = {"panel", "header", "routes", "routeList", "strip", "canvas", "detail", "footer", "closeButton", "setActiveButton", "buyButton", "detailText", "zoomOut", "zoomIn", "zoomFit"};
        Rect[] rects = {lay.panel, lay.header, lay.routes, lay.routeList, lay.strip, lay.canvas, lay.detail, lay.footer, lay.closeButton, lay.setActiveButton, lay.buyButton, lay.detailText, lay.zoomOut,
                lay.zoomIn, lay.zoomFit};
        for (int i = 0; i < names.length; i++) o.append(i == 0 ? "\n    " : ",\n    ").append('"').append(names[i]).append("\": ").append(rect(rects[i]));
        o.append("\n  },\n  \"tree\": {\n    \"contentW\": ").append(tl.contentWidth()).append(", \"contentH\": ").append(tl.contentHeight());
        o.append(",\n    \"initialView\": ").append(floats(view)).append(",\n    \"fitView\": ").append(floats(fit)).append(",\n    \"nodes\": [");
        for (int i = 0; i < nodes.size(); i++) {
            NodeState st = NodeState.of(player, route, nodes.get(i));
            Rect b = tl.box(i);
            o.append(i == 0 ? "\n      " : ",\n      ").append("{\"id\": \"").append(nodes.get(i).id()).append("\", \"cx\": ").append(tl.centerX(i)).append(", \"cy\": ").append(tl.centerY(i))
                    .append(", \"box\": ").append(rect(b)).append(", \"status\": \"").append(st.status()).append("\", \"level\": ").append(st.level()).append(", \"max\": ").append(st.max())
                    .append(", \"price\": ").append(st.price()).append(", \"verdict\": \"").append(st.verdict()).append("\", \"canBuy\": ").append(st.canBuy()).append('}');
        }
        o.append("\n    ],\n    \"edges\": [");
        List<TreeLayout.Edge> edges = tl.edges(nodes);
        for (int i = 0; i < edges.size(); i++) {
            TreeLayout.Edge e = edges.get(i);
            o.append(i == 0 ? "\n      " : ",\n      ").append("{\"parent\": ").append(e.parent()).append(", \"child\": ").append(e.child()).append(", \"req\": ").append(e.requiredLevel())
                    .append(", \"points\": ").append(ints(e.points())).append('}');
        }
        o.append("\n    ]\n  },\n  \"buttons\": {");
        ButtonPlacement.Menu[] menus = ButtonPlacement.Menu.values();
        for (int i = 0; i < menus.length; i++)
            o.append(i == 0 ? "\n    " : ",\n    ").append('"').append(menus[i]).append("\": ").append(rect(ButtonPlacement.place(menus[i], w, h, label, List.of())));
        o.append("\n  }\n}\n");
        System.out.print(o);
    }

    private static String rect(Rect r) { return "[" + r.x() + ", " + r.y() + ", " + r.w() + ", " + r.h() + "]"; }

    private static String ints(int[] a) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) b.append(i == 0 ? "" : ", ").append(a[i]);
        return b.append(']').toString();
    }

    private static String floats(float[] a) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) b.append(i == 0 ? "" : ", ").append(a[i]);
        return b.append(']').toString();
    }
}
