package com.puppycrawl.tools.ast.web.viewer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Stack;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.grammars.CommentListener;
import com.puppycrawl.tools.checkstyle.grammars.GeneratedJavaLexer;
import com.puppycrawl.tools.checkstyle.grammars.GeneratedJavaRecognizer;

/**
 * @author <a href="mailto:nesterenko-aleksey@list.ru">Aleksey Nesterenko</a>
 */
public final class Utils {

	/**
	 * To prevent instantiation
	 */
	private Utils() {

		throw new AssertionError();
	}

	/**
	 * Parses a file and returns the parse tree.
	 * 
	 * @return the root node of the parse tree
	 * @throws TokenStreamException
	 * @throws RecognitionException
	 * @throws ANTLRException
	 *             if the file is not a Java source
	 */
	public static DetailAST parse(String fullText) throws RecognitionException,
			TokenStreamException {

		Reader sr = new StringReader(fullText);
		GeneratedJavaLexer lexer = new GeneratedJavaLexer(sr);

		// Ugly hack to skip comments support for now
		lexer.setCommentListener(new CommentListener() {

			@Override
			public void reportSingleLineComment(String aType, int aStartLineNo,
					  int aStartColNo) {
			}

			@Override
			public void reportBlockComment(String aType, int aStartLineNo,
					  int aStartColNo, int aEndLineNo, int aEndColNo) {
			}
		});

		lexer.setTreatAssertAsKeyword(true);
		lexer.setTreatEnumAsKeyword(true);

		GeneratedJavaRecognizer parser = new GeneratedJavaRecognizer(lexer);
		parser.setASTNodeClass(DetailAST.class.getName());
		parser.compilationUnit();

		return (DetailAST) parser.getAST();
	}

	/**
	 * Gets Json representation of DetailAST object.
	 * 
	 * @param ast
	 *            DetailAST object.
	 * @return String containing Json representation.
	 * @throws IOException
	 */
	public static String toJson(DetailAST ast) throws IOException {

		int ROOT_NODE_ID = -1;
		int nodeId = ROOT_NODE_ID;
		
		Stack<Integer> parentNodeIdStack = new Stack<>();
		parentNodeIdStack.push(nodeId); // root node id

		DetailAST currentNode = ast;

		StringWriter writer = new StringWriter();
		JsonFactory jFactory = new JsonFactory();
		JsonGenerator jsonGenerator = jFactory.createJsonGenerator(writer)
				  .useDefaultPrettyPrinter();

		jsonGenerator.writeStartObject();
		
		while (currentNode != null) {

			DetailAST toVisit = currentNode.getFirstChild();
			nodeId++;

			writeJson(nodeId, parentNodeIdStack, currentNode, jsonGenerator);

			if (toVisit != null) {
				parentNodeIdStack.push(nodeId);
			}

			while ((currentNode != null) && (toVisit == null)) {
				toVisit = currentNode.getNextSibling();
				if (toVisit == null) {
					currentNode = currentNode.getParent();
					parentNodeIdStack.pop();
				}
			}
			currentNode = toVisit;
		}

		jsonGenerator.writeEndObject();
		jsonGenerator.close();

		String result = writer.toString();
		writer.close();

		parentNodeIdStack = null;

		return result;

	}

	private static void writeJson(int nodeId, Stack<Integer> parentNodeIdStack,
			  DetailAST currentNode, JsonGenerator jsonGenerator)
			throws IOException, JsonGenerationException {
		
		jsonGenerator.writeObjectFieldStart(currentNode.toString());
		jsonGenerator.writeStringField("text", currentNode.getText());
		jsonGenerator.writeStringField("type",
				  TokenTypes.getTokenName(currentNode.getType()));
		jsonGenerator.writeNumberField("lineNo", currentNode.getLineNo());
		jsonGenerator.writeNumberField("columnNo",
				  currentNode.getColumnNo());
		jsonGenerator.writeNumberField("id", nodeId);
		jsonGenerator.writeNumberField("parentId", parentNodeIdStack.peek());
		jsonGenerator.writeEndObject();
	}

}