package com.puppycrawl.tools.ast.web.viewer;

import java.io.IOException;
import java.net.URLDecoder;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import antlr.ANTLRException;

import com.puppycrawl.tools.ast.web.viewer.Utils;
import com.puppycrawl.tools.checkstyle.api.DetailAST;

/**
 * Provides convertation of source code to AST and serializes it to Json format.
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
	 */
	@RequestMapping(value = "/ast/json", consumes = "text/plain", produces = "application/json",
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
					HttpStatus.BAD_REQUEST);
		}
		
		return responseEntity;
	}

}