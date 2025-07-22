import java.nio.file.Files;
import java.nio.file.Paths;

public class FileOutputTest {
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
        
        try {
            String generatedCode = JsonToObjectGenerator.generateObjectCode(Company.class, json);
            
            // Write generated code to a file to avoid console output issues
            Files.write(Paths.get("e:\\Office Rough\\JsonToObjectGenCode\\generated_code.txt"), 
                        generatedCode.getBytes());
            
            System.out.println("Generated code written to generated_code.txt");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
