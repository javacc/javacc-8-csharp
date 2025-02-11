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
 *     * Neither the names of of the copyright holders nor the names of its
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
import org.javacc.jjtree.JJTreeContext;
import org.javacc.parser.CodeGeneratorSettings;
import org.javacc.parser.Context;
import org.javacc.parser.JavaCCGlobals;
import org.javacc.parser.TokenizerData;
import org.javacc.utils.CodeBuilder.GenericCodeBuilder;

public class CodeGenerator implements org.javacc.parser.CodeGenerator {

  /** The name of the C# code generator. */
  @Override
  public String getName() {
    return "C#";
  }

  /** Generate any other support files you need. */
  @Override
  public boolean generateHelpers(
      final Context context,
      final CodeGeneratorSettings settings,
      final TokenizerData tokenizerData) {
    final File directory = new File((String) settings.get("OUTPUT_DIRECTORY"));
    try {
      try (GenericCodeBuilder gcb = GenericCodeBuilder.of(context, settings)) {
        gcb.setFile(new File(directory, "CharStream.cs"));
        gcb
            .addTools(JavaCCGlobals.toolName)
            .printTemplate("/templates/csharp/CharStream.template");
      }

      try (GenericCodeBuilder gcb = GenericCodeBuilder.of(context, settings)) {
        gcb.setFile(new File(directory, "TokenMgrError.cs"));
        gcb
            .addTools(JavaCCGlobals.toolName)
            .printTemplate("/templates/csharp/TokenMgrError.template");
      }

      try (GenericCodeBuilder gcb = GenericCodeBuilder.of(context, settings)) {
        gcb.setFile(new File(directory, "ParseException.cs"));
        gcb
            .addTools(JavaCCGlobals.toolName)
            .printTemplate("/templates/csharp/ParseException.template");
      }

      try (GenericCodeBuilder gcb = GenericCodeBuilder.of(context, settings)) {
        gcb.addTools(JavaCCGlobals.toolName);

        if ((Boolean) settings.get("JAVA_UNICODE_ESCAPE")) {
          gcb.setFile(new File(directory, "JavaCharStream.cs"));
          gcb.printTemplate("/templates/csharp/JavaCharStream.template");
        } else {
          gcb.setFile(new File(directory, "CharStream.cs"));
          gcb.printTemplate("/templates/csharp/CharStream.template");
        }
      }
    } catch (final Exception e) {
      return false;
    }

    return true;
  }

  /** The Token class generator. */
  @Override
  public TokenCodeGenerator getTokenCodeGenerator(final Context context) {
    return new TokenCodeGenerator(context);
  }

  /** The TokenManager class generator. */
  @Override
  public TokenManagerCodeGenerator getTokenManagerCodeGenerator(final Context context) {
    return new TokenManagerCodeGenerator(context);
  }

  /** The Parser class generator. */
  @Override
  public ParserCodeGenerator getParserCodeGenerator(final Context context) {
    return new ParserCodeGenerator(context);
  }

  /**
   * TODO(sreeni): Fix this when we do tree annotations in the parser code generator. The JJTree
   * preprocesor.
   */
  @Override
  public org.javacc.jjtree.DefaultJJTreeVisitor getJJTreeCodeGenerator(
      final JJTreeContext context) {
    return new JJTreeCodeGenerator(context);
  }
}
