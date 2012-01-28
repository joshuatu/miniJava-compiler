package miniJava.SyntacticAnalyzer;

import java.io.InputStream;

public class Parser {
	private Scanner scanner;
	private Token currentToken = null;

	public Parser(InputStream in) {
		scanner = new Scanner(in);
	}

	/**
	 * Consumes the current token silently
	 * 
	 * @throws ScannerException
	 *             If a token cannot be constructed
	 */
	private void consume() throws SyntaxErrorException {
		try {
			currentToken = scanner.nextToken();
		} catch (ScannerException e) {
			throw new SyntaxErrorException("unrecognized token " + e.currentString, currentToken);
		}
	}

	/**
	 * Consumes a token of expected type or throw an exception
	 * 
	 * @param type
	 *            The type of the token that is expected
	 * @throws SyntaxErrorException
	 *             If an unexpected type is found
	 * @throws ScannerException
	 *             If a token cannot be constructed
	 */
	private void expect(TokenType type) throws SyntaxErrorException {
		if (currentToken.type != type)
			if (type.spelling != null) {
				throw new SyntaxErrorException("expected " + type.spelling + " but found", currentToken);
			} else {
				throw new SyntaxErrorException("expected " + type.toString() + " but found", currentToken);
			}

		consume();
	}

	/**
	 * Parses the <i>Program</i> non-terminal, which is also the start symbol of
	 * miniJava grammar
	 * 
	 * <pre>
	 * Program ::= (ClassDeclaration)* <b>eot</b>
	 * </pre>
	 * 
	 * @throws ScannerException
	 */
	public void parseProgram() throws SyntaxErrorException {
		consume();

		while (currentToken.type != TokenType.EOT)
			parseClassDeclaration();
	}

	/**
	 * Parses the <i>ClassDeclaration</i> non-terminal
	 * 
	 * <pre>
	 * ClassDeclaration ::= 
	 *     <b>class</b> <i>id</i> <b>{</b>
	 *         (FieldDeclaration | MethodDeclaration)*
	 *     <b>}</b>
	 * 
	 * FieldDeclaration ::= Declarators <i>id</i><b>;</b>
	 * 
	 * MethodDeclaration ::= 
	 *     Declarators <i>id</i><b>(</b>ParameterList?<b>) {</b>
	 *         Statement* (<b>return</b> Expression<b>;</b>)?
	 *     <b>}</b>
	 * 
	 * </pre>
	 * 
	 * @throws SyntaxErrorException
	 */
	private void parseClassDeclaration() throws SyntaxErrorException {
		expect(TokenType.CLASS);
		expect(TokenType.IDENTIFIER);
		expect(TokenType.LCURL);
		while (currentToken.type != TokenType.RCURL) {
			// Left factorize the Declarators and id
			parseDeclarators();
			expect(TokenType.IDENTIFIER);

			if (currentToken.type == TokenType.SEMICOLON) {
				// FieldDeclaration
				consume();
			} else if (currentToken.type == TokenType.LPAREN) {
				// MethodDeclaration
				consume();
				if (currentToken.type != TokenType.RPAREN)
					parseParameterList();
				expect(TokenType.RPAREN);

				// Method body
				expect(TokenType.LCURL);
				while (currentToken.type != TokenType.RCURL) {
					while (currentToken.type != TokenType.RETURN && currentToken.type != TokenType.RCURL)
						parseStatement();
					if (currentToken.type == TokenType.RETURN) {
						consume();
						parseExpression();
						expect(TokenType.SEMICOLON);
					}
				}
				consume();
			}
		}
		expect(TokenType.RCURL);
	}

	/**
	 * Parses the <i>Declarators</i> non-terminal
	 * 
	 * <pre>
	 * Declarators ::= (<b>public</b> | <b>private</b>)? <b>static</b>? Type
	 * </pre>
	 * 
	 * @throws SyntaxErrorException
	 */
	private void parseDeclarators() throws SyntaxErrorException {
		if (currentToken.type == TokenType.PUBLIC || currentToken.type == TokenType.PRIVATE)
			consume();
		if (currentToken.type == TokenType.STATIC)
			consume();
		parseType();
	}

	/**
	 * Parses the <i>Type</i> non-terminal
	 * 
	 * <pre>
	 * Type ::= PrimType | ClassType | ArrayType
	 * PrimType ::= <b>int</b> | <b>boolean</b> | <b>void</b>
	 * ClassType ::= id
	 * ArrType ::= (int | ClassType) <b>[]</b>
	 * </pre>
	 * 
	 * <i>Type</i> can be rewritten as:
	 * 
	 * <pre>
	 * Type ::= (<b>int</b> | id) (<i>&#949;</i> | <b>[]</b>) | <b>boolean</b> | <b>void</b>
	 * </pre>
	 * 
	 * @throws SyntaxErrorException
	 */
	private void parseType() throws SyntaxErrorException {
		switch (currentToken.type) {
		case INT:
		case IDENTIFIER:
			consume();
			if (currentToken.type == TokenType.LSQUARE) {
				consume();
				expect(TokenType.RSQUARE);
			}
			break;
		case BOOLEAN:
		case VOID:
			consume();
			break;
		default:
			throw new SyntaxErrorException("expected a Type, found ", currentToken);
		}
	}

	/**
	 * Parses the <i>ParameterList</i> non-terminal
	 * 
	 * <pre>
	 * ParameterList ::= Type id (, Type id)*
	 * </pre>
	 * 
	 * @throws SyntaxErrorException
	 */
	private void parseParameterList() throws SyntaxErrorException {
		parseType();
		expect(TokenType.IDENTIFIER);
		while (currentToken.type == TokenType.COMMA) {
			consume();
			parseType();
			expect(TokenType.IDENTIFIER);
		}
	}

	/**
	 * Parses the <i>ArgumentList</i> non-terminal
	 * 
	 * <pre>
	 * ArgumentList ::= Expression (, Expression)*
	 * </pre>
	 * 
	 * @throws SyntaxErrorException
	 */
	private void parseArgumentList() throws SyntaxErrorException {
		parseExpression();
		while (currentToken.type == TokenType.COMMA) {
			consume();
			parseExpression();
		}
	}

	/**
	 * Parses the members of a <i>Reference</i>
	 * 
	 * <pre>
	 * Reference = (this | <b>id</b>) ReferenceMember
	 * ReferenceMember = (. <b>id</b>)*
	 * </pre>
	 * 
	 * @throws SyntaxErrorException
	 */
	private void parseReferenceMember() throws SyntaxErrorException {
		while (currentToken.type == TokenType.DOT) {
			consume();
			expect(TokenType.IDENTIFIER);
		}
	}

	/**
	 * Parses the <i>Statement</i> non-terminal
	 * 
	 * <pre>
	 * Statement ::=
	 *     { Statement* }
	 *     | Type id = Expression <b>;</b>
	 *     | Reference (<b>[</b> Expression <b>]</b>)? = Expression <b>;</b>
	 *     | Reference <b>(</b> ArgumentList? <b>)</b> <b>;</b>
	 *     | <b>if (</b> Expression <b>)</b> Statement (<b>else</b> Statement)?
	 *     | <b>while (</b> Expression <b>)</b> Statement
	 * </pre>
	 * 
	 * @throws SyntaxErrorException
	 */
	private void parseStatement() throws SyntaxErrorException {
		// Starters(Statement) = {, int, boolean, void, this, <id>, if, while
		switch (currentToken.type) {
		case LCURL:
			while (currentToken.type != TokenType.RCURL)
				parseStatement();
			expect(TokenType.RCURL);
			break;

		case INT:
		case BOOLEAN:
		case VOID:
			parseType();
			expect(TokenType.IDENTIFIER);
			expect(TokenType.EQUALTO);
			parseExpression();
			expect(TokenType.SEMICOLON);
			break;

		case THIS:
			consume();
			switch (currentToken.type) {
			// this = Expression;
			case EQUALTO:
				consume();
				parseExpression();
				break;

			// this[Expression] = Expression;
			case LSQUARE:
				consume();
				parseExpression();
				expect(TokenType.RSQUARE);
				expect(TokenType.EQUALTO);
				parseExpression();
				break;

			// this(ArgumentList?);
			case LPAREN:
				consume();
				if (currentToken.type != TokenType.RPAREN)
					parseArgumentList();
				expect(TokenType.RPAREN);
				break;

			default:
				throw new SyntaxErrorException(currentToken);
			}
			expect(TokenType.SEMICOLON);

		case IDENTIFIER:
			consume();

			switch (currentToken.type) {
			case EQUALTO:
				consume();
				parseExpression();
				break;

			case IDENTIFIER:
				// id id
				consume();

				switch(currentToken.type) {
				case EQUALTO:
					// id id = Expression
					consume();
					break;

				case LSQUARE:
					// id id[Expression?] = Expression;
					consume();
					if(currentToken.type != TokenType.RSQUARE)
						parseExpression();
					expect(TokenType.RSQUARE);
					break;

				default:
					throw new SyntaxErrorException(currentToken);
				}
				expect(TokenType.EQUALTO);
				parseExpression();
				break;

			case LPAREN:
				// id (ArgumentList?)
				consume();
				if (currentToken.type != TokenType.RPAREN)
					parseArgumentList();
				expect(TokenType.RPAREN);
				break;

			case LSQUARE:
				// id[Expression] = Expression
				consume();
				parseExpression();
				expect(TokenType.RSQUARE);
				expect(TokenType.EQUALTO);
				parseExpression();
				break;

			case DOT:
				// id.
				parseReferenceMember();

				switch (currentToken.type) {

				case LSQUARE:
					// id.id[Expression] = Expression
					consume();
					parseExpression();
					expect(TokenType.RSQUARE);
					expect(TokenType.EQUALTO);
					parseExpression();
					break;

				case LPAREN:
					// id.id(ArgumentList?)
					consume();
					if (currentToken.type != TokenType.RPAREN)
						parseArgumentList();
					expect(TokenType.RPAREN);
					break;

				case EQUALTO:
					// id.id = Expression
					consume();
					parseExpression();
					break;

				default:
					throw new SyntaxErrorException(currentToken);
				}
				break;

			default:
				throw new SyntaxErrorException(currentToken);
			}
			expect(TokenType.SEMICOLON);
			break;

		case IF:
			expect(TokenType.LPAREN);
			parseExpression();
			expect(TokenType.RPAREN);
			parseStatement();
			if (currentToken.type == TokenType.ELSE) {
				consume();
				parseStatement();
			}
			break;

		case WHILE:
			expect(TokenType.LPAREN);
			parseExpression();
			expect(TokenType.RPAREN);
			parseStatement();
			break;

		default:
			throw new SyntaxErrorException(currentToken);
		}
	}

	/**
	 * Parses the <i>Expression</i> non-terminal
	 * 
	 * <pre>
	 * Expression ::=
	 *     Reference ( <b>[</b> Expression <b>]</b> )?
	 *     | Reference <b>(</b> ArgumentList? <b>)</b>
	 *     | <i>unop</i> Expression
	 *     | Expression <i>binop</i> Expression
	 *     | ( Expression )
	 *     | num | true | false
	 *     | new (id ( ) | int [ Expression ] | id [ Expression ] )
	 * </pre>
	 * 
	 * @throws SyntaxErrorException
	 */
	private void parseExpression() throws SyntaxErrorException {
		expect(TokenType.NUMBER);
	}

}