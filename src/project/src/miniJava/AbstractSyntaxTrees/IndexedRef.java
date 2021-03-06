/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IndexedRef extends Reference {

	public IndexedRef(Reference ref, Expression e, SourcePosition posn) {
		super(posn);
		this.ref = ref;
		this.indexExpr = e;
	}

	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitIndexedRef(this, o);
	}

	@Override
	public Declaration getDeclaration() {
		return ref.getDeclaration();
	}

	@Override
	public void setDeclaration(Declaration decl) {
		//ref.setDeclaration(decl);
		throw new RuntimeException("IndexedRef's setDeclaration should never be called!");
	}

	public Reference ref;
	public Expression indexExpr;
}
