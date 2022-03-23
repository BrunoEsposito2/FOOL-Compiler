package compiler;

import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

import static compiler.lib.FOOLlib.nlJoin;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {
	
	private List<Map<String, STentry>> symTable = new ArrayList<>();
	private Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private HashSet<String> localDeclaration;
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
	    for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		symTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : n.parlist) parTypes.add(par.getType()); 
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		} 
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=-2;
		
		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level 
		return null;
	}
	
	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		visit(n.exp);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}
	
	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(AndNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(NotNode n) {
		if (print) printNode(n);
		visit(n.arg);
		return null;
	}

	@Override
	public Void visitNode(MinusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(FieldNode fieldNode) {
		if (print) printNode(fieldNode);
		visit((fieldNode));
		return null;
	}

	@Override
	public Void visitNode(MethodNode methodNode) {
		if (print) printNode(methodNode);

		var symbolTable = symTable.get(nestingLevel);
		var parameters = new ArrayList<TypeNode>();
		methodNode.parlist.stream().map(DecNode::getType).forEach(parameters::add);
		var methodType = new MethodTypeNode(new ArrowTypeNode(parameters, methodNode.retType));
		methodNode.setType(methodType);

		nestingLevel += 1;
		var hmn = new HashMap<String, STentry>();
		symTable.add(hmn);
		var previousNestingLevelDecOffset = decOffset;
		int parametersOffset=1;
		for(var parNode : methodNode.parlist){
			if(hmn.put(parNode.id, new STentry(nestingLevel, parNode.getType(), parametersOffset++)) != null){
				System.out.format("Parameter %s at line %d has already been declared!", parNode.id, parNode.getLine());
				stErrors++;
			}
		}
		methodNode.declist.forEach(this::visit);
		visit(methodNode.exp);

		symTable.remove(nestingLevel--);
		decOffset = previousNestingLevelDecOffset;

		return null;
	}

	@Override
	public Void visitNode(ClassNode classNode) {
		if (print) printNode(classNode);
		var symbolTable = symTable.get(0);
		localDeclaration = new HashSet<>();
		var type = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());
		var virtualTable = new HashMap<String, STentry>();

		classNode.type = type;
		var entry = new STentry(0, type, decOffset--);
		if (symbolTable.put(classNode.id, entry) != null) {
			System.out.format("Class %s at line %d has already been declared!", classNode.id, classNode.getLine());
			stErrors += 1;
		}

		classTable.put(classNode.id, virtualTable);
		symTable.add(virtualTable);

		nestingLevel += 1;
		//Fields handling
		var fieldOffset = -(type.allFields.size()) - 1;
		for(var field : classNode.fields) {
			if(localDeclaration.contains(field.id)){
				System.out.format("Field %s at line %d has already been declared!", field.id, field.getLine());
				stErrors += 1;
			}else{
				localDeclaration.add(field.id);
				field.offset = fieldOffset;
				virtualTable.put(field.id, new STentry(nestingLevel, field.getType(), field.offset));
				fieldOffset -= 1;
				type.allFields.add(field.getType());
			}
		}

		var previousNestingLevelDecOffset = decOffset;
		decOffset = type.allMethods.size();

		//Methods handling
		for(var method : classNode.methods){
			if(localDeclaration.contains(method.id)){
				System.out.format("Method %s at line %d has already been declared!", method.id, method.getLine());
				stErrors += 1;
			}else{
				localDeclaration.add(method.id);
				visit(method);
				virtualTable.put(method.id, new STentry(nestingLevel, method.getType(), decOffset));
				decOffset += 1;
				type.allMethods.add(((MethodTypeNode)method.getType()).fun);
			}
		}
		symTable.remove(nestingLevel--);
		decOffset = previousNestingLevelDecOffset;
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode classCallNode) {
		if (print) printNode(classCallNode);

		var objectEntry = stLookup(classCallNode.objectId);
		if(objectEntry == null){
			System.out.format("Object %s at line %d has not been declared!", classCallNode.objectId, classCallNode.getLine());
			stErrors += 1;
		}else{
			//Fetch the method
			if (!(objectEntry.type instanceof RefTypeNode)){
				System.out.format("Object %s at line %d has not a RefTypeNode!", classCallNode.objectId, classCallNode.getLine());
				stErrors += 1;
			} else{
				String classId = ((RefTypeNode) objectEntry.type).id;
				STentry methodEntry = classTable.get(classId).get(classCallNode.methodId);
				if (methodEntry == null){
					System.out.format("Method %s at line %d has not been declared!", classCallNode.methodId, classCallNode.getLine());
					stErrors += 1;
				}else{
					classCallNode.entry = objectEntry;
					classCallNode.methodEntry = methodEntry;
					classCallNode.nl = nestingLevel;
				}
			}
		}
		classCallNode.arglist.forEach(this::visit);
		return null;
	}

	@Override
	public Void visitNode(EmptyNode n) {
		if (print) printNode(n);

		return null;
	}

	@Override
	public Void visitNode(NewNode newNode) {
		if(print) printNode(newNode);

		//Checks if ID is in ClassTable (class has to be declared)
		if(!(classTable.containsKey(newNode.classId))){
			System.out.format("Class %s at line %d has not been declared!", newNode.classId, newNode.getLine());
			stErrors += 1;
		}else{
			var classEntry = symTable.get(0).get(newNode.classId);
			//Class needs to be declared at level 0, otherwise it is invalid
			if(classTable == null){
				System.out.format("Class %s at line %d has not been declared at level 0!", newNode.classId, newNode.getLine());
				stErrors += 1;
			}else{
				newNode.entry = classEntry;
			}
		}

		newNode.arglist.forEach(this::visit);

		return null;
	}
}
