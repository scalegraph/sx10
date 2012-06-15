package x10.constraint;

import java.util.List;


public interface XConstraintSystem {
	public XConstraint mkConstraint(); 
	public XConstraint makeTrueConstraint();
	public XLit xtrue(); 
	public XLit xfalse(); 
	public XLit xnull(); 
	
	/**
	 * Make a fresh EQV with a system chosen name. 
	 * @return
	 */
	public XEQV makeEQV();
	/**
	 * Make a fresh UQV with a system chosen name. 
	 * @return
	 */
	public XUQV makeUQV();

	/**
	 * Make a fresh UQV whose name starts with prefix.
	 * @param prefix -- a prefix of the name for the returned UQV
	 * @return
	 */
	public XUQV makeUQV(String prefix);

	/**
	 * Make and return <code>receiver.field</code>.
	 * @param receiver
	 * @param field
	 * @return
	 */
	public <T> XField<T> makeField(XVar receiver, T field);
	public XField<Object> makeFakeField(XVar receiver, Object field);


    /** Make and return a literal containing o. null, true and false are
     * interned.
     */
	public XLit makeLit(Object o);
	
	public XFormula<Object> makeAtom(Object op, XTerm... terms);
	public XFormula<Object> makeAtom(Object op, boolean isAtomicFormula, XTerm... terms);
	
	public XTerm makeEquals(XTerm left, XTerm right);
	public XTerm makeDisEquals(XTerm left, XTerm right);
	public XTerm makeAnd(XTerm left, XTerm right);
	public XTerm makeNot(XTerm arg);

	//*************************************** Implementation
	/**
    Make and return op(terms1,..., termsn) -- an expression 
    with operator op and arguments terms. If atomicFormula is true
    then this is marked as an atomicFormula, else it is considered a term 
    (a function application term).
	 */

	
	public <T> XLocal<T> makeLocal(T name); 
}