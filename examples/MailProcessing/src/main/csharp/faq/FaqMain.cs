using System;
//using System.Collections.Generic;
using System.IO;
//using System.Linq;
using System.Text;
//using System.Threading.Tasks;

namespace FAQ {
  
  class FaqMain {

    static public int count = 0;

    static public int beginAt = 1;

    static public String outdir;

    static public StreamWriter indexsw;

    static public String fix(String s) {
      String retval = "";
      for (int i = 0; i < s.Length; i++) {
        char c = s[i];
        if (c == '<') {
          retval += "&lt;";
        } else if (c == '>') {
          retval += "&gt;";
        } else {
          retval += c;
        }
      }
      return retval;
    }
    
  /*
   * The corresponding Java example declares the main method to throw ParseException, IOException
   * and FileNotFoundException, and silently displays nothing on these 3 exceptions
   * (and IOException is indeed occurring when the end of the input file is reached
   * and seen by the SimpleCharStream, who throws a new IOException()).
   * In order to mimic this behavior in C#, we add catching / swallowing them;
   * more, as in C# an attempt to read a closed (disposed) input stream produces a
   * System.ObjectDisposedException (in Java it produces an IOException), we catch / swallow it too.
   */
  
    static void Main(string[] args) {
      
      if (args.Length < 3) {
        Console.Error.WriteLine("Error: invalid number of arguments (" + args.Length  + " instead of 3)");
        Console.Error.WriteLine("Usage: FaqMain index infile outdir");
        return;
      }
      try {
        FaqMain fm = new FaqMain();
        fm.doMain(args);
      } catch(System.Exception e) when (e is ParseException ||
                                        e is System.IO.IOException ||
                                        e is System.IO.FileNotFoundException  ||
                                        e is System.ObjectDisposedException) {
        // do nothing
      }
    }

    void doMain(String[] args) {
    
      String fn = null;
      StreamReader fInput = null;
      beginAt = Int32.Parse(args[0]);
      
      // open files
      fn = "index";
      outdir = args[2];
      indexsw = new StreamWriter(outdir + "/index.out.html");
      indexsw.WriteLine("<title>Selected list of emails from the JavaCC mailing list</title>");
      indexsw.WriteLine("<h2>Selected list of emails from the JavaCC mailing list</h2>");
      indexsw.AutoFlush = true;
      fn = "input";
      fInput = new StreamReader(args[1]);
      
      // parse
      Faq parser = new Faq(fInput);
      parser.MailFile();
    }
    
  }
  
}
