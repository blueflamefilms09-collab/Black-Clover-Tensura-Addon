"""Puts this folder and tools/relics on sys.path (the generators of the worn library are plain modules, not a package)."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
for p in (HERE, os.path.join(HERE, "..", "..", "relics")):
    if p not in sys.path:
        sys.path.insert(0, p)
