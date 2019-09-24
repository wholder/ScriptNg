import java.math.*;

public class NumberVal implements Comparable {
  private static MathContext  PRECISION = MathContext.DECIMAL128;
  static final NumberVal      ONE = new NumberVal(1L);
  private Number              value;

  NumberVal (String val) {
    if (val.contains(".")) {
      value = new BigDecimal(val);
    } else {
      if (val.startsWith("0x")) {
        value = new BigInteger(val.substring(2), 16);
      } else {
        value = new BigInteger(val);
      }
    }
  }
  NumberVal (Number val) {
    value = val;
  }

  private NumberVal (BigInteger val) {
    value = val;
  }

  private NumberVal (BigDecimal val) {
    value = val;
  }

  NumberVal (long val) {
    value = BigInteger.valueOf(val);
  }

  NumberVal add (NumberVal arg) {
    if (value instanceof BigDecimal) {
      if (arg.value instanceof BigDecimal) {
        // This object is BigDecimal and argument is BigDecimal
        return new NumberVal(((BigDecimal) value).add((BigDecimal) arg.value));
      } else {
        // This object is BigDecimal and argument is BigInteger
        return new NumberVal(((BigDecimal) value).add(new BigDecimal((BigInteger) arg.value)));
      }
    } else {
      if (arg.value instanceof BigDecimal) {
        // This object is BigInteger and argument is BigDecimal
        return new NumberVal((new BigDecimal((BigInteger) value)).add((BigDecimal) arg.value));
      } else {
        // This object is BigInteger and argument is BigInteger
        return new NumberVal(((BigInteger) value).add((BigInteger) arg.value));
      }
    }
  }

  NumberVal subtract (NumberVal arg) {
    if (value instanceof BigDecimal) {
      if (arg.value instanceof BigDecimal) {
        // This object is BigDecimal and argument is BigDecimal
        return new NumberVal(((BigDecimal) value).subtract((BigDecimal) arg.value));
      } else {
        // This object is BigDecimal and argument is BigInteger
        return new NumberVal(((BigDecimal) value).subtract(new BigDecimal((BigInteger) arg.value)));
      }
    } else {
      if (arg.value instanceof BigDecimal) {
        // This object is BigInteger and argument is BigDecimal
        return new NumberVal((new BigDecimal((BigInteger) value)).subtract((BigDecimal) arg.value));
      } else {
        // This object is BigInteger and argument is BigInteger
        return new NumberVal(((BigInteger) value).subtract((BigInteger) arg.value));
      }
    }
  }

  NumberVal multiply (NumberVal arg) {
    if (value instanceof BigDecimal) {
      if (arg.value instanceof BigDecimal) {
        // This object is BigDecimal and argument is BigDecimal
        return new NumberVal(((BigDecimal) value).multiply((BigDecimal) arg.value));
      } else {
        // This object is BigDecimal and argument is BigInteger
        return new NumberVal(((BigDecimal) value).multiply(new BigDecimal((BigInteger) arg.value)));
      }
    } else {
      if (arg.value instanceof BigDecimal) {
        // This object is BigInteger and argument is BigDecimal
        return new NumberVal((new BigDecimal((BigInteger) value)).multiply((BigDecimal) arg.value));
      } else {
        // This object is BigInteger and argument is BigInteger
        return new NumberVal(((BigInteger) value).multiply((BigInteger) arg.value));
      }
    }
  }

  NumberVal divide (NumberVal arg) {
    if (value instanceof BigDecimal) {
      if (arg.value instanceof BigDecimal) {
        // This object is BigDecimal and argument is BigDecimal
        return new NumberVal(((BigDecimal) value).divide((BigDecimal) arg.value, PRECISION));
      } else {
        // This object is BigDecimal and argument is BigInteger
        return new NumberVal(((BigDecimal) value).divide(new BigDecimal((BigInteger) arg.value), PRECISION));
      }
    } else {
      if (arg.value instanceof BigDecimal) {
        // This object is BigInteger and argument is BigDecimal
        return new NumberVal((new BigDecimal((BigInteger) value)).divide((BigDecimal) arg.value, PRECISION));
      } else {
        // This object is BigInteger and argument is BigInteger
        return new NumberVal(((BigInteger) value).divide((BigInteger) arg.value));
      }
    }
  }

  NumberVal min ( NumberVal arg) {
    if (value instanceof BigDecimal) {
      if (arg.value instanceof BigDecimal) {
        // This object is BigDecimal and argument is BigDecimal
        return new NumberVal(((BigDecimal) value).min((BigDecimal) arg.value));
      } else {
        // This object is BigDecimal and argument is BigInteger
        return new NumberVal(((BigDecimal) value).min(new BigDecimal((BigInteger) arg.value)));
      }
    } else {
      if (arg.value instanceof BigDecimal) {
        // This object is BigInteger and argument is BigDecimal
        return new NumberVal((new BigDecimal((BigInteger) value)).min((BigDecimal) arg.value));
      } else {
        // This object is BigInteger and argument is BigInteger
        return new NumberVal(((BigInteger) value).min((BigInteger) arg.value));
      }
    }
  }

  NumberVal max (NumberVal arg) {
    if (value instanceof BigDecimal) {
      if (arg.value instanceof BigDecimal) {
        // This object is BigDecimal and argument is BigDecimal
        return new NumberVal(((BigDecimal) value).max((BigDecimal) arg.value));
      } else {
        // This object is BigDecimal and argument is BigInteger
        return new NumberVal(((BigDecimal) value).max(new BigDecimal((BigInteger) arg.value)));
      }
    } else {
      if (arg.value instanceof BigDecimal) {
        // This object is BigInteger and argument is BigDecimal
        return new NumberVal((new BigDecimal((BigInteger) value)).max((BigDecimal) arg.value));
      } else {
        // This object is BigInteger and argument is BigInteger
        return new NumberVal(((BigInteger) value).max((BigInteger) arg.value));
      }
    }
  }

  NumberVal mod (NumberVal arg) {
    if (value instanceof BigDecimal || arg.value instanceof BigDecimal) {
      throw new IllegalArgumentException("arg not compatible for mod() operation");
    } else {
      return new NumberVal(((BigInteger) value).mod(((BigInteger) arg.value)));
    }
  }

  NumberVal and (NumberVal arg) {
    if (value instanceof BigDecimal || arg.value instanceof BigDecimal) {
      throw new IllegalArgumentException("arg not compatible for and() operation");
    } else {
      return new NumberVal(((BigInteger) value).and(((BigInteger) arg.value)));
    }
  }

  NumberVal or (NumberVal arg) {
    if (value instanceof BigDecimal || arg.value instanceof BigDecimal) {
      throw new IllegalArgumentException("arg not compatible for or() operation");
    } else {
      return new NumberVal(((BigInteger) value).or(((BigInteger) arg.value)));
    }
  }

  NumberVal xor (NumberVal arg) {
    if (value instanceof BigDecimal || arg.value instanceof BigDecimal) {
      throw new IllegalArgumentException("arg not compatible for xor() operation");
    } else {
      return new NumberVal(((BigInteger) value).xor(((BigInteger) arg.value)));
    }
  }

  NumberVal not () {
    if (value instanceof BigDecimal) {
      throw new IllegalArgumentException("arg not compatible for not() operation");
    } else {
      return new NumberVal(((BigInteger) value).not());
    }
  }

  // Signed left shift (>>)
  NumberVal sls (NumberVal arg) {
    if (value instanceof BigDecimal || arg.value instanceof BigDecimal) {
      throw new IllegalArgumentException("arg not compatible for sls() operation");
    } else {
      return new NumberVal(((BigInteger) value).shiftLeft(arg.value.intValue()));
    }
  }

  // Signed right shift (<<)
  NumberVal srs (NumberVal arg) {
    if (value instanceof BigDecimal || arg.value instanceof BigDecimal) {
      throw new IllegalArgumentException("arg not compatible for srs() operation");
    } else {
      BigInteger divisor = BigInteger.ONE.shiftLeft((arg.value.intValue()));
      return new NumberVal(((BigInteger) value).divide(divisor));

    }
  }

  // Unsigned right shift (>>>)
  NumberVal urs (NumberVal arg) {
    if (value instanceof BigDecimal || arg.value instanceof BigDecimal) {
      throw new IllegalArgumentException("arg not compatible for urs() operation");
    } else {
      return new NumberVal(((BigInteger) value).shiftRight(arg.value.intValue()));
    }
  }

  NumberVal pow (NumberVal arg) {
    if (arg.value instanceof BigDecimal) {
      throw new IllegalArgumentException("arg not compatible for pow() operation");
    } else {
      int power = arg.value.intValue();
      if (value instanceof BigDecimal) {
        return new NumberVal(((BigDecimal) value).pow(power, PRECISION));
      } else  {
        return new NumberVal(((BigInteger) value).pow(power));
      }
    }
  }

  // Implement Comparable interface
  public int compareTo (Object argIn) {
    NumberVal arg = (NumberVal) argIn;
    if (value instanceof BigDecimal) {
      if (arg.value instanceof BigDecimal) {
        // This object is BigDecimal and argument is BigDecimal
        return ((BigDecimal) value).compareTo((BigDecimal) arg.value);
      } else {
        // This object is BigDecimal and argument is BigInteger
        return ((BigDecimal) value).compareTo(new BigDecimal((BigInteger) arg.value));
      }
    } else {
      if (arg.value instanceof BigDecimal) {
        // This object is BigInteger and argument is BigDecimal
        return (new BigDecimal((BigInteger) value)).compareTo((BigDecimal) arg.value);
      } else {
        // This object is BigInteger and argument is BigInteger
        return ((BigInteger) value).compareTo((BigInteger) arg.value);
      }
    }
  }

  NumberVal abs () {
    if (value instanceof BigDecimal) {
      return new NumberVal(((BigDecimal) value).abs());
    } else  {
      return new NumberVal(((BigInteger) value).abs());
    }
  }

  public String toString () {
    return value.toString();
  }

  Number getValue () {
    return value;
  }

  int intValue () {
    return value.intValue();
  }

  public static void main (String[] args) {
  }
}
