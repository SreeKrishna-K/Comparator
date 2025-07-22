public class SimpleTest {
    public static void main(String[] args) {
        String json = "{\n" +
                     "  \"a\":\"Krishna\",\n" +
                     "  \"b\": {\n" +
                     "     \"x\":10\n" +
                     "  }\n" +
                     "}";
        
        System.out.println("Generated Java Code:");
        System.out.println("===================");
        
        String code = JsonToObjectGenerator.generateObjectCode(A.class, json);
        System.out.println(code);
    }
}
