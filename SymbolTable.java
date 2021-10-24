import java.util.Map;
import java.util.HashMap;


/**
 * Provides a symbol table abstraction.
 * The symbol table associates the identifier names found in the program with identifier properties
 *  needed for compilation: type, kind, and a running index.
 * The symbol table for Jack programs has two nested scopes (class/subroutine).
 */
class SymbolTable {
    
    
    //*** Data Members ***//
    // counters for the number of currently defined class level identifiers
    private int staticCount;
    private int fieldCount;
    
    // counters for the number of subroutine level identifiers defined in the current scope
    private int argumentCount;
    private int localsCount;
    
    // Maps for keeping the correspondence between identifier names and their properties
    private Map<String, IdProperties> classTable;
    private Map<String, IdProperties> subroutineTable;
    
    
    /**
     * Inner class representing the properties of an identifier in a Jack program.
     */
    static private class IdProperties {
        
        String type;
        Kind kind;
        int index;
        
        /**
         * Class constructor.
         */
        IdProperties(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }
    
    
    /**
     * Creates a new empty symbol table.
     */
    SymbolTable() {
        classTable = new HashMap<>();
        staticCount = 0;
        fieldCount = 0;
    }
    
    
    /**
     * Starts a new subroutine scope (i.e. resets the subroutine symbol table).
     */
    void startSubroutine() {
        subroutineTable = new HashMap<>();
        argumentCount = 0;
        localsCount = 0;
    }
    
    
    /**
     * Represents the kind of an identifier
     * (if it is an existing variable: static / field / argument / var(local), and otherwise NONE).
     */
    enum Kind {
        STATIC, FIELD, ARG, VAR, NONE;
        
        public VMWriter.Segment toSegment() {
            switch (this) {
                case VAR: return VMWriter.Segment.LOCAL;
                case FIELD: return VMWriter.Segment.THIS;
                default: return VMWriter.Segment.valueOf(this.name());
            }
        }
    }
    
    
    /**
     * Defines a new identifier of a given name, type and kind,
     *  and assigns it a running index.
     * STATIC and FIELD identifiers have a class scope,
     *  while ARG and VAR identifiers have a subroutine scope.
     *
     * @param name the name of the newly defined identifier.
     * @param type the type of the newly defined identifier.
     * @param kind the kind of the newly defined identifier.
     */
    void define(String name, String type, Kind kind) {
        
        switch (kind) {
            case STATIC:
                classTable.put(name, new IdProperties(type, kind, staticCount));
                staticCount++;
                break;
            case FIELD:
                classTable.put(name, new IdProperties(type, kind, fieldCount));
                fieldCount++;
                break;
            case ARG:
                subroutineTable.put(name, new IdProperties(type, kind, argumentCount));
                argumentCount++;
                break;
            case VAR:
                subroutineTable.put(name, new IdProperties(type, kind, localsCount));
                localsCount++;
                break;
        }
    }
    
    
    /**
     * Returns the number of variables of the given kind already defined in the current scope.
     *
     * @param kind the kind of the variables.
     * @return the number of variables of the given kind recognized in the current scope.
     */
    int varCount(Kind kind) {
        
        switch (kind) {
            case STATIC:
                return staticCount;
            case FIELD:
                return fieldCount;
            case ARG:
                return argumentCount;
            case VAR:
                return localsCount;
            default:
                return 0;
        }
    }
    
    
    /**
     * Returns the kind of the named identifier of the current scope.
     * If the identifier is unknown in the current scope, returns NONE.
     *
     * @param name the name of the referenced identifier.
     * @return the kind of the variable with the given name.
     */
    Kind kindOf(String name) {
        
        if (subroutineTable.containsKey(name)) {
            return subroutineTable.get(name).kind;
        }
        else if (classTable.containsKey(name)) {
            return classTable.get(name).kind;
        }
        else {
            return Kind.NONE;
        }
    }
    
    
    /**
     * Returns the type of the named identifier of the current scope.
     *
     * @param name the name of the referenced identifier.
     * @return the type of the variable with the given name.
     */
    String typeOf(String name) {
    
        if (subroutineTable.containsKey(name)) {
            return subroutineTable.get(name).type;
        }
        else {
            return classTable.get(name).type;
        }
    }
    
    
    /**
     * Returns the index assigned to the named identifier.
     *
     * @param name the name of the referenced identifier.
     * @return the index of the variable with the given name.
     */
    int indexOf(String name) {
    
        if (subroutineTable.containsKey(name)) {
            return subroutineTable.get(name).index;
        }
        else {
            return classTable.get(name).index;
        }
    }
}
