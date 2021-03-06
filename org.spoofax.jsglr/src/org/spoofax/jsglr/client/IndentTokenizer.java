package org.spoofax.jsglr.client;

import java.io.IOException;

import org.spoofax.jsglr.shared.ArrayDeque;

/*
 * Creates tokens DEDENT and INDENT based on indentation.
 * Use: use this class for whitespace sensitive languages like python.
 * In the grammar: Use [244] and [245] as chars indicating indent and dedent respectively
 */
public class IndentTokenizer {
        
    private static final char INDENT_TOK = 244; //char used to represent indentation in syntax definition
    private static final char DEDENT_TOK = 245; //char used to represent 'dedentation' in syntax definition
    private final ArrayDeque<Integer> indentStack;
    private final boolean strictMode;
    private final IndentationHandler myIndentHandler;
    private int dedentCount;
    private boolean indentShift;
    
    /*
     * Creates indent and dedent tokens during parsing
     */
    public IndentTokenizer(IndentationHandler indentInfo, boolean isStrictMode)
    {
        indentStack = new ArrayDeque<Integer>();
        this.strictMode=isStrictMode;
        myIndentHandler = indentInfo;
        initEvaluationVariables();
    }
 
    private void initEvaluationVariables() {
        myIndentHandler.initEvaluationVariables();
        indentStack.clear();
        indentStack.push(0);
        resetForNextLine();
    }
    
    private void resetForNextLine() {
        indentShift=false;
        dedentCount=0;
    }

    private void updateIndentFields() {
        int diff=myIndentHandler.getIndentValue() - indentStack.peek();
        while(diff<0)
        {
            indentStack.pop();
            dedentCount+=1;
            diff = myIndentHandler.getIndentValue() - indentStack.peek();
        }
        if(diff>0){
            indentStack.push(diff);
            indentShift=true;
        }
    }
    /*
     * Recognizes an indent or dedent at the new line and tries to parse a indent-token or dedent-token
     */
    public void handleIndentShifts(SGLR parser) throws IOException, ParseException, InterruptedException
    {
        int curTok=parser.getCurrentToken();
        if(myIndentHandler.lineMarginEnded()){
            updateIndentFields();
            parseIndentation(parser);
            resetForNextLine();
            parser.setCurrentToken(curTok);
        }
    }    

    private void parseIndentation(SGLR parser) throws ParseException,
            IOException, InterruptedException {
        ArrayDeque<Frame> oldActiveStacks = parser.activeStacks;
        if(dedentCount > 0 && indentShift){
            //TODO: warnings?
            if(strictMode)
                throw new ParseException(parser, "Indentation inconsistent");
        }        
        for (int i = 0; i < dedentCount; i++) {
            parser.setCurrentToken(DEDENT_TOK);                         
            parser.doParseStep();
        }
        if(indentShift){
            parser.setCurrentToken(INDENT_TOK);                         
            parser.doParseStep();
        }
        if(parser.activeStacks.size()==0)//if indent and dedent tokens are not expected, ignore them as layout 
            parser.activeStacks.addAll(oldActiveStacks);
    }
}
