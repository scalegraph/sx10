package com.ibm.wala.cast.x10.ipa.callgraph;

import com.ibm.wala.cast.x10.classLoader.X10LanguageImpl;
import com.ibm.wala.cast.x10.classLoader.X10PrimordialClassLoaderImpl;
import com.ibm.wala.cast.x10.ipa.summaries.X10SyntheticLoaderImpl;
import com.ibm.wala.cast.x10.loader.X10SourceLoaderImpl;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.types.ClassLoaderReference;

import java.util.*;

public class X10SourceAnalysisScope extends JavaSourceAnalysisScope {
    private static final Set<Language> languages = new HashSet<Language>(2);

    static {
        // RMF 5/1/2009 - For now, we will only analyze X10 code. Any Java code will have to be modeled.
//      languages.add(Language.JAVA);
      languages.add(X10LanguageImpl.X10Lang);
    }

    public X10SourceAnalysisScope() {
	super(languages);

	loadersByName.put(X10PrimordialClassLoaderImpl.X10PrimordialName, X10PrimordialClassLoaderImpl.X10Primordial);
	loadersByName.put(X10SourceLoaderImpl.X10SourceLoaderName, X10SourceLoaderImpl.X10SourceLoader);
	loadersByName.put(X10SyntheticLoaderImpl.X10SyntheticLoaderName, X10SyntheticLoaderImpl.X10SyntheticLoader);
	
	setLoaderImpl(X10SourceLoaderImpl.X10SourceLoader, "com.ibm.wala.cast.x10.translator.polyglot.X10SourceLoaderImpl");
	setLoaderImpl(X10PrimordialClassLoaderImpl.X10Primordial, "com.ibm.wala.cast.x10.loader.X10PrimordialClassLoader");
	setLoaderImpl(X10SyntheticLoaderImpl.X10SyntheticLoader, "com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader");

	initSynthetic(X10SourceLoaderImpl.X10SourceLoader);
    }

    public ClassLoaderReference getX10PrimordialLoader() {
	return X10PrimordialClassLoaderImpl.X10Primordial;
    }

    public ClassLoaderReference getX10SourceLoader() {
	return X10SourceLoaderImpl.X10SourceLoader;
    }
}
