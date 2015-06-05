package com.iwebpp.node.others;

// Wrap basic type in object
public final class BasicBean <T> {
    private T bean;
    
    public T get() {
    	return this.bean;
    }
    
    public void set(T bean) {
    	this.bean = bean;
    }
    
    public BasicBean(T bean) {
    	this.bean = bean;
    }
    
    @SuppressWarnings("unused")
	private BasicBean() {}
}
