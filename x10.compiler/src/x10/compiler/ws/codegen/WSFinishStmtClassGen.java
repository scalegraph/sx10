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


package x10.compiler.ws.codegen;

import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.types.ClassDef;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import x10.ast.Finish;
import x10.compiler.ws.util.TransCodes;
import x10.compiler.ws.util.WSCodeGenUtility;
import x10.types.X10ClassType;
import x10.util.synthesizer.ClassSynth;
import x10.util.synthesizer.CodeBlockSynth;
import x10.util.synthesizer.ConstructorSynth;
import x10.util.synthesizer.InstanceCallSynth;
import x10.util.synthesizer.SuperCallSynth;
import x10.util.synthesizer.SwitchSynth;

/**
 * @author Haichuan
 * 
 * Transform a finish stmt to a inner class
 * 
 */
public class WSFinishStmtClassGen extends AbstractWSClassGen {
    public WSFinishStmtClassGen(AbstractWSClassGen parent, Finish finishStmt) {
        super(parent, parent,
                WSCodeGenUtility.getFinishStmtClassName(parent.getClassName()),
                parent.wts.finishFrameType, finishStmt.body());
        
        if(WSOptimizeConfig.OPT_PC_FIELD == 0){
            addPCField();
        }
    }

    protected void genClassConstructor() throws SemanticException {
        Expr upRef = conSynth.addFormal(compilerPos, Flags.FINAL, wts.frameType, "up"); //up:Frame!
        
        CodeBlockSynth codeBlockSynth = conSynth.createConstructorBody(compilerPos);
        SuperCallSynth superCallSynth = codeBlockSynth.createSuperCall(compilerPos, classSynth.getClassDef());
        superCallSynth.addArgument(wts.frameType, upRef);
    }

    @Override
    protected void genMethods() throws SemanticException {
        
        CodeBlockSynth fastBodySynth = fastMSynth.getMethodBodySynth(compilerPos);
        CodeBlockSynth resumeBodySynth = resumeMSynth.getMethodBodySynth(compilerPos);
        CodeBlockSynth backBodySynth = backMSynth.getMethodBodySynth(compilerPos);
        

        AbstractWSClassGen childFrameGen = genChildFrame(wts.regularFrameType, codeBlock, WSCodeGenUtility.getBlockFrameClassName(getClassName()));
        TransCodes callCodes = this.genInvocateFrameStmts(1, childFrameGen);
        
        //now add codes to three path;
        //fast path
        fastBodySynth.addStmts(callCodes.first());
        
        //resume/back path
        if(WSOptimizeConfig.OPT_PC_FIELD == 0){
            Expr pcRef = synth.makeFieldAccess(compilerPos, getThisRef(), PC, xct);
            
            SwitchSynth resumeSwitchSynth = resumeBodySynth.createSwitchStmt(compilerPos, pcRef);
            SwitchSynth backSwitchSynth = backBodySynth.createSwitchStmt(compilerPos, pcRef);      
            resumeSwitchSynth.insertStatementsInCondition(0, callCodes.second());
            if(callCodes.third().size() > 0){ //only assign call has back
                backSwitchSynth.insertStatementsInCondition(callCodes.getPcValue(), callCodes.third());
                backSwitchSynth.insertStatementInCondition(callCodes.getPcValue(), xnf.Break(compilerPos));
            }
        }
   }

}
