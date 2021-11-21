import java.util.*;

/*
 * ScriptNg implements a simple scripting language using my ExpressionParser class
 *  Note: statement nesting is implemented by indentation, but an optional "end" line can be used for clarity
 *
 *  Features:
 *    1.  Assignment expression, such as "ii = 10 + 5"
 *    2.  While loops, such as "while (ii < 10)"
 *    3.  For loops, such as "for (ii = 0; ii < 10; ii++)"
 *    3.  Conditional tests: "if", "elif", and "else"
 *    4.  Output via print("hello") and println(1 + 2) functions
 *    5.  Local function declaration, such as "function sum(a, b)" with optional "return" statement
 *    6.  Expression evaluation is handled by the ExpressionParser class
 *
 *  Author: Wayne Holder, 2019
 *  License: MIT (https://opensource.org/licenses/MIT)
 */

class ScriptNg {
  private final List<Node>                              nodes;
  private final Map<String, ExpressionParser.Function>  funcs;
  private RunCallback                                   callback;

  static class StoppedException extends IllegalStateException { }

  interface RunCallback {
    void callback (int lineNum, Map<String,Object> vars) throws StoppedException;
  }

  static class Node {
    int         lineNum;
    String      line;
    List<Node>  nodes;

    Node (int lineNum, String line) {
      this.lineNum = lineNum;
      this.line = line;
    }

    Node (List<Node> nodes) {
      this.nodes = nodes;
    }

    boolean isList () {
      return nodes != null;
    }
  }

  class Function implements ExpressionParser.Function {
    private final List<Node>    code;
    private final List<String>  args;

    Function (List<Node> code, List<String> args) {
      this.code = code;
      this.args = args;
    }

    public Object call (LinkedList<Object> stack) throws StoppedException {
      Object arg = stack.removeLast();
      Map<String,Object> vars = new HashMap<>();
      for (int ii = 0; ii < args.size(); ii++) {
        String argName = args.get(ii);
        if (ii == 0) {
          vars.put(argName, arg);
        } else {
          vars.put(argName, stack.removeLast());
        }
      }
      return eval(code, vars);
    }
  }

  /**
   * Preprocess code and build internal Node structure representing program
   * @param script a String containing indented lines of code
   * @param funcs Map of external functions
   */
  ScriptNg (String script, Map<String, ExpressionParser.Function> funcs) {
    this.funcs = funcs;
    // Preprocess code to remove comments
    script = script.replace('"', '\'');
    List<String> lines = new ArrayList<>();
    for (String line : script.split("\n")) {
      int idx = line.indexOf("//");
      if (idx >= 0) {
        line = line.substring(0, idx);
      }
      lines.add(line);
    }
    // Use indentation to process into nested List of Lists
    LinkedList<List<Node>> lStack = new LinkedList<>();
    LinkedList<Integer> iStack = new LinkedList<>();
    List<Node> items = new ArrayList<>();
    int indent = 0;
    int lineNum = 1;
    for (String line : lines) {
      if (line.length() > 0) {
        int len = 0;
        while (Character.isWhitespace(line.charAt(len))) {
          len++;
        }
        if (len == indent) {
          items.add(new Node(lineNum, line.substring(indent)));
        } else if (len > indent) {
          // Push on Stack
          List<Node> newList = new ArrayList<>();
          items.add(new Node(newList));
          lStack.add(items);
          iStack.add(indent);
          items = newList;
          indent = len;
          items.add(new Node(lineNum, line.substring(indent)));
        } else {
          // Pop from Stack
          do {
            items = lStack.removeLast();
            indent = iStack.removeLast();
          } while (indent > len);
          items.add(new Node(lineNum, line.substring(indent)));
        }
      }
      lineNum++;
    }
    nodes = items;
  }

  /**
   * Execute the code contained in the Node structure
   * @param callback reference to Object implementing RunCallback interface
   * @return result returned from code, or null
   */
  Object run (RunCallback callback) {
    this.callback = callback;
    try {
      return eval(nodes, new HashMap<>());
    } catch (Exception ex) {
      return ex;
    }
  }

  private void lineCheck (int lineNum, Map<String,Object> vars) throws StoppedException {
    if (callback != null) {
      callback.callback(lineNum, vars);
    }
    // Pause so an infinite loop in script doesn't lock up JVM
    try {
      Thread.sleep(0, 10000);
    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Recursive method used to evaluate the Node structure
   * @param nodes List of Node objects that contain the statements to avaluate
   * @param vals Map used to hold variables
   * @return result returned from code, or null
   */
  private Object eval (List<Node> nodes, Map<String,Object> vals) throws StoppedException {
    Object retVal = null;
    for (int ii = 0; ii < nodes.size(); ii++) {
      Node item = nodes.get(ii);
      lineCheck(item.lineNum, vals);
      if (item.isList()) {
        eval(item.nodes, vals);
      } else {
        String line = item.line.trim();
        if (line.startsWith("for")) {
          // Evaluate for() loop
          Node block = nodes.get(ii + 1);
          if (block.isList()) {
            line = line.substring(3).trim();
            while (line.startsWith("(") && line.endsWith(")")) {
              line = line.substring(1, line.length() - 1).trim();
            }
            String[] fParts = line.split(";");
            if (fParts.length == 3) {
              Map<String, Object> lVals = new HashMap<>(vals);
              ExpressionParser.run(fParts[0], lVals, funcs);
              while ((Boolean) ExpressionParser.run(fParts[1], lVals, funcs)) {
                eval(block.nodes, lVals);
                ExpressionParser.run(fParts[2], lVals, funcs);
              }
            } else {
              throw new IllegalArgumentException("for() missing needed subexpression");
            }
            ii++;
          }
        } else if (line.startsWith("while")) {
          // Evaluate while() loop
          Node block = nodes.get(ii + 1);
          if (block.isList()) {
            line = line.substring(5).trim();
            boolean loop;
            Map<String, Object> lVals = new HashMap<>(vals);
            do {
              Object val = ExpressionParser.run(line.trim(), lVals, funcs);
              if (val instanceof Boolean) {
                if (loop = (Boolean) val) {
                  eval(block.nodes, lVals);
                }
              } else {
                throw new IllegalArgumentException("while() expression not boolean");
              }
            } while (loop);
            ii++;
          }
        } else if (line.startsWith("if")) {
          // Evaluate if()
          boolean ifTaken = false;
          Node block = nodes.get(ii + 1);
          if (block.isList()) {
            line = line.substring(2).trim();
            Map<String, Object> lVals = new HashMap<>(vals);
            Object val = ExpressionParser.run(line.trim(), lVals, funcs);
            if (val instanceof Boolean) {
              if (ifTaken = (Boolean) val) {
                eval(block.nodes, lVals);
              }
              ii++;
            } else {
              throw new IllegalArgumentException("if() expression not boolean");
            }
          }
          block = nodes.get(ii + 1);
          while (!block.isList() && block.line.startsWith("elif")) {
            lineCheck(block.lineNum, vals);
            line = block.line.substring(4).trim();
            ii++;
            block = nodes.get(ii + 1);
            if (block.isList()) {
              if (!ifTaken) {
                Map<String, Object> lVals = new HashMap<>(vals);
                Object val = ExpressionParser.run(line.trim(), lVals, funcs);
                if (val instanceof Boolean) {
                  if ((Boolean) val) {
                    ifTaken = true;
                    eval(block.nodes, lVals);
                  }
                } else {
                  throw new IllegalArgumentException("elif() expression not boolean");
                }
              }
              ii++;
            }
          }
          block = nodes.get(ii + 1);
          if (!block.isList() && block.line.startsWith("else")) {
            lineCheck(block.lineNum, vals);
            ii++;
            block = nodes.get(ii + 1);
            if (block.isList()) {
              if (!ifTaken) {
                Map<String, Object> lVals = new HashMap<>(vals);
                eval(block.nodes, lVals);
              }
            }
            ii++;
          }
        } else if (line.startsWith("function")) {
          // Evaluate function() definition
          line = line.substring(8);
          StringTokenizer tok = new StringTokenizer(line, " \t");
          String name = tok.nextToken();
          Node block = nodes.get(ii + 1);
          if (block.isList()) {
            line = line.substring(line.indexOf(name) + name.length()).trim();
            funcs.put(name, new Function(block.nodes, ExpressionParser.getFunctionArgs(line)));
            ii++;
          } else {
            throw new IllegalArgumentException("error defining function: " + name);
          }
        } else if (line.startsWith("return")) {
          // Evaluate function() return
          line = line.substring(6);
          Map<String, Object> lVals = new HashMap<>(vals);
          retVal = ExpressionParser.run(line.trim(), lVals, funcs);
        } else if (line.startsWith("end")) {
          // Note: end is optional
        } else {
          ExpressionParser.run(line, vals, funcs);
        }
      }
    }
    if (callback != null) {
      callback.callback(0, vals);
    }
    return retVal;
  }
}
