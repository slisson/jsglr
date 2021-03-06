package org.spoofax.jssglr.client;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jssglr.client.services.NativeTermFactory;
import org.spoofax.jssglr.client.services.Parser;
import org.spoofax.terms.TermFactory;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.webworker.client.DedicatedWorkerEntryPoint;

/**
 *
 * @author Karl Trygve Kalleberg &lt;karltk@spoofax.org&gt;
 *
 * Licensed under MPL 1.1/GPL 2.0/LGPL 2.1
 */
public class Worker extends DedicatedWorkerEntryPoint {

	private Parser parser;
	private NativeTermFactory factory;
	//private STRJSNativeFactory factory;
	//private JavaScriptObject jsfactory;

	private native void nativeOnWorkerLoad() /*-{
		var oneself = this;

		self.spoofax = {};
		self.spoofax.factory = oneself.@org.spoofax.jssglr.client.Worker::getFactory()();
		self.spoofax.createParser = function(lang) {
			return oneself.@org.spoofax.jssglr.client.Worker::createParser(Ljava/lang/String;)(lang);
		}
		self.spoofax.createParserSync = function(ast) {
			return oneself.@org.spoofax.jssglr.client.Worker::createParserSync(Lorg/spoofax/interpreter/terms/IStrategoTerm;)(ast);
		}
		
		self.spoofax.cliinvoke = function(args) {
			@org.spoofax.jssglr.client.JSMain::main([Ljava/lang/String;)(args);
		}
		
	}-*/;

	@Override
	public void onWorkerLoad() {
		//factory = new STRJSNativeFactory();
		factory = new NativeTermFactory();
		nativeOnWorkerLoad();
	}
	
	public JavaScriptObject getFactory() {
		return factory.exposeFactory();
	}

	private JavaScriptObject createParser(String grammarUrl) {
		parser = new Parser(new TermFactory());
		return parser.asyncInitializeFromURL(grammarUrl);
	}

	private JavaScriptObject createParserSync(IStrategoTerm ast) {
		parser = new Parser(factory);
		return parser.initializeFromTable(ast);
	}
}
