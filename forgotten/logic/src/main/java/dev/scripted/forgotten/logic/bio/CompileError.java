package dev.scripted.forgotten.logic.bio;

/**
 * Why source did not compile. A syntax error makes the Compiler Chest explode or summon a
 * NullPointerException; which one depends on {@link #severe()}.
 */
public record CompileError(Kind kind, int line, int column, String message) {
    public enum Kind {
        EMPTY_PROGRAM,
        UNKNOWN_OPCODE,
        MISSING_ARGUMENT,
        BAD_ARGUMENT,
        BAD_JUMP,
        /** No silicon wafer to compile onto. Not a syntax error: nothing explodes. */
        NO_MEDIUM
    }

    /** Errors in control flow are worse than typos: they summon something. */
    public boolean severe() {
        return kind == Kind.BAD_JUMP || kind == Kind.UNKNOWN_OPCODE;
    }

    public String describe() {
        return "line " + line + (column > 0 ? ":" + column : "") + ": " + message;
    }
}
