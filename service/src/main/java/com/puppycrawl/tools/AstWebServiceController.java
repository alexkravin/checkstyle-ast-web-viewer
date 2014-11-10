package com.puppycrawl.tools;

import java.io.IOException;
import java.net.URLDecoder;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

/**
 * Service which provides convertation of source code to AST and serializes it to Json format.
 * Firstly, builds an AST object from a java source code and then serialize it to Json
 * using <a href="https://github.com/FasterXML/jackson">Jackson</a> library.
 * 
 * @author <a href="mailto:nesterenko-aleksey@list.ru">Aleksey Nesterenko</a>
 */
@Controller
public final class AstWebServiceController {
	
	/**
	 * Handles <b>POST</b> request containing java source file (in String representation).
	 * Builds an AST object from these sources and converts AST to Json.
	 * @param fullText 
	 * 			String representation of java source file.
	 * @return	Json representation of AST.
	 * @throws RecognitionException
	 * @throws TokenStreamException
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@RequestMapping(value = "/ast/json", consumes="text/plain", produces="application/json",
			method = RequestMethod.POST)
	@ResponseBody
	public static ResponseEntity<String> javaSourceToJson(@RequestBody String fullText) {
		
		ResponseEntity<String> responseEntity = null;
		String responseString = "";
		
		try {
			
			URLDecoder.decode(fullText, "UTF-8");
			DetailAST astTree = Utils.parse(fullText);
			responseString = Utils.toJson(astTree);
			responseEntity = new ResponseEntity<>(responseString, HttpStatus.OK);
		} catch (IOException | ANTLRException e) {
			
			responseString = ExceptionUtils.getFullStackTrace(e);
			responseEntity = new ResponseEntity<String>(responseString,
					HttpStatus.NON_AUTHORITATIVE_INFORMATION);
		}
		
		return responseEntity;
	}

}