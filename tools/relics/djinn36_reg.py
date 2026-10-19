"""djinn36_reg: the registry of the per-Djinn builders of the 24 new Djinn (name -> {piece: function}); the djinn36_set* modules fill it."""
SETS = {}


def register(name, **fns):
    SETS[name] = fns
