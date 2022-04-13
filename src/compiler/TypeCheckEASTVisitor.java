package compiler;

import compiler.AST.*;
import compiler.exc.IncomplException;
import compiler.exc.TypeException;
import compiler.lib.BaseEASTVisitor;
import compiler.lib.Node;
import compiler.lib.TypeNode;

import static compiler.TypeRels.*;

//visitNode(n) fa il type checking di un Node n e ritorna:
//- per una espressione, il suo tipo (oggetto BoolTypeNode o IntTypeNode)
//- per una dichiarazione, "null"; controlla la correttezza interna della dichiarazione
//(- per un tipo: "null"; controlla che il tipo non sia incompleto) 
//
//visitSTentry(s) ritorna, per una STentry s, il tipo contenuto al suo interno
public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode, TypeException> {

    TypeCheckEASTVisitor() {
        super(true);
    } // enables incomplete tree exceptions

    TypeCheckEASTVisitor(boolean debug) {
        super(false, debug);
    } // enables print for debugging

    //checks that a type object is visitable (not incomplete)
    private TypeNode ckvisit(TypeNode t) throws TypeException {
        visit(t);
        return t;
    }

    @Override
    public TypeNode visitNode(ProgLetInNode n) throws TypeException {
        if (print) printNode(n);
        for (Node dec : n.declist)
            try {
                visit(dec);
            } catch (IncomplException e) {
            } catch (TypeException e) {
                System.out.println("Type checking error in a declaration: " + e.text);
            }
        return visit(n.exp);
    }

    @Override
    public TypeNode visitNode(ProgNode n) throws TypeException {
        if (print) printNode(n);
        return visit(n.exp);
    }

    @Override
    public TypeNode visitNode(FunNode n) throws TypeException {
        if (print) printNode(n, n.id);
        for (Node dec : n.declist)
            try {
                visit(dec);
            } catch (IncomplException e) {
            } catch (TypeException e) {
                System.out.println("Type checking error in a declaration: " + e.text);
            }
        if (!isSubtype(visit(n.exp), ckvisit(n.retType)))
            throw new TypeException("Wrong return type for function " + n.id, n.getLine());
        return null;
    }

    @Override
    public TypeNode visitNode(VarNode n) throws TypeException {
        if (print) printNode(n, n.id);
        if (!isSubtype(visit(n.exp), ckvisit(n.getType())))
            throw new TypeException("Incompatible value for variable " + n.id, n.getLine());
        return null;
    }

    @Override
    public TypeNode visitNode(PrintNode n) throws TypeException {
        if (print) printNode(n);
        return visit(n.exp);
    }

    @Override
    public TypeNode visitNode(IfNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.cond), new BoolTypeNode())))
            throw new TypeException("Non boolean condition in if", n.getLine());
        TypeNode t = visit(n.th);
        TypeNode e = visit(n.el);
        TypeNode result = lowestCommonAncestor(t, e);
        if (result == null) {
            throw new TypeException("Incompatible types in then-else branches", n.getLine());
        } else {
            return result;
        }
    }

    @Override
    public TypeNode visitNode(EqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if (!(isSubtype(l, r) || isSubtype(r, l)))
            throw new TypeException("Incompatible types in equal", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(TimesNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.left), new IntTypeNode())
                && isSubtype(visit(n.right), new IntTypeNode())))
            throw new TypeException("Non integers in multiplication", n.getLine());
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(PlusNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.left), new IntTypeNode())
                && isSubtype(visit(n.right), new IntTypeNode())))
            throw new TypeException("Non integers in sum", n.getLine());
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(OrNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.left), new BoolTypeNode())
                && isSubtype(visit(n.right), new BoolTypeNode())))
            throw new TypeException("Non boolean values in or", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(AndNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.left), new BoolTypeNode())
                && isSubtype(visit(n.right), new BoolTypeNode())))
            throw new TypeException("Non boolean values in and", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(DivNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.left), new IntTypeNode())
                && isSubtype(visit(n.right), new IntTypeNode())))
            throw new TypeException("Non integers in div", n.getLine());
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(CallNode n) throws TypeException {
        if (print) printNode(n, n.id);
        TypeNode t = visit(n.entry);

        ArrowTypeNode at = null;
        if (!((t instanceof ArrowTypeNode) || (t instanceof MethodTypeNode)))
            throw new TypeException("Invocation of a non-function " + n.id, n.getLine());

        if (t instanceof MethodTypeNode) {
            MethodTypeNode m = (MethodTypeNode) t;
            at = m.fun;
        } else {
            at = (ArrowTypeNode) t;
        }

        if (!(at.parlist.size() == n.arglist.size()))
            throw new TypeException("Wrong number of parameters in the invocation of " + n.id, n.getLine());
        for (int i = 0; i < n.arglist.size(); i++)
            if (!(isSubtype(visit(n.arglist.get(i)), at.parlist.get(i))))
                throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of " + n.id, n.getLine());

        return at.ret;
    }

    @Override
    public TypeNode visitNode(IdNode n) throws TypeException {
        if (print) printNode(n, n.id);

        TypeNode t = visit(n.entry);
        if (t instanceof ArrowTypeNode)
            throw new TypeException("Wrong usage of function identifier " + n.id, n.getLine());
        if (t instanceof MethodTypeNode)
            throw new TypeException("Wrong usage of method identifier " + n.id, n.getLine());
        if (t instanceof ClassTypeNode)
            throw new TypeException("Wrong usage of class identifier " + n.id, n.getLine());

        return t;
    }

    @Override
    public TypeNode visitNode(BoolNode n) {
        if (print) printNode(n, n.val.toString());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(IntNode n) {
        if (print) printNode(n, n.val.toString());
        return new IntTypeNode();
    }

// gestione tipi incompleti	(se lo sono lancia eccezione)

    @Override
    public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
        if (print) printNode(n);
        for (Node par : n.parlist)
            visit(par);
        visit(n.ret, "->"); //marks return type
        return null;
    }

    @Override
    public TypeNode visitNode(BoolTypeNode n) {
        if (print) printNode(n);
        return null;
    }

    @Override
    public TypeNode visitNode(IntTypeNode n) {
        if (print) printNode(n);
        return null;
    }

    @Override
    public TypeNode visitNode(GreaterEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if (!(isSubtype(l, r) || isSubtype(r, l)))
            throw new TypeException("Incompatible types in equal", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(LessEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.left);
        TypeNode r = visit(n.right);
        if (!(isSubtype(l, r) || isSubtype(r, l)))
            throw new TypeException("Incompatible types in equal", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(NotNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode arg = visit(n.arg);
        if (!(isSubtype(visit(n.arg), new BoolTypeNode())))
            throw new TypeException("Non boolean after NOT", n.getLine());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(MinusNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.left), new IntTypeNode())
                && isSubtype(visit(n.right), new IntTypeNode())))
            throw new TypeException("Non integers in sum", n.getLine());
        return new IntTypeNode();
    }

    // STentry (ritorna campo type)

    @Override
    public TypeNode visitSTentry(STentry entry) throws TypeException {
        if (print) printSTentry("type");
        return ckvisit(entry.type);
    }

    //OOP extension

    @Override
    public TypeNode visitNode(MethodNode methodNode) throws TypeException {
        if (print) printNode(methodNode, methodNode.id);

        for (Node dec : methodNode.declist) {
            try {
                visit(dec);
            } catch (IncomplException ignored) {
            } catch (TypeException e) {
                System.out.format("Type checking error in a declaration: %s", e.text);
            }
        }

        if (!isSubtype(visit(methodNode.exp), ckvisit(methodNode.retType)))
            throw new TypeException(String.format("Wrong return type for method %s at line %d", methodNode.id, methodNode.getLine()), methodNode.getLine());

        return null;
    }

    @Override
    public TypeNode visitNode(ClassNode classNode) throws TypeException {
        if (print)
            printNode(classNode, classNode.id + ((classNode.superID == null) ? "" : "extends " + classNode.superID));
        if (classNode.superID != null) {
            superType.put(classNode.id, classNode.superID);
            ClassTypeNode type = classNode.type;
            ClassTypeNode parentCT = (ClassTypeNode) classNode.superEntry.type;

            // controllo override dei campi
            //Seconda ottimizzazione
            for (var field : classNode.fields) {
                var offset = -field.offset - 1;
                if (offset < parentCT.allFields.size()) {
                    if (!isSubtype(type.allFields.get(offset), parentCT.allFields.get(offset))) {
                        throw new TypeException(String.format("Wrong overriding type for field %s at line %d", classNode.fields.get(offset).id,
                                classNode.fields.get(offset).getLine()), classNode.fields.get(offset).getLine());
                    }
                }
            }

            // controllo override dei metodi
            //Seconda ottimizzazione
            for (var method : classNode.methods) {
                var offset = method.offset;
                if (offset < parentCT.allMethods.size()) {
                    if (!isSubtype(type.allMethods.get(offset), parentCT.allMethods.get(offset))) {
                        throw new TypeException(String.format("Wrong overriding type for method %s at line %d", classNode.methods.get(offset).id,
                                classNode.methods.get(offset).getLine()), classNode.methods.get(offset).getLine());
                    }
                }
            }
        } else {
            for (MethodNode method : classNode.methods) {
                visit(method);
            }
        }
        return null;
    }

    @Override
    public TypeNode visitNode(ClassCallNode classCallNode) throws TypeException {
        if (print) printNode(classCallNode);

        if (classCallNode.methodEntry == null) {
            return null;
        }

        ArrowTypeNode at = null;
        if (classCallNode.methodEntry.type instanceof MethodTypeNode) {
            at = ((MethodTypeNode) classCallNode.methodEntry.type).fun;
            if (!(at.parlist.size() == classCallNode.arglist.size()))
                throw new TypeException("Wrong number of parameters in the invocation of " + classCallNode.methodId, classCallNode.getLine());
            for (int i = 0; i < classCallNode.arglist.size(); i++)
                if (!(isSubtype(visit(classCallNode.arglist.get(i)), at.parlist.get(i))))
                    throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of " + classCallNode.methodId, classCallNode.getLine());

        }

        return at.ret;
    }

    @Override
    public TypeNode visitNode(NewNode newNode) throws TypeException {
        if (print) printNode(newNode, newNode.classId);

        if (newNode.entry == null) {
            throw new TypeException("Invalid type", newNode.getLine());
        }

        ClassTypeNode type = (ClassTypeNode) newNode.entry.type;

        if (type.allFields.size() != newNode.arglist.size()) {
            throw new TypeException("Wrong number of parameters for the method call " + newNode.classId, newNode.getLine());
        }
        for (int i = 0; i < newNode.arglist.size(); i++) {
            if (!(isSubtype(visit(newNode.arglist.get(i)), type.allFields.get(i)))) {
                throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of " + newNode.classId, newNode.getLine());
            }
        }

        return new RefTypeNode(newNode.classId);
    }

    @Override
    public TypeNode visitNode(EmptyNode emptyNode) {
        if (print) printNode(emptyNode);

        return new EmptyTypeNode();
    }

    @Override
    public TypeNode visitNode(MethodTypeNode methodTypeNode) throws TypeException {
        if (print) printNode(methodTypeNode);

        visit(methodTypeNode.fun);

        return null;
    }

    @Override
    public TypeNode visitNode(RefTypeNode refTypeNode) {
        if (print) printNode(refTypeNode);

        return null;
    }


    public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {

        if (isSubtype(a, new IntTypeNode()) && isSubtype(b, new IntTypeNode())) {
            //torna int se uno dei due int, altrimenti bool
            if (a instanceof IntTypeNode || b instanceof IntTypeNode) {
                return new IntTypeNode();
            } else {
                return new BoolTypeNode();
            }
        }

        if ((a instanceof RefTypeNode || a instanceof EmptyTypeNode)
                && (b instanceof RefTypeNode || b instanceof EmptyTypeNode)) {

            //se uno dei due è empty ritorna l'altro
            if (a instanceof EmptyTypeNode) return b;
            if (b instanceof EmptyTypeNode) return a;

            String superClassId = "";
            String classA = ((RefTypeNode) a).id;
            String classB = ((RefTypeNode) b).id;

            if (classA.equals(classB)) return a;

            /*
            all'inizio considera la classe di "a" e risale, poi, le sue superclassi
            (tramite la funzione "superType") controllando, ogni volta, se "b"
            sia sottotipo (metodo "isSubtype") della classe considerata:
            • torna un RefTypeNode a tale classe qualora il controllo abbia, prima o poi, successo, null altrimenti
             */
            do {
                superClassId = superType.get(classA);
                RefTypeNode superClass = new RefTypeNode(superClassId);
                if (isSubtype(b, superClass)) {
                    return superClass;
                }
                classA = superClassId;
            } while (superClassId != null);
        }

        return null;
    }
}