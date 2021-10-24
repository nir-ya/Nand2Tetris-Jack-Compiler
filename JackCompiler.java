import java.io.File;
import java.io.IOException;


/**
 * The top level driver that sets up and invokes the other modules.
 *
 * The compiler program operates on a given source, where source is either a file name of the form xxx.jack
 *  or a directory name containing one or more such files.
 * For each source xxx.jack file, the compiler creates an output file named xxx.vm,
 *  into which it writes the translation of the given Jack class to Hack VM code.
 */
public class JackCompiler {
    
    
    // The index of source file name in command line arguments array
    private static final int ARGUMENT_INDEX = 0;
    
    // The extension (type) of the output VM code file
    private static final String OUTPUT_FILE_EXTENSION = ".vm";
    
    // The extension (type) of an input Jack code file
    private static final String INPUT_FILES_EXTENSION = ".jack";
    
    
    //*** Data Members ***//
    
    // The file/directory of the jack program to translate
    private File sourceToCompile;
    
    
    /**
     * Class constructor. creates a new JackCompiler of the input source (directory/file).
     * @param source the source to compile (file or directory).
     */
    private JackCompiler(File source) {
        sourceToCompile = source;
    }

    
    /**
     * Creates a File object from a given source pathname, and ensures its validity.
     *
     * @param pathname the path of the input code file (relative or absolute).
     * @return A file object representing the input source pathname.
     * @throws IllegalArgumentException In case the given pathname is not a valid file.
     */
    private static File createSource(String pathname) throws IllegalArgumentException {
        
        File source = new File(pathname);
        
        if (!source.exists()) {
            throw new IllegalArgumentException("The input argument '" + source.getName()
                    + "' isn't a valid file/directory path!");
        }
        return source;
    }
    
    
    /**
     * Creates a new VM output file with the same name of the input file
     *  (different extension: for example, If the name of the input file was 'SomeClass.jack',
     *  then the output file will be named 'SomeClass.vm').
     * If an output file with the corresponding name already exists, the method will Overwrite it.
     *
     * @return the newly created output file (File object).
     * @throws IOException in case of a problem handling the output file / directory.
     */
    private File createOutputFile(String fileName) throws IOException {
        
        File outputFile = new File(fileName + OUTPUT_FILE_EXTENSION);
        
        if (!outputFile.createNewFile()) {
            System.out.println("The program is overwriting " + outputFile.getName());
        }
        return outputFile;
    }


    /**
     * Executes the translation process of a single .jack file to VM code output,
     * as described above (see class description).
     *
     * @param currentFile the jack code file to compile.
     * @throws IOException in case of a problem handling the input file.
     */
    private void compile(File currentFile) throws IOException {
        
        JackTokenizer tokenizer = new JackTokenizer(currentFile);
    
        String sourcePath = currentFile.getAbsolutePath();
        String outputName = sourcePath.substring(0, sourcePath.lastIndexOf("."));

        VMWriter writer = new VMWriter(createOutputFile(outputName));
        SymbolTable table = new SymbolTable();
        
        CompilationEngine engine = new CompilationEngine(tokenizer, writer, table);

        engine.compileClass();

        writer.close();
        tokenizer.close();
    }
    
    
    /**
     * Executes the translation process of the entire input (directory or file).
     * If the input is a single file, outputs a single file residing in the same directory.
     * If the input is a directory, outputs a single .vm file for every .jack file in the directory,
     *  into the same directory.
     *
     * @throws IOException in case of an i/o problem with handling the input or output files.
     */
    private void execute() throws IOException {
        
        if (sourceToCompile.isDirectory()) {
            
            File[] jackFiles = sourceToCompile.listFiles(pathname -> pathname.isFile()
                    && pathname.getName().endsWith(INPUT_FILES_EXTENSION));
            
            if (jackFiles != null) {
                for (File currentFile : jackFiles) {
                    compile(currentFile);
                }
            }
        }
        else if (sourceToCompile.isFile()) {
            compile(sourceToCompile);
        }
    }
    
    
    /**
     * Accepts a single command line argument (either a file name or a directory name),
     * and for each source xxx.jack file, creates a VM code file named xxx.vm,
     * and outputs to it the translation of the input jack code, into corresponding Hack VM code.
     *
     * @param args command line argument array.
     */
    public static void main(String[] args) {
    
        try {
            JackCompiler compiler = new JackCompiler(createSource(args[ARGUMENT_INDEX]));
            compiler.execute();
        }
        catch (IOException e) {
            System.err.println("I/O Problem: " + e.getMessage());
        }
        catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }
}
