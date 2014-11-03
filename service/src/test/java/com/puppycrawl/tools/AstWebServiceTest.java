package com.puppycrawl.tools;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * 
 * @author <a href="mailto:nesterenko-aleksey@list.ru">Aleksey Nesterenko</a>
 *
 */
public class AstWebServiceTest {
    
	@Test
    public void astToJsonConvertationTest() throws JsonGenerationException,
    	JsonMappingException, RecognitionException, TokenStreamException, IOException {
		
		URL testSource = getClass().getResource("/Testsource");
		URL resultJson = getClass().getResource("/ast.json");
		
		String expectedResult = getWriterAsString(resultJson);
		String javaSource = getWriterAsString(testSource);
		
		ResponseEntity<String> jsonFromAst = AstWebServiceController.javaSourceToJson(javaSource);
		assertEquals(expectedResult.trim(), jsonFromAst.getBody());
    }

	private static String getWriterAsString(URL resultJson) throws IOException {
		BufferedReader generatedJsonReader = new BufferedReader(new InputStreamReader(resultJson.
				openStream()));
		
		Writer writer = new StringWriter();
		IOUtils.copy(generatedJsonReader, writer);
		writer.close();
		
		return writer.toString();
	}
	
}