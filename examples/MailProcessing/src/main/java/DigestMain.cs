using System;
//using System.Collections.Generic;
using System.IO;
//using System.Linq;
using System.Text;
//using System.Threading.Tasks;

namespace MPDigest {
  
  class DigestMain {
    
    static void Main(string[] args) {
      
      if (args.Length < 2) {
        Console.Error.WriteLine("Error: invalid number of arguments (" + args.Length  + " instead of 2)");
        Console.Error.WriteLine("Usage: DigestMain infile outfile");
        return;
      }
    DigestMain dm = new DigestMain();
    dm.doMain(args);
  }

  void doMain(String[] args) {
      
      String fn = null;
      StreamReader fInput = null;
      StreamWriter fOutput = null;
      try {
        // open files
        fn = "input";
        fInput = new StreamReader(args[0]);
        fn = "output";
        fOutput = new StreamWriter(args[1]);
        fOutput.AutoFlush = true;
        // parse
        ComplexLineComment parser = new ComplexLineComment(fInput);
        parser.Input();
        Console.Error.WriteLine("Input file parsed successfully");
      } catch (IOException e) {
        Console.Error.WriteLine("Error opening " + fn + " file");
        Console.Error.WriteLine(e.Message);
      } catch (Exception e) {
        Console.Error.WriteLine("Error parsing input file");
        Console.Error.WriteLine(e.ToString());
      } finally {
        if (fInput  != null) fInput.Close();
        if (fOutput != null) fOutput.Close();
        Console.In.Close();
        Console.Out.Close();
        Console.Error.Close();
      }
      
    }
    
  }
  
}
/*
import java.io.*;

public class Digest {

  static int count = 0;

  static String buffer = "";

  public static void main(String args[]) throws ParseException, FileNotFoundException  {
    Digest parser = new Digest(new FileInputStream(args[0]));
    System.out.println("DIGEST OF RECENT MESSAGES FROM THE JAVACC MAILING LIST");
    System.out.println("----------------------------------------------------------------------");
    System.out.println("");
    System.out.println("MESSAGE SUMMARY:");
    System.out.println("");
    parser.MailFile();
    if (count == 0) {
      System.out.println("There have been no messages since the last digest posting.");
      System.out.println("");
      System.out.println("----------------------------------------------------------------------");
    } else {
      System.out.println("");
      System.out.println("----------------------------------------------------------------------");
      System.out.println("");
      System.out.println(buffer);
    }
  }

}
*/