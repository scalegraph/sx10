package com.ibm.wala.cast.x10.translator.polyglot;

import polyglot.frontend.JobExt;
import polyglot.frontend.Scheduler;
import x10.ExtensionInfo;

import com.ibm.wala.cast.java.translator.polyglot.IRTranslatorExtension;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotIdentityMapper;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotSourceLoaderImpl;
import com.ibm.wala.cast.tree.impl.CAstRewriterFactory;
import com.ibm.wala.cast.x10.analysis.AnalysisJobExt;
import com.ibm.wala.types.ClassLoaderReference;

public abstract class X10ExtensionInfo extends ExtensionInfo implements IRTranslatorExtension {
    protected X10SourceLoaderImpl fSourceLoader;
    protected PolyglotIdentityMapper fMapper;
    protected CAstRewriterFactory rewriterFactory;

    public void setSourceLoader(PolyglotSourceLoaderImpl sourceLoader) {
	fSourceLoader = (X10SourceLoaderImpl) sourceLoader;
	fMapper= new X10PolyglotIdentityMapper(fSourceLoader.getReference());
    }

    public X10SourceLoaderImpl getSourceLoader() {
        return fSourceLoader;
    }

    public ClassLoaderReference getSourceLoaderReference() {
	return X10SourceLoaderImpl.X10SourceLoader;
    }

    protected Scheduler createScheduler() {
        return new X10WALAScheduler(this);
    }

    public JobExt jobExt() {
	return new AnalysisJobExt();
    }

    public PolyglotIdentityMapper getIdentityMapper() {
        return fMapper;
    }

    public void setCAstRewriterFactory(CAstRewriterFactory factory) {
        rewriterFactory = factory;
    }
    
    public CAstRewriterFactory getCAstRewriterFactory() {
        return rewriterFactory;
    }
    
	public boolean getReplicateForDoLoops() {
		return false;
	}

}
