<p align="left"><img src="https://github.com/wholder/ScriptNg/blob/master/images/ScriptRunner%20Screenshot.png?sanitize=true"></p>

ScriptNg is an experimental interperter for a simple scripting language I'm developing.  The included ScriptRunner class (shown above) provides an easy way to try out the ScriptNg interpreter by running the sample script, or writing your own.  ScriptRunner provides the following features:

   - Press "RUN" ScriptNg scripts at full speed, or use the "Run" menu to set the run speed and watch the code animate line by line
   - Use "STEP" button to manually advance through program line by line, or "STOP" to cancel execution
   - Set and clear breakpoints by clicking the small circles next to the line numbers
   - After hitting a breakpoint you can press "RUN" to resume running, "STEP" to advance one line, or "STOP" to cancel execution
   - You can also press "STOP" to pause running code, then use "STEP" or "RUN" to continue, or press "STOP" again to cancel execution
   - Use the File menu to Read or Save .script files, or select "New" to clear the edit pane.
   
Note: Currently, ScriptRunner will let you set nonfunctional breakpoints on blanks lines, so don't expect these to work.

## Requirements
The current code and runnable Jar file requires Java 8, or later.  [Click Here to download and install the latest version of Java from Oracle](https://www.java.com/en/download/).  I wrote ScriptNg on a Mac Pro using the _Community_ version of [IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) and macOs is the only environment where I have extensively tested ScriptNg .  However, as the code is 100% Java, it should also run on MS Windows and Linux systems.

If you just want to try out the program, you don't need to download and compile the source code, as I try to maintain a pre-built, executable JAR file in the [out/artifacts/ScriptNg](https://github.com/wholder/ScriptNg/tree/master/out/artifacts/ScriptNg) folder from which you can download and run ScriptNg as long as you have Java installed on your system.  On a Mac, simply double click the ScriptNg.jar file to run it once you've downloaded it, although you'll probably have to right click and select "Open" the  first time you run ScriptNg due to the Mac OS X security check and the fact that I'm currently unable to digitally sign the JAR file.

## The ScriptNg Language

ScriptNg implements a simple scripting language using my ExpressionParser class.  Statement nesting is implemented by indentation, but an optional "end" line can be used for clarity.  ScriptNg supports the following constructs:

  - Assignment expression, such as "**`ii = 10 + 5`**"
  - While loops, such as "**`while (ii < 10)`**"
  - For loops, such as "**`for (ii = 0; ii < 10; ii++)`**"
  - Conditional tests: "**`if`**", "**`elif`**", and "**`else`**"
  - Output via **`print("hello")`** and **`println(1 + 2)`** functions
  - Local function declaration, such as "**`function sum(a, b)`**" with optional "**`return`**" statement
  - Expression evaluation is handled by the **`ExpressionParser`** class

## ScriptNg's **`ExpressionParser`** class

The **`ExpressionParser`** class used by ScriptNg supports the following features and/or limitations:

  - Auto type assignment, such as "**`a = 10`**" for Integer, or "**`a = 2.5`**" for Decimal values
  - For extended precision, **`ExpressionParser`** uses Java's **`BigInteger`** and **`BigDecimal`** classes for arithmetic
  - NumberVal class handles mixed Decimal/Integer Math with auto conversion from Integer to Decimal, when needed
  - Internally, NumberVal uses **`MathContext.DECIMAL128`** to limit precision, when needed
  - New **`trunc(v,n)`** function to set value v to n fractional digits (if n = 0, result is Integer, else Decimal)
  - The **`pow(v,p)`** function raises v to n power (result is Decimal if v is Decimal, else Integer)
  - Assignment operators: **`=`**, **`+=`**, **`-=`**, **`*=`**, **`/=`** and **`%=`** (assign to variable and place in return stack) are supported
  - Comparison operators work with with String args, such as "**`==`**", "**`!=`**", "**`<`**", "**`>`**", "**`<=`**" and "**`>=`**"
  - The **`Function`** interface supports calling external functions written in Java
  - Pre and Post Increment and Decrement ("**`++`**" & "**`--`**") operators are supported
  - Bit-related functions provided are: **`bit(v,b)`**, **`set(v,b)`**, **`clr(v,b)`** and **`flip(v,b)`**
  - The built-in **`millis()`** function returns **`System.currentTimeMillis()`** for timing calculations
  - String concatenation works with numbers, such as "**`a = 10 + ' ms'`**" as well as "**`+=`**" operator
  - Only one dimensional arrays, such as "**`a[0] = 2`**" or "**`b[2] = 'test'`**" are supported
  - Strings can be delimited by (**`'`**) or (**`"`**), such as **`'abc'`** or **`"abc"`**
