using System;
//using System.Collections.Generic;
using System.IO;
//using System.Linq;
using System.Text;
//using System.Threading.Tasks;

namespace Bug001 {
  
  class Program {
    
    static void Main(string[] args) {
      
      if (args.Length != 3) {
        Console.Error.WriteLine("Error: invalid number of arguments (" + args.Length + ")");
        Console.Error.WriteLine("Usage: ComplexLineComment inputfile outputfile errorfile");
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
        ComplexLineComment parser = new ComplexLineComment(Console.In);
        parser.Input();
        Console.Error.WriteLine("Input file parsed successfully");
      } catch (IOException e) {
        Console.Error.WriteLine("Error opening " + fn);
        Console.Error.WriteLine(e.Message);
      } catch (Exception e) {
        Console.Error.WriteLine("Error parsing input file " + args[0]);
        Console.Error.WriteLine(e.ToString());
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
