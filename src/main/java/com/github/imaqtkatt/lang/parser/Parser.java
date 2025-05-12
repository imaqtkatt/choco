package com.github.imaqtkatt.lang.parser;

import com.github.imaqtkatt.lang.lexer.Lexer;
import com.github.imaqtkatt.lang.lexer.Token;
import com.github.imaqtkatt.lang.lexer.TokenType;
import com.github.imaqtkatt.lang.tree.Expression;
import com.github.imaqtkatt.lang.tree.Node;
import com.github.imaqtkatt.lang.tree.Operation;
import com.github.imaqtkatt.lang.tree.Program;

import java.util.ArrayList;
import java.util.List;

public final class Parser {
    private final Lexer lexer;
    private Token current;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        current = this.lexer.nextToken();
    }

    public TokenType peek() {
        return current.tokenType();
    }

    private boolean is(TokenType type) {
        return current.tokenType() == type;
    }

    private Token eat() {
        var tmp = current;
        current = lexer.nextToken();
        return tmp;
    }

    private <T> T unexpected() {
        throw new RuntimeException(
                "Unexpected token '%s'".formatted(current.lexeme())
        );
    }

    private Token expect(TokenType expected) {
        if (peek() == expected) {
            return eat();
        } else {
            return unexpected();
        }
    }

    private boolean consume(TokenType type) {
        if (is(type)) {
            eat();
            return true;
        } else {
            return false;
        }
    }

    private Expression primary() {
        return switch (peek()) {
//            case Let -> null;
//            case Fun -> null;
//            case Package -> null;
//            case Import -> null;
            case UpperIdent -> null;
            case LowerIdent -> {
                var token = expect(TokenType.LowerIdent);
                yield new Expression.Variable(token.lexeme());
            }
            case Integer -> {
                var token = expect(TokenType.Integer);
                yield new Expression.Int(Integer.parseInt(token.lexeme()));
            }
            case Number -> null;
            case Unit -> null;
            case LParens -> {
                expect(TokenType.LParens);
                var expression = expression(Precedence.Start.left());
                expect(TokenType.RParens);
                yield expression;
            }
            case True -> {
                expect(TokenType.True);
                yield new Expression.Bool(true);
            }
            case False -> {
                expect(TokenType.False);
                yield new Expression.Bool(false);
            }
            default -> unexpected();
//                System.out.println("peek() = " + peek());
//                throw new UnsupportedOperationException();

//            case RParens -> null;
//            case LBrace -> null;
//            case RBrace -> null;
//            case Plus -> null;
//            case Minus -> null;
//            case Star -> null;
//            case Slash -> null;
//            case Equal -> null;
//            case EqualEqual -> null;
//            case Dot -> null;
//            case Comma -> null;
//            case Semicolon -> null;
//            case Error -> null;
//            case EOF -> null;
        };
    }

    private Precedence precedence() {
        return switch (peek()) {
            case Plus, Minus -> Precedence.Sum;
            case Star, Slash -> Precedence.Product;
            case LeftArrow -> Precedence.Set;
            case Semicolon -> Precedence.Seq;
            case LessThan, GreaterThan, LessEqual, GreaterEqual, EqualEqual -> Precedence.Compare;
            case And -> Precedence.And;
            case Or -> Precedence.Or;

            default -> Precedence.End;
        };
    }

    private Expression infix(Expression left) {
        return switch (peek()) {
            case Plus -> add(left);
            case Minus -> minus(left);
            case Star -> star(left);
            case Slash -> slash(left);
            case Semicolon -> seq(left);
            case LeftArrow -> set(left);
            case LessThan -> lt(left);
            case GreaterThan -> gt(left);
            case LessEqual -> le(left);
            case GreaterEqual -> ge(left);
            case And -> and(left);
            case Or -> or(left);
            case EqualEqual -> eql(left);
            default -> unexpected();
//            default -> throw new UnsupportedOperationException();
        };
    }

    private Expression seq(Expression left) {
        expect(TokenType.Semicolon);
        var right = expression(Precedence.Seq);
        return new Expression.Seq(left, right);
    }

    private Expression call() {
        var callee = primary();
        if (consume(TokenType.LParens)) {
            var arguments = new ArrayList<Expression>();
            while (!is(TokenType.RParens)) {
                arguments.add(expression(Precedence.Start));
                if (!consume(TokenType.Comma)) {
                    break;
                }
            }
            expect(TokenType.RParens);
            return new Expression.Call(callee, arguments);
        }
        if (consume(TokenType.Unit)) {
            return new Expression.Call(callee, List.of());
        }
        return callee;
    }

    private Expression let() {
        expect(TokenType.Let);
        var name = expect(TokenType.LowerIdent);
        expect(TokenType.Equal);
        var value = expression(Precedence.Start.left());
        expect(TokenType.In);
        var body = expression(Precedence.Start);
        return new Expression.Let(name.lexeme(), value, body);
    }

    private Expression ifExpression() {
        expect(TokenType.If);
        var condition = expression(Precedence.Start.left());
        expect(TokenType.Then);
        var then = expression(Precedence.Start.left());
        expect(TokenType.Else);
        var otherwise = expression(Precedence.Start.left());
        return new Expression.If(condition, then, otherwise);
    }

    private Expression mutable() {
        expect(TokenType.Mutable);
        var e = expression(Precedence.End);
        return new Expression.Mutable(e);
    }

    private Expression deref() {
        expect(TokenType.Deref);
        var e = expression(Precedence.End);
        return new Expression.Deref(e);
    }

    private Expression prefix() {
        return switch (peek()) {
            case Let -> let();
            case Mutable -> mutable();
            case Deref -> deref();
            case If -> ifExpression();
            default -> call();
        };
    }

    private Expression expression(Precedence precedence) {
        var left = prefix();

        while (true) {
            var p = precedence();
            if (p.value >= precedence.value && p != Precedence.End) {
                left = infix(left);
            } else {
                break;
            }
        }

        return left;
    }

    private Node node() {
        return switch (peek()) {
            case Fun -> funNode();
            case Val -> valNode();
            default -> unexpected();
        };
    }

    private Node funNode() {
        expect(TokenType.Fun);
        var name = expect(TokenType.LowerIdent);

        var parameters = new ArrayList<String>();

        if (!consume(TokenType.Unit)) {
            expect(TokenType.LParens);
            while (!is(TokenType.RParens)) {
                var param = expect(TokenType.LowerIdent);
                parameters.add(param.lexeme());
                if (!consume(TokenType.Comma)) {
                    break;
                }
            }
            expect(TokenType.RParens);
        }

        expect(TokenType.Equal);
        var body = expression(Precedence.Start);
        return new Node.FunDefinition(name.lexeme(), parameters, body);
    }

    private Node valNode() {
        expect(TokenType.Val);
        var name = expect(TokenType.LowerIdent);
        expect(TokenType.Equal);
        var e = expression(Precedence.Start);
        return new Node.ValDefinition(name.lexeme(), e);
    }

    public Program program() {
        expect(TokenType.Package);
        var packageName = expect(TokenType.LowerIdent);

        var definitions = new ArrayList<Node>();

        while (!is(TokenType.EOF)) {
            definitions.add(node());
        }

        return new Program(packageName.lexeme(), definitions);
    }

    private Expression add(Expression left) {
        expect(TokenType.Plus);
        var right = expression(Precedence.Sum.left());
        return new Expression.Binary(left, Operation.Add, right);
    }

    private Expression minus(Expression left) {
        expect(TokenType.Minus);
        var right = expression(Precedence.Sum.left());
        return new Expression.Binary(left, Operation.Sub, right);
    }

    private Expression star(Expression left) {
        expect(TokenType.Star);
        var right = expression(Precedence.Product.left());
        return new Expression.Binary(left, Operation.Mul, right);
    }

    private Expression slash(Expression left) {
        expect(TokenType.Slash);
        var right = expression(Precedence.Product.left());
        return new Expression.Binary(left, Operation.Div, right);
    }

    private Expression set(Expression left) {
        expect(TokenType.LeftArrow);
        var right = expression(Precedence.Set.left());
        return new Expression.Binary(left, Operation.Set, right);
    }

    private Expression lt(Expression left) {
        expect(TokenType.LessThan);
        var right = expression(Precedence.Compare.left());
        return new Expression.Binary(left, Operation.LT, right);
    }

    private Expression gt(Expression left) {
        expect(TokenType.GreaterThan);
        var right = expression(Precedence.Compare.left());
        return new Expression.Binary(left, Operation.GT, right);
    }

    private Expression le(Expression left) {
        expect(TokenType.LessEqual);
        var right = expression(Precedence.Compare.left());
        return new Expression.Binary(left, Operation.LE, right);
    }

    private Expression ge(Expression left) {
        expect(TokenType.GreaterEqual);
        var right = expression(Precedence.Compare.left());
        return new Expression.Binary(left, Operation.GE, right);
    }

    private Expression and(Expression left) {
        expect(TokenType.And);
        var right = expression(Precedence.And.left());
        return new Expression.Binary(left, Operation.And, right);
    }

    private Expression or(Expression left) {
        expect(TokenType.Or);
        var right = expression(Precedence.Or.left());
        return new Expression.Binary(left, Operation.Or, right);
    }

    private Expression eql(Expression left) {
        expect(TokenType.EqualEqual);
        var right = expression(Precedence.Compare.left());
        return new Expression.Binary(left, Operation.Eql, right);
    }
}
