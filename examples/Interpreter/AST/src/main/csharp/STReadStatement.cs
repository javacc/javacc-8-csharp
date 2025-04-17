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

public partial class STReadStatement : Node {

  public string name;

  public STReadStatement(int id) : base (id) {}

  public STReadStatement(SPLParser p, int id) : base(p, id) {}

  public override void interpret() {
    object o = symtab[name];
    string s;

    if (o == null) {
      Console.Out.Flush();
      Console.Error.WriteLine("Undefined variable : " + name);
      Console.Error.Flush();
      Environment.Exit(1);
    }

    try {
      Console.Out.Flush();
      if (o.GetType() == typeof(bool)) {
        Console.Out.WriteLine("Enter a value for \'" + name + "\' (boolean) : ");
        s = Console.In.ReadLine().Trim();
        bool bb = Convert.ToBoolean(s);
        Console.Out.WriteLine("Read this value for \'" + name + "\' (boolean) : " + bb);
        symtab[name] = bb;
      } else if (o.GetType() == typeof(int)) {
        Console.Out.WriteLine("Enter a value for \'" + name + "\' (int) : ");
        s = Console.In.ReadLine().Trim();
        int bi = Int32.Parse(s);
        Console.Out.WriteLine("Read this value for \'" + name + "\' (int) : " + bi);
        symtab[name] = bi;
      }
    } catch (IOException ioe) {
      Console.Error.WriteLine(ioe.Message );
      Console.Error.WriteLine(ioe.StackTrace);
      Console.Error.Flush();
      Environment.Exit(2);
    } catch (FormatException fe) {
      Console.Error.WriteLine(fe.Message );
      Console.Error.WriteLine(fe.StackTrace);
      Console.Error.Flush();
      Environment.Exit(4);
    } catch (OverflowException oe) {
      Console.Error.WriteLine(oe.Message );
      Console.Error.WriteLine(oe.StackTrace);
      Console.Error.Flush();
      Environment.Exit(8);
    } catch (ArgumentException ae) {
      Console.Error.WriteLine(ae.Message );
      Console.Error.WriteLine(ae.StackTrace);
      Console.Error.Flush();
      Environment.Exit(16);
    } catch (Exception e) {
      Console.Error.WriteLine(e.Message );
      Console.Error.WriteLine(e.StackTrace);
      Console.Error.Flush();
      Environment.Exit(32);
    }
  }
}
