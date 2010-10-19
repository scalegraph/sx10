package polyglot.types;

public interface VarInstance<T extends VarDef> extends Use<T> {
    /**
     * The flags of the variable.
     */
    Flags flags();
//    VarInstance<T> flags(Flags flags);   // FIXME causes problems with javac; eclipse doesn't complain

    /**
     * The name of the variable.
     */
    Name name();
    VarInstance<T> name(Name name);

    /**
     * The type of the variable.
     */
    Type type();
    VarInstance<T> type(Type type);

    /**
     * The variable's constant value, or null.
     */
    Object constantValue();
    VarInstance<T> constantValue(Object o);
    VarInstance<T> notConstant();

    /**
     * Whether the variable has a constant value.
     */
    boolean isConstant();
}
