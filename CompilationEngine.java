import java.io.IOException;
import java.util.HashMap;


/**
 * Effects the actual compilation output.
 * Gets its input from a JackTokenizer and emits its parsed structure into an output file.
 * The output is generated by a series of compileXxx() routines,
 *  one for every syntactic element Xxx of the Jack grammar.
 * The contract between these routines is that each compileXxx() routine should read
 *  the syntactic construct Xxx from the input, output the parsing of Xxx,
 *  and advance the tokenizer exactly beyond Xxx.
 * Thus, compileXxx() may only be called if indeed Xxx is the next syntactic element of the input.
 */
class CompilationEngine {
    
    
    //*** Data Members ***//

    // Tokenizer of the input jack code file.
    private JackTokenizer tokenizer;

    // Writes to the output xml file.
    private VMWriter writer;
    
    // Keeps correspondence between identifiers and their properties (kind, type, index) on the VM
    private SymbolTable symbolTable;

    // The name of the currently compiled class
    private String className;

    // Keeps correspondence between binary operators (chars) and their enum constant representation
    private HashMap<Character, VMWriter.Command> binaryOps;

    // Counter for the number of While clauses met in a subroutine, for creating unique labels
    private int whileLabelCount;
    
    // Counter for the number of If clauses met in a subroutine, for creating unique labels
    private int ifLabelCount;
    
    
    /**
     * Creates a new compilation engine with the given input and output.
     * The next routine called must be compileClass().
     *
     * @param input the tokenizer object of the input file.
     * @param writer the VMWriter object of the output file.
     */
    CompilationEngine(JackTokenizer input, VMWriter writer, SymbolTable table) {

        this.tokenizer = input;
        this.writer = writer;
        this.symbolTable = table;
        
        initBinaryOps();
    }
    
    
    /**
     * Initializes the binary operators Map.
     */
    private void initBinaryOps() {
        binaryOps = new HashMap<>();
        
        binaryOps.put('+', VMWriter.Command.ADD);
        binaryOps.put('-', VMWriter.Command.SUB);
        binaryOps.put('=', VMWriter.Command.EQ);
        binaryOps.put('<', VMWriter.Command.LT);
        binaryOps.put('>', VMWriter.Command.GT);
        binaryOps.put('|', VMWriter.Command.OR);
        binaryOps.put('&', VMWriter.Command.AND);
    }


    /**
     * Compiles a complete class.
     */
    void compileClass() throws IOException {
        tokenizer.advance(); // skip class keyword
        className = tokenizer.identifier();
        tokenizer.advanceTwice(); // skip to classVarDec*

        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {

            switch (tokenizer.keyword()) {
                case STATIC:
                case FIELD:
                    compileClassVarDec();
                    break;
                case CONSTRUCTOR:
                case FUNCTION:
                case METHOD:
                    compileSubroutine();
                    break;
            }
        }
    }


    /**
     * A helper method for compiling variable declarations.
     */
    private void compileVarList(String type, SymbolTable.Kind kind) throws IOException {

        while (tokenizer.symbol() == ',') {

            tokenizer.advance(); //,

            String name =  tokenizer.identifier();

            tokenizer.advance(); // varName
            symbolTable.define(name, type, kind);
        }
    }

    
    /**
     * Compiles a static variable declaration or a field declaration.
     */
    private void compileClassVarDec() throws IOException {

        String kind =  tokenizer.keyword().toString(); // static|field
        tokenizer.advance();

        String type;
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
            type = tokenizer.keyword().toString().toLowerCase();
        }
        else {
            type = tokenizer.identifier();
        }
        tokenizer.advance();

        String varName =  tokenizer.identifier();
        tokenizer.advance();

        symbolTable.define(varName, type, SymbolTable.Kind.valueOf(kind));
        compileVarList(type, SymbolTable.Kind.valueOf(kind));

        tokenizer.advance(); //;
    }
    
    
    /**
     * A helper method for compiling the body of a subroutine.
     *
     * @param name the name of the subroutine to compile.
     * @param type the type of the subroutine to compile (constructor/method/function).
     */
    private void compileSubroutineBody(String name, JackTokenizer.Keyword type) throws IOException {
        tokenizer.advance(); // {
    
        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD
                && tokenizer.keyword() == JackTokenizer.Keyword.VAR) {
            compileVarDec();
        }
        writer.writeFunction(className + "." + name, symbolTable.varCount(SymbolTable.Kind.VAR));
    
        if (type == JackTokenizer.Keyword.CONSTRUCTOR) {
            writer.writePush(VMWriter.Segment.CONST, symbolTable.varCount(SymbolTable.Kind.FIELD));
            writer.writeCall("Memory.alloc", 1);
            writer.writePop(VMWriter.Segment.POINTER, 0);
        } else if (type == JackTokenizer.Keyword.METHOD) {
            writer.writePush(VMWriter.Segment.ARG, 0);
            writer.writePop(VMWriter.Segment.POINTER, 0);
        }
        compileStatements();
    
        tokenizer.advance(); // }
    }
    
    
    /**
     * Compiles a complete method, constructor, or function.
     */
    private void compileSubroutine() throws IOException {
        symbolTable.startSubroutine();
        whileLabelCount = 0;
        ifLabelCount = 0;
    
        JackTokenizer.Keyword currentSubroutineType = tokenizer.keyword();
        
        tokenizer.advanceTwice(); // skip to subroutineName

        String currentSubroutineName = tokenizer.identifier();
        tokenizer.advanceTwice(); // skip to parameterList
        
        if (currentSubroutineType == JackTokenizer.Keyword.METHOD) {
            symbolTable.define("this", className, SymbolTable.Kind.ARG);
        }

        compileParameterList();
        tokenizer.advance(); // )

        // subroutineBody
        compileSubroutineBody(currentSubroutineName, currentSubroutineType);
    }
    
    
    /**
     * A helper method for compileParameterList().
     * Compiles a single parameter, i.e. defines it in the symbol table.
     */
    private void defineParameter() throws IOException {
        String type;
        
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
            type = tokenizer.keyword().toString().toLowerCase();
        }
    
        else {
            type = tokenizer.identifier();
        }
        tokenizer.advance();
        String name =  tokenizer.identifier();
        tokenizer.advance();
    
        symbolTable.define(name, type, SymbolTable.Kind.ARG);
    }
    
    
    /**
     * Compiles a (possibly empty) parameter list, not including the enclosing brackets "()".
     */
    private void compileParameterList() throws IOException {

        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL) {

            defineParameter();

            while (tokenizer.symbol() == ',') {
                tokenizer.advance();
                defineParameter();
            }
        }
    }
    
    
    /**
     * Compiles a variable declaration
     */
    private void compileVarDec() throws IOException {

        tokenizer.advance(); // var

        String type;
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
            type = tokenizer.keyword().toString().toLowerCase();
        }
        else {
            type = tokenizer.identifier();
        }

        tokenizer.advance(); // type

        String varName =  tokenizer.identifier();
        tokenizer.advance(); // varName

        symbolTable.define(varName, type, SymbolTable.Kind.VAR);
        compileVarList(type, SymbolTable.Kind.VAR);

        tokenizer.advance(); //;
    }
    
    
    /**
     * Compiles a sequence of statements, not including the enclosing curly brackets "{}".
     */
    private void compileStatements() throws IOException {

        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {

            switch (tokenizer.keyword()) {
                case LET:
                    compileLet();
                    break;
                case IF:
                    compileIf();
                    break;
                case WHILE:
                    compileWhile();
                    break;
                case DO:
                    compileDo();
                    break;
                case RETURN:
                    compileReturn();
                    break;
            }
        }
    }


    /**
     * A helper method for compiling subroutine calls.
     */
    private void compileSubroutineCall(String identifier) throws IOException {

        String subroutineName;
        String subroutineClass = identifier;
        int nArgs = 0;

        if (tokenizer.symbol() == '.') {
            tokenizer.advance(); //.
            subroutineName = tokenizer.identifier();
            tokenizer.advance();
    
            if (symbolTable.kindOf(identifier) != SymbolTable.Kind.NONE) {
                
                writer.writePush(symbolTable.kindOf(identifier).toSegment(), symbolTable.indexOf(identifier));
                nArgs++;
                subroutineClass = symbolTable.typeOf(identifier);
            }
        }
        else {
            subroutineName = identifier;
            subroutineClass = className;
            writer.writePush(VMWriter.Segment.POINTER, 0);
            nArgs++;
        }
        tokenizer.advance(); // (

        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ')') {
            nArgs += compileExpressionList();
        }
        writer.writeCall(subroutineClass + "." + subroutineName, nArgs);
    }
    
    
    /**
     * Compiles a do statement.
     */
    private void compileDo() throws IOException {

        tokenizer.advance(); // do keyword

        String identifier = tokenizer.identifier(); // subroutineName|className|varName
        tokenizer.advance();

        compileSubroutineCall(identifier);

        writer.writePop(VMWriter.Segment.TEMP, 0);

        tokenizer.advanceTwice(); // ) and ;
    }
    
    
    /**
     * Compiles a let statement.
     */
    private void compileLet() throws IOException {

        tokenizer.advance(); // skip let

        String varName = tokenizer.identifier();
        boolean isArray = false;

        tokenizer.advance(); // skip varName
        
        if (tokenizer.symbol() == '[') {
            isArray = true;
            writer.writePush(symbolTable.kindOf(varName).toSegment(), symbolTable.indexOf(varName));
            
            tokenizer.advance(); // [
            compileExpression();
            tokenizer.advance(); // skip ]
            
            writer.writeArithmetic(VMWriter.Command.ADD);
        }
        tokenizer.advance(); // skip =

        compileExpression();
        
        if (isArray) {
            writer.writePop(VMWriter.Segment.TEMP, 1);
            writer.writePop(VMWriter.Segment.POINTER, 1);
            writer.writePush(VMWriter.Segment.TEMP, 1);
            writer.writePop(VMWriter.Segment.THAT, 0);
        }
        else {
            writer.writePop(symbolTable.kindOf(varName).toSegment(), symbolTable.indexOf(varName));
        }
        tokenizer.advance(); // skip ;
    }
    
    
    /**
     * Compiles a while statement.
     */
    private void compileWhile() throws IOException {
        
        String labelSuffix = Integer.toString(whileLabelCount);
        whileLabelCount++;

        tokenizer.advanceTwice(); // while and (

        writer.writeLabel("WHILE" + labelSuffix);
        compileExpression();
        writer.writeArithmetic(VMWriter.Command.NOT);

        writer.writeIf("END_WHILE" + labelSuffix);

        tokenizer.advanceTwice(); // ) and {
        compileStatements();
        tokenizer.advance(); // }

        writer.writeGoto("WHILE" + labelSuffix);
        writer.writeLabel("END_WHILE" + labelSuffix);
    }
    
    
    /**
     * Compiles an if statement, possibly with a trailing else clause.
     */
    private void compileIf() throws IOException {
    
        String labelSuffix = Integer.toString(ifLabelCount);
        ifLabelCount++;
        
        tokenizer.advanceTwice(); // if keyword and (
        compileExpression();
        writer.writeArithmetic(VMWriter.Command.NOT);
        tokenizer.advanceTwice(); // ) and {

        writer.writeIf("IF_FALSE" + labelSuffix);

        compileStatements();
        tokenizer.advance(); // }

        boolean elseExists = tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD
                && tokenizer.keyword() == JackTokenizer.Keyword.ELSE;

        if (elseExists) {

            tokenizer.advanceTwice(); // else and {
            writer.writeGoto("END_IF" + labelSuffix);
        }
        writer.writeLabel("IF_FALSE" + labelSuffix);

        if (elseExists) {

            compileStatements();
            tokenizer.advance(); // }
            writer.writeLabel("END_IF" + labelSuffix);
        }
    }


    /**
     * Compiles a return statement.
     */
    private void compileReturn() throws IOException {
        tokenizer.advance(); // return keyword

        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL || tokenizer.symbol() != ';') {
            
            compileExpression();
        }
        else {
            writer.writePush(VMWriter.Segment.CONST, 0);
        }
        writer.writeReturn();
        tokenizer.advance(); // ;
    }


    /**
     * Compiles an expression.
     */
    private void compileExpression() throws IOException {

        compileTerm();

        while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL
                && String.valueOf(tokenizer.symbol()).matches("[+\\-*/&|<>=]")) {

            char operator = tokenizer.symbol();
            tokenizer.advance();

            compileTerm();

            if (operator == '*') {
                writer.writeCall("Math.multiply", 2);
            } else if (operator == '/') {
                writer.writeCall("Math.divide", 2);
            } else {
                writer.writeArithmetic(binaryOps.get(operator));
            }
        }
    }
    
    
    /**
     * Compiles a string constant.
     */
    private void compileStringConst() throws IOException {
        String value = tokenizer.stringVal();
        tokenizer.advance();
    
        writer.writePush(VMWriter.Segment.CONST, value.length());
        writer.writeCall("String.new", 1);
    
        for (int i = 0; i < value.length(); i++) {
            writer.writePush(VMWriter.Segment.CONST, (int) value.charAt(i));
            writer.writeCall("String.appendChar", 2);
        }
    }
    
    
    /**
     * Compiles a keyword constant.
     */
    private void compileKeywordConst() throws IOException {
        JackTokenizer.Keyword keyword = tokenizer.keyword();
        tokenizer.advance();
    
        if (keyword == JackTokenizer.Keyword.TRUE) {
            writer.writePush(VMWriter.Segment.CONST, 0);
            writer.writeArithmetic(VMWriter.Command.NOT);
        }
        else if (keyword == JackTokenizer.Keyword.THIS) {
            writer.writePush(VMWriter.Segment.POINTER, 0);
        }
        else {
            writer.writePush(VMWriter.Segment.CONST, 0);
        }
    }
    
    
    /**
     * A helper method for compiling symbol terms.
     */
    private void symbolTermHelper() throws IOException {
        if (tokenizer.symbol() == '(') {
            tokenizer.advance(); // (
            compileExpression();
            tokenizer.advance(); // )
        
        } else if (tokenizer.symbol() == '-') {
            tokenizer.advance();
            compileTerm();
            writer.writeArithmetic(VMWriter.Command.NEG);
        
        } else if (tokenizer.symbol() == '~') {
            tokenizer.advance();
            compileTerm();
            writer.writeArithmetic(VMWriter.Command.NOT);
        }
    }
    
    
    /**
     * A helper method for compiling identifier terms.
     */
    private void identifierTermHelper() throws IOException {
        String name = tokenizer.identifier();
        tokenizer.advance();
    
        if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL
                && String.valueOf(tokenizer.symbol()).matches("[\\[(.]")) {
            if (tokenizer.symbol() == '[') {
            
                writer.writePush(symbolTable.kindOf(name).toSegment(), symbolTable.indexOf(name));
            
                tokenizer.advance(); // [
                compileExpression();
                tokenizer.advance(); // ]
            
                writer.writeArithmetic(VMWriter.Command.ADD);
                writer.writePop(VMWriter.Segment.POINTER, 1);
                writer.writePush(VMWriter.Segment.THAT, 0);
            
            } else if (tokenizer.symbol() == '(' || tokenizer.symbol() == '.') {
                compileSubroutineCall(name);
                tokenizer.advance(); // )
            }
        }
        else {
            writer.writePush(symbolTable.kindOf(name).toSegment(), symbolTable.indexOf(name));
        }
    }
    
    
    /**
     * Compiles a term.
     */
    private void compileTerm() throws IOException {

        switch (tokenizer.tokenType()) {
            case INT_CONST:
                writer.writePush(VMWriter.Segment.CONST, tokenizer.intVal());
                tokenizer.advance();
                break;

            case STRING_CONST:
                compileStringConst();
                break;

            case KEYWORD:
                compileKeywordConst();
                break;

            case IDENTIFIER:
                identifierTermHelper();
                break;

            case SYMBOL:
                symbolTermHelper();
                break;
        }
    }
    
    
    /**
     * Compiles a (possibly empty) comma-separated list of expressions.
     */
    private int compileExpressionList() throws IOException {

        int nArgs = 1;

        compileExpression();

        while (tokenizer.symbol() == ',') {
            tokenizer.advance(); //,
            compileExpression();
            nArgs++;
        }
        return nArgs;
    }
}
