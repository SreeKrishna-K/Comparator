public class TestComposite {
    public static void main(String[] args) {
        System.out.println("Testing Composite Design Pattern (TreeNode):");
        System.out.println("============================================");
        
        // Test the composite pattern with TreeNode
        try {
            String result = JsonToObjectGenerator.generateObjectCode(TreeNode.class, 
                new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("e:\\Office Rough\\JsonToObjectGenCode\\test_composite.json"))));
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
