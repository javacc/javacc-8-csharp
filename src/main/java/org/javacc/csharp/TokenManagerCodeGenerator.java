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
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javacc.parser.CodeGeneratorSettings;
import org.javacc.parser.Context;
import org.javacc.parser.JavaCCGlobals;
import org.javacc.parser.Options;
import org.javacc.parser.TokenizerData;
import org.javacc.utils.CodeBuilder;
import org.javacc.utils.CodeBuilder.GenericCodeBuilder;

/** Class that implements a table driven code generator for the token manager in C#. */
class TokenManagerCodeGenerator implements org.javacc.parser.TokenManagerCodeGenerator {

  private static final String tokenManagerTemplate =
      "/templates/csharp/TokenManagerDriver.template";

  private final Context context;
  private GenericCodeBuilder gcb;

  TokenManagerCodeGenerator(final Context context) {
    this.context = context;
  }

  @Override
  public void generateCode(
      final CodeGeneratorSettings settings, final TokenizerData tokenizerData) {

    settings.putAll(Options.getOptions());

    settings.put("maxOrdinal", tokenizerData.allMatches.size());
    settings.put("maxLexStates", tokenizerData.lexStateNames.length);
    settings.put("nfaSize", tokenizerData.nfa.size());
    settings.put("charsVectorSize", ((Character.MAX_VALUE >> 6) + 1));
    settings.put("stateSetSize", tokenizerData.nfa.size());
    settings.put("parserName", tokenizerData.parserName);
    settings.put("maxLongs", (tokenizerData.allMatches.size() / 64) + 1);
    settings.put("parserName", tokenizerData.parserName);
    settings.put("charStreamName", Options.getCharStreamName());
    settings.put("defaultLexState", tokenizerData.lexStateNames[tokenizerData.defaultLexState]);
    settings.put("decls", tokenizerData.decls);
    settings.put("generatedStates", tokenizerData.nfa.size());

    final String tmSuperClass = (String) settings.get(Options.UO__TOKEN_MANAGER_SUPER_CLASS);
    settings.put(
        "tmSuperClass",
        ((tmSuperClass == null) || tmSuperClass.equals("")) ? "" : " :  " + tmSuperClass);

    settings.put("noDfa", Options.getNoDfa());

    if (Options.hasNamespace()) {
      settings.put("NAMESPACE", Options.getNamespace());
    }

    try {

      final File file =
          new File(Options.getOutputDirectory(), tokenizerData.parserName + "TokenManager.cs");
      gcb = GenericCodeBuilder.of(context, settings).setFile(file);
      if (!"".equals(Options.getNamespace())) {
        gcb.println("namespace " + Options.getNamespace() + " {\n");
      }

      generateConstantsClass(gcb, tokenizerData);

      gcb.println("/* Beginning of code from " + tokenManagerTemplate + " */");
      gcb.println();
      gcb.printTemplate(tokenManagerTemplate);
      gcb.println();
      gcb.println("/* End of code from " + tokenManagerTemplate + " */");
      gcb.println();

      gcb.println("  /* Match info. */");
      gcb.println();
      dumpMatchInfo(gcb, tokenizerData);

      if (!Options.getNoDfa()) {
        gcb.println("  /* DFA tables. */");
        gcb.println();
        dumpDfaTables(gcb, tokenizerData);
      }

      gcb.println("  /* NFA tables. */");
      gcb.println();
      dumpNfaTables(gcb, tokenizerData);

      gcb.println("  static " + tokenizerData.parserName + "TokenManager() {");
      if (!Options.getNoDfa()) {
        gcb.println("    InitStartAndSize();");
      }
      gcb.println("    initJjChars();");
      gcb.println("  }");
      gcb.println();
      gcb.println("}");

    } catch (final IOException ioe) {
      ioe.printStackTrace();
      assert (false);
    }
  }

  @Override
  public void finish(final CodeGeneratorSettings settings, final TokenizerData tokenizerData) {

    if (!"".equals(Options.getNamespace())) {
      gcb.println();
      gcb.println("}");
    }
    if (!Options.getBuildTokenManager()) {
      return;
    }

    try {
      gcb.close();
    } catch (final IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private static void dumpDfaTables(final CodeBuilder<?> cb, final TokenizerData tokenizerData) {
    final Map<Integer, int[]> startAndSize = new HashMap<>();
    int i = 0;

    /* stringLiterals. */
    cb.println("  private static readonly int[] stringLiterals = {");
    for (final int key : tokenizerData.literalSequence.keySet()) {
      final int[] arr = new int[2];
      final List<String> l = tokenizerData.literalSequence.get(key);
      final List<Integer> kinds = tokenizerData.literalKinds.get(key);
      arr[0] = i;
      arr[1] = l.size();
      int j = 0;
      if (i > 0) {
        cb.println(",");
      }
      for (final String s : l) {
        if (j > 0) {
          cb.println(", ");
        }
        cb.print("    ");
        cb.print(s.length());
        for (int k = 0; k < s.length(); k++) {
          cb.print(", ");
          cb.print((int) s.charAt(k));
          i++;
        }
        final int kind = kinds.get(j);
        cb.print(", " + kind);
        cb.print(", " + tokenizerData.kindToNfaStartState.get(kind));
        i += 3;
        j++;
      }
      startAndSize.put(key, arr);
    }
    cb.println();
    cb.println("  };");
    cb.println();

    /* startAndSize. */
    cb.println(
        "  private static readonly System.Collections.Generic.Dictionary<int, int[]> startAndSize =");
    cb.println("      new System.Collections.Generic.Dictionary<int, int[]>();");
    cb.println();

    /* InitStartAndSize. */
    cb.println("  static void InitStartAndSize() {");
    for (final int key : tokenizerData.literalSequence.keySet()) {
      final int[] arr = startAndSize.get(key);
      cb.println("    startAndSize[" + key + "] = new int[] { " + arr[0] + ", " + arr[1] + " };");
    }
    cb.println("  }");
    cb.println();
  }

  private static void dumpNfaTables(final CodeBuilder<?> cb, final TokenizerData tokenizerData) {

    /* canMatchAnyChar. */
    cb.println("  private static readonly int[] canMatchAnyChar = {");
    int v = 0;
    for (int i = 0; i < tokenizerData.wildcardKind.size(); i++) {
      if (v++ > 0) {
        cb.print(", ");
      } else {
        cb.print("    ");
      }
      cb.print(tokenizerData.wildcardKind.get(i));
    }
    cb.println();
    cb.println("  };");
    cb.println();

    /* jjInitStates. */
    cb.println("  private static readonly int[] jjInitStates  = {");
    v = 0;
    for (final int i : tokenizerData.initialStates.keySet()) {
      if (v++ > 0) {
        cb.print(", ");
      } else {
        cb.print("    ");
      }
      cb.print(tokenizerData.initialStates.get(i));
    }
    cb.println();
    cb.println("  };");
    cb.println();

    /* jjCharData. */
    cb.println("  private static readonly long[][] jjCharData = {");
    final Map<Integer, TokenizerData.NfaState> nfa = tokenizerData.nfa;
    for (int i = 0; i < nfa.size(); i++) {
      final TokenizerData.NfaState tmp = nfa.get(i);
      if (i > 0) {
        cb.println(",");
      }
      if (tmp == null) {
        cb.print("    new long[] {}");
      } else {
        cb.print("    new long[] { ");
        final BitSet bits = new BitSet();
        for (final char c : tmp.characters) {
          bits.set(c);
        }
        final long[] longs = bits.toLongArray();
        for (int k = 0; k < longs.length; k++) {
          int rep = 1;
          while (((k + rep) < longs.length) && (longs[k + rep] == longs[k])) {
            rep++;
          }
          if (k > 0) {
            cb.print(", ");
          }
          cb.print(rep + "L, ");
          // codeGenerator.genCode("0x" + Long.toHexString(longs[k]) + "L");
          cb.print("" + Long.toString(longs[k]) + "L");
          k += rep - 1;
        }
        cb.print(" }");
      }
    }
    cb.println();
    cb.println("  };");
    cb.println();

    /* jjcompositeState. */
    cb.println("  private static readonly int[][] jjcompositeState = {");
    for (int i = 0; i < nfa.size(); i++) {
      final TokenizerData.NfaState tmp = nfa.get(i);
      if (i > 0) {
        cb.println(", ");
      }
      if (tmp == null) {
        cb.print("    new int[] {}");
        continue;
      }
      cb.print("    new int[] { ");
      int k = 0;
      for (final int st : tmp.compositeStates) {
        if (k++ > 0) {
          cb.print(", ");
        }
        cb.print(st);
      }
      cb.print(" }");
    }
    cb.println();
    cb.println("  };");
    cb.println();

    /* jjmatchKinds. */
    cb.println("  private static readonly int[] jjmatchKinds = {");
    for (int i = 0; i < nfa.size(); i++) {
      final TokenizerData.NfaState tmp = nfa.get(i);
      if (i > 0) {
        cb.println(",");
      }
      cb.print("    ");
      // TODO(sreeni) : Fix this mess.
      cb.print(tmp == null ? Integer.MAX_VALUE : tmp.kind);
    }
    cb.println();
    cb.println("  };");
    cb.println();

    /* jjnextStateSet. */
    cb.println("  private static readonly int[][]  jjnextStateSet = {");
    for (int i = 0; i < nfa.size(); i++) {
      final TokenizerData.NfaState tmp = nfa.get(i);
      if (i > 0) {
        cb.println(",");
      }
      if (tmp == null) {
        cb.print("    new int[] {}");
        continue;
      }
      int k = 0;
      cb.print("    new int[] { ");
      for (final int s : tmp.nextStates) {
        if (k++ > 0) {
          cb.print(", ");
        }
        cb.print(s);
      }
      cb.print(" }");
    }
    cb.println();
    cb.println("  };");
    cb.println();
  }

  private static void dumpMatchInfo(final CodeBuilder<?> cb, final TokenizerData tokenizerData) {
    final Map<Integer, TokenizerData.MatchInfo> allMatches = tokenizerData.allMatches;

    // A bit ugly.

    final BitSet toSkip = new BitSet(allMatches.size());
    final BitSet toSpecial = new BitSet(allMatches.size());
    final BitSet toMore = new BitSet(allMatches.size());
    final BitSet toToken = new BitSet(allMatches.size());
    final int[] newStates = new int[allMatches.size()];
    toSkip.set(allMatches.size() + 1, true);
    toToken.set(allMatches.size() + 1, true);
    toMore.set(allMatches.size() + 1, true);
    toSpecial.set(allMatches.size() + 1, true);

    /* jjstrLiteralImages. */
    cb.println("  public static readonly string[] jjstrLiteralImages = {");

    int k = 0;
    for (int i = 0; i < allMatches.size(); i++) {
      final TokenizerData.MatchInfo matchInfo = allMatches.get(i);
      switch (matchInfo.matchType) {
        case SKIP:
          toSkip.set(i);
          break;
        case SPECIAL_TOKEN:
          toSpecial.set(i);
          break;
        case MORE:
          toMore.set(i);
          break;
        case TOKEN:
          toToken.set(i);
          break;
      }
      newStates[i] = matchInfo.newLexState;
      final String image = matchInfo.image;
      if (k++ > 0) {
        cb.println(",");
      }
      if (image != null) {
        cb.print("    \"");
        for (int j = 0; j < image.length(); j++) {
          final int cj = image.charAt(j);
          switch (cj) {
            case '\b':
              cb.print("\\b");
              continue;
            case '\t':
              cb.print("\\t");
              continue;
            case '\n':
              cb.print("\\n");
              continue;
            case '\f':
              cb.print("\\f");
              continue;
            case '\r':
              cb.print("\\r");
              continue;
            case '\"':
              cb.print("\\\"");
              continue;
            case '\'':
              cb.print("\\\'");
              continue;
            case '\\':
              cb.print("\\\\");
              continue;
            default:
              if (cj <= 0xff) {
                if (cj < 0x20 || (cj > 0x7e)) {
                  cb.print("0x" + Integer.toHexString(cj));
                } else {
                  cb.print(image.charAt(j));
                }
              } else {
                String hexVal = Integer.toHexString(image.charAt(j));
                if (hexVal.length() == 3) {
                  hexVal = "0" + hexVal;
                }
                cb.print("\\u" + hexVal);
              }
              continue;
          }
        }
        cb.print("\"");
      } else {
        cb.print("    null");
      }
    }
    cb.println();
    cb.println("  };");
    cb.println();

    /* Bit masks. */
    generateBitVector(cb, "jjtoToken", toToken);
    cb.println();
    generateBitVector(cb, "jjtoSkip", toSkip);
    cb.println();
    generateBitVector(cb, "jjtoSpecial", toSpecial);
    cb.println();
    generateBitVector(cb, "jjtoMore", toMore);
    cb.println();

    /* jjnewLexState. */
    cb.println("  private static readonly int[] jjnewLexState = {");
    for (int i = 0; i < newStates.length; i++) {
      if (i > 0) {
        cb.print(", ");
      } else {
        cb.print("    ");
      }
      // codeGenerator.genCode("0x" + Integer.toHexString(newStates[i]));
      cb.print(Integer.toString(newStates[i]));
    }
    cb.println();
    cb.println("  };");
    cb.println();

    // Action functions.

    // Token actions.
    cb.println("  void TokenLexicalActions(Token matchedToken) {");
    cb.println("  // TOKEN lexical actions");
    dumpLexicalActions(cb, allMatches, TokenizerData.MatchType.TOKEN, "matchedToken.kind");
    cb.println("  }");
    cb.println();

    // Skip actions.
    // TODO(sreeni) : Streamline this mess.
    cb.println("  void SkipLexicalActions(Token matchedToken) {");
    cb.println("  // SKIP lexical actions");
    dumpLexicalActions(cb, allMatches, TokenizerData.MatchType.SKIP, "jjmatchedKind");
    cb.println("  // SPECIAL_TOKEN lexical actions");
    dumpLexicalActions(cb, allMatches, TokenizerData.MatchType.SPECIAL_TOKEN, "jjmatchedKind");
    cb.println("  }");
    cb.println();

    // More actions.
    cb.println("  void MoreLexicalActions() {");
    cb.println("    jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    cb.println("  // MORE lexical actions");
    dumpLexicalActions(cb, allMatches, TokenizerData.MatchType.MORE, "jjmatchedKind");
    cb.println("  }");
    cb.println();
  }

  private static void dumpLexicalActions(
      final CodeBuilder<?> cb,
      final Map<Integer, TokenizerData.MatchInfo> allMatches,
      final TokenizerData.MatchType matchType,
      final String kindString) {

    cb.println("    // " + matchType.name() + " lexical actions");
    cb.println("    switch(" + kindString + ") {");
    for (final int i : allMatches.keySet()) {
      final TokenizerData.MatchInfo matchInfo = allMatches.get(i);
      if ((matchInfo.action == null) || (matchInfo.matchType != matchType)) {
        continue;
      }
      cb.println("      case " + i + ": {");
      cb.println("        " + matchInfo.action.trim());
      cb.println("        break;");
      cb.println("      }");
    }
    cb.println("      default: break;");
    cb.println("    }");
  }

  private static void generateBitVector(
      final CodeBuilder<?> cb, final String name, final BitSet bits) {
    cb.println("  private static readonly long[] " + name + " = {");
    final long[] longs = bits.toLongArray();
    for (int i = 0; i < longs.length; i++) {
      if (i > 0) {
        cb.print(", ");
      }
      // codeGenerator.genCode("0x" + Long.toHexString(longs[i]) + "L");
      cb.print("    " + Long.toString(longs[i]) + "L");
    }
    cb.println();
    cb.println("  };");
  }

  private static void generateConstantsClass(
      final CodeBuilder<?> cb, final TokenizerData tokenizerData) {

    cb.println("public class " + tokenizerData.parserName + "Constants {");
    cb.println();

    cb.println("  /** Token kind 0. */");
    cb.println("  public const int EOF = 0;");
    for (final Integer i : tokenizerData.labels.keySet()) {
      cb.println("  /** Labeled token " + i + " kind. */");
      cb.println("  public const int " + tokenizerData.labels.get(i) + " = " + i + ";");
    }
    cb.println();

    cb.println("  /**");
    cb.println("   * Tokens labels (if any) or images (if string literal) or named kinds");
    cb.println("   * (for non labeled non string literals).");
    cb.println("   */");
    cb.println("  public static string[] tokenImage = {");
    for (int i = 0; i < tokenizerData.images.length; i++) {
      String lbl;
      if (i > 0) {
        cb.println(",");
      }
      if (tokenizerData.images[i] == null) {
        cb.print("    @\"<EOF>\"");
      } else if ((lbl = tokenizerData.labels.get(i)) != null) {
        //        cb.print("    @\"<" + JavaCCGlobals.add_escapes(lbl) + ">\"");
        cb.print("    @\"<" + lbl + ">\"");
      } else {
        cb.print("    @\"" + JavaCCGlobals.add_escapes(tokenizerData.images[i]) + "\"");
      }
    }
    cb.println();
    cb.println("  };");
    cb.println();

    //    cb.println("  /** Literal token labels (for display purposes). */");
    //    cb.println("  public static string[] tokenLabel = {");
    //    for (int i = 0; i < tokenizerData.images.length; i++) {
    //      String lbl;
    //      if (i > 0) {
    //        cb.println(",");
    //      }
    //      if (tokenizerData.images[i] == null) {
    //        cb.print("    @\"<EOF>\"");
    //      } else if ((lbl = tokenizerData.labels.get(i)) != null) {
    //        //        cb.print("    @\"<" + JavaCCGlobals.add_escapes(lbl) + ">\"");
    //        cb.print("    @\"<" + lbl + ">\"");
    //      } else {
    //        cb.print("    @\"\"\"" + JavaCCGlobals.add_escapes(tokenizerData.images[i]) +
    // "\"\"\"");
    //      }
    //    }
    //    cb.println();
    //    cb.println("  };");
    //    cb.println();

    for (int i = 0; i < tokenizerData.lexStateNames.length; i++) {
      cb.println("  /** Lexical state " + i + ". */");
      cb.println("  public const int " + tokenizerData.lexStateNames[i] + " = " + i + ";");
    }
    cb.println();

    cb.println("  /** Lexical state names. */");
    cb.println("  public static string[] lexStateNames = {");
    for (int i = 0; i < tokenizerData.lexStateNames.length; i++) {
      if (i > 0) {
        cb.println(",");
      }
      cb.print("    \"" + tokenizerData.lexStateNames[i] + "\"");
    }
    cb.println();
    cb.println("  };");
    cb.println();

    cb.println("};");
    cb.println();
  }
}
