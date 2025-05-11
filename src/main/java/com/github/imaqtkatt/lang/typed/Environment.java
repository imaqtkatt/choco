package com.github.imaqtkatt.lang.typed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class Environment {
    private Map<String, Scheme> variables = new LinkedHashMap<>();
//    private Map<String, Type> vals = new LinkedHashMap<>();
    private Map<String, Scheme> declarations = new LinkedHashMap<>();

    public void insert(String name, Scheme scheme) {
        variables.put(name, scheme);
    }

    public Optional<Scheme> fetch(String name) {
        var variable = variables.get(name);
        if (variable == null) {
            var val = declarations.get(name);
            return Optional.ofNullable(val);
        } else {
            return Optional.of(variable);
        }
    }

    public void remove(String name) {
        variables.remove(name);
    }

    public void declare(String name, Scheme scheme) {
        declarations.put(name, scheme);
    }

    public void declareVal(String name, Type type) {
        declarations.put(name, Scheme.ofType(type));
    }

    public Environment clone() {
        var cloned = new Environment();
        cloned.variables = new LinkedHashMap<>(variables);
        cloned.declarations = new LinkedHashMap<>(declarations);
        return cloned;
    }

//    @Override
//    public Object clone() throws CloneNotSupportedException {
//        var cloned = (Environment) super.clone();
//        cloned.variables.putAll(variables);
//        return cloned;
//    }
}
