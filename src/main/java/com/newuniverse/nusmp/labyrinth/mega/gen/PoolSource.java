package com.newuniverse.nusmp.labyrinth.mega.gen;

/**
 * Why: the seam that lets one assembler run in two worlds. In the game the source is an adapter over the server's data pack (pool JSON read with the
 * resource manager, templates read with the structure template manager); in a unit test it is an adapter over {@code catalog.json}, a dump of the REAL
 * template files, or over a handful of shapes written in the test. The assembler asks it only two questions, so nothing Minecraft-shaped can leak
 * into the algorithm.
 */
public interface PoolSource {
    /** The pool with this id, or null when no such pool exists (a socket that names it is then left for the cap pass). */
    PoolDef pool(String id);

    /** The shape of the template with this id, or null when the template does not exist or cannot be read. */
    Shape shape(String id);
}
