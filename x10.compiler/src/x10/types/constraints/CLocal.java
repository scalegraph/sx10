/**
 * 
 */
package x10.types.constraints;

import polyglot.types.Type;
import polyglot.types.Types;
import x10.constraint.XLocal;
import x10.types.X10LocalDef;


/**
 * A representation of a local variable, with its associated type information.
 * <p>In essence, a CLocal is a serializable representation of an X10LocalInstance.
 * @author vj
 *
 */
public class CLocal extends XLocal<X10LocalDef> {
    
    String s; // just for documentation
    public CLocal(X10LocalDef ld) {
        super(ld);
        s=ld.name().toString();
    }
    
    public CLocal(X10LocalDef ld, String s) {
        super(ld);
        this.s=s;
    }
    public X10LocalDef localDef() {
        return name;
    }
    public Type type() {
        return Types.get(name.type());
    }

    @Override public String toString() {
	return s;
    }
}
