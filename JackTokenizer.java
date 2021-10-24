import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Allows the input jack code file to be viewed as a stream of Jack-language tokens (lexical elements),
 *  as specified by the jack grammar (skips all comments and white spaces),
 *  and provides easy access to them.
 */
class JackTokenizer {


    // A regular expression describing a keyword in the jack language
    private static final String KEYWORD = "class|constructor|function|method|field|static|var|int|char|" +
            "boolean|void|true|false|null|this|let|do|if|else|while|return";

    // A regular expression describing a symbol in the jack language
    private static final String SYMBOL = "[{}()\\[\\].,;+\\-*/&|<>=~]";

    // A regular expression describing an integer constant in the jack language
    private static final String INTEGER_CONSTANT = "(\\d+)";

    // A regular expression describing a string constant in the jack language
    private static final String STRING_CONSTANT = "(\"[^\"]*\")";

    // A regular expression describing an identifier in the jack language
    private static final String IDENTIFIER = "([\\D&&[\\w]][\\w]*)";


    // Patterns for spotting and matching Jack-language lexical elements (as described above)
    private static final Pattern tokenPattern = Pattern.compile(SYMBOL  + "|" + INTEGER_CONSTANT
            + "|" + STRING_CONSTANT + "|" + IDENTIFIER);
    private static final Pattern symbolPattern = Pattern.compile(SYMBOL);
    private static final Pattern intPattern = Pattern.compile(INTEGER_CONSTANT);
    private static final Pattern stringPattern = Pattern.compile(STRING_CONSTANT);
    private static final Pattern keywordPattern = Pattern.compile(KEYWORD);


    //*** Data Members ***//

    // Buffered reader for parsing the input file
    private BufferedReader reader;
    
    // Current line in the input code file.
    private String currentLine;

    // The current offset in the current line
    private int currentOffset;

    // The current token in the input file
    private String currentToken;

    // A Matcher object for matching Jack-language tokens
    private Matcher tokenMatcher;
    

    /**
     * Class constructor.
     * Opens the input file/stream, and gets ready to tokenize it
     * (advances to the first token in the code).
     *
     * @param inputFile the jack code file to tokenize.
     * @throws IOException in case of a problem handling the input file.
     */
    JackTokenizer(File inputFile) throws IOException {
        
        reader = Files.newBufferedReader(inputFile.toPath());
    
        readLine();
        advance();
    }


    /**
     * Skips one-line and multi-line comments in the file, until the next token is met
     * (or reaching the end of the file).
     */
    private void skipComments() throws IOException {
    
        while (currentLine != null && currentLine.substring(currentOffset).matches("\\s*//.*")) {
            readLine();
        }
    
        while (currentLine != null && currentLine.substring(currentOffset).matches("\\s*/\\*.*")) {
            while (currentLine != null && !currentLine.substring(currentOffset).contains("*/")) {
                currentLine = reader.readLine();
                currentOffset = 0;
            }
            if (currentLine != null) {
                tokenMatcher = tokenPattern.matcher(currentLine);
                currentOffset = currentLine.substring(currentOffset).indexOf("*/") + currentOffset + 2;
            }
        }
    }
    
    
    /**
     * Advances the tokenizer to the next line in the input code file,
     *  and resets the current offset to 0.
     */
    private void readLine() throws IOException {
        currentLine = reader.readLine();
        currentOffset = 0;
        if (currentLine != null) {
            tokenMatcher = tokenPattern.matcher(currentLine);
        }
        else {
            tokenMatcher = null;
        }
    }
    
    
    /**
     * Gets the next token from the input, and makes it the current token.
     * This method should only be called if hasMoreTokens() returns true.
     * Initially, there is no current token.
     */
    void advance() throws IOException {
        
        boolean found = false;
        skipComments();
        if (tokenMatcher != null) {
            found = tokenMatcher.find(currentOffset);
        }

        while (!found && currentLine != null) {
            readLine();
            skipComments();
            if (tokenMatcher != null) {
                found = tokenMatcher.find(currentOffset);
            }
        }
        if (found) {
            currentToken = tokenMatcher.group();
            currentOffset = tokenMatcher.end();
        }
        else currentToken = null;
    }
    
    
    /**
     * Advances the tokenizer twice (to the token after the next one).
     */
    void advanceTwice() throws IOException {
        advance();
        advance();
    }


    /**
     * Represents a jack-language token type.
     */
    public enum TokenType {
        KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST;
        
        @Override
        public String toString() {
            switch (this) {
                case INT_CONST: return "integerConstant";
                case STRING_CONST: return "stringConstant";
                default: return this.name().toLowerCase();
            }
        }
    }
    
    
    /**
     * @return the type of the current token.
     */
    TokenType tokenType() {
        
        if (symbolPattern.matcher(currentToken).matches()) {
            return TokenType.SYMBOL;
        }
        else if (intPattern.matcher(currentToken).matches()) {
            return TokenType.INT_CONST;
        }
        else if (stringPattern.matcher(currentToken).matches()) {
            return TokenType.STRING_CONST;
        }
        else if (keywordPattern.matcher(currentToken).matches()) {
            return TokenType.KEYWORD;
        }
        else {
            return TokenType.IDENTIFIER;
        }
    }


    /**
     * Represents a jack-language keyword.
     */
    public enum Keyword {CLASS, METHOD, FUNCTION, CONSTRUCTOR, INT, BOOLEAN, CHAR, VOID, VAR, STATIC, FIELD,
                         LET, DO, IF, ELSE, WHILE, RETURN, TRUE, FALSE, NULL, THIS}


    /**
     * Called when tokenType() returns KEYWORD
     * @return the keyword which is the current token
     */
    Keyword keyword() {
        return Keyword.valueOf(currentToken.toUpperCase());
    }
    
    
    /**
     * Called when tokenType() returns SYMBOL
     * @return the character which is the current token
     */
    char symbol() {
        return currentToken.charAt(0); //if it's a symbol, it's a string with one char
    }
    
    
    /**
     * Called when tokenType() returns IDENTIFIER
     * @return the identifier which is the current token
     */
    String identifier() {
        return currentToken;
    }
    
    
    /**
     * Called when tokenType() returns INT_CONST
     * @return the integer value of the current token
     */
    int intVal() {
        return Integer.parseInt(currentToken);
    }
    
    
    /**
     * Called when tokenType() returns STRING_CONST
     * @return the string value of the current token, without the double quotes
     */
    String stringVal() {
        return currentToken.substring(1, currentToken.length() - 1);
    }


    /**
     * Closes the reader stream object.
     *
     * @throws IOException in case of a problem closing the file reader.
     */
    void close() throws IOException {
        reader.close();
    }
}
