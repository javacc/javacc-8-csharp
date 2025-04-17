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
using System.Collections;
using System.IO;

public partial class InterpreterVisitor : SPLParserDefaultVisitor {

  private TextReader trin;
  private TextWriter twout;
  private TextWriter twerr;

  /** Symbol table */
  Hashtable symtab = new Hashtable();

  /** "Stack" for calculations. */
  object[] stack = new object[1024];

  //  Stack<Tree> nodestack;

  /** Top of the "stack". */
  int top = -1;

  public InterpreterVisitor(TextReader trin, TextWriter twout, TextWriter twerr) {
    this.trin = trin;
    this.twout = twout;
    this.twerr = twerr;
  }

   public override void Visit(Node node, object data) {}

   public override void Visit(ST_CompilationUnit node, object data) {
    if (node.jjtGetChildren() != null) {
      foreach (Node child in node.jjtGetChildren()) {

        twout.Write("Executing:");
        Token first = child.jjtGetFirstToken();
        Token last = child.jjtGetLastToken();
        for (Token t = first; t != null; t = t.next) {
          twout.Write(" " + t);
          if (t == last) break;
        }
        twout.WriteLine();

        // VarDeclaration()  | Statement()
        child.jjtAccept(this, data);
      }
    }
  }

   public override void Visit(ST_VarDeclaration node, object data) {
    if (node.type == SPLParserConstants.BOOL) {
      symtab[node.name] = false;
    } else {
      symtab[node.name] = 0;
    }
  }

   public override void Visit(ST_Assignment node, object data) {
    string name;

    // Expression()
    node.jjtGetChild(1).jjtAccept(this, data);

    // PrimaryExpression()
    // note that here we do not visit the child!
    name = ((ST_Id) node.jjtGetChild(0)).name;
    symtab[name] = stack[top];
  }

   public override void Visit(ST_OrNode node, object data) {
    // ConditionalAndExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    if ((bool) stack[top]) {
      stack[top] = true;
      return;
    }

    // ConditionalAndExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((bool) stack[top]) || ((bool) stack[top + 1]);
  }

   public override void Visit(ST_AndNode node, object data) {
    // InclusiveOrExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    if (!((bool) stack[top])) {
      stack[top] = false;
      return;
    }

    // InclusiveOrExpression()
    node.jjtGetChild(1).jjtAccept(this, data);
  }

   public override void Visit(ST_BitwiseOrNode node, object data) {
    // ExclusiveOrExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // ExclusiveOrExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    if (stack[top].GetType() == typeof(bool))
      stack[--top] =((bool) stack[top]) | ((bool) stack[top + 1]);
    else if (stack[top].GetType() == typeof(int))
      stack[--top] = ((int) stack[top]) | ((int) stack[top + 1]);
  }

   public override void Visit(ST_BitwiseXorNode node, object data) {
    // AndExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // AndExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    if (stack[top].GetType() == typeof(bool))
      stack[--top] = ((bool) stack[top]) ^ ((bool) stack[top + 1]);
    else if (stack[top].GetType() == typeof(int))
      stack[--top] = ((int) stack[top]) ^ ((int) stack[top + 1]);
  }

   public override void Visit(ST_BitwiseAndNode node, object data) {
    // EqualityExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // EqualityExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    if (stack[top].GetType() == typeof(bool))
      stack[--top] = ((bool) stack[top]) & ((bool) stack[top + 1]);
    else if (stack[top].GetType() == typeof(int))
      stack[--top] = ((int) stack[top]) & ((int) stack[top + 1]);
  }

   public override void Visit(ST_EQNode node, object data) {
    // RelationalExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // RelationalExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    if (stack[top].GetType() == typeof(bool))
      stack[--top] = ((bool) stack[top]) == ((bool) stack[top + 1]);
    else
      stack[--top] = ((int) stack[top]) == ((int) stack[top + 1]);
  }

   public override void Visit(ST_NENode node, object data) {
    // RelationalExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // RelationalExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    if (stack[top].GetType() == typeof(bool))
      stack[--top] = ((bool) stack[top]) != ((bool) stack[top + 1]);
    else
      stack[--top] = ((int) stack[top]) != ((int) stack[top + 1]);
  }

   public override void Visit(ST_LTNode node, object data) {
    // AdditiveExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // AdditiveExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((int) stack[top]) < ((int) stack[top + 1]);
  }

   public override void Visit(ST_GTNode node, object data) {
    // AdditiveExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // AdditiveExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((int) stack[top]) > ((int) stack[top + 1]);
  }

   public override void Visit(ST_LENode node, object data) {
    // AdditiveExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // AdditiveExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((int) stack[top]) <= ((int) stack[top + 1]);
  }

   public override void Visit(ST_GENode node, object data) {
    // AdditiveExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // AdditiveExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((int) stack[top]) >= ((int) stack[top + 1]);
  }

   public override void Visit(ST_AddNode node, object data) {
    // MultiplicativeExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // MultiplicativeExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((int) stack[top]) + ((int) stack[top + 1]);
  }

   public override void Visit(ST_SubtractNode node, object data) {
    // MultiplicativeExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // MultiplicativeExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((int) stack[top]) - ((int) stack[top + 1]);
  }

   public override void Visit(ST_MulNode node, object data) {
    // UnaryExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // UnaryExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((int) stack[top]) * ((int) stack[top + 1]);
  }

   public override void Visit(ST_DivNode node, object data) {
    // UnaryExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // UnaryExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((int) stack[top]) / ((int) stack[top + 1]);
  }

   public override void Visit(ST_ModNode node, object data) {
    // UnaryExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    // UnaryExpression()
    node.jjtGetChild(1).jjtAccept(this, data);

    stack[--top] = ((int) stack[top]) % ((int) stack[top + 1]);
  }

   public override void Visit(ST_BitwiseComplNode node, object data) {
    // UnaryExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    stack[top] = ~((int) stack[top]);
  }

   public override void Visit(ST_NotNode node, object data) {
    // UnaryExpression()
    node.jjtGetChild(0).jjtAccept(this, data);

    stack[top] = !((bool) stack[top]);
  }

   public override void Visit(ST_Id node, object data) {
    stack[++top] = symtab[node.name];
  }

   public override void Visit(ST_IntConstNode node, object data) {
    stack[++top] = node.val;
  }

   public override void Visit(ST_TrueNode node, object data) {
    stack[++top] = true;
  }

   public override void Visit(ST_FalseNode node, object data) {
    stack[++top] = false;
  }

   public override void Visit(ST_Block node, object data) {
    if (node.jjtGetChildren() != null) {
      foreach (Node child in node.jjtGetChildren()) {
        // Statement()
        child.jjtAccept(this, data);
      }
    }
  }

   public override void Visit(ST_StatementExpression node, object data) {
    // Assignment()
    node.jjtGetChild(0).jjtAccept(this, data);
    
    top--; // just throw away the value.
  }

   public override void Visit(ST_IfStatement node, object data) {
    // Expression()
    node.jjtGetChild(0).jjtAccept(this, data);

    if (((bool) stack[top--])) {
      // Statement()
      node.jjtGetChild(1).jjtAccept(this, data);
    } else if (node.jjtGetNumChildren() == 3) {
      // Statement()
      node.jjtGetChild(2).jjtAccept(this, data);
    }
  }

   public override void Visit(ST_WhileStatement node, object data) {
    do {
      // Expression()
      node.jjtGetChild(0).jjtAccept(this, data);

    if (((bool) stack[top--])) {
        // Statement()
        node.jjtGetChild(1).jjtAccept(this, data);
      } else {
        break;
      }
    } while (true);
  }

   public override void Visit(ST_ReadStatement node, object data) {
    string name = node.name;
    object o = symtab[name];
    string s;

    if (o == null) {
      twerr.Flush();
      twerr.WriteLine("Undefined variable : " + name);
      twerr.Flush();
      Environment.Exit(1);
    }

    try {
      twerr.Flush();
      if (o.GetType() == typeof(bool)) {
        twout.WriteLine("Enter a value for \'" + name + "\' (boolean) : ");
        s = trin.ReadLine().Trim();
        bool bb = Convert.ToBoolean(s);
        twout.WriteLine("Read this value for \'" + name + "\' (boolean) : " + bb);
        symtab[name] = bb;
      } else if (o.GetType() == typeof(int)) {
        twout.WriteLine("Enter a value for \'" + name + "\' (int) : ");
        s = trin.ReadLine().Trim();
        int bi = Int32.Parse(s);
        twout.WriteLine("Read this value for \'" + name + "\' (int) : " + bi);
        symtab[name] = bi;
      }
    } catch (IOException ioe) {
      twerr.WriteLine(ioe.Message );
      twerr.WriteLine(ioe.StackTrace);
      twerr.Flush();
      Environment.Exit(2);
    } catch (FormatException fe) {
      twerr.WriteLine(fe.Message );
      twerr.WriteLine(fe.StackTrace);
      twerr.Flush();
      Environment.Exit(4);
    } catch (OverflowException oe) {
      twerr.WriteLine(oe.Message );
      twerr.WriteLine(oe.StackTrace);
      twerr.Flush();
      Environment.Exit(8);
    } catch (ArgumentException ae) {
      twerr.WriteLine(ae.Message );
      twerr.WriteLine(ae.StackTrace);
      twerr.Flush();
      Environment.Exit(16);
    } catch (Exception e) {
      twerr.WriteLine(e.Message );
      twerr.WriteLine(e.StackTrace);
      twerr.Flush();
      Environment.Exit(32);
    }
  }

   public override void Visit(ST_WriteStatement node, object data) {
    string name = node.name;
    object o = symtab[name];

    if (o == null) {
      twerr.Flush();
      twerr.WriteLine("Undefined variable : " + name);
      twerr.Flush();
    } else if (o.GetType() == typeof(bool)) {
      // to get the same case as in the java version
      twout.WriteLine("Value of " + name + " : " + o.ToString().ToLower());
    } else {
      twout.WriteLine("Value of " + name + " : " + o);
    }
  }
}
