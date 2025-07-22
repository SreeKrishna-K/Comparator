import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class TestEmployeeArray {
    public static void main(String[] args) {
        try {
            // Load the test JSON file with Company and Employee objects
            String filePath = "e:\\Office Rough\\JsonToObjectGenCode\\test_array.json";
            String json = new String(Files.readAllBytes(Paths.get(filePath)));
            
            System.out.println("=== Testing Array/List Handling with Employee Objects ===\n");
            System.out.println("JSON Content:");
            System.out.println(json);
            System.out.println("\n=======================================");
            
            // Generate object code using our enhanced generator
            String code = JsonToObjectGenerator.generateObjectCode(Company.class, json);
            
            System.out.println("Generated Object Creation Code:");
            System.out.println("=======================================");
            System.out.println(code);
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
