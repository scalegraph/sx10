/**
 * 
 */
package polyglot.ext.x10.types;

import java.util.List;

import polyglot.types.ProcedureDef;
import polyglot.types.ProcedureInstance;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import x10.constraint.XConstraint;

/**
 * @author vj
 *
 */
public interface X10ProcedureInstance<T extends ProcedureDef> extends X10TypeObject, ProcedureInstance<T> {
    boolean callValidNoClauses(Type thisType, List<Type> argTypes);

    // Constructors, methods, and closures all have return types.
    Type returnType();
    X10ProcedureInstance<T> returnType(Type t);
    
    List<Type> typeParameters();
    X10ProcedureInstance<T> typeParameters(List<Type> typeParameters);
    
    /**
     * Return the constraint on the formal parameters, if any.
     * @return
     */
    XConstraint whereClause();
    X10ProcedureInstance<T> whereClause(XConstraint where);
}
