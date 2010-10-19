/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.util.Position;

/**
 * Signals an error in the class resolver system. This exception is thrown
 * when a <code>ClassResolver</code> is unable to resolve a given class name.
 */
public class NoMemberException extends SemanticException {
    private static final long serialVersionUID = 1085715680905007871L;

    private int kind;
    public static final int METHOD = 1;
    public static final int CONSTRUCTOR = 2;
    public static final int FIELD = 3;
    
    public NoMemberException(int kind, String s) {
        super(s);
        this.kind = kind;
    }
    
    public NoMemberException(int kind, String s, Position position) {
        super(s, position);
        this.kind = kind;
    }
    
    public int getKind() {
        return kind;
    }
    public String getKindStr() {
        switch (kind) {
            case METHOD:
                return "method";
            case CONSTRUCTOR:
                return "constructor";
            case FIELD:
                return "field";
            default:
                return "unknown!!!";    
        }
    }
}
