package com.github.imaqtkatt.lang.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Scope {
    Map<String, DeclarationType> declarations;
    List<Map<String, DeclarationType>> scopes = new ArrayList<>();

    public Scope() {
        declarations = new LinkedHashMap<>();
    }

    public void declare(String name, DeclarationType declarationType) {
        if (declarations.containsKey(name)) {
            throw new RuntimeException("Name '%s' was already declared in the current scope.".formatted(name));
        }
        declarations.put(name, declarationType);
    }

    public DeclarationType fetch(String name) {
        if (!declarations.containsKey(name)) {
            throw new RuntimeException("Name '%s' was not declared in the current scope.".formatted(name));
        }
        return declarations.get(name);
    }

    public void enterRestrictScope() {
        scopes.addLast(this.declarations);
        this.declarations = new LinkedHashMap<>();
    }

    public void enterDynamicScope() {
        scopes.addLast(this.declarations);
        this.declarations = new LinkedHashMap<>(this.declarations);
    }

    public void leaveScope() {
        if (scopes.isEmpty()) {
            throw new IllegalStateException();
        }
        this.declarations = scopes.removeLast();
    }

    public enum DeclarationType {
        Val,
        Fun,
        Var,
    }
}
