using System;
//using System.Collections.Generic;
using System.IO;
//using System.Linq;
using System.Text;
//using System.Threading.Tasks;

namespace DIG {
  
  class DigestMain {
    
    StreamWriter digsw;

  /*
   * The corresponding Java example declares the main method to throw ParseException, IOException
   * and FileNotFoundException, and silently displays nothing on these 3 exceptions
   * (and IOException is indeed occurring when the end of the input file is reached
   * and seen by the SimpleCharStream, who throws a new IOException()).
   * In order to mimic this behavior in C#, we add catching / swallowing them;
   * note that the System.ObjectDisposedException should have been transformed in a
   * System.IO.IOException by the CharStream and therefore is not caught here.
   */
  
    static void Main(string[] args) {
      if (args.Length < 2) {
        Console.Error.WriteLine("Error: invalid number of arguments (" + args.Length  + " instead of 2)");
        Console.Error.WriteLine("Usage: DigestMain infile outfile");
        return;
      }
      try {
        DigestMain dm = new DigestMain();
        dm.doMain(args);
      } catch(System.Exception e) when (e is ParseException ||
                                        e is System.IO.IOException ||
                                        e is System.IO.FileNotFoundException) {
        // do nothing; i.e. swallow it
      }
    }

    void doMain(String[] args) {
      
      String fn = null;
      StreamReader fInput = null;
      
      // open files
      fn = "digsw";
      digsw = new StreamWriter(args[1]);
      fn = "input";
      fInput = new StreamReader(args[0]);
      
      // parse
      Digest parser = new Digest(fInput);
      parser.setDigsw(digsw);
      
      digsw.WriteLine("DIGEST OF RECENT MESSAGES FROM THE JAVACC MAILING LIST");
      digsw.WriteLine("----------------------------------------------------------------------");
      digsw.WriteLine("");
      digsw.WriteLine("MESSAGE SUMMARY:");
      digsw.WriteLine("");
      
      String buffer = parser.MailFile();
      
      if (buffer.Length == 0) {
        digsw.WriteLine("There have been no messages since the last digest posting.");
        digsw.WriteLine("");
        digsw.WriteLine("----------------------------------------------------------------------");
      } else {
        digsw.WriteLine("");
        digsw.WriteLine("----------------------------------------------------------------------");
        digsw.WriteLine("");
        digsw.WriteLine(buffer);
      }
      // do not forget this otherwise output will be lost
      // when the IOException("Object disposed") is caught (above in the Main).
      digsw.Flush();
    }
    
  }
  
}
