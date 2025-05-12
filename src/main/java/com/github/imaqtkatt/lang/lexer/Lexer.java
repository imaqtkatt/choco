package com.github.imaqtkatt.lang.lexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public final class Lexer {
    private final String source;

    private Integer index;
    private Integer start;
    private final String path;

    public static Lexer fromFile(File file) throws IOException {
        var fileContents = "";
        var filePath = file.getPath();

//        System.out.println("file.getPath() = " + file.getPath());

        try (var fileInputStream = new FileInputStream(file)) {
            var bytes = fileInputStream.readAllBytes();
            fileContents = new String(bytes);
        }

        return new Lexer(fileContents, filePath);
    }

    private Lexer(String source, String path) {
        this.source = source;
        this.index = 0;
        this.start = 0;
        this.path = path;
    }

    private Optional<Character> peekOptional() {
        Optional<Character> retVal = Optional.empty();

        try {
            var c = this.source.charAt(this.index);
            retVal = Optional.of(c);
        } catch (IndexOutOfBoundsException ignored) {
        }

        return retVal;
    }

    private Character peek() {
        return this.source.charAt(this.index);
    }

    private Optional<Character> next() {
        Optional<Character> retVal = Optional.empty();

        try {
            var c = this.source.charAt(this.index++);
            retVal = Optional.of(c);
        } catch (IndexOutOfBoundsException ignored) {
        }

        return retVal;
    }

    private Location currentLocation() {
        return new Location(this.path, this.start, this.index);
    }

    private String currentLexeme() {
        return this.source.substring(this.start, this.index);
    }

    private String takeWhile(Function<Character, Boolean> predicate) {
        var builder = new StringBuilder();
        var current = peekOptional();
        while (current.isPresent()) {
            char c = current.get();

            if (predicate.apply(c)) {
                next();
                builder.append(c);
                current = peekOptional();
            } else {
                break;
            }
        }
        return builder.toString();
    }

    private void skipWhile(Function<Character, Boolean> predicate) {
        takeWhile(predicate);
    }

    private Boolean consume(Character c) {
        var p = peekOptional();
        if (p.isPresent() && p.get().equals(c)) {
            next();
            return true;
        } else {
            return false;
        }
    }

    private void skipWhitespaces() {
        this.skipWhile(Character::isWhitespace);
    }

    private void skip() {
        loop:
        while (true) {
            var p = peekOptional();
            if (p.isEmpty()) {
                return;
            }
            var c = p.get();
            switch (c) {
                case '#' -> skipWhile((c_) -> c_ != '\n');
                case ' ', '\n', '\r', '\t' -> skipWhitespaces();
                default -> {
                    break loop;
                }
            }
        }
    }

    private void save() {
        this.start = this.index;
    }

    public Token nextToken() {
        skip();
        save();

        var next = next();
        if (next.isEmpty()) {
            return new Token(TokenType.EOF, "", currentLocation());
        }

        char c = next.get();
        var tokenType = switch (c) {
            case '(' -> {
                if (consume(')')) {
                    yield TokenType.Unit;
                } else {
                    yield TokenType.LParens;
                }
            }
            case '<' -> {
                if (consume('-')) {
                    yield TokenType.LeftArrow;
                } else if (consume('=')) {
                    yield TokenType.LessEqual;
                } else {
                    yield TokenType.LessThan;
                }
            }
            case '>' -> {
                if (consume('=')) {
                    yield TokenType.GreaterEqual;
                } else {
                    yield TokenType.GreaterThan;
                }
            }
            case ',' -> TokenType.Comma;
            case ';' -> TokenType.Semicolon;
            case ')' -> TokenType.RParens;
            case '+' -> TokenType.Plus;
            case '-' -> TokenType.Minus;
            case '*' -> TokenType.Star;
            case '/' -> TokenType.Slash;
            case '=' -> TokenType.Equal;
            case '!' -> TokenType.Exclamation;
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                this.skipWhile(Character::isDigit);
                if (!this.consume('.')) {
                    yield TokenType.Integer;
                }
                this.skipWhile(Character::isDigit);
                yield TokenType.Number;
            }
            default -> {
                if (Character.isUpperCase(c)) {
                    this.skipWhile(Character::isLetterOrDigit);
                    yield TokenType.UpperIdent;
                } else if (Character.isLowerCase(c)) {
                    this.skipWhile(Character::isLetterOrDigit);
                    yield switch (currentLexeme()) {
                        case "package" -> TokenType.Package;
                        case "import" -> TokenType.Import;
                        case "let" -> TokenType.Let;
                        case "in" -> TokenType.In;
                        case "fun" -> TokenType.Fun;
                        case "true" -> TokenType.True;
                        case "false" -> TokenType.False;
                        case "if" -> TokenType.If;
                        case "then" -> TokenType.Then;
                        case "else" -> TokenType.Else;
                        case "mutable" -> TokenType.Mutable;
                        case "deref" -> TokenType.Deref;
                        case "val" -> TokenType.Val;
                        default -> TokenType.LowerIdent;
                    };
                } else {
                    yield TokenType.Error;
                }
            }
        };

        var lexeme = currentLexeme();
        var location = currentLocation();

        return new Token(tokenType, lexeme, location);
    }
}
