package polyglot.types;

import java.util.List;

/**
 * A container type is an X10 class or interface or struct type. It contains members such
 * as other types, fields, methods. (Used to be called StructType in Polyglot.)
 *
 */
public interface ContainerType extends Type {

    /**
     * Return a list of a all the type's members.
     * @return A list of <code>MemberInstance</code>.
     * @see polyglot.types.MemberDef
     */
    List<MemberInstance<?>> members();
    
    /**
     * Return the type's fields.
     * @return A list of <code>FieldInstance</code>.
     * @see polyglot.types.FieldDef
     */
    List<FieldInstance> fields();
    
    /**
     * Return the field named <code>name</code>, or null.
     */
    FieldInstance fieldNamed(Name name);

    /**
     * Return the type's methods.
     * @return A list of <code>MethodInstance</code>.
     * @see polyglot.types.MethodDef
     */
    List<MethodInstance> methods();

    /**
     * Return the methods named <code>name</code>, if any.
     * @param name Name of the method to search for.
     * @return A list of <code>MethodInstance</code>.
     * @see polyglot.types.MethodDef
     */
    List<MethodInstance> methodsNamed(Name name);

    /**
     * Return the methods named <code>name</code> with the given formal
     * parameter types, if any.
     * @param name Name of the method to search for.
     * @param argTypes A list of <code>Type</code>.
     * @return A list of <code>MethodInstance</code>.
     * @see polyglot.types.Type
     * @see polyglot.types.MethodDef
     */
    List<MethodInstance> methods(Name name, List<Type> argTypes, Context context);

    /**
     * Return the true if the type has the given method.
     */
    boolean hasMethod(MethodInstance mi, Context context);

}
