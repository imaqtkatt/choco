package com.github.imaqtkatt.lang.lexer;

public record Token(
        TokenType tokenType,
        String lexeme,
        Location location
) {
}
