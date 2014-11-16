package com.puppycrawl.tools.ast.web.viewer;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

/**
 * 
 * @author <a href="mailto:nesterenko-aleksey@list.ru">Aleksey Nesterenko</a>
 *
 */
public class AstWebServiceTest {
    
	@Test
    public void astToJsonConvertationTest() throws IOException {
		
		URL testInput = getClass().getResource("/com/puppycrawl/tools/ast/web/viewer/"
				  + "AstWebServiceTestInput.java");
		URL testExpected = getClass().getResource("/com/puppycrawl/tools/ast/web/viewer/"
				  + "AstWebServiceTestExpected.json");
		
		String javaSource = getFileAsString(testInput);
		String expectedJson = getFileAsString(testExpected);
		
		ResponseEntity<String> jsonFromAst = AstWebServiceController.javaSourceToJson(javaSource);
		assertEquals(expectedJson.trim(), jsonFromAst.getBody());
    }

	private static String getFileAsString(URL resultJson) throws IOException {
		
		BufferedReader generatedJsonReader = new BufferedReader(new InputStreamReader(resultJson.
				  openStream()));
		
		Writer writer = new StringWriter();
		IOUtils.copy(generatedJsonReader, writer);
		writer.close();
		
		return writer.toString();
	}
	
}