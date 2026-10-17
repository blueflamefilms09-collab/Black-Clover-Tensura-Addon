package net.minecraft.network.chat;

import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/** Why: harness shim. */
public class MutableComponent implements Component {
    private final String text;
    private final List<Component> siblings = new ArrayList<>();
    private int rgb = -1;

    MutableComponent(String text) { this.text = text; }

    public MutableComponent append(Component c) { siblings.add(c); return this; }

    public MutableComponent withStyle(ChatFormatting... f) {
        for (ChatFormatting x : f) if (x.rgb >= 0) rgb = x.rgb;
        return this;
    }

    public MutableComponent withStyle(Style st) { if (st != null && st.rgb >= 0) rgb = st.rgb; return this; }

    @Override public Style getStyle() { return new Style(rgb); }

    public int rgb() { return rgb; }

    @Override
    public String getString() {
        StringBuilder b = new StringBuilder(text);
        for (Component c : siblings) b.append(c.getString());
        return b.toString();
    }

    @Override
    public MutableComponent copy() {
        MutableComponent m = new MutableComponent(text);
        m.siblings.addAll(siblings);
        m.rgb = rgb;
        return m;
    }
}
