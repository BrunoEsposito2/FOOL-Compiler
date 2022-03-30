package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.sql.Ref;
import java.util.HashMap;
import java.util.Map;

public class TypeRels {

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		if( ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))
				|| ((a instanceof BoolTypeNode) && (b instanceof BoolTypeNode))
				|| ((a instanceof IntTypeNode) && (b instanceof IntTypeNode))
		){
			return true;
		}

		if (a instanceof ArrowTypeNode && b instanceof ArrowTypeNode){
			ArrowTypeNode funA = (ArrowTypeNode) a;
			ArrowTypeNode funB = (ArrowTypeNode) b;
			if (isSubtype(funA.ret,funB.ret)){ //co-varianza
				//contro-varianza
				if (funA.parlist.size() == funB.parlist.size()){
					for (int i=0; i<funA.parlist.size(); i++){
						if(!(isSubtype(funB.parlist.get(i),funA.parlist.get(i)))){
							return false;
						}
					}
					return true;
				}else{
					return false;
				}
			}else{
				return false;
			}
		}

		if (a instanceof RefTypeNode && b instanceof RefTypeNode){
			String idA = ((RefTypeNode) a).id;
			String idB = ((RefTypeNode) b).id;
			return idA.equals(idB);

		}
		// OBJECT-ORIENTATION EXTENSION
		if (a instanceof EmptyTypeNode && b instanceof RefTypeNode){
			return true;
		}

		return false;
	}
}
