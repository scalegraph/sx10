/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.util;

import java.io.*;
import java.util.*;

import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import x10.util.CollectionFactory;

/** Output stream for writing type objects. */
public class TypeOutputStream extends ObjectOutputStream
{
    protected TypeSystem ts;
    protected Set<TypeObject> roots;
    protected Map<Object,Object> placeHolders;
    
    public TypeOutputStream(OutputStream out, TypeSystem ts, TypeObject root) 
        throws IOException
    {
        super( out);
        
        this.ts = ts;
        this.roots = ts.getTypeEncoderRootSet(root);
        this.placeHolders = CollectionFactory.newHashMap();
        
        if (Report.should_report(Report.serialize, 2)) {
            Report.report(2, "Began TypeOutputStream with roots: " + roots);
        }
        
        enableReplaceObject( true);
    }
    
    protected Object placeHolder(TypeObject o, boolean useRoots) {
        Object k = new IdentityKey(o);
        Object p = placeHolders.get(k);
        if (p == null) {
            p = ts.placeHolder(o, useRoots ? roots : Collections.<TypeObject>emptySet());
            placeHolders.put(k, p);
        }
        return p;
    }
    
    protected Object replaceObject(Object o) throws IOException {
        if (o instanceof TypeObject) {
            Object r;
            
            if (roots.contains(o)) {
                if (Report.should_report(Report.serialize, 2)) {
                    Report.report(2, "+ In roots: " + o + " : " + o.getClass());
                }
                
                r = o;
            }
            else {
                r = placeHolder((TypeObject) o, true);
            }
            
            if (Report.should_report(Report.serialize, 2)) {
                if (r != o) {
                    Report.report(2, "+ Replacing: " + o + " : " + o.getClass()
                                  + " with " + r);
                } 
                else {
                    Report.report(2, "+ " + o + " : " + o.getClass());
                }
            }
                
            return r;
        }
        else {
            if (Report.should_report(Report.serialize, 2)) {
                Report.report(2, "+ " + o + " : " + o.getClass());
            }
            return o;
        }
    }
}
