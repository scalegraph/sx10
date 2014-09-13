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

package polyglot.ast;

/**
 * An <code>ExtFactory</code> constructs extensions. It is only used by
 * a <code>NodeFactory</code>, during the creation of AST nodes. ExtFactories
 * may be chained together (see AbstractExtFactory_c) to allow extensions to be
 * composed.
 */
public interface ExtFactory
{

    /**
     * The next extFactory in the chain. 
     */
    ExtFactory nextExtFactory();

    //////////////////////////////////////////////////////////////////
    // Factory Methods
    //////////////////////////////////////////////////////////////////

    Ext extFlagsNode();
    
    Ext extId();
    
    Ext extAmbAssign();

    Ext extAmbExpr();
    
    Ext extAmbPrefix();
    
    Ext extAmbReceiver();
    
    Ext extAmbTypeNode();
    
    Ext extArrayAccess();
    
    Ext extArrayInit();
    
    Ext extArrayTypeNode();
    
    Ext extAssert();
    
    Ext extAssign();

    Ext extLocalAssign();
    Ext extFieldAssign();
    Ext extArrayAccessAssign();
    
    Ext extBinary();
    
    Ext extBlock();
    
    Ext extBooleanLit();
    
    Ext extBranch();
    
    Ext extCall();
    
    Ext extCanonicalTypeNode();
    
    Ext extCase();
    
    Ext extCast();
    
    Ext extCatch();
    
    Ext extCharLit();
    
    Ext extClassBody();
    
    Ext extClassDecl();

    Ext extClassLit();

    Ext extClassMember();

    Ext extCodeDecl();
    
    Ext extCompoundStmt();
    
    Ext extConditional();
    
    Ext extConstructorCall();
    
    Ext extConstructorDecl();
    
    Ext extDo();
    
    Ext extEmpty();
    
    Ext extEval();
    
    Ext extExpr();
    
    Ext extField();
    
    Ext extFieldDecl();
    
    Ext extFloatLit();
    
    Ext extFor();
    
    Ext extFormal();
    
    Ext extIf();
    
    Ext extImport();
    
    Ext extInitializer();
    
    Ext extInstanceof();
    
    Ext extIntLit();
    
    Ext extLabeled();
    
    Ext extLit();
    
    Ext extLocal();
    
    Ext extLocalClassDecl();
    
    Ext extLocalDecl();
    
    Ext extLoop();
    
    Ext extMethodDecl();
    
    Ext extNewArray();
    
    Ext extNode();
    
    Ext extNodeList();
    
    Ext extNew();
    
    Ext extNullLit();
    
    Ext extNumLit();
    
    Ext extPackageNode();
    
    Ext extProcedureDecl();

    Ext extReturn();
    
    Ext extSourceCollection();
    
    Ext extSourceFile();
    
    Ext extSpecial();
    
    Ext extStmt();
    
    Ext extStringLit();
    
    Ext extSwitchBlock();
    
    Ext extSwitchElement();
    
    Ext extSwitch();
    
    Ext extSynchronized();
    
    Ext extTerm();
    
    Ext extThrow();
    
    Ext extTry();
    
    Ext extTypeNode();
    
    Ext extUnary();
    
    Ext extWhile();
}
