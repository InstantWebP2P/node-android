package com.iwebpp.node.others;

//@desc three states: -1,0,1 map to null,false,true
public enum TripleState {
    MAYBE    (-1),
    FALSE    ( 0),
    TRUE     ( 1);
    
    private int state;
    private TripleState(int state) {
    	this.state = state;
    }
    public int state() {
    	return this.state;
    }
    public String toString() {
    	if (this.state ==  0) return "false";
    	if (this.state ==  1) return "true";
    	                      return "maybe";
    }
}

