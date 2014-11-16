package com.puppycrawl.tools.ast.web.viewer.json.domain;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;


/**
 * @author <a href="mailto:nesterenko-aleksey@list.ru">Aleksey Nesterenko</a>
 */
public final class Ast {
	
	private final String text;

	private final int type;

	private final int lineNo;
	
	private final int columnNo;
	
	private final List<Ast> childrenAstList;
	
	public Ast(String text, int type, int lineNo, int columnNo, List<Ast>
			  childrenAstList) {
		
		this.text = text;
		this.type = type;
		this.lineNo = lineNo;
		this.columnNo = columnNo;
		this.childrenAstList = childrenAstList;
	}
	
	public String getText() {
		return text;
	}

	public int getLineNo() {
		return lineNo;
	}

	public int getColumnNo() {
		return columnNo;
	}

	public int getType() {
		return type;
	}
	
	@JsonProperty(value = "Child nodes:")
	@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
	public List<Ast> getChildren() {
		return childrenAstList;
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
				+ ((childrenAstList == null) ? 0 : childrenAstList.hashCode());
		result = prime * result + columnNo;
		result = prime * result + lineNo;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Ast other = (Ast) obj;
		if (childrenAstList == null) {
			if (other.childrenAstList != null) {
				return false;
			}
		} else if (!childrenAstList.equals(other.childrenAstList)) {
			return false;
		}
		if (columnNo != other.columnNo) {
			return false;
		}
		if (lineNo != other.lineNo) {
			return false;
		}
		if (text == null) {
			if (other.text != null) {
				return false;
			}
		} else if (!text.equals(other.text)) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}

}
