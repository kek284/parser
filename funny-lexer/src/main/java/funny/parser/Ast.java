package funny.parser;

import funny.lexer.SourcePosition;
import java.util.List;

/* узлы сохраняют позицию начала исходной конструкции. */
public final class Ast {
    private Ast() {}
    public interface Node { SourcePosition position(); }
    public interface Statement extends Node {}
    public interface Expression extends Node {}
    public record Program(List<Function> functions, SourcePosition position) implements Node {
        public Program { functions = List.copyOf(functions); }
    }

    public record Variable(String name, String type, SourcePosition position) implements Node {}
    
    public record Function(String name, List<Variable> parameters, List<Variable> returns,
                           Expression requires, Expression ensures, List<Variable> locals,
                           Block body, SourcePosition position) implements Node {
        public Function {
            parameters = List.copyOf(parameters); returns = List.copyOf(returns);
            locals = List.copyOf(locals);
        }
    }
    public record Block(List<Statement> statements, SourcePosition position) implements Statement {
        public Block { statements = List.copyOf(statements); }
    }
    public record Assign(List<String> targets, Expression value, SourcePosition position) implements Statement {
        public Assign { targets = List.copyOf(targets); }
    }
    public record If(Expression condition, Block thenBranch, Block elseBranch,
                     SourcePosition position) implements Statement {}
    
    public record While(Expression condition, Expression invariant, Block body,
                        SourcePosition position) implements Statement {}
    
    public record Assert(Expression predicate, SourcePosition position) implements Statement {}
    
    public record Assume(Expression predicate, SourcePosition position) implements Statement {}
    // Число хранится строкой
    public record IntegerLiteral(String value, SourcePosition position) implements Expression {}
    public record BooleanLiteral(boolean value, SourcePosition position) implements Expression {}
    public record Name(String name, SourcePosition position) implements Expression {}
    public record Unary(String operator, Expression operand, SourcePosition position) implements Expression {}
    public record Binary(String operator, Expression left, Expression right,
                         SourcePosition position) implements Expression {}
    
    public record Call(String name, List<Expression> arguments, SourcePosition position) implements Expression {
        public Call { arguments = List.copyOf(arguments); }
    }
    
    public record ArrayRead(Expression array, Expression index, SourcePosition position) implements Expression {}
    public record ArraySet(Expression array, Expression index, Expression value,
                           SourcePosition position) implements Expression {}
}
