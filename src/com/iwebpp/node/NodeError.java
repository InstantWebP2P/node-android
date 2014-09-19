package com.iwebpp.node;

@SuppressWarnings("serial")
public final class NodeError 
extends RuntimeException {
    private String code;
    
    /**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	public NodeError(String code, String message) {
    	super(message);
    	this.code = code;
    }
    
	public NodeError(String message) {
    	super(message);
    	this.code = "";
    }
	
	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(4096);
        sb.append("code: ");
        sb.append(code);
        sb.append(", message: ");
        sb.append(super.getMessage());
        return sb.toString();
    }

}
