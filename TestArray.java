public class TestArray {
    public static void main(String[] args) {
        System.out.println("Testing Array/List Handling:");
        System.out.println("============================");
        
        try {
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("e:\\Office Rough\\JsonToObjectGenCode\\test_array.json")));
            System.out.println("JSON Content:");
            System.out.println(json);
            System.out.println("\nGenerated Code:");
            System.out.println("===============");
            
            // We'll test with a simpler approach since Company class has List<Employee>
            // For now, let's create a simple test JSON without complex generics
            String simpleArrayJson = "{\"items\":[\"item1\",\"item2\",\"item3\"]}";
            System.out.println("\nSimple Array Test:");
            System.out.println("JSON: " + simpleArrayJson);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
