package compiler;

import compiler.AST.*;
import compiler.lib.TypeNode;

import java.util.HashMap;
import java.util.Map;

public class TypeRels {

    protected static final Map<String, String> superType = new HashMap<>();//mappa nome classe sulla sua super()

    // valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
    public static boolean isSubtype(TypeNode a, TypeNode b) {

        if (((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))
                || ((a instanceof BoolTypeNode) && (b instanceof BoolTypeNode))
                || ((a instanceof IntTypeNode) && (b instanceof IntTypeNode))
                || ((a instanceof EmptyTypeNode) && (b instanceof RefTypeNode))
        ) {
            return true;
        }

        // OBEJECT-ORIENTATION EXTENSION
        if (a instanceof RefTypeNode && b instanceof RefTypeNode) {
            String idA = ((RefTypeNode) a).id;
            String idB = ((RefTypeNode) b).id;
            //If necessario per dichiarazione di variabili del tipo A var = new A(); se no andavamo in superType e non trovavamo nulla
            if (idA.equals(idB)) return true;
            return ((superType.get(idA) != null) && (superType.get(idA).equals(idB))); //check if superType exists
        }

        if (a instanceof ArrowTypeNode && b instanceof ArrowTypeNode) {
            ArrowTypeNode funA = (ArrowTypeNode) a;
            ArrowTypeNode funB = (ArrowTypeNode) b;
            if (isSubtype(funA.ret, funB.ret)) { //co-varianza
                //contro-varianza
                if (funA.parlist.size() == funB.parlist.size()) {
                    for (int i = 0; i < funA.parlist.size(); i++) {
                        if (!(isSubtype(funB.parlist.get(i), funA.parlist.get(i)))) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }

}
