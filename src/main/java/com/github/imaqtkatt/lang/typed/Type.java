package com.github.imaqtkatt.lang.typed;

import java.util.List;

public sealed interface Type {
    String javaDescriptor();

    String javaType();

    String signature();

    Type BOOL = new Bool();
    Type INT = new Int();
    Type STRING = new TString();
    Type VOID = new TVoid();

    record Hole(HoleRef ref) implements Type {
        @Override
        public String javaDescriptor() {
            return switch (ref.get()) {
                case com.github.imaqtkatt.lang.typed.Hole.Bound(Type type) -> type.javaDescriptor();
                case com.github.imaqtkatt.lang.typed.Hole.Unbound ignored -> "Ljava/lang/Object;";
            };
        }

        @Override
        public String javaType() {
            return switch (ref.get()) {
                case com.github.imaqtkatt.lang.typed.Hole.Bound(Type type) -> type.javaType();
                case com.github.imaqtkatt.lang.typed.Hole.Unbound ignored -> "java/lang/Object";
            };
        }

        @Override
        public String signature() {
            return switch (ref.get()) {
                case com.github.imaqtkatt.lang.typed.Hole.Bound(Type bound) -> bound.signature();
                case com.github.imaqtkatt.lang.typed.Hole.Unbound ignored -> "Ljava/lang/Object;";
            };
        }

        @Override
        public String toString() {
            return switch (ref.get()) {
                case com.github.imaqtkatt.lang.typed.Hole.Bound bound -> bound.type().toString();
                case com.github.imaqtkatt.lang.typed.Hole.Unbound unbound -> "<?%s>".formatted(unbound.id());
            };
        }
    }

    record Mutable(Type inner) implements Type {
        @Override
        public String javaDescriptor() {
            return "Lchoco/lang/Mutable;";
        }

        @Override
        public String javaType() {
            return "choco/lang/Mutable";
        }

        @Override
        public String signature() {
            return "Lchoco/lang/Mutable<%s>;".formatted(inner.signature());
        }
    }

    record Int() implements Type {
        @Override
        public String javaDescriptor() {
            return "Ljava/lang/Integer;";
        }

        @Override
        public String javaType() {
            return "java/lang/Integer";
        }

        @Override
        public String signature() {
            return "Ljava/lang/Integer;";
        }
    }

    record Bool() implements Type {
        @Override
        public String javaDescriptor() {
            return "Ljava/lang/Boolean;";
        }

        @Override
        public String javaType() {
            return "java/lang/Boolean";
        }

        @Override
        public String signature() {
            return javaDescriptor();
        }
    }

    record TString() implements Type {
        @Override
        public String javaDescriptor() {
            return "Ljava/lang/String;";
        }

        @Override
        public String javaType() {
            return "java/lang/String";
        }

        @Override
        public String signature() {
            return javaDescriptor();
        }
    }

    record Fun(List<Type> params, Type ret) implements Type {
        @Override
        public String javaDescriptor() {
            var builder = new StringBuilder();
            builder.append('(');
            for (var param : params) {
                builder.append(param.javaDescriptor());
            }
            builder.append(')');
            builder.append(ret.javaDescriptor());
            return builder.toString();
        }

        @Override
        public String javaType() {
            return "java/lang/Object";
        }

        @Override
        public String signature() {
            var builder = new StringBuilder();
            builder.append('(');
            for (var param : params) {
                builder.append(param.signature());
            }
            builder.append(')');
            builder.append(ret.signature());
            return builder.toString();
        }
    }

    record TVoid() implements Type {
        @Override
        public String javaDescriptor() {
            return "V";
        }

        @Override
        public String javaType() {
            return "V";
        }

        @Override
        public String signature() {
            return "V";
        }
    }

    static Type extract(Type type) {
        return switch (type) {
            case Hole(HoleRef ref) -> switch (ref.get()) {
                case com.github.imaqtkatt.lang.typed.Hole.Bound bound -> extract(bound.type());
                case com.github.imaqtkatt.lang.typed.Hole.Unbound unbound -> type;
            };
            default -> type;
        };
    }
}
