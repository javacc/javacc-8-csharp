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
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace IDL {

  class Program {

    static void Main(string[] args) {

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
        output.WriteLine("IDL Parser Version 0.1:  Reading from file " + args[0] + " . . .");
        IDLParser parser = new IDLParser(input);
        // parser.enable_tracing();
        parser.specification();
        output.WriteLine("IDL Parser Version 0.1:  IDL file parsed successfully.");
      } catch (Exception e) {
          output.Write(e);
      } finally {
        if (prevInput != null) {
          input.Close();Console.SetIn(prevInput);
        }
        if (prevOutput != null) {
            output.Close();Console.SetOut(prevOutput);
        }
        if (prevError != null) {
            error.Close();Console.SetError(prevError);
        }
      }
    }
  }
}
