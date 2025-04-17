/*
 * Copyright (c) 2020-2025, Sreeni Viswanadha <sreeni@viswanadha.net>.
 * Copyright (c) 2024-2025, Marc Mazas <mazas.marc@gmail.com>.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the names of the copyright holders nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
using System;
using System.IO;
using System.Text;

class SPLMain {

  static void Main(string[] args) {

    TextReader spl = null;
    TextReader input = Console.In;
    TextWriter output = Console.Out;
    TextWriter error = Console.Error;
    TextReader prevInput = null;
    TextWriter prevOutput = null;
    TextWriter prevError = null;

    if (args.Length == 3) {
      prevInput = input;
      input = new StreamReader(@args[0]);
      Console.SetIn(input);
      prevOutput = output;
      output = new StreamWriter(@args[1]);
      Console.SetOut(output);
      prevError = error;
      error = new StreamWriter(@args[2]);
      Console.SetError(error);    
    }

    try {
      SPLParser parser;
      switch (args.Length) {
        case 1:
          output.WriteLine(
              "Simple Programming Language Interpreter (AST):  Reading from file "
                  + args[0]
                  + " . . .");
          try {
            parser = new SPLParser(new StreamReader(args[0]));
          } catch (FileNotFoundException) {
            error.WriteLine(
                "Simple Programming Language Interpreter (AST):  File "
                    + args[0]
                    + " not found.");
            return;
          }
          break;
        case 4:
          spl = new StreamReader(args[0]);
          prevInput = input;
          input = new StreamReader(args[1]);
          Console.SetIn(input);
          prevOutput = output;
          output = new StreamWriter(args[2]);
          Console.SetOut(output);
          prevError = error;
          error = new StreamWriter(args[3]);
          Console.SetError(error);
          parser = new SPLParser(spl);
          break;
        default:
          output.WriteLine("Simple Programming Language Interpreter (AST):  Usage :");
          output.WriteLine("         java SPLMain spl [in out err]");
          return;
      }
      parser.CompilationUnit();
      parser.jjtree.rootNode().interpret();
    } catch (ParseException pe) {
      error.WriteLine(
          "Simple Programming Language Interpreter (AST):  Encountered errors during parse.");
      error.WriteLine(pe.Message );
      error.WriteLine(pe.StackTrace);
    } catch (Exception e) {
      error.WriteLine(
          "Simple Programming Language Interpreter (AST):  Encountered errors during interpretation/tree building.");
      error.WriteLine(e.Message );
      error.WriteLine(e.StackTrace);
    } finally {
      try {
        input.Close();
        output.Close();
        error.Close();
      } catch (IOException) {
      }
      if (prevInput != null) Console.SetIn(prevInput);
      if (prevOutput != null) Console.SetOut(prevOutput);
      if (prevError != null) Console.SetError(prevError);
    }
    
  } // Main
  
} // class
