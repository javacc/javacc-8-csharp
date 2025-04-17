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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.javacc.Version;
import org.javacc.jjtree.ASTNodeDescriptor;
import org.javacc.jjtree.JJTreeContext;
import org.javacc.jjtree.JJTreeGlobals;
import org.javacc.parser.CodeGeneratorSettings;
import org.javacc.parser.Options;
import org.javacc.utils.CodeBuilder.GenericCodeBuilder;

final class NodeFiles {

  NodeFiles() {}

  private final Set<String> nodesToBuild = new HashSet<>();

  void generateNodeType(final String nodeType) {
    if (!nodeType.equals("Tree") && !nodeType.equals("Node")) {
      nodesToBuild.add(nodeType);
    }
  }

  void generateOutputFiles(final JJTreeContext context) throws IOException {
    generateDefaultNode(context);
    generateTreeNodes(context);
    generateTreeConstants(context);
    generateVisitor(context);
    generateDefaultVisitor(context);
  }

  private static void generateDefaultNode(final JJTreeContext context) throws IOException {
    final CodeGeneratorSettings options = CodeGeneratorSettings.of(Options.getOptions());
    options.set(Options.NUO__PARSER_NAME, JJTreeGlobals.parserName);
    options.set(
        "VISITOR_RETURN_TYPE_VOID",
        Boolean.valueOf(context.treeOptions().getVisitorReturnType().equals("void")));

    try (GenericCodeBuilder gcb = GenericCodeBuilder.of(context, options)) {
      gcb.setFile(new File(context.treeOptions().getJJTreeOutputDirectory(), "Node.cs"));
      gcb.setVersion(Version.version).addTools(JJTreeGlobals.toolName);
      gcb.addOption(
          "NODE_EXTENDS",
          "NODE_FACTORY",
          "SUPPORT_CLASS_VISIBILITY_PUBLIC",
          "TRACK_TOKENS",
          "VISITOR",
          "VISITOR_DATA_TYPE",
          "VISITOR_DATA_TYPE_IS_POINTER",
          "VISITOR_EXCEPTION",
          "VISITOR_RETURN_TYPE",
          "VISITOR_RETURN_TYPE_VOID");

      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println("namespace " + Options.stringValue("NAMESPACE_OPEN"));
      }

      gcb.printTemplate("/templates/csharp/Node.template");

      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println(Options.stringValue("NAMESPACE_CLOSE"));
      }
    }
  }

  private void generateTreeNodes(final JJTreeContext context) {
    /* Options.getOptions() gets a copy of Options.resOptions, so the following non user options
     *  are not known by Options.fmtOptionsArray() when OutputFile.getPrintWriter() prints
     *  the options banner line and are output with a null value. */
    final CodeGeneratorSettings options = CodeGeneratorSettings.of(Options.getOptions());
    options.set(Options.NUO__PARSER_NAME, JJTreeGlobals.parserName);
    options.set(
        "VISITOR_RETURN_TYPE_VOID",
        Boolean.valueOf(context.treeOptions().getVisitorReturnType().equals("void")));
    options.set(
        "VISITOR_DATA_TYPE_IS_POINTER", context.treeOptions().getVisitorDataTypeIsPointer());

    try (GenericCodeBuilder gcb = GenericCodeBuilder.of(context, options)) {
      gcb.setFile(
          new File(
              context.treeOptions().getJJTreeOutputDirectory(),
              JJTreeGlobals.parserName + "Tree.cs"));
      gcb.setVersion(Version.version).addTools(JJTreeGlobals.toolName);
      gcb.addOption(
          "NODE_CLASS",
          "NODE_EXTENDS",
          "NODE_FACTORY",
          "NODE_PREFIX",
          "NODE_USES_PARSER",
          "TRACK_TOKENS",
          "VISITOR",
          "VISITOR_DATA_TYPE",
          "VISITOR_EXCEPTION",
          "VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME",
          "VISITOR_RETURN_TYPE");

      gcb.println("/* ");
      gcb.println(
          " * Option MULTI set to true produces this file containing the set of all generated node classes");
      gcb.println(" *  (those that are not user defined); it may be empty.");
      gcb.println(" */");
      gcb.println();

      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println("namespace " + Options.stringValue("NAMESPACE_OPEN"));
      }

      final File path =
          new File(
              context.treeOptions().getNodeDirectory(), context.treeOptions().getNodePackage());
      for (final String node : nodesToBuild) {
        if (!new File(path, node + ".cs").exists()) {
          options.set("NODE_TYPE", node);
          gcb.printTemplate("/templates/csharp/MultiNode.template", options);
        }
      }

      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println(Options.stringValue("NAMESPACE_CLOSE"));
      }

    } catch (final IOException e) {
      throw new Error(e.toString());
    }
  }

  private static void generateTreeConstants(final JJTreeContext context) {
    try (GenericCodeBuilder gcb = GenericCodeBuilder.of(context, CodeGeneratorSettings.create())) {
      gcb.setFile(
          new File(
              context.treeOptions().getJJTreeOutputDirectory(), NodeFiles.nodeConstants() + ".cs"));

      final List<String> nodeIds = ASTNodeDescriptor.getNodeIds();
      final List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println("namespace " + Options.stringValue("NAMESPACE_OPEN"));
      }
      gcb.println("public class " + NodeFiles.nodeConstants());
      gcb.println("{");

      for (int i = 0; i < nodeIds.size(); ++i) {
        final String n = nodeIds.get(i);
        gcb.println("  public const int " + n + " = " + i + ";");
      }

      gcb.println();
      gcb.println();

      gcb.println("  public static string[] jjtNodeName = {");
      for (final String n : nodeNames) {
        gcb.println("    \"" + n + "\",");
      }
      gcb.println("  };");

      gcb.println("}");
      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println(Options.stringValue("NAMESPACE_CLOSE"));
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private static void generateVisitor(final JJTreeContext context) {
    if (!context.treeOptions().getVisitor()) {
      return;
    }

    final List<String> nodeNames = ASTNodeDescriptor.getNodeNames();
    final String ve = NodeFiles.mergeVisitorException(context);
    String argumentType = "object";
    if (!context.treeOptions().getVisitorDataType().equals("")) {
      argumentType = context.treeOptions().getVisitorDataType();
    }

    try (GenericCodeBuilder gcb = GenericCodeBuilder.of(context, CodeGeneratorSettings.create())) {
      gcb.setFile(
          new File(
              context.treeOptions().getJJTreeOutputDirectory(), NodeFiles.visitorClass() + ".cs"));

      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println("namespace " + Options.stringValue("NAMESPACE_OPEN"));
      }

      gcb.println("public interface " + NodeFiles.visitorClass() + " {");
      gcb.println(
          "  "
              + (context.treeOptions().getVisitorDataTypeIsPointer() ? "unsafe " : "")
              + context.treeOptions().getVisitorReturnType()
              + " Visit(Node node, "
              + argumentType
              + " data)"
              + ve
              + ";");

      if (context.treeOptions().getMulti()) {
        for (final String n : nodeNames) {
          if (n.equals("void")) {
            continue;
          }
          final String nodeType = context.treeOptions().getNodePrefix() + n;
          gcb.println(
              "  "
                  + (context.treeOptions().getVisitorDataTypeIsPointer() ? "unsafe " : "")
                  + context.treeOptions().getVisitorReturnType()
                  + " "
                  + NodeFiles.getVisitMethodName(nodeType)
                  + "("
                  + nodeType
                  + " node, "
                  + argumentType
                  + " data)"
                  + ve
                  + ";");
        }
      }
      gcb.println("}");

      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println(Options.stringValue("NAMESPACE_CLOSE"));
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private static void generateDefaultVisitor(final JJTreeContext context) {
    if (!context.treeOptions().getVisitor()) {
      return;
    }

    final String ve = NodeFiles.mergeVisitorException(context);
    final String ret = context.treeOptions().getVisitorReturnType();
    String argumentType = "object";
    if (!context.treeOptions().getVisitorDataType().equals("")) {
      argumentType = context.treeOptions().getVisitorDataType();
    }
    final List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

    try (GenericCodeBuilder gcb = GenericCodeBuilder.of(context, CodeGeneratorSettings.create())) {
      gcb.setFile(
          new File(
              context.treeOptions().getJJTreeOutputDirectory(),
              NodeFiles.defaultVisitorClass() + ".cs"));

      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println("namespace " + Options.stringValue("NAMESPACE_OPEN"));
      }

      gcb.println(
          "public class "
              + NodeFiles.defaultVisitorClass()
              + " : "
              + NodeFiles.visitorClass()
              + "{");

      gcb.println(
          "  "
              + (context.treeOptions().getVisitorDataTypeIsPointer() ? "unsafe " : "")
              + "public virtual "
              + ret
              + " defaultVisit(Node node, "
              + argumentType
              + " data)"
              + ve
              + "{");
      gcb.println("    node.childrenAccept(this, data);");
      gcb.println("    return" + (ret.trim().equals("void") ? "" : " data") + ";");
      gcb.println("  }");

      gcb.println(
          "  "
              + (context.treeOptions().getVisitorDataTypeIsPointer() ? "unsafe " : "")
              + "public virtual "
              + ret
              + " Visit(Node node, "
              + argumentType
              + " data)"
              + ve
              + "{");
      gcb.println(
          "    " + (ret.trim().equals("void") ? "" : "return ") + "defaultVisit(node, data);");
      gcb.println("  }");

      if (context.treeOptions().getMulti()) {
        for (final String n : nodeNames) {
          if (n.equals("void")) {
            continue;
          }
          final String nodeType = context.treeOptions().getNodePrefix() + n;
          gcb.println(
              "  "
                  + (context.treeOptions().getVisitorDataTypeIsPointer() ? "unsafe " : "")
                  + "public virtual "
                  + ret
                  + " "
                  + NodeFiles.getVisitMethodName(nodeType)
                  + "("
                  + nodeType
                  + " node, "
                  + argumentType
                  + " data)"
                  + ve
                  + "{");
          gcb.println(
              "    " + (ret.trim().equals("void") ? "" : "return ") + "defaultVisit(node, data);");
          gcb.println("  }");
        }
      }
      gcb.println("}");

      if (Options.stringValue(Options.UO__NAMESPACE).length() > 0) {
        gcb.println(Options.stringValue("NAMESPACE_CLOSE"));
      }

    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private static String mergeVisitorException(final JJTreeContext context) {
    String ve = context.treeOptions().getVisitorException();
    if (!"".equals(ve)) {
      ve = " throws " + ve;
    }
    return ve;
  }

  private static String getVisitMethodName(final String className) {
    final StringBuffer sb = new StringBuffer("Visit");
    if (Options.booleanValue("VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME")) {
      sb.append(Character.toUpperCase(className.charAt(0)));
      for (int i = 1; i < className.length(); i++) {
        sb.append(className.charAt(i));
      }
    }

    return sb.toString();
  }

  private static String nodeConstants() {
    return JJTreeGlobals.parserName + "TreeConstants";
  }

  private static String visitorClass() {
    return JJTreeGlobals.parserName + "Visitor";
  }

  private static String defaultVisitorClass() {
    return JJTreeGlobals.parserName + "DefaultVisitor";
  }
}
