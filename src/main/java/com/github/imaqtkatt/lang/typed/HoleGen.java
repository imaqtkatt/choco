package com.github.imaqtkatt.lang.typed;

public final class HoleGen {
    private static Integer id;
    private static Integer level;

    static {
        reset();
    }

    public static Integer newId() {
        var tmp = id;
        id++;
        return tmp;
    }

    public static Integer currentLevel() {
        return level;
    }

    public static void enterLevel() {
        level++;
    }

    public static void leaveLevel() {
        level--;
    }

    public static Type newHole() {
        var inner = new Hole.Unbound(newId(), level);
        var ref = new HoleRef(inner);
        return new Type.Hole(ref);
    }

    public static void reset() {
        id = 0;
        level = 0;
    }
}
