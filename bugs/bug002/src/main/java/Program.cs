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
//    class Program
//    {
//        static void Main(string[] args)
//        {
//            TextReader input = Console.In;
//            TextWriter output = Console.Out;
//            TextWriter error = Console.Error;
//            TextReader prevInput = null;
//            TextWriter prevOutput = null;
//            TextWriter prevError = null;
//            if (args.Length == 3)
//            {
//                prevInput = input; input = new StreamReader(@args[0]);
//                prevOutput = output; output = new StreamWriter(@args[1]);
//                prevError = error; error = new StreamWriter(@args[2]);
//            
//            }
//            try
//            {
//                Bug parser = new Bug(input);
//                parser.enable_tracing();
//                parser.EnumerationItem();
//                output.WriteLine("Parser :  file parsed successfully.");
//            }
//            catch (Exception e)
//            {
//                error.Write(e);
//            }
//            finally
//            {
//            if (prevInput != null)
//            {
//                input.Close();
//                Console.SetIn(prevInput);
//            }
//            if (prevOutput != null)
//            {
//                output.Flush(); output.Close();
//                Console.SetOut(prevOutput);
//            }
//            if (prevError != null)
//            {
//                error.Flush(); error.Close();
//                Console.SetError(prevError);
//            }
//        }
//    }
//    }
