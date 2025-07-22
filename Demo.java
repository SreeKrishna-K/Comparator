import com.google.gson.*;

public class Demo {
    public static void main(String[] args) {
        String json = "{\n" +
                     "  \"a\":\"Krishna\",\n" +
                     "  \"b\": {\n" +
                     "     \"x\":10\n" +
                     "  }\n" +
                     "}";
        
        System.out.println("Input JSON:");
        System.out.println(json);
        System.out.println("\nGenerated Java Code:");
        System.out.println("===================");
        
        String generatedCode = JsonToObjectGenerator.generateObjectCode(A.class, json);
        System.out.println(generatedCode);
        
        System.out.println("\nExecuting the generated code logic:");
        System.out.println("===================================");
        
        // Demonstrate the actual execution
        B b = new B();
        b.setX(10);
        A a = new A();
        a.setA("Krishna");
        a.setB(b);
        
        System.out.println("Created object: " + a);
    }
}
