package com.puppycrawl.tools.json.domain;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;


/**
 * Domain class, based on 
 * <a href="http://checkstyle.sourceforge.net/apidocs/com/puppycrawl/tools/checkstyle/api/DetailAST.html">
 * DetailAST</a>
 * 
 * @author <a href="mailto:nesterenko-aleksey@list.ru">Aleksey Nesterenko</a>
 */
public final class MyAST {
	
	private String text;

	private int type;

	private int lineNo;
	
	private int columnNo;
	
	private List<MyAST> childrenAst;
	
	public String getText() {
		return text;
	}

	public int getLineNo() {
		return lineNo;
	}

	public void setLineNo(int lineNo) {
		this.lineNo = lineNo;
	}

	public int getColumnNo() {
		return columnNo;
	}

	public void setColumnNo(int columnNo) {
		this.columnNo = columnNo;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	public void setChildren(List<MyAST> childrenAst) {
		this.childrenAst = childrenAst;
	}
	
	@JsonProperty(value = "Child nodes:")
	@JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
	public List<MyAST> getChildren() {
		return childrenAst;
	}

	@Override
	public String toString()
	{
		return this.getText() + "[" + getLineNo() + "x" + getColumnNo() + "]";
	}
	
	@Override
	public int hashCode() {
		
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((childrenAst == null) ? 0 : childrenAst.hashCode());
		result = prime * result + columnNo;
		result = prime * result + lineNo;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + type;
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MyAST other = (MyAST) obj;
		if (childrenAst == null) {
			if (other.childrenAst != null)
				return false;
		} else if (!childrenAst.equals(other.childrenAst))
			return false;
		if (columnNo != other.columnNo)
			return false;
		if (lineNo != other.lineNo)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (type != other.type)
			return false;
		
		return true;
	}


}
