package com.puppycrawl.tools.checkstyle.ast.web.viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;
import org.vaadin.aceeditor.AceTheme;

import com.puppycrawl.tools.Utils;
import com.puppycrawl.tools.json.domain.MyAST;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Represents the viewer of AST. Viewer contains code area for input java source and
 * result area for printing the resulting AST.
 * 
 * @author <a href="mailto:nesterenko-aleksey@list.ru">Aleksey Nesterenko</a>
 *
 */
@Title("AST viewer")
@Theme("reindeer")
public final class AstWebViewer extends UI
{

	private static final long serialVersionUID = 1L;

	private TreeTable resultArea;

	private AceEditor codeArea;
	
	private Button button;

	@Override
	public void init(VaadinRequest request)
	{

		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.setSplitPosition(40);
		VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();
		VerticalLayout leftLayout = new VerticalLayout();
		VerticalLayout rightLayout = new VerticalLayout();
		rightLayout.setSizeFull();

		codeArea = new AceEditor();
		codeArea.setImmediate(true);
		codeArea.setSizeFull();
		codeArea.setMode(AceMode.java);
		codeArea.setTheme(AceTheme.eclipse);
		
		HorizontalLayout bottomLeftLayout = new HorizontalLayout();
		
		
		leftLayout.addComponent(codeArea);
		leftLayout.setSizeFull();

		button = new Button("Process");
		button.addClickListener(new ParsingClickListener());
		button.setImmediate(true);
		
		bottomLeftLayout.addComponent(button);
		leftLayout.addComponent(bottomLeftLayout);
		
		resultArea = new TreeTable("AST");
		resultArea.addContainerProperty("Name", MyAST.class, null);
		resultArea.addContainerProperty("Type", Integer.class, 0);
		resultArea.addContainerProperty("LineNo", Integer.class, 0);
		resultArea.addContainerProperty("ColumnNo", Integer.class, 0);
		resultArea.setSizeFull();
		
		rightLayout.addComponent(resultArea);

		leftLayout.setExpandRatio(codeArea, 50);

		rightLayout.setExpandRatio(resultArea, 50);

		splitPanel.addComponent(leftLayout);
		splitPanel.addComponent(rightLayout);
		
		layout.addComponent(splitPanel);
		
		setContent(layout);

	}

	private final class ParsingClickListener implements Button.ClickListener {
		
		private static final long serialVersionUID = 1L;

		/**
		 * Sends data in codeArea to ast-web-service via Post-request, receives response in
		 * Json format, converts each AST node to MyAST domain object and prints them in TreeTable
		 */
		public void buttonClick(ClickEvent event) {
			
			ResponseEntity<String> responseEntity = sendRequestAndReceiveResponse();
			
			String responseString = responseEntity.getBody();
			
			if (responseEntity.getStatusCode() == HttpStatus.OK) {
			
				printAst(responseString);
			
			} else if (responseEntity.getStatusCode()
						== HttpStatus.NON_AUTHORITATIVE_INFORMATION) {
					
					Notification notification = new Notification(responseString);
					notification.show(Page.getCurrent());
				}
		}
		
		/**
		 * Sends data from codeArea to ast-web-service via Post-request
		 * and receives AST represented in Json format.
		 * @return
		 * 		Response entity containing Json string.
		 */
		private ResponseEntity<String> sendRequestAndReceiveResponse() {
			
			RestTemplate restTemplate = new RestTemplate();
			
			String fullText = codeArea.getValue().toString();
			
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.setContentType(MediaType.TEXT_PLAIN);
			HttpEntity<String> requestBody = new HttpEntity<>(fullText);
			String astServiceUrl = "http://checkstyle-ast-service.appspot.com/ast/json";
			ResponseEntity<String> responseEntity = restTemplate.exchange(astServiceUrl,
					HttpMethod.POST, requestBody, String.class);
			
			return responseEntity;
		}

		/**
		 * Prints AST to TreeTable.
		 * Firstly, converts AST in Json to AST in MyAST domain object,
		 * Then prints in hierarchical tree representation.
		 * @param responseString
		 * 			Json string containing AST.
		 */
		private void printAst(String responseString) {
			
			ObjectMapper mapper = new ObjectMapper();

			JsonNode rootNode = null;

			try {
				
				rootNode = mapper.readTree(responseString);
			} catch (IOException e) {
				
				String exceptionTrace = ExceptionUtils.getFullStackTrace(e);
				Notification notification = new Notification(exceptionTrace);
				notification.show(Page.getCurrent());
			}

			Iterator<JsonNode> iterator = rootNode.getElements();

			List<JsonNode> nodesList = new ArrayList<>(rootNode.size());

			List<MyAST> myAstList = new ArrayList<>(rootNode.size());

			while (iterator.hasNext()) {
				
				JsonNode element = iterator.next();
				nodesList.add(element);
			}

			for (JsonNode jsonNode : nodesList) {
				
				List<MyAST> myAstChildrenList = getMyAstChildren(jsonNode);
				MyAST myNodeAst = Utils.convertJsonNodeToMyAstNode(jsonNode,
						myAstChildrenList);
				
				myAstList.add(myNodeAst);
			}

			Set<MyAST> allAstElementsSet = Utils.getAstElementsSet(myAstList);
			
			Map<MyAST, MyAST> childrenToParentMap = getChildrenToParentMap(myAstList);
			
			printTree(allAstElementsSet, childrenToParentMap);
		}
		
		/**
		 * Returns child nodes of each root node from Json converted to MyAST domain object 
		 * @param jsonNode
		 * 			node of AST in Json format
		 * @return List of child nodes represented in MyAST domain object.
		 */
		private List<MyAST> getMyAstChildren(JsonNode jsonNode) {
			
			int childCount = jsonNode.size();
			
			List<MyAST> myAstChildrenList = new ArrayList<>(childCount);
			
			JsonNode jsonChildren = jsonNode.findValue("Child nodes:");
			
			if (jsonChildren != null) {
			
				Iterator<JsonNode> jsonChildrenIterator = jsonChildren.getElements();
		
				while (jsonChildrenIterator.hasNext()) {
					JsonNode jsonChildNode = jsonChildrenIterator.next();
					MyAST myChildAst = Utils.convertJsonNodeToMyAstNode(jsonChildNode,
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
		 * Returns Map containing pairs child-parent of AST nodes.
		 * @param myAstList
		 * 			List of MyAST domain objects.
		 * @return Map with child-parent pairs.
		 */
		private Map<MyAST, MyAST> getChildrenToParentMap(List<MyAST> myAstList) {
			
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

		/**
		 * Prints AST Tree 
		 * @param myAstSet
		 * 			Set of all objects in tree
		 * @param childrenToParentMap
		 * 			Map containing pairs child-parent
		 */
		private void printTree(Set<MyAST> myAstSet, Map<MyAST, MyAST> childrenToParentMap) {
			
			for (MyAST myAstNode : myAstSet) {
				
				resultArea.addItem(getTreeTableNode(myAstNode), myAstNode);
			}
			
			for (Map.Entry<MyAST, MyAST> entry : childrenToParentMap.entrySet()) {
				
				resultArea.setParent(entry.getKey(), entry.getValue());
			}
			
		}
		
		/**
		 * Returns Object array containing information about node.
		 * @param myAstNode
		 */
		private Object[] getTreeTableNode(MyAST myAstNode) {
			
			return new Object[] { myAstNode, myAstNode.getType(), myAstNode.getLineNo(),
					myAstNode.getColumnNo() };
		}
		
	}

}
