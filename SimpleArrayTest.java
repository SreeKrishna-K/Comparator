public class SimpleArrayTest {
    public static void main(String[] args) {
        String json = "{\n" +
                      "  \"name\": \"Acme Corp\",\n" +
                      "  \"employees\": [\n" +
                      "    {\n" +
                      "      \"name\": \"John\",\n" +
                      "      \"age\": 30\n" +
                      "    },\n" +
                      "    {\n" +
                      "      \"name\": \"Jane\",\n" +
                      "      \"age\": 25\n" +
                      "    }\n" +
                      "  ]\n" +
                      "}";
        
        System.out.println("=== Testing Employee Array Handling ===\n");
        System.out.println("Test JSON:");
        System.out.println(json);
        System.out.println("\n" + "=".repeat(50));
        
        try {
            String generatedCode = JsonToObjectGenerator.generateObjectCode(Company.class, json);
            
            System.out.println("Generated Object Creation Code:");
            System.out.println("=".repeat(50));
            System.out.println(generatedCode);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
