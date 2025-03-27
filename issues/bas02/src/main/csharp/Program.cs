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
//using System.Collections.Generic;
using System.IO;
//using System.Linq;
using System.Text;
//using System.Threading.Tasks;

namespace Bug002 {
  
  class Program {
    
    static void Main(string[] args) {
      
      if (args.Length != 3) {
        Console.Error.WriteLine("Error: invalid number of arguments (" + args.Length + ")");
        Console.Error.WriteLine("Usage: Bug inputfile outputfile errorfile");
        return;
      }
      
      String fn = null;
      TextReader stdInput = null;
      TextWriter stdOutput = null;
      TextWriter stdError = null;
      StreamWriter fOutput = null;
      StreamWriter fError = null;
      try {
        // open files and redirect standard streams to them
        fn = "input file " + args[0];
        stdInput = Console.In;
        Console.SetIn(new StreamReader(args[0]));
        fn = "output file " + args[01];
        stdOutput = Console.Out;
        fOutput = new StreamWriter(args[1]);
        fOutput.AutoFlush = true;
        Console.SetOut(fOutput);
        fn = "error file " + args[2];
        stdError = Console.Error;
        fError = new StreamWriter(args[2]);
        fError.AutoFlush = true;
        Console.SetError(fError);
        // parse
        mns.Bug parser = new mns.Bug(Console.In);
        parser.enable_tracing();
        parser.EnumerationItem();
        Console.Error.WriteLine("Input file parsed successfully");
      } catch (IOException e) {
        Console.Error.WriteLine("IOException opening " + fn);
        Console.Error.WriteLine(e.Message);
      } catch (mns.ParseException e) {
        Console.Error.WriteLine("ParseException parsing input file");
        Console.Error.WriteLine(e.Message);
      } catch (Exception e) {
        Console.Error.WriteLine("Error parsing input file");
        Console.Error.WriteLine(e.Message);
      } finally {
        Console.In.Close();
        Console.Out.Close();
        Console.Error.Close();
        if (stdInput  != null) Console.SetIn(stdInput);
        if (stdOutput != null) Console.SetOut(stdOutput);
        if (stdError  != null) Console.SetError(stdError);
      }
     
    }
    
  }
  
}
