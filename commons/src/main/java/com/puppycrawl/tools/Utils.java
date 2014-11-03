package com.puppycrawl.tools;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.grammars.CommentListener;
import com.puppycrawl.tools.checkstyle.grammars.GeneratedJavaLexer;
import com.puppycrawl.tools.checkstyle.grammars.GeneratedJavaRecognizer;
import com.puppycrawl.tools.json.domain.MyAST;

/**
 * Utilities class containing methods like converting java source to DetailAST,
 * converting DetailAST to MyAST domain object, MyAST to Json, Json to MyAST, etc.
 * 
 * @author <a href="mailto:nesterenko-aleksey@list.ru">Aleksey Nesterenko</a>
 */
public final class Utils {

	/**
	 * Parses a file and returns the parse tree.
	 * 
	 * @return the root node of the parse tree
	 * @throws TokenStreamException
	 * @throws RecognitionException
	 * @throws ANTLRException
	 *             if the file is not a Java source
	 */
	public final static DetailAST parse(String fullText) throws RecognitionException,
			TokenStreamException {
		
		Reader sr = new StringReader(fullText);
		GeneratedJavaLexer lexer = new GeneratedJavaLexer(sr);
		lexer.setFilename("test");
		
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
		parser.setFilename("test");
		parser.setASTNodeClass(DetailAST.class.getName());
		parser.compilationUnit();

		
		return (DetailAST) parser.getAST();
	}
	
	/**
	 * Gets Json representation of an AST object.
	 * @param astTree
	 * 			AST object.
	 * @return
	 * 			String containing Json representation.
	 * @throws IOException
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	public final static String getJsonFromAst(DetailAST astTree) throws IOException,
			JsonGenerationException, JsonMappingException,
			JsonProcessingException {
		
		StringBuilder jsonStringBuilder = new StringBuilder();
		
		DetailAST currentNodeAst = astTree;
		
		ObjectMapper mapper = new ObjectMapper();
		jsonStringBuilder.append("{");
		
		while (currentNodeAst != null) {
			List<MyAST> myAstChildrenList = new ArrayList<>(currentNodeAst.getChildCount());
			myAstChildrenList = getMyAstChildNodes(currentNodeAst);
			MyAST myNodeAst = convertDetailAstToMyAst(currentNodeAst, myAstChildrenList);
			
			jsonStringBuilder.append("\"" + myNodeAst + "\"" + ":" + " [");
			String json = mapper.writer().withDefaultPrettyPrinter().
					writeValueAsString(myNodeAst);
			jsonStringBuilder.append(json);
			jsonStringBuilder.append("]");
			
			DetailAST nextNodeAst = currentNodeAst.getNextSibling();
			if (nextNodeAst == null) {
				currentNodeAst = null;
			}
			
			if (currentNodeAst != null) {
				
				currentNodeAst = currentNodeAst.getFirstChild();
			
				astTree = astTree.getNextSibling();
				currentNodeAst = astTree;
			
				jsonStringBuilder.append(",");
			}
			
		}
		
		jsonStringBuilder.append("}");
		
		return jsonStringBuilder.toString();
	}
	
	/**
	 * Gets <b>all</b> DetailAST subnodes converted to MyAST domain object
	 * @param nodeAst
	 * 		DetailAST object representing node of an AST
	 * @return
	 * 		List of MyAST child nodes
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public final static List<MyAST> getMyAstChildNodes(DetailAST nodeAst) throws
			JsonGenerationException, JsonMappingException, IOException {

		int childCount = nodeAst.getChildCount();

		List<MyAST> myAstChildrenList = new ArrayList<>(childCount);
		List<DetailAST> detailAstChildrenAst = new ArrayList<>(childCount);

		if (nodeAst.getChildCount() > 0) {
	
			DetailAST currentNodeAst = nodeAst;
			detailAstChildrenAst.addAll(getChildNodes(currentNodeAst));
		}

		for (DetailAST childAst : detailAstChildrenAst) {
	
			MyAST myChildAst = convertDetailAstToMyAst(childAst, getMyAstChildNodes(childAst));
			myAstChildrenList.add(myChildAst);
		}

		if (myAstChildrenList.isEmpty()) {
	
			myAstChildrenList = null;
		}

		return myAstChildrenList;
	}

	/**
	 * Converts DetailAST object to domain object MyAST
	 * @param nodeAst
	 * 		DetailAST object representing node of an AST
	 * @param myAstChildrenList
	 * 		List of MyAST child nodes
	 * @return MyAST object
	 */
	public final static MyAST convertDetailAstToMyAst(DetailAST nodeAst,
			List<MyAST> myAstChildrenList) {

		MyAST myAstNode = new MyAST();

		myAstNode.setText(nodeAst.getText());
		myAstNode.setType(nodeAst.getType());
		myAstNode.setLineNo(nodeAst.getLineNo());
		myAstNode.setColumnNo(nodeAst.getColumnNo());
		myAstNode.setChildren(myAstChildrenList);

		return myAstNode;
	}

	/**
	 * Gets all the children one level below on the current parent node.
	 * 
	 * @param nodeAst
	 *            the parent node.
	 * @return list of children one level below on the current parent node
	 *         (aNode).
	 */
	private static List<DetailAST> getChildNodes(DetailAST nodeAst) {

		ArrayList<DetailAST> childrenAst = new ArrayList<DetailAST>(
				nodeAst.getChildCount());

		DetailAST currentNodeAst = nodeAst.getFirstChild();
		
		while (currentNodeAst != null) {
			childrenAst.add(currentNodeAst);
			currentNodeAst = currentNodeAst.getNextSibling();
		}
		
		return childrenAst;
	}
	
	/**
	 * Converts json-node of AST to MyAST domain object.
	 * @param jsonNode
	 * 			node of AST in Json format
	 * @param myAstchildrenList
	 * 			List of AST child nodes represented in MyAST domain object.
	 * @return
	 * 			MyAST object
	 */
	public final static MyAST convertJsonNodeToMyAstNode(JsonNode jsonNode,
			List<MyAST> myAstchildrenList) {
		
		MyAST myAstNode = new MyAST();
		
		myAstNode.setLineNo(Integer.parseInt(jsonNode.findValue("lineNo").toString()));
		myAstNode.setColumnNo(Integer.parseInt(jsonNode.findValue("columnNo").toString()));
		myAstNode.setType(Integer.parseInt(jsonNode.findValue("type").toString()));
		myAstNode.setText(jsonNode.findValue("text").toString());
		myAstNode.setChildren(myAstchildrenList);
		
		return myAstNode;
	}
	
	/**
	 * Returns child nodes of each root node from Json converted to MyAST domain object 
	 * @param jsonNode
	 * 			node of AST in Json format
	 * @return List of child nodes represented in MyAST domain object.
	 */
	public final static List<MyAST> getMyAstChildren(JsonNode jsonNode) {
		
		int childCount = jsonNode.size();
		
		List<MyAST> myAstChildrenList = new ArrayList<>(childCount);
		
		JsonNode jsonChildren = jsonNode.findValue("Child nodes:");
		
		if (jsonChildren != null) {
		
			Iterator<JsonNode> jsonChildrenIterator = jsonChildren.getElements();
	
			while (jsonChildrenIterator.hasNext()) {
				JsonNode jsonChildNode = jsonChildrenIterator.next();
				MyAST myChildAst = convertJsonNodeToMyAstNode(jsonChildNode,
						getMyAstChildren(jsonChildNode));
				
				myAstChildrenList.add(myChildAst);
			}
		} 
		
		if (myAstChildrenList.isEmpty()) {
			
			myAstChildrenList = null;
		}
		
		return myAstChildrenList;
	}
	
	/**
	 * Returns Set containing all AST primitive (without child nodes) elements
	 * @param myAstList
	 * 			List of MyAST domain objects.
	 * @return Set of all elements in AST represented in MyAST domain object.
	 */
	public static Set<MyAST> getAstElementsSet(List<MyAST> myAstList) {
		
		Set<MyAST> allAstElementsSet = new LinkedHashSet<>();
		
		for (MyAST myAstNode : myAstList) {
			
			allAstElementsSet.add(myAstNode);
			
			if (myAstNode.getChildren() != null) {
				
				for (MyAST myAstChildNode : myAstNode.getChildren()) {
					
					allAstElementsSet.add(myAstChildNode);
				}
				
				allAstElementsSet.addAll(getAstElementsSet(myAstNode.getChildren()));
			}
		}
		
		return allAstElementsSet;
	}
	
	/**
	 * Returns Map containing pairs child-parent of AST nodes.
	 * @param myAstList
	 * 			List of MyAST domain objects.
	 * @return Map with child-parent pairs.
	 */
	public static Map<MyAST, MyAST> getChildrenToParentMap(List<MyAST> myAstList) {
		
		Map<MyAST, MyAST> childrenToParentMap = new LinkedHashMap<>();
				
		for (MyAST myAstNode : myAstList) {
			
			if (myAstNode.getChildren() != null) {
				
				for (MyAST myAstChildNode : myAstNode.getChildren()) {
					
					childrenToParentMap.put(myAstChildNode, myAstNode);
				}
				
				childrenToParentMap.putAll(getChildrenToParentMap(myAstNode.getChildren()));
			}
		}
		
		return childrenToParentMap;
	}
	

}