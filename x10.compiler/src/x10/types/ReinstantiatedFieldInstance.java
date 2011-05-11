/**
 * 
 */
package x10.types;

import polyglot.types.Ref;
import polyglot.types.ContainerType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import x10.types.constraints.CConstraint;

final class ReinstantiatedFieldInstance extends X10FieldInstance_c {
	private static final long serialVersionUID = 8234625319808346804L;

	private final TypeParamSubst typeParamSubst;
	private final X10FieldInstance fi;

	ReinstantiatedFieldInstance(TypeParamSubst typeParamSubst, TypeSystem ts, Position pos,
			Ref<? extends X10FieldDef> def, X10FieldInstance fi) {
		super(ts, pos, def);
		this.typeParamSubst = typeParamSubst;
		this.fi = fi;
	}

    public TypeParamSubst typeParamSubst() {
        ContainerType ct = fi.container();
        if (ct != null && ct.isClass()) {
            TypeParamSubst dsubst = ct.toClass().def().subst();
            if (dsubst != null) {
                return dsubst.reinstantiate(typeParamSubst);
            }
        }
        return typeParamSubst;
    }

    @Override
	public Type type() {
		if (type == null)
			return this.typeParamSubst().reinstantiate(fi.type());
		return type;
	}

	@Override
	public ContainerType container() {
		if (container == null)
			return this.typeParamSubst().reinstantiate(fi.container());
		return container;
	}
}
