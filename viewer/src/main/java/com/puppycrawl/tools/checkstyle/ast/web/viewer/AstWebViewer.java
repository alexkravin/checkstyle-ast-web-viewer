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
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.puppycrawl.tools.ast.web.viewer.Utils;
import com.puppycrawl.tools.ast.web.viewer.json.domain.Ast;
import com.vaadin.annotations.Title;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
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
public final class AstWebViewer extends UI
{

	private static final long serialVersionUID = 1L;

	private TreeTable resultArea;
	
	private TextArea codeArea;
	
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

		codeArea = new TextArea();
		codeArea.setImmediate(true);
		codeArea.setSizeFull();
		
		HorizontalLayout bottomLeftLayout = new HorizontalLayout();
		
		
		leftLayout.addComponent(codeArea);
		leftLayout.setSizeFull();

		button = new Button("Process");
		button.addClickListener(new ParsingClickListener());
		button.setImmediate(true);
		
		bottomLeftLayout.addComponent(button);
		leftLayout.addComponent(bottomLeftLayout);
		
		resultArea = new TreeTable("AST");
		resultArea.addContainerProperty("Name", Ast.class, null);
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
		 * Json format, converts each AST node to Ast domain object and prints them in TreeTable
		 */
		public void buttonClick(ClickEvent event) {
			
			ResponseEntity<String> responseEntity = sendRequestAndReceiveResponse();
			
			String responseString = responseEntity.getBody();
			
			if (responseEntity.getStatusCode() == HttpStatus.OK) {
			
				printAst(responseString);
			
			} else if (responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST) {
					
				Notification notification = new Notification(responseString);
				notification.show(Page.getCurrent());
			}
		}
		
		private ResponseEntity<String> sendRequestAndReceiveResponse() {
			
			RestTemplate restTemplate = new RestTemplate();
			
			
			String fullText = codeArea.getValue().toString();
			
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.setContentType(MediaType.TEXT_PLAIN);
			HttpEntity<String> requestBody = new HttpEntity<>(fullText);
			String astServiceUrl = "http://checkstyle-ast-service.appspot.com/ast/json";
			restTemplate.setErrorHandler(new ResponseErrorHandler() {
					
				@Override
				public boolean hasError(ClientHttpResponse response) throws IOException {
					
					return true;
				}
					
				@Override
				public void handleError(ClientHttpResponse response) throws IOException {
					
					response.getBody();
					
				}
			});
			
			ResponseEntity<String> responseEntity = restTemplate.exchange(astServiceUrl,
					  HttpMethod.POST, requestBody, String.class);
			
			return responseEntity;
		}

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

			List<Ast> astList = new ArrayList<>(rootNode.size());

			while (iterator.hasNext()) {
				
				JsonNode element = iterator.next();
				nodesList.add(element);
			}

			for (JsonNode jsonNode : nodesList) {
				
				List<Ast> astChildrenList = Utils.getAstChildren(jsonNode);
				Ast astNode = Utils.convertJsonNodeToAstNode(jsonNode,
						    astChildrenList);
				
				astList.add(astNode);
			}

			final Set<Ast> allAstElementsSet = Utils.getAstElementsSet(astList);
			
			Map<Ast, Ast> childrenToParentMap = getChildrenToParentMap(astList);
			
			printTree(allAstElementsSet, childrenToParentMap);
		}
		
		private Map<Ast, Ast> getChildrenToParentMap(List<Ast> astList) {
			
			Map<Ast, Ast> childrenToParentMap = new LinkedHashMap<>();
					
			for (Ast astNode : astList) {
				
				if (astNode.getChildren() != null) {
					
					for (Ast astChildNode : astNode.getChildren()) {
						
						childrenToParentMap.put(astChildNode, astNode);
					}
					
					childrenToParentMap.putAll(getChildrenToParentMap(astNode.getChildren()));
				}
			}
			
			return childrenToParentMap;
		}

		private void printTree(Set<Ast> astSet, Map<Ast, Ast> childrenToParentMap) {
			
			HierarchicalContainer container = new HierarchicalContainer();
			
			container.addContainerProperty("Name", Ast.class, null);
			container.addContainerProperty("Type", Integer.class, 0);
			container.addContainerProperty("LineNo", Integer.class, 0);
			container.addContainerProperty("ColumnNo", Integer.class, 0);
			
			for (Ast node : astSet) {
				
				addContent(container, node);
			}
			
			for (Map.Entry<Ast, Ast> entry : childrenToParentMap.entrySet()) {
				
				container.setParent(entry.getKey(), entry.getValue());
			}
			
			resultArea.setContainerDataSource(container);
			
		}

		@SuppressWarnings("unchecked")
		private void addContent(HierarchicalContainer container, Ast astNode) {
			
			Item item = container.addItem(astNode);
			
			item.getItemProperty("Name").setValue(astNode);
			item.getItemProperty("Type").setValue(astNode.getType());
			item.getItemProperty("LineNo").setValue(astNode.getLineNo());
			item.getItemProperty("ColumnNo").setValue(astNode.getColumnNo());
		}
		
	}

}
