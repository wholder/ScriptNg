<p align="center"><img src="https://github.com/wholder/ScriptNg/blob/master/images/ScriptRunner%20Screenshot.png?sanitize=true"></p>

**ScriptNg** is an experimental interpreter for a simple scripting language I'm developing as a potential teaching aid and for inclusion in some of my other projects, such as [RglComm](https://github.com/wholder/RglComm) and [ADU218](https://github.com/wholder/ADU218).  The included **ScriptRunner** class (shown above) is provided as a way to try out the ScriptNg interpreter by running the sample script, or writing your own.  ScriptRunner provides the following features:

   - Press "RUN" ScriptNg scripts at full speed, or use the "Run" menu to set the run speed and watch the code animate line by line
   - Use "STEP" button to manually advance through program line by line, or "STOP" to cancel execution
   - Set and clear breakpoints by clicking the small circles next to the line numbers
   - After hitting a breakpoint you can press "RUN" to resume running, "STEP" to advance one line, or "STOP" to cancel execution
   - You can also press "STOP" to pause running code, then use "STEP" or "RUN" to continue, or press "STOP" again to cancel execution
   - Use the File menu to Read or Save .script files, or select "New" to clear the edit pane.
   - When using STEP or stopped at a breakpoint, the "Variables" pane shows the current values of each variable in the current scope
   
## Limitations

  - As I mentioned above, this code is still in development and is, therefore, likely to have bugs.
  - ScriptRunner will let you set breakpoints on blanks lines, but these will not work to stop code execution.

## Requirements
The current code and runnable Jar file requires Java 8, or later.  [Click Here to download and install the latest version of Java from Oracle](https://www.java.com/en/download/).  I wrote ScriptNg on a Mac Pro using the _Community_ version of [#IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) and macOs is the only environment where I have extensively tested ScriptNg .  However, as the code is 100% Java, it should also run on MS Windows and Linux systems.

If you just want to try out the program, you don't need to download and compile the source code, as I try to maintain a pre-built, executable JAR file in the [out/artifacts/ScriptNg](https://github.com/wholder/ScriptNg/tree/master/out/artifacts/ScriptNg_jar) folder from which you can download and run ScriptNg as long as you have Java installed on your system.  On a Mac, simply double click the ScriptNg.jar file to run it once you've downloaded it, although you'll probably have to right click and select "Open" the  first time you run ScriptNg due to the Mac OS X security check and the fact that I'm currently unable to digitally sign the JAR file.

## The ScriptNg Language

**ScriptNg** uses my **`ExpressionParser`** class to evaluate conditionals and to perform arithmetic and logic operations.  Statement nesting is implemented by indentation, but an optional "end" line can be used for clarity.  ScriptNg currently supports the following constructs:

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

## Arbitrary Precision Integer Math

While my **`ExpressionParser`** class' use of Java's BigInteger class to internally represent integers does greatly slow down its ability to calculate, it does allow you to easily write code that can performs calculations with arbitrarily-large integer values.  For an example of where this can be useful, recent episodes of [Numberphile](https://www.youtube.com/user/numberphile/videos) have discussed the discovery the last number below 100 which previously could not be expressed as the sum of three cubes ([The Mystery of 42 is Solved](https://www.youtube.com/watch?v=zyG8Vlw5aAw)).  And, a follow up Numberphile video shows a new way to compute [3 as the sum of the 3 cubes](https://www.youtube.com/watch?v=GXhzZAem7k0&t=732s).  If you want to repeat these calculations yourself, they are easy to perform in ScriptRunner, as shown below: 

### Using ScriptRunner to compute 3 as the sum of three cubes

<p align="center"><img src="https://github.com/wholder/ScriptNg/blob/master/images/Cubes3.png?sanitize=true"></p>

    // Compute 3 as the sum of 3 cubes
    a = 569936821221962380720
    b = -569936821113563493509
    c = -472715493453327032
    println('result = ' + a*a*a + b*b*b + c*c*c)

### ScriptNg code to compute 42 as the sum of three cubes

    // Compute 42 as the sum of 3 cubes
    a = -80538738812075974
    b = 80435758145817515
    c = 12602123297335631
    println('result = ' + a*a*a + b*b*b + c*c*c)
