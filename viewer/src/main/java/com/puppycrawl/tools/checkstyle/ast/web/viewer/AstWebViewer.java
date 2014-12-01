package com.puppycrawl.tools.checkstyle.ast.web.viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
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
		resultArea.addContainerProperty("Name", String.class, null);
		resultArea.addContainerProperty("Type", String.class, 0);
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
			
				try {
					
					printAst(responseString);
				} catch (IOException e) {
					
					String exceptionTrace = ExceptionUtils.getFullStackTrace(e);
					
					Notification notification = new Notification(exceptionTrace);
					notification.show(Page.getCurrent());
				}
			
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

		private void printAst(String responseString) throws JsonParseException, IOException {

			ObjectMapper mapper = new ObjectMapper();
				
			Iterator<JsonNode> rootNode = mapper.readTree(responseString).iterator();
			
			List<JsonNode> jsonNodesList = new ArrayList<>();
			
			while (rootNode.hasNext()) {
				
				JsonNode jsonNode = rootNode.next();
				jsonNodesList.add(jsonNode);
			}
				

			printTree(jsonNodesList);
		}
		
		private void printTree(List<JsonNode> jsonNodesList) {
			
			HierarchicalContainer container = new HierarchicalContainer();
			
			container.addContainerProperty("Name", String.class, null);
			container.addContainerProperty("Type", String.class, 0);
			container.addContainerProperty("LineNo", Integer.class, 0);
			container.addContainerProperty("ColumnNo", Integer.class, 0);
			
			for (JsonNode jsonNode : jsonNodesList) {
				
				addContent(container, jsonNode);
				
				int id = Integer.parseInt(jsonNode.findValue("id").toString());
				int parentId = Integer.parseInt(jsonNode.findValue("parentId").toString());

				container.setChildrenAllowed(parentId, true);
				container.setParent(id, parentId);
				container.setChildrenAllowed(id, false);
			}
			
			resultArea.setContainerDataSource(container);
			
		}

		@SuppressWarnings("unchecked")
		private void addContent(HierarchicalContainer container, JsonNode jsonNode) {
			
			StringBuilder elementInContainerView = new StringBuilder();
			
			//To view element's name as Object[lineNo x columnNo]
			elementInContainerView.append(jsonNode.findValue("text")).append("[")
				.append(jsonNode.findValue("lineNo"))
				.append("x").append(jsonNode.findValue("columnNo")).append("]");
			
			Item item = container.addItem(Integer.parseInt(jsonNode.findValue("id").toString()));
			
			item.getItemProperty("Name").setValue(elementInContainerView.toString());
			item.getItemProperty("Type").setValue(jsonNode.findValue("type").toString());
			item.getItemProperty("LineNo").setValue(Integer.parseInt(jsonNode
					  .findValue("lineNo").toString()));
			item.getItemProperty("ColumnNo").setValue(Integer.parseInt(jsonNode
					  .findValue("columnNo").toString()));
		}
		
	}

}
