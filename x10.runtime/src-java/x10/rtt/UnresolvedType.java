package x10.rtt;

import java.util.List;

public final class UnresolvedType implements Type {

    int index = -1;
    
    public UnresolvedType(int index) {
        this.index = index;
    }

    public String toString() {
        return "UnresolvedType(" + index + ")";
    }
    
    public final int arrayLength(Object array) {
        throw new UnsupportedOperationException();
    }

    public final Object getArray(Object array, int i) {
        throw new UnsupportedOperationException();
    }

    public final Class<?> getJavaClass() {
        throw new UnsupportedOperationException();
    }

//    public final List getTypeParameters() {
//        throw new UnsupportedOperationException();
//    }

    public final boolean instanceof$(Object o) {
        throw new UnsupportedOperationException();
    }

    public final boolean isSubtype(Type o) {
        throw new UnsupportedOperationException();
    }

    public final Object makeArray(int length) {
        throw new UnsupportedOperationException();
    }

    public Object makeArray(Object... elems) {
        throw new UnsupportedOperationException();
    }

    public final Object setArray(Object array, int i, Object v) {
        throw new UnsupportedOperationException();
    }

    public final String typeName() {
        throw new UnsupportedOperationException();
    }

}
