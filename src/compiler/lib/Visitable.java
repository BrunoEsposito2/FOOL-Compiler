package compiler.lib;

import compiler.exc.TypeException;

public interface Visitable {

	<S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E, TypeException;

}
