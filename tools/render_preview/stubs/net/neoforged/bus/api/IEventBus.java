package net.neoforged.bus.api;

import java.util.function.Consumer;

/** Preview stub of NeoForge's IEventBus (listeners are never called in the preview). */
public interface IEventBus {
    <T extends Event> void addListener(Consumer<T> consumer);
}
