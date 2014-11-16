package com.puppycrawl.tools.ast.web.viewer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.puppycrawl.tools.ast.web.viewer.json.domain.Ast;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
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
	 * Gets Json representation of an AST object.
	 * @param ast
	 * 			AST object.
	 * @return
	 * 			String containing Json representation.
	 * @throws IOException
	 */
	public static String toJson(DetailAST ast) throws IOException {
		
		StringBuilder jsonStringBuilder = new StringBuilder();
		
		DetailAST rootAst = ast;
		
		DetailAST currentNodeAst = rootAst;
		
		ObjectMapper mapper = new ObjectMapper();
		jsonStringBuilder.append("{");
		
		while (currentNodeAst != null) {
			
			List<Ast> astChildrenList = new ArrayList<>(currentNodeAst.getChildCount());
			astChildrenList = getAstChildNodes(currentNodeAst);
			Ast astNode = convertDetailAstToAst(currentNodeAst, astChildrenList);
			
			jsonStringBuilder.append("\"").append(astNode).append("\"").append(":").append(" [");
			String json = mapper.writer().withDefaultPrettyPrinter().
					  writeValueAsString(astNode);
			jsonStringBuilder.append(json);
			jsonStringBuilder.append("]");
			
			DetailAST nextNodeAst = currentNodeAst.getNextSibling();
			
			if (nextNodeAst == null) {
				
				currentNodeAst = null;
			}
			
			if (currentNodeAst != null) {
				
				currentNodeAst = currentNodeAst.getFirstChild();
			
				rootAst = rootAst.getNextSibling();
				currentNodeAst = rootAst;
			
				jsonStringBuilder.append(",");
			}
			
		}
		
		jsonStringBuilder.append("}");
		
		return jsonStringBuilder.toString();
	}
	
	/**
	 * Gets <b>all</b> DetailAST subnodes converted to Ast domain object
	 * @param nodeAst
	 * 		DetailAST object representing node of an AST
	 * @return
	 * 		List of Ast child nodes
	 * @throws IOException
	 */
	public static List<Ast> getAstChildNodes(DetailAST nodeAst) {

		int childCount = nodeAst.getChildCount();

		List<Ast> astChildrenList = new ArrayList<>(childCount);
		List<DetailAST> detailAstChildrenAst = new ArrayList<>(childCount);

		if (nodeAst.getChildCount() > 0) {
	
			DetailAST currentNodeAst = nodeAst;
			detailAstChildrenAst.addAll(getChildNodes(currentNodeAst));
		}

		for (DetailAST detailAstChild : detailAstChildrenAst) {
	
			Ast childAst = convertDetailAstToAst(detailAstChild,
					  getAstChildNodes(detailAstChild));
			astChildrenList.add(childAst);
		}

		if (astChildrenList.isEmpty()) {
	
			astChildrenList = null;
		}

		return astChildrenList;
	}

	/**
	 * Converts DetailAST object to domain object Ast
	 * @param nodeAst
	 * 		DetailAST object representing node of an DetailAST
	 * @param astChildrenList
	 * 		List of Ast child nodes
	 * @return Ast object
	 */
	public static Ast convertDetailAstToAst(DetailAST nodeAst,
			  List<Ast> astChildrenList) {
		
		String text = nodeAst.getText();
		int type = nodeAst.getType();
		int lineNo = nodeAst.getLineNo();
		int columnNo = nodeAst.getColumnNo();
		List<Ast> childrenAstList = astChildrenList;
		
		Ast astNode = new Ast(text, type, lineNo, columnNo, childrenAstList);

		return astNode;
	}
	
	public static List<Ast> getAstChildren(JsonNode jsonNode) {
		
		int childCount = jsonNode.size();
		
		List<Ast> astChildrenList = new ArrayList<>(childCount);
		
		JsonNode jsonChildren = jsonNode.findValue("Child nodes:");
		
		if (jsonChildren != null) {
		
			Iterator<JsonNode> jsonChildrenIterator = jsonChildren.getElements();
	
			while (jsonChildrenIterator.hasNext()) {
				JsonNode jsonChildNode = jsonChildrenIterator.next();
				Ast childAst = Utils.convertJsonNodeToAstNode(jsonChildNode,
						  getAstChildren(jsonChildNode));
				
				astChildrenList.add(childAst);
			}
		} 
		
		if (astChildrenList.isEmpty()) {
			
			astChildrenList = null;
		}
		
		return astChildrenList;
	}

	private static List<DetailAST> getChildNodes(DetailAST nodeAst) {

		ArrayList<DetailAST> childrenAst = new ArrayList<>(
				  nodeAst.getChildCount());

		DetailAST currentNodeAst = nodeAst.getFirstChild();
		
		while (currentNodeAst != null) {
			
			childrenAst.add(currentNodeAst);
			currentNodeAst = currentNodeAst.getNextSibling();
		}
		
		return childrenAst;
	}
	
	/**
	 * Converts json-node of AST to Ast domain object.
	 * @param jsonNode
	 * 			node of AST in Json format
	 * @param astChildrenList
	 * 			List of AST child nodes represented in Ast domain object.
	 * @return
	 * 			Ast object
	 */
	public static Ast convertJsonNodeToAstNode(JsonNode jsonNode,
			  List<Ast> astChildrenList) {
		
		String text = jsonNode.findValue("text").toString();
		int type = Integer.parseInt(jsonNode.findValue("type").toString());
		int lineNo = Integer.parseInt(jsonNode.findValue("lineNo").toString());
		int columnNo = Integer.parseInt(jsonNode.findValue("columnNo").toString());
		List<Ast> childrenAstList = astChildrenList;
		
		Ast astNode = new Ast(text, type, lineNo, columnNo, childrenAstList);
		
		return astNode;
	}
	
	/**
	 * Returns Set containing all AST primitive (without child nodes) elements
	 * @param astList
	 * 			List of Ast domain objects.
	 * @return Set of all elements in AST represented in Ast domain object.
	 */
	public static Set<Ast> getAstElementsSet(List<Ast> astList) {
		
		Set<Ast> allAstElementsSet = new LinkedHashSet<>();
		
		for (Ast astNode : astList) {
			
			allAstElementsSet.add(astNode);
			
			if (astNode.getChildren() != null) {
				
				for (Ast astChildNode : astNode.getChildren()) {
					
					allAstElementsSet.add(astChildNode);
				}
				
				allAstElementsSet.addAll(getAstElementsSet(astNode.getChildren()));
			}
		}
		
		return allAstElementsSet;
	}
	
}