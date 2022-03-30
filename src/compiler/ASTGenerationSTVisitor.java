package compiler;

import compiler.AST.*;
import compiler.lib.DecNode;
import compiler.lib.Node;
import compiler.lib.TypeNode;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import compiler.FOOLParser.*;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.extractCtxName;
import static compiler.lib.FOOLlib.lowerizeFirstChar;

public class ASTGenerationSTVisitor extends compiler.FOOLBaseVisitor<Node> {

    String indent;
    public boolean print;

    ASTGenerationSTVisitor() {
    }

    ASTGenerationSTVisitor(boolean debug) {
        print = debug;
    }

    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix = "";
        Class<?> ctxClass = ctx.getClass(), parentClass = ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) // parentClass is the var context (and not ctxClass itself)
            prefix = lowerizeFirstChar(extractCtxName(parentClass.getName())) + ": production #";
        System.out.println(indent + prefix + lowerizeFirstChar(extractCtxName(ctxClass.getName())));
    }

    @Override
    public Node visit(ParseTree t) {
        if (t == null) return null;
        String temp = indent;
        indent = (indent == null) ? "" : indent + "  ";
        Node result = super.visit(t);
        indent = temp;
        return result;
    }

    @Override
    public Node visitProg(ProgContext c) {
        if (print) printVarAndProdName(c);
        return visit(c.progbody());
    }

    @Override
    public Node visitLetInProg(LetInProgContext c) {
        if (print) printVarAndProdName(c);
        List<DecNode> declist = new ArrayList<>();
        for (CldecContext dec : c.cldec()) declist.add((DecNode) visit(dec));
        for (DecContext dec : c.dec()) declist.add((DecNode) visit(dec));
        return new ProgLetInNode(declist, visit(c.exp()));
    }

    @Override
    public Node visitNoDecProg(NoDecProgContext c) {
        if (print) printVarAndProdName(c);
        return new ProgNode(visit(c.exp()));
    }

    @Override
    public Node visitTimesDiv(TimesDivContext c) {
        if (print) printVarAndProdName(c);
        if (c.TIMES() == null) {
            Node n = new DivNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.DIV().getSymbol().getLine());        // setLine added
            return n;
        } else {
            Node n = new TimesNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.TIMES().getSymbol().getLine());        // setLine added
            return n;
        }

    }

    @Override
    public Node visitPlusMinus(PlusMinusContext c) {
        if (print) printVarAndProdName(c);
        if (c.PLUS() == null) {
            Node n = new MinusNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.MINUS().getSymbol().getLine());
            return n;
        } else {
            Node n = new PlusNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.PLUS().getSymbol().getLine());
            return n;
        }
    }

    @Override
    public Node visitComp(CompContext c) {
        if (print) printVarAndProdName(c);
        if (c.LE() == null && c.GE() == null) {
            Node en = new EqualNode(visit(c.exp(0)), visit(c.exp(1)));
            en.setLine(c.EQ().getSymbol().getLine());
            return en;
        } else if (c.LE() == null && c.EQ() == null) {
            Node gen = new GreaterEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            gen.setLine(c.GE().getSymbol().getLine());
            return gen;
        } else {
            Node len = new LessEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            len.setLine(c.LE().getSymbol().getLine());
            return len;
        }
    }

    @Override
    public Node visitVardec(VardecContext c) {
        if (print) printVarAndProdName(c);
        Node n = null;
        if (c.ID() != null) { //non-incomplete ST
            n = new VarNode(c.ID().getText(), (TypeNode) visit(c.type()), visit(c.exp()));
            n.setLine(c.VAR().getSymbol().getLine());
        }
        return n;
    }

    @Override
    public Node visitFundec(FundecContext c) {
        if (print) printVarAndProdName(c);
        List<ParNode> parList = new ArrayList<>();
        for (int i = 1; i < c.ID().size(); i++) {
            ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
            p.setLine(c.ID(i).getSymbol().getLine());
            parList.add(p);
        }
        List<DecNode> decList = new ArrayList<>();
        for (DecContext dec : c.dec()) decList.add((DecNode) visit(dec));
        Node n = null;
        if (c.ID().size() > 0) { //non-incomplete ST
            n = new FunNode(c.ID(0).getText(), (TypeNode) visit(c.type(0)), parList, decList, visit(c.exp()));
            n.setLine(c.FUN().getSymbol().getLine());
        }
        return n;
    }

    @Override
    public Node visitNot(NotContext ctx) {
        if (print) printVarAndProdName(ctx);
        Node n = new NotNode(visit(ctx.exp()));
        n.setLine(ctx.NOT().getSymbol().getLine());
        return n;
    }

    @Override
    public Node visitAndOr(AndOrContext ctx) {
        if (print) printVarAndProdName(ctx);
        if (ctx.AND() == null) {
            Node n = new OrNode(visit(ctx.exp(0)), visit(ctx.exp(1)));
            n.setLine(ctx.OR().getSymbol().getLine());
            return n;
        } else {
            Node n = new AndNode(visit(ctx.exp(0)), visit(ctx.exp(1)));
            n.setLine(ctx.AND().getSymbol().getLine());
            return n;
        }
    }

    @Override
    public Node visitIntType(IntTypeContext c) {
        if (print) printVarAndProdName(c);
        return new IntTypeNode();
    }

    @Override
    public Node visitBoolType(BoolTypeContext c) {
        if (print) printVarAndProdName(c);
        return new BoolTypeNode();
    }

    @Override
    public Node visitInteger(IntegerContext c) {
        if (print) printVarAndProdName(c);
        int v = Integer.parseInt(c.NUM().getText());
        return new IntNode(c.MINUS() == null ? v : -v);
    }

    @Override
    public Node visitTrue(TrueContext c) {
        if (print) printVarAndProdName(c);
        return new BoolNode(true);
    }

    @Override
    public Node visitFalse(FalseContext c) {
        if (print) printVarAndProdName(c);
        return new BoolNode(false);
    }

    @Override
    public Node visitIf(IfContext c) {
        if (print) printVarAndProdName(c);
        Node ifNode = visit(c.exp(0));
        Node thenNode = visit(c.exp(1));
        Node elseNode = visit(c.exp(2));
        Node n = new IfNode(ifNode, thenNode, elseNode);
        n.setLine(c.IF().getSymbol().getLine());
        return n;
    }

    @Override
    public Node visitPrint(PrintContext c) {
        if (print) printVarAndProdName(c);
        return new PrintNode(visit(c.exp()));
    }

    @Override
    public Node visitPars(ParsContext c) {
        if (print) printVarAndProdName(c);
        return visit(c.exp());
    }

    @Override
    public Node visitId(IdContext c) {
        if (print) printVarAndProdName(c);
        Node n = new IdNode(c.ID().getText());
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    @Override
    public Node visitCall(CallContext c) {
        if (print) printVarAndProdName(c);
        List<Node> arglist = new ArrayList<>();
        for (ExpContext arg : c.exp()) arglist.add(visit(arg));
        Node n = new CallNode(c.ID().getText(), arglist);
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    // OBJECT-ORIENTED EXTENSION

    public Node visitCldec(CldecContext c) {
        if (print) printVarAndProdName(c);

        int start = 1;

        // fields
        List<FieldNode> fields = new ArrayList<>();
        for (int i = start, j = 0; i < c.ID().size(); i++, j++) {
            FieldNode f = new FieldNode(c.ID(i).getText(), (TypeNode) visit(c.type(j)));
            f.setLine(c.ID(i).getSymbol().getLine());
            fields.add(f);
        }

        // methods
        List<MethodNode> methods = new ArrayList<>();
        for (MethdecContext dec : c.methdec()) methods.add((MethodNode) visit(dec));

        // new class node
        Node n = null;
        if (c.ID().size() > 0) { //non-incomplete ST
            n = new ClassNode(c.ID(0).getText(), fields, methods);
            n.setLine(c.CLASS().getSymbol().getLine());
        }

        return n;
    }

    @Override
    public Node visitMethdec(MethdecContext c) {
        if (print) printVarAndProdName(c);

        // parametri
        List<ParNode> parList = new ArrayList<>();
        for (int i = 1; i < c.ID().size(); i++) {
            ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
            p.setLine(c.ID(i).getSymbol().getLine());
            parList.add(p);
        }

        // dichiarazioni all'interno del metodo
        List<DecNode> decList = new ArrayList<>();
        for (DecContext dec : c.dec()) {
            decList.add((DecNode) visit(dec));
        }

        // new method node
        Node n = null;
        if (c.ID().size() > 0) { //non-incomplete ST
            n = new MethodNode(c.ID(0).getText(), (TypeNode) visit(c.type(0)), parList, decList, visit(c.exp()));
            n.setLine(c.FUN().getSymbol().getLine());
        }

        return n;
    }

    @Override
    public Node visitDotCall(DotCallContext c) {
        if (print) printVarAndProdName(c);

        // argomenti chiamata metodo
        List<Node> arglist = new ArrayList<>();
        for (ExpContext arg : c.exp()){
            arglist.add(visit(arg));
        }

        // new dot call node
        Node n = new ClassCallNode(c.ID(0).getText(), // object id
                c.ID(1).getText(), // method id
                arglist);
        n.setLine(c.ID(0).getSymbol().getLine());

        return n;
    }

    @Override
    public Node visitNew(NewContext c) {
        if (print) printVarAndProdName(c);

        // argomenti
        List<Node> arglist = new ArrayList<>();
        for (ExpContext arg : c.exp()) arglist.add(visit(arg));

        // new node
        Node n = new NewNode(c.ID().getText(), arglist);
        n.setLine(c.ID().getSymbol().getLine());

        return n;
    }

    @Override
    public Node visitNull(NullContext c) {
        if (print) printVarAndProdName(c);

        Node n = new EmptyNode();
        n.setLine(c.NULL().getSymbol().getLine());

        return n;
    }

    public Node visitIdType(IdTypeContext c) {
        if (print) printVarAndProdName(c);

        Node n = new RefTypeNode(c.ID().getText());
        n.setLine(c.ID().getSymbol().getLine());

        return n;
    }
}
