package org.spoofax.interpreter.library.jsglr.origin;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;

import java.util.ArrayList;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;

/**
 * Tokens, ignoring layout
 */
public class OriginTokensPrimitive extends AbstractOriginPrimitive {
	
	public OriginTokensPrimitive() {
		super("SSL_EXT_origin_tokens");
	}

	@Override
	public IStrategoTerm call(IContext env, IStrategoTerm origin) {
		ITokenizer tokenizer=getTokenizer(origin);
		int startIndex=getLeftToken(origin).getIndex();
		int endIndex = getRightToken(origin).getIndex();
		ArrayList<IStrategoTerm> tokenStrings=new ArrayList<IStrategoTerm>();
		for (int i = startIndex; i <= endIndex; i++) {
			if(tokenizer.getTokenAt(i).getKind() != IToken.TK_EOF)
				tokenStrings.add(env.getFactory().makeString(tokenizer.getTokenAt(i).toString()));
		}		
		return env.getFactory().makeList(tokenStrings);
	}
}
