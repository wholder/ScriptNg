import java.io.*;
import java.util.*;
import java.math.*;

/**
 *  Converts infix expression to postfix expression using the "shunting yard" algorithm:
 *    1.  Initialize an empty stack and empty output List.
 *    2.  Read the infix expression from left to right, one token at a time.
 *    3.  If the token is an operand, append it to the output List.
 *    4.  If the token is an operator, pop operators until you reach an opening
 *        parenthesis, an operator of lower precedence, or a right associative symbol
 *        of equal precedence. Push the operator onto the stack.
 *    5.  If the token is an opening parenthesis, push it onto the stack.
 *    6.  If the token is a closing parenthesis, pop all operators until you reach
 *        an opening parenthesis and append them to the output List.
 *    7.  If the end of the input is found, pop all operators and append them
 *        to the output List.
 *
 *  Note: additional techniques are used to handle the "&&" and "||" shortcut operators and the urnary "++"
 *  and "--" auto increment and decrement operators.
 *
 *  This update version now supports the following new features:
 *    1.  Auto type assignment, such as "a = 10" for Integer, or "a = 2.5" for Decimal values
 *    2.  NumberVal class handles mixed Decimal/Integer Math with auto conversion from Integer to Decimal, when needed
 *    3.  NumberVal uses MathContext.DECIMAL128 to limit precision, when needed
 *    4.  New trunc(v,n) function to set value v to n fractional digits (if n = 0, result is Integer, else Decimal)
 *    5.  New pow(v,p) raises v to n power (result is Decimal if v is Decimal, else Integer)
 *    6.  New assignment operators: =, +=, -=, *=, /= and %= (assign to variable and place in return stack)
 *    7.  Comparison operators work with with String args, such as "==", "!=", "<", ">", "<=" and ">="
 *    8.  New Function interface supports calling external functions
 *    9.  Pre and Post Increment and Decrement (++/--) operators
 *    10. New Bit-related functions, bit(v,b), set(v,b), clr(v,b) and flip(v,b)
 *    11. New millis() function that returns System.currentTimeMillis()
 *    12. String concatenation works with numbers, such as "a = 10 + ' ms'" as well as "+=" operator
 *    13. One dimensional arrays, such as "a[0] = 2" or "b[2] = 'test'"  are now supported
 *    14. Strings can be delimited by (') or ("), such as 'abc' or "abc"
 *
 *  Note: the main() method in this class implements a self test function which you can use to verify
 *  continued proper operation in the event you decide to alter the code.  This code is rather "ad hoc"
 *  and may not test every case needed for a full test.
 *
 *  Author: Wayne Holder, 2004-2019
 *  License: MIT (https://opensource.org/licenses/MIT)
 */

public class ExpressionParser {
  private static final Null             NULL = new Null();
  private static Map<String,Integer>    ops = new HashMap<>();
  private static Map<String,Function>   functions = new HashMap<>();
  private static Map<String,Token>      opr1 =  new HashMap<>();
  private static Map<String,Token>      opr2 =  new HashMap<>();
  private static Map<String,Token>      opr3 =  new HashMap<>();

  static {
    // Operator precedence (1 is lowest, 10 is highest)
    ops.put("=",  0);       // Assignment
    ops.put("+=",  0);      // Assignment Addition
    ops.put("-=",  0);      // Assignment Subtraction
    ops.put("*=",  0);      // Assignment Multiplication
    ops.put("/=",  0);      // Assignment Division
    ops.put("%=",  0);      // Assignment Modulo

    ops.put("|",  1);       // OR
    ops.put("||", 1);       // shortcut OR
    ops.put("&",  1);       // AND
    ops.put("&&", 1);       // shortcut AND
    ops.put("^",  1);       // XOR

    ops.put("S&", 2);       // shortcut OR operators (used internally)
    ops.put("S|", 2);       // shortcut AND operators (used internally)

    ops.put("==", 3);       // Equals
    ops.put("!=", 3);       // Not Equals

    ops.put("<",  4);       // Less than
    ops.put("<=", 4);       // Less than, or equal to
    ops.put(">",  4);       // Greater than
    ops.put(">=", 4);       // Greater than, or equal to

    ops.put("<<", 5);       // Signed Left shift
    ops.put(">>", 5);       // Signed Right shift
    ops.put(">>>", 5);      // Unsigned Right shift

    ops.put("-",  6);       // Subtract, or minus sign
    ops.put("+",  6);       // Subtract, or positive sign, or String concatenation

    ops.put("/",  7);       // Divide
    ops.put("*",  7);       // Multiply
    ops.put("%",  7);       // Modulo

    ops.put("!",  8);       // Unary Negation
    ops.put("++",  8);      // Unary Increment
    ops.put("--",  8);      // Unary Decrement

    ops.put("(",  9);       // Left, opening parenthesis
    ops.put(")",  9);       // Right, closing parenthesis
    ops.put("[",  9);       // Left, opening bracket
    ops.put("]",  9);       // Right, closing bracket

    ops.put("()", 10);      // Function call
    ops.put("[]", 10);      // Array reference
    //
    // Define Operators
    //
    opr1.put("=", new Token("=", Token.Type.OP));
    opr1.put("+", new Token("+", Token.Type.OP));
    opr1.put("-", new Token("-", Token.Type.OP));
    opr1.put("*", new Token("*", Token.Type.OP));
    opr1.put("/", new Token("/", Token.Type.OP));
    opr1.put("%", new Token("%", Token.Type.OP));
    opr1.put(",", new Token(",", Token.Type.CMA));
    opr2.put("+=", new Token("+=", Token.Type.OP));
    opr2.put("-=", new Token("-=", Token.Type.OP));
    opr2.put("*=", new Token("*=", Token.Type.OP));
    opr2.put("/=", new Token("/=", Token.Type.OP));
    opr2.put("%=", new Token("%=", Token.Type.OP));
    opr1.put("<", new Token("<", Token.Type.OP));
    opr2.put("<=", new Token("<=", Token.Type.OP));
    opr1.put(">", new Token(">", Token.Type.OP));
    opr2.put(">=", new Token(">=", Token.Type.OP));
    opr2.put("==", new Token("==", Token.Type.OP));
    opr2.put("!=", new Token("!=", Token.Type.OP));
    opr2.put("<<", new Token("<<", Token.Type.OP));
    opr2.put(">>", new Token(">>", Token.Type.OP));
    opr2.put("++", new Token("++", Token.Type.OP));
    opr2.put("--", new Token("--", Token.Type.OP));
    opr3.put(">>>", new Token(">>>", Token.Type.OP));
    opr1.put("&", new Token("&", Token.Type.OP));
    opr1.put("|", new Token("|", Token.Type.OP));
    opr1.put("!", new Token("!", Token.Type.OP));
    opr1.put("^", new Token("^", Token.Type.OP));
    opr1.put("(", new Token("(", Token.Type.OP));
    opr1.put(")", new Token(")", Token.Type.OP));
    opr1.put("[", new Token("[", Token.Type.OP));
    opr1.put("]", new Token("]", Token.Type.OP));
    //
    // Define Built-in Functions
    //
    functions.put("max", (stack) -> {
      NumberVal rArg = (NumberVal) stack.removeLast();                // rightmost arg
      NumberVal lArg = (NumberVal) stack.removeLast();                // leftmost arg
      return lArg.max(rArg);
    });
    functions.put("min", (stack) -> {
      NumberVal rArg = (NumberVal) stack.removeLast();                // rightmost arg
      NumberVal lArg = (NumberVal) stack.removeLast();                // leftmost arg
      return lArg.min(rArg);
    });
    functions.put("abs", (stack) -> {
      NumberVal arg = (NumberVal) stack.removeLast();                 // Argument
      return arg.abs();
    });
    functions.put("pow", (stack) -> {
      NumberVal rArg = (NumberVal) stack.removeLast();                // rightmost arg
      NumberVal lArg = (NumberVal) stack.removeLast();                // leftmost arg
      return lArg.pow(rArg);
    });
    functions.put("trunc", (stack) -> {
      Number rArg = ((NumberVal) stack.removeLast()).getValue();      // rightmost arg
      Number lArg = ((NumberVal) stack.removeLast()).getValue();      // leftmost arg
      if (rArg instanceof BigInteger && lArg instanceof BigDecimal) {
        int places = (rArg).intValue();
        if (places == 0) {
          return new NumberVal(BigInteger.valueOf((lArg).longValue()));
        } else if (places > 0) {
          return new NumberVal(((BigDecimal) lArg).setScale(places, BigDecimal.ROUND_HALF_UP));
        }
      }
      throw new ExpressionParserError("incorrect arg types for trunc(" + lArg + ", " + rArg + ')');
    });
    functions.put("radix", (stack) -> {
      Number rArg = ((NumberVal) stack.removeLast()).getValue();      // rightmost arg
      Number lArg = ((NumberVal) stack.removeLast()).getValue();      // leftmost arg
      if (rArg instanceof BigInteger && lArg instanceof BigInteger) {
        int radix = (rArg).intValue();
        return ((BigInteger) lArg).toString(radix).toUpperCase();
      } else {
        throw new ExpressionParserError("incorrect arg types for radix(" + lArg + ", " + rArg + ')');
      }
    });
    functions.put("bit", (stack) -> {
      Number rArg = ((NumberVal) stack.removeLast()).getValue();      // rightmost arg
      Number lArg = ((NumberVal) stack.removeLast()).getValue();      // leftmost arg
      if (rArg instanceof BigInteger && lArg instanceof BigInteger) {
        int bit = (rArg).intValue();
        return ((BigInteger) lArg).testBit(bit);
      } else {
        throw new ExpressionParserError("incorrect arg types for bit(" + lArg + ", " + rArg + ')');
      }
    });
    functions.put("set", (stack) -> {
      Number rArg = ((NumberVal) stack.removeLast()).getValue();      // rightmost arg
      Number lArg = ((NumberVal) stack.removeLast()).getValue();      // leftmost arg
      if (rArg instanceof BigInteger && lArg instanceof BigInteger) {
        int bit = (rArg).intValue();
        return new NumberVal(((BigInteger) lArg).setBit(bit));
      } else {
        throw new ExpressionParserError("incorrect arg types for set(" + lArg + ", " + rArg + ')');
      }
    });
    functions.put("clr", (stack) -> {
      Number rArg = ((NumberVal) stack.removeLast()).getValue();      // rightmost arg
      Number lArg = ((NumberVal) stack.removeLast()).getValue();      // leftmost arg
      if (rArg instanceof BigInteger && lArg instanceof BigInteger) {
        int bit = (rArg).intValue();
        return new NumberVal(((BigInteger) lArg).clearBit(bit));
      } else {
        throw new ExpressionParserError("incorrect arg types for clr(" + lArg + ", " + rArg + ')');
      }
    });
    functions.put("flip", (stack) -> {
      Number rArg = ((NumberVal) stack.removeLast()).getValue();      // rightmost arg
      Number lArg = ((NumberVal) stack.removeLast()).getValue();      // leftmost arg
      if (rArg instanceof BigInteger && lArg instanceof BigInteger) {
        int bit = (rArg).intValue();
        return new NumberVal(((BigInteger) lArg).flipBit(bit));
      } else {
        throw new ExpressionParserError("incorrect arg types for flip(" + lArg + ", " + rArg + ')');
      }
    });
    functions.put("millis", (stack) -> new NumberVal(System.currentTimeMillis()));
  }

  static class ExpressionParserError extends IllegalStateException {
    ExpressionParserError (String message) {
      super(message);
    }
  }

  public static class Token {
    enum Type {
      VAR, VAL, STR, OP, FNC, ARY, CMA, EXP
    }
    String              prePostOp;
    Type                type;
    private String      val;
    private int         shortcutId = -1;
    private int         prec;

    private Token (String val, Type type) {
      this.val = val;
      this.type = type;
      if (type == Type.OP && ops.containsKey(val)) {
        prec = ops.get(val);
      } else if (type == Type.FNC) {
        prec = ops.get("()");
      } else if (type == Type.ARY) {
        prec = ops.get("[]");
      }
    }

    private Token (String val, Type type, int shortcutId) {
      this(val, type);
      this.shortcutId = shortcutId;
    }

    private Object getValue (Map<String, Object> vals) {
      switch (val) {
        case "true":
          return Boolean.TRUE;
        case "false":
          return Boolean.FALSE;
        case "null":
          return NULL;
        default:
          Object ret = vals.get(val);
          if (ret instanceof NumberVal) {
            if (prePostOp != null) {
              switch (prePostOp) {
                case "++v":
                  vals.put(val, ret = ((NumberVal) ret).add(NumberVal.ONE));
                  break;
                case "--v":
                  vals.put(val, ret = ((NumberVal) ret).subtract(NumberVal.ONE));
                  break;
                case "v++":
                  vals.put(val, ((NumberVal) ret).add(NumberVal.ONE));
                  break;
                case "v--":
                  vals.put(val, ((NumberVal) ret).subtract(NumberVal.ONE));
                  break;
                default:
                  throw new ExpressionParserError("invalid pre/post operator " + prePostOp);
              }
              prePostOp = null;
            }
            return ret;
          } else if (ret == null) {
            return NULL;
          } else {
            return ret;
          }
      }
    }

    private void setPrePostIncDec (String op) {
      prePostOp = op;
    }

    public String toString () {
      return shortcutId >= 0 ? val + ":" + shortcutId : val;
    }
  }

  private static class ArrayRef {
    private Map<Integer,Object>   array;
    private int                   index;
    private String                prePostOp;

    ArrayRef (Map<String,Object> vals, Token tok, int index) {
      array = (Map<Integer,Object>) vals.get(tok.val);
      prePostOp = tok.prePostOp;
      if (array == null) {
        array = new HashMap<>();
        vals.put(tok.val, array);
      }
      this.index = index;
    }

    Object getValue () {
      Object ret = array.get(index);
      if (ret instanceof NumberVal) {
        if (prePostOp != null) {
          switch (prePostOp) {
            case "++v":
              array.put(index, ret = ((NumberVal) ret).add(NumberVal.ONE));
              break;
            case "--v":
              array.put(index, ret = ((NumberVal) ret).subtract(NumberVal.ONE));
              break;
            case "v++":
              array.put(index, ((NumberVal) ret).add(NumberVal.ONE));
              break;
            case "v--":
              array.put(index, ((NumberVal) ret).subtract(NumberVal.ONE));
              break;
          }
          prePostOp = null;
        }
        return ret;
      } else if (ret == null) {
        return NULL;
      } else {
        return ret;
      }
    }

    void setValue (Object value) {
      array.put(index, value);
    }
  }

  private static class Null {
    @Override
    public boolean equals(Object obj) {
      return obj == null  ||  obj instanceof Null;
    }
  }

  interface Function {
    Object call (LinkedList<Object> stack);
  }

  /*
   * Define Global Functions
   */

  // Example external function
  static class Reverse implements Function {
    public Object call (LinkedList<Object> stack) {
      Object arg = stack.size() > 0 ? stack.removeLast() : null;
      if (arg instanceof String) {
        String str = (String) arg;
        StringBuilder buf = new StringBuilder();
        for (int ii = str.length() - 1; ii >= 0; ii--) {
          buf.append(str.charAt(ii));
        }
        return buf.toString();
      } else {
        throw new ExpressionParserError("arg not String reverse(" + arg + ')');
      }
    }
  }

  /**
   * Parse and execute infix expression
   * @param expr infix expression
   * @param vals Map of externally-defined variables with values
   * @param eFuncs Map of externally-defined functions
   * @return result of evaluation, or null
   */
  static Object run (String expr, Map<String,Object> vals, Map<String,Function> eFuncs) {
    Map<String,Function> funcs = new HashMap<>(functions);
    if (eFuncs != null) {
      funcs.putAll(eFuncs);
    }
    return eval(parse(expr), vals, funcs);
  }

  /**
   * Parse and extract all variables in src
   * @param src String containing variable names
   * @return List of variable names
   */
  static List<String> getFunctionArgs (String src) {
    ExpressionParser.Token[] args = ExpressionParser.parse(src);
    List<String> argNames = new ArrayList<>();
    for (int ii = 1; ii < args.length; ii++) {
      ExpressionParser.Token arg = args[ii];
      if (arg.type == ExpressionParser.Token.Type.VAR) {
        argNames.add(arg.val);
      } else {
        throw new ExpressionParserError("error parsing function arguments in : " + src);
      }
    }
    return argNames;
  }

  /**
   * Tokenize String into a Token[] array
   * @param in expression to tokenize
   * @return Token[] array in postfix form
   */
  private static Token[] tokenize (String in) {
    int id = 0;
    in = condenseWhitespace(in) + '\t';    // Trailing space is kluge to for eval of trailing Number or Variable Name
    List<Token> out = new ArrayList<>();
    out.add(new Token(in, Token.Type.EXP));    // Save expression for stack trace display
    int len = in.length();
    int state = 0;
    StringBuilder acc = new StringBuilder();
    for (int ii = 0; ii < len; ii++) {
      char c1 = in.charAt(ii);
      Token tok;
      switch (state) {
        case 0: // waiting
          char c2 = ii < len - 1 ? in.charAt(ii + 1) : ' ';
          if (Character.isDigit(c1) || c1 == '.' || (c1 == '-' || c1 == '+') && Character.isDigit(c2)) {
            acc.append(c1);
            state = 2;                                        // Number
          } else if (Character.isLetter(c1) || c1 == '_') {
            acc.append(c1);
            state = 1;                                        // Variable name
          } else if (c1 == '\'') {
            state = 3;                                        // String
          } else if (c1 == '&' && c2 == '&') {
            out.add(new Token("S&", Token.Type.OP, id));      // Shortcut skip check
            out.add(new Token("&&", Token.Type.OP, id));      // Operator, if not skipped
            id++;
            ii++;
          } else if (c1 == '|' && c2 == '|') {
            out.add(new Token("S|", Token.Type.OP, id));      // Shortcut skip check
            out.add(new Token("||", Token.Type.OP, id));      // Operator, if not skipped
            id++;
            ii++;
          } else if (ii < len - 2 && (tok = opr3.get(Character.toString(c1 )+ c2 + in.charAt(ii + 2))) != null) {
            out.add(tok);
            ii += 2;
          } else if (ii < len - 1 && (tok = opr2.get(Character.toString(c1 )+ c2)) != null) {
            out.add(tok);
            ii++;
          } else if ((tok = opr1.get(Character.toString(c1))) != null) {
            out.add(tok);
          }
          break;
        case 1: // variable
          if (Character.isLetter(c1)  ||  Character.isDigit(c1)  ||  c1 == '.'  ||  c1 == '_'  ||  c1 == ':') {
            acc.append(c1);
          } else if (c1 != ' ') {
            String name = acc.toString();
            if (c1 == '(') {
              out.add(new Token(name, Token.Type.FNC));
            } else if (c1 == '[') {
              out.add(new Token(name, Token.Type.ARY));
            } else {
              out.add(new Token(name, Token.Type.VAR));
            }
            acc = new StringBuilder();
            state = 0;
            ii--;
          }
          break;
        case 2: // number
          if (Character.isDigit(c1) || c1 == '.' ||  (acc.length() == 1 && c1 == 'x')  ||
              (acc.length() >= 2 && acc.charAt(1) == 'x' && isHex(c1))) {
            acc.append(c1);
          } else {
            String val = acc.toString().trim();
            // Handle unary +/- operators
            if ((val.startsWith("+") || val.startsWith("-")) && !out.isEmpty()) {
              Token top = out.get(out.size() - 1);
              if (top.type == Token.Type.VAL || top.type == Token.Type.VAR) {
                out.add(new Token(val.substring(0, 1), Token.Type.OP));
                out.add(new Token(val.substring(1), Token.Type.VAL));
              } else {
                out.add(new Token(val, Token.Type.VAL));
              }
            } else {
              out.add(new Token(val, Token.Type.VAL));
            }
            acc = new StringBuilder();
            state = 0;
            ii--;
          }
          break;
        case 3: // String
          if (c1 == '\'') {
            out.add(new Token(acc.toString(), Token.Type.STR));
            acc = new StringBuilder();
            state = 0;
          } else {
            acc.append(c1);
          }
          break;
      }
    }
    // Scan for "++" and "--" prefix operators and merge into VAR or ARY tokens
    List<Token> out2 = new ArrayList<>();
    for (int ii = 0; ii < out.size(); ii++) {
      Token tok = out.get(ii);
      if (tok.type == Token.Type.OP && (tok.val.equals("++") || tok.val.equals("--"))) {
        if (ii + 1 < out.size()) {
          Token next = out.get(ii + 1);
          if (next.type == Token.Type.VAR || next.type == Token.Type.ARY) {
            next.setPrePostIncDec(tok.val + "v");
            continue;
          }
        }
      }
      out2.add(tok);
    }
    return out2.toArray(new Token[0]);
  }

  /**
   * Parse infix into List in postfix order. Note: Strings like "TEST" are treated as
   * variable names, and ones with surrounding ' marks, such as "'TEST'" are treated as
   * string literals.
   * @param in infix expression
   * @return Token[] array postfix expression
   */
  private static Token[] parse (String in) {
    try {
      Token[] expr = tokenize(in);
      List<Token> out = new ArrayList<>();
      LinkedList<Token> tokStack = new LinkedList<>();
      int parenCount = 0;
      int brackCount = 0;
      for (Token tok : expr) {
        Token top;
        switch (tok.type) {
          case EXP:
          case VAR:
          case VAL:
          case STR:
            out.add(tok);
            break;
          case OP:
          case ARY:
          case FNC:
            switch (tok.val) {
              case "(":
                parenCount++;
                tokStack.add(tok);
                break;
              case ")":
                parenCount--;
                while (tokStack.size() > 0 && !(top = tokStack.getLast()).val.equals("(")) {
                  tokStack.removeLast();
                  out.add(top);                     // Copy tokens from stack to output (stop on finding "(")
                }
                if (tokStack.size() > 0) {
                  tokStack.removeLast();            // Discard opening "("
                  if (!tokStack.isEmpty()) {
                    top = tokStack.getLast();
                    if (top.type == Token.Type.FNC) {
                      tokStack.removeLast();
                      out.add(top);                 // Copy function or array from stack to output
                    }
                  }
                } else {
                  throw new ExpressionParserError("unbalanced ()");
                }
                break;
              case "[":
                brackCount++;
                tokStack.add(tok);
                break;
              case "]":
                brackCount--;
                while (tokStack.size() > 0 && !(top = tokStack.getLast()).val.equals("[")) {
                  tokStack.removeLast();
                  out.add(top);                     // Copy tokens from stack to output until "[" found
                }
                if (tokStack.size() > 0) {
                  tokStack.removeLast();            // Discard opening "["
                  if (!tokStack.isEmpty()) {
                    top = tokStack.getLast();
                    if (top.type == Token.Type.ARY) {
                      tokStack.removeLast();
                      out.add(top);                 // Copy function or array from stack to output
                    } else {
                      throw new ExpressionParserError("missing array for matching []");
                    }
                  }
                } else {
                  throw new ExpressionParserError("unbalanced []");
                }
                break;
              default:
                while (tokStack.size() > 0 && !(top = tokStack.getLast()).val.equals("(")  && !top.val.equals("[") && top.prec > tok.prec) {
                  tokStack.removeLast();            // Copy higher precedence tokens from stack to output until "(" or "[" found
                  out.add(top);
                }
                tokStack.add(tok);                  // Push lower precedence token to stack
                break;
            }
            break;
          case CMA:
            while (tokStack.size() > 0 && !(top = tokStack.getLast()).val.equals("(") && !top.val.equals("[")) {
              tokStack.removeLast();
              out.add(top);                         // Copy tokens from stack to output (stop on finding "(" or "[")
            }
            break;
        }
      }
      if (parenCount != 0) {
        throw new ExpressionParserError("unbalanced ()");
      }
      if (brackCount != 0) {
        throw new ExpressionParserError("unbalanced []");
      }
      while (!tokStack.isEmpty()) {
        Token rem = tokStack.removeLast();
        if (!rem.val.equals("(") && !rem.val.equals(")")) {
          out.add(rem);
        }
      }
      // Scan for "++" and "--" postfix operators and merge into VAR or ARY tokens
      List<Token> out2 = new ArrayList<>();
      Token prev = null;
      for (Token tok : out) {
        if (tok.type == Token.Type.OP && (tok.val.equals("++") || tok.val.equals("--"))) {
          if (prev != null && (prev.type == Token.Type.VAR || prev.type == Token.Type.ARY)) {
            prev.setPrePostIncDec("v" + tok.val);
            continue;
          }
        }
        out2.add(tok);
        prev = tok;
      }
      return out2.toArray(new Token[0]);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new ExpressionParserError("Error parsing: '" + in + "' - " + ex.getMessage());
    }
  }

  /**
   * Evaluate the postfix expression in Token[] array expr using the variable
   * values provided in the vals Map and return the result.
   * @param expr Token[] array representing a postfix expression
   * @param vals Map that supplies values for all expression variables
   * @param funcs Map of  external functions
   * @return Object containing result (Boolean or BigInteger)
   */
  private static Object eval (Token[] expr, Map<String,Object> vals, Map<String,Function> funcs) throws ExpressionParserError {
    try {
      LinkedList<Object> valStack = new LinkedList<>();
      int shortcutId = -1;
      for (Token tok : expr) {
        if (shortcutId >= 0) {
          if (shortcutId == tok.shortcutId) {
            shortcutId = -1;
          }
          continue;
        }
        switch (tok.type) {
          case EXP:
            // Ignore expression token (used for diagnostic purposes)
            break;
          case VAL:
            valStack.add(new NumberVal(tok.val));
            break;
          case STR:
            valStack.add(tok.val);
            break;
          case VAR:
            valStack.add(tok.getValue(vals));
            break;
          case ARY:
            int index = ((NumberVal) valStack.removeLast()).intValue();
            valStack.add(new ArrayRef(vals, tok, index));
            break;
          case FNC:
            String func = tok.val;
            if (funcs != null && funcs.containsKey(func.toLowerCase())) {
              Function ff = funcs.get(func.toLowerCase());
              Object arg = ff.call(valStack);
              valStack.add(arg == null ? NULL : arg);
            } else {
              throw new ExpressionParserError(" unknown function " + func + "()");
            }
            break;
          case OP:
            String op = tok.val;
            if (op.equals("!")) {
              Object arg = valStack.removeLast();
              if (arg instanceof Boolean) {
                valStack.add(!(Boolean) arg ? Boolean.TRUE : Boolean.FALSE);
              } else {
                valStack.add(((NumberVal) arg).not());
              }
            } else {
              // Check for shortcut evaluation
              Object stkTop = valStack.getLast();
              if (op.equals("S&")) {
                if (!((Boolean) stkTop)) {
                  shortcutId = tok.shortcutId;
                }
                continue;
              } else if (op.equals("S|")) {
                if (((Boolean) stkTop)) {
                  shortcutId = tok.shortcutId;
                }
                continue;
              }
              ArrayRef lAry = null, rAry;
              Object rArg = valStack.removeLast();
              Object lArg = valStack.removeLast();
              // Note: order of value fetch for ArrayRefs important for ++ and -- operators
              if (lArg instanceof ArrayRef) {
                lAry = (ArrayRef) lArg;
                lArg = lAry.getValue();
              }
              if (rArg instanceof ArrayRef) {
                rAry = (ArrayRef) rArg;
                rArg = rAry.getValue();
              }
              switch (op) {
                case "<":
                case "<=":
                case ">":
                case ">=":
                case "==":
                case "!=":
                  int comp;
                  if (lArg instanceof Null || rArg instanceof Null) {
                    if (op.equals("==")) {
                      valStack.add(lArg.equals(rArg) ? Boolean.TRUE : Boolean.FALSE);
                    } else if (op.equals("!=")) {
                      valStack.add(!lArg.equals(rArg) ? Boolean.TRUE : Boolean.FALSE);
                    } else {
                      throw new ExpressionParserError("illegal comparison to null value for operator " + op);
                    }
                    break;
                  } else if (lArg instanceof String && rArg instanceof String) {
                    comp = ((String) lArg).compareTo((String) rArg);
                  } else if (lArg instanceof NumberVal && rArg instanceof NumberVal) {
                    comp = ((NumberVal) lArg).compareTo(rArg);
                  } else {
                    throw new ExpressionParserError("illegal args for operator " + op);
                  }
                  switch (op) {
                    case "<":
                      valStack.add((comp < 0) ? Boolean.TRUE : Boolean.FALSE);
                      break;
                    case "<=":
                      valStack.add((comp <= 0) ? Boolean.TRUE : Boolean.FALSE);
                      break;
                    case ">":
                      valStack.add((comp > 0) ? Boolean.TRUE : Boolean.FALSE);
                      break;
                    case ">=":
                      valStack.add((comp >= 0) ? Boolean.TRUE : Boolean.FALSE);
                      break;
                    case "==":
                      valStack.add((comp == 0) ? Boolean.TRUE : Boolean.FALSE);
                      break;
                    case "!=":
                      valStack.add((comp != 0) ? Boolean.TRUE : Boolean.FALSE);
                      break;
                  }
                  break;
                case "=":
                  if (expr[1].type == Token.Type.VAR) {
                    vals.put(expr[1].val, rArg);
                    valStack.add(rArg);
                  } else if (lAry != null) {
                    lAry.setValue(rArg);
                    valStack.add(lAry);
                  } else {
                    throw new ExpressionParserError("= assignment to non variable");
                  }
                  break;
                case "+":
                case "+=":
                  if (lArg instanceof String) {
                    valStack.add(((String) lArg).concat(rArg.toString()));
                  } else if (rArg instanceof String) {
                    valStack.add(lArg.toString().concat((String) rArg));
                  } else {
                    valStack.add(((NumberVal) lArg).add((NumberVal) rArg));
                  }
                  if (op.equals("+=")) {
                    if (expr[1].type == Token.Type.VAR) {
                      vals.put(expr[1].val, valStack.getLast());
                    } else if (lAry != null) {
                      lAry.setValue(valStack.getLast());
                    } else {
                      throw new ExpressionParserError("+= assignment to non variable " + op);
                    }
                  }
                  break;
                case "-":
                case "-=":
                  valStack.add(((NumberVal) lArg).subtract((NumberVal) rArg));
                  if (op.equals("-=")) {
                    if (expr[1].type == Token.Type.VAR) {
                      vals.put(expr[1].val, valStack.getLast());
                    } else if (lAry != null) {
                      lAry.setValue(valStack.getLast());
                    } else {
                      throw new ExpressionParserError("-= assignment to non variable " + op);
                    }
                  }
                  break;
                case "*":
                case "*=":
                  valStack.add(((NumberVal) lArg).multiply((NumberVal) rArg));
                  if (op.equals("*=")) {
                    if (expr[1].type == Token.Type.VAR) {
                      vals.put(expr[1].val, valStack.getLast());
                    } else if (lAry != null) {
                      lAry.setValue(valStack.getLast());
                    } else {
                      throw new ExpressionParserError("*= assignment to non variable " + op);
                    }
                  }
                  break;
                case "/":
                case "/=":
                  valStack.add(((NumberVal) lArg).divide((NumberVal) rArg));
                  if (op.equals("/=")) {
                    if (expr[1].type == Token.Type.VAR) {
                      vals.put(expr[1].val, valStack.getLast());
                    } else if (lAry != null) {
                      lAry.setValue(valStack.getLast());
                    } else {
                      throw new ExpressionParserError("assignment to non variable " + op);
                    }
                  }
                  break;
                case "%":
                case "%=":
                  valStack.add(((NumberVal) lArg).mod((NumberVal) rArg));
                  if (op.equals("%=")) {
                    if (expr[1].type == Token.Type.VAR) {
                      vals.put(expr[1].val, valStack.getLast());
                    } else if (lAry != null) {
                      lAry.setValue(valStack.getLast());
                    } else {
                      throw new ExpressionParserError("assignment to non variable " + op);
                    }
                  }
                  break;
                case "<<":
                  // Signed left shift
                  valStack.add(((NumberVal) lArg).sls((NumberVal) rArg));
                  break;
                case ">>":
                  // Signed right shift
                  valStack.add(((NumberVal) lArg).srs((NumberVal) rArg));
                  break;
                case ">>>":
                  // Unsigned right shift
                  valStack.add(((NumberVal) lArg).urs((NumberVal) rArg));
                  break;
                case "&":
                case "&&":
                  if (lArg instanceof Boolean && rArg instanceof Boolean) {
                    valStack.add(Boolean.logicalAnd((Boolean) lArg, (Boolean) rArg));
                  } else if (lArg instanceof NumberVal && rArg instanceof NumberVal) {
                    valStack.add(((NumberVal) lArg).and((NumberVal) rArg));
                  } else {
                    throw new ExpressionParserError("illegal args for operator " + op);
                  }
                  break;
                case "|":
                case "||":
                  if (lArg instanceof Boolean && rArg instanceof Boolean) {
                    valStack.add(Boolean.logicalOr((Boolean) lArg, (Boolean) rArg));
                  } else if (lArg instanceof NumberVal && rArg instanceof NumberVal) {
                    valStack.add(((NumberVal) lArg).or((NumberVal) rArg));
                  } else {
                    throw new ExpressionParserError("illegal args for operator " + op);
                  }
                  break;
                case "^":
                  if (lArg instanceof Boolean && rArg instanceof Boolean) {
                    valStack.add(Boolean.logicalXor((Boolean) lArg, (Boolean) rArg));
                  } else if (lArg instanceof NumberVal && rArg instanceof NumberVal) {
                    valStack.add(((NumberVal) lArg).xor((NumberVal) rArg));
                  } else {
                    throw new ExpressionParserError("illegal args for operator " + op);
                  }
                  break;
                default:
                  throw new ExpressionParserError("Unknown operator " + op);
              }
            }
            break;
        }
      }
      if (valStack.size() > 1) {
        throw new ExpressionParserError("leftover on stack after eval");
      }
      Object ret = valStack.removeLast();
      if (ret instanceof ArrayRef) {
        ret = ((ArrayRef) ret).getValue();
      }
      return ret;
    } catch (ScriptNg.StoppedException ex) {
      throw ex;
    } catch (Exception ex) {
      ExpressionParserError nex;
      if (expr.length > 0 && expr[0].type == Token.Type.EXP) {
        nex = new ExpressionParserError("Error evaluating: '" + expr[0].val + "'" + " - " + ex.getMessage());
      } else {
        nex = new ExpressionParserError("Error evaluating: " + tokensToString(expr)+ " - " + ex.getMessage());
      }
      nex.initCause(ex);
      throw nex;
    }
  }

  private static boolean isHex (char cc) {
    return (cc >= 'a' && cc <= 'f') || (cc >= 'A' && cc <= 'F');
  }

  /**
   * Reformat String to reduce all whitespace to a single space
   * @param text Input text
   * @return Reformatted output
   */
  private static String condenseWhitespace (String text) {
    StringTokenizer tok = new StringTokenizer(text);
    StringBuilder buf = new StringBuilder();
    while (tok.hasMoreTokens()) {
      buf.append(tok.nextToken());
      buf.append(' ');
    }
    return buf.toString().trim();
  }

  /**
   * Convert String[] into a String where each item is separated by a common String separator
   * @param tokens Token array of values that support toString() call
   */
  private static String tokensToString (Token[] tokens) {
    StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (Object obj : tokens) {
      if (!first) {
        buf.append(", ");
      }
      first = false;
      buf.append(obj.toString());
    }
    return buf.toString();
  }

  private static boolean evalCompare (PrintStream out, String expr, Comparable expected) {
    return evalCompare(out, expr, null, expected);
  }

  private static boolean evalCompare (PrintStream out, String expr, Map<String,Object> vals, Comparable expected) {
    return evalCompare(out, expr, vals, null, expected);
  }

  private static boolean evalCompare (PrintStream out, String expr, Map<String,Object> vals,  Map<String,Function> funcs, Comparable expected) {
    try {
      Object result = run(expr, vals, funcs);
      boolean err = !(((Comparable) result).compareTo(expected) == 0);
      if (err && out != null) {
        out.println(expr + ": result = " + result + ", expected " + expected);
      }
      return err;
    } catch (ExpressionParserError ex) {
      if (out != null) {
        out.println(expr + " -> " + ex.getMessage());
      }
    }
    return true;
  }

  private static boolean doTests (PrintStream out) {
    boolean err;
    // Test BigDecimal, BigInteger and mixed BigDecimal/BigInteger Addition
    Map<String, Object> vals = new HashMap<>();
    vals.put("dec1", new NumberVal("1.2"));
    vals.put("int1", new NumberVal("3"));
    err = evalCompare(out, "1 + 1", vals, new NumberVal("2"));
    err |= evalCompare(out, "2.2 + 3.3", vals, new NumberVal("5.5"));
    err |= evalCompare(out, "dec1 + dec1", vals, new NumberVal("2.4"));
    err |= evalCompare(out, "dec1 + int1", vals, new NumberVal("4.2"));
    err |= evalCompare(out, "int1 + int1", vals, new NumberVal("6"));
    // Test BigDecimal, BigInteger and mixed BigDecimal/BigInteger Subtraction
    err |= evalCompare(out, "2 - 3", vals, new NumberVal("-1"));
    err |= evalCompare(out, "2.2 - 3.3", vals, new NumberVal("-1.1"));
    err |= evalCompare(out, "dec1 - dec1", vals, new NumberVal("0"));
    err |= evalCompare(out, "dec1 - int1", vals, new NumberVal("-1.8"));
    err |= evalCompare(out, "int1 - int1", vals, new NumberVal("0"));
    // Test BigDecimal, BigInteger and mixed BigDecimal/BigInteger Multiplication
    err |= evalCompare(out, "2 * 3", vals, new NumberVal("6"));
    err |= evalCompare(out, "2.2 * 3.3", vals, new NumberVal("7.26"));
    err |= evalCompare(out, "dec1 * dec1", vals, new NumberVal("1.44"));
    err |= evalCompare(out, "dec1 * int1", vals, new NumberVal("3.6"));
    err |= evalCompare(out, "int1 * int1", vals, new NumberVal("9"));
    // Test BigDecimal, BigInteger and mixed BigDecimal/BigInteger Division
    err |= evalCompare(out, "4 / 2", vals, new NumberVal("2"));
    err |= evalCompare(out, "6.6 / 3.3", vals, new NumberVal("2"));
    err |= evalCompare(out, "dec1 / dec1", vals, new NumberVal("1"));
    err |= evalCompare(out, "dec1 / int1", vals, new NumberVal("0.4"));
    err |= evalCompare(out, "int1 / int1", vals, new NumberVal("1"));
    // Test scale invariant BigDecimal comparison
    err |= evalCompare(out, "2.000 == 2.0", vals, Boolean.TRUE);
    err |= evalCompare(out, "2.000 == 2", vals, Boolean.TRUE);
    err |= evalCompare(out, "2 != 2.00", vals, Boolean.FALSE);
    // Test String Concatenation
    vals = new HashMap<>();
    vals.put("I", "X");
    vals.put("Q", "Y");
    err |= evalCompare(out, "I + Q == 'XY'", vals, Boolean.TRUE);
    err |= evalCompare(out, "I += Q", vals, "XY");
    err |= evalCompare(out, "I == 'XY'", vals, Boolean.TRUE);
    err |= evalCompare(out, "0xD8 + 0x01", vals, new NumberVal("0xD9"));
    // Test String Comparison operators
    err |= evalCompare(out, "'XX' == 'XX'", Boolean.TRUE);
    err |= evalCompare(out, "'XX' != 'YY'", Boolean.TRUE);
    err |= evalCompare(out, "('XX' == 'XX') & true", Boolean.TRUE);
    err |= evalCompare(out, "'XX' < 'XY'", Boolean.TRUE);
    err |= evalCompare(out, "'XY' > 'XX'", Boolean.TRUE);
    err |= evalCompare(out, "'XX' <= 'XY'", Boolean.TRUE);
    err |= evalCompare(out, "'XX' <= 'XY'", Boolean.TRUE);
    err |= evalCompare(out, "'X' + 'Y' == 'XY'", Boolean.TRUE);
    err |= evalCompare(out, "'X' + 10 == 'X10'", Boolean.TRUE);
    err |= evalCompare(out, "10 + 'X' == '10X'", Boolean.TRUE);
    err |= evalCompare(out, "Q + 10 == 'Y10'", vals, Boolean.TRUE);
    err |= evalCompare(out, "10 + Q == '10Y'", vals, Boolean.TRUE);
    // Test BigInteger Math and Logical operators
    vals.clear();
    vals.put("A", null);
    err |= evalCompare(out, "(((2) + (2)) > ((1 + 1)))", Boolean.TRUE);
    err |= evalCompare(out, "(2 + 2) * (1 + 1)", new HashMap<>(), new NumberVal("8"));
    err |= evalCompare(out, "4 + 2", new HashMap<>(), new NumberVal("6"));
    err |= evalCompare(out, "4 + -2", new HashMap<>(), new NumberVal("2"));
    err |= evalCompare(out, "4 - 2", new HashMap<>(), new NumberVal("2"));
    err |= evalCompare(out, "4 / 2", new HashMap<>(), new NumberVal("2"));
    err |= evalCompare(out, "(2 * (3 + 3)) / 2", new HashMap<>(), new NumberVal("6"));
    err |= evalCompare(out, "(1 ^ (1 | 2)) & 3", new HashMap<>(), new NumberVal("2"));
    err |= evalCompare(out, "(1 ^ !3) & 3", new HashMap<>(), new NumberVal("1"));
    err |= evalCompare(out, "5 % 2", new HashMap<>(), new NumberVal("1"));
    err |= evalCompare(out, "1 << 2", new HashMap<>(), new NumberVal("4"));
    err |= evalCompare(out, "-1 << 2", new HashMap<>(), new NumberVal("-4"));
    err |= evalCompare(out, "8 >> 2", new HashMap<>(), new NumberVal("2"));
    err |= evalCompare(out, "-8 >> 2", new HashMap<>(), new NumberVal("-2"));
    err |= evalCompare(out, "-8 >>> 2", new HashMap<>(), new NumberVal("-2"));
    vals.clear();
    vals.put("A", new NumberVal("10"));
    vals.put("B", new NumberVal("20"));
    err |= evalCompare(out, "A + B", vals, new NumberVal("30"));
    err |= evalCompare(out, "A - B", vals, new NumberVal("-10"));
    // Test Assignment operators
    vals.clear();
    err |= evalCompare(out, "ii = 2", vals, new NumberVal("2"));
    err |= evalCompare(out, "ii += 5 - 2", vals, new NumberVal("5"));
    err |= evalCompare(out, "ii -= 5 - 2", vals, new NumberVal("2"));
    err |= evalCompare(out, "ii *= 5 - 2", vals, new NumberVal("6"));
    err |= evalCompare(out, "ii /= 5 - 2", vals, new NumberVal("2"));
    err |= evalCompare(out, "ii %= 5 - 2", vals, new NumberVal("2"));
    // Test Pre and Post Increment and Decrement operators
    vals = new HashMap<>();
    run("ii = 5", vals, null);
    run("jj = 3", vals, null);
    err |= evalCompare(out, "ii-- + jj--", vals, new NumberVal("8"));   // ii = 4, jj = 2
    err |= evalCompare(out, "ii + jj", vals, new NumberVal("6"));       // ii = 4, jj = 2
    err |= evalCompare(out, "++ii + ++jj", vals, new NumberVal("8"));   // ii = 5, jj = 3
    err |= evalCompare(out, "--ii + ii", vals, new NumberVal("8"));     // ii = 4
    err |= evalCompare(out, "++ii + ii", vals, new NumberVal("10"));    // ii = 5
    err |= evalCompare(out, "ii-- + ii", vals, new NumberVal("9"));     // ii = 4
    // Test Shortcut evaluation
    vals = new HashMap<>();
    run("ii = 5", vals, null);
    err |= evalCompare(out, "ii == 5 && ++ii == 6", vals, Boolean.TRUE);
    err |= evalCompare(out, "ii == 6", vals, Boolean.TRUE);
    err |= evalCompare(out, "ii == 5 && ++ii == 6", vals, Boolean.FALSE);
    err |= evalCompare(out, "ii == 6", vals, Boolean.TRUE);
    run("ii = 5", vals, null);
    err |= evalCompare(out, "ii == 5 || ++ii == 6", vals, Boolean.TRUE);
    err |= evalCompare(out, "ii == 5", vals, Boolean.TRUE);
    err |= evalCompare(out, "ii == 4 || ++ii == 6", vals, Boolean.TRUE);
    err |= evalCompare(out, "ii == 6", vals, Boolean.TRUE);
    // Test Pre and Post Increment and Decrement operators
    vals = new HashMap<>();
    run("ii = 5", vals, null);
    run("jj = 3", vals, null);
    err |= evalCompare(out, "ii-- + jj--", vals, new NumberVal("8"));   // ii = 4, jj = 2
    err |= evalCompare(out, "ii + jj", vals, new NumberVal("6"));       // ii = 4, jj = 2
    err |= evalCompare(out, "++ii + ++jj", vals, new NumberVal("8"));   // ii = 5, jj = 3
    err |= evalCompare(out, "--ii + ii", vals, new NumberVal("8"));     // ii = 4
    err |= evalCompare(out, "++ii + ii", vals, new NumberVal("10"));    // ii = 5
    err |= evalCompare(out, "ii-- + ii", vals, new NumberVal("9"));     // ii = 4
    // Test Pre and Post Increment and Decrement operators woth BigDecimal values
    vals = new HashMap<>();
    run("ii = 5.1", vals, null);
    run("jj = 3.2", vals, null);
    err |= evalCompare(out, "ii-- + jj--", vals, new NumberVal("8.3")); // ii = 4.1, jj = 2.2
    err |= evalCompare(out, "ii + jj", vals, new NumberVal("6.3"));     // ii = 4.1, jj = 2.2
    err |= evalCompare(out, "++ii + ++jj", vals, new NumberVal("8.3")); // ii = 5.1, jj = 3.2
    err |= evalCompare(out, "--ii + ii", vals, new NumberVal("8.2"));   // ii = 4.1
    err |= evalCompare(out, "++ii + ii", vals, new NumberVal("10.2"));  // ii = 5.1
    err |= evalCompare(out, "ii-- + ii", vals, new NumberVal("9.2"));   // ii = 4.1
    // Test shortcut operators
    vals = new HashMap<>();
    vals.put("I", "X");
    vals.put("Q", "Y");
    vals.put("V1", "1");
    vals.put("V2", "2");
    vals.put("V3", null);
    err |= evalCompare(out, "QQ == '1'", vals, Boolean.FALSE);                      // QQ undefined
    err |= evalCompare(out, "QQ != '1'", vals, Boolean.TRUE);                       // QQ undefined
    err |= evalCompare(out, "V3 != null  &&  V3 == 'TEST'", vals, Boolean.FALSE);   // V3 is null
    err |= evalCompare(out, "V3 == null  ||  V3 == 'TEST'", vals, Boolean.TRUE);    // V3 is null
    err |= evalCompare(out, "V2 == null  ||  V2 == 'TEST'", vals, Boolean.FALSE);   // V3 is null
    err |= evalCompare(out, "11 < 12", Boolean.TRUE);
    err |= evalCompare(out, "11 < 11", Boolean.FALSE);
    err |= evalCompare(out, "12 > 10", Boolean.TRUE);
    err |= evalCompare(out, "11 > 11", Boolean.FALSE);
    err |= evalCompare(out, "12 <= 12", Boolean.TRUE);
    err |= evalCompare(out, "12 <= 12", Boolean.TRUE);
    err |= evalCompare(out, "13 <= 12", Boolean.FALSE);
    err |= evalCompare(out, "12 >= 12", Boolean.TRUE);
    err |= evalCompare(out, "13 >= 12", Boolean.TRUE);
    err |= evalCompare(out, "12 >= 13", Boolean.FALSE);
    err |= evalCompare(out, "10 == 10", Boolean.TRUE);
    err |= evalCompare(out, "10 == 11", Boolean.FALSE);
    err |= evalCompare(out, "10 != 10", Boolean.FALSE);
    err |= evalCompare(out, "10 != 11", Boolean.TRUE);
    err |= evalCompare(out, "-2 < -1", Boolean.TRUE);
    err |= evalCompare(out, "true & true", Boolean.TRUE);
    err |= evalCompare(out, "true & false", Boolean.FALSE);
    err |= evalCompare(out, "true | false", Boolean.TRUE);
    err |= evalCompare(out, "!true ^ !false", Boolean.TRUE);
    // Test Pre and Post Increment and Decrement operators woth Array values
    vals = new HashMap<>();
    run("ii[0] = 5", vals, null);
    run("ii[1] = 3", vals, null);
    err |= evalCompare(out, "ii[0]-- + ii[1]--", vals, new NumberVal("8"));
    err |= evalCompare(out, "ii[0] + ii[1]", vals, new NumberVal("6"));
    err |= evalCompare(out, "++ii[0] + ++ii[1]", vals, new NumberVal("8"));
    err |= evalCompare(out, "--ii[0] + ii[0]", vals, new NumberVal("8"));
    err |= evalCompare(out, "++ii[0] + ii[0]", vals, new NumberVal("10"));
    err |= evalCompare(out, "ii[0]-- + ii[0]", vals, new NumberVal("9"));
    // Test String Concatenation with Arrays
    vals = new HashMap<>();
    run("s[0] = 'XX'", vals, null);
    run("s[1] = 'ABC'", vals, null);
    run("t[0] = 123", vals, null);
    err |= evalCompare(out, "s[0] += 'YY'", vals, "XXYY");
    err |= evalCompare(out, "s[0] + 'YY'", vals, "XXYYYY");
    err |= evalCompare(out, "s[0] + s[1]", vals, "XXYYABC");
    err |= evalCompare(out, "++t[0] + ' ms'", vals, "124 ms");
    // Test Array assignments and references
    vals = new HashMap<>();
    run("v = 5", vals, null);
    run("c = a[0]", vals, null);
    err |= evalCompare(out, "c == null", vals, Boolean.TRUE);
    run("a[0] = 5", vals, null);
    run("c = a[0]", vals, null);
    err |= evalCompare(out, "c == 5", vals, Boolean.TRUE);
    run("a[1] = 2", vals, null);
    run("a[2] = 3", vals, null);
    err |= evalCompare(out, "a[1]", vals, new NumberVal("2"));
    err |= evalCompare(out, "a[2]", vals, new NumberVal("3"));
    err |= evalCompare(out, "a[1] == a[2]", vals, Boolean.FALSE);
    run("a[2] += 1", vals, null);
    err |= evalCompare(out, "a[2] == 4", vals, Boolean.TRUE);
    err |= evalCompare(out, "4 == a[2]", vals, Boolean.TRUE);
    err |= evalCompare(out, "a[1]", vals, new NumberVal("2"));
    err |= evalCompare(out, "a[1] += 1", vals, new NumberVal("3"));
    err |= evalCompare(out, "a[1] -= 1", vals, new NumberVal("2"));
    err |= evalCompare(out, "a[1] /= 2", vals, new NumberVal("1"));
    err |= evalCompare(out, "a[1] *= 4", vals, new NumberVal("4"));
    vals = new HashMap<>();
    run("A = 10", vals, null);
    run("B = 20", vals, null);
    err |= evalCompare(out, "max((A),(B))", vals, new NumberVal("20"));
    err |= evalCompare(out, "min(A,B)", vals, new NumberVal("10"));
    err |= evalCompare(out, "max(10, 9.9)", vals, new NumberVal("10"));
    err |= evalCompare(out, "max(9.9, 10)", vals, new NumberVal("10"));
    err |= evalCompare(out, "min(10, 9.9)", vals, new NumberVal("9.9"));
    err |= evalCompare(out, "min(9.9, 10)", vals, new NumberVal("9.9"));
    err |= evalCompare(out, "pow(3.0, 2)", vals, new NumberVal("9.0"));
    err |= evalCompare(out, "pow(3, 2)", vals, new NumberVal("9"));
    err |= evalCompare(out, "abs(-2)", vals, new NumberVal("2"));
    // Test built-in Global Bit-related functions
    vals = new HashMap<>();
    run("A = 0", vals, null);
    run("B = 0", vals, null);
    err |= evalCompare(out, "trunc(1.0 / 3, 2)", vals, new NumberVal("0.33"));
    err |= evalCompare(out, "trunc(1.22, 0)", vals, new NumberVal("1"));
    err |= evalCompare(out, "bit(0x80, 7)", Boolean.TRUE);
    err |= evalCompare(out, "set(0, 2)", vals, new NumberVal("4"));
    err |= evalCompare(out, "clr(7, 1)", vals, new NumberVal("5"));
    err |= evalCompare(out, "A = flip(A, 1)", vals, new NumberVal("2"));
    err |= evalCompare(out, "B = set(B, 2)", vals, new NumberVal("4"));
    err |= evalCompare(out, "A == 2 && B == 4", vals, Boolean.TRUE);
    // Test Array of String values;
    vals = new HashMap<>();
    run("s[0] = 'A'", vals, null);
    run("s[1] = 'B'", vals, null);
    run("s[2] = 'C'", vals, null);
    err |= evalCompare(out, "s[0] + s[1] + s[2]", vals, "ABC");
    // Test example External function
    Map<String, Function> funcs = new HashMap<>();
    funcs.put("reverse", new Reverse());
    err |= evalCompare(out, "reverse('XYZ') == 'ZYX'", null, funcs, Boolean.TRUE);
    return err;
  }

  public static void main (String[] args) {
    long start = System.currentTimeMillis();
    boolean err = doTests(System.out);
    long end = System.currentTimeMillis();
    System.out.println("Execution time " + (end - start) + "ms");
    if (!err) {
      System.out.println("All tests pass!");
    }
  }
}
