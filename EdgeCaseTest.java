public class EdgeCaseTest {
    public static void main(String[] args) {
        System.out.println("=== Testing Edge Cases ===\n");
        
        // Test 1: Composite Design Pattern (TreeNode with same type nested objects)
        System.out.println("1. Testing Composite Design Pattern:");
        System.out.println("=====================================");
        JsonToObjectGenerator.processJsonFile("e:\\Office Rough\\JsonToObjectGenCode\\test_composite.json", TreeNode.class);
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test 2: Array/List handling
        System.out.println("2. Testing Array/List Handling:");
        System.out.println("===============================");
        // Note: This will show TODO comments for now since full array support needs more implementation
        JsonToObjectGenerator.processJsonFile("e:\\Office Rough\\JsonToObjectGenCode\\test_array.json", Company.class);
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test 3: Original simple case to verify it still works
        System.out.println("3. Testing Original Simple Case:");
        System.out.println("================================");
        JsonToObjectGenerator.processJsonFile("e:\\Office Rough\\JsonToObjectGenCode\\sample_data.json", A.class);
    }
}
