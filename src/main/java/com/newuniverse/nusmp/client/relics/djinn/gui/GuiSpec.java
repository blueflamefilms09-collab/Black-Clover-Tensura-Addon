package com.newuniverse.nusmp.client.relics.djinn.gui;

import com.newuniverse.nusmp.relics.djinn.Affinity;
import com.newuniverse.nusmp.relics.djinn.Djinn;

/**
 * Everything the panel needs to know about a Djinn, as plain data copied from its row of the {@link Djinn} table. The panel's code never switches on
 * a Djinn constant: a Djinn added to the enum later is drawn from its own colours, affinity, look and name. Tests build specs by hand for
 * combinations the table does not have.
 */
public record GuiSpec(String id, String name, String title, String form, int color, int glow, Affinity affinity, Djinn.Look look) {

    public static GuiSpec of(Djinn d) {
        return new GuiSpec(d.id(), d.displayName(), d.title(), d.formName(), d.color() & 0xFFFFFF, d.glow() & 0xFFFFFF, d.affinity(), d.look());
    }

    /** The tint of the Djinn's magic (a neutral grey-violet for a missing affinity). */
    public int tint() { return affinity == null ? 0xA090C0 : affinity.tint; }
}
