package com.newuniverse.nusmp.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: every report of "my relics do not show" ended at a guess, because the one place the mod meets Curios is reflection and a reflection failure is silent. This walks the very same
 * calls as {@link CuriosLink}, one step at a time, WITHOUT any caching or any pause after a failure, and says for each step what happened (the class, the method, what it returned, the
 * exception if it threw), so {@code /multiverse relic debug} shows the exact step that breaks on the player's own machine. Read only; allocation is not a concern (an operator asked).
 */
public final class CuriosProbe {
    private CuriosProbe() {}

    private static final String API = "top.theillusivec4.curios.api.CuriosApi",
            HANDLER = "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler",
            STACKS = "top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler";

    /** The result of a probe: the lines to print, and whether every step passed. */
    public record Report(List<String> lines, boolean ok, int stacksFound) {}

    /** Walks the Curios reads for this entity. Never throws. */
    public static Report probe(LivingEntity e) {
        List<String> out = new ArrayList<>();
        boolean ok = true;
        int found = 0;
        ModList list = ModList.get();
        if (list == null || !list.isLoaded("curios")) {
            out.add("Curios mod: NOT LOADED (relics can only be worn in a Curios slot with Curios installed; armour relics and hand relics do not need it)");
            return new Report(out, false, 0);
        }
        String version = list.getModContainerById("curios").map(c -> c.getModInfo().getVersion().toString()).orElse("?");
        out.add("Curios mod: loaded, version " + version);
        Class<?> api;
        try {
            api = Class.forName(API);
            out.add("step 1 class CuriosApi: found");
        } catch (Throwable t) {
            out.add("step 1 class CuriosApi: FAILED " + t);
            return new Report(out, false, 0);
        }
        Object handler = null;
        try {
            Method inv = api.getMethod("getCuriosInventory", LivingEntity.class);
            out.add("step 2 method getCuriosInventory(LivingEntity): found, returns " + inv.getReturnType().getSimpleName());
            Object opt = inv.invoke(null, e);
            if (opt instanceof Optional<?> o) {
                if (o.isPresent()) {
                    handler = o.get();
                    out.add("step 3 invoke: an inventory (" + handler.getClass().getSimpleName() + ")");
                } else {
                    out.add("step 3 invoke: EMPTY (this entity has no Curios inventory: no slot is assigned to its entity type)");
                    ok = false;
                }
            } else {
                out.add("step 3 invoke: FAILED returned " + (opt == null ? "null" : opt.getClass().getName()) + " instead of an Optional");
                ok = false;
            }
        } catch (Throwable t) {
            out.add("step 2/3 getCuriosInventory: FAILED " + unwrap(t) + methodHint(api, "getCuriosInventory"));
            return new Report(out, false, 0);
        }
        if (handler == null) return new Report(out, false, 0);
        try {
            Class<?> hc = Class.forName(HANDLER);
            Method eq = hc.getMethod("getEquippedCurios");
            Object res = eq.invoke(handler);
            if (res instanceof IItemHandler h) {
                List<String> names = new ArrayList<>();
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (!s.isEmpty()) names.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
                }
                found = names.size();
                out.add("step 4 getEquippedCurios: ok, " + h.getSlots() + " slots, " + found + " worn " + names);
            } else {
                out.add("step 4 getEquippedCurios: FAILED returned " + (res == null ? "null" : res.getClass().getName()) + ", not an IItemHandler");
                ok = false;
            }
        } catch (Throwable t) {
            out.add("step 4 getEquippedCurios: FAILED " + unwrap(t) + " (the mod then falls back to the per-slot road)");
            ok = false;
        }
        try {
            Class<?> hc = Class.forName(HANDLER), sc = Class.forName(STACKS);
            Object map = hc.getMethod("getCurios").invoke(handler);
            if (map instanceof Map<?, ?> m) {
                StringBuilder b = new StringBuilder();
                boolean renders = true;
                Method stacks = sc.getMethod("getStacks");
                Method rend = null;
                try { rend = sc.getMethod("getRenders"); } catch (NoSuchMethodException none) { renders = false; }
                int n = 0;
                for (Map.Entry<?, ?> en : m.entrySet()) {
                    Object stacksHandler = en.getValue();
                    Object st = stacks.invoke(stacksHandler);
                    int count = 0, slots = 0;
                    if (st instanceof IItemHandler h) {
                        slots = h.getSlots();
                        for (int i = 0; i < slots; i++) if (!h.getStackInSlot(i).isEmpty()) count++;
                    }
                    String eye = "";
                    if (rend != null && rend.invoke(stacksHandler) instanceof List<?> l) eye = " eye=" + l;
                    if (n++ < 14) b.append(' ').append(en.getKey()).append('[').append(count).append('/').append(slots).append(eye).append(']');
                }
                out.add("step 5 per-slot road: " + m.size() + " slot types:" + b + (renders ? "" : " (no getRenders: the eye icon cannot be read, everything is shown)"));
            } else {
                out.add("step 5 getCurios: FAILED returned " + (map == null ? "null" : map.getClass().getName()));
                ok = false;
            }
        } catch (Throwable t) {
            out.add("step 5 per-slot road: FAILED " + unwrap(t));
            ok = false;
        }
        String last = CuriosLink.lastFailure();
        out.add("CuriosLink: " + (CuriosLink.available() ? "reading" : "PAUSED after a failure (retries every 10 s)") + ", " + CuriosLink.failureCount() + " failed reads since start" + (last.isEmpty() ? "" : ", last: " + last));
        return new Report(out, ok, found);
    }

    private static String unwrap(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c != c.getCause()) c = c.getCause();
        return c.getClass().getSimpleName() + ": " + c.getMessage();
    }

    /** When a method is missing: the methods the class does have with a similar name, so the fix is a one-word change. */
    private static String methodHint(Class<?> c, String like) {
        StringBuilder b = new StringBuilder();
        String key = like.substring(0, Math.min(like.length(), 8)).toLowerCase();
        for (Method m : c.getMethods()) if (m.getName().toLowerCase().contains(key)) b.append(' ').append(m.getName()).append('(').append(m.getParameterCount()).append(" args)");
        return b.length() == 0 ? "" : "; similar methods:" + b;
    }
}
