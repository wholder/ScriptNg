import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class BigTest {
  public static void main (String[] args) {
    BigInteger v1 = new BigInteger("1");
    BigInteger v2 = new BigInteger("3");
    BigInteger v3 = v1.add(v2);             // Does not assign result to v1
    System.out.println("v1 = " + v1);
    System.out.println("v2 = " + v2);
    System.out.println("v3 = " + v3);
  }
}
