/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * This file was originally derived from the Polyglot extensible compiler framework.
 *
 *  (C) Copyright 2000-2007 Polyglot project group, Cornell University
 *  (C) Copyright IBM Corporation 2007-2014.
 */

package polyglot.util;

import java.io.Serializable;

import polyglot.frontend.Globals;
import polyglot.main.Options;

/**
 * This class represents a posiiton within a file.
 **/
public class Position implements Serializable, Copy
{
    static final long serialVersionUID = -4588386982624074261L;

    private String path;
    private String file;

    private int line;
    private int column;
    
    private int endLine;
    private int endColumn;

    // Position in characters from the beginning of the containing character
    // stream
    private int offset;
    private int endOffset;
    
    private boolean compilerGenerated;
    private Position outer = null;

    public static final int UNKNOWN = -1;
    public static final int END_UNUSED = -2;
    public static final Position COMPILER_GENERATED = new Position(true, null, "Compiler Generated");
    
    public static final int THIS_METHOD = 1;
    public static final int CALLER = THIS_METHOD + 1;

//    /**
//     * Get a compiler generated position using the caller at the given stack
//     * depth.  Depth 1 is the caller.  Depth 2 is the caller's caller, etc.
//     */ 
//    public static Position compilerGenerated(int depth) {
//    	if (! Globals.Options().precise_compiler_generated_positions)
//            return COMPILER_GENERATED;
//        StackTraceElement[] stack = new Exception().getStackTrace();
//        if (depth < stack.length) {
//            return new Position(true, null, stack[depth].getFileName() + " (compiler generated)", stack[depth].getLineNumber());
//        }
//        else {
//            return COMPILER_GENERATED;
//        }
//    }
//
//    /** Get a compiler generated position. */ 
//    public static Position compilerGenerated() {
//        return compilerGenerated(CALLER);
//    }

    public static Position compilerGenerated(Position pos) {
        return pos==null ? COMPILER_GENERATED : pos.startOf().markCompilerGenerated();
    }

    public boolean isCompilerGenerated() {
        return compilerGenerated;
    }
    
    public Position copy() {
	try {
	    return (Position) super.clone();
	} catch (CloneNotSupportedException e) {
	    assert false;
	    return Position.COMPILER_GENERATED;
	}
    }
    
    public Position markCompilerGenerated() {
	Position p = copy();
	p.compilerGenerated = true;
	return p;
    }
    
    /** For deserialization. */
    protected Position() {
        line = endLine = 0;
        column = endColumn = 0;
        offset = endOffset = 0;
    }

    private Position(boolean compilerGenerated, String path, String file) {
        this(path, file, UNKNOWN, UNKNOWN);
        this.compilerGenerated = compilerGenerated;
    }

    public Position(String path, String file) {
        this(path, file, UNKNOWN, UNKNOWN);
    }

    private Position(boolean compilerGenerated, String path, String file, int line) {
        this(path, file, line, UNKNOWN);
        this.compilerGenerated = compilerGenerated;
    }

    public Position(String path, String file, int line) {
        this(path, file, line, UNKNOWN);
    }

    public Position(String path, String file, int line, int column) {
        this(path, file, line, column, END_UNUSED, END_UNUSED);
    }
    
    public Position(String path, String file, int line, int column, int endLine, int endColumn) {
        this(path, file, line, column, endLine, endColumn, 0, 0);
    }

    public Position(String path, String file, int line, int column, int endLine, int endColumn, int offset, int endOffset) {
        this.file = file;
        assert (path == null || path.equals("")) : "Non-empty path in the position object";
        this.path = path;
        this.line = line;
        this.column = column;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.offset = offset;
        this.endOffset = endOffset;
    }

    public Position(Position start, Position end) {
        this(start.path(), start.file(), start.line, start.column, end.endLine, end.endColumn, start.offset, end.endOffset);
    }

    public Position truncateEnd(int len) {
    	if (this.isCompilerGenerated())
    	    return this;
    	
	int eo = endOffset;
	int el = endLine;
	int ec = endColumn;
	
	if (eo >= offset + len)
	    eo -= len;
	else
	    eo = offset;
	
	if (line == el) {
	    if (ec >= column + len)
		ec -= len;
	    else
		ec = column;
	}
	else {
	    if (ec >= len)
		ec -= len;
	    else {
		el = line;
		ec = column;
	    }
	}
	
	return new Position(path, file, line, column, el, ec, offset, eo);
    }
    
    public Position startOf() {
    	if (this.isCompilerGenerated())
    	    return this;
        return new Position(path, file, line, column, line, column, offset, offset);
    }

    public Position endOf() {
    	if (this.isCompilerGenerated())
    	    return this;
        return new Position(path, file, endLine, endColumn, endLine, endColumn, endOffset, endOffset);
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public int endLine() {
        if (endLine == UNKNOWN || (line != UNKNOWN && endLine < line)) {
            return line;
        }
        return endLine;
    }

    public int endColumn() {
        if (endColumn == UNKNOWN || (column != UNKNOWN && endLine()==line() && endColumn < column)) {
            return column;
        }
        return endColumn;
    }

    public int offset() {
        return offset;
    }

    public int endOffset() {
        if (endOffset == UNKNOWN || (offset != UNKNOWN && endOffset < offset)) {
            return offset;
        }
        return endOffset;
    }
    
    public String file() {
        return file;
    }

    public String path() {
        return path;
    }

    public Position outer() {
        return this.outer;
    }

    public Position outerMost() {
        Position p = this;
        while (p.outer != null)
            p = p.outer;
        return p;
    }

    public Position outer(Position o) {
        Position p = copy();
        p.outer = o;
        return p;
    }

    public Position addOuter(Position o) {
        if (o == this) {
            return this;
        }
        if (this.outer == null) {
            return outer(o);
        }
        Position t = this.outer.addOuter(o);
        if (t == this.outer) {
            return this;
        }
        Position p = copy();
        p.outer = t;
        return p;
    }

    public String nameAndLineString() {
        // Maybe we should use path here, if it isn't too long...
        String s = path;

        if (s == null || s.length() == 0) {
            s = file;
        }

        if (s == null || s.length() == 0) {
            s = "unknown file";
        }
        
        if (line != UNKNOWN) {
            s += ":" + line;
            if (endLine != line &&
                    endLine != UNKNOWN && endLine != END_UNUSED) {
                s += "-" + endLine;
            }
        }
        
        return s;
    }

    public String toString() {
        String s = path;

        if (s == null || s.length() == 0) {
            s = file;
        }

        if (s == null || s.length() == 0) {
            s = "unknown file";
        }

        if (line != UNKNOWN) {
            s += ":" + line;
        
            if (column != UNKNOWN) {
                s += "," + column;
                if (line == endLine && 
                  endColumn != UNKNOWN && endColumn != END_UNUSED) {
                    s += "-" + endColumn;
                }
                if (line != endLine && 
                      endColumn != UNKNOWN && endColumn != END_UNUSED) {
                    s += "-" + endLine + "," + endColumn;
                }
            }
        }
        
        if (outer != null) {
            s += " inlined from " + outer;
        }
        return s;
    }
}
