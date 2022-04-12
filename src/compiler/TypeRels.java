package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.lang.reflect.Type;
import java.sql.Ref;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TypeRels {

	protected static final Map<String,String> superTypeMap = new HashMap<>();//mappa nome classe sulla sua super()

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		if( ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))
				|| ((a instanceof BoolTypeNode) && (b instanceof BoolTypeNode))
				|| ((a instanceof IntTypeNode) && (b instanceof IntTypeNode))
		){
			return true;
		}
		// OOP
		if (a instanceof EmptyTypeNode && b instanceof RefTypeNode){
			return true;
		}

		if (a instanceof RefTypeNode && b instanceof RefTypeNode){
			String idA = ((RefTypeNode) a).id;
			String idB = ((RefTypeNode) b).id;
			return superTypeMap.get(idA).equals(superTypeMap.get(idB));
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

		if (a instanceof ArrowTypeNode && b instanceof RefTypeNode){
			return true;
		}


		return false;
	}

	public static void superType(RefTypeNode a, RefTypeNode b){
		superTypeMap.put(a.id,b.id);//definisco gerarchia tra A e B
	}

}
