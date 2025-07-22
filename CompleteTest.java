import java.nio.file.Files;
import java.nio.file.Paths;

public class CompleteTest {
    public static void main(String[] args) {
        StringBuilder output = new StringBuilder();
        
        try {
            output.append("=== Complete JsonToObjectGenerator Test Results ===\n\n");
            
            // Test 1: Composite Design Pattern
            output.append("1. Testing Composite Design Pattern (TreeNode):\n");
            output.append("=" .repeat(50)).append("\n");
            String compositeJson = new String(Files.readAllBytes(Paths.get("e:\\Office Rough\\JsonToObjectGenCode\\test_composite.json")));
            String compositeCode = JsonToObjectGenerator.generateObjectCode(TreeNode.class, compositeJson);
            output.append(compositeCode).append("\n\n");
            
            // Test 2: Array/List Handling
            output.append("2. Testing Array/List Handling (Company with Employees):\n");
            output.append("=" .repeat(50)).append("\n");
            String arrayJson = new String(Files.readAllBytes(Paths.get("e:\\Office Rough\\JsonToObjectGenCode\\test_array.json")));
            String arrayCode = JsonToObjectGenerator.generateObjectCode(Company.class, arrayJson);
            output.append(arrayCode).append("\n\n");
            
            // Test 3: Simple Nested Object
            output.append("3. Testing Simple Nested Object (A with B):\n");
            output.append("=" .repeat(50)).append("\n");
            String simpleJson = new String(Files.readAllBytes(Paths.get("e:\\Office Rough\\JsonToObjectGenCode\\sample_data.json")));
            String simpleCode = JsonToObjectGenerator.generateObjectCode(A.class, simpleJson);
            output.append(simpleCode).append("\n\n");
            
            // Write all results to file
            Files.write(Paths.get("e:\\Office Rough\\JsonToObjectGenCode\\complete_test_results.txt"), 
                        output.toString().getBytes());
            
            System.out.println("Complete test results written to complete_test_results.txt");
            System.out.println("All tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
