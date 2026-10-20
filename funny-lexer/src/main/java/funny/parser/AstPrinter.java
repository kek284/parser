package funny.parser;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/** JSON для records AST: kind содержит имя узла, остальные поля совпадают с API. */
public final class AstPrinter {
    private AstPrinter() {}
    public static String toJson(Ast.Program program) { return json(program, 0); }

    private static String json(Object value, int depth) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        
        String indent = "  ".repeat(depth + 1);
        var entries = new ArrayList<String>();
        
        boolean array = value instanceof List<?>;

        if (value instanceof List<?> list) {
            for (Object item : list) entries.add(json(item, depth + 1));
        } else if (value.getClass().isRecord()) {
            if (value instanceof Ast.Node) entries.add("\"kind\": " + quote(value.getClass().getSimpleName()));
            
            for (RecordComponent component : value.getClass().getRecordComponents()) {
                try {
                    Object field = component.getAccessor().invoke(value);
                    entries.add(quote(component.getName()) + ": " + json(field, depth + 1));
                
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Cannot serialize AST", exception);
                }
            }
        } else throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
        
        String open = array ? "[" : "{";
        String close = array ? "]" : "}";

        if (entries.isEmpty()) return open + close;
        
        return open + "\n" + indent + String.join(",\n" + indent, entries)
                + "\n" + "  ".repeat(depth) + close;
    }

    private static String quote(String text) {
        var result = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 32) result.append(String.format("\\u%04x", (int) c));
                    else result.append(c);
                }
            }
        }
        return result.append('"').toString();
    }
}
