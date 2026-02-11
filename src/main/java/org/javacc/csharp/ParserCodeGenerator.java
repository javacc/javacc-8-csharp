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
package org.javacc.csharp;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.javacc.parser.Action;
import org.javacc.parser.BNFProduction;
import org.javacc.parser.Choice;
import org.javacc.parser.CodeGeneratorSettings;
import org.javacc.parser.CodeProduction;
import org.javacc.parser.Context;
// import org.javacc.parser.CppCodeProduction;
import org.javacc.parser.Expansion;
import org.javacc.parser.JavaCCGlobals;
import org.javacc.parser.JavaCCParserConstants;
// import org.javacc.parser.JavaCodeProduction;
import org.javacc.parser.Lookahead;
import org.javacc.parser.NonTerminal;
import org.javacc.parser.NormalProduction;
import org.javacc.parser.OneOrMore;
import org.javacc.parser.Options;
import org.javacc.parser.ParserData;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.Semanticize;
import org.javacc.parser.Sequence;
import org.javacc.parser.Token;
import org.javacc.parser.TryBlock;
import org.javacc.parser.ZeroOrMore;
import org.javacc.parser.ZeroOrOne;
import org.javacc.utils.CodeBuilder;
import org.javacc.utils.CodeBuilder.GenericCodeBuilder;

/** Generate the parser. */
class ParserCodeGenerator implements org.javacc.parser.ParserCodeGenerator {
  
  private static final String parserTemplate = "/templates/csharp/ParserDriver.template";
  
  /*
   * These lists are used to maintain expansions for which code generation in phase 2 and phase 3
   *  is required.
   * Whenever a call is generated to a phase 2 or phase 3 routine, a corresponding entry is added
   *  here if it has not already been added.
   * The phase 3 routines have been optimized in version 0.7pre2.
   * Essentially only those methods (and only those portions of these methods) are generated
   *  that are required.
   * The lookahead amount is used to determine this.
   * This change requires the use of a hash table because it is now possible for the same phase 3
   *  routine to be requested multiple times with different lookaheads.
   * The hash table provides a easily searchable capability to determine the previous requests.
   * The phase 3 routines nExpressionTreeConstantsow are performed in a two step process:
   *  - the first step gathers the requests (replacing requests with lower lookaheads with those
   *     requiring larger lookaheads),
   *  - the second step then generates these methods.
   */
  
  private final List<Lookahead>                  phase2list  = new ArrayList<>();
  private final List<Phase3Data>                 phase3list  = new ArrayList<>();
  private boolean                                jj2LA;
  private final Hashtable<Expansion, Phase3Data> phase3table = new Hashtable<>();
  
  private final Context                 context;
  private final Map<Expansion, String>  internalNames   = new HashMap<>();
  private final Map<Expansion, Integer> internalIndexes = new HashMap<>();
  
  private GenericCodeBuilder cb;
  
  private ParserData parserData;
  
  /**
   * True to add debug comment tags in the generated code (to link it with this generator),<br>
   * false otherwise (which should be the normal case.
   */
  private static final boolean DCT = true;
  
  ParserCodeGenerator(final Context context) {
    this.context = context;
  }
  
  @Override
  public void generateCode(final CodeGeneratorSettings settings, final ParserData parserData) {
    
    this.parserData = parserData;
    
    settings.putAll(Options.getOptions());
    settings.put("parserName", parserData.parserName);
    //    final String superClass = (String) settings.get(Options.UO__TOKEN_MANAGER_SUPER_CLASS);
    //    if (superClass != null && !"".equals(superClass.trim())) {
    //      settings.put("superClass", " :  " + superClass);
    //    }
    if (!"".equals(Options.getNamespace())) {
      settings.put("NAMESPACE", Options.getNamespace());
    }
    
    try {
      cb = GenericCodeBuilder.of(context, settings);
      cb.setFile(new File(Options.getOutputDirectory(), parserData.parserName + ".cs"));
      
      if (!"".equals(Options.getNamespace())) {
        cb.println("namespace " + Options.getNamespace() + " {\n");
      }
      
      cb.print("public partial class " + parserData.parserName + " : ");
      //      if (settings.containsKey("superClass")) {
      //        gcb.print(settings.get("superClass") + ", ");
      //      }
      cb.println(parserData.parserName + "Constants {");
      
      cb.println("  /* Beginning of code from parser begin-end section */");
      cb.println(parserData.decls);
      cb.println();
      
      if (context.globals().jjtreeGenerated) {
        cb.println("  /* Beginning of code specific to JJTree */");
        cb.println();
        cb.println("  public JJT" + parserData.parserName + "State jjtree = new JJT"
            + parserData.parserName + "State();");
        cb.println();
      }
      
      cb.println("  /* Beginning of generated code for productions */");
      cb.println();
      processProductions(settings, cb);
      
      settings.put("numproductions", internalIndexes.size());
      settings.put("jj2index", context.globals().jj2index);
      settings.put("maskindex", context.globals().maskindex);
      settings.put("tokenCount", context.globals().tokenCount);
      
      cb.println("  /* Beginning of code from template " + parserTemplate + " */");
      cb.println();
      cb.printTemplate(parserTemplate);
      cb.println();
      
      cb.println("  /* Beginning of generated code for error reporting */");
      cb.println();
      if (Options.getErrorReporting()) {
        cb.println("  /** Generate a ParseException. */");
        cb.println("  public ParseException generateParseException(string loc) {");
        cb.println("    jj_expentries.Clear();");
        cb.println("    jj_expentries_loc.Clear();");
        cb.println("    bool[] la1tokens       = new bool[" + context.globals().tokenCount + "];");
        cb.println(
            "    string[] la1tokens_loc = new string[" + context.globals().tokenCount + "];");
        cb.println("    if (jj_kind >= 0) {");
        cb.println("      la1tokens[jj_kind]     = true;");
        cb.println("      la1tokens_loc[jj_kind] = (loc != null ? loc : \"?:?\");");
        cb.println("      jj_kind = -1;");
        cb.println("    }");
        cb.println("    for (int i = 0; i < " + context.globals().maskindex + "; i++) {");
        cb.println("      if (jj_la1[i] == jj_gen) {");
        cb.println("        for (int j = 0; j < 32; j++) {");
        for (int i = 0; i < (((context.globals().tokenCount - 1) / 32) + 1); i++) {
          cb.println("          if ((jj_la1_" + i + "[i] & (1 << j)) != 0) {");
          cb.print("            la1tokens[");
          if (i != 0) {
            cb.print((32 * i) + " + ");
          }
          cb.println("j]     = true;");
          cb.print("            la1tokens_loc[");
          if (i != 0) {
            cb.print((32 * i) + " + ");
          }
          cb.println("j] = jj_la1_loc[i];");
          cb.println("          }");
        }
        cb.println("        }");
        cb.println("      }");
        cb.println("    }");
        cb.println("    for (int k = 0; k < " + context.globals().tokenCount + "; k++) {");
        cb.println("      if (la1tokens[k]) {");
        cb.println("        jj_expentry     = new int[1];");
        cb.println("        jj_expentry_loc = new string[1];");
        cb.println("        jj_expentry[0]     = k;");
        cb.println("        jj_expentry_loc[0] = la1tokens_loc[k];");
        cb.println("        jj_expentries.Add(jj_expentry);");
        cb.println("        jj_expentries_loc.Add(jj_expentry_loc);");
        cb.println("      }");
        cb.println("    }");
        if (context.globals().jj2index != 0) {
          cb.println("    jj_endpos = 0;");
          cb.println("    jj_rescan_token();");
          cb.println("    jj_add_error_token(0, 0, \"0:0\");");
        }
        cb.println("    int[][] exptokseq       = new int[jj_expentries.Count ][];");
        cb.println("    string[][] exptokseqloc = new string[jj_expentries.Count ][];");
        cb.println("    for (int x = 0; x < jj_expentries.Count ; x++) {");
        cb.println("      exptokseq[x]    = jj_expentries[x];");
        cb.println("      exptokseqloc[x] = jj_expentries_loc[x];");
        //        }
        cb.println("    }");
        cb.println(
            "    return new ParseException(token, exptokseq, exptokseqloc, tokenImage, loc);");
        cb.println("  }");
        cb.println();
        
        cb.println(
            "  private        int[]    jj_la1     = new int[" + context.globals().maskindex + "];");
        cb.println("  private        string[] jj_la1_loc = new string["
            + context.globals().maskindex + "];");
        final int tokenMaskSize = ((context.globals().tokenCount - 1) / 32) + 1;
        for (int i = 0; i < tokenMaskSize; i++) {
          cb.println("  private static uint[]   jj_la1_" + i + ";");
        }
        cb.println();
        
        cb.println("  static " + context.globals().cu_name + "() {");
        for (int i = 0; i < tokenMaskSize; i++) {
          cb.println("    jj_la1_init_" + i + "();");
        }
        cb.println("  }");
        for (int i = 0; i < tokenMaskSize; i++) {
          cb.println();
          cb.println("  private static void jj_la1_init_" + i + "() {");
          cb.print("    jj_la1_" + i + " = new uint[] {");
          for (final int[] tokenMask : context.globals().maskVals) {
            cb.print("0x" + Integer.toHexString(tokenMask[i]) + ", ");
          }
          cb.println("};");
          cb.println("  }");
        }
      } else {
        // no error reporting
        cb.println("  /** Generate a ParseException. */");
        cb.println("  public ParseException generateParseException() {");
        cb.println("    Token errortok = token.next;");
        if (Options.getKeepLineColumn()) {
          cb.println("    int line = errortok.beginLine, column = errortok.beginColumn;");
        }
        cb.println("    String mess = (errortok.kind == 0) ? tokenImage[0] : errortok.image;");
        if (Options.getKeepLineColumn()) {
          cb.println("    return new ParseException("
              + "\"Parse error at line \" + line + \", column \" + column + \".  "
              + "Encountered: \" + mess);");
        } else {
          cb.println("    return new ParseException(\"Parse error at <line:column not kept>.  "
              + "Encountered: \" + mess);");
        }
        cb.println("  }");
      }
      
      cb.println("");
      cb.println("}");
      if (!"".equals(Options.getNamespace())) {
        cb.println("\n}");
      }
      cb.close();
    }
    catch (final Exception e) {
      e.printStackTrace();
      assert (false);
    }
  }
  
  @Override
  public void finish(final CodeGeneratorSettings settings, final ParserData parserData) {}
  
  private void processProductions(final CodeGeneratorSettings settings,
      final GenericCodeBuilder codeGenerator) {
    NormalProduction p;
    //    JavaCodeProduction jp;
    //    CppCodeProduction cp;
    //    Token t = null;
    
    this.cb = codeGenerator;
    for (final Object element : parserData.bnfproductions) {
      p = (NormalProduction) element;
      if (p instanceof CodeProduction) {
        GenerateCodeProduction((CodeProduction) p, settings, codeGenerator);
      } else {
        buildPhase1Routine((BNFProduction) p);
      }
    }
    
    for (final Object element : phase2list) {
      buildPhase2Routine((Lookahead) (element));
    }
    
    int phase3index = 0;
    
    while (phase3index < phase3list.size()) {
      for (; phase3index < phase3list.size(); phase3index++) {
        setupPhase3Builds((phase3list.get(phase3index)));
      }
    }
    
    for (final Enumeration<Phase3Data> enumeration = phase3table.elements(); enumeration
        .hasMoreElements();) {
      buildPhase3Routine((enumeration.nextElement()), false, "");
    }
  }
  
  // TODO(sreeni): Fix this mess.
  private void GenerateCodeProduction(final CodeProduction production,
      final CodeGeneratorSettings settings, final CodeBuilder<?> cb) {
    Token t = (production.getReturnTypeTokens().get(0));
    cb.printTokenSetup(t);
    //    ccol = 1;
    cb.printLeadingComments(t, "  ");
    cb.print("  " + (production.getAccessMod() != null ? production.getAccessMod() + " " : ""));
    //    cline = t.beginLine;
    //    ccol = t.beginColumn;
    cb.printTokenOnly(t);
    for (int i = 1; i < production.getReturnTypeTokens().size(); i++) {
      t = (production.getReturnTypeTokens().get(i));
      cb.printToken(t);
    }
    cb.printTrailingComments(t);
    cb.print(" " + production.getLhs() + "(");
    if (production.getParameterListTokens().size() != 0) {
      cb.printTokenSetup((production.getParameterListTokens().get(0)));
      for (final Object element : production.getParameterListTokens()) {
        t = (Token) element;
        cb.printToken(t);
      }
      cb.printTrailingComments(t);
    }
    cb.print(")");
    for (final Object element : production.getThrowsList()) {
      cb.print(", ");
      final List<?> name = (List<?>) element;
      for (final Iterator<?> it2 = name.iterator(); it2.hasNext();) {
        t = (Token) it2.next();
        cb.print(t.image);
      }
    }
    cb.print(" {");
    if (Options.getDebugParser()) {
      cb.println("");
      cb.println("    trace_call(\"" + cb.escapeToUnicode(production.getLhs()) + "\");");
      cb.print("    try {");
    }
    if (production.getCodeTokens().size() != 0) {
      cb.printTokenSetup((production.getCodeTokens().get(0)));
      //      cline--;
      cb.printTokenList(production.getCodeTokens());
    }
    cb.println("");
    if (Options.getDebugParser()) {
      cb.println("    } finally {");
      cb.println("      trace_return(\"" + cb.escapeToUnicode(production.getLhs()) + "\");");
      cb.println("    }");
    }
    cb.println("  }");
    cb.println("");
  }
  
  /*
   * The phase 1 routines generates their output into String's and dumps these String's once for
   *  each method.
   * These String's contain the special characters '\u0001' to indicate a positive indent,
   *  and '\u0002' to indicate a negative indent.
   * '\n' is used to indicate a line terminator.
   * The characters '\u0003' and '\u0004' are used to delineate portions of text where '\n's
   *  should not be followed by an indentation.
   */
  
  private int indentamt;
  
  private void buildPhase1Routine(final BNFProduction p) {
    Token t = p.getReturnTypeTokens().get(0);
    final boolean voidReturn = t.kind == JavaCCParserConstants.VOID;
    cb.printTokenSetup(t);
    cb.printLeadingComments(t, "  ");
    cb.print("  " + (p.getAccessMod() != null ? p.getAccessMod() : "public") + " ");
    cb.printTokenOnly(t);
    for (int i = 1; i < p.getReturnTypeTokens().size(); i++) {
      t = (p.getReturnTypeTokens().get(i));
      cb.printToken(t);
    }
    cb.printTrailingComments(t);
    cb.print(p.getLhs() + "(");
    if (p.getParameterListTokens().size() != 0) {
      cb.printTokenSetup((p.getParameterListTokens().get(0)));
      for (final Object element : p.getParameterListTokens()) {
        t = (Token) element;
        cb.printToken(t);
      }
      cb.printTrailingComments(t);
    }
    cb.print(")");
    
    for (final Object element : p.getThrowsList()) {
      cb.print(", ");
      final List<?> name = (List<?>) element;
      for (final Iterator<?> it2 = name.iterator(); it2.hasNext();) {
        t = (Token) it2.next();
        cb.print(t.image);
      }
    }
    
    cb.print(" {");
    
    indentamt = 4;
    String fmtProd = "";
    if (Options.getDebugParser()) {
      fmtProd = fmtProd(p);
      cb.println();
      cb.println("    trace_call(\"" + fmtProd + "\");");
      cb.print("    try {");
      if (DCT) {
        cb.print(" /*bp1r-a*/");
      }
      indentamt = 6;
    }
    
    if (!Options.getIgnoreActions() && (p.getDeclarationTokens().size() != 0)) {
      cb.println();
      cb.printTokenSetup((p.getDeclarationTokens().get(0)));
      for (final Object element : p.getDeclarationTokens()) {
        t = (Token) element;
        cb.printToken(t);
      }
      cb.printTrailingComments(t);
    }
    
    final String code = phase1ExpansionGen(p.getExpansion());
    dumpFormattedString(code);
    cb.println();
    
    if (p.isJumpPatched() && !voidReturn) {
      cb.println("    throw new System.Exception(\"Missing return statement in function\");");
    }
    if (Options.getDebugParser()) {
      cb.print("    } finally {");
      if (DCT) {
        cb.print(" /*bp1r-b*/");
      }
      cb.println();
      cb.println("      trace_return(\"" + fmtProd + "\");");
      cb.println("    }");
    }
    
    cb.println("  }");
    cb.println();
  }
  
  private int gensymindex = 0;
  
  private String phase1ExpansionGen(final Expansion e) {
    String retval = "";
    Token t = null;
    Lookahead[] conds;
    String[] actions;
    if (e instanceof RegularExpression) {
      final RegularExpression e_nrw = (RegularExpression) e;
      retval += "\n";
      if (e_nrw.lhsTokens.size() != 0) {
        cb.printTokenSetup((e_nrw.lhsTokens.get(0)));
        for (final Object element : e_nrw.lhsTokens) {
          t = (Token) element;
          retval += CodeBuilder.toString(t);
        }
        retval += cb.getTrailingComments(t);
        retval += " = ";
      }
      Object label = e_nrw.label;
      if (label.equals("")) {
        // See if there is a name given.
        label = parserData.namesOfTokens.get(e_nrw.ordinal);
      }
      if (label == null) {
        label = e_nrw.ordinal;
      }
      retval += "jj_consume_token(" + label;
      if (Options.getErrorReporting()) {
        retval += ", \"" + e.getLine() + ":" + e.getColumn() + "\"";
      }
      // We allow things like String s = <MYTOKEN>.image
      retval += e_nrw.rhsToken == null ? ");" : ")." + e_nrw.rhsToken.image + ";";
      if (DCT) {
        retval += " /*p1eg-re*/";
      }
      
    } else if (e instanceof NonTerminal) {
      final NonTerminal e_nrw = (NonTerminal) e;
      retval += "\n";
      if (e_nrw.getLhsTokens().size() != 0) {
        cb.printTokenSetup((e_nrw.getLhsTokens().get(0)));
        for (final Object element : e_nrw.getLhsTokens()) {
          t = (Token) element;
          retval += CodeBuilder.toString(t);
        }
        retval += cb.getTrailingComments(t);
        retval += " = ";
      }
      retval += e_nrw.getName() + "(";
      if (e_nrw.getArgumentTokens().size() != 0) {
        cb.printTokenSetup((e_nrw.getArgumentTokens().get(0)));
        for (final Object element : e_nrw.getArgumentTokens()) {
          t = (Token) element;
          retval += CodeBuilder.toString(t);
        }
        retval += cb.getTrailingComments(t);
      }
      if (DCT) {
        retval += " /*p1eg-nt*/ ";
      }
      retval += ");";
      
    } else if (e instanceof Action) {
      final Action e_nrw = (Action) e;
      if (!Options.getIgnoreActions() && (e_nrw.getActionTokens().size() != 0)) {
        if (DCT) {
          retval += " /*p1eg-ac*/";
        }
        retval += "\n "; // half indent for distinguishing user actions from generated code
        // this formatting is ok for an action of a single line, not of multiple lines
        String code = "";
        cb.printTokenSetup((e_nrw.getActionTokens().get(0)));
        for (final Object element : e_nrw.getActionTokens()) {
          t = (Token) element;
          code += CodeBuilder.toString(t);
        }
        code += cb.getTrailingComments(t);
        retval += code.trim();
      }
      
    } else if (e instanceof Choice) {
      final Choice e_nrw = (Choice) e;
      final int nbChoices = e_nrw.getChoices().size();
      conds = new Lookahead[nbChoices];
      actions = new String[nbChoices + 1];
      for (int i = 0; i < e_nrw.getChoices().size(); i++) {
        final Sequence nestedSeq = (Sequence) (e_nrw.getChoices().get(i));
        actions[i] = phase1ExpansionGen(nestedSeq);
        conds[i] = (Lookahead) (nestedSeq.units.get(0));
      }
      // note 1: jj_consume_token(-1...) should raise a ParseException;
      //  the following throw is there to avoid compiler errors (like uninitialized variables)
      if (Options.getErrorReporting()) {
        actions[nbChoices] = "\njj_consume_token(-1, \"*loc*\");"
            + "\n// above statement should throw a ParseException.";
      } else {
        actions[nbChoices] = "\njj_consume_token(-1);"
            + "\n// above statement should throw a ParseException.";
      }
      retval = buildLookaheadChecker(conds, actions, e);
      
    } else if (e instanceof Sequence) {
      final Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the Lookahead object.
      for (int i = 1; i < e_nrw.units.size(); i++) {
        retval += phase1ExpansionGen((e_nrw.units.get(i)));
      }
      
    } else if (e instanceof OneOrMore) {
      final OneOrMore e_nrw = (OneOrMore) e;
      final Expansion nested_e = e_nrw.getExpansion();
      Lookahead la;
      if (nested_e instanceof Sequence) {
        la = (Lookahead) (((Sequence) nested_e).units.get(0));
      } else {
        la = new Lookahead();
        la.setAmount(Options.getLookahead());
        la.setLaExpansion(nested_e);
      }
      if (DCT) {
        retval += " /*p1eg-1n-1*/";
      }
      retval += "\n";
      final int labelIndex = ++gensymindex;
      retval += "while (!hasError) {\u0001";
      retval += phase1ExpansionGen(nested_e);
      conds = new Lookahead[1];
      conds[0] = la;
      actions = new String[2];
      actions[0] = "";
      actions[1] = "\ngoto end_label_" + labelIndex + ";";
      retval += buildLookaheadChecker(conds, actions, e);
      if (DCT) {
        retval += " /*p1eg-1n-2*/";
      }
      retval += "\u0002\n" + "}";
      retval += "\nend_label_" + labelIndex + ": ;";
      
    } else if (e instanceof ZeroOrMore) {
      final ZeroOrMore e_nrw = (ZeroOrMore) e;
      final Expansion nested_e = e_nrw.getExpansion();
      Lookahead la;
      if (nested_e instanceof Sequence) {
        la = (Lookahead) (((Sequence) nested_e).units.get(0));
      } else {
        la = new Lookahead();
        la.setAmount(Options.getLookahead());
        la.setLaExpansion(nested_e);
      }
      if (DCT) {
        retval += " /*p1eg-0n-1*/";
      }
      retval += "\n";
      final int labelIndex = ++gensymindex;
      retval += "while (!hasError) {\u0001";
      conds = new Lookahead[1];
      conds[0] = la;
      actions = new String[2];
      actions[0] = "";
      actions[1] = "\ngoto end_label_" + labelIndex + ";";
      retval += buildLookaheadChecker(conds, actions, e);
      retval += phase1ExpansionGen(nested_e);
      if (DCT) {
        retval += " /*p1eg-0n-2*/";
      }
      retval += "\u0002\n" + "}";
      retval += "\nend_label_" + labelIndex + ": ;";
      
    } else if (e instanceof ZeroOrOne) {
      final ZeroOrOne e_nrw = (ZeroOrOne) e;
      final Expansion nested_e = e_nrw.getExpansion();
      Lookahead la;
      if (nested_e instanceof Sequence) {
        la = (Lookahead) (((Sequence) nested_e).units.get(0));
      } else {
        la = new Lookahead();
        la.setAmount(Options.getLookahead());
        la.setLaExpansion(nested_e);
      }
      conds = new Lookahead[1];
      conds[0] = la;
      actions = new String[2];
      actions[0] = phase1ExpansionGen(nested_e);
      actions[1] = "";
      retval += buildLookaheadChecker(conds, actions, e);
      
    } else if (e instanceof TryBlock) {
      final TryBlock e_nrw = (TryBlock) e;
      final Expansion nested_e = e_nrw.exp;
      List<?> list;
      retval += "\n";
      retval += "try {\u0001";
      if (DCT) {
        retval += " /*p1eg-tb-1*/";
      }
      retval += phase1ExpansionGen(nested_e);
      retval += "\u0002\n" + "}";
      if (DCT) {
        retval += " /*p1eg-tb-2*/ ";
      }
      for (final List<Token> element : e_nrw.catchblks) {
        retval += " catch {";
        // why different here from java & c# (no printing on e_nrw.types)?
        list = element;
        if (list.size() != 0) {
          cb.printTokenSetup((Token) (list.get(0)));
          for (final Iterator<?> it = list.iterator(); it.hasNext();) {
            t = (Token) it.next();
            retval += CodeBuilder.toString(t);
          }
          retval += cb.getTrailingComments(t);
        }
        retval += "\u0004\n" + "}";
        if (DCT) {
          retval += " /*p1eg-tb-3*/";
        }
      }
      if (e_nrw.finallyblk != null) {
        retval += " finally {\u0003\n";
        if (e_nrw.finallyblk.size() != 0) {
          cb.printTokenSetup((e_nrw.finallyblk.get(0)));
          for (final Object element : e_nrw.finallyblk) {
            t = (Token) element;
            retval += CodeBuilder.toString(t);
          }
          retval += cb.getTrailingComments(t);
        }
        retval += "\u0004\n" + "}";
        if (DCT) {
          retval += " /*p1eg-tb-4*/";
        }
      }
    }
    
    return retval;
  }
  
  /* Constants used in the following method "buildLookaheadChecker". */
  final int   NOOPENSTM   = 0;
  final int   OPENIF      = 1;
  final int   OPENSWITCH  = 2;
  private int switchIndex = 0;
  
  /**
   * This method takes two parameters - an array of Lookahead's "<code>conds</code>", and an array
   * of String's "<code>actions</code>".<br>
   * "<code>actions</code>" contains exactly one element more than "<code>conds</code>".<br>
   * "<code>actions</code>" are Java source code, and "<code>conds</code>" translate to conditions
   * <br>
   * - so lets say "<code>f(conds[i])</code>" is <code>true</code> if the lookahead required by "
   * <code>conds[i] </code>" is indeed the case. <br>
   * This method returns a string corresponding to the Java code for: <br>
   * <code>
   * if (f(conds[0]) actions[0]<br>
   * else if (f(conds[1]) actions[1]<br>
   * . . .<br>
   * else actions[action.length-1]
   * </code> <br>
   * A particular action entry ("<code>actions[i]</code>") can be <code>null</code>, in which case,
   * a noop is generated for that action.
   */
  private String buildLookaheadChecker(final Lookahead[] conds, final String[] actions,
      final Expansion exp) {
    
    // The state variables.
    int state = NOOPENSTM;
    int indentAmt = 0;
    final boolean[] casedValues = new boolean[parserData.tokenCount];
    String retval = "";
    Lookahead la;
    Token t = null;
    final int tokenMaskSize = ((parserData.tokenCount - 1) / 32) + 1;
    int[] tokenMask = null;
    
    // Iterate over all the conditions.
    int index = 0;
    while (index < conds.length) {
      
      la = conds[index];
      jj2LA = false;
      
      if ((la.getAmount() == 0) || Semanticize.emptyExpansionExists(la.getLaExpansion())) {
        
        // This handles the following cases:
        // . If syntactic lookahead is not wanted (and hence explicitly specified as 0).
        // . If it is possible for the lookahead expansion to recognize the empty string
        //    - in which case the lookahead trivially passes.
        // . If the lookahead expansion has a JAVACODE production that it directly expands to
        //    - in which case the lookahead trivially passes.
        if (la.getActionTokens().size() == 0) {
          // In addition, if there is no semantic lookahead, then the lookahead trivially succeeds.
          // So break the main loop and treat this case as the default last action.
          break;
        } else {
          // This case is when there is only semantic lookahead (without any preceding syntactic
          //  lookahead). In this case, an "if" statement is generated.
          switch (state) {
          case NOOPENSTM:
            retval += "\n" + "if (";
            if (DCT) {
              retval += " /*semla1*/";
            }
            indentAmt++;
            break;
          case OPENIF:
            retval += "\u0002\n" + "} else if (";
            if (DCT) {
              retval += " /*semla2*/";
            }
            break;
          case OPENSWITCH:
            retval += " else {" + "\u0001";
            if (DCT) {
              retval += " /*semla3*/";
            }
            if (Options.getErrorReporting()) {
              retval += "\njj_la1[" + context.globals().maskindex + "]     = jj_gen;";
              retval += "\njj_la1_loc[" + context.globals().maskindex + "] = \"" + exp.getLine()
                  + ":" + exp.getColumn() + "\";";
              context.globals().maskindex++;
              context.globals().maskVals.add(tokenMask);
            }
            retval += "\n" + "if (";
            if (DCT) {
              retval += " /*semla4*/ ";
            }
            indentAmt++;
          }
          cb.printTokenSetup((la.getActionTokens().get(0)));
          for (final Object element : la.getActionTokens()) {
            t = (Token) element;
            retval += CodeBuilder.toString(t);
          }
          retval += cb.getTrailingComments(t);
          if (DCT) {
            retval += " /*semla5*/ ";
          }
          retval += ") {\u0001" + actions[index];
          state = OPENIF;
        }
        
      } else if ((la.getAmount() == 1) && (la.getActionTokens().size() == 0)) {
        // Special optimal processing when the lookahead is exactly 1,
        //  and there is no semantic lookahead.
        
        if (firstSet == null) {
          firstSet = new boolean[parserData.tokenCount];
        }
        for (int i = 0; i < parserData.tokenCount; i++) {
          firstSet[i] = false;
        }
        // jj2LA is set to false at the beginning of the containing "if" statement.
        // It is checked immediately after the end of the same statement to determine
        //  if lookaheads are to be performed using calls to the jj2 methods.
        genFirstSet(la.getLaExpansion());
        // genFirstSet may find that semantic attributes are appropriate for the next token.
        // In which case, it sets jj2LA to true.
        if (!jj2LA) {
          
          // This case is if there is no applicable semantic lookahead and the lookahead is one
          //  (excluding the earlier cases such as JAVACODE, etc.).
          switch (state) {
          case OPENIF:
            retval += "\u0002\n" + "} else {\u0001";
            if (DCT) {
              retval += " /*la11*/";
            }
            // Control flows through to next case.
          case NOOPENSTM:
            retval += "\n" + "int switch_" + ++switchIndex + " = ";
            // why no Options.getCacheTokens() here as in java & cpp?
            if (DCT) {
              retval += " /*la12*/ ";
            }
            retval += "(jj_ntk == -1) ? jj_ntk_f() : jj_ntk;\n";
            for (int i = 0; i < parserData.tokenCount; i++) {
              casedValues[i] = false;
            }
            indentAmt++;
            tokenMask = new int[tokenMaskSize];
            for (int i = 0; i < tokenMaskSize; i++) {
              tokenMask[i] = 0;
            }
            break;
          case OPENSWITCH:
            retval += " else ";
            if (DCT) {
              retval += "/*la13*/ ";
            }
            break;
          }
          retval += "if (false\u0001";
          if (DCT) {
            retval += " /*la14*/";
          }
          for (int i = 0; i < parserData.tokenCount; i++) {
            if (firstSet[i]) {
              if (!casedValues[i]) {
                casedValues[i] = true;
                retval += " ||\n\u0001  switch_" + switchIndex + " == ";
                if (DCT) {
                  retval += "/*la15*/ ";
                }
                final int j1 = i / 32;
                final int j2 = i % 32;
                tokenMask[j1] |= 1 << j2;
                final String s = (parserData.namesOfTokens.get(i));
                if (s == null) {
                  retval += i;
                } else {
                  retval += s;
                }
                retval += "\u0002";
              }
            }
          }
          retval += ") {";
          if (DCT) {
            retval += " /*la16*/";
          }
          retval += actions[index];
          if (DCT) {
            retval += " /*la20*/";
          }
          retval += "\u0002\n}";
          state = OPENSWITCH;
        }
        
      } else {
        // This is the case when lookahead is determined through calls to jj2 methods.
        // The other case is when lookahead is 1, but semantic attributes need to be evaluated.
        // Hence this crazy control structure.
        
        jj2LA = true;
      }
      
      if (jj2LA) {
        // In this case lookahead is determined by the jj2 methods.
        
        switch (state) {
        case NOOPENSTM:
          retval += "\n" + "if (";
          if (DCT) {
            retval += " /*jj21*/ ";
          }
          indentAmt++;
          break;
        case OPENIF:
          retval += "\u0002\n" + "} else if (";
          if (DCT) {
            retval += " /*jj22*/ ";
          }
          break;
        case OPENSWITCH:
          retval += "else {" + "\u0001";
          if (DCT) {
            retval += " /*jj23*/";
          }
          if (Options.getErrorReporting()) {
            retval += "\njj_la1[" + context.globals().maskindex + "]     = jj_gen;";
            retval += "\njj_la1_loc[" + context.globals().maskindex + "] = \"" + exp.getLine() + ":"
                + exp.getColumn() + "\";";
            context.globals().maskindex++;
            context.globals().maskVals.add(tokenMask);
          }
          retval += "\n" + "if (";
          if (DCT) {
            retval += " /*jj24*/";
          }
          indentAmt++;
        }
        context.globals().jj2index++;
        // At this point, la.la_expansion.internal_name must be "".
        internalNames.put(la.getLaExpansion(), "_" + context.globals().jj2index);
        internalIndexes.put(la.getLaExpansion(), context.globals().jj2index);
        phase2list.add(la);
        retval += "LA_Phase2_Success == jj_2" + internalNames.get(la.getLaExpansion()) + "("
            + la.getAmount() + (DCT ? " /*jj25*/" : "") + ")";
        if (la.getActionTokens().size() != 0) {
          // In addition, there is also a semantic lookahead.
          // So concatenate the semantic check with the syntactic one.
          retval += " && (";
          if (DCT) {
            retval += " /*jj26*/";
          }
          cb.printTokenSetup((la.getActionTokens().get(0)));
          for (final Object element : la.getActionTokens()) {
            t = (Token) element;
            retval += CodeBuilder.toString(t);
          }
          retval += cb.getTrailingComments(t);
          retval += ")";
        }
        retval += ") {\u0001" + actions[index];
        if (DCT) {
          retval += " /*jj27*/";
        }
        state = OPENIF;
      }
      
      index++;
    }
    
    // Generate code for the default case. Note this may not be the last entry of "actions"
    //  if any condition can be statically determined to be always "true".
    
    switch (state) {
    case NOOPENSTM:
      if (Options.getErrorReporting()) {
        retval += actions[index].replace("*loc*", "n/a");
      } else {
        retval += actions[index];
      }
      break;
    case OPENIF:
      retval += "\u0002\n" + "} else {\u0001";
      if (DCT) {
        retval += " /*la91*/";
      }
      if (Options.getErrorReporting()) {
        retval += actions[index].replace("*loc*", "n/a");
        
      } else {
        retval += actions[index];
      }
      break;
    case OPENSWITCH:
      retval += " else {" + "\u0001";
      if (DCT) {
        retval += "/*la92*/";
      }
      if (Options.getErrorReporting()) {
        retval += "\njj_la1[" + context.globals().maskindex + "]     = jj_gen;";
        retval += "\njj_la1_loc[" + context.globals().maskindex + "] = \"" + exp.getLine() + ":"
            + exp.getColumn() + "\";";
        retval += actions[index].replace("*loc*", exp.getLine() + ":" + exp.getColumn());
        context.globals().maskindex++;
        context.globals().maskVals.add(tokenMask);
      } else {
        retval += actions[index];
      }
      break;
    }
    for (int i = 0; i < indentAmt; i++) {
      retval += "\u0002\n}";
      if (DCT) {
        retval += " /*la93*/";
      }
    }
    
    return retval;
  }
  
  /**
   * An array used to store the first sets generated by the following method. A true entry means
   * that the corresponding token is in the first set.
   */
  private boolean[] firstSet;
  
  /**
   * Sets up the array "firstSet" above based on the Expansion argument passed to it. Since this is
   * a recursive function, it assumes that "firstSet" has been reset before the first call.
   */
  private void genFirstSet(final Expansion exp) {
    if (exp instanceof RegularExpression) {
      firstSet[((RegularExpression) exp).ordinal] = true;
    } else if (exp instanceof NonTerminal) {
      if (!(((NonTerminal) exp).getProd() instanceof CodeProduction)) {
        genFirstSet((((NonTerminal) exp).getProd()).getExpansion());
      }
    } else if (exp instanceof Choice) {
      final Choice ch = (Choice) exp;
      for (final Expansion element : ch.getChoices()) {
        genFirstSet((element));
      }
    } else if (exp instanceof Sequence) {
      final Sequence seq = (Sequence) exp;
      final Object obj = seq.units.get(0);
      if ((obj instanceof Lookahead) && (((Lookahead) obj).getActionTokens().size() != 0)) {
        jj2LA = true;
      }
      for (int i = 0; i < seq.units.size(); i++) {
        final Expansion unit = seq.units.get(i);
        // Javacode productions can not have FIRST sets.
        // Instead we generate the FIRST set for the preceding LOOKAHEAD
        //  (the semantic checks should have made sure that the LOOKAHEAD is suitable).
        if ((unit instanceof NonTerminal)
            && (((NonTerminal) unit).getProd() instanceof CodeProduction)) {
          if ((i > 0) && (seq.units.get(i - 1) instanceof Lookahead)) {
            final Lookahead la = (Lookahead) seq.units.get(i - 1);
            genFirstSet(la.getLaExpansion());
          }
        } else {
          genFirstSet((seq.units.get(i)));
        }
        if (!Semanticize.emptyExpansionExists((seq.units.get(i)))) {
          break;
        }
      }
    } else if (exp instanceof OneOrMore) {
      final OneOrMore om = (OneOrMore) exp;
      genFirstSet(om.getExpansion());
    } else if (exp instanceof ZeroOrMore) {
      final ZeroOrMore zm = (ZeroOrMore) exp;
      genFirstSet(zm.getExpansion());
    } else if (exp instanceof ZeroOrOne) {
      final ZeroOrOne zo = (ZeroOrOne) exp;
      genFirstSet(zo.getExpansion());
    } else if (exp instanceof TryBlock) {
      final TryBlock tb = (TryBlock) exp;
      genFirstSet(tb.exp);
    }
  }
  
  private void dumpFormattedString(final String str) {
    char ch = ' ';
    char prevChar;
    boolean indentOn = true;
    for (int i = 0; i < str.length(); i++) {
      prevChar = ch;
      ch = str.charAt(i);
      if ((ch == '\n') && (prevChar == '\r')) {
        // do nothing - we've already printed a new line for the '\r' during the previous iteration.
      } else if ((ch == '\n') || (ch == '\r')) {
        cb.println();
        if (indentOn) {
          for (int i1 = 0; i1 < indentamt; i1++) {
            cb.print(" ");
          }
        }
      } else if (ch == '\u0001') {
        indentamt += 2;
      } else if (ch == '\u0002') {
        indentamt -= 2;
      } else if (ch == '\u0003') {
        indentOn = false;
      } else if (ch == '\u0004') {
        indentOn = true;
      } else {
        cb.print(ch);
      }
    }
  }
  
  private String internalName(final Expansion e) {
    return internalNames.containsKey(e) ? internalNames.get(e) : "";
  }
  
  private int internalIndex(final Expansion e) {
    return internalIndexes.get(e);
  }
  
  private void buildPhase2Routine(final Lookahead la) {
    final Expansion e = la.getLaExpansion();
    cb.println("  private bool jj_2" + internalName(e) + "(int xla) {");
    cb.println("    jj_la = xla; jj_lastpos = jj_scanpos = token;");
    cb.println("    jj_laok = LA_Scan_Token_Failure;");
    
    if (Options.getDebugLookahead()) {
      // parent null for a top level lookahead expansion,
      //  need to go through the lookahead itself (with mod in grammar)
      Object par = e.parent != null ? e.parent : la.parent;
      while (par != null && !(par instanceof NormalProduction) && (par instanceof Expansion)) {
        par = ((Expansion) par).parent;
      }
      final NormalProduction prod = ((NormalProduction) par);
      cb.println(
          "    trace_la_call(\"Entering LOOKAHEAD (\" + xla + \") " + fmtAt(e, prod) + "\");");
      cb.println("    bool rc = jj_3" + internalNames.get(e) + "()" + ";");
      cb.println("    if (LA_Scan_Token_Success == jj_laok) {");
      cb.println(
          "      trace_la_return(\"Caught SUCCESSFUL LOOKAHEAD (\" + xla + \"/\" + jj_la + \") "
              + fmtAt(e, prod) + "\");");
      if (Options.getErrorReporting()) {
        cb.println("      jj_save(" + (internalIndex(e) - 1) + ", xla);");
      }
      cb.println("      return LA_Phase2_Success;");
      cb.println("    } else {");
      cb.println("      trace_la_return(\"Exiting \" + (rc ? \"FAILED\" : \"SUCCESSFUL\") + \""
          + " LOOKAHEAD (\" + xla + \"/\" + jj_la + \") " + fmtAt(e, prod) + "\");");
      if (Options.getErrorReporting()) {
        cb.println("      jj_save(" + (internalIndex(e) - 1) + ", xla);");
      }
      cb.println("      return (!rc);");
      cb.println("    }");
      
    } else {
      // no DebugLookahead
      cb.println("    if (!jj_3" + internalName(e)
          + "() || (LA_Scan_Token_Success == jj_laok)) return LA_Phase2_Success;");
      if (Options.getErrorReporting()) {
        cb.println("    jj_save(" + (internalIndex(e) - 1) + ", xla);");
      }
      cb.println("    return LA_Phase2_Failure;");
    }
    
    cb.println("  }");
    cb.println();
    final Phase3Data p3d = new Phase3Data(e, la.getAmount());
    phase3list.add(p3d);
    phase3table.put(e, p3d);
  }
  
  private static String fmtAt(final Expansion e, final NormalProduction prod) {
    return "(at " + e.getLine() + ":" + e.getColumn() + " in " + fmtProd(prod) + ")";
  }
  
  private void setupPhase3Builds(final Phase3Data inf) {
    final Expansion e = inf.exp;
    if (e instanceof RegularExpression) {
      // nothing to here
      
    } else if (e instanceof NonTerminal) {
      // All expansions of non-terminals have the "name" fields set. So
      // there's no need to check it below for "e_nrw" and "ntexp". In
      // fact, we rely here on the fact that the "name" fields of both these
      // variables are the same.
      final NonTerminal e_nrw = (NonTerminal) e;
      final NormalProduction ntprod = (parserData.productionTable.get(e_nrw.getName()));
      if (ntprod instanceof CodeProduction) {
        // nothing to do here
      } else {
        generate3R(ntprod.getExpansion(), inf);
      }
      
    } else if (e instanceof Choice) {
      final Choice e_nrw = (Choice) e;
      for (final Expansion element : e_nrw.getChoices()) {
        generate3R((element), inf);
      }
      
    } else if (e instanceof Sequence) {
      final Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      int cnt = inf.count;
      for (int i = 1; i < e_nrw.units.size(); i++) {
        final Expansion eseq = (e_nrw.units.get(i));
        setupPhase3Builds(new Phase3Data(eseq, cnt));
        cnt -= minimumSize(eseq);
        if (cnt <= 0) {
          break;
        }
      }
      
    } else if (e instanceof TryBlock) {
      final TryBlock e_nrw = (TryBlock) e;
      setupPhase3Builds(new Phase3Data(e_nrw.exp, inf.count));
      
    } else if (e instanceof OneOrMore) {
      final OneOrMore e_nrw = (OneOrMore) e;
      generate3R(e_nrw.getExpansion(), inf);
      
    } else if (e instanceof ZeroOrMore) {
      final ZeroOrMore e_nrw = (ZeroOrMore) e;
      generate3R(e_nrw.getExpansion(), inf);
      
    } else if (e instanceof ZeroOrOne) {
      final ZeroOrOne e_nrw = (ZeroOrOne) e;
      generate3R(e_nrw.getExpansion(), inf);
    }
  }
  
  private void generate3R(final Expansion e, final Phase3Data inf) {
    Expansion seq = e;
    if (internalName(e).equals("")) {
      while (true) {
        if ((seq instanceof Sequence) && (((Sequence) seq).units.size() == 2)) {
          seq = ((Sequence) seq).units.get(1);
        } else if (seq instanceof NonTerminal) {
          final NonTerminal e_nrw = (NonTerminal) seq;
          final NormalProduction ntprod = parserData.productionTable.get(e_nrw.getName());
          if (ntprod instanceof CodeProduction) {
            break; // nothing to do here
          } else {
            seq = ntprod.getExpansion();
          }
        } else {
          break;
        }
      }
      
      if (seq instanceof RegularExpression) {
        final RegularExpression re = (RegularExpression) seq;
        String str = "jj_scan_token(";
        str += re.ordinal;
        if (Options.getErrorReporting()) {
          str += ", \"" + e.getLine() + ":" + e.getColumn() + "\"";
        }
        str += ")";
        if (DCT) {
          str += " /*g3r-re*/";
        }
        internalNames.put(e, str);
        return;
      }
      
      gensymindex++;
      internalNames.put(e, "R_" + e.getProductionName() + "_" + e.getLine() + "_" + e.getColumn()
          + "_" + gensymindex + (DCT ? "/*g3r-!re*/" : ""));
      internalIndexes.put(e, gensymindex);
    }
    Phase3Data p3d = (phase3table.get(e));
    if ((p3d == null) || (p3d.count < inf.count)) {
      p3d = new Phase3Data(e, inf.count);
      phase3list.add(p3d);
      phase3table.put(e, p3d);
    }
  }
  
  private boolean xsp_declared;
  
  Expansion jj3_expansion;
  
  private void buildPhase3Routine(final Phase3Data inf, final boolean recursive_call,
      final String indent) {
    final Expansion e = inf.exp;
    final String name = internalName(e);
    if (name.startsWith("jj_scan_token")) {
      return;
    }
    Token t = null;
    final String ind = indent;
    
    if (!recursive_call) {
      cb.println("  private bool jj_3" + name + "() {");
      cb.println("    if (LA_Scan_Token_Success == jj_laok) { return LA_Phase3_Success; }");
      xsp_declared = false;
      if (Options.getDebugLookahead() && (e.parent instanceof NormalProduction)) {
        cb.print("    ");
        if (Options.getErrorReporting()) {
          cb.print("if (!jj_rescan) { ");
        }
        cb.print("trace_la_call(\"" + fmtProd((NormalProduction) e.parent)
            + ": looking ahead (\" + jj_la + \")...\");");
        if (Options.getErrorReporting()) {
          cb.print(" }");
        }
        cb.println();
        jj3_expansion = e;
      } else {
        jj3_expansion = null;
      }
    }
    
    if (e instanceof RegularExpression) {
      final RegularExpression e_nrw = (RegularExpression) e;
      // RStringLiteral
      Object kindStr = e_nrw.label;
      if (kindStr.equals("")) {
        // RStringLiteral
        kindStr = parserData.namesOfTokens.get(e_nrw.ordinal);
      }
      if (kindStr == null) {
        // RJustName
        kindStr = e_nrw.ordinal;
      }
      cb.print(ind + "    if (LA_Scan_Token_Failure == jj_scan_token(" + kindStr);
      if (Options.getErrorReporting()) {
        cb.print(", \"" + e.getLine() + ":" + e.getColumn() + "\"");
      }
      cb.println(")) {");
      cb.println(ind + "      " + genReturn(true, 0, ind));
      cb.print(ind + "    }");
      if (DCT) {
        cb.print(" /*bp3r-re*/");
      }
      cb.println();
      
    } else if (e instanceof NonTerminal) {
      // All expansions of non-terminals have the "name" fields set.
      // So there's no need to check it below for "e_nrw" and "ntexp".
      // We rely here on the fact that the "name" fields of both these variables are the same.
      final NonTerminal e_nrw = (NonTerminal) e;
      final NormalProduction ntprod = (parserData.productionTable.get(e_nrw.getName()));
      if (ntprod instanceof CodeProduction) {
        cb.println(ind + "    if (true) {");
        cb.println(ind + "      jj_la = 0;");
        cb.println(ind + "      jj_scanpos = jj_lastpos;");
        cb.println(ind + "      " + genReturn(false, 0, ind));
        cb.print(ind + "    }");
        if (DCT) {
          cb.print(" /*bp3r-nt1*/");
        }
        cb.println();
      } else {
        final Expansion ntexp = ntprod.getExpansion();
        cb.println(ind + "    if (" + genjj_3Call(ntexp) + ") {");
        cb.println(ind + "      " + genReturn(true, 0, ind));
        cb.print(ind + "    }");
        if (DCT) {
          cb.print(" /*bp3r-nt2*/");
        }
        cb.println();
      }
      
    } else if (e instanceof Choice) {
      Sequence nested_seq;
      final Choice e_nrw = (Choice) e;
      if (e_nrw.getChoices().size() != 1) {
        if (!xsp_declared) {
          xsp_declared = true;
          cb.println(ind + "    " + getTypeForToken() + " xsp;");
        }
        cb.println(ind + "    xsp = jj_scanpos;");
      }
      for (int i = 0; i < e_nrw.getChoices().size(); i++) {
        String dec = "";
        for (int k = 0; k < i; k++) {
          dec += "  ";
        }
        nested_seq = (Sequence) (e_nrw.getChoices().get(i));
        final Lookahead la = (Lookahead) (nested_seq.units.get(0));
        if (la.getActionTokens().size() != 0) {
          // We have semantic lookahead that must be evaluated.
          cb.println(ind + "    jj_lookingAhead = true;");
          cb.print(ind + "    jj_semLA = ");
          cb.printTokenSetup((la.getActionTokens().get(0)));
          for (final Object element : la.getActionTokens()) {
            t = (Token) element;
            cb.printToken(t);
          }
          //          gcb.printTrailingComments(t);
          cb.println(ind + ";");
          cb.println(ind + "    jj_lookingAhead = false;");
        }
        cb.print(dec + ind + "    if (");
        if (DCT) {
          cb.print("/*bp3r-ch1*/ ");
        }
        if (la.getActionTokens().size() != 0) {
          cb.print("!jj_semLA || ");
        }
        cb.println(genjj_3Call(nested_seq) + ") {");
        if (i != (e_nrw.getChoices().size() - 1)) {
          cb.println(dec + ind + "      jj_scanpos = xsp;");
        } else {
          cb.println(dec + ind + "      " + genReturn(true, i, ind));
          cb.print(dec + ind + "    }");
          if (DCT) {
            cb.print(" /*bp3r-ch2*/");
          }
          cb.println();
        }
      }
      for (int i = e_nrw.getChoices().size(); i > 1; i--) {
        for (int k = i - 1; k > 1; k--) {
          cb.print("  ");
        }
        cb.print(ind + "    }");
        if (DCT) {
          cb.print(" /*bp3r-ch3*/");
        }
        cb.println();
      }
      
    } else if (e instanceof Sequence) {
      final Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the Lookahead object.
      int cnt = inf.count;
      for (int i = 1; i < e_nrw.units.size(); i++) {
        final Expansion eseq = (e_nrw.units.get(i));
        buildPhase3Routine(new Phase3Data(eseq, cnt), true, ind);
        cnt -= minimumSize(eseq);
        if (cnt <= 0) {
          break;
        }
      }
      
    } else if (e instanceof TryBlock) {
      final TryBlock e_nrw = (TryBlock) e;
      buildPhase3Routine(new Phase3Data(e_nrw.exp, inf.count), true, ind);
      
    } else if (e instanceof OneOrMore) {
      if (!xsp_declared) {
        xsp_declared = true;
        cb.println(ind + "    " + getTypeForToken() + " xsp;");
      }
      final OneOrMore e_nrw = (OneOrMore) e;
      final Expansion nested_e = e_nrw.getExpansion();
      cb.println(ind + "    if (" + genjj_3Call(nested_e) + ") {");
      cb.println(ind + "      " + genReturn(true, 0, ind));
      cb.print(ind + "    }");
      if (DCT) {
        cb.print(" /*bp3r-1n1*/");
      }
      cb.println();
      cb.println(ind + "    while (true) {");
      cb.println(ind + "      xsp = jj_scanpos;");
      cb.println(ind + "      if (" + genjj_3Call(nested_e) + ") {");
      cb.println(ind + "        jj_scanpos = xsp;");
      cb.println(ind + "        break;");
      cb.println(ind + "      }");
      cb.print(ind + "    }");
      if (DCT) {
        cb.print(" /*bp3r-1n2*/");
      }
      cb.println();
      
    } else if (e instanceof ZeroOrMore) {
      if (!xsp_declared) {
        xsp_declared = true;
        cb.println(ind + "    " + getTypeForToken() + " xsp;");
      }
      final ZeroOrMore e_nrw = (ZeroOrMore) e;
      final Expansion nested_e = e_nrw.getExpansion();
      cb.println(ind + "    while (true) {");
      cb.println(ind + "      xsp = jj_scanpos;");
      cb.println(ind + "      if (" + genjj_3Call(nested_e) + ") {");
      cb.println(ind + "        jj_scanpos = xsp;");
      cb.println(ind + "        break;");
      cb.println(ind + "      }");
      cb.print(ind + "    }");
      if (DCT) {
        cb.print(" /*bp3r-0n*/");
      }
      cb.println();
      
    } else if (e instanceof ZeroOrOne) {
      if (!xsp_declared) {
        xsp_declared = true;
        cb.println(ind + "    " + getTypeForToken() + " xsp;");
      }
      final ZeroOrOne e_nrw = (ZeroOrOne) e;
      final Expansion nested_e = e_nrw.getExpansion();
      cb.println(ind + "    xsp = jj_scanpos;");
      cb.println(ind + "    if (" + genjj_3Call(nested_e) + ") {");
      cb.println(ind + "      jj_scanpos = xsp;");
      cb.print(ind + "    }");
      if (DCT) {
        cb.print(" /*bp3r-01*/");
      }
      cb.println();
    }
    
    if (!recursive_call) {
      if (Options.getDebugLookahead() && (e.parent instanceof NormalProduction)) {
        if (Options.getErrorReporting()) {
          cb.println(ind + "    if (!jj_rescan) { ");
        }
        cb.println(ind + "      if (LA_Scan_Token_Success == jj_laok) {");
        cb.print(ind + "        trace_la_return(\"");
        cb.print(fmtProd((NormalProduction) e.parent));
        cb.println(": look ahead SUCCESSFUL\");");
        cb.println(ind + "      } else {");
        cb.print(ind + "        trace_la_return(\"");
        cb.print(fmtProd((NormalProduction) e.parent));
        cb.println(": look ahead scan (\" + jj_la + \") SUCCEDEED\");");
        cb.println(ind + "      }");
        if (Options.getErrorReporting()) {
          cb.println(ind + "    }");
        }
        cb.print(ind + "    return LA_Phase3_Success;");
        if (DCT) {
          cb.print(" /*bp3r-5*/");
        }
        cb.println();
      } else {
        cb.print(ind + "    " + genReturn(false, 0, ind));
        if (DCT) {
          cb.print(" /*bp3r-6*/");
        }
        cb.println();
      }
      cb.println("  }");
      cb.println();
    }
  }
  
  private String genjj_3Call(final Expansion e) {
    final String name = internalName(e);
    if (name.startsWith("jj_scan_token")) {
      return "LA_Scan_Token_Failure == " + name;
    } else {
      return "LA_Phase3_Failure == jj_3" + name + "()";
    }
  }
  
  protected static final String EOL = System.getProperty("line.separator", "\n");
  
  private String genReturn(final boolean value, final int amt, final String curInd) {
    String ind = "";
    for (int i = 0; i < amt; i++) {
      ind += "  ";
    }
    final String rc = value ? "LA_Phase3_Failure" : "LA_Phase3_Success";
    if (Options.getDebugLookahead() && (jj3_expansion != null)) {
      final StringBuilder sb = new StringBuilder(160);
      if (Options.getErrorReporting()) {
        sb.append("if (!jj_rescan) { ");
      }
      sb.append("trace_la_return(\"");
      sb.append(fmtProd((NormalProduction) jj3_expansion.parent));
      sb.append(": look ahead scan (\" + jj_la + \") ");
      sb.append(value ? "FAILED" : "SUCCEDEED");
      sb.append("\");");
      if (Options.getErrorReporting()) {
        sb.append(" }");
      }
      sb.append(EOL).append(value ? "      " : "    ").append(ind);
      sb.append(curInd).append("return ").append(rc).append(";");
      return sb.toString();
    } else {
      return curInd + "return " + rc + ";";
    }
  }
  
  private String getTypeForToken() {
    return "Token";
  }
  
  private static String fmtProd(final NormalProduction p) {
    return p == null ? "?-?" : (JavaCCGlobals.addUnicodeEscapes(p.getLhs()) + "-" + p.getLine()
    //        + ":" + p.getColumn()
    );
  }
  
  Hashtable<?, ?> generated = new Hashtable<>();
  
  private int minimumSize(final Expansion e) {
    return minimumSize(e, Integer.MAX_VALUE);
  }
  
  /** Returns the minimum number of tokens that can parse to this expansion. */
  private int minimumSize(final Expansion e, final int oldMin) {
    int retval = 0; // should never be used. Will be bad if it is.
    if (e.inMinimumSize) {
      // recursive search for minimum size unnecessary.
      return Integer.MAX_VALUE;
    }
    e.inMinimumSize = true;
    
    if (e instanceof RegularExpression) {
      retval = 1;
      
    } else if (e instanceof NonTerminal) {
      final NonTerminal e_nrw = (NonTerminal) e;
      final NormalProduction ntprod = (parserData.productionTable.get(e_nrw.getName()));
      if (ntprod instanceof CodeProduction) {
        retval = Integer.MAX_VALUE;
        // Make caller think this is unending
        //  (for we do not go beyond JAVACODE during phase3 execution).
      } else {
        final Expansion ntexp = ntprod.getExpansion();
        retval = minimumSize(ntexp);
      }
      
    } else if (e instanceof Choice) {
      int min = oldMin;
      Expansion nested_e;
      final Choice e_nrw = (Choice) e;
      for (int i = 0; (min > 1) && (i < e_nrw.getChoices().size()); i++) {
        nested_e = (e_nrw.getChoices().get(i));
        final int min1 = minimumSize(nested_e, min);
        if (min > min1) {
          min = min1;
        }
      }
      retval = min;
      
    } else if (e instanceof Sequence) {
      int min = 0;
      final Sequence e_nrw = (Sequence) e;
      // We skip the first element in the following iteration since it is the Lookahead object.
      for (int i = 1; i < e_nrw.units.size(); i++) {
        final Expansion eseq = (e_nrw.units.get(i));
        final int mineseq = minimumSize(eseq);
        if ((min == Integer.MAX_VALUE) || (mineseq == Integer.MAX_VALUE)) {
          // Adding infinity to something results in infinity.
          min = Integer.MAX_VALUE;
        } else {
          min += mineseq;
          if (min > oldMin) {
            break;
          }
        }
      }
      retval = min;
      
    } else if (e instanceof TryBlock) {
      final TryBlock e_nrw = (TryBlock) e;
      retval = minimumSize(e_nrw.exp);
      
    } else if (e instanceof OneOrMore) {
      final OneOrMore e_nrw = (OneOrMore) e;
      retval = minimumSize(e_nrw.getExpansion());
      
    } else if (e instanceof ZeroOrMore) {
      retval = 0;
      
    } else if (e instanceof ZeroOrOne) {
      retval = 0;
      
    } else if (e instanceof Lookahead) {
      retval = 0;
      
    } else if (e instanceof Action) {
      retval = 0;
    }
    
    e.inMinimumSize = false;
    return retval;
  }
}

/** This class stores information to pass from phase 2 to phase 3. */
class Phase3Data {
  
  /** The expansion to generate the jj3 method for. */
  Expansion exp;
  
  /**
   * The number of tokens that can still be consumed.<br>
   * This number is used to limit the number of jj3 methods generated.
   */
  int count;
  
  Phase3Data(final Expansion e, final int c) {
    exp = e;
    count = c;
  }
}
