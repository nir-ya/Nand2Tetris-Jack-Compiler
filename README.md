# Nand2Tetris - Jack Compiler
Project #11 in "Nand to Tetris" course

specification: https://www.nand2tetris.org/project11

Authors: Nir Yazdi, Lautaro Borrovinsky


Files
---------------
JackCompiler - small script for executing purposes.

Code Files:

JackCompiler.java - The main module that sets up and invokes the other modules.

JackTokenizer.java - Allows the input Jack code file to be viewed as a stream of tokens (lexical elements),
                     providing easy access to them.

CompilationEngine.java - Recursive top-down compilation engine. Effects the actual compilation output,
                         using the tokenizer as input, and a VMWriter for writing to the output vm file.

SymbolTable.java - symbol table module.

VMWriter.java - output module, generating VM code.
