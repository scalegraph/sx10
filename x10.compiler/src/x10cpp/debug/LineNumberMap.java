/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

package x10cpp.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;

import polyglot.types.MemberDef;
import polyglot.types.MethodDef;
import polyglot.types.ProcedureDef;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.util.QuotedStringTokenizer;
import polyglot.util.StringUtil;
import x10.util.ClassifiedStream;
import x10cpp.visit.Emitter;

/**
 * Map from generated filename and line to source filename and line.
 * (C++ filename (String), C++ line number range (int, int)) ->
 *     (X10 filename (String), X10 line number (int)).
 * Also stores the method mapping.
 * 
 * @author igor
 */
public class LineNumberMap extends StringTable {
    /** A map key representing a C++ source file/line range combination. */
    private static class Key {
        public final int fileId;
        public final int start_line;
        public final int end_line;
        public Key(int f, int s, int e) {
            fileId = f;
            start_line = s;
            end_line = e;
        }
        public String toString() {
            return fileId + ":" + start_line + "-" + end_line;
        }
        public int hashCode() {
            return fileId + start_line + end_line;
        }
        public boolean equals(Object o) {
            if (getClass() != o.getClass()) return false;
            Key k = (Key) o;
            return fileId == k.fileId && start_line == k.start_line && end_line == k.end_line;
        }
    }
    /** A map entry representing an X10 source file/line combination. */
    private static class Entry {
        public final int fileId;
        public final int line;
        public final int column;
        public Entry(int f, int l, int c) {
            fileId = f;
            line = l;
            column = c;
        }
        public String toString() {
        	return fileId + ":" + line;
        }
    }
    private final HashMap<Key, Entry> map;
    private static class MethodDescriptor {
        public final int returnType;
        public final int container;
        public final int name;
        public final int[] args;
        public final Key lines;
        public MethodDescriptor(int returnType, int container, int name, int[] args, Key lines) {
            this.returnType = returnType;
            this.container = container;
            this.name = name;
            this.args = args;
            this.lines = lines;
        }
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass())
                return false;
            MethodDescriptor md = (MethodDescriptor) obj;
            if (md.name != name || md.container != container ||
                    md.returnType != returnType || md.args.length != args.length)
            {
                return false;
            }
            for (int i = 0; i < args.length; i++) {
                if (args[i] != md.args[i])
                    return false;
            }
            return true;
        }
        public int hashCode() {
            int h = name;
            h = 31*h + container;
            h = 31*h + returnType;
            for (int arg : args) {
                h = 31*h + arg;
            }
            return h;
        }
        public String toString() {
            StringBuilder res = new StringBuilder();
            res.append(returnType).append(" ");
            res.append(container).append(".");
            res.append(name).append("(");
            boolean first = true;
            for (int arg : args) {
                if (!first) res.append(",");
                first = false;
                res.append(arg);
            }
            res.append(")");
            res.append("{").append(lines.fileId);
            res.append(",").append(lines.start_line);
            res.append(",").append(lines.end_line);
            res.append("}");
            return res.toString();
        }
        public static MethodDescriptor parse(String str) {
            StringTokenizer st = new StringTokenizer(str, ". (),", true);
            String s = st.nextToken(" ");
            int r = Integer.parseInt(s);
            s = st.nextToken();
            assert (s.equals(" "));
            s = st.nextToken(".");
            int c = Integer.parseInt(s);
            s = st.nextToken();
            assert (s.equals("."));
            s = st.nextToken("(");
            int n = Integer.parseInt(s);
            s = st.nextToken();
            assert (s.equals("("));
            ArrayList<String> al = new ArrayList<String>(); 
            while (st.hasMoreTokens()) {
                String t = st.nextToken(",)");
                if (t.equals(")"))
                    break;
                al.add(t);
                t = st.nextToken();
                if (t.equals(")"))
                    break;
                assert (t.equals(","));
            }
            int[] a = new int[al.size()];
            for (int i = 0; i < a.length; i++) {
                a[i] = Integer.parseInt(al.get(i));
            }
            s = st.nextToken("{,}");
            assert (s.equals("{"));
            s = st.nextToken();
            int f = Integer.parseInt(s);
            s = st.nextToken();
            int l = Integer.parseInt(s);
            s = st.nextToken();
            int e = Integer.parseInt(s);
            s = st.nextToken();
            assert (s.equals("}"));
            Key k = new Key(f, l, e);
            assert (!st.hasMoreTokens());
            return new MethodDescriptor(r, c, n, a, k);
        }
        public String toPrettyString(LineNumberMap map) {
            return toPrettyString(map, true);
        }
        public String toPrettyString(LineNumberMap map, boolean includeReturnType) {
            StringBuilder res = new StringBuilder();
            if (includeReturnType)
                res.append(map.lookupString(returnType)).append(" ");
            res.append(map.lookupString(container)).append("::");
            res.append(map.lookupString(name)).append("(");
            boolean first = true;
            for (int arg : args) {
                if (!first) res.append(",");
                first = false;
                res.append(map.lookupString(arg));
            }
            res.append(")");
            return res.toString();
        }
    }
    private final HashMap<MethodDescriptor, MethodDescriptor> methods;

    /**
     */
    public LineNumberMap() {
    	this(new ArrayList<String>());
    }

    private LineNumberMap(ArrayList<String> strings) {
    	super(strings);
        this.map = new HashMap<Key, Entry>();
        this.methods = new HashMap<MethodDescriptor, MethodDescriptor>();
    }

    /**
     * @param cppFile C++ filename
     * @param startLine C++ start line number
     * @param endLine C++ end line number
     * @param sourceFile X10 filename
     * @param sourceLine X10 line number
     */
    public void put(String cppFile, int startLine, int endLine, String sourceFile, int sourceLine, int sourceColumn) {
        map.put(new Key(stringId(cppFile), startLine, endLine), new Entry(stringId(sourceFile), sourceLine, sourceColumn));
    }

	private MethodDescriptor createMethodDescriptor(String container, String name, String returnType, String[] args, Key l) {
		int c = stringId(container);
		int n = stringId(name);
		int r = stringId(returnType);
		int[] a = new int[args.length];
		for (int i = 0; i < a.length; i++) {
			a[i] = stringId(args[i]);
		}
		return new MethodDescriptor(r, c, n, a, l);
	}

	/**
	 * @param def X10 method or constructor signature
	 * @param cppFile generated file containing the method body
	 * @param startLine first generated line of the method body
	 * @param endLine last generated line of the method body
	 */
	public void addMethodMapping(MemberDef def, String cppFile, int startLine, int endLine) {
	    Key tk = new Key(stringId(cppFile), startLine, endLine);
	    Type container = def.container().get();
	    assert (def instanceof ProcedureDef);
	    String name = (def instanceof MethodDef) ? ((MethodDef) def).name().toString() : null;
	    Type returnType = (def instanceof MethodDef) ? ((MethodDef) def).returnType().get() : null;
	    List<Ref<? extends Type>> formalTypes = ((ProcedureDef) def).formalTypes();
	    addMethodMapping(container, name, returnType, formalTypes, tk);
	}

	private void addMethodMapping(Type c, String n, Type r, List<Ref<? extends Type>> f, Key tk) {
		assert (c != null);
		assert (f != null);
		String sc = c.toString();
		String sn = n == null ? "this" : n;
		String sr = r == null ? "" : r.toString();
		String[] sa = new String[f.size()];
		for (int i = 0; i < sa.length; i++) {
			sa[i] = f.get(i).get().toString();
		}
		MethodDescriptor src = createMethodDescriptor(sc, sn, sr, sa, null);
		String tc = Emitter.translateType(c);
		String tn = n == null ? "_constructor" : Emitter.mangled_method_name(n);
		String tr = r == null ? "void" : Emitter.translateType(r, true);
		String[] ta = new String[sa.length];
		for (int i = 0; i < ta.length; i++) {
			ta[i] = Emitter.translateType(f.get(i).get(), true);
		}
		MethodDescriptor tgt = createMethodDescriptor(tc, tn, tr, ta, tk);
		// TODO: This line below causes the X10 standard library to fail to compile with the -DEBUG flag.  Why?
//		assert (methods.get(tgt) == null); 
		methods.put(tgt, src);
	}
	
	private class LocalVariableMapInfo
	{
		int _x10name;			// Index of the X10 variable name in _X10strings
		int _x10type;          // Classification of this type
		int _x10typeIndex; 	// Index of the X10 type into appropriate _X10ClassMap, _X10ClosureMap  (if applicable)
		int _cppName;			// Index of the C++ variable name in _X10strings
	    String _x10index;         // Index of X10 file name in _X10sourceList
		int _x10startLine;     // First line number of X10 line range
		int _x10endLine;       // Last line number of X10 line range
	}
	
	private class MemberVariableMapInfo
	{
		int _x10type;       // Classification of this type
		int _x10typeIndex;  // Index of the X10 type into appropriate _X10typeMap
		int _x10memberName; // Index of the X10 member name in _X10strings
		int _cppMemberName; // Index of the C++ member name in _X10strings
		int _cppClass; // Index of the C++ containing struct/class name in _X10strings
	}
	
	private static ArrayList<Integer> arrayMap = new ArrayList<Integer>();
	private static ArrayList<Integer> refMap = new ArrayList<Integer>();
	private static ArrayList<Integer> closureMap = new ArrayList<Integer>();
	private static ArrayList<LocalVariableMapInfo> localVariables;
	private static Hashtable<String, ArrayList<MemberVariableMapInfo>> memberVariables;
	
	// the type numbers were provided by Steve Cooper in "x10dbg_types.h"
	static int determineTypeId(String type)
	{
		//System.out.println("looking up type \""+type+"\"");
		if (type.equals("x10.lang.Int") || type.startsWith("x10.lang.Int{"))
			return 6;
		if (type.startsWith("x10.array.Array"))
			return 200;
		if (type.startsWith("x10.lang.PlaceLocalHandle"))
			return 203;
		if (type.equals("x10.lang.Boolean") || type.startsWith("x10.lang.Boolean{"))
			return 1;
		if (type.equals("x10.lang.Byte") || type.startsWith("x10.lang.Byte{"))
			return 2;
		if (type.equals("x10.lang.Char") || type.startsWith("x10.lang.Char{"))
			return 3;
		if (type.equals("x10.lang.Double") || type.startsWith("x10.lang.Double{"))
			return 4;
		if (type.equals("x10.lang.Float") || type.startsWith("x10.lang.Float{"))
			return 5;
		if (type.equals("x10.lang.Long") || type.startsWith("x10.lang.Long{"))
			return 7;
		if (type.equals("x10.lang.Short") || type.startsWith("x10.lang.Short{"))
			return 8;		
		if (type.equals("x10.lang.UByte") || type.startsWith("x10.lang.UByte{"))
			return 9;
		if (type.equals("x10.lang.UInt") || type.startsWith("x10.lang.UInt{"))
			return 10;
		if (type.equals("x10.lang.ULong") || type.startsWith("x10.lang.ULong{"))
			return 11;
		if (type.equals("x10.lang.UShort") || type.startsWith("x10.lang.UShort{"))
			return 12;
		if (type.startsWith("x10.array.DistArray"))
			return 202;
		if (type.startsWith("x10.array.Dist"))
			return 201;
		if (type.startsWith("x10.lang.Rail"))
			return 204;
		if (type.startsWith("x10.util.Random"))
			return 205;
		if (type.startsWith("x10.lang.String"))
			return 206;		
		if (type.startsWith("x10.array.Point"))
			return 208;
		if (type.startsWith("x10.array.Region"))
			return 300;
		return 101; // generic class
	}
	
	static int determineSubtypeId(String type, ArrayList<Integer> list)
	{
		int bracketStart = type.indexOf('[');
		int bracketEnd = type.lastIndexOf(']');
		if (bracketStart != -1 && bracketEnd != -1)
		{
			String subtype = type.substring(bracketStart+1, bracketEnd);
			int subtypeId = determineTypeId(subtype);
			int position = list.size();
			list.add(subtypeId);
			list.add(determineSubtypeId(subtype, list));			
			return position/2;
		}
		else 
			return -1;
	}
	
	public void addLocalVariableMapping(String name, String type, int startline, int endline, String file)
	{
		if (localVariables == null)
			localVariables = new ArrayList<LineNumberMap.LocalVariableMapInfo>();
		
		LocalVariableMapInfo v = new LocalVariableMapInfo();
		v._x10name = stringId(name);
		v._x10type = determineTypeId(type);
		if (v._x10type == 203)
			v._x10typeIndex = determineSubtypeId(type, refMap);
		else if (v._x10type == 200 || v._x10type == 202 || v._x10type == 204 || v._x10type == 207)
			v._x10typeIndex = determineSubtypeId(type, arrayMap);
		else if (v._x10type == 101)
		{
			int b = type.indexOf('{');
			if (b == -1)				
				v._x10typeIndex = stringId(Emitter.mangled_non_method_name(type));
			else
				v._x10typeIndex = stringId(Emitter.mangled_non_method_name(type.substring(0, b)));
		}
		else 
			v._x10typeIndex = -1;
		v._cppName = stringId(Emitter.mangled_non_method_name(name)); 
		v._x10index = file;
		v._x10startLine = startline;
		v._x10endLine = endline;
		localVariables.add(v);
	}
	
	public void addClassMemberVariable(String name, String type, String containingClass)
	{
		if (memberVariables == null)
			memberVariables = new Hashtable<String, ArrayList<LineNumberMap.MemberVariableMapInfo>>();
		ArrayList<MemberVariableMapInfo> members = memberVariables.get(containingClass);
		if (members == null)
		{
			members = new ArrayList<LineNumberMap.MemberVariableMapInfo>();
			memberVariables.put(containingClass, members);
		}
		
		MemberVariableMapInfo v = new MemberVariableMapInfo();
		v._x10type = determineTypeId(type);
		if (v._x10type == 203)
			v._x10typeIndex = determineSubtypeId(type, refMap);
		else if (v._x10type == 200 || v._x10type == 202 || v._x10type == 204 || v._x10type == 207)
			v._x10typeIndex = determineSubtypeId(type, arrayMap);
		else 
			v._x10typeIndex = -1;
		v._x10memberName = stringId(name);
		v._cppMemberName = stringId("x10__"+Emitter.mangled_non_method_name(name));
		v._cppClass = stringId(containingClass);
		members.add(v);
	}

	/**
	 * @param method target method signature
	 * @return source method signature
	 */
	public String getMappedMethod(String method) {
		for (MethodDescriptor m : methods.keySet()) {
			String mm = m.toPrettyString(this, false);
			if (mm.equals(method))
				return methods.get(m).toPrettyString(this, false);
		}
		return null;
	}

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (Key pos : map.keySet()) {
            Entry entry = map.get(pos);
            sb.append(lookupString(pos.fileId)).append(":").append(pos.start_line).append("->");
            sb.append(lookupString(entry.fileId)).append(":").append(entry.line).append("\n");
        }
        sb.append("\n");
        for (MethodDescriptor md : methods.keySet()) {
			MethodDescriptor sm = methods.get(md);
			sb.append(md.toPrettyString(this, true)).append("->");
			sb.append(sm.toPrettyString(this, true)).append("\n");
		}
        return sb.toString();
    }

    /**
     * Is the map empty?
     * @return true if the map is empty.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Produces a string suitable for initializing a field in the generated file.
     * The resulting string can be parsed by {@link #importMap(String)}.
     */
    public String exportMap() {
        final StringBuilder sb = new StringBuilder();
        sb.append("F");
        exportStringMap(sb);
        sb.append(" L{");
        for (Key pos : map.keySet()) {
            Entry entry = map.get(pos);
            sb.append(pos.fileId).append(":");
            sb.append(pos.start_line).append("-").append(pos.end_line).append("->");
            sb.append(entry.fileId).append(":").append(entry.line).append(",");
        }
        sb.append("} M{");
        for (MethodDescriptor md : methods.keySet()) {
			MethodDescriptor sm = methods.get(md);
			sb.append(md.toString()).append("->").append(sm.toString()).append(";");
		}
        sb.append("}");
        return sb.toString();
    }

    /**
     * Parses a string into a {@link LineNumberMap}.
     * The string is produced by {@link #exportMap()}.
     * @param input the input string
     */
    public static LineNumberMap importMap(String input) {
        StringTokenizer st = new QuotedStringTokenizer(input, " ", "\"\'", '\\', true);
        String s = st.nextToken("{}");
        assert (s.equals("F"));
        ArrayList<String> strings = importStringMap(st);
        LineNumberMap res = new LineNumberMap(strings);
        if (!st.hasMoreTokens())
        	return res;
        s = st.nextToken("{}");
        assert (s.equals(" L"));
        s = st.nextToken();
        assert (s.equals("{"));
        while (st.hasMoreTokens()) {
            String t = st.nextToken(":}");
            if (t.equals("}"))
                break;
            int n = Integer.parseInt(t);
            t = st.nextToken();
            assert (t.equals(":"));
            int i = Integer.parseInt(st.nextToken("-"));
            t = st.nextToken();
            assert (t.equals("-"));
            int e = Integer.parseInt(st.nextToken("-"));
            t = st.nextToken(">");
            assert (t.equals("-"));
            t = st.nextToken(">");
            assert (t.equals(">"));
            int f = Integer.parseInt(st.nextToken(":"));
            t = st.nextToken();
            assert (t.equals(":"));
            int l = Integer.parseInt(st.nextToken(","));
            res.map.put(new Key(n, i, e), new Entry(f, l, -1));
            t = st.nextToken();
            assert (t.equals(","));
        }
        if (!st.hasMoreTokens())
        	return res;
        s = st.nextToken("{}");
        assert (s.equals(" M"));
        s = st.nextToken();
        assert (s.equals("{"));
        while (st.hasMoreTokens()) {
            String t = st.nextToken("-}");
            if (t.equals("}"))
                break;
            MethodDescriptor md = MethodDescriptor.parse(t);
            t = st.nextToken(">");
            assert (t.equals("-"));
            t = st.nextToken(">");
            assert (t.equals(">"));
            MethodDescriptor sm = MethodDescriptor.parse(st.nextToken(";"));
            res.methods.put(md, sm);
            t = st.nextToken();
            assert (t.equals(";"));
        }
        assert (!st.hasMoreTokens());
        return res;
    }

	/**
	 * Merges a set of new entries into a given map.  Changes the map in place!
	 * @param map the target map (changed in place!)
	 * @param newEntries the set of new entries
	 */
	private static void mergeMap(LineNumberMap map, LineNumberMap newEntries) {
		assert (map != null);
		final LineNumberMap m = map;
		final LineNumberMap n = newEntries;
		for (Key p : n.map.keySet()) {
//			assert (!m.map.containsKey(p));
			Entry e = n.map.get(p);
			m.put(n.lookupString(p.fileId), p.start_line, p.end_line, n.lookupString(e.fileId), e.line, e.column);
		}
		for (MethodDescriptor d : n.methods.keySet()) {
//		    if (m.methods.containsKey(d))
//		        assert (false) : d.toPrettyString(n)+" already present";
			assert (!m.methods.containsKey(d)) : d.toPrettyString(n)+" already present";
			MethodDescriptor e = n.methods.get(d);
			assert (e.lines == null);
			Key dk = new Key(m.stringId(n.lookupString(d.lines.fileId)), d.lines.start_line, d.lines.end_line);
			MethodDescriptor dp = m.createMethodDescriptor(n.lookupString(d.container),
					n.lookupString(d.name), n.lookupString(d.returnType), n.lookupStrings(d.args), dk);
			MethodDescriptor ep = m.createMethodDescriptor(n.lookupString(e.container),
					n.lookupString(e.name), n.lookupString(e.returnType), n.lookupStrings(e.args), null);
			m.methods.put(dp, ep);
		}
	}

	/**
	 * Merges a set of maps into a new map.  Returns the new map.
	 * @param maps the set of maps to merge
	 * @return the newly-created map containing merged entries from maps
	 */
	public static LineNumberMap mergeMaps(LineNumberMap[] maps) {
	    LineNumberMap map = new LineNumberMap();
	    for (int i = 0; i < maps.length; i++) {
	        mergeMap(map, maps[i]);
	    }
	    return map;
	}

	private String[] allFiles() {
	    TreeSet<String> files = new TreeSet<String>();
	    for (Entry e : map.values()) {
	        files.add(lookupString(e.fileId));
	    }
	    return files.toArray(new String[files.size()]);
	}

	private static int findFile(String name, String[] files) {
	    // files is assumed sorted alphanumerically
	    return Arrays.binarySearch(files, name);
	}

	private static class CPPLineInfo {
	    public final int x10index;
	    public final int x10method;
	    public final int cppindex;
	    public final int x10line;
	    public final int x10column;
	    public final int cppfromline;
	    public final int cpptoline;
	    public final int fileId;
	    public CPPLineInfo(int x10index, int x10method, int cppindex, int x10line, int cppfromline, int cpptoline, int fileId, int x10column) {
	        this.x10index = x10index;
	        this.x10method = x10method;
	        this.cppindex = cppindex;
	        this.x10line = x10line;
	        this.x10column = x10column;
	        this.cppfromline = cppfromline;
	        this.cpptoline = cpptoline;
	        this.fileId = fileId;
	    }
	    public static Comparator<CPPLineInfo> byX10info() {
	        return new Comparator<CPPLineInfo>() {
	            public int compare(CPPLineInfo o1, CPPLineInfo o2) {
	                int index_d = o1.x10index - o2.x10index;
	                if (index_d != 0) return index_d;
	                return o1.x10line - o2.x10line;
	            }
	        };
	    }
	    public static Comparator<CPPLineInfo> byCPPinfo() {
	        return new Comparator<CPPLineInfo>() {
	            public int compare(CPPLineInfo o1, CPPLineInfo o2) {
	                int index_d = o1.cppindex - o2.cppindex;
	                if (index_d != 0) return index_d;
	                return o1.cppfromline - o2.cppfromline;
	            }
	        };
	    }
	}

	private class CPPMethodInfo implements Comparable<CPPMethodInfo> {
	    public final int x10class;
	    public final int x10method;
	    public final int x10rettype;
	    public final int[] x10args;
	    public final int cppclass;
	    public int cpplineindex;
	    public CPPMethodInfo(int x10class, int x10method, int x10rettype, int[] x10args, int cppclass) {
	        this.x10class = x10class;
	        this.x10method = x10method;
	        this.x10rettype = x10rettype;
	        this.x10args = x10args;
	        this.cppclass = cppclass;
	    }
	    private String concatArgs() {
	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < x10args.length; i++) {
	            sb.append(lookupString(x10args[i]));
	        }
	        return sb.toString();
	    }
	    public int compareTo(CPPMethodInfo o) {
	        int class_d = lookupString(x10class).compareTo(lookupString(o.x10class));
	        if (class_d != 0) return class_d;
	        int name_d = lookupString(x10method).compareTo(lookupString(o.x10method));
	        if (name_d != 0) return name_d;
	        return concatArgs().compareTo(o.concatArgs());
	    }
	}

	private static final String _X10_DEBUG = "_X10_DEBUG";
	private static final String _X10_DEBUG_DATA = "_X10_DEBUG_DATA";

	/**
	 * Generates code for the line number map as required by the Toronto C++
	 * Debugger backend into the specified stream.
	 * @param w the output stream
	 * @param m the map to export
	 */
	public static void exportForCPPDebugger(ClassifiedStream w, LineNumberMap m) {
	    String debugSectionAttr = "__attribute__((section(\""+_X10_DEBUG+"\")))";
	    String debugDataSectionAttr = "__attribute__((section(\""+_X10_DEBUG_DATA+"\")))";
	    int size = m.size();
	    int offset = 0;
	    int[] offsets = new int[size];
	    // All strings, concatenated, with intervening nulls.
	    w.writeln("static const char _X10strings[] __attribute__((used)) "+debugDataSectionAttr+" =");
	    for (int i = 0; i < size; i++) {
	        offsets[i] = offset;
	        String s = m.lookupString(i);
	        w.writeln("    \""+StringUtil.escape(s)+"\\0\" //"+offsets[i]);
	        offset += s.length()+1;
	    }
	    w.writeln("    \"\";");
	    w.forceNewline();

        if (!m.isEmpty()) {
		    String[] files = m.allFiles();
		    // A list of X10 source files that contributed to the generation of the current C++ file.
		    w.writeln("static const struct _X10sourceFile _X10sourceList[] __attribute__((used)) "+debugDataSectionAttr+" = {");
		    for (int i = 0; i < files.length; i++) {
		        w.write("    { ");
		        w.write(""+0+", ");                                                // FIXME: _numLines
		        w.write(""+offsets[m.stringId(files[i])]);                         // _stringIndex
		        w.writeln(" },");
		    }
		    w.writeln("};");
		    w.forceNewline();
	
	//	    // A cross reference of X10 statements to the first C++ statement.
	//	    // Sorted by X10 file index and X10 source file line.
		    ArrayList<CPPLineInfo> x10toCPPlist = new ArrayList<CPPLineInfo>(m.map.size());
		    for (Key p : m.map.keySet()) {
		        Entry e = m.map.get(p);
		        x10toCPPlist.add(
		                new CPPLineInfo(findFile(m.lookupString(e.fileId), files), // _X10index
		                                0,                                         // FIXME: _X10method
		                                offsets[p.fileId],                         // _CPPindex
		                                e.line,                                    // _X10line
		                                p.start_line,                              // _CPPline
		                                p.end_line,
		                                p.fileId,
		                                e.column));                                  // _X10column	                                
		    }
		    Collections.sort(x10toCPPlist, CPPLineInfo.byX10info());
		    
		    // remove itens that have duplicate lines, leaving only the one with the earlier column
		    int previousLine=-1, previousColumn=-1;
		    for (int i=0; i<x10toCPPlist.size();)
		    {
		    	CPPLineInfo cppDebugInfo = x10toCPPlist.get(i);
		    	if (cppDebugInfo.x10line == previousLine)
		    	{
		    		if (cppDebugInfo.x10column >= previousColumn)
			    		x10toCPPlist.remove(i); // keep the previous one, delete this one
		    		else
		    		{
		    			// keep this one, delete the previous one
		    			x10toCPPlist.remove(i-1);
		    			previousLine = 	cppDebugInfo.x10line;
				    	previousColumn = cppDebugInfo.x10column;
		    		}
		    	}
		    	else
		    	{
			    	previousLine = cppDebugInfo.x10line;
			    	previousColumn = cppDebugInfo.x10column;
			    	i++;
		    	}
		    }	
		    
		    w.writeln("static const struct _X10toCPPxref _X10toCPPlist[] __attribute__((used)) "+debugDataSectionAttr+" = {");
		    for (CPPLineInfo cppDebugInfo : x10toCPPlist) {
		        w.write("    { ");
		        w.write(""+cppDebugInfo.x10index+", ");                            // _X10index
		        w.write(""+cppDebugInfo.x10method+", ");                           // _X10method
		        w.write(""+cppDebugInfo.cppindex+", ");                            // _CPPindex
		        w.write(""+cppDebugInfo.x10line+", ");                             // _X10line
		        w.write(""+cppDebugInfo.cppfromline+", ");                         // _CPPFromLine
		        w.write(""+cppDebugInfo.cpptoline);                                // _CPPtoLine
		        w.writeln(" },");
		    }
		    w.writeln("};");
		    w.forceNewline();
	
		    // A list of the X10 method names.
		    // Sorted by X10 method name.
		    ArrayList<CPPMethodInfo> x10MethodList = new ArrayList<CPPMethodInfo>(m.methods.size());
		    HashMap<Key, CPPMethodInfo> keyToMethod = new HashMap<Key, CPPMethodInfo>();
		    for (MethodDescriptor md : m.methods.keySet()) {
		        MethodDescriptor sm = m.methods.get(md);
		        final CPPMethodInfo cmi = m.new CPPMethodInfo(sm.container,        // _x10class
		                                                      sm.name,             // _x10method
		                                                      sm.returnType,       // _x10returnType
		                                                      sm.args,             // _x10args
		                                                      md.container);       // _cppClass
		        x10MethodList.add(cmi);
		        keyToMethod.put(md.lines, cmi);
		    }
	
		    // A cross reference of C++ statements to X10 statements.
		    // Sorted by C++ file index and C++ source file line. 
		    // A line range is used to minimize the storage required.
		    ArrayList<CPPLineInfo> cpptoX10xrefList = new ArrayList<CPPLineInfo>(m.map.size());
		    for (Key p : m.map.keySet()) {
		        Entry e = m.map.get(p);
		        cpptoX10xrefList.add(
		                new CPPLineInfo(findFile(m.lookupString(e.fileId), files), // _X10index
		                                0,                                         // FIXME: _X10method
		                                offsets[p.fileId],                         // _CPPindex
		                                e.line,                                    // _X10line
		                                p.start_line,                              // _CPPfromline
		                                p.end_line,                                // _CPPtoline
		                                p.fileId,
		                                e.column));
		    }
		    Collections.sort(cpptoX10xrefList, CPPLineInfo.byCPPinfo());
		    w.writeln("static const struct _CPPtoX10xref _CPPtoX10xrefList[] __attribute__((used)) "+debugDataSectionAttr+" = {");
		    int i = 0;
		    for (CPPLineInfo cppDebugInfo : cpptoX10xrefList) {
		        w.write("    { ");
		        w.write(""+cppDebugInfo.x10index+", ");                            // _X10index
		        w.write(""+cppDebugInfo.x10method+", ");                           // _X10method
		        w.write(""+cppDebugInfo.cppindex+", ");                            // _CPPindex
		        w.write(""+cppDebugInfo.x10line+", ");                             // _X10line
		        w.write(""+cppDebugInfo.cppfromline+", ");                         // _CPPfromline
		        w.write(""+cppDebugInfo.cpptoline);                                // _CPPtoline
		        w.writeln(" },");
		        Key k = new Key(cppDebugInfo.fileId, cppDebugInfo.cppfromline, cppDebugInfo.cpptoline);
		        CPPMethodInfo methodInfo = keyToMethod.get(k);
		        if (methodInfo != null) {
		            methodInfo.cpplineindex = i;                                   // _lineIndex
		        }
		        i++;
		    }
		    w.writeln("};");
		    w.forceNewline();
	
	        if (!m.methods.isEmpty()) {
		        Collections.sort(x10MethodList);
		        // FIXME: Cannot put _X10methodNameList in debugDataSectionAttr, because it's not constant
		        // (the strings cause static initialization for some reason)
		        w.writeln("static const struct _X10methodName _X10methodNameList[] __attribute__((used)) = {");
		        w.writeln("#if defined(__xlC__)");
		        for (CPPMethodInfo cppMethodInfo : x10MethodList) {        	
		            w.write("    { ");
		            w.write(""+offsets[cppMethodInfo.x10class]+", ");                  // _x10class
		            w.write(""+offsets[cppMethodInfo.x10method]+", ");                 // _x10method
		            w.write(""+offsets[cppMethodInfo.x10rettype]+", ");                // _x10returnType
		            w.write(""+offsets[cppMethodInfo.cppclass]+", ");                  // _cppClass
		            w.write("(uint64_t) 0, "); // TODO - this needs to be re-designed, with the debugger team            
		            w.write(""+cppMethodInfo.x10args.length+", ");                     // _x10argCount
		            w.write(""+cppMethodInfo.cpplineindex);                            // _lineIndex
		            w.writeln(" },");
		        }
		        w.writeln("#else");
		        for (CPPMethodInfo cppMethodInfo : x10MethodList) {        	
		            w.write("    { ");
		            w.write(""+offsets[cppMethodInfo.x10class]+", ");                  // _x10class
		            w.write(""+offsets[cppMethodInfo.x10method]+", ");                 // _x10method
		            w.write(""+offsets[cppMethodInfo.x10rettype]+", ");                // _x10returnType
		            w.write(""+offsets[cppMethodInfo.cppclass]+", ");                  // _cppClass
		            w.write("(uint64_t) ");
		            for (i = 0; i < cppMethodInfo.x10args.length; i++) {
		                int a = cppMethodInfo.x10args[i];
		                w.write("\""+encodeIntAsChars(offsets[a])+"\" ");              // _x10args
		            }
		            w.write("\"\", ");
		            w.write(""+cppMethodInfo.x10args.length+", ");                     // _x10argCount
		            w.write(""+cppMethodInfo.cpplineindex);                            // _lineIndex
		            w.writeln(" },");
		        }
		        w.writeln("#endif");
		        w.writeln("};");
		        w.forceNewline();
	        }
	        
	        // variable map stuff
	        if (localVariables != null)
	        {
		        w.writeln("static const struct _X10LocalVarMap _X10variableNameList[] __attribute__((used)) "+debugDataSectionAttr+" = {");
		        for (LocalVariableMapInfo v : localVariables)
		        	w.writeln("    { "+offsets[v._x10name]+", "+v._x10type+", "+(v._x10type==101?offsets[v._x10typeIndex]:v._x10typeIndex)+", "+offsets[v._cppName]+", "+findFile(v._x10index, files)+", "+v._x10startLine+", "+v._x10endLine+" }, // "+m.lookupString(v._x10name));
		        w.writeln("};");
		        w.forceNewline();
	        }
        }
	        
        if (memberVariables != null)
        {
        	for (String classname : memberVariables.keySet())
        	{
		        w.writeln("static const struct _X10TypeMember _X10"+classname.substring(classname.lastIndexOf('.')+1)+"Members[] __attribute__((used)) "+debugDataSectionAttr+" = {");
		        for (MemberVariableMapInfo v : memberVariables.get(classname))
		        	w.writeln("    { "+v._x10type+", "+v._x10typeIndex+", "+offsets[v._x10memberName]+", "+offsets[v._cppMemberName]+", "+offsets[v._cppClass]+" }, // "+m.lookupString(v._x10memberName));
			    w.writeln("};");
			    w.forceNewline();
        	}
        	w.writeln("static const struct _X10ClassMap _X10ClassMapList[] __attribute__((used)) /*"+debugDataSectionAttr+"*/ = {");
        	for (String classname : memberVariables.keySet())
	        	w.writeln("    { 101, "+offsets[memberVariables.get(classname).get(0)._cppClass]+", sizeof("+classname.replace(".", "::")+"), "+memberVariables.get(classname).size()+", _X10"+classname.substring(classname.lastIndexOf('.')+1)+"Members },");	        
        	w.writeln("};");
        	w.forceNewline();
        }
        	    
	    if (!closureMap.isEmpty())
	    {
		    w.writeln("static const struct _X10ClosureMap _X10ClosureMapList[] __attribute__((used)) "+debugDataSectionAttr+" = {");
		    // TODO
		    w.writeln("};");
		    w.forceNewline();
	    }
	    
	    if (!arrayMap.isEmpty())
	    {
		    w.writeln("static const struct _X10ArrayMap _X10ArrayMapList[] __attribute__((used)) "+debugDataSectionAttr+" = {");
		    Iterator<Integer> iterator = arrayMap.iterator();
		    while(iterator.hasNext())
		    	w.writeln("    { "+iterator.next()+", "+iterator.next()+" },");
		    w.writeln("};");
		    w.forceNewline();
	    }
	    
	    if (!refMap.isEmpty())
	    {
		    w.writeln("static const struct _X10RefMap _X10RefMapList[] __attribute__((used)) "+debugDataSectionAttr+" = {");
		    Iterator<Integer> iterator = refMap.iterator();
		    while(iterator.hasNext())
		    	w.writeln("    { "+iterator.next()+", "+iterator.next()+" },");
		    w.writeln("};");
		    w.forceNewline();
	    }	    

        // A meta-structure that refers to all of the above
        w.write("static const struct _MetaDebugInfo_t _MetaDebugInfo __attribute__((used)) "+debugSectionAttr+" = {");
        w.newline(4); w.begin(0);
        w.writeln("sizeof(struct _MetaDebugInfo_t),");
        w.writeln("X10_META_LANG,");
        w.writeln("0,");
        w.writeln("sizeof(_X10strings),");
        if (!m.isEmpty()) {
            w.writeln("sizeof(_X10sourceList),");
            w.writeln("sizeof(_X10toCPPlist),"); // w.writeln("0,");
            w.writeln("sizeof(_CPPtoX10xrefList),");
        } else {
            w.writeln("0,");
            w.writeln("0,");
            w.writeln("0,");
        }
        if (!m.methods.isEmpty()) {
            w.writeln("sizeof(_X10methodNameList),");
        } else {
            w.writeln("0, // no member variable mappings");
        }
        if (!m.isEmpty() && localVariables != null)
        	w.writeln("sizeof(_X10variableNameList),");
        else
        	w.writeln("0,  // no local variable mappings");
        if (memberVariables != null)
        	w.writeln("sizeof(_X10ClassMapList),");
        else
        	w.writeln("0, // no class mappings");
        if (!closureMap.isEmpty())        	
        	w.writeln("sizeof(_X10ClosureMapList),");
        else
        	w.writeln("0,  // no closure mappings");
        if (!arrayMap.isEmpty())
        	w.writeln("sizeof(_X10ArrayMapList),");
        else
        	w.writeln("0, // no array mappings");
        if (!refMap.isEmpty())
        	w.writeln("sizeof(_X10RefMapList),");
        else
        	w.writeln("0, // no reference mappings");
        
        w.writeln("_X10strings,");
        if (!m.isEmpty()) {
            w.writeln("_X10sourceList,");
            w.writeln("_X10toCPPlist,");  // w.writeln("NULL,");
            w.writeln("_CPPtoX10xrefList,");
        } else {
            w.writeln("NULL,");
            w.writeln("NULL,");
            w.writeln("NULL,");
        }
        if (!m.methods.isEmpty()) {
            w.writeln("_X10methodNameList,");
            m.methods.clear();
        } else {
            w.writeln("NULL,");
        }
        
        if (localVariables != null)
        {
        	if (!m.isEmpty()) 
        		w.writeln("_X10variableNameList,");
        	else 
        		w.writeln("NULL,");
        	localVariables.clear();
        	localVariables = null;
        }
        else
        	w.writeln("NULL,");
        if (memberVariables != null)
        {
        	w.writeln("_X10ClassMapList,");
        	memberVariables.clear();
        	memberVariables = null;
        }
        else
        	w.writeln("NULL,");
        if (!closureMap.isEmpty())
        {
        	closureMap.clear();
        	w.writeln("_X10ClosureMapList,");
        }
        else
        	w.writeln("NULL,");
        if (!arrayMap.isEmpty())
        {
        	arrayMap.clear();
        	w.writeln("_X10ArrayMapList,");
        }
        else
        	w.writeln("NULL,");
        if (!refMap.isEmpty())
        {
        	refMap.clear();
        	w.write("_X10RefMapList");
        }
        else
        	w.write("NULL");
        
        w.end(); w.newline();
        w.writeln("};");
	}

	private static String encodeIntAsChars(int i) {
	    String b1 = "\\"+Integer.toOctalString((i >> 24) & 0xFF);
	    String b2 = "\\"+Integer.toOctalString((i >> 16) & 0xFF);
	    String b3 = "\\"+Integer.toOctalString((i >>  8) & 0xFF);
	    String b4 = "\\"+Integer.toOctalString((i >>  0) & 0xFF);
	    return b1+b2+b3+b4;
	}
}
