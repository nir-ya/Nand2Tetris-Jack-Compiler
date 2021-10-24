import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Emits Hack VM commands into a file, using the VM command syntax.
 */
class VMWriter {
    
    
    // writer object for the output file
    private PrintWriter writer;
    
    
    /**
     * Creates a new writer, and prepares for writing the output.
     *
     * @param output the output .vm file to write into.
     */
    VMWriter(File output) throws IOException {
        writer = new PrintWriter(output);
    }
    
    
    /**
     * Represents a virtual memory segment of the Hack VM.
     */
    enum Segment {
        CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP;
    
        @Override
        public String toString() {
            switch (this) {
                case CONST: return "constant";
                case ARG: return "argument";
                case STATIC:
                default: return this.name().toLowerCase();
            }
        }
    }
    
    
    /**
     * Writes a VM push command.
     *
     * @param segment the segment to push from.
     * @param index the index of the data to push.
     */
    void writePush(Segment segment, int index) {
        writer.println("push " + segment.toString() + " " + Integer.toString(index));
    }
    
    
    /**
     * Writes a VM pop command.
     *
     * @param segment the segment to pop into.
     * @param index the index (in the segment) to pop into.
     */
    void writePop(Segment segment, int index) {
        writer.println("pop " + segment.toString() + " " + Integer.toString(index));
    }
    
    
    /**
     * Represents a VM arithmetic command.
     */
    enum Command {ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT}
    
    
    /**
     * Writes a VM arithmetic command.
     *
     * @param command Command constant corresponding to the desired VM command.
     */
    void writeArithmetic(Command command) {
        writer.println(command.toString().toLowerCase());
    }
    
    
    /**
     * Writes a VM label command.
     *
     * @param label the name of the label to generate.
     */
    void writeLabel(String label) {
        writer.println("label " + label);
    }
    
    
    /**
     * Writes a VM goto command.
     *
     * @param label the name of the referenced label.
     */
    void writeGoto(String label) {
        writer.println("goto " + label);
    }
    
    
    /**
     * Writes a VM if-goto command.
     *
     * @param label the name of the referenced label.
     */
    void writeIf(String label) {
        writer.println("if-goto " + label);
    }
    
    
    /**
     * Writes a VM call command.
     *
     * @param name the name of the called function.
     * @param nArgs the number of arguments received by the called function.
     */
    void writeCall(String name, int nArgs) {
        writer.println("call " + name + " " + Integer.toString(nArgs));
    }
    
    
    /**
     * Writes a VM function command.
     *
     * @param name the name of the currently defined function.
     * @param nLocals the number of local variables declared in the function.
     */
    void writeFunction(String name, int nLocals) {
        writer.println("function " + name + " " + Integer.toString(nLocals));
    }
    
    
    /**
     * Writes a VM return command.
     */
    void writeReturn() {
        writer.println("return");
    }
    
    
    /**
     * Closes the writer.
     */
    void close() {
        writer.close();
    }
}
