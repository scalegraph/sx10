/*
 * Created on Oct 6, 2005
 */
package x10.wala.translator;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayAccessAssign;
import polyglot.ast.ArrayInit;
import polyglot.ast.ArrayTypeNode;
import polyglot.ast.Assert;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.BooleanLit;
import polyglot.ast.Branch;
import polyglot.ast.Call;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Case;
import polyglot.ast.Cast;
import polyglot.ast.Catch;
import polyglot.ast.CharLit;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassLit;
import polyglot.ast.Conditional;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Do;
import polyglot.ast.Empty;
import polyglot.ast.Eval;
import polyglot.ast.Field;
import polyglot.ast.FieldAssign;
import polyglot.ast.FieldDecl;
import polyglot.ast.FloatLit;
import polyglot.ast.For;
import polyglot.ast.Formal;
import polyglot.ast.If;
import polyglot.ast.Import;
import polyglot.ast.Initializer;
import polyglot.ast.Instanceof;
import polyglot.ast.IntLit;
import polyglot.ast.Labeled;
import polyglot.ast.Local;
import polyglot.ast.LocalAssign;
import polyglot.ast.LocalClassDecl;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.New;
import polyglot.ast.NewArray;
import polyglot.ast.NullLit;
import polyglot.ast.PackageNode;
import polyglot.ast.Return;
import polyglot.ast.Special;
import polyglot.ast.StringLit;
import polyglot.ast.Switch;
import polyglot.ast.SwitchBlock;
import polyglot.ast.Throw;
import polyglot.ast.Try;
import polyglot.ast.Unary;
import polyglot.ast.While;
import x10.ast.AssignPropertyCall;
import x10.ast.Async;
import x10.ast.AtStmt;
import x10.ast.Atomic;
import x10.ast.Clocked;
import x10.ast.Closure;
import x10.ast.ClosureCall;
import x10.ast.Finish;
import x10.ast.ForLoop;
import x10.ast.Here;
import x10.ast.LocalTypeDef;
import x10.ast.Next;
import x10.ast.ParExpr;
import x10.ast.SettableAssign;
import x10.ast.StmtSeq;
import x10.ast.Tuple;
import x10.ast.When;
import x10.ast.X10Formal;

import com.ibm.wala.cast.tree.CAstNode;

/**
 * An alternative visitor API for Polyglot, whose API is somewhat brain-damaged...
 * 
 * @author rfuhrer
 */
public interface X10TranslatorVisitor {
    CAstNode visit(MethodDecl m, WalkContext wc);
    CAstNode visit(ConstructorDecl cd, WalkContext wc);
    CAstNode visit(FieldDecl f, WalkContext wc);
    CAstNode visit(Import i, WalkContext wc);
    CAstNode visit(PackageNode p, WalkContext wc);
    CAstNode visit(CanonicalTypeNode ctn, WalkContext wc);
    CAstNode visit(ArrayTypeNode ctn, WalkContext wc);
    CAstNode visit(ArrayInit ai, WalkContext wc);
    CAstNode visit(ArrayAccessAssign aaa, WalkContext wc);
    CAstNode visit(FieldAssign fa, WalkContext wc);
    CAstNode visit(LocalAssign la, WalkContext wc);
    CAstNode visit(Binary b, WalkContext wc);
    CAstNode visit(Call c, WalkContext wc);
    CAstNode visit(ConstructorCall cc, WalkContext wc);
    CAstNode visit(Cast c, WalkContext wc);
    CAstNode visit(Conditional c, WalkContext wc);
    CAstNode visit(Instanceof io, WalkContext wc);
    CAstNode visit(BooleanLit bl, WalkContext wc);
    CAstNode visit(ClassLit cl, WalkContext wc);
    CAstNode visit(FloatLit fl, WalkContext wc);
    CAstNode visit(NullLit nl, WalkContext wc);
    CAstNode visit(CharLit cl, WalkContext wc);
    CAstNode visit(IntLit il, WalkContext wc);
    CAstNode visit(StringLit sl, WalkContext wc);
    CAstNode visit(New n, WalkContext wc);
    CAstNode visit(NewArray na, WalkContext wc);
    CAstNode visit(Special s, WalkContext wc);
    CAstNode visit(Unary u, WalkContext wc);
    CAstNode visit(ArrayAccess aa, WalkContext wc);
    CAstNode visit(Field f, WalkContext wc);
    CAstNode visit(Local l, WalkContext wc);
    CAstNode visit(ClassBody cb, WalkContext wc); // should never see this when producing CAstNodes
    CAstNode visit(ClassDecl cd, WalkContext wc); // should never see this when producing CAstNodes
    CAstNode visit(Initializer i, WalkContext wc); // should never see this when producing CAstNodes
    CAstNode visit(Assert a, WalkContext wc);
    CAstNode visit(Branch b, WalkContext wc);
    CAstNode visit(Block b, WalkContext wc);
    CAstNode visit(SwitchBlock sb, WalkContext wc);
    CAstNode visit(StmtSeq sb, WalkContext wc);
    CAstNode visit(Catch c, WalkContext wc);
    CAstNode visit(If i, WalkContext wc);
    CAstNode visit(Labeled l, WalkContext wc);
    CAstNode visit(LocalClassDecl lcd, WalkContext wc);
    CAstNode visit(Do d, WalkContext wc);
    CAstNode visit(For f, WalkContext wc);
    CAstNode visit(While w, WalkContext wc);
    CAstNode visit(Switch s, WalkContext wc);
    CAstNode visit(Try t, WalkContext wc);
    CAstNode visit(Empty e, WalkContext wc);
    CAstNode visit(Eval e, WalkContext wc);
    CAstNode visit(LocalDecl ld, WalkContext wc);
    CAstNode visit(Return r, WalkContext wc);
    CAstNode visit(Case c, WalkContext wc);
    CAstNode visit(Throw t, WalkContext wc);
    CAstNode visit(Formal f, WalkContext wc); // may not see these (might be handled by parent)
    CAstNode visit(Async a, WalkContext context);
    CAstNode visit(AssignPropertyCall a, WalkContext context);
    CAstNode visit(Atomic a, WalkContext context);
    CAstNode visit(AtStmt atStmt, WalkContext context);
    CAstNode visit(Clocked c, WalkContext context);
    CAstNode visit(Closure closure, WalkContext context);
    CAstNode visit(ClosureCall closureCall, WalkContext context);
    CAstNode visit(Finish f, WalkContext context);
    CAstNode visit(ForLoop f, WalkContext context);
    CAstNode visit(Here h, WalkContext context);
    CAstNode visit(LocalTypeDef l, WalkContext context);
    CAstNode visit(Next n, WalkContext context);
    CAstNode visit(ParExpr expr, WalkContext wc);
    CAstNode visit(Tuple t, WalkContext context);
    CAstNode visit(When w, WalkContext context);
    CAstNode visit(X10Formal f, WalkContext context);
    CAstNode visit(SettableAssign n, WalkContext wc);
}
