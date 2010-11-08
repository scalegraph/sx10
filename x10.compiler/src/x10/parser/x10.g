%options fp=X10Parser,states
%options list
%options la=6
%options variables=nt
%options conflicts
%options softkeywords
%options package=x10.parser
%options template=btParserTemplateF.gi
%options import_terminals="X10Lexer.gi"

%include
    "MissingId.gi"
%End

%Notice
    /./*
     *  This file is part of the X10 project (http://x10-lang.org).
     *
     *  This file is licensed to You under the Eclipse Public License (EPL);
     *  You may not use this file except in compliance with the License.
     *  You may obtain a copy of the License at
     *      http://www.opensource.org/licenses/eclipse-1.0.php
     *
     *  (C) Copyright IBM Corporation 2006-2010.
     */
    ./
%End

%Globals
    /.
    //#line $next_line "$input_file$"
    import java.util.Arrays;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.LinkedList;
    import java.util.List;
    import java.io.File;

    import polyglot.types.QName;
    import polyglot.types.Name;
    import polyglot.ast.AmbTypeNode;
    import polyglot.ast.AmbExpr;
    import polyglot.ast.Assign;
    import polyglot.ast.Binary;
    import polyglot.ast.Block;
    import polyglot.ast.Case;
    import polyglot.ast.Catch;
    import polyglot.ast.ClassBody;
    import polyglot.ast.ClassDecl;
    import polyglot.ast.ClassMember;
    import polyglot.ast.ConstructorCall;
    import polyglot.ast.ConstructorDecl;
    import polyglot.ast.Eval;
    import polyglot.ast.Expr;
    import polyglot.ast.Field;
    import polyglot.ast.FloatLit;
    import polyglot.ast.ForInit;
    import polyglot.ast.ForUpdate;
    import polyglot.ast.Formal;
    import polyglot.ast.Id;
    import polyglot.ast.Import;
    import polyglot.ast.IntLit;
    import polyglot.ast.LocalDecl;
    import polyglot.ast.MethodDecl;
    import polyglot.ast.FieldDecl;
    import polyglot.ast.Node;
    import polyglot.ast.NodeFactory;
    import polyglot.ast.PackageNode;
    import polyglot.ast.ProcedureDecl;
    import polyglot.ast.SourceFile;
    import polyglot.ast.Stmt;
    import polyglot.ast.SwitchElement;
    import polyglot.ast.TopLevelDecl;
    import polyglot.ast.TypeNode;
    import polyglot.ast.Unary;
    import polyglot.ast.FlagsNode;
    import polyglot.parse.ParsedName;
    import x10.ast.AddFlags;
    import x10.ast.AnnotationNode;
    import x10.ast.Closure;
    import x10.ast.ClosureCall;
    import x10.ast.SettableAssign;
    import x10.ast.Here;
    import x10.ast.DepParameterExpr;
    import x10.ast.Tuple;
    import x10.ast.When;
    import x10.ast.X10Formal;
    import x10.ast.X10Formal_c;
    import x10.ast.X10Loop;
    import x10.ast.X10Call;
    import x10.ast.ConstantDistMaker;
    import x10.ast.TypeDecl;
    import x10.ast.TypeParamNode;

    import x10.types.ParameterType;
    import polyglot.types.TypeSystem;

    import x10.ast.PropertyDecl;
    import x10.ast.RegionMaker;
    import x10.ast.X10Binary_c;
    import x10.ast.X10Unary_c;
    import x10.ast.X10IntLit_c;
    import x10.extension.X10Ext;
    import polyglot.frontend.FileSource;
    import polyglot.frontend.Parser;
    import polyglot.lex.BooleanLiteral;
    import polyglot.lex.CharacterLiteral;
    import polyglot.lex.DoubleLiteral;
    import polyglot.lex.FloatLiteral;
    import polyglot.lex.Identifier;
    import polyglot.lex.LongLiteral;
    import polyglot.lex.NullLiteral;
    import polyglot.lex.Operator;
    import polyglot.lex.StringLiteral;
    import polyglot.parse.VarDeclarator;
    import polyglot.types.Flags;
    import x10.types.X10Flags;
    import x10.types.checker.Converter;
    import x10.errors.Errors;
    import polyglot.types.TypeSystem;
    import polyglot.util.CollectionUtil;
    import polyglot.util.ErrorInfo;
    import polyglot.util.ErrorQueue;
    import polyglot.util.Position;
    import polyglot.util.TypedList;
    import polyglot.util.CollectionUtil;

    import lpg.runtime.BacktrackingParser;
    import lpg.runtime.BadParseException;
    import lpg.runtime.BadParseSymFileException;
    import lpg.runtime.DiagnoseParser;
    import lpg.runtime.IToken;
    import lpg.runtime.NotBacktrackParseTableException;
    import lpg.runtime.NullExportedSymbolsException;
    import lpg.runtime.NullTerminalSymbolsException;
    import lpg.runtime.ParseTable;
    import lpg.runtime.PrsStream;
    import lpg.runtime.RuleAction;
    import lpg.runtime.UndefinedEofSymbolException;
    import lpg.runtime.UnimplementedTerminalsException;
    ./
%End

%Terminals

    IntegerLiteral        -- the usual
    LongLiteral           -- IntegerLiteral followed by 'l' or 'L'
    FloatingPointLiteral  --
                          -- FloatingPointLiteral ::= Digits . Digits? ExponentPart? FloatingTypeSuffix?
                          --                        | . Digits ExponentPart? FloatingTypeSuffix?
                          --                        | Digits ExponentPart FloatingTypeSuffix?
                          --                        | Digits ExponentPart? FloatingTypeSuffix
                          --
                          -- ExponentPart ::= ('e'|'E') ('+'|'-')? Digits
                          -- FloatingTypeSuffix ::= 'f' |  'F'
                          --
    DoubleLiteral         -- See FloatingPointLiteral except that
                          -- FloatingTypeSuffix ::= 'd' | 'D'
                          --
    CharacterLiteral      -- the usual
    StringLiteral         -- the usual

    MINUS_MINUS ::= '--'
    OR ::= '|' 
    MINUS ::= -
    MINUS_EQUAL ::= -=
    NOT ::= !
    NOT_EQUAL ::= !=
    REMAINDER ::= '%'
    REMAINDER_EQUAL ::= '%='
    AND ::= &
    AND_AND ::= && 
    AND_EQUAL ::= &= 
    LPAREN ::= (
    RPAREN ::= )
    MULTIPLY ::= *
    MULTIPLY_EQUAL ::= *=
    COMMA ::= ,
    DOT ::= .
    DIVIDE ::= / 
    DIVIDE_EQUAL ::= /= 
    COLON ::= :
    SEMICOLON ::= ;
    QUESTION ::= ?
    AT ::= @  
    LBRACKET ::= '['
    RBRACKET ::= ']'
    XOR ::= ^ 
    XOR_EQUAL ::= ^=
    LBRACE ::= {
    OR_OR ::= || 
    OR_EQUAL ::= |=  
    RBRACE ::= }  
    TWIDDLE ::= ~  
    PLUS ::= + 
    PLUS_PLUS ::= ++
    PLUS_EQUAL ::= +=
    LESS ::= <  
    LEFT_SHIFT ::= << 
    LEFT_SHIFT_EQUAL ::= <<= 
    RIGHT_SHIFT ::= >>
    RIGHT_SHIFT_EQUAL ::= >>= 
    UNSIGNED_RIGHT_SHIFT ::= >>> 
    UNSIGNED_RIGHT_SHIFT_EQUAL ::= >>>= 
    LESS_EQUAL ::= <=
    EQUAL ::= =  
    EQUAL_EQUAL ::= ==  
    GREATER ::= >
    GREATER_EQUAL ::= >=
    ELLIPSIS ::= ...

    RANGE ::= '..'
    ARROW ::= '->'
    DARROW ::= '=>'
    SUBTYPE ::= '<:'
    SUPERTYPE ::= ':>'
%End

%Define
    --
    -- Definition of macros used in the parser template
    --
    $ast_class /.polyglot.ast.Node./
    $additional_interfaces /., Parser, ParseErrorCodes./
%End

%Identifier
    IDENTIFIER
%End

%Start
    CompilationUnit
%End

%SoftKeywords
--    abstract
--    as
--    assert
--    async
    at
--    ateach
    atomic
--    break
--    case
--    catch
--    class
      clocked
--    const
--    continue
--    def
--    default
--    do
--    else
--    extends
--    extern
--    false
--    final
--    finally
--    finish
--    for
--    foreach
--    future
--    global
--    goto
    here
--    if
--    implements
--    import
--    in
--    incomplete
--    instanceof
--    interface
--    local
--    native
--    new
    next
--    nonblocking
--    null
    offer
--    offers
--    operator
--    or
--    package
--    private
--    property
--    protected
--    proto
--    public
      resume
--    return
--    safe
--    self
--    sequential
--    shared
--    static
--    struct
--    super
--    switch
--    this
--    throw
--    throws
--    true
--    try
    type
    val -- used in Boxed... classes!
--    var
--    when
--    while
%End

%Headers
    /.
        //#line $next_line "$input_file$"
        private ErrorQueue eq;
        private TypeSystem ts;
        private NodeFactory nf;
        private FileSource source;
        private boolean unrecoverableSyntaxError = false;

        public void initialize(TypeSystem t, NodeFactory n, FileSource source, ErrorQueue q)
        {
            this.ts = (TypeSystem) t;
            this.nf = (NodeFactory) n;
            this.source = source;
            this.eq = q;
        }
        
        public $action_type(ILexStream lexStream, TypeSystem t, NodeFactory n, FileSource source, ErrorQueue q)
        {
            this(lexStream);
            initialize((TypeSystem) t,
                       (NodeFactory) n,
                       source,
                       q);
            prsStream.setMessageHandler(new MessageHandler(q));
        }

        public static class MessageHandler implements IMessageHandler {
            ErrorQueue eq;

            public MessageHandler(ErrorQueue eq) {
                this.eq = eq;
            }

            public static String getErrorMessageFor(int errorCode, String[] errorInfo) {

                String msg = "";
                String info = "";

                for (String s : errorInfo) {
                    info += s;
                }

                switch (errorCode) {
                case LEX_ERROR_CODE:
                    msg = "Unexpected character ignored: " + info;
                    break;
                case ERROR_CODE:
                    msg = "Parse terminated at this token: " + info;
                    break;
                case BEFORE_CODE:
                    msg = "Token " + info + " expected before this input";
                    break;
                case INSERTION_CODE:
                    msg = "Token " + info + " expected after this input";
                    break;
                case INVALID_CODE:
                    msg = "Unexpected input discarded: " + info;
                    break;
                case SUBSTITUTION_CODE:
                    msg = "Token " + info + " expected instead of this input";
                    break;
                case DELETION_CODE:
                    msg = "Unexpected input ignored: " + info;
                    break;
                case MERGE_CODE:
                    msg = "Merging token(s) to recover: " + info;
                    break;
                case MISPLACED_CODE:
                    msg = "Misplaced constructs(s): " + info;
                    break;
                case SCOPE_CODE:
                    msg = "Token(s) inserted to complete scope: " + info;
                    break;
                case EOF_CODE:
                    msg = "Reached after this token: " + info;
                    break;
                case INVALID_TOKEN_CODE:
                    msg = "Invalid token: " + info;
                    break;
                case ERROR_RULE_WARNING_CODE:
                    msg = "Ignored token: " + info;
                    break;
                case NO_MESSAGE_CODE:
                    msg = "Syntax error";
                    break;
                }

                // FIXME: HACK! Prepend "Syntax error: " until we figure out how to
                // get Polyglot to do it for us.
                if (errorCode != NO_MESSAGE_CODE) {
                    msg = "Syntax error: " + msg;
                }
                return msg;
            }

            public void handleMessage(int errorCode, int[] msgLocation,
                                      int[] errorLocation, String filename,
                                      String[] errorInfo)
            {
                File file = new File(filename);
        
                int l0 = msgLocation[2];
                int c0 = msgLocation[3];
                int l1 = msgLocation[4];
                int c1 = msgLocation[5];
                int o0 = msgLocation[0];
                int o1 = msgLocation[0] + msgLocation[1];
        
                Position pos = new JPGPosition("",
                            file.getPath(), l0, c0, l1, c1+1, o0, o1);
        
                String msg = getErrorMessageFor(errorCode, errorInfo);
                eq.enqueue(ErrorInfo.SYNTAX_ERROR, msg, pos);
            }
        }
    
        public String getErrorLocation(int lefttok, int righttok)
        {
            return prsStream.getFileName() + ':' +
                   prsStream.getLine(lefttok) + ":" + prsStream.getColumn(lefttok) + ":" +
                   prsStream.getEndLine(righttok) + ":" + prsStream.getEndColumn(righttok) + ": ";
        }

        public Position getErrorPosition(int lefttok, int righttok)
        {
            return new JPGPosition(null, prsStream.getFileName(),
                   prsStream.getIToken(lefttok), prsStream.getIToken(righttok));
        }

        //
        // Temporary classes used to wrap modifiers.
        //
        private static class Modifier {
        }

        private static class FlagModifier extends Modifier {
            public static int ABSTRACT    = 0;
            public static int ATOMIC      = 1;
           // public static int EXTERN      = 2;
            public static int FINAL       = 3;
            //public static int GLOBAL      = 4;
            //public static int INCOMPLETE  = 5;
            public static int NATIVE      = 6;
            //public static int NON_BLOCKING = 7;
            public static int PRIVATE     = 8;
            public static int PROPERTY    = 9;
            public static int PROTECTED   = 10;
            public static int PUBLIC      = 11;
            //public static int SAFE        = 12;
            //public static int SEQUENTIAL  = 13;
            public static int CLOCKED     = 14;
            public static int STATIC      = 15;
            public static int TRANSIENT   = 16;
            public static int NUM_FLAGS   = TRANSIENT + 1;

            private JPGPosition pos;
            private int flag;

            public JPGPosition position() { return pos; }
            public int flag() { return flag; }
            public Flags flags() {
                if (flag == ABSTRACT)     return Flags.ABSTRACT;
                if (flag == ATOMIC)       return X10Flags.ATOMIC;
              //  if (flag == EXTERN)       return X10Flags.EXTERN;
                if (flag == FINAL)        return Flags.FINAL;
               // if (flag == GLOBAL)       return X10Flags.GLOBAL;
                //if (flag == INCOMPLETE)   return X10Flags.INCOMPLETE;
                if (flag == NATIVE)       return Flags.NATIVE;
                //if (flag == NON_BLOCKING) return X10Flags.NON_BLOCKING;
                if (flag == PRIVATE)      return Flags.PRIVATE;
                if (flag == PROPERTY)     return X10Flags.PROPERTY;
                if (flag == PROTECTED)    return Flags.PROTECTED;
                if (flag == PUBLIC)       return Flags.PUBLIC;
                //if (flag == SAFE)         return X10Flags.SAFE;
                //if (flag == SEQUENTIAL)   return X10Flags.SEQUENTIAL;
                if (flag == CLOCKED)       return X10Flags.CLOCKED;
                if (flag == TRANSIENT)    return X10Flags.TRANSIENT;
                if (flag == STATIC)       return Flags.STATIC;
                assert(false);
                return null;
            }

            public String name() {
                if (flag == ABSTRACT)     return "abstract";
                if (flag == ATOMIC)       return "atomic";
                //if (flag == EXTERN)       return "extern";
                if (flag == FINAL)        return "final";
                //if (flag == GLOBAL)       return "global";
                //if (flag == INCOMPLETE)   return "incomplete";
                if (flag == NATIVE)       return "native";
                //if (flag == NON_BLOCKING) return "nonblocking";
                if (flag == PRIVATE)      return "private";
                if (flag == PROPERTY)     return "property";
                if (flag == PROTECTED)    return "protected";
                if (flag == PUBLIC)       return "public";
                //if (flag == SAFE)         return "safe";
                //if (flag == SEQUENTIAL)   return "sequential";
                if (flag == CLOCKED)       return "clocked";
                if (flag == STATIC)       return "static";
                if (flag == TRANSIENT)    return "transient";
                assert(false);
                return "?";
            }


            public static boolean classModifiers[] = new boolean[NUM_FLAGS];
            static {
                classModifiers[ABSTRACT] = true;
                classModifiers[FINAL] = true;
                classModifiers[PRIVATE] = true;
                classModifiers[PROTECTED] = true;
                classModifiers[PUBLIC] = true;
                //classModifiers[SAFE] = true;
                classModifiers[STATIC] = true;
                classModifiers[CLOCKED] = true;
                // classModifiers[GLOBAL] = true;
            }
            public boolean isClassModifier(int flag) {
                return  classModifiers[flag];
            }

            public static boolean typeDefModifiers[] = new boolean[NUM_FLAGS];
            static {
                typeDefModifiers[ABSTRACT] = true;
                typeDefModifiers[FINAL] = true;
                typeDefModifiers[PRIVATE] = true;
                typeDefModifiers[PROTECTED] = true;
                typeDefModifiers[PUBLIC] = true;
                typeDefModifiers[STATIC] = true;
            }
            public boolean isTypeDefModifier(int flag) {
                return typeDefModifiers[flag];
            }

            public static boolean fieldModifiers[] = new boolean[NUM_FLAGS];
            static {
                fieldModifiers[TRANSIENT] = true;
                // fieldModifiers[GLOBAL] = true;
                fieldModifiers[CLOCKED] = true;
                fieldModifiers[PRIVATE] = true;
                fieldModifiers[PROTECTED] = true;
                fieldModifiers[PROPERTY] = true;
                fieldModifiers[PUBLIC] = true;
                fieldModifiers[STATIC] = true;
            }
            public boolean isFieldModifier(int flag) {
                return fieldModifiers[flag];
            }

            public static boolean variableModifiers[] = new boolean[NUM_FLAGS];
            static {
                variableModifiers[CLOCKED] = true;
            }
            public boolean isVariableModifier(int flag) {
                return variableModifiers[flag];
            }

            public static boolean methodModifiers[] = new boolean[NUM_FLAGS];
            static {
                methodModifiers[ABSTRACT] = true;
                methodModifiers[ATOMIC] = true;
               // methodModifiers[EXTERN] = true;
                methodModifiers[FINAL] = true;
                // methodModifiers[GLOBAL] = true;
                //methodModifiers[INCOMPLETE] = true;
                methodModifiers[NATIVE] = true;
                //methodModifiers[NON_BLOCKING] = true;
                methodModifiers[PRIVATE] = true;
                methodModifiers[PROPERTY] = true;
                methodModifiers[PROTECTED] = true;
                methodModifiers[PUBLIC] = true;
                //methodModifiers[SAFE] = true;
                //methodModifiers[SEQUENTIAL] = true;
                methodModifiers[STATIC] = true;
                //methodModifiers[CLOCKED] = true;
            }
            public boolean isMethodModifier(int flag) {
                return methodModifiers[flag];
            }

            public static boolean constructorModifiers[] = new boolean[NUM_FLAGS];
            static {
                constructorModifiers[NATIVE] = true;
                constructorModifiers[PRIVATE] = true;
                constructorModifiers[PROTECTED] = true;
                constructorModifiers[PUBLIC] = true;
            }
            public boolean isConstructorModifier(int flag) {
                return constructorModifiers[flag];
            }

            public static boolean interfaceModifiers[] = new boolean[NUM_FLAGS];
            static {
                interfaceModifiers[ABSTRACT] = true;
                interfaceModifiers[PRIVATE] = true;
                interfaceModifiers[PROTECTED] = true;
                interfaceModifiers[PUBLIC] = true;
                interfaceModifiers[STATIC] = true;
                interfaceModifiers[CLOCKED] = true;

            }
            public boolean isInterfaceModifier(int flag) {
                return interfaceModifiers[flag];
            }

            public FlagModifier(JPGPosition pos, int flag) {
                this.pos = pos;
                this.flag = flag;
            }
        }

        private static class AnnotationModifier extends Modifier {
            private AnnotationNode annotation;

            public AnnotationNode annotation() { return annotation; }
            
            public AnnotationModifier(AnnotationNode annotation) {
                this.annotation = annotation;
            }
        }

        //    
        // TODO: Say something!
        //    
        private List<Node> checkModifiers(String kind, List<Modifier> modifiers, boolean legal_flags[]) {
            List<Node> l = new LinkedList<Node>();

            assert(modifiers.size() > 0);

            boolean flags[] = new boolean[FlagModifier.NUM_FLAGS]; // initialized to false
            for (int i = 0; i < modifiers.size(); i++) {
                Object element = modifiers.get(i);
                if (element instanceof FlagModifier) {
                    FlagModifier modifier = (FlagModifier) element;
                    l.addAll(Collections.singletonList(nf.FlagsNode(modifier.position(), modifier.flags())));

                    if (! flags[modifier.flag()]) {
                        flags[modifier.flag()] = true;
                    }
                    else {
                        syntaxError("Duplicate specification of modifier: " + modifier.name(), modifier.position());
                    }

                    if (! legal_flags[modifier.flag()]) {
                        syntaxError("\"" + modifier.name() + "\" is not a valid " + kind + " modifier", modifier.position());
                    }
                }
                else {
                    AnnotationModifier modifier = (AnnotationModifier) element;
                    l.addAll(Collections.singletonList(modifier.annotation()));
                }
            }

            return l;
        }

        private List<Node> checkClassModifiers(List<Modifier> modifiers) {
            return (modifiers.size() == 0
                     ? Collections.<Node>singletonList(nf.FlagsNode(JPGPosition.COMPILER_GENERATED, X10Flags.toX10Flags(Flags.NONE)))
                     : checkModifiers("class", modifiers, FlagModifier.classModifiers));
        }

        private List<Node> checkTypeDefModifiers(List<Modifier> modifiers) {
            return (modifiers.size() == 0
                     ? Collections.<Node>singletonList(nf.FlagsNode(JPGPosition.COMPILER_GENERATED, X10Flags.toX10Flags(Flags.NONE)))
                     : checkModifiers("typedef", modifiers, FlagModifier.typeDefModifiers));
        }

        private List<Node> checkFieldModifiers(List<Modifier> modifiers) {
            return (modifiers.size() == 0
                     ? Collections.<Node>emptyList()
                     : checkModifiers("field", modifiers, FlagModifier.fieldModifiers));
        }

        private List<Node> checkVariableModifiers(List<Modifier> modifiers) {
            return (modifiers.size() == 0
                     ? Collections.<Node>emptyList()
                     : checkModifiers("variable", modifiers, FlagModifier.variableModifiers));
        }

        private List<Node> checkMethodModifiers(List<Modifier> modifiers) {
            return (modifiers.size() == 0
                     ? Collections.<Node>emptyList()
                     : checkModifiers("method", modifiers, FlagModifier.methodModifiers));
        }

        private List<Node> checkConstructorModifiers(List<Modifier> modifiers) {
            return (modifiers.size() == 0
                     ? Collections.<Node>emptyList()
                     : checkModifiers("constructor", modifiers, FlagModifier.constructorModifiers));
        }

        private List<Node> checkInterfaceModifiers(List<Modifier> modifiers) {
            return (modifiers.size() == 0
                     ? Collections.<Node>emptyList()
                     : checkModifiers("interface", modifiers, FlagModifier.interfaceModifiers));
        }

        // RMF 11/7/2005 - N.B. This class has to be serializable, since it shows up inside Type objects,
        // which Polyglot serializes to save processing when loading class files generated from source
        // by Polyglot itself.
        public static class JPGPosition extends Position
        {
            private static final long serialVersionUID= -1593187800129872262L;
            private final transient IToken leftIToken,
                                           rightIToken;

            public JPGPosition(String path, String filename, IToken leftToken, IToken rightToken)
            {
                super(path, filename,
                      leftToken.getLine(), leftToken.getColumn(),
                      rightToken.getEndLine(), rightToken.getEndColumn(),
                      leftToken.getStartOffset(), rightToken.getEndOffset());
                this.leftIToken = null; // BRT -- was null, need to keep leftToken for later reference
                this.rightIToken = null;  // BRT -- was null, need to keep rightToken for later reference
            }

            public JPGPosition(Position start, Position end)
            {
                super(start, end);
                this.leftIToken = (start instanceof JPGPosition) ? ((JPGPosition)start).leftIToken : null;
                this.rightIToken = (end instanceof JPGPosition) ? ((JPGPosition)end).rightIToken : null;
            }

            JPGPosition(String path, String filename, int line, int column, int endLine, int endColumn, int offset, int endOffset)
            {
                super(path, filename, line, column, endLine, endColumn, offset, endOffset);
                this.leftIToken = null;
                this.rightIToken = null;
            }

            private JPGPosition() {
                super(null, "Compiler Generated");
                this.leftIToken = null;
                this.rightIToken = null;
            }
            public static final JPGPosition COMPILER_GENERATED = (JPGPosition)(new JPGPosition().markCompilerGenerated());

            public IToken getLeftIToken() { return leftIToken; }
            public IToken getRightIToken() { return rightIToken; }

            public String toText()
            {
                if (leftIToken == null) return "...";
                IPrsStream prsStream = leftIToken.getIPrsStream();
                return new String(prsStream.getInputChars(), offset(), endOffset() - offset() + 1);
            }
        }

        public void syntaxError(String msg, Position pos) {
            syntaxError(msg, pos, false);
        }

        public void syntaxError(String msg, Position pos, boolean unrecoverable) {
            unrecoverableSyntaxError = unrecoverable;
            eq.enqueue(ErrorInfo.SYNTAX_ERROR, msg, pos);
        }

        public $ast_class parse() {
            try
            {
                SourceFile sf = (SourceFile) parser();

                if (sf != null)
                {
                    if (! unrecoverableSyntaxError)
                        return sf.source(source);
                    eq.enqueue(ErrorInfo.SYNTAX_ERROR, "Unable to parse " + source.name() + ".", new JPGPosition(null, file(), 1, 1, 1, 1, 0, 0).markCompilerGenerated());
                }   
            }
            catch (RuntimeException e) {
                // Let the Compiler catch and report it.
                throw e;
            }
            catch (Exception e) {
                // Used by cup to indicate a non-recoverable error.
                eq.enqueue(ErrorInfo.SYNTAX_ERROR, e.getMessage(), new JPGPosition(null, file(), 1, 1, 1, 1, 0, 0).markCompilerGenerated());
            }

            return null;
        }

        public String file()
        {
            return prsStream.getFileName();
        }

        public JPGPosition pos()
        {
            return new JPGPosition("",
                                   prsStream.getFileName(),
                                   prsStream.getIToken(getLeftSpan()),
                                   prsStream.getIToken(getRightSpan()));
        }

        public JPGPosition pos(int i)
        {
            return new JPGPosition("",
                                   prsStream.getFileName(),
                                   prsStream.getIToken(i),
                                   prsStream.getIToken(i));
        }

        public JPGPosition pos(int i, int j)
        {
            return new JPGPosition("",
                                   prsStream.getFileName(),
                                   prsStream.getIToken(i),
                                   prsStream.getIToken(j));
        }

        /**
         * Return the source position of the declaration.
         */
        public JPGPosition pos (VarDeclarator n)
        {
          if (n == null) return null;
          return (JPGPosition) n.pos;
        }

        public JPGPosition pos(JPGPosition start, JPGPosition end) {
            return new JPGPosition(start.path(), start.file(), start.leftIToken, end.rightIToken);
        }

        private void checkTypeName(Id identifier) {
            String filename = file();
            String idname = identifier.id().toString();
            int dot = filename.lastIndexOf('.'),
                slash = filename.lastIndexOf('/', dot);
            if (slash == -1)
                slash = filename.lastIndexOf('\\', dot);
            String clean_filename = (slash >= 0 && dot >= 0 ? filename.substring(slash+1, dot) : "");
            if ((! clean_filename.equals(idname)) && clean_filename.equalsIgnoreCase(idname))
                eq.enqueue(ErrorInfo.SYNTAX_ERROR,
                           "This type name does not match the name of the containing file: " + filename.substring(slash+1),
                           identifier.position());
       }


        private polyglot.lex.Operator op(int i) {
            return new Operator(pos(i), prsStream.getName(i), prsStream.getKind(i));
        }

        private polyglot.lex.Identifier id(int i) {
            return new Identifier(pos(i), prsStream.getName(i), $sym_type.TK_IDENTIFIER);
        }
        private String comment(int i) {
            IToken[] adjuncts = prsStream.getTokenAt(i).getPrecedingAdjuncts();
            String s = null;
            for (IToken a : adjuncts) {
                String c = a.toString();
                if (c.startsWith("/**") && c.endsWith("*/")) {
                    s = c;
                }
            }
            return s;
        }

        private List<Formal> toFormals(List<Formal> l) { return l; }

        private List<Expr> toActuals(List<Formal> l) {
            List<Expr> l2 = new ArrayList<Expr>();
            for (Formal f : l) {
                l2.add(nf.Local(f.position(), f.name()));
            }
            return l2;
        }

        private List<TypeParamNode> toTypeParams(List<TypeParamNode> l) { return l; }

        private List<TypeNode> toTypeArgs(List<TypeParamNode> l) {
            List<TypeNode> l2 = new ArrayList<TypeNode>();
            for (TypeParamNode f : l) {
                l2.add(nf.AmbTypeNode(f.position(), null, f.name()));
            }
            return l2;
        }

                
        private List<AnnotationNode> extractAnnotations(List<? extends Node> l) {
            List<AnnotationNode> l2 = new LinkedList<AnnotationNode>();
            for (Node n : l) {
                if (n instanceof AnnotationNode) {
                    l2.add((AnnotationNode) n);
                }
            }
            return l2;
        }
    
        private FlagsNode extractFlags(List<? extends Node> l, Flags f) {
            FlagsNode fn = extractFlags(l);
            fn = fn.flags(fn.flags().set(f));
            return fn;
        }
        
        private FlagsNode extractFlags(List<? extends Node> l1, List<? extends Node> l2) {
            List<Node> l = new ArrayList<Node>();
            l.addAll(l1);
            l.addAll(l2);
            return extractFlags(l);
        }
        
        private FlagsNode extractFlags(List<? extends Node> l) {
            Position pos = null;
            X10Flags xf = X10Flags.toX10Flags(Flags.NONE);
            for (Node n : l) {
                if (n instanceof FlagsNode) {
                    FlagsNode fn = (FlagsNode) n;
                    pos = pos == null ? fn.position() : new JPGPosition(pos, fn.position());
                    Flags f = fn.flags();
                    if (f instanceof X10Flags) {
                        xf = xf.set((X10Flags) f);
                    }
                    else {
                        xf = X10Flags.toX10Flags(xf.set(f));
                    }
                }
            }
            return nf.FlagsNode(pos == null ? JPGPosition.COMPILER_GENERATED : pos, xf);
        }

        /* Roll our own integer parser.  We can't use Long.parseLong because
         * it doesn't handle numbers greater than 0x7fffffffffffffff correctly.
         */
        private long parseLong(String s, int radix)
        {
            long x = 0L;

            s = s.toLowerCase();

            for (int i = 0; i < s.length(); i++) {
                int c = s.charAt(i);

                if (c < '0' || c > '9') {
                    c = c - 'a' + 10;
                }
                else {
                    c = c - '0';
                }

                x *= radix;
                x += c;
            }

            return x;
        }

        private long parseLong(String s)
        {
            int radix;
            int start_index;
            int end_index;
            
            end_index = s.length();

            while (end_index > 0) {
                char lastCh = s.charAt(end_index - 1);
                if (lastCh != 'l' && lastCh != 'L' && lastCh != 'u' && lastCh != 'U') {
                        break;
                }
                end_index--;
            }

            if (s.charAt(0) == '0')
            {
               if (s.length() > 1 && (s.charAt(1) == 'x' || s.charAt(1) == 'X'))
               {
                   radix = 16;
                   start_index = 2;
               }
               else
               {
                   radix = 8;
                   start_index = 0;
               }
            }
            else
            {
                radix = 10;
                start_index = 0;
            }

            return parseLong(s.substring(start_index, end_index), radix);
        }

        private polyglot.lex.LongLiteral int_lit(int i)
        {
            long x = parseLong(prsStream.getName(i));
            return new LongLiteral(pos(i),  x, $sym_type.TK_IntegerLiteral);
        }

        private polyglot.lex.LongLiteral long_lit(int i)
        {
            long x = parseLong(prsStream.getName(i));
            return new LongLiteral(pos(i), x, $sym_type.TK_LongLiteral);
        }
        private polyglot.lex.LongLiteral ulong_lit(int i)
        {
            long x = parseLong(prsStream.getName(i));
            return new LongLiteral(pos(i), x, $sym_type.TK_UnsignedLongLiteral);
        }
        private polyglot.lex.LongLiteral uint_lit(int i)
        {
            long x = parseLong(prsStream.getName(i));
            return new LongLiteral(pos(i), x, $sym_type.TK_UnsignedIntegerLiteral);
        }

        private polyglot.lex.FloatLiteral float_lit(int i)
        {
            try {
                String s = prsStream.getName(i);
                int end_index = (s.charAt(s.length() - 1) == 'f' || s.charAt(s.length() - 1) == 'F'
                                                           ? s.length() - 1
                                                           : s.length());
                float x = Float.parseFloat(s.substring(0, end_index));
                return new FloatLiteral(pos(i), x, $sym_type.TK_FloatingPointLiteral);
            }
            catch (NumberFormatException e) {
                unrecoverableSyntaxError = true;
                eq.enqueue(ErrorInfo.LEXICAL_ERROR,
                           "Illegal float literal \"" + prsStream.getName(i) + "\"", pos(i));
                return null;
            }
        }

        private polyglot.lex.DoubleLiteral double_lit(int i)
        {
            try {
                String s = prsStream.getName(i);
                int end_index = (s.charAt(s.length() - 1) == 'd' || s.charAt(s.length() - 1) == 'D'
                                                           ? s.length() - 1
                                                           : s.length());
                double x = Double.parseDouble(s.substring(0, end_index));
                return new DoubleLiteral(pos(i), x, $sym_type.TK_DoubleLiteral);
            }
            catch (NumberFormatException e) {
                unrecoverableSyntaxError = true;
                eq.enqueue(ErrorInfo.LEXICAL_ERROR,
                           "Illegal float literal \"" + prsStream.getName(i) + "\"", pos(i));
                return null;
            }
        }

        private polyglot.lex.CharacterLiteral char_lit(int i)
        {
            char x;
            String s = prsStream.getName(i);
            if (s.charAt(1) == '\\') {
                switch(s.charAt(2)) {
                    case 'u':
                        x = (char) parseLong(s.substring(3, s.length() - 1), 16);
                        break;
                    case 'b':
                        x = '\b';
                        break;
                    case 't':
                        x = '\t';
                        break;
                    case 'n':
                        x = '\n';
                        break;
                    case 'f':
                        x = '\f';
                        break;
                    case 'r':
                        x = '\r';
                        break;
                    case '\"':
                        x = '\"';
                        break;
                    case '\'':
                        x = '\'';
                        break;
                    case '\\':
                        x = '\\';
                        break;
                    default:
                        x = (char) parseLong(s.substring(2, s.length() - 1), 8);
                        if (x > 255) {
                            unrecoverableSyntaxError = true;
                            eq.enqueue(ErrorInfo.LEXICAL_ERROR,
                                       "Illegal character literal " + s, pos(i));
                        }
                }
            }
            else {
                assert(s.length() == 3);
                x = s.charAt(1);
            }

            return new CharacterLiteral(pos(i), x, $sym_type.TK_CharacterLiteral);
        }

        private polyglot.lex.BooleanLiteral boolean_lit(int i)
        {
            return new BooleanLiteral(pos(i), prsStream.getKind(i) == $sym_type.TK_true, prsStream.getKind(i));
        }

        private polyglot.lex.StringLiteral string_lit(int i)
        {
            String s = prsStream.getName(i);
            char x[] = new char[s.length()];
            int j = 1,
                k = 0;
            while(j < s.length() - 1) {
                if (s.charAt(j) != '\\')
                    x[k++] = s.charAt(j++);
                else {
                    switch(s.charAt(j + 1)) {
                        case 'u':
                            x[k++] = (char) parseLong(s.substring(j + 2, j + 6), 16);
                            j += 6;
                            break;
                        case 'b':
                            x[k++] = '\b';
                            j += 2;
                            break;
                        case 't':
                            x[k++] = '\t';
                            j += 2;
                            break;
                        case 'n':
                            x[k++] = '\n';
                            j += 2;
                            break;
                        case 'f':
                            x[k++] = '\f';
                            j += 2;
                            break;
                        case 'r':
                            x[k++] = '\r';
                            j += 2;
                            break;
                        case '\"':
                            x[k++] = '\"';
                            j += 2;
                            break;
                        case '\'':
                            x[k++] = '\'';
                            j += 2;
                            break;
                        case '\\':
                            x[k++] = '\\';
                            j += 2;
                            break;
                        default:
                        {
                            int n = j + 1;
                            for (int l = 0; l < 3 && Character.isDigit(s.charAt(n)); l++)
                                n++;
                            char c = (char) parseLong(s.substring(j + 1, n), 8);
                            if (c > 255) {
                                unrecoverableSyntaxError = true;
                                eq.enqueue(ErrorInfo.LEXICAL_ERROR,
                                           "Illegal character (" + s.substring(j, n) + ") in string literal " + s, pos(i));
                            }
                            x[k++] = c;
                            j = n;
                        }
                    }
                }
            }

            return new StringLiteral(pos(i), new String(x, 0, k), $sym_type.TK_StringLiteral);
        }

        private polyglot.lex.NullLiteral null_lit(int i)
        {
            return new NullLiteral(pos(i), $sym_type.TK_null);
        }

    ./
%End

%Rules
    Modifiersopt ::= %Empty
        /.$BeginJava
                    setResult(new LinkedList<Modifier>());
          $EndJava
        ./
                   | Modifiersopt Modifier
        /.$BeginJava
                    Modifiersopt.add(Modifier);
          $EndJava
        ./

    Modifier ::= abstract
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.ABSTRACT));
          $EndJava
        ./
                   | Annotation
        /.$BeginJava
                    setResult(new AnnotationModifier(Annotation));
          $EndJava
        ./
                   | atomic
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.ATOMIC));
          $EndJava
        ./
--                   | extern
--        /.$BeginJava
--                    setResult(new FlagModifier(pos(), FlagModifier.EXTERN));
--          $EndJava
--        ./
                   | final
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.FINAL));
          $EndJava
        ./
--                   | global
--        /.$BeginJava
--                    setResult(new FlagModifier(pos(), FlagModifier.GLOBAL));
--          $EndJava
--        ./
                   | native
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.NATIVE));
          $EndJava
        ./
                   | private
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.PRIVATE));
          $EndJava
        ./
                   | protected
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.PROTECTED));
          $EndJava
        ./
                   | public
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.PUBLIC));
          $EndJava
        ./
                   | static
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.STATIC));
          $EndJava
        ./
                    | transient
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.TRANSIENT));
          $EndJava
        ./
                     | clocked
        /.$BeginJava
                    setResult(new FlagModifier(pos(), FlagModifier.CLOCKED));
          $EndJava
        ./

    MethodModifiersopt ::= Modifiersopt
                         | MethodModifiersopt property$property
        /.$BeginJava
                    MethodModifiersopt.add(new FlagModifier(pos(getRhsFirstTokenIndex($property)), FlagModifier.PROPERTY));
          $EndJava
        ./
                         | MethodModifiersopt Modifier
        /.$BeginJava
                    MethodModifiersopt.add(Modifier);
          $EndJava
        ./

    TypeDefDeclaration ::= Modifiersopt type Identifier TypeParametersopt FormalParametersopt WhereClauseopt = Type ;
        /.$BeginJava
                    List<Node> modifiers = checkTypeDefModifiers(Modifiersopt);
                    FlagsNode f = extractFlags(modifiers);
                    List<AnnotationNode> annotations = extractAnnotations(modifiers);
                    List<Formal> formals = new ArrayList<Formal>();
                    for (Formal v : FormalParametersopt) {
                        FlagsNode flags = v.flags();
                        if (!flags.flags().isFinal()) {
                            syntaxError("Type definition parameters must be final.", v.position());
                            v = v.flags(flags.flags(flags.flags().Final()));
                        }
                        formals.add(v);
                    }
                    TypeDecl cd = nf.TypeDecl(pos(), f, Identifier, TypeParametersopt, formals, WhereClauseopt, Type);
                    cd = (TypeDecl) ((X10Ext) cd.ext()).annotations(annotations);
                    setResult(cd);
          $EndJava
        ./
        
    Properties ::= ( PropertyList )
      /.$BeginJava
                    setResult(PropertyList);
     $EndJava ./

       PropertyList ::= Property
        /.$BeginJava
                    List<PropertyDecl> l = new TypedList<PropertyDecl>(new LinkedList<PropertyDecl>(), PropertyDecl.class, false);
                    l.add(Property);
                    setResult(l);
          $EndJava
        ./
                          | PropertyList , Property
        /.$BeginJava
                    PropertyList.add(Property);
          $EndJava
        ./
    
    
    Property ::=  Annotationsopt Identifier ResultType
        /.$BeginJava
                    List<AnnotationNode> annotations = extractAnnotations(Annotationsopt);
                    PropertyDecl cd = nf.PropertyDecl(pos(), nf.FlagsNode(pos(), Flags.PUBLIC.Final()), ResultType, Identifier);
                    cd = (PropertyDecl) ((X10Ext) cd.ext()).annotations(annotations);
                    setResult(cd);
          $EndJava
        ./

    MethodDeclaration ::= MethodModifiersopt def Identifier TypeParametersopt FormalParameters WhereClauseopt HasResultTypeopt  Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    ProcedureDecl pd;
                    if (Identifier.id().toString().equals("this")) {
                        pd = nf.X10ConstructorDecl(pos(),
                                                   extractFlags(modifiers),
                                                   Identifier,
                                                   HasResultTypeopt,
                                                   TypeParametersopt,
                                                   FormalParameters,
                                                   WhereClauseopt,
                                                
                                                   Offersopt,
                                                   MethodBody);

                    }
                    else {
                        pd = nf.X10MethodDecl(pos(),
                                              extractFlags(modifiers),
                                              HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                              Identifier,
                                              TypeParametersopt,
                                              FormalParameters,
                                              WhereClauseopt,
                                            
                                              Offersopt,
                                              MethodBody);
                    }
                    pd = (ProcedureDecl) ((X10Ext) pd.ext()).annotations(extractAnnotations(modifiers));
                    setResult(pd);
          $EndJava
        ./
      | MethodModifiersopt operator TypeParametersopt ( FormalParameter$fp1 ) BinOp ( FormalParameter$fp2 ) WhereClauseopt HasResultTypeopt  Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    Name opName = X10Binary_c.binaryMethodName(BinOp);
                    if (opName == null) {
                        syntaxError("Cannot override binary operator '"+BinOp+"'.", pos());
                        opName = Name.make("invalid operator");
                    }
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     nf.Id(pos(getRhsFirstTokenIndex($BinOp)), opName),
                                                     TypeParametersopt,
                                                     Arrays.<Formal>asList(fp1, fp2),
                                                     WhereClauseopt,
                                                    
                                                     Offersopt,
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (! flags.flags().isStatic()) {
                        syntaxError("Binary operator with two parameters must be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().Static()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
      | MethodModifiersopt operator TypeParametersopt PrefixOp ( FormalParameter$fp2 ) WhereClauseopt HasResultTypeopt  Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    Name opName = X10Unary_c.unaryMethodName(PrefixOp);
                    if (opName == null) {
                        syntaxError("Cannot override unary operator '"+PrefixOp+"'.", pos());
                        opName = Name.make("invalid operator");
                    }
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     nf.Id(pos(getRhsFirstTokenIndex($PrefixOp)), opName),
                                                     TypeParametersopt,
                                                     Collections.<Formal>singletonList(fp2),
                                                     WhereClauseopt,
                                                    
                                                     Offersopt,
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (! flags.flags().isStatic()) {
                        syntaxError("Unary operator with one parameter must be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().Static()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
      | MethodModifiersopt operator TypeParametersopt this BinOp ( FormalParameter$fp2 ) WhereClauseopt HasResultTypeopt  Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    Name opName = X10Binary_c.binaryMethodName(BinOp);
                    if (opName == null) {
                        syntaxError("Cannot override binary operator '"+BinOp+"'.", pos());
                        opName = Name.make("invalid operator");
                    }
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     nf.Id(pos(getRhsFirstTokenIndex($BinOp)), opName),
                                                     TypeParametersopt,
                                                     Collections.<Formal>singletonList(fp2),
                                                     WhereClauseopt,
                                                   
                                                     Offersopt,
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (flags.flags().isStatic()) {
                        syntaxError("Binary operator with this parameter cannot be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().clearStatic()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
      | MethodModifiersopt operator TypeParametersopt ( FormalParameter$fp1 ) BinOp this WhereClauseopt HasResultTypeopt   Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    Name opName = X10Binary_c.invBinaryMethodName(BinOp);
                    if (opName == null) {
                        syntaxError("Cannot override binary operator '"+BinOp+"'.", pos());
                        opName = Name.make("invalid operator");
                    }
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     nf.Id(pos(getRhsFirstTokenIndex($BinOp)), opName),
                                                     TypeParametersopt,
                                                     Collections.<Formal>singletonList(fp1),
                                                     WhereClauseopt,
                                                 
                                                     Offersopt,
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (flags.flags().isStatic()) {
                        syntaxError("Binary operator with this parameter cannot be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().clearStatic()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
      | MethodModifiersopt operator TypeParametersopt PrefixOp this WhereClauseopt HasResultTypeopt  Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    Name opName = X10Unary_c.unaryMethodName(PrefixOp);
                    if (opName == null) {
                        syntaxError("Cannot override unary operator '"+PrefixOp+"'.", pos());
                        opName = Name.make("invalid operator");
                    }
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     nf.Id(pos(getRhsFirstTokenIndex($PrefixOp)), opName),
                                                     TypeParametersopt,
                                                     Collections.<Formal>emptyList(),
                                                     WhereClauseopt,
                                                
                                                     Offersopt,
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (flags.flags().isStatic()) {
                        syntaxError("Unary operator with this parameter cannot be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().clearStatic()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
      | MethodModifiersopt operator this TypeParametersopt FormalParameters WhereClauseopt HasResultTypeopt   Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     nf.Id(pos(), ClosureCall.APPLY),
                                                     TypeParametersopt,
                                                     FormalParameters,
                                                     WhereClauseopt,
                                                  
                                                     Offersopt,
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (flags.flags().isStatic()) {
                        syntaxError("Apply operator cannot be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().clearStatic()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
      | MethodModifiersopt operator this TypeParametersopt FormalParameters = ( FormalParameter$fp2 ) WhereClauseopt HasResultTypeopt  Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     nf.Id(pos(), SettableAssign.SET),
                                                     TypeParametersopt,
                                                     CollectionUtil.append(Collections.singletonList(fp2), FormalParameters),
                                                     WhereClauseopt,
                                                     
                                                     Offersopt,
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (flags.flags().isStatic()) {
                        syntaxError("Set operator cannot be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().clearStatic()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
      | MethodModifiersopt operator TypeParametersopt ( FormalParameter$fp1 ) as Type WhereClauseopt  Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     Type,
                                                     nf.Id(pos(), Converter.operator_as),
                                                     TypeParametersopt,
                                                     Collections.<Formal>singletonList(fp1),
                                                     WhereClauseopt,
                                                     
                                                     Offersopt, 
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (! flags.flags().isStatic()) {
                        syntaxError("Conversion operator must be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().Static()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
      | MethodModifiersopt operator TypeParametersopt ( FormalParameter$fp1 ) as ? WhereClauseopt HasResultTypeopt   Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     nf.Id(pos(), Converter.operator_as),
                                                     TypeParametersopt,
                                                     Collections.<Formal>singletonList(fp1),
                                                     WhereClauseopt,
                                                     
                                                     Offersopt, 
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (! flags.flags().isStatic()) {
                        syntaxError("Conversion operator must be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().Static()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
      | MethodModifiersopt operator TypeParametersopt ( FormalParameter$fp1 ) WhereClauseopt HasResultTypeopt  Offersopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     nf.Id(pos(), Converter.implicit_operator_as),
                                                     TypeParametersopt,
                                                     Collections.<Formal>singletonList(fp1),
                                                     WhereClauseopt,
                                                     
                                                     Offersopt,
                                                     MethodBody);
                    FlagsNode flags = md.flags();
                    if (! flags.flags().isStatic()) {
                        syntaxError("Conversion operator must be static.", md.position());
                        md = md.flags(flags.flags(flags.flags().Static()));
                    }
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./

    PropertyMethodDeclaration ::= MethodModifiersopt Identifier TypeParametersopt FormalParameters WhereClauseopt HasResultTypeopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers, X10Flags.PROPERTY),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     Identifier,
                                                     TypeParametersopt,
                                                     FormalParameters,
                                                     WhereClauseopt,
                                                  
                                                     null, // offersOpt
                                                     MethodBody);
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./
                                | MethodModifiersopt Identifier WhereClauseopt HasResultTypeopt MethodBody
        /.$BeginJava
                    List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
                    MethodDecl md = nf.X10MethodDecl(pos(),
                                                     extractFlags(modifiers, X10Flags.PROPERTY),
                                                     HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,
                                                     Identifier,
                                                     Collections.<TypeParamNode>emptyList(),
                                                     Collections.<Formal>emptyList(),
                                                     WhereClauseopt,
                                                 
                                                     null, // offersOpt
                                                     MethodBody);
                    md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
                    setResult(md);
          $EndJava
        ./

    ExplicitConstructorInvocation ::= this TypeArgumentsopt ( ArgumentListopt ) ;
        /.$BeginJava
                    setResult(nf.X10ThisCall(pos(), TypeArgumentsopt, ArgumentListopt));
          $EndJava
        ./
                                    | super TypeArgumentsopt ( ArgumentListopt ) ;
        /.$BeginJava
                    setResult(nf.X10SuperCall(pos(), TypeArgumentsopt, ArgumentListopt));
          $EndJava
        ./
                                    | Primary . this TypeArgumentsopt ( ArgumentListopt ) ;
        /.$BeginJava
                    setResult(nf.X10ThisCall(pos(), Primary, TypeArgumentsopt, ArgumentListopt));
          $EndJava
        ./
                                    | Primary . super TypeArgumentsopt ( ArgumentListopt ) ;
        /.$BeginJava
                    setResult(nf.X10SuperCall(pos(), Primary, TypeArgumentsopt, ArgumentListopt));
          $EndJava
        ./

    NormalInterfaceDeclaration ::= Modifiersopt interface Identifier TypeParamsWithVarianceopt Propertiesopt WhereClauseopt ExtendsInterfacesopt InterfaceBody
        /.$BeginJava
                    List<Node> modifiers = checkInterfaceModifiers(Modifiersopt);
                    checkTypeName(Identifier);
                    List<TypeParamNode> TypeParametersopt = TypeParamsWithVarianceopt;
                    List<PropertyDecl> props = Propertiesopt;
                    DepParameterExpr ci = WhereClauseopt;
                    FlagsNode fn = extractFlags(modifiers, Flags.INTERFACE);
                    ClassDecl cd = nf.X10ClassDecl(pos(),
                                                   fn,
                                                   Identifier,
                                                   TypeParametersopt,
                                                   props,
                                                   ci,
                                                   null,
                                                   ExtendsInterfacesopt,
                                                   InterfaceBody);
                    cd = (ClassDecl) ((X10Ext) cd.ext()).annotations(extractAnnotations(modifiers));
                    setResult(cd);
          $EndJava
        ./

    ClassInstanceCreationExpression ::= new TypeName TypeArgumentsopt ( ArgumentListopt ) ClassBodyopt
        /.$BeginJava
                    if (ClassBodyopt == null)
                         setResult(nf.X10New(pos(), TypeName.toType(), TypeArgumentsopt, ArgumentListopt));
                    else setResult(nf.X10New(pos(), TypeName.toType(), TypeArgumentsopt, ArgumentListopt, ClassBodyopt)) ;
          $EndJava
        ./
                 | new TypeName '[' Type ']' '[' ArgumentListopt ']'
        /.$BeginJava
                    String arrayTypeName = TypeName.name.id().toString();
                    if (! (arrayTypeName.equals("x10.array.Array") || arrayTypeName.equals("Array")))
                        syntaxError(new Errors.ArrayLiteralMustBeOfArrayType(arrayTypeName, TypeName.pos).getMessage(),TypeName.pos);
                    setResult(nf.Tuple(pos(), Type, ArgumentListopt));
          $EndJava
        ./
                                      | Primary . new Identifier TypeArgumentsopt ( ArgumentListopt ) ClassBodyopt
        /.$BeginJava
                    ParsedName b = new X10ParsedName(nf, ts, pos(), Identifier);
                    if (ClassBodyopt == null)
                         setResult(nf.X10New(pos(), Primary, b.toType(), TypeArgumentsopt, ArgumentListopt));
                    else setResult(nf.X10New(pos(), Primary, b.toType(), TypeArgumentsopt, ArgumentListopt, ClassBodyopt));
          $EndJava
        ./
                                      | AmbiguousName . new Identifier TypeArgumentsopt ( ArgumentListopt ) ClassBodyopt
        /.$BeginJava
                    ParsedName b = new X10ParsedName(nf, ts, pos(), Identifier);
                    if (ClassBodyopt == null)
                         setResult(nf.X10New(pos(), AmbiguousName.toExpr(), b.toType(), TypeArgumentsopt, ArgumentListopt));
                    else setResult(nf.X10New(pos(), AmbiguousName.toExpr(), b.toType(), TypeArgumentsopt, ArgumentListopt, ClassBodyopt));
          $EndJava
        ./
                       
      AssignPropertyCall ::= property TypeArgumentsopt ( ArgumentListopt ) ;
       /.$BeginJava
                    setResult(nf.AssignPropertyCall(pos(), TypeArgumentsopt, ArgumentListopt));
          $EndJava
        ./

    -------------------------------------- Section:::Types
    Type ::= FunctionType
           |  ConstrainedType

    FunctionType ::= TypeParametersopt ( FormalParameterListopt ) WhereClauseopt  Offersopt => Type
        /.$BeginJava
                    setResult(nf.FunctionTypeNode(pos(), TypeParametersopt, FormalParameterListopt, WhereClauseopt, Type,  Offersopt));
          $EndJava
        ./

    ClassType ::= NamedType

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    InterfaceType ::= FunctionType | NamedType | ( Type )
    
    AnnotatedType ::= Type Annotations
        /.$BeginJava
                    TypeNode tn = Type;
                    tn = (TypeNode) ((X10Ext) tn.ext()).annotations((List<AnnotationNode>) Annotations);
                    setResult(tn.position(pos()));
          $EndJava
        ./

    ConstrainedType ::=  NamedType
           | AnnotatedType
           | ( Type )
        /.$BeginJava
                    setResult(Type);
          $EndJava
        ./

    PlaceType ::=  PlaceExpression
--        /.$BeginJava
--                    setResult(nf.Binary(pos(),
--                                        nf.Field(pos(), nf.This(pos()), nf.Id(pos(), "home")), Binary.EQ,
--                                        PlaceExpression));
--          $EndJava
--        ./

    SimpleNamedType ::= TypeName
        /.$BeginJava
                setResult(TypeName.toType());
          $EndJava
        ./
                      | Primary . Identifier
        /.$BeginJava
                setResult(nf.AmbTypeNode(pos(), Primary, Identifier));
          $EndJava
        ./
                      | DepNamedType . Identifier
        /.$BeginJava
                setResult(nf.AmbTypeNode(pos(), DepNamedType, Identifier));
          $EndJava
        ./

    DepNamedType ::= SimpleNamedType DepParameters
        /.$BeginJava
                TypeNode type = nf.AmbDepTypeNode(pos(), ((AmbTypeNode) SimpleNamedType).prefix(), ((AmbTypeNode) SimpleNamedType).name(),
                                                  new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false),
                                                  new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false),
                                                  DepParameters);
                setResult(type);
          $EndJava
        ./
                | SimpleNamedType Arguments
        /.$BeginJava
                TypeNode type = nf.AmbDepTypeNode(pos(), ((AmbTypeNode) SimpleNamedType).prefix(), ((AmbTypeNode) SimpleNamedType).name(),
                                                  new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false),
                                                  Arguments,
                                                  null);
                setResult(type);
          $EndJava
        ./
                | SimpleNamedType Arguments DepParameters
        /.$BeginJava
                TypeNode type = nf.AmbDepTypeNode(pos(), ((AmbTypeNode) SimpleNamedType).prefix(), ((AmbTypeNode) SimpleNamedType).name(),
                                                  new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false),
                                                  Arguments,
                                                  DepParameters);
                setResult(type);
          $EndJava
        ./
                | SimpleNamedType TypeArguments
        /.$BeginJava
                TypeNode type = nf.AmbDepTypeNode(pos(), ((AmbTypeNode) SimpleNamedType).prefix(), ((AmbTypeNode) SimpleNamedType).name(),
                                                  TypeArguments,
                                                  new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false),
                                                  null);
                setResult(type);
          $EndJava
        ./
                | SimpleNamedType TypeArguments DepParameters
        /.$BeginJava
                TypeNode type = nf.AmbDepTypeNode(pos(), ((AmbTypeNode) SimpleNamedType).prefix(), ((AmbTypeNode) SimpleNamedType).name(),
                                                  TypeArguments,
                                                  new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false),
                                                  DepParameters);
                setResult(type);
          $EndJava
        ./
                | SimpleNamedType TypeArguments Arguments
        /.$BeginJava
                TypeNode type = nf.AmbDepTypeNode(pos(), ((AmbTypeNode) SimpleNamedType).prefix(), ((AmbTypeNode) SimpleNamedType).name(),
                                                  TypeArguments,
                                                  Arguments,
                                                  null);
                setResult(type);
          $EndJava
        ./
                | SimpleNamedType TypeArguments Arguments DepParameters
        /.$BeginJava
                TypeNode type = nf.AmbDepTypeNode(pos(), ((AmbTypeNode) SimpleNamedType).prefix(), ((AmbTypeNode) SimpleNamedType).name(),
                                                  TypeArguments,
                                                  Arguments,
                                                  DepParameters);
                setResult(type);
          $EndJava
        ./
        
    NamedType ::= SimpleNamedType
                | DepNamedType
        
    DepParameters ::= { ExistentialListopt Conjunctionopt }
         /.$BeginJava
                    setResult(nf.DepParameterExpr(pos(), ExistentialListopt, Conjunctionopt));
          $EndJava
        ./


    TypeParamsWithVariance ::= '[' TypeParamWithVarianceList ']'
        /.$BeginJava
                    setResult(TypeParamWithVarianceList);
          $EndJava
        ./
        
    TypeParameters ::= '[' TypeParameterList ']'
        /.$BeginJava
                    setResult(TypeParameterList);
          $EndJava
        ./

    FormalParameters ::= ( FormalParameterListopt )
        /.$BeginJava
                    setResult(FormalParameterListopt);
          $EndJava
        ./

    Conjunction ::= Expression
        /.$BeginJava
                    List<Expr> l = new ArrayList<Expr>();
                    l.add(Expression);
                    setResult(l);
          $EndJava
        ./
                  | Conjunction , Expression
         /.$BeginJava
                    Conjunction.add(Expression);
          $EndJava
        ./

    HasZeroConstraint ::= Type$t1 hasZero
         /.$BeginJava
                    setResult(nf.HasZeroTest(pos(), t1));
          $EndJava
        ./

    SubtypeConstraint ::= Type$t1 <: Type$t2
         /.$BeginJava
                    setResult(nf.SubtypeTest(pos(), t1, t2, false));
          $EndJava
        ./
                        | Type$t1 :> Type$t2
         /.$BeginJava
                    setResult(nf.SubtypeTest(pos(), t2, t1, false));
          $EndJava
        ./
                        
    WhereClause ::= DepParameters
            /.$BeginJava
                setResult(DepParameters);
          $EndJava
          ./

    Conjunctionopt ::= %Empty
          /.$BeginJava
                    List<Expr> l = new ArrayList<Expr>();
                    setResult(l);
          $EndJava
          ./
          | Conjunction
          /.$BeginJava
                setResult(Conjunction);
          $EndJava
        ./

    ExistentialListopt ::= %Empty
          /.$BeginJava
                setResult(new ArrayList<Formal>());
          $EndJava
          ./
          | ExistentialList ;
          /.$BeginJava
                setResult(ExistentialList);
          $EndJava
        ./

       ExistentialList ::= FormalParameter
        /.$BeginJava
                    List<Formal> l = new TypedList<Formal>(new LinkedList<Formal>(), Formal.class, false);
                    l.add(FormalParameter.flags(nf.FlagsNode(Position.compilerGenerated(FormalParameter.position()), Flags.FINAL)));
                    setResult(l);
          $EndJava
        ./
                          | ExistentialList ; FormalParameter
        /.$BeginJava
                    ExistentialList.add(FormalParameter.flags(nf.FlagsNode(Position.compilerGenerated(FormalParameter.position()), Flags.FINAL)));
          $EndJava
        ./


    ------------------------------------- Section ::: Classes
    ClassDeclaration ::= StructDeclaration
                       | NormalClassDeclaration
        
    NormalClassDeclaration ::= Modifiersopt class Identifier TypeParamsWithVarianceopt Propertiesopt WhereClauseopt Superopt Interfacesopt ClassBody
        /.$BeginJava
                    List<Node> modifiers = checkClassModifiers(Modifiersopt);
                    checkTypeName(Identifier);
                    List<TypeParamNode> TypeParametersopt = TypeParamsWithVarianceopt;
                    List<PropertyDecl> props = Propertiesopt;
                    DepParameterExpr ci = WhereClauseopt;
                    FlagsNode f = extractFlags(modifiers);
                    List<AnnotationNode> annotations = extractAnnotations(modifiers);
                    ClassDecl cd = nf.X10ClassDecl(pos(),
                                                   f, Identifier, TypeParametersopt, props, ci,
                                                   Superopt, Interfacesopt, ClassBody);
                    cd = (ClassDecl) ((X10Ext) cd.ext()).annotations(annotations);
                    setResult(cd);
          $EndJava
        ./


    StructDeclaration ::= Modifiersopt struct Identifier TypeParamsWithVarianceopt Propertiesopt WhereClauseopt Interfacesopt ClassBody
        /.$BeginJava
                    List<Node> modifiers = checkClassModifiers(Modifiersopt);
                    checkTypeName(Identifier);
                    List<TypeParamNode> TypeParametersopt = TypeParamsWithVarianceopt;
                    List<PropertyDecl> props = Propertiesopt;
                    DepParameterExpr ci = WhereClauseopt;
                    ClassDecl cd = nf.X10ClassDecl(pos(getLeftSpan(), getRightSpan()),
                                                   extractFlags(modifiers, X10Flags.STRUCT), Identifier,
                                                   TypeParametersopt, props, ci, null, Interfacesopt, ClassBody);
                    cd = (ClassDecl) ((X10Ext) cd.ext()).annotations(extractAnnotations(modifiers));
                    setResult(cd);
          $EndJava
        ./

    ConstructorDeclaration ::= Modifiersopt def this TypeParametersopt FormalParameters WhereClauseopt HasResultTypeopt  Offersopt ConstructorBody
        /.$BeginJava
                    List<Node> modifiers = checkConstructorModifiers(Modifiersopt);
                    ConstructorDecl cd = nf.X10ConstructorDecl(pos(),
                                                               extractFlags(modifiers),
                                                               nf.Id(pos(getRhsFirstTokenIndex(3)), "this"),
                                                               HasResultTypeopt,
                                                               TypeParametersopt,
                                                               FormalParameters,
                                                               WhereClauseopt,
                                                               
                                                               Offersopt,
                                                               ConstructorBody);
                    cd = (ConstructorDecl) ((X10Ext) cd.ext()).annotations(extractAnnotations(modifiers));
                    setResult(cd);
         $EndJava
        ./
       
     Super ::= extends ClassType
        /.$BeginJava
                    setResult(ClassType);
          $EndJava
        ./
    
    FieldKeyword ::= val
        /.$BeginJava
                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.FINAL)));
          $EndJava
        ./
                   | var
        /.$BeginJava
                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.NONE)));
          $EndJava
        ./
                   
                   
                   
    VarKeyword ::= val 
        /.$BeginJava
                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.FINAL)));
          $EndJava
        ./
                   | var 
        /.$BeginJava
                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.NONE)));
          $EndJava
        ./
                    
                   
    FieldDeclaration ::= Modifiersopt FieldKeyword FieldDeclarators ;
        /.$BeginJava
                    List<Node> modifiers = checkFieldModifiers(Modifiersopt);
                    FlagsNode fn = extractFlags(modifiers, FieldKeyword);
        
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                        for (Object[] o : FieldDeclarators)
                        {
                            Position pos = (Position) o[0];
                            Id name = (Id) o[1];
                            if (name == null) name = nf.Id(pos, Name.makeFresh());
                            List<Id> exploded = (List<Id>) o[2];
                            TypeNode type = (TypeNode) o[3];
                            if (type == null) type = nf.UnknownTypeNode(name.position());
                            Expr init = (Expr) o[4];
                            FieldDecl fd = nf.FieldDecl(pos, fn,
                                               type, name, init);
                            fd = (FieldDecl) ((X10Ext) fd.ext()).annotations(extractAnnotations(modifiers));
                            fd = (FieldDecl) ((X10Ext) fd.ext()).setComment(comment(getRhsFirstTokenIndex(1)));
                            l.add(fd);
                        }
                    setResult(l);
          $EndJava
        ./
        
                       | Modifiersopt FieldDeclarators ;
        /.$BeginJava
                    List<Node> modifiers = checkFieldModifiers(Modifiersopt);
                    List<FlagsNode> FieldKeyword = Collections.singletonList(nf.FlagsNode(pos(), Flags.FINAL));
                    FlagsNode fn = extractFlags(modifiers, FieldKeyword);
        
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                        for (Object[] o : FieldDeclarators)
                        {
                            Position pos = (Position) o[0];
                            Id name = (Id) o[1];
                            if (name == null) name = nf.Id(pos, Name.makeFresh());
                            List<Id> exploded = (List<Id>) o[2];
                            TypeNode type = (TypeNode) o[3];
                            if (type == null) type = nf.UnknownTypeNode(name.position());
                            Expr init = (Expr) o[4];
                            FieldDecl fd = nf.FieldDecl(pos, fn,
                                               type, name, init);
                            fd = (FieldDecl) ((X10Ext) fd.ext()).annotations(extractAnnotations(modifiers));
                            fd = (FieldDecl) ((X10Ext) fd.ext()).setComment(comment(getRhsFirstTokenIndex(1)));
                            l.add(fd);
                        }
                    setResult(l);
          $EndJava
        ./
        
        
    --------------------------------------- Section :: Statement

    Statement ::= AnnotationStatement
                | ExpressionStatement

    AnnotationStatement ::= Annotationsopt NonExpressionStatement
        /.$BeginJava
                    if (NonExpressionStatement.ext() instanceof X10Ext) {
                        NonExpressionStatement = (Stmt) ((X10Ext) NonExpressionStatement.ext()).annotations(Annotationsopt);
                    }
                    setResult(NonExpressionStatement.position(pos()));
          $EndJava
        ./

    NonExpressionStatement ::= Block
                | EmptyStatement
                | AssertStatement
                | SwitchStatement
                | DoStatement
                | BreakStatement
                | ContinueStatement
                | ReturnStatement
                | ThrowStatement
                | TryStatement
                | LabeledStatement
                | IfThenStatement
                | IfThenElseStatement
                | WhileStatement
                | ForStatement
                | AsyncStatement
                | AtStatement
                | AtomicStatement
                | WhenStatement
--                | ForEachStatement
                | AtEachStatement
                | FinishStatement
                | NextStatement
                | ResumeStatement
--                | AwaitStatement
                | AssignPropertyCall
                | OfferStatement
    
   OfferStatement ::= offer Expression ;
         /.$BeginJava
                    setResult(nf.Offer(pos(), Expression));
          $EndJava
        ./
    
    IfThenStatement ::= if ( Expression ) Statement
        /.$BeginJava
                    setResult(nf.If(pos(), Expression, Statement));
          $EndJava
        ./
    
    IfThenElseStatement ::= if ( Expression ) Statement$s1 else Statement$s2
        /.$BeginJava
                    setResult(nf.If(pos(), Expression, s1, s2));
          $EndJava
        ./
    
    EmptyStatement ::= ;
        /.$BeginJava
                    setResult(nf.Empty(pos()));
          $EndJava
        ./
    
    LabeledStatement ::= Identifier : LoopStatement
        /.$BeginJava
                    setResult(nf.Labeled(pos(), Identifier, LoopStatement));
          $EndJava
        ./
        
    LoopStatement ::= ForStatement
                    | WhileStatement
                    | DoStatement
                    | AtEachStatement
--                    | ForEachStatement
    
    ExpressionStatement ::= StatementExpression ;
        /.$BeginJava
                    setResult(nf.Eval(pos(), StatementExpression));
          $EndJava
        ./
    
    StatementExpression ::= Assignment
                          | PreIncrementExpression
                          | PreDecrementExpression
                          | PostIncrementExpression
                          | PostDecrementExpression
                          | MethodInvocation
                          | ClassInstanceCreationExpression
    
    AssertStatement ::= assert Expression ;
        /.$BeginJava
                    setResult(nf.Assert(pos(), Expression));
          $EndJava
        ./
                      | assert Expression$expr1 : Expression$expr2 ;
        /.$BeginJava
                    setResult(nf.Assert(pos(), expr1, expr2));
          $EndJava
        ./
    
    SwitchStatement ::= switch ( Expression ) SwitchBlock
        /.$BeginJava
                    setResult(nf.Switch(pos(), Expression, SwitchBlock));
          $EndJava
        ./
    
    SwitchBlock ::= { SwitchBlockStatementGroupsopt SwitchLabelsopt }
        /.$BeginJava
                    SwitchBlockStatementGroupsopt.addAll(SwitchLabelsopt);
                    setResult(SwitchBlockStatementGroupsopt);
          $EndJava
        ./
    
    SwitchBlockStatementGroups ::= SwitchBlockStatementGroup
                                 | SwitchBlockStatementGroups SwitchBlockStatementGroup
        /.$BeginJava
                    SwitchBlockStatementGroups.addAll(SwitchBlockStatementGroup);
                    // setResult(SwitchBlockStatementGroups);
          $EndJava
        ./
    
    SwitchBlockStatementGroup ::= SwitchLabels BlockStatements
        /.$BeginJava
                    List<SwitchElement> l = new TypedList<SwitchElement>(new LinkedList<SwitchElement>(), SwitchElement.class, false);
                    l.addAll(SwitchLabels);
                    l.add(nf.SwitchBlock(pos(), BlockStatements));
                    setResult(l);
          $EndJava
        ./
    
    SwitchLabels ::= SwitchLabel
        /.$BeginJava
                    List<Case> l = new TypedList<Case>(new LinkedList<Case>(), Case.class, false);
                    l.add(SwitchLabel);
                    setResult(l);
          $EndJava
        ./
                   | SwitchLabels SwitchLabel
        /.$BeginJava
                    SwitchLabels.add(SwitchLabel);
                    //setResult(SwitchLabels);
          $EndJava
        ./
    
    SwitchLabel ::= case ConstantExpression :
        /.$BeginJava
                    setResult(nf.Case(pos(), ConstantExpression));
          $EndJava
        ./
                  | default :
        /.$BeginJava
                    setResult(nf.Default(pos()));
          $EndJava
        ./

    WhileStatement ::= while ( Expression ) Statement
        /.$BeginJava
                    setResult(nf.While(pos(), Expression, Statement));
          $EndJava
        ./
    
    DoStatement ::= do Statement while ( Expression ) ;
        /.$BeginJava
                    setResult(nf.Do(pos(), Statement, Expression));
          $EndJava
        ./
    
    ForStatement ::= BasicForStatement
                   | EnhancedForStatement
    
    BasicForStatement ::= for ( ForInitopt ; Expressionopt ; ForUpdateopt ) Statement
        /.$BeginJava
                    setResult(nf.For(pos(), ForInitopt, Expressionopt, ForUpdateopt, Statement));
          $EndJava
        ./
    
    ForInit ::= StatementExpressionList
              | LocalVariableDeclaration
        /.$BeginJava
                    List<ForInit> l = new TypedList<ForInit>(new LinkedList<ForInit>(), ForInit.class, false);
                    l.addAll(LocalVariableDeclaration);
                    //setResult(l);
          $EndJava
        ./
    
    ForUpdate ::= StatementExpressionList
    
    StatementExpressionList ::= StatementExpression
        /.$BeginJava
                    List<Eval> l = new TypedList<Eval>(new LinkedList<Eval>(), Eval.class, false);
                    l.add(nf.Eval(pos(), StatementExpression));
                    setResult(l);
          $EndJava
        ./
                              | StatementExpressionList , StatementExpression
        /.$BeginJava
                    StatementExpressionList.add(nf.Eval(pos(), StatementExpression));
          $EndJava
        ./
    
    BreakStatement ::= break Identifieropt ;
        /.$BeginJava
                    setResult(nf.Break(pos(), Identifieropt));
          $EndJava
        ./
    
    ContinueStatement ::= continue Identifieropt ;
        /.$BeginJava
                    setResult(nf.Continue(pos(), Identifieropt));
          $EndJava
        ./
    
    ReturnStatement ::= return Expressionopt ;
        /.$BeginJava
                    setResult(nf.Return(pos(), Expressionopt));
          $EndJava
        ./
    
    ThrowStatement ::= throw Expression ;
        /.$BeginJava
                    setResult(nf.Throw(pos(), Expression));
          $EndJava
        ./
    
    TryStatement ::= try Block Catches
        /.$BeginJava
                    setResult(nf.Try(pos(), Block, Catches));
          $EndJava
        ./
                   | try Block Catchesopt Finally
        /.$BeginJava
                    setResult(nf.Try(pos(), Block, Catchesopt, Finally));
          $EndJava
        ./
    
    Catches ::= CatchClause
        /.$BeginJava
                    List<Catch> l = new TypedList<Catch>(new LinkedList<Catch>(), Catch.class, false);
                    l.add(CatchClause);
                    setResult(l);
          $EndJava
        ./
              | Catches CatchClause
        /.$BeginJava
                    Catches.add(CatchClause);
                    //setResult(Catches);
          $EndJava
        ./
    
    CatchClause ::= catch ( FormalParameter ) Block
        /.$BeginJava
                    setResult(nf.Catch(pos(), FormalParameter, Block));
          $EndJava
        ./
    
    Finally ::= finally Block
        /.$BeginJava
                    setResult(Block);
          $EndJava
        ./

   ClockedClause ::= clocked ( ClockList )
        /.$BeginJava
                    setResult(ClockList);
          $EndJava
        ./
        

    AsyncStatement ::= async ClockedClauseopt Statement
        /.$BeginJava
                  setResult(nf.Async(pos(), ClockedClauseopt, Statement));
          $EndJava
        ./
         | clocked async Statement
        /.$BeginJava
                  setResult(nf.Async(pos(), Statement, true));
          $EndJava
        ./


    AtStatement ::= at PlaceExpressionSingleList Statement
        /.$BeginJava
                  setResult(nf.AtStmt(pos(), PlaceExpressionSingleList, Statement));
          $EndJava
        ./

    AtomicStatement ::= atomic Statement
        /.$BeginJava
                  setResult(nf.Atomic(pos(), nf.Here(pos(getLeftSpan())), Statement));
          $EndJava
        ./


    WhenStatement  ::= when ( Expression ) Statement
        /.$BeginJava
                    setResult(nf.When(pos(), Expression, Statement));
          $EndJava
        ./
--                     | WhenStatement or$or ( Expression ) Statement
--        /.$BeginJava
--                  WhenStatement.addBranch(pos(getRhsFirstTokenIndex($or), getRightSpan()), Expression, Statement);
--                  setResult(WhenStatement);
--          $EndJava
--        ./

--    ForEachStatement ::= foreach ( LoopIndex in Expression ) ClockedClauseopt Statement
--        /.$BeginJava
--                    FlagsNode fn = LoopIndex.flags();
--                    if (! fn.flags().isFinal()) {
--                        syntaxError("Enhanced foreach loop may not have var loop index" + LoopIndex, LoopIndex.position());
--                        fn = fn.flags(fn.flags().Final());
--                        LoopIndex = LoopIndex.flags(fn);
--                    }
--                    setResult(nf.ForEach(pos(),
--                                 LoopIndex,
--                                  Expression,
--                                  ClockedClauseopt,
--                                  Statement));
--          $EndJava
--        ./ 
--         | clocked foreach ( LoopIndex in Expression ) Statement
--        /.$BeginJava
--                    FlagsNode fn = LoopIndex.flags();
--                    if (! fn.flags().isFinal()) {
--                        syntaxError("Enhanced foreach loop cannot have var loop index" + LoopIndex, LoopIndex.position());
--                        fn = fn.flags(fn.flags().Final());
--                        LoopIndex = LoopIndex.flags(fn);
--                    }
--                    setResult(nf.ForEach(pos(),
--                                  LoopIndex,
--                                  Expression,
--                                  Statement));
--          $EndJava
--        ./ 
--         | foreach ( Expression ) Statement
--        /.$BeginJava
--                    Id name = nf.Id(pos(), Name.makeFresh());
--                    TypeNode type = nf.UnknownTypeNode(pos());
--                    setResult(nf.ForEach(pos(),
--                            nf.X10Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), type, name, null, true),
--                            Expression,
--                            new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false),
--                            Statement));
--          $EndJava
--        ./ 
--         | clocked foreach ( Expression ) Statement
--        /.$BeginJava
--                    Id name = nf.Id(pos(), Name.makeFresh());
--                    TypeNode type = nf.UnknownTypeNode(pos());
--                    setResult(nf.ForEach(pos(),
--                            nf.X10Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), type, name, null, true),
--                            Expression,
--                            Statement));
--          $EndJava
--        ./ 

    AtEachStatement ::= ateach ( LoopIndex in Expression ) ClockedClauseopt Statement
        /.$BeginJava
                    FlagsNode fn = LoopIndex.flags();
                    if (! fn.flags().isFinal()) {
                        syntaxError("Enhanced ateach loop may not have var loop index" + LoopIndex, LoopIndex.position());
                        fn = fn.flags(fn.flags().Final());
                        LoopIndex = LoopIndex.flags(fn);
                    }
                    setResult(nf.AtEach(pos(),
                                 LoopIndex,
                                 Expression,
                                 ClockedClauseopt,
                                 Statement));
          $EndJava
        ./   
     | ateach ( Expression ) Statement
        /.$BeginJava
                    Id name = nf.Id(pos(), Name.makeFresh());
                    TypeNode type = nf.UnknownTypeNode(pos());
                    setResult(nf.AtEach(pos(),
                            nf.X10Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), type, name, null, true),
                            Expression,
                            new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false),
                            Statement));
          $EndJava
        ./ 
    EnhancedForStatement ::= for ( LoopIndex in Expression ) Statement
        /.$BeginJava
                    FlagsNode fn = LoopIndex.flags();
                    if (! fn.flags().isFinal()) {
                        syntaxError("Enhanced for loop may not have var loop index" + LoopIndex, LoopIndex.position());
                        fn = fn.flags(fn.flags().Final());
                        LoopIndex = LoopIndex.flags(fn);
                    }
                    setResult(nf.ForLoop(pos(),
                            LoopIndex,
                            Expression,
                            Statement));
          $EndJava
        ./ 
       | for ( Expression ) Statement
        /.$BeginJava
                    Id name = nf.Id(pos(), Name.makeFresh());
                    TypeNode type = nf.UnknownTypeNode(pos());
                    setResult(nf.ForLoop(pos(),
                            nf.X10Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), type, name, null, true),
                            Expression,
                            Statement));
          $EndJava
        ./ 
        

    FinishStatement ::= finish Statement
        /.$BeginJava
                    setResult(nf.Finish(pos(),  Statement, false));
          $EndJava
        ./
                | clocked finish Statement
        /.$BeginJava
                    setResult(nf.Finish(pos(),  Statement, true));
          $EndJava
        ./
    PlaceExpressionSingleList ::= ( PlaceExpression )
        /.$BeginJava
                  setResult(PlaceExpression);
          $EndJava
        ./

    PlaceExpression ::= Expression

    NextStatement ::= next ;
        /.$BeginJava
                    setResult(nf.Next(pos()));
          $EndJava
        ./
        
        ResumeStatement ::= resume ;
        /.$BeginJava
                    setResult(nf.Resume(pos()));
          $EndJava
        ./

 ClockList ::= Clock
        /.$BeginJava
                    List<Expr> l = new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false);
                    l.add(Clock);
                    setResult(l);
          $EndJava
        ./
                | ClockList , Clock
        /.$BeginJava
                    ClockList.add(Clock);
                    setResult(ClockList);
          $EndJava
        ./

    -- The type-checker will ensure that the identifier names a variable declared as a clock.
    Clock ::= Expression
        /.$BeginJava
                    setResult(Expression);
          $EndJava
        ./
--
--      Clock ::= Identifier
--        /.$BeginJava
--                    setResult(new X10ParsedName(nf, ts, pos(), Identifier).toExpr());
--          $EndJava
--        ./

    CastExpression ::= Primary
                     | ExpressionName
        /.$BeginJava
                    setResult(ExpressionName.toExpr());
          $EndJava
        ./
                     | CastExpression as Type
        /.$BeginJava
                    setResult(nf.X10Cast(pos(), Type, CastExpression));
          $EndJava
        ./
    
     --------------------------------------- Section :: Expression
     TypeParamWithVarianceList ::= TypeParamWithVariance
        /.$BeginJava
                    List<TypeParamNode> l = new TypedList<TypeParamNode>(new LinkedList<TypeParamNode>(), TypeParamNode.class, false);
                    l.add(TypeParamWithVariance);
                    setResult(l);
          $EndJava
        ./
                      | TypeParamWithVarianceList , TypeParamWithVariance
        /.$BeginJava
                    TypeParamWithVarianceList.add(TypeParamWithVariance);
                    setResult(TypeParamWithVarianceList);
          $EndJava
        ./
        
     TypeParameterList ::= TypeParameter
        /.$BeginJava
                    List<TypeParamNode> l = new TypedList<TypeParamNode>(new LinkedList<TypeParamNode>(), TypeParamNode.class, false);
                    l.add(TypeParameter);
                    setResult(l);
          $EndJava
        ./
                      | TypeParameterList , TypeParameter
        /.$BeginJava
                    TypeParameterList.add(TypeParameter);
                    setResult(TypeParameterList);
          $EndJava
        ./
        
    TypeParamWithVariance ::= Identifier
        /.$BeginJava
                    setResult(nf.TypeParamNode(pos(), Identifier, ParameterType.Variance.INVARIANT));
          $EndJava
        ./
                   | + Identifier
        /.$BeginJava
                    setResult(nf.TypeParamNode(pos(), Identifier, ParameterType.Variance.COVARIANT));
          $EndJava
        ./
                   | - Identifier
        /.$BeginJava
                    setResult(nf.TypeParamNode(pos(), Identifier, ParameterType.Variance.CONTRAVARIANT));
          $EndJava
        ./
        
    TypeParameter ::= Identifier
        /.$BeginJava
                    setResult(nf.TypeParamNode(pos(), Identifier));
          $EndJava
        ./

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    RegionExpression ::= Expression
--
--    RegionExpressionList ::= RegionExpression
--        /.$BeginJava
--                    List<Expr> l = new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false);
--                    l.add(RegionExpression);
--                    setResult(l);
--          $EndJava
--        ./
--               | RegionExpressionList , RegionExpression
--        /.$BeginJava
--                    RegionExpressionList.add(RegionExpression);
--                    //setResult(RegionExpressionList);
--          $EndJava
--        ./

    AssignmentExpression ::= Expression$expr1 '->' Expression$expr2
        /.$BeginJava
                    Expr call = nf.ConstantDistMaker(pos(), expr1, expr2);
                    setResult(call);
          $EndJava
        ./
    ClosureExpression ::= FormalParameters WhereClauseopt HasResultTypeopt  Offersopt => ClosureBody
        /.$BeginJava
                    setResult(nf.Closure(pos(), FormalParameters, WhereClauseopt, 
              HasResultTypeopt == null ? nf.UnknownTypeNode(pos()) : HasResultTypeopt,  ClosureBody));
          $EndJava
        ./

    LastExpression ::= Expression
        /.$BeginJava
                    setResult(nf.X10Return(pos(), Expression, true));
          $EndJava
        ./

    ClosureBody ::= ConditionalExpression
        /.$BeginJava
                    setResult(nf.Block(pos(), nf.X10Return(pos(), ConditionalExpression, true)));
          $EndJava
        ./
                  | Annotationsopt { BlockStatementsopt LastExpression }
        /.$BeginJava
                    List<Stmt> l = new ArrayList<Stmt>();
                    l.addAll(BlockStatementsopt);
                    l.add(LastExpression);
                    Block b = nf.Block(pos(), l);
                    b = (Block) ((X10Ext) b.ext()).annotations(Annotationsopt);
                    setResult(b);
          $EndJava
        ./
                  | Annotationsopt Block
        /.$BeginJava
                    Block b = Block;
                    b = (Block) ((X10Ext) b.ext()).annotations(Annotationsopt);
                    setResult(b.position(pos()));
          $EndJava
        ./
                  
                  
    AtExpression ::= at PlaceExpressionSingleList ClosureBody
        /.$BeginJava
                    setResult(nf.AtExpr(pos(), PlaceExpressionSingleList, nf.UnknownTypeNode(pos()), ClosureBody));
          $EndJava
        ./

    FinishExpression ::= finish ( Expression ) Block
        /.$BeginJava
                    setResult(nf.FinishExpr(pos(), Expression, Block));
          $EndJava
        ./
        
    ---------------------------------------- All the opts...

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    DepParametersopt ::= %Empty
--        /.$NullAction./
--                       | DepParameters

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    PropertyListopt ::=  %Empty
--        /.$BeginJavppa
--                    setResult(new TypedList<PropertyDecl>(new LinkedList<PropertyDecl>(), PropertyDecl.class, false));
--          $EndJava
--        ./
--                       | PropertyList
                       
    WhereClauseopt ::= %Empty
        /.$NullAction./
                     | WhereClause

    PlaceExpressionSingleListopt ::= %Empty
        /.$NullAction./
                                   | PlaceExpressionSingleList

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    ClassModifiersopt ::= %Empty
--        /.$BeginJava
--             setResult(Collections.singletonList(nf.FlagsNode(JPGPosition.COMPILER_GENERATED, X10Flags.toX10Flags(Flags.NONE))));
--          $EndJava ./
--          | ClassModifiers
--          
--    TypeDefModifiersopt ::= %Empty
--        /.$BeginJava
--             setResult(Collections.singletonList(nf.FlagsNode(JPGPosition.COMPILER_GENERATED, X10Flags.toX10Flags(Flags.NONE))));
--          $EndJava ./
--          | TypeDefModifiers
          
    ClockedClauseopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false));
          $EndJava
        ./
                       | ClockedClause


    ------------------------------------------------------------
    --- All the Java-derived rules

    identifier ::= IDENTIFIER$ident
        /.$BeginJava
                    ident.setKind($sym_type.TK_IDENTIFIER);
                    setResult(id(getRhsFirstTokenIndex($ident)));
          $EndJava
        ./

    TypeName ::= Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf, ts, pos(), Identifier));
          $EndJava
        ./
               | TypeName . Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf,
                                      ts,
                                      pos(getLeftSpan(), getRightSpan()),
                                      TypeName,
                                      Identifier));
          $EndJava
        ./

    ClassName ::= TypeName

    TypeArguments ::= '[' TypeArgumentList ']'
        /.$BeginJava
                    setResult(TypeArgumentList);
          $EndJava
        ./

    
    TypeArgumentList ::= Type
        /.$BeginJava
                    List<TypeNode> l = new ArrayList<TypeNode>();
                    l.add(Type);
                    setResult(l);
          $EndJava
        ./
                       | TypeArgumentList , Type
        /.$BeginJava
                    TypeArgumentList.add(Type);
          $EndJava
        ./
        
    

    -- Chapter 6

    PackageName ::= Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf, ts, pos(), Identifier));
          $EndJava
        ./
                  | PackageName . Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf,
                                      ts,
                                      pos(getLeftSpan(), getRightSpan()),
                                      PackageName,
                                      Identifier));
          $EndJava
        ./

    --
    -- See Chapter 4
    --
    -- TypeName ::= Identifier
    --           | PackageOrTypeName . Identifier
    --
    ExpressionName ::=? Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf, ts, pos(), Identifier));
          $EndJava
        ./
                     | AmbiguousName . Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf,
                                      ts,
                                      pos(getLeftSpan(), getRightSpan()),
                                      AmbiguousName,
                                      Identifier));
          $EndJava
        ./

    MethodName ::=? Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf, ts, pos(), Identifier));
          $EndJava
        ./
                 | AmbiguousName . Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf,
                                      ts,
                                      pos(getLeftSpan(), getRightSpan()),
                                      AmbiguousName,
                                      Identifier));
          $EndJava
        ./

    PackageOrTypeName ::= Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf, ts, pos(), Identifier));
          $EndJava
        ./
                        | PackageOrTypeName . Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf,
                                      ts,
                                      pos(getLeftSpan(), getRightSpan()),
                                      PackageOrTypeName,
                                      Identifier));
          $EndJava
        ./

    AmbiguousName ::=? Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf, ts, pos(), Identifier));
          $EndJava
        ./
                    | AmbiguousName . Identifier
        /.$BeginJava
                    setResult(new X10ParsedName(nf,
                                      ts,
                                      pos(getLeftSpan(), getRightSpan()),
                                      AmbiguousName,
                                      Identifier));
         $EndJava
        ./

    -- Chapter 7

    CompilationUnit ::= PackageDeclarationopt TypeDeclarationsopt
        /.$BeginJava
                    // Add import x10.lang.* by default.
//                    int token_pos = (ImportDeclarationsopt.size() == 0
//                                       ? TypeDeclarationsopt.size() == 0
//                                               ? prsStream.getSize() - 1
//                                               : prsStream.getPrevious(getRhsFirstTokenIndex($TypeDeclarationsopt))
//                                     : getRhsLastTokenIndex($ImportDeclarationsopt)
//                                );
//                    Import x10LangImport = 
//                    nf.Import(pos(token_pos), Import.PACKAGE, QName.make("x10.lang"));
//                    ImportDeclarationsopt.add(x10LangImport);
                    setResult(nf.SourceFile(pos(getLeftSpan(), getRightSpan()),
                                            PackageDeclarationopt,
                                            new TypedList<Import>(new LinkedList<Import>(), Import.class, false),
                                            TypeDeclarationsopt));
          $EndJava
        ./
                      | PackageDeclarationopt ImportDeclarations TypeDeclarationsopt
        /.$BeginJava
                    setResult(nf.SourceFile(pos(getLeftSpan(), getRightSpan()),
                                            PackageDeclarationopt,
                                            ImportDeclarations,
                                            TypeDeclarationsopt));
          $EndJava
        ./
                      | ImportDeclarations PackageDeclaration$misplacedPackageDeclaration ImportDeclarationsopt$misplacedImportDeclarations TypeDeclarationsopt  -- Extend grammar to accept this illegal construct so that we can fail gracefully
        /.$BeginJava
                    syntaxError("Misplaced package declaration", misplacedPackageDeclaration.position());
                    ImportDeclarations.addAll(misplacedImportDeclarations); // merge the two import lists
                    setResult(nf.SourceFile(pos(getLeftSpan(), getRightSpan()),
                                            misplacedPackageDeclaration,
                                            ImportDeclarations,
                                            TypeDeclarationsopt));
          $EndJava
        ./
                      | PackageDeclaration ImportDeclarations PackageDeclaration$misplacedPackageDeclaration ImportDeclarationsopt$misplacedImportDeclarations TypeDeclarationsopt  -- Extend grammar to accept this illegal construct so that we can fail gracefully
        /.$BeginJava
                    syntaxError("Misplaced package declaration, ignoring", misplacedPackageDeclaration.position());
                    ImportDeclarations.addAll(misplacedImportDeclarations); // merge the two import lists
                    setResult(nf.SourceFile(pos(getLeftSpan(), getRightSpan()),
                                            PackageDeclaration,
                                            ImportDeclarations,
                                            TypeDeclarationsopt));
          $EndJava
        ./

    ImportDeclarations ::= ImportDeclaration
        /.$BeginJava
                    List<Import> l = new TypedList<Import>(new LinkedList<Import>(), Import.class, false);
                    l.add(ImportDeclaration);
                    setResult(l);
          $EndJava
        ./
                         | ImportDeclarations ImportDeclaration
        /.$BeginJava
                    if (ImportDeclaration != null)
                        ImportDeclarations.add(ImportDeclaration);
                    //setResult(l);
          $EndJava
        ./

    TypeDeclarations ::= TypeDeclaration
        /.$BeginJava
                    List<TopLevelDecl> l = new TypedList<TopLevelDecl>(new LinkedList<TopLevelDecl>(), TopLevelDecl.class, false);
                    if (TypeDeclaration != null)
                        l.add(TypeDeclaration);
                    setResult(l);
          $EndJava
        ./
                       | TypeDeclarations TypeDeclaration
        /.$BeginJava
                    if (TypeDeclaration != null)
                        TypeDeclarations.add(TypeDeclaration);
                    //setResult(l);
          $EndJava
        ./

    PackageDeclaration ::= Annotationsopt package PackageName ;
        /.$BeginJava
                    PackageNode pn = PackageName.toPackage();
                    pn = (PackageNode) ((X10Ext) pn.ext()).annotations(Annotationsopt);
                    setResult(pn.position(pos()));
          $EndJava
        ./
    

    ImportDeclaration ::= SingleTypeImportDeclaration
                        | TypeImportOnDemandDeclaration
--                        | SingleStaticImportDeclaration
--                        | StaticImportOnDemandDeclaration

    SingleTypeImportDeclaration ::= import TypeName ;
        /.$BeginJava
                    setResult(nf.Import(pos(getLeftSpan(), getRightSpan()), Import.CLASS, QName.make(TypeName.toString())));
          $EndJava
        ./

    TypeImportOnDemandDeclaration ::= import PackageOrTypeName . * ;
        /.$BeginJava
                    setResult(nf.Import(pos(getLeftSpan(), getRightSpan()), Import.PACKAGE, QName.make(PackageOrTypeName.toString())));
          $EndJava
        ./
    
--    SingleStaticImportDeclaration ::= import static TypeName . Identifier ;
--        /.$BadAction./

--    StaticImportOnDemandDeclaration ::= import static TypeName . * ;
--        /.$BadAction./

    TypeDeclaration ::= ClassDeclaration
                      | InterfaceDeclaration
                      | TypeDefDeclaration
                      | ;
        /.$BeginJava
                    setResult(null);
          $EndJava
        ./

    -- Chapter 8

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    ClassModifiers ::= ClassModifier
--        /.$BeginJava
--                    List<Node> l = new LinkedList<Node>();
--                    l.addAll(ClassModifier);
--                    setResult(l);
--          $EndJava
--        ./
--                     | ClassModifiers ClassModifier
--        /.$BeginJava
--                    ClassModifiers.addAll(ClassModifier);
--          $EndJava
--        ./
--
--    ClassModifier ::= Annotation
--        /.$BeginJava
--                    setResult(Collections.singletonList(Annotation));
--          $EndJava
--        ./
--                    | public
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PUBLIC)));
--          $EndJava
--        ./
--                    | protected
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PROTECTED)));
--          $EndJava
--        ./
--                    | private
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PRIVATE)));
--          $EndJava
--        ./
--                    | abstract
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.ABSTRACT)));
--          $EndJava
--        ./
--                    | static
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.STATIC)));
--          $EndJava
--        ./
--                    | final
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.FINAL)));
--          $EndJava
--        ./
--                    | safe
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.SAFE)));
--          $EndJava
--        ./
--        
--    TypeDefModifiers ::= TypeDefModifier
--        /.$BeginJava
--                    List<Node> l = new LinkedList<Node>();
--                    l.addAll(TypeDefModifier);
--                    setResult(l);
--          $EndJava
--        ./
--                     | TypeDefModifiers TypeDefModifier
--        /.$BeginJava
--                    TypeDefModifiers.addAll(TypeDefModifier);
--          $EndJava
--        ./
--
--    TypeDefModifier ::= Annotation
--        /.$BeginJava
--                    setResult(Collections.singletonList(Annotation));
--          $EndJava
--        ./
--                    | public
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PUBLIC)));
--          $EndJava
--        ./
--                    | protected
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PROTECTED)));
--          $EndJava
--        ./
--                    | private
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PRIVATE)));
--          $EndJava
--        ./
--                    | abstract
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.ABSTRACT)));
--          $EndJava
--        ./
--                    | static
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.STATIC)));
--          $EndJava
--        ./
--                    | final
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.FINAL)));
--          $EndJava
--        ./
        
    --
    -- See Chapter 4
    --
    Interfaces ::= implements InterfaceTypeList
        /.$BeginJava
                    setResult(InterfaceTypeList);
          $EndJava
        ./

    InterfaceTypeList ::= Type
        /.$BeginJava
                    List<TypeNode> l = new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false);
                    l.add(Type);
                    setResult(l);
          $EndJava
        ./
                        | InterfaceTypeList , Type
        /.$BeginJava
                    InterfaceTypeList.add(Type);
                    setResult(InterfaceTypeList);
          $EndJava
        ./

    --
    -- See Chapter 4
    --
    ClassBody ::= { ClassBodyDeclarationsopt }
        /.$BeginJava
                    setResult(nf.ClassBody(pos(getLeftSpan(), getRightSpan()), ClassBodyDeclarationsopt));
          $EndJava
        ./

    ClassBodyDeclarations ::= ClassBodyDeclaration
                            | ClassBodyDeclarations ClassBodyDeclaration
        /.$BeginJava
                    ClassBodyDeclarations.addAll(ClassBodyDeclaration);
                    // setResult(a);
          $EndJava
        ./

    ClassBodyDeclaration ::= ClassMemberDeclaration
--                           | InstanceInitializer
--        /.$BeginJava
--                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
--                    l.add(InstanceInitializer);
--                    setResult(l);
--          $EndJava
--        ./
--                           | StaticInitializer
--        /.$BeginJava
--                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
--                    l.add(StaticInitializer);
--                    setResult(l);
--          $EndJava
--        ./
                           | ConstructorDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(ConstructorDeclaration);
                    setResult(l);
          $EndJava
        ./

    ClassMemberDeclaration ::= FieldDeclaration
                             | MethodDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(MethodDeclaration);
                    setResult(l);
          $EndJava
        ./
                             | PropertyMethodDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(PropertyMethodDeclaration);
                    setResult(l);
          $EndJava
        ./
                             | TypeDefDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(TypeDefDeclaration);
                    setResult(l);
          $EndJava
        ./
                             | ClassDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(ClassDeclaration);
                    setResult(l);
          $EndJava
        ./
                             | InterfaceDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(InterfaceDeclaration);
                    setResult(l);
          $EndJava
        ./
                             | ;
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    setResult(l);
          $EndJava
        ./
    
    FormalDeclarators ::= FormalDeclarator
        /.$BeginJava
                    List<Object[]> l = new TypedList<Object[]>(new LinkedList<Object[]>(), Object[].class, false);
                    l.add(FormalDeclarator);
                    setResult(l);
          $EndJava
        ./
                          | FormalDeclarators , FormalDeclarator
        /.$BeginJava
                    FormalDeclarators.add(FormalDeclarator);
          $EndJava
        ./
    
    
    FieldDeclarators ::= FieldDeclarator
        /.$BeginJava
                    List<Object[]> l = new TypedList<Object[]>(new LinkedList<Object[]>(), Object[].class, false);
                    l.add(FieldDeclarator);
                    setResult(l);
          $EndJava
        ./
                          | FieldDeclarators , FieldDeclarator
        /.$BeginJava
                    FieldDeclarators.add(FieldDeclarator);
                    // setResult(FieldDeclarators);
          $EndJava
        ./
    
    
    VariableDeclaratorsWithType ::= VariableDeclaratorWithType
        /.$BeginJava
                    List<Object[]> l = new TypedList<Object[]>(new LinkedList<Object[]>(), Object[].class, false);
                    l.add(VariableDeclaratorWithType);
                    setResult(l);
          $EndJava
        ./
                          | VariableDeclaratorsWithType , VariableDeclaratorWithType
        /.$BeginJava
                    VariableDeclaratorsWithType.add(VariableDeclaratorWithType);
                    // setResult(VariableDeclaratorsWithType);
          $EndJava
        ./
    
    VariableDeclarators ::= VariableDeclarator
        /.$BeginJava
                    List<Object[]> l = new TypedList<Object[]>(new LinkedList<Object[]>(), Object[].class, false);
                    l.add(VariableDeclarator);
                    setResult(l);
          $EndJava
        ./
                          | VariableDeclarators , VariableDeclarator
        /.$BeginJava
                    VariableDeclarators.add(VariableDeclarator);
                    // setResult(VariableDeclarators);
          $EndJava
        ./
    
    VariableInitializer ::= Expression
    
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    FieldModifiers ::= FieldModifier
--        /.$BeginJava
--                    List<Node> l = new LinkedList<Node>();
--                    l.addAll(FieldModifier);
--                    setResult(l);
--          $EndJava
--        ./
--                     | FieldModifiers FieldModifier
--        /.$BeginJava
--                    FieldModifiers.addAll(FieldModifier);
--          $EndJava
--        ./
--    
--    FieldModifier ::= Annotation
--        /.$BeginJava
--                    setResult(Collections.singletonList(Annotation));
--          $EndJava
--        ./
--                    | public
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PUBLIC)));
--          $EndJava
--        ./
--                    | protected
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PROTECTED)));
--          $EndJava
--        ./
--                    | private
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PRIVATE)));
--          $EndJava
--        ./
--                    | static
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.STATIC)));
--          $EndJava
--        ./
--                    | global
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.GLOBAL)));
--          $EndJava
--        ./
    
    ResultType ::= : Type
     /.$BeginJava
                    setResult(Type);
          $EndJava
        ./
    HasResultType ::= : Type
     /.$BeginJava
                    setResult(Type);
          $EndJava
        ./
                  | '<:' Type
     /.$BeginJava
                    setResult(nf.HasType(Type));
          $EndJava
        ./

--
-- This duplicated rule is not needed!
--       
--    FormalParameters ::= ( FormalParameterList )
--        /.$BeginJava
--                    setResult(FormalParameterList);
--          $EndJava
--        ./
    
    FormalParameterList ::= FormalParameter
        /.$BeginJava
                    List<Formal> l = new TypedList<Formal>(new LinkedList<Formal>(), Formal.class, false);
                    l.add(FormalParameter);
                    setResult(l);
          $EndJava
        ./
                       | FormalParameterList , FormalParameter
        /.$BeginJava
                    FormalParameterList.add(FormalParameter);
          $EndJava
        ./
        
     LoopIndexDeclarator ::= Identifier HasResultTypeopt
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, Collections.<Id>emptyList(), null, HasResultTypeopt, null });
          $EndJava
        ./
                         | '[' IdentifierList ']' HasResultTypeopt
        /.$BeginJava
                    setResult(new Object[] { pos(), null, IdentifierList, null, HasResultTypeopt, null });
          $EndJava
        ./
                         | Identifier '[' IdentifierList ']' HasResultTypeopt
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, IdentifierList, null, HasResultTypeopt, null });
          $EndJava
        ./
        
    LoopIndex ::= Modifiersopt LoopIndexDeclarator
        /.$BeginJava
                List<Node> modifiers = checkVariableModifiers(Modifiersopt);
                Formal f;
                FlagsNode fn = extractFlags(modifiers, Flags.FINAL);
                Object[] o = LoopIndexDeclarator;
                Position pos = (Position) o[0];
                Id name = (Id) o[1];
                boolean unnamed = name == null;
                if (name == null) name = nf.Id(pos, Name.makeFresh());
                List<Id> exploded = (List<Id>) o[2];
                DepParameterExpr guard = (DepParameterExpr) o[3];
                TypeNode type = (TypeNode) o[4];
                if (type == null) type = nf.UnknownTypeNode(name != null ? name.position() : pos);
                List<Formal> explodedFormals = new ArrayList<Formal>();
                for (Id id : exploded) {
                    explodedFormals.add(nf.Formal(id.position(), fn, nf.UnknownTypeNode(id.position()), id));
                }
                f = nf.X10Formal(pos(), fn, type, name, explodedFormals, unnamed);
                f = (Formal) ((X10Ext) f.ext()).annotations(extractAnnotations(modifiers));
                setResult(f);
          $EndJava
        ./
                      | Modifiersopt VarKeyword LoopIndexDeclarator
        /.$BeginJava
                List<Node> modifiers = checkVariableModifiers(Modifiersopt);
                Formal f;
                FlagsNode fn = extractFlags(modifiers, VarKeyword);
                Object[] o = LoopIndexDeclarator;
                Position pos = (Position) o[0];
                Id name = (Id) o[1];
                boolean unnamed = name == null;
                if (name == null) name = nf.Id(pos, Name.makeFresh());
                List<Id> exploded = (List<Id>) o[2];
                DepParameterExpr guard = (DepParameterExpr) o[3];
                TypeNode type = (TypeNode) o[4];
                if (type == null) type = nf.UnknownTypeNode(name != null ? name.position() : pos);
                List<Formal> explodedFormals = new ArrayList<Formal>();
                for (Id id : exploded) {
                    explodedFormals.add(nf.Formal(id.position(), fn, nf.UnknownTypeNode(id.position()), id));
                }
                f = nf.X10Formal(pos(), fn, type, name, explodedFormals, unnamed);
                f = (Formal) ((X10Ext) f.ext()).annotations(extractAnnotations(modifiers));
                setResult(f);
          $EndJava
        ./
    
    FormalParameter ::= Modifiersopt FormalDeclarator
        /.$BeginJava
                List<Node> modifiers = checkVariableModifiers(Modifiersopt);
                Formal f;
                FlagsNode fn = extractFlags(modifiers, Flags.FINAL);
                Object[] o = FormalDeclarator;
                Position pos = (Position) o[0];
                Id name = (Id) o[1];
                boolean unnamed = name == null;
                if (name == null) name = nf.Id(pos, Name.makeFresh());
                List<Id> exploded = (List<Id>) o[2];
                DepParameterExpr guard = (DepParameterExpr) o[3];
                TypeNode type = (TypeNode) o[4];
                if (type == null) type = nf.UnknownTypeNode(name != null ? name.position() : pos);
                Expr init = (Expr) o[5];
                List<Formal> explodedFormals = new ArrayList<Formal>();
                for (Id id : exploded) {
                    explodedFormals.add(nf.Formal(id.position(), fn, nf.UnknownTypeNode(id.position()), id));
                }
                f = nf.X10Formal(pos(), fn, type, name, explodedFormals, unnamed);
                f = (Formal) ((X10Ext) f.ext()).annotations(extractAnnotations(modifiers));
                setResult(f);
          $EndJava
        ./
                      | Modifiersopt VarKeyword FormalDeclarator
        /.$BeginJava
                List<Node> modifiers = checkVariableModifiers(Modifiersopt);
                Formal f;
                FlagsNode fn = extractFlags(modifiers, VarKeyword);
                Object[] o = FormalDeclarator;
                Position pos = (Position) o[0];
                Id name = (Id) o[1];
                boolean unnamed = name == null;
                if (name == null) name = nf.Id(pos, Name.makeFresh());
                List<Id> exploded = (List<Id>) o[2];
                DepParameterExpr guard = (DepParameterExpr) o[3];
                TypeNode type = (TypeNode) o[4];
                if (type == null) type = nf.UnknownTypeNode(name != null ? name.position() : pos);
                Expr init = (Expr) o[5];
                List<Formal> explodedFormals = new ArrayList<Formal>();
                for (Id id : exploded) {
                    explodedFormals.add(nf.Formal(id.position(), fn, nf.UnknownTypeNode(id.position()), id));
                }
                f = nf.X10Formal(pos(), fn, type, name, explodedFormals, unnamed);
                f = (Formal) ((X10Ext) f.ext()).annotations(extractAnnotations(modifiers));
                setResult(f);
          $EndJava
        ./
                      | Type
        /.$BeginJava
                Formal f;
                f = nf.X10Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), Type, nf.Id(pos(), Name.makeFresh("id$")), Collections.<Formal>emptyList(), true);
                setResult(f);
          $EndJava
        ./
    
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    VariableModifiers ::= VariableModifier
--        /.$BeginJava
--                    List<Node> l = new LinkedList<Node>();
--                    l.addAll(VariableModifier);
--                    setResult(l);
--          $EndJava
--        ./
--                        | VariableModifiers VariableModifier
--        /.$BeginJava
--                    VariableModifiers.addAll(VariableModifier);
--          $EndJava
--        ./
--    
--    VariableModifier ::= Annotation
--        /.$BeginJava
--                    setResult(Collections.singletonList(Annotation));
--          $EndJava
--        ./
--                       | shared
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.SHARED)));
--          $EndJava
--        ./
    
    --
    -- See above
    --    
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    MethodModifiers ::= MethodModifier
--        /.$BeginJava
--                    List<Node> l = new LinkedList<Node>();
--                    l.addAll(MethodModifier);
--                    setResult(l);
--          $EndJava
--        ./
--                      | MethodModifiers MethodModifier
--        /.$BeginJava
--                    MethodModifiers.addAll(MethodModifier);
--          $EndJava
--        ./
--    
--    MethodModifier ::= Annotation
--        /.$BeginJava
--                    setResult(Collections.singletonList(Annotation));
--          $EndJava
--        ./
--                     | public
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PUBLIC)));
--          $EndJava
--        ./
--                     | protected
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PROTECTED)));
--          $EndJava
--        ./
--                     | private
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PRIVATE)));
--          $EndJava
--        ./
--                     | abstract
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.ABSTRACT)));
--          $EndJava
--        ./
--                     | static
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.STATIC)));
--          $EndJava
--        ./
--                     | final
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.FINAL)));
--          $EndJava
--        ./
--                     | native
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.NATIVE)));
--          $EndJava
--        ./
--                     | atomic
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.ATOMIC)));
--          $EndJava
--        ./
--                     | extern
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.EXTERN)));
--          $EndJava
--        ./
--                     | safe
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.SAFE)));
--          $EndJava
--        ./
--                     | sequential
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.SEQUENTIAL)));
--          $EndJava
--        ./
--                     | nonblocking
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.NON_BLOCKING)));
--          $EndJava
--        ./
--                     | incomplete
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.INCOMPLETE)));
--          $EndJava
--        ./
--                     | property
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.PROPERTY)));
--          $EndJava
--        ./
--                     | global
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.GLOBAL)));
--          $EndJava
--        ./
--                     | proto
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), X10Flags.PROTO)));
--          $EndJava
--        ./

    
--    Throws ::= throws ExceptionTypeList
--        /.$BeginJava
--                    setResult(ExceptionTypeList);
--          $EndJava
--       ./
     Offers ::= offers Type
        /.$BeginJava
                    setResult(Type);
          $EndJava
        ./
    
    ExceptionTypeList ::= ExceptionType
        /.$BeginJava
                    List<TypeNode> l = new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false);
                    l.add(ExceptionType);
                    setResult(l);
          $EndJava
        ./
                        | ExceptionTypeList , ExceptionType
        /.$BeginJava
                    ExceptionTypeList.add(ExceptionType);
          $EndJava
        ./
    
    ExceptionType ::= ClassType
        
    MethodBody ::= = LastExpression ;
        /.$BeginJava
                    setResult(nf.Block(pos(), LastExpression));
          $EndJava
        ./
                  | = Annotationsopt { BlockStatementsopt LastExpression }
        /.$BeginJava
                    List<Stmt> l = new ArrayList<Stmt>();
                    l.addAll(BlockStatementsopt);
                    l.add(LastExpression);
                    setResult((Block) ((X10Ext) nf.Block(pos(),l).ext()).annotations(Annotationsopt));
          $EndJava
        ./
                  | = Annotationsopt Block
        /.$BeginJava
                    setResult((Block) ((X10Ext) Block.ext()).annotations(Annotationsopt).position(pos()));
          $EndJava
        ./
                  | Annotationsopt Block
        /.$BeginJava
                    setResult((Block) ((X10Ext) Block.ext()).annotations(Annotationsopt).position(pos()));
          $EndJava
        ./
                      | ;
        /.$NullAction./
    
--    InstanceInitializer ::= Block
--        /.$BeginJava
--                    setResult(nf.Initializer(pos(), nf.FlagsNode(pos(), Flags.NONE), Block));
--          $EndJava
--        ./
    
--    StaticInitializer ::= static Block
--        /.$BeginJava
--                    setResult(nf.Initializer(pos(), nf.FlagsNode(pos(getLeftSpan()), Flags.STATIC), Block));
--          $EndJava
--        ./
      
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    SimpleTypeName ::= Identifier
--        /.$BeginJava
--                    setResult(new X10ParsedName(nf, ts, pos(), Identifier));
--          $EndJava
--        ./

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    ConstructorModifiers ::= ConstructorModifier
--        /.$BeginJava
--                    List<Node> l = new LinkedList<Node>();
--                    l.addAll(ConstructorModifier);
--                    setResult(l);
--          $EndJava
--        ./
--                           | ConstructorModifiers ConstructorModifier
--        /.$BeginJava
--                    ConstructorModifiers.addAll(ConstructorModifier);
--          $EndJava
--        ./
--    
--    ConstructorModifier ::= Annotation
--        /.$BeginJava
--                    setResult(Collections.singletonList(Annotation));
--          $EndJava
--        ./
--                          | public
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PUBLIC)));
--          $EndJava
--        ./
--                          | protected
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PROTECTED)));
--          $EndJava
--        ./
--                          | private
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PRIVATE)));
--          $EndJava
--        ./
--                          | native
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.NATIVE)));
--          $EndJava
--        ./
    
    ConstructorBody ::= = ConstructorBlock
        /.$BeginJava
                    setResult(ConstructorBlock);
          $EndJava
        ./
                      | ConstructorBlock
        /.$BeginJava
                    setResult(ConstructorBlock);
          $EndJava
        ./
                    | = ExplicitConstructorInvocation
        /.$BeginJava
                    List<Stmt> l = new TypedList<Stmt>(new LinkedList<Stmt>(), Stmt.class, false);
                    l.add(ExplicitConstructorInvocation);
                    setResult(nf.Block(pos(), l));
          $EndJava
        ./
                    | = AssignPropertyCall
        /.$BeginJava
                    List<Stmt> l = new TypedList<Stmt>(new LinkedList<Stmt>(), Stmt.class, false);
                    l.add(AssignPropertyCall);
                    setResult(nf.Block(pos(), l));
          $EndJava
        ./
                      | ;
        /.$NullAction./

    ConstructorBlock ::= { ExplicitConstructorInvocationopt BlockStatementsopt }
        /.$BeginJava
                    List<Stmt> l = new TypedList<Stmt>(new LinkedList<Stmt>(), Stmt.class, false);
                    if (ExplicitConstructorInvocationopt != null)
                    {
                        l.add(ExplicitConstructorInvocationopt);
                    }
                    l.addAll(BlockStatementsopt);
                    setResult(nf.Block(pos(), l));
          $EndJava
        ./
    
    Arguments ::= ( ArgumentListopt )
        /.$BeginJava
                    setResult(ArgumentListopt);
          $EndJava
        ./
    
    -- chapter 9
    
    InterfaceDeclaration ::= NormalInterfaceDeclaration
    
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    InterfaceModifiers ::= InterfaceModifier
--        /.$BeginJava
--                    List<Node> l = new LinkedList<Node>();
--                    l.addAll(InterfaceModifier);
--                    setResult(l);
--          $EndJava
--        ./
--                         | InterfaceModifiers InterfaceModifier
--        /.$BeginJava
--                    InterfaceModifiers.addAll(InterfaceModifier);
--          $EndJava
--        ./
--    
--    InterfaceModifier ::= Annotation
--        /.$BeginJava
--                    setResult(Collections.singletonList(Annotation));
--          $EndJava
--        ./
--                        | public
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PUBLIC)));
--          $EndJava
--        ./
--                        | protected
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PROTECTED)));
--          $EndJava
--        ./
--                        | private
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.PRIVATE)));
--          $EndJava
--        ./
--                        | abstract
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.ABSTRACT)));
--          $EndJava
--        ./
--                        | static
--        /.$BeginJava
--                    setResult(Collections.singletonList(nf.FlagsNode(pos(), Flags.STATIC)));
--          $EndJava
--        ./
    
    ExtendsInterfaces ::= extends Type
        /.$BeginJava
                    List<TypeNode> l = new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false);
                    l.add(Type);
                    setResult(l);
          $EndJava
        ./
                        | ExtendsInterfaces , Type
        /.$BeginJava
                    ExtendsInterfaces.add(Type);
          $EndJava
        ./
    
    --
    -- See Chapter 4

    InterfaceBody ::= { InterfaceMemberDeclarationsopt }
        /.$BeginJava
                    setResult(nf.ClassBody(pos(), InterfaceMemberDeclarationsopt));
          $EndJava
        ./
    
    InterfaceMemberDeclarations ::= InterfaceMemberDeclaration
                                  | InterfaceMemberDeclarations InterfaceMemberDeclaration
        /.$BeginJava
                    InterfaceMemberDeclarations.addAll(InterfaceMemberDeclaration);
                    // setResult(l);
          $EndJava
        ./
    
    InterfaceMemberDeclaration ::= MethodDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(MethodDeclaration);
                    setResult(l);
          $EndJava
        ./
                                 | PropertyMethodDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(PropertyMethodDeclaration);
                    setResult(l);
          $EndJava
        ./
                                 | FieldDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.addAll(FieldDeclaration);
                    setResult(l);
          $EndJava
        ./
                                 | ClassDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(ClassDeclaration);
                    setResult(l);
          $EndJava
        ./
                                 | InterfaceDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(InterfaceDeclaration);
                    setResult(l);
          $EndJava
        ./
                                 | TypeDefDeclaration
        /.$BeginJava
                    List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
                    l.add(TypeDefDeclaration);
                    setResult(l);
          $EndJava
        ./
                                 | ;
        /.$BeginJava
                    setResult(Collections.<ClassMember>emptyList());
          $EndJava
        ./
    
    Annotations ::= Annotation
        /.$BeginJava
                    List<AnnotationNode> l = new TypedList<AnnotationNode>(new LinkedList<AnnotationNode>(), AnnotationNode.class, false);
                    l.add(Annotation);
                    setResult(l);
          $EndJava
        ./
                  | Annotations Annotation
        /.$BeginJava
                    Annotations.add(Annotation);
          $EndJava
        ./
    
    Annotation ::= @ NamedType
        /.$BeginJava
                    setResult(nf.AnnotationNode(pos(), NamedType));
          $EndJava
        ./
    
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    SimpleName ::= Identifier
--        /.$BeginJava
--                    setResult(new X10ParsedName(nf, ts, pos(), Identifier));
--          $EndJava
--        ./
        
    Identifier ::= identifier
        /.$BeginJava
                    setResult( nf.Id(identifier.getPosition(), identifier.getIdentifier()));
          $EndJava
        ./

    -- Chapter 10
    
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    VariableInitializers ::= VariableInitializer
--        /.$BeginJava
--                    List<Expr> l = new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false);
--                    l.add(VariableInitializer);
--                    setResult(l);
--          $EndJava
--        ./
--                           | VariableInitializers , VariableInitializer
--        /.$BeginJava
--                    VariableInitializers.add(VariableInitializer);
--                    //setResult(VariableInitializers);
--          $EndJava
--        ./
    
    --
    -- See Chapter 8
    
    -- Chapter 11
    
    -- Chapter 12
    
    -- Chapter 13
    
    -- Chapter 14
    
    Block ::= { BlockStatementsopt }
        /.$BeginJava
                    setResult(nf.Block(pos(), BlockStatementsopt));
          $EndJava
        ./
    
    BlockStatements ::= BlockStatement
        /.$BeginJava
                    List<Stmt> l = new TypedList<Stmt>(new LinkedList<Stmt>(), Stmt.class, false);
                    l.addAll(BlockStatement);
                    setResult(l);
          $EndJava
        ./
                      | BlockStatements BlockStatement
        /.$BeginJava
                    BlockStatements.addAll(BlockStatement);
                    //setResult(l);
          $EndJava
        ./
    
    BlockStatement ::= LocalVariableDeclarationStatement
                     | ClassDeclaration
        /.$BeginJava
                    List<Stmt> l = new TypedList<Stmt>(new LinkedList<Stmt>(), Stmt.class, false);
                    l.add(nf.LocalClassDecl(pos(), ClassDeclaration));
                    setResult(l);
          $EndJava
        ./
                     | TypeDefDeclaration
        /.$BeginJava
                    List<Stmt> l = new TypedList<Stmt>(new LinkedList<Stmt>(), Stmt.class, false);
                    l.add(nf.LocalTypeDef(pos(), TypeDefDeclaration));
                    setResult(l);
          $EndJava
        ./
                     | Statement
        /.$BeginJava
                    List<Stmt> l = new TypedList<Stmt>(new LinkedList<Stmt>(), Stmt.class, false);
                    l.add(Statement);
                    setResult(l);
          $EndJava
        ./
    
    IdentifierList ::= Identifier
        /.$BeginJava
                    List<Id> l = new TypedList<Id>(new LinkedList<Id>(), Id.class, false);
                    l.add(Identifier);
                    setResult(l);
          $EndJava
        ./
                     | IdentifierList , Identifier
        /.$BeginJava
                    IdentifierList.add(Identifier);
          $EndJava
        ./
                    
    FormalDeclarator ::= Identifier ResultType
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, Collections.<Id>emptyList(), null, ResultType, null });
          $EndJava
        ./
                         | '[' IdentifierList ']' ResultType
        /.$BeginJava
                    setResult(new Object[] { pos(), null, IdentifierList, null, ResultType, null });
          $EndJava
        ./
                         | Identifier '[' IdentifierList ']' ResultType
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, IdentifierList, null, ResultType, null });
          $EndJava
        ./
    
    FieldDeclarator ::= Identifier HasResultType
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, Collections.<Id>emptyList(), HasResultType, null });
          $EndJava
        ./
                         | Identifier HasResultTypeopt = VariableInitializer
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, Collections.<Id>emptyList(), HasResultTypeopt, VariableInitializer });
          $EndJava
        ./
                    
    VariableDeclarator ::= Identifier HasResultTypeopt = VariableInitializer
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, Collections.<Id>emptyList(), null, HasResultTypeopt, VariableInitializer });
          $EndJava
        ./
                         | '[' IdentifierList ']' HasResultTypeopt = VariableInitializer
        /.$BeginJava
                    setResult(new Object[] { pos(), null, IdentifierList, null, HasResultTypeopt, VariableInitializer });
          $EndJava
        ./
                         | Identifier '[' IdentifierList ']' HasResultTypeopt = VariableInitializer
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, IdentifierList, null, HasResultTypeopt, VariableInitializer });
          $EndJava
        ./
                    
    VariableDeclaratorWithType ::= Identifier HasResultType = VariableInitializer
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, Collections.<Id>emptyList(), null, HasResultType, VariableInitializer });
          $EndJava
        ./
                         | '[' IdentifierList ']' HasResultType = VariableInitializer
        /.$BeginJava
                    setResult(new Object[] { pos(), null, IdentifierList, null, HasResultType, VariableInitializer });
          $EndJava
        ./
                         | Identifier '[' IdentifierList ']' HasResultType = VariableInitializer
        /.$BeginJava
                    setResult(new Object[] { pos(), Identifier, IdentifierList, null, HasResultType, VariableInitializer });
          $EndJava
        ./
    
    LocalVariableDeclarationStatement ::= LocalVariableDeclaration ;
    
    LocalVariableDeclaration ::= Modifiersopt VarKeyword VariableDeclarators
        /.$BeginJava
                    List<Node> modifiers = checkVariableModifiers(Modifiersopt);
                    FlagsNode fn = extractFlags(modifiers, VarKeyword);
        
                    List<LocalDecl> l = new TypedList<LocalDecl>(new LinkedList<LocalDecl>(), LocalDecl.class, false);
                        for (Object[] o : VariableDeclarators)
                        {
                            Position pos = (Position) o[0];
                            Id name = (Id) o[1];
                            if (name == null) name = nf.Id(pos, Name.makeFresh());
                            List<Id> exploded = (List<Id>) o[2];
                            DepParameterExpr guard = (DepParameterExpr) o[3];
                            TypeNode type = (TypeNode) o[4];
                            if (type == null) type = nf.UnknownTypeNode(name != null ? name.position() : pos);
                            Expr init = (Expr) o[5];
                            LocalDecl ld = nf.LocalDecl(pos, fn,
                                               type, name, init);
                            ld = (LocalDecl) ((X10Ext) ld.ext()).annotations(extractAnnotations(modifiers));
                            int index = 0;
                            l.add(ld);
                            for (Id id : exploded) {
                                TypeNode tni = nf.UnknownTypeNode(id.position());
                                l.add(nf.LocalDecl(id.position(), fn, tni, id, init != null ? nf.ClosureCall(JPGPosition.COMPILER_GENERATED, nf.Local(JPGPosition.COMPILER_GENERATED, name),  Collections.<Expr>singletonList(nf.IntLit(JPGPosition.COMPILER_GENERATED, IntLit.INT, index))) : null));
                                index++;
                            }
                        }
                    setResult(l);
          $EndJava
        ./
                               | Modifiersopt VariableDeclaratorsWithType
        /.$BeginJava
                    List<Node> modifiers = checkVariableModifiers(Modifiersopt);
                    FlagsNode fn = extractFlags(modifiers, Flags.FINAL);
        
                    List<LocalDecl> l = new TypedList<LocalDecl>(new LinkedList<LocalDecl>(), LocalDecl.class, false);
                        for (Object[] o : VariableDeclaratorsWithType)
                        {
                            Position pos = (Position) o[0];
                            Id name = (Id) o[1];
                            if (name == null) name = nf.Id(pos, Name.makeFresh());
                            List<Id> exploded = (List<Id>) o[2];
                            DepParameterExpr guard = (DepParameterExpr) o[3];
                            TypeNode type = (TypeNode) o[4];
                            if (type == null) type = nf.UnknownTypeNode(name != null ? name.position() : pos);
                            Expr init = (Expr) o[5];
                            LocalDecl ld = nf.LocalDecl(pos, fn,
                                               type, name, init);
                            ld = (LocalDecl) ((X10Ext) ld.ext()).annotations(extractAnnotations(modifiers));
                            int index = 0;
                            l.add(ld);
                            for (Id id : exploded) {
                                // HACK: if the local is non-final, assume the type is point and the component is int
                                TypeNode tni = nf.UnknownTypeNode(id.position());
                                l.add(nf.LocalDecl(id.position(), fn, tni, id, init != null ? nf.ClosureCall(JPGPosition.COMPILER_GENERATED, nf.Local(JPGPosition.COMPILER_GENERATED, name),  Collections.<Expr>singletonList(nf.IntLit(JPGPosition.COMPILER_GENERATED, IntLit.INT, index))) : null));
                                index++;
                            }
                        }
                    setResult(l);
          $EndJava
        ./
                  | Modifiersopt VarKeyword FormalDeclarators
        /.$BeginJava
                    List<Node> modifiers = checkVariableModifiers(Modifiersopt);
                    FlagsNode fn = extractFlags(modifiers, VarKeyword);
        
                    List<LocalDecl> l = new TypedList<LocalDecl>(new LinkedList<LocalDecl>(), LocalDecl.class, false);
                        for (Object[] o : FormalDeclarators)
                        {
                            Position pos = (Position) o[0];
                            Id name = (Id) o[1];
                            if (name == null) name = nf.Id(pos, Name.makeFresh());
                            List<Id> exploded = (List<Id>) o[2];
                            DepParameterExpr guard = (DepParameterExpr) o[3];
                            TypeNode type = (TypeNode) o[4];
                                                        if (type == null) type = nf.UnknownTypeNode(name != null ? name.position() : pos);
                            Expr init = (Expr) o[5];
                            LocalDecl ld = nf.LocalDecl(pos, fn,
                                               type, name, init);
                            ld = (LocalDecl) ((X10Ext) ld.ext()).annotations(extractAnnotations(modifiers));
                            int index = 0;
                            l.add(ld);
                            for (Id id : exploded) {
                                // HACK: if the local is non-final, assume the type is point and the component is int
                                TypeNode tni = nf.UnknownTypeNode(id.position());
                                // todo: fixme: do this desugaring after type-checking, and remove this code duplication 
                                l.add(nf.LocalDecl(id.position(), fn, tni, id, init != null ? nf.ClosureCall(JPGPosition.COMPILER_GENERATED, nf.Local(JPGPosition.COMPILER_GENERATED, name),  Collections.<Expr>singletonList(nf.IntLit(JPGPosition.COMPILER_GENERATED, IntLit.INT, index))) : null));
                                index++;
                            }
                        }
                    setResult(l);
          $EndJava
        ./
    
    --
    -- See Chapter 8

    -- Chapter 15
    
    Primary ::= here
        /.$BeginJava
                    setResult(((NodeFactory) nf).Here(pos()));
          $EndJava
        ./
              | '[' ArgumentListopt ']'
        /.$BeginJava
                    Tuple tuple = nf.Tuple(pos(), ArgumentListopt);
                    setResult(tuple);
          $EndJava
        ./

              | Literal
              | self
        /.$BeginJava
                    setResult(nf.Self(pos()));
          $EndJava
        ./
              | this
        /.$BeginJava
                    setResult(nf.This(pos()));
          $EndJava
        ./
              | ClassName . this
        /.$BeginJava
                    setResult(nf.This(pos(), ClassName.toType()));
          $EndJava
        ./
              | ( Expression )
        /.$BeginJava
                    setResult(nf.ParExpr(pos(), Expression));
          $EndJava
        ./
              | ClassInstanceCreationExpression
              | FieldAccess
              | MethodInvocation
              | MethodSelection
              | OperatorFunction
                        
    OperatorFunction ::= TypeName . +
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.ADD, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . -
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn, nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.SUB, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . *
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,   nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.MUL, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . /
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,   nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.DIV, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . '%'
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,   nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.MOD, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . &
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,   nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.BIT_AND, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . '|'
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,   nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.BIT_OR, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . ^
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn, nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.BIT_XOR, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . <<
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.SHL, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . >>
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.SHR, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . >>>
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,   nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.USHR, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . <
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.CanonicalTypeNode(pos(), ts.Boolean());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.LT, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . <=
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.CanonicalTypeNode(pos(), ts.Boolean());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.LE, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . >=
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.CanonicalTypeNode(pos(), ts.Boolean());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn, nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.GE, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . >
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.CanonicalTypeNode(pos(), ts.Boolean());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.GT, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . ==
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.CanonicalTypeNode(pos(), ts.Boolean());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.EQ, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       | TypeName . !=
            /.$BeginJava
                    List<Formal> formals = new ArrayList<Formal>();
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "x")));
                    formals.add(nf.Formal(pos(), nf.FlagsNode(pos(), Flags.FINAL), TypeName.toType(), nf.Id(pos(), "y")));
                    TypeNode tn = nf.CanonicalTypeNode(pos(), ts.Boolean());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(), nf.Binary(pos(), nf.Local(pos(), nf.Id(pos(), "x")),
                                                               Binary.NE, nf.Local(pos(), nf.Id(pos(), "y"))), true))));
          $EndJava
        ./
                       

    Literal ::= IntegerLiteral$lit
        /.$BeginJava
                    polyglot.lex.LongLiteral a = int_lit(getRhsFirstTokenIndex($lit));
                    setResult(nf.IntLit(pos(), IntLit.INT, a.getValue().longValue()));
          $EndJava
        ./
              | LongLiteral$lit
        /.$BeginJava
                    polyglot.lex.LongLiteral a = long_lit(getRhsFirstTokenIndex($lit));
                    setResult(nf.IntLit(pos(), IntLit.LONG, a.getValue().longValue()));
          $EndJava
        ./
              | UnsignedIntegerLiteral$lit
        /.$BeginJava
                    polyglot.lex.LongLiteral a = uint_lit(getRhsFirstTokenIndex($lit));
                    setResult(nf.IntLit(pos(), X10IntLit_c.UINT, a.getValue().longValue()));
          $EndJava
        ./
              | UnsignedLongLiteral$lit
        /.$BeginJava
                    polyglot.lex.LongLiteral a = ulong_lit(getRhsFirstTokenIndex($lit));
                    setResult(nf.IntLit(pos(), X10IntLit_c.ULONG, a.getValue().longValue()));
          $EndJava
        ./
              | FloatingPointLiteral$lit
        /.$BeginJava
                    polyglot.lex.FloatLiteral a = float_lit(getRhsFirstTokenIndex($lit));
                    setResult(nf.FloatLit(pos(), FloatLit.FLOAT, a.getValue().floatValue()));
          $EndJava
        ./
              | DoubleLiteral$lit
        /.$BeginJava
                    polyglot.lex.DoubleLiteral a = double_lit(getRhsFirstTokenIndex($lit));
                    setResult(nf.FloatLit(pos(), FloatLit.DOUBLE, a.getValue().doubleValue()));
          $EndJava
        ./
              | BooleanLiteral
        /.$BeginJava
                    setResult(nf.BooleanLit(pos(), BooleanLiteral.getValue().booleanValue()));
          $EndJava
        ./
              | CharacterLiteral$lit
        /.$BeginJava
                    polyglot.lex.CharacterLiteral a = char_lit(getRhsFirstTokenIndex($lit));
                    setResult(nf.CharLit(pos(), a.getValue().charValue()));
          $EndJava
        ./
              | StringLiteral$str
        /.$BeginJava
                    polyglot.lex.StringLiteral a = string_lit(getRhsFirstTokenIndex($str));
                    setResult(nf.StringLit(pos(), a.getValue()));
          $EndJava
        ./
              | null
        /.$BeginJava
                    setResult(nf.NullLit(pos()));
          $EndJava
        ./

    BooleanLiteral ::= true$trueLiteral
        /.$BeginJava
                    setResult(boolean_lit(getRhsFirstTokenIndex($trueLiteral)));
          $EndJava
        ./
                     | false$falseLiteral
        /.$BeginJava
                    setResult(boolean_lit(getRhsFirstTokenIndex($falseLiteral)));
          $EndJava
        ./

    --
    -- The following case appeared to be missing from the spec:
    --
    ArgumentList ::= Expression
        /.$BeginJava
                    List<Expr> l = new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false);
                    l.add(Expression);
                    setResult(l);
          $EndJava
        ./
                   | ArgumentList , Expression
        /.$BeginJava
                    ArgumentList.add(Expression);
          $EndJava
        ./

    FieldAccess ::= Primary . Identifier
        /.$BeginJava
                    setResult(nf.Field(pos(), Primary, Identifier));
          $EndJava
        ./
                  | super . Identifier
        /.$BeginJava
                    setResult(nf.Field(pos(), nf.Super(pos(getLeftSpan())), Identifier));
          $EndJava
        ./
                  | ClassName . super$sup . Identifier
        /.$BeginJava
                    setResult(nf.Field(pos(), nf.Super(pos(getLeftSpan(),getRhsFirstTokenIndex($sup)), ClassName.toType()), Identifier));
          $EndJava
        ./
                  | Primary . class$c
        /.$BeginJava
                    setResult(nf.Field(pos(), Primary, nf.Id(pos(getRhsFirstTokenIndex($c)), "class")));
          $EndJava
        ./
                  | super . class$c
        /.$BeginJava
                    setResult(nf.Field(pos(), nf.Super(pos(getLeftSpan())), nf.Id(pos(getRhsFirstTokenIndex($c)), "class")));
          $EndJava
        ./
                  | ClassName . super$sup . class$c
        /.$BeginJava
                    setResult(nf.Field(pos(), nf.Super(pos(getLeftSpan(),getRhsFirstTokenIndex($sup)), ClassName.toType()), nf.Id(pos(getRhsFirstTokenIndex($c)), "class")));
          $EndJava
        ./
    
    MethodInvocation ::= MethodName TypeArgumentsopt ( ArgumentListopt )
        /.$BeginJava
                    setResult(nf.X10Call(pos(), MethodName.prefix == null
                                                                 ? null
                                                                 : MethodName.prefix.toReceiver(), MethodName.name, TypeArgumentsopt, ArgumentListopt));
          $EndJava
        ./
                       | Primary . Identifier TypeArgumentsopt ( ArgumentListopt )
        /.$BeginJava
                    setResult(nf.X10Call(pos(), Primary, Identifier, TypeArgumentsopt, ArgumentListopt));
          $EndJava
        ./
                       | super . Identifier TypeArgumentsopt ( ArgumentListopt )
        /.$BeginJava
                    setResult(nf.X10Call(pos(), nf.Super(pos(getLeftSpan())), Identifier, TypeArgumentsopt, ArgumentListopt));
          $EndJava
        ./
                       | ClassName . super$sup . Identifier TypeArgumentsopt ( ArgumentListopt )
        /.$BeginJava
                    setResult(nf.X10Call(pos(), nf.Super(pos(getRhsFirstTokenIndex($sup)), ClassName.toType()), Identifier, TypeArgumentsopt, ArgumentListopt));
          $EndJava
        ./
                       | Primary TypeArgumentsopt ( ArgumentListopt )
        /.$BeginJava
                    if (Primary instanceof Field) {
                        Field f = (Field) Primary;
                        setResult(nf.X10Call(pos(), f.target(), f.name(), TypeArgumentsopt, ArgumentListopt));
                    }
                    else if (Primary instanceof AmbExpr) {
                        AmbExpr f = (AmbExpr) Primary;
                        setResult(nf.X10Call(pos(), null, f.name(), TypeArgumentsopt, ArgumentListopt));
                    }
                    else if (Primary instanceof Here) {
                        Here f = (Here) Primary;
                        setResult(nf.X10Call(pos(), null, nf.Id(Primary.position(), Name.make("here")), TypeArgumentsopt, ArgumentListopt));
                    }
                    else {
                        setResult(nf.ClosureCall(pos(), Primary, TypeArgumentsopt, ArgumentListopt));
                    }
          $EndJava
        ./
        
    MethodSelection ::= MethodName .  ( FormalParameterListopt )
        /.$BeginJava
//                    List<TypeNode> typeArgs = toTypeArgs(TypeParametersopt);
//                    List<TypeParamNode> typeParams = toTypeParams(TypeParametersopt);
                    List<Formal> formals = toFormals(FormalParameterListopt);
                    List<Expr> actuals = toActuals(FormalParameterListopt);
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(), formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(), nf.X10Call(pos(),
                                                                 MethodName.prefix == null ? null : MethodName.prefix.toReceiver(),
                                                                 MethodName.name, Collections.<TypeNode>emptyList(), actuals), true))));
          $EndJava
        ./
                       | Primary . Identifier .  ( FormalParameterListopt )
        /.$BeginJava
//                    List<TypeNode> typeArgs = toTypeArgs(TypeParametersopt);
//                    List<TypeParamNode> typeParams = toTypeParams(TypeParametersopt);
                    List<Formal> formals = toFormals(FormalParameterListopt);
                    List<Expr> actuals = toActuals(FormalParameterListopt);
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(), formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(),
                                                   nf.X10Call(pos(), Primary, Identifier, Collections.<TypeNode>emptyList(), actuals), true))));
          $EndJava
        ./
                       | super . Identifier .  ( FormalParameterListopt )
        /.$BeginJava
//                    List<TypeNode> typeArgs = toTypeArgs(TypeParametersopt);
//                    List<TypeParamNode> typeParams = toTypeParams(TypeParametersopt);
                    List<Formal> formals = toFormals(FormalParameterListopt);
                    List<Expr> actuals = toActuals(FormalParameterListopt);
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(),
                                                   nf.X10Call(pos(), nf.Super(pos(getLeftSpan())), Identifier,
                                                         Collections.<TypeNode>emptyList(),    actuals), true))));
          $EndJava
        ./
                       | ClassName . super$sup . Identifier .  ( FormalParameterListopt )
        /.$BeginJava
//                    List<TypeNode> typeArgs = toTypeArgs(TypeParametersopt);
//                    List<TypeParamNode> typeParams = toTypeParams(TypeParametersopt);
                    List<Formal> formals = toFormals(FormalParameterListopt);
                    List<Expr> actuals = toActuals(FormalParameterListopt);
                    TypeNode tn = nf.UnknownTypeNode(pos());
                    setResult(nf.Closure(pos(),  formals, (DepParameterExpr) null, tn,  nf.Block(pos(),
                                         nf.X10Return(pos(),
                                                   nf.X10Call(pos(), nf.Super(pos(getRhsFirstTokenIndex($sup)), ClassName.toType()), Identifier, 
                                                              Collections.<TypeNode>emptyList(), actuals), true))));
          $EndJava
        ./

    PostfixExpression ::= CastExpression
                        | PostIncrementExpression
                        | PostDecrementExpression
    
    PostIncrementExpression ::= PostfixExpression ++
        /.$BeginJava
                    setResult(nf.Unary(pos(), PostfixExpression, Unary.POST_INC));
          $EndJava
        ./
    
    PostDecrementExpression ::= PostfixExpression '--'
        /.$BeginJava
                    setResult(nf.Unary(pos(), PostfixExpression, Unary.POST_DEC));
          $EndJava
        ./
    
    UnannotatedUnaryExpression ::= PreIncrementExpression
                      | PreDecrementExpression
                      | + UnaryExpressionNotPlusMinus
        /.$BeginJava
                    setResult(nf.Unary(pos(), Unary.POS, UnaryExpressionNotPlusMinus));
          $EndJava
        ./
                      | - UnaryExpressionNotPlusMinus
        /.$BeginJava
                    setResult(nf.Unary(pos(), Unary.NEG, UnaryExpressionNotPlusMinus));
          $EndJava
        ./
                      | UnaryExpressionNotPlusMinus

    UnaryExpression ::= UnannotatedUnaryExpression
                      | Annotations UnannotatedUnaryExpression
        /.$BeginJava
                    Expr e = UnannotatedUnaryExpression;
                    e = (Expr) ((X10Ext) e.ext()).annotations(Annotations);
                    setResult(e.position(pos()));
          $EndJava
        ./
    
    PreIncrementExpression ::= ++ UnaryExpressionNotPlusMinus
        /.$BeginJava
                    setResult(nf.Unary(pos(), Unary.PRE_INC, UnaryExpressionNotPlusMinus));
          $EndJava
        ./
    
    PreDecrementExpression ::= '--' UnaryExpressionNotPlusMinus
        /.$BeginJava
                    setResult(nf.Unary(pos(), Unary.PRE_DEC, UnaryExpressionNotPlusMinus));
          $EndJava
        ./
    
    UnaryExpressionNotPlusMinus ::= PostfixExpression
                                  | ~ UnaryExpression
        /.$BeginJava
                    setResult(nf.Unary(pos(), Unary.BIT_NOT, UnaryExpression));
          $EndJava
        ./
                                  | ! UnaryExpression
        /.$BeginJava
                    setResult(nf.Unary(pos(), Unary.NOT, UnaryExpression));
          $EndJava
        ./
    
    MultiplicativeExpression ::= UnaryExpression
                               | MultiplicativeExpression * UnaryExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), MultiplicativeExpression, Binary.MUL, UnaryExpression));
          $EndJava
        ./
                               | MultiplicativeExpression / UnaryExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), MultiplicativeExpression, Binary.DIV, UnaryExpression));
          $EndJava
        ./
                               | MultiplicativeExpression '%' UnaryExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), MultiplicativeExpression, Binary.MOD, UnaryExpression));
          $EndJava
        ./
    
    AdditiveExpression ::= MultiplicativeExpression
                         | AdditiveExpression + MultiplicativeExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), AdditiveExpression, Binary.ADD, MultiplicativeExpression));
          $EndJava
        ./
                         | AdditiveExpression - MultiplicativeExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), AdditiveExpression, Binary.SUB, MultiplicativeExpression));
          $EndJava
        ./
    
    ShiftExpression ::= AdditiveExpression
                      | ShiftExpression << AdditiveExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), ShiftExpression, Binary.SHL, AdditiveExpression));
          $EndJava
        ./
                      | ShiftExpression >> AdditiveExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), ShiftExpression, Binary.SHR, AdditiveExpression));
          $EndJava
        ./
                      | ShiftExpression >>> AdditiveExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), ShiftExpression, Binary.USHR, AdditiveExpression));
          $EndJava
        ./
    
    RangeExpression ::= ShiftExpression
                      | ShiftExpression$expr1 .. ShiftExpression$expr2
        /.$BeginJava
                    Expr regionCall = nf.RegionMaker(pos(), expr1, expr2);
                    setResult(regionCall);
          $EndJava
        ./
    
    RelationalExpression ::= RangeExpression
                           | HasZeroConstraint
                           | SubtypeConstraint
                           | RelationalExpression < RangeExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), RelationalExpression, Binary.LT, RangeExpression));
          $EndJava
        ./
                           | RelationalExpression > RangeExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), RelationalExpression, Binary.GT, RangeExpression));
          $EndJava
        ./
                           | RelationalExpression <= RangeExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), RelationalExpression, Binary.LE, RangeExpression));
          $EndJava
        ./
                           | RelationalExpression >= RangeExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), RelationalExpression, Binary.GE, RangeExpression));
          $EndJava
        ./
                           | RelationalExpression instanceof Type
        /.$BeginJava
                    setResult(nf.Instanceof(pos(), RelationalExpression, Type));
          $EndJava
        ./
                           | RelationalExpression in ShiftExpression
        /.$BeginJava
                    setResult(nf.Contains(pos(), RelationalExpression, ShiftExpression));
          $EndJava
        ./
    
    EqualityExpression ::= RelationalExpression
                         | EqualityExpression == RelationalExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), EqualityExpression, Binary.EQ, RelationalExpression));
          $EndJava
        ./
                         | EqualityExpression != RelationalExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), EqualityExpression, Binary.NE, RelationalExpression));
          $EndJava
        ./
                         | Type$t1 == Type$t2
        /.$BeginJava
                    setResult(nf.SubtypeTest(pos(), t1, t2, true));
          $EndJava
        ./
    
    AndExpression ::= EqualityExpression
                    | AndExpression & EqualityExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), AndExpression, Binary.BIT_AND, EqualityExpression));
          $EndJava
        ./
    
    ExclusiveOrExpression ::= AndExpression
                            | ExclusiveOrExpression ^ AndExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), ExclusiveOrExpression, Binary.BIT_XOR, AndExpression));
          $EndJava
        ./
    
    InclusiveOrExpression ::= ExclusiveOrExpression
                            | InclusiveOrExpression '|' ExclusiveOrExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), InclusiveOrExpression, Binary.BIT_OR, ExclusiveOrExpression));
          $EndJava
        ./
    
    ConditionalAndExpression ::= InclusiveOrExpression
                               | ConditionalAndExpression && InclusiveOrExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), ConditionalAndExpression, Binary.COND_AND, InclusiveOrExpression));
          $EndJava
        ./
    
    ConditionalOrExpression ::= ConditionalAndExpression
                              | ConditionalOrExpression || ConditionalAndExpression
        /.$BeginJava
                    setResult(nf.Binary(pos(), ConditionalOrExpression, Binary.COND_OR, ConditionalAndExpression));
          $EndJava
        ./
    
    
    ConditionalExpression ::= ConditionalOrExpression
                            | ClosureExpression
                            | AtExpression
                            | FinishExpression
                            | ConditionalOrExpression ? Expression : ConditionalExpression
        /.$BeginJava
                    setResult(nf.Conditional(pos(), ConditionalOrExpression, Expression, ConditionalExpression));
          $EndJava
        ./
    
    AssignmentExpression ::= Assignment
                           | ConditionalExpression
    
    Assignment ::= LeftHandSide AssignmentOperator AssignmentExpression
        /.$BeginJava
                    setResult(nf.Assign(pos(), LeftHandSide, AssignmentOperator, AssignmentExpression));
          $EndJava
        ./
                 | ExpressionName$e1 ( ArgumentListopt ) AssignmentOperator AssignmentExpression
        /.$BeginJava
                    setResult(nf.SettableAssign(pos(), e1.toExpr(), ArgumentListopt, AssignmentOperator, AssignmentExpression));
          $EndJava
        ./
                 | Primary$e1 ( ArgumentListopt ) AssignmentOperator AssignmentExpression
        /.$BeginJava
                    setResult(nf.SettableAssign(pos(), e1, ArgumentListopt, AssignmentOperator, AssignmentExpression));
          $EndJava
        ./
    
    LeftHandSide ::= ExpressionName
        /.$BeginJava
                    setResult(ExpressionName.toExpr());
          $EndJava
        ./
                   | FieldAccess
    
    AssignmentOperator ::= =
        /.$BeginJava
                    setResult(Assign.ASSIGN);
          $EndJava
        ./
                         | *=
        /.$BeginJava
                    setResult(Assign.MUL_ASSIGN);
          $EndJava
        ./
                         | /=
        /.$BeginJava
                    setResult(Assign.DIV_ASSIGN);
          $EndJava
        ./
                         | '%='
        /.$BeginJava
                    setResult(Assign.MOD_ASSIGN);
          $EndJava
        ./
                         | +=
        /.$BeginJava
                    setResult(Assign.ADD_ASSIGN);
          $EndJava
        ./
                         | -=
        /.$BeginJava
                    setResult(Assign.SUB_ASSIGN);
          $EndJava
        ./
                         | <<=
        /.$BeginJava
                    setResult(Assign.SHL_ASSIGN);
          $EndJava
        ./
                         | >>=
        /.$BeginJava
                    setResult(Assign.SHR_ASSIGN);
          $EndJava
        ./
                         | >>>=
        /.$BeginJava
                    setResult(Assign.USHR_ASSIGN);
          $EndJava
        ./
                         | &=
        /.$BeginJava
                    setResult(Assign.BIT_AND_ASSIGN);
          $EndJava
        ./
                         | ^=
        /.$BeginJava
                    setResult(Assign.BIT_XOR_ASSIGN);
          $EndJava
        ./
                         | |=
        /.$BeginJava
                    setResult(Assign.BIT_OR_ASSIGN);
          $EndJava
        ./
    
    Expression ::= AssignmentExpression
    
    ConstantExpression ::= Expression


    PrefixOp ::= +
        /.$BeginJava
                    setResult(Unary.POS);
          $EndJava
        ./
      | -
        /.$BeginJava
                    setResult(Unary.NEG);
          $EndJava
        ./
      | !
        /.$BeginJava
                    setResult(Unary.NOT);
          $EndJava
        ./
      | ~
        /.$BeginJava
                    setResult(Unary.BIT_NOT);
          $EndJava
        ./
        
    BinOp ::= +
        /.$BeginJava
                    setResult(Binary.ADD);
          $EndJava
        ./
      | -
        /.$BeginJava
                    setResult(Binary.SUB);
          $EndJava
        ./
      | *
        /.$BeginJava
                    setResult(Binary.MUL);
          $EndJava
        ./
      | /
        /.$BeginJava
                    setResult(Binary.DIV);
          $EndJava
        ./
      | '%'
        /.$BeginJava
                    setResult(Binary.MOD);
          $EndJava
        ./
      | &
        /.$BeginJava
                    setResult(Binary.BIT_AND);
          $EndJava
        ./
      | '|'
        /.$BeginJava
                    setResult(Binary.BIT_OR);
          $EndJava
        ./
      | ^
        /.$BeginJava
                    setResult(Binary.BIT_XOR);
          $EndJava
        ./
      | &&
        /.$BeginJava
                    setResult(Binary.COND_AND);
          $EndJava
        ./
      | '||'
        /.$BeginJava
                    setResult(Binary.COND_OR);
          $EndJava
        ./
      | <<
        /.$BeginJava
                    setResult(Binary.SHL);
          $EndJava
        ./
      | >>
        /.$BeginJava
                    setResult(Binary.SHR);
          $EndJava
        ./
      | >>>
        /.$BeginJava
                    setResult(Binary.USHR);
          $EndJava
        ./
      | >=
        /.$BeginJava
                    setResult(Binary.GE);
          $EndJava
        ./
      | <=
        /.$BeginJava
                    setResult(Binary.LE);
          $EndJava
        ./
      | >
        /.$BeginJava
                    setResult(Binary.GT);
          $EndJava
        ./
      | <
        /.$BeginJava
                    setResult(Binary.LT);
          $EndJava
        ./
        
-- FIXME: == and != shouldn't be allowed to be overridden.
              
      | ==
        /.$BeginJava
                    setResult(Binary.EQ);
          $EndJava
        ./
      | !=
        /.$BeginJava
                    setResult(Binary.NE);
          $EndJava
        ./
            
    --
    -- Optional rules
    --
    Catchesopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<Catch>(new LinkedList<Catch>(), Catch.class, false));
          $EndJava
        ./
                 | Catches

    Identifieropt ::= %Empty
        /.$NullAction./
                    | Identifier
        /.$BeginJava
                    setResult(Identifier);
          $EndJava
        ./

    ForUpdateopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<ForUpdate>(new LinkedList<ForUpdate>(), ForUpdate.class, false));
          $EndJava
        ./
                   | ForUpdate

    Expressionopt ::= %Empty
        /.$NullAction./
                    | Expression

    ForInitopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<ForInit>(new LinkedList<ForInit>(), ForInit.class, false));
          $EndJava
        ./
                 | ForInit

    SwitchLabelsopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<Case>(new LinkedList<Case>(), Case.class, false));
          $EndJava
        ./
                      | SwitchLabels

    SwitchBlockStatementGroupsopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<SwitchElement>(new LinkedList<SwitchElement>(), SwitchElement.class, false));
          $EndJava
        ./
                                    | SwitchBlockStatementGroups

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    VariableModifiersopt ::= %Empty
--        /.$BeginJava
--                    setResult(Collections.<Node>emptyList());
--          $EndJava
--        ./
--                           | VariableModifiers

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    VariableInitializersopt ::= %Empty
--        /.$NullAction./
--                              | VariableInitializers

    InterfaceMemberDeclarationsopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false));
          $EndJava
        ./
                                     | InterfaceMemberDeclarations

    ExtendsInterfacesopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false));
          $EndJava
        ./
                           | ExtendsInterfaces

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    InterfaceModifiersopt ::= %Empty
--        /.$BeginJava
--                    setResult(Collections.<Node>emptyList());
--          $EndJava
--        ./
--                            | InterfaceModifiers

    ClassBodyopt ::= %Empty
        /.$NullAction./
                   | ClassBody

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    Argumentsopt ::= %Empty
--        /.$BeginJava
--                    setResult(new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false));
--          $EndJava
--        ./
--                   | Arguments

    ArgumentListopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false));
          $EndJava
        ./
                      | ArgumentList

    BlockStatementsopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<Stmt>(new LinkedList<Stmt>(), Stmt.class, false));
          $EndJava
        ./
                         | BlockStatements

    ExplicitConstructorInvocationopt ::= %Empty
        /.$NullAction./
                                       | ExplicitConstructorInvocation

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    ConstructorModifiersopt ::= %Empty
--        /.$BeginJava
--                    setResult(Collections.<Node>emptyList());
--          $EndJava
--        ./
--                              | ConstructorModifiers

    FormalParameterListopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<Formal>(new LinkedList<Formal>(), Formal.class, false));
          $EndJava
        ./
                             | FormalParameterList

--    Throwsopt ::= %Empty
--        /.$BeginJava
--                    setResult(new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false));
--          $EndJava
--        ./
--                | Throws
     Offersopt ::= %Empty
        /.$BeginJava
                    setResult(null);
          $EndJava
        ./
                | Offers

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    MethodModifiersopt ::= %Empty
--        /.$BeginJava
--                    setResult(Collections.<Node>emptyList());
--          $EndJava
--        ./
--                         | MethodModifiers

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    TypeModifieropt ::= %Empty
--        /.$BeginJava
--                    setResult(Collections.<Node>emptyList());
--          $EndJava
--        ./
--                         | TypeModifier

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    FieldModifiersopt ::= %Empty
--        /.$BeginJava
--                    setResult(Collections.<Node>emptyList());
--          $EndJava
--        ./
--                        | FieldModifiers

    ClassBodyDeclarationsopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false));
          $EndJava
        ./
                               | ClassBodyDeclarations

    Interfacesopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false));
          $EndJava
        ./
                    | Interfaces

    Superopt ::= %Empty
        /.$NullAction./
               | Super

    TypeParametersopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<TypeParamNode>(new LinkedList<TypeParamNode>(), TypeParamNode.class, false));
          $EndJava
        ./
                        | TypeParameters
                        
    FormalParametersopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<Formal>(new LinkedList<Formal>(), Formal.class, false));
          $EndJava
        ./
                        | FormalParameters

    Annotationsopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<AnnotationNode>(new LinkedList<AnnotationNode>(), AnnotationNode.class, false));
          $EndJava
        ./
                     | Annotations

    TypeDeclarationsopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<TopLevelDecl>(new LinkedList<TopLevelDecl>(), TopLevelDecl.class, false));
          $EndJava
        ./
                          | TypeDeclarations

    ImportDeclarationsopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<Import>(new LinkedList<Import>(), Import.class, false));
          $EndJava
        ./
                            | ImportDeclarations

    PackageDeclarationopt ::= %Empty
        /.$NullAction./
                            | PackageDeclaration
                            
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    ResultTypeopt ::= %Empty
--        /.$NullAction./
--                            | ResultType
    HasResultTypeopt ::= %Empty
        /.$NullAction./
                            | HasResultType
        
    TypeArgumentsopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false));
          $EndJava
        ./
                       | TypeArguments
        
    TypeParamsWithVarianceopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<TypeParamNode>(new LinkedList<TypeParamNode>(), TypeParamNode.class, false));
          $EndJava
        ./
                       | TypeParamsWithVariance

    Propertiesopt ::= %Empty
        /.$BeginJava
                    setResult(new TypedList<PropertyDecl>(new LinkedList<PropertyDecl>(), PropertyDecl.class, false));
          $EndJava
        ./
                       | Properties
%End

%Types
    Expr ::= PlaceType
    SourceFile ::= CompilationUnit
    polyglot.ast.Lit ::= Literal
    TypeNode ::= Type
    TypeNode ::= AnnotatedType
    TypeNode ::= SimpleNamedType
    TypeNode ::= DepNamedType
    TypeNode ::= NamedType
    TypeNode ::= ClassType
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    TypeNode ::= InterfaceType
    TypeNode ::= FunctionType
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    ParsedName ::= SimpleName
    PackageNode ::= PackageDeclarationopt | PackageDeclaration
    List<Import> ::= ImportDeclarationsopt | ImportDeclarations
    List<TopLevelDecl> ::= TypeDeclarationsopt | TypeDeclarations
    Import ::= ImportDeclaration
    Import ::= SingleTypeImportDeclaration
    Import ::= TypeImportOnDemandDeclaration
    TopLevelDecl ::= TypeDeclaration
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    List<Node> ::= ClassModifier
--            | ClassModifiers
--            | ClassModifiersopt
--    List<Node> ::= ConstructorModifier
--            | ConstructorModifiers
--            | ConstructorModifiersopt
--    List<Node> ::= FieldModifier
--            | FieldModifiers
--            | FieldModifiersopt
--    List<Node> ::= InterfaceModifier
--            | InterfaceModifiers
--            | InterfaceModifiersopt
--    List<Node> ::= MethodModifier
--            | MethodModifiers
--            | MethodModifiersopt
--    List<Node> ::= VariableModifier
--            | VariableModifiers
--            | VariableModifiersopt
--    List<Node> ::= TypeDefModifier
--           | TypeDefModifiers
--           | TypeDefModifiersopt
    Modifier ::= Modifier
    List<Modifier> ::= Modifiersopt | MethodModifiersopt
    ClassDecl ::= ClassDeclaration | NormalClassDeclaration
    TypeNode ::= Super | Superopt
    List<TypeNode> ::= Interfaces | Interfacesopt | InterfaceTypeList
    ClassBody ::= ClassBody | ClassBodyopt
    List<ClassMember> ::= ClassBodyDeclarations | ClassBodyDeclarationsopt
    List<ClassMember> ::= ClassBodyDeclaration | ClassMemberDeclaration
    List<ClassMember> ::= FieldDeclaration
    'List<Object[]>' ::= VariableDeclarators | FormalDeclarators | FieldDeclarators
    'List<Object[]>' ::= VariableDeclaratorsWithType
    'Object[]' ::= VariableDeclarator
    'Object[]' ::= VariableDeclaratorWithType
    'Object[]' ::= FormalDeclarator
    'Object[]' ::= LoopIndexDeclarator
    'Object[]' ::= FieldDeclarator
    Expr ::= VariableInitializer
    ClassMember ::= MethodDeclaration 
--
-- I do not think this is needed.  Review later...
--
    ClassMember ::= PropertyMethodDeclaration 
    List<Formal> ::= FormalParameterListopt | FormalParameterList 
    List<Formal> ::= FormalParametersopt | FormalParameters 
    List<Formal> ::= ExistentialListopt | ExistentialList 
    X10Formal ::= FormalParameter
    X10Formal ::= LoopIndex
    Stmt ::= LoopStatement
--    List<TypeNode> ::= Throwsopt | Throws
    TypeNode ::= Offersopt | Offers
    Block ::= MethodBody
--    Initializer ::= StaticInitializer
    ConstructorDecl ::= ConstructorDeclaration
    Block ::= ConstructorBody
    Block ::= ConstructorBlock
    ConstructorCall ::= ExplicitConstructorInvocation
    ClassDecl ::= InterfaceDeclaration | NormalInterfaceDeclaration
    List<TypeNode> ::= ExtendsInterfacesopt | ExtendsInterfaces
    ClassBody ::= InterfaceBody
    List<ClassMember> ::= InterfaceMemberDeclarationsopt | InterfaceMemberDeclarations
    List<ClassMember> ::= InterfaceMemberDeclaration
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    List<Expr> ::= VariableInitializers
    Block ::= Block
    List<Stmt> ::= BlockStatementsopt | BlockStatements
    List<Stmt> ::= BlockStatement
    List<LocalDecl> ::= LocalVariableDeclarationStatement
    List<LocalDecl> ::= LocalVariableDeclaration
    Stmt ::= Statement
    Stmt ::= NonExpressionStatement
    Empty ::= EmptyStatement
    Labeled ::= LabeledStatement
    Stmt ::= ExpressionStatement
    Expr ::= StatementExpression
    Offer ::= OfferStatement
    If ::= IfThenStatement
    If ::= IfThenElseStatement
    Switch ::= SwitchStatement
    List<SwitchElement> ::= SwitchBlock | SwitchBlockStatementGroups
    List<SwitchElement> ::= SwitchBlockStatementGroup | SwitchLabels
    Case ::= SwitchLabel
    Expr ::= ConstantExpression
    While ::= WhileStatement
    Do ::= DoStatement
    For ::= ForStatement
    Stmt ::= AnnotationStatement
    List<ForInit> ::= ForInitopt | ForInit
    List<ForUpdate> ::= ForUpdateopt | ForUpdate
    List<Eval> ::= StatementExpressionList
    polyglot.lex.Identifier ::= identifier
    Id ::= Identifieropt
    Branch ::= BreakStatement | ContinueStatement
    Return ::= ReturnStatement
    Throw ::= ThrowStatement
    Try ::= TryStatement
    List<Catch> ::= Catchesopt | Catches
    Catch ::= CatchClause
    Block ::= Finally
    Assert ::= AssertStatement
    Expr ::= Primary
    Expr ::= OperatorFunction
    Expr ::= ClassInstanceCreationExpression
    List<Expr> ::= ArgumentListopt | ArgumentList
    Field ::= FieldAccess 
    Call ::= MethodInvocation
    Expr ::= PostfixExpression
    Unary ::= PostIncrementExpression | PostDecrementExpression
    Expr ::= UnaryExpression | UnaryExpressionNotPlusMinus | UnannotatedUnaryExpression
    Unary ::= PreIncrementExpression | PreDecrementExpression
    Expr ::= CastExpression
    Expr ::= MultiplicativeExpression | AdditiveExpression
    Expr ::= ShiftExpression | RelationalExpression | EqualityExpression
    Expr ::= AndExpression | ExclusiveOrExpression | InclusiveOrExpression
    Expr ::= ConditionalAndExpression | ConditionalOrExpression
    Expr ::= ConditionalExpression | AssignmentExpression | FinishExpression
    Expr ::= Assignment
    Expr ::= LeftHandSide
    Assign.Operator ::= AssignmentOperator
    Expr ::= Expressionopt | Expression

    ParsedName ::= TypeName
    ParsedName ::= ClassName
    ParsedName ::= PackageName
    ParsedName ::= ExpressionName
    ParsedName ::= AmbiguousName
    ParsedName ::= MethodName
    ParsedName ::= PackageOrTypeName
--    Initializer ::= InstanceInitializer
    TypeNode ::= ResultType
    TypeNode ::= HasResultType
    List<TypeNode> ::= ExceptionTypeList
    TypeNode ::= ExceptionType
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    ParsedName ::= SimpleTypeName
    Stmt ::= ExplicitConstructorInvocationopt
    List<Expr> ::= Arguments
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--           | Argumentsopt

--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    List<Expr> ::= VariableInitializersopt
    List<Stmt> ::= SwitchBlockStatementGroupsopt
    List<Case> ::= SwitchLabelsopt
    For ::= BasicForStatement
    For ::= EnhancedForStatement
    polyglot.lex.BooleanLiteral ::= BooleanLiteral
    TypeNode ::= ConstrainedType
    Expr ::= PlaceExpression
    DepParameterExpr ::= WhereClauseopt
    DepParameterExpr ::= WhereClause
    ClassDecl ::= StructDeclaration
    Async ::= AsyncStatement
    AtStmt ::= AtStatement
    AtExpr ::= AtExpression
    Atomic ::= AtomicStatement
    When ::= WhenStatement
--    ForEach ::= ForEachStatement
    AtEach ::= AtEachStatement
    Finish ::= FinishStatement
    Next ::= NextStatement
    Resume ::= ResumeStatement
--    Await ::= AwaitStatement
    Expr ::= Clock
    List<Expr> ::= ClockList
           | ClockedClause
           | ClockedClauseopt
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    Expr ::= RegionExpression
--    List<Expr> ::= RegionExpressionList
    Expr ::= PlaceExpressionSingleListopt
           | PlaceExpressionSingleList
    Stmt ::= AssignPropertyCall
    DepParameterExpr ::= DepParameters
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--                     | DepParametersopt

    List<PropertyDecl> ::= Properties | Propertiesopt 
    List<PropertyDecl> ::=  PropertyList 
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--         | PropertyListopt
    PropertyDecl ::= Property
    List<AnnotationNode> ::= Annotations | Annotationsopt
    AnnotationNode ::= Annotation
    Block ::= ClosureBody
    Stmt ::= LastExpression
    List<Expr> ::= Conjunction | Conjunctionopt
    Expr ::= ClosureExpression
    List<TypeNode> ::=  TypeArguments | TypeArgumentsopt
    TypeParamNode ::= TypeParameter
    List<TypeParamNode> ::=  TypeParameterList
    List<TypeParamNode> ::=  TypeParameters | TypeParametersopt
    TypeParamNode ::= TypeParamWithVariance
--
-- This is a useless nonterminal that is not used anywhere else in the grammar.
--
--    TypeNode ::= ResultTypeopt
    TypeNode ::= HasResultTypeopt
    List<TypeParamNode> ::=  TypeParamWithVarianceList
    List<TypeParamNode> ::= TypeParamsWithVariance | TypeParamsWithVarianceopt
    List<TypeNode> ::=  TypeArgumentList
    Id ::= Identifier
    List<Id> ::= IdentifierList
    List<FlagsNode> ::= VarKeyword | FieldKeyword
    Expr ::= MethodSelection
    Expr ::= SubtypeConstraint
    Expr ::= HasZeroConstraint  
    Expr ::= RangeExpression
    TypeDecl ::= TypeDefDeclaration
    Binary.Operator ::= BinOp
    Unary.Operator ::= PrefixOp
%End
