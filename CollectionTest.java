import java.nio.file.Files;
import java.nio.file.Paths;

public class CollectionTest {
    public static void main(String[] args) {
        try {
            // Load the test JSON file with various collection types
            String filePath = "e:\\Office Rough\\JsonToObjectGenCode\\test_collections.json";
            String json = new String(Files.readAllBytes(Paths.get(filePath)));
            
            System.out.println("=== Testing All Collection Types ===\n");
            System.out.println("JSON Content:");
            System.out.println(json);
            System.out.println("\n" + "=".repeat(60));
            
            // Generate object code using our enhanced generator
            String code = JsonToObjectGenerator.generateObjectCode(Department.class, json);
            
            System.out.println("Generated Object Creation Code:");
            System.out.println("=".repeat(60));
            System.out.println(code);
            
            // Also write to file for easier viewing
            Files.write(Paths.get("e:\\Office Rough\\JsonToObjectGenCode\\collection_test_results.txt"), 
                        code.getBytes());
            System.out.println("\nResults also written to collection_test_results.txt");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
