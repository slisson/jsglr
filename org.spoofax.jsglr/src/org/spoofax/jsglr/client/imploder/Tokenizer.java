package org.spoofax.jsglr.client.imploder;

import static java.lang.Math.min;
import static org.spoofax.jsglr.client.imploder.IToken.TK_ERROR;
import static org.spoofax.jsglr.client.imploder.IToken.TK_ERROR_KEYWORD;
import static org.spoofax.jsglr.client.imploder.IToken.TK_LAYOUT;
import static org.spoofax.jsglr.client.imploder.IToken.TK_RESERVED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.spoofax.jsglr.client.KeywordRecognizer;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 * @author Karl Trygve Kalleberg <karltk near strategoxt dot org>
 */
public class Tokenizer extends AbstractTokenizer {
	
	private static final double EXPECTED_TOKENS_DIVIDER = 1.3;
	
	private final KeywordRecognizer keywords;
	
	private final String filename;
	
	private final String input;

	private final ArrayList<Token> tokens;
	
	/** Start of the next token. */
	private int startOffset;
	
	/** Line number of the next token. */
	private int line;

	private int offsetAtLineStart;
	
	/**
	 * Creates a new tokenizer for the given
	 * file name (if applicable) and contents.
	 */
	public Tokenizer(KeywordRecognizer keywords, String filename, String input) {
		this.keywords = keywords;
		this.filename = filename;
		this.input = input;
		this.tokens = new ArrayList<Token>((int) (input.length() / EXPECTED_TOKENS_DIVIDER));
		startOffset = 0;
		line = 0;
		offsetAtLineStart = 0;
		// Ensure there's at least one token
		tokens.add(new Token(this, 0, line, 0, 0, -1, TK_RESERVED));
	}
	
	public final String getInput() {
		return input;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public final int getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(int startOffset) {
		assert isAmbigous();
		this.startOffset = startOffset;
	}

	public IToken currentToken() {
		return tokens.size() == 0
			? null
			: tokens.get(tokens.size() - 1);
	}
	
	public int getTokenCount() {
		return tokens.size();
	}
	
	public IToken getTokenAt(int i) {
		return tokens.get(i);
	}
	
	public IToken getTokenAtOffset(int offset) {
		Token key = new Token(this, -1, -1, -1, offset, offset, TK_RESERVED);
		int resultIndex = Collections.binarySearch(tokens, key);
		return resultIndex == -1 ? null : getTokenAt(resultIndex);
	}
	
	public final IToken makeToken(int endOffset, int kind, boolean allowEmptyToken) {
		return makeToken(endOffset, kind, allowEmptyToken, null);
	}
		
	public IToken makeToken(int endOffset, int kind, boolean allowEmptyToken, String errorMessage) {
		assert endOffset <= input.length();
		if (!allowEmptyToken && startOffset > endOffset) // empty token
			return null;
		
		assert endOffset + 1 >= startOffset || (kind == TK_RESERVED && startOffset == 0)
			: "Creating token ending before current start offset";
		
		int offset;
		IToken token = null;
		for (offset = min(startOffset, endOffset); offset < endOffset; offset++) {
			if (input.charAt(offset) == '\n') {
				if (offset - 1 > startOffset)
					token = internalMakeToken(kind, offset - 1, errorMessage);
				internalMakeToken(TK_LAYOUT, offset, errorMessage); // newline
				line++;
				offsetAtLineStart = startOffset;
			}
		}
		
		if (token == null || offset <= endOffset) {
			int oldStartOffset = startOffset;
			token = internalMakeToken(kind, offset, errorMessage);
			if (offset >= oldStartOffset && input.charAt(offset) == '\n') {
				line++;
				offsetAtLineStart = startOffset;
			}
			return token;
		} else {
			return token;
		}
	}

	protected Token internalMakeToken(int kind, int endOffset, String errorMessage) {
		Token result = new Token(this, tokens.size(), line, startOffset - offsetAtLineStart, startOffset, endOffset, kind);
		if (errorMessage != null) result.setError(errorMessage);
		tokens.add(result);
		startOffset = endOffset + 1;
		return result;
	}
	
	public void setErrorMessage(IToken leftToken, IToken rightToken, String message) {
		assert leftToken.getTokenizer() == this && rightToken.getTokenizer() == this;
		for (int i = leftToken.getIndex(), max = rightToken.getIndex(); i <= max; i++) {
			tokens.get(i).setError(message);
		}
	}

	public void tryMakeSkippedRegionToken(int offset) {
		char inputChar = input.charAt(offset);
		
		boolean isInputKeywordChar = KeywordRecognizer.isPotentialKeywordChar(inputChar);
		if (isAtPotentialKeywordEnd(offset, isInputKeywordChar)) {
			if (keywords.isKeyword(toString(startOffset, offset - 1))) {
				makeToken(offset - 1, TK_ERROR_KEYWORD, false, ERROR_SKIPPED_REGION);
			} else {
				makeToken(offset - 1, TK_ERROR, false, ERROR_SKIPPED_REGION);
			}
		}
		if (isAtPotentialKeywordStart(offset, isInputKeywordChar)) {
			if (keywords.isKeyword(toString(startOffset, offset))) {
				makeToken(offset, TK_ERROR_KEYWORD, false, ERROR_SKIPPED_REGION);
			} else {
				makeToken(offset, TK_ERROR, false, ERROR_SKIPPED_REGION);
			}
		}
	}

	private boolean isAtPotentialKeywordEnd(int offset, boolean isInputKeywordChar) {
		if (offset >= 1 && offset > startOffset) {
			char prevChar = input.charAt(offset - 1);
			return 	(isInputKeywordChar && !isKeywordChar(prevChar))
					|| (!isInputKeywordChar && isKeywordChar(prevChar));
		}
		return false;
	}

	private boolean isAtPotentialKeywordStart(int offset, boolean isInputKeywordChar) {
		if (offset + 1 < input.length()) {
			char nextChar = input.charAt(offset + 1);
			if ((isInputKeywordChar && !isKeywordChar(nextChar))
					|| (!isInputKeywordChar && isKeywordChar(nextChar))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determines whether the given character could possibly 
	 * be part of a keyword (as opposed to an operator).
	 */
	private boolean isKeywordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	public void tryMakeLayoutToken(int endOffset, int lastOffset, LabelInfo label) {
		// Create separate tokens for >1 char layout lexicals (e.g., comments)
		if (endOffset > lastOffset + 1 && label.isLexLayout()) {
			if (startOffset <= lastOffset)
				makeToken(lastOffset, TK_LAYOUT, false);
			makeToken(endOffset, TK_LAYOUT, false);
		} else {
			String sort = label.getSort();
			if ("WATERTOKEN".equals(sort) || "WATERTOKENSEPARATOR".equals(sort)) {
				if (getStartOffset() <= lastOffset)
					makeToken(lastOffset, TK_LAYOUT, false);
				makeToken(endOffset, TK_ERROR, false);
			}
		}
	}
	
	/**
	 * Searches towards the left of the given token for the
	 * leftmost layout or error token, returning the current token if
	 * no layout token is found.
	 */
	public static IToken findLeftMostLayoutToken(IToken token) {
		if (token == null) return null;
		ITokenizer tokens = token.getTokenizer();
	loop:
		for (int i = token.getIndex() - 1; i >= 0; i--) {
			IToken neighbour = tokens.getTokenAt(i);
			switch (neighbour.getKind()) {
				case TK_LAYOUT: case TK_ERROR: case TK_ERROR_KEYWORD: break;
				default: break loop;
			}
			token = neighbour;
		}
		return token;
	}
	
	/**
	 * Searches towards the right of the given token for the
	 * rightmost layout or error token, returning the current token if
	 * no layout token is found.
	 */
	public static IToken findRightMostLayoutToken(IToken token) {
		if (token == null) return null;
		ITokenizer tokens = token.getTokenizer();
	loop:
		for (int i = token.getIndex() + 1, count = tokens.getTokenCount(); i < count; i++) {
			IToken neighbour = tokens.getTokenAt(i);
			switch (neighbour.getKind()) {
				case TK_LAYOUT: case TK_ERROR: case TK_ERROR_KEYWORD: break;
				default: break loop;
			}
			token = neighbour;
		}
		return token;
	}
	
	/**
	 * Gets the token with an offset following the given token,
	 * even in an ambiguous token stream.
	 * 
	 * @see #isAmbiguous()
	 */
	public static IToken getTokenAfter(IToken token) {
		if (token == null) return null;
		int nextOffset = token.getEndOffset();
		ITokenizer tokens = token.getTokenizer();
		for (int i = token.getIndex() + 1, max = tokens.getTokenCount(); i < max; i++) {
			IToken result = tokens.getTokenAt(i);
			if (result.getStartOffset() >= nextOffset) return result;
		}
		return null;
	}
	
	/**
	 * Gets the token with an offset preceding the given token,
	 * even in an ambiguous token stream.
	 * 
	 * @see #isAmbiguous()
	 */
	public static IToken getTokenBefore(IToken token) {
		if (token == null) return null;
		int prevOffset = token.getStartOffset();
		ITokenizer tokens = token.getTokenizer();
		for (int i = token.getIndex() - 1; i >= 0; i--) {
			IToken result = tokens.getTokenAt(i);
			if (result.getEndOffset() <= prevOffset) return result;
		}
		return null;
	}
	
	public static int getLength(IToken token) {
		return token.getEndOffset() - token.getStartOffset() + 1;
	}
	
	public String toString(IToken left, IToken right) {
		int startOffset = left.getStartOffset();
		int endOffset = right.getEndOffset();
		return toString(startOffset, endOffset);
	}

	private String toString(int startOffset, int endOffset) {
		return input.substring(startOffset, endOffset + 1);
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append('[');
		for (IToken token : tokens) {
			int offset = token.getStartOffset();
			result.append(input, offset, token.getEndOffset() + 1);
			result.append(',');
		}
		if (tokens.size() > 0)
			result.deleteCharAt(result.length() - 1);
		result.append(']');
		return result.toString();
	}

	protected KeywordRecognizer getKeywordRecognizer() {
		return keywords;
	}

	public Iterator<IToken> iterator() {
		@SuppressWarnings("unchecked") // covariance
		Iterator<IToken> result = (Iterator<IToken>) (Iterator<?>) tokens.iterator();
		return result;
	}

}