package x10.wala.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import x10.wala.classLoader.X10LanguageImpl;
import x10.wala.tree.X10CAstEntity;

import com.ibm.wala.cast.ir.translator.AstTranslator.AstLexicalInformation;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.translator.SourceModuleTranslator;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public class X10SourceLoaderImpl extends JavaSourceLoaderImpl {
    public final static Atom X10SourceLoaderName = Atom.findOrCreateAsciiAtom("X10Source");

    public final static Atom X10 = Atom.findOrCreateUnicodeAtom("X10");

    public static ClassLoaderReference X10SourceLoader = new ClassLoaderReference(X10SourceLoaderName, X10, null);

    public X10SourceLoaderImpl(ClassLoaderReference loaderRef, IClassLoader parent, SetOfClasses exclusions, IClassHierarchy cha)
            throws IOException {
        super(loaderRef, parent, exclusions, cha);
    }

    @Override
    public ClassLoaderReference getReference() {
        return X10SourceLoader;
    }

    @Override
    public Language getLanguage() {
        return X10LanguageImpl.X10Lang;
    }

    public void defineAsync(CAstEntity fn, TypeReference asyncRef, CAstSourcePositionMap.Position fileName) {
        X10AsyncObject asyncType = new X10AsyncObject(asyncRef, X10LanguageImpl.X10Lang.getAnyType(), this, fileName, cha);
        fTypeMap.put(fn, asyncType);
        loadedClasses.put(asyncType.getName(), asyncType);
    }

    public void defineClosure(CAstEntity fn, TypeReference closureRef, CAstSourcePositionMap.Position fileName) {
        X10ClosureObject closureType = new X10ClosureObject(closureRef, X10LanguageImpl.X10Lang.getAnyType(), this, fileName,
                cha);
        fTypeMap.put(fn, closureType);
        loadedClasses.put(closureType.getName(), closureType);
    }

    public String toString() {
        return "X10 Source Loader (classes " + loadedClasses.values() + ")";
    }

    @Override
    public IClass defineType(CAstEntity type, String typeName, CAstEntity owner) {
        final Collection<TypeName> superTypeNames = new ArrayList<TypeName>();
        for (final Object superType : type.getType().getSupertypes()) {
            superTypeNames.add(TypeName.string2TypeName(((CAstType) superType).getName()));
        }
        final X10Class x10Class = new X10Class(typeName, superTypeNames, type.getPosition(), type.getQualifiers(), this,
                (owner != null) ? (JavaClass) fTypeMap.get(owner) : (JavaClass) null);
        fTypeMap.put(type, x10Class);
        loadedClasses.put(x10Class.getName(), x10Class);
        return x10Class;
    }

    private final class X10Class extends JavaClass {

        public X10Class(final String typeName, final Collection<TypeName> superTypeNames, final Position position,
                final Collection qualifiers, final JavaSourceLoaderImpl loader, final IClass enclosingClass) {
            super(typeName, superTypeNames, position, qualifiers, loader, enclosingClass);
        }

        @Override
        public IClass getSuperclass() {
            for (final Object superTypeName : superTypeNames) {
                final IClass domoType = lookupClass((TypeName) superTypeName);
                if (domoType != null && !domoType.isInterface()) {
                    return domoType;
                }
            }
            return null;
        }

        @Override
        public Collection<IClass> getDirectInterfaces() {
            final List<IClass> result = new ArrayList<IClass>();
            for (final Object superTypeName : superTypeNames) {
                final IClass domoType = lookupClass((TypeName) superTypeName);
                if (domoType != null && domoType.isInterface()) {
                    result.add(domoType);
                }
            }
            return result;
        }

    }

    @Override
    protected SourceModuleTranslator getTranslator() {
        return new SourceModuleTranslator() {
            public void loadAllSources(Set s) {
            }
        };
    }

    @Override
    public InstructionFactory getInstructionFactory() {
        return X10LanguageImpl.X10Lang.instructionFactory();
    }
}
