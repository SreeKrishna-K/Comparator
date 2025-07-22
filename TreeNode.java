public class TreeNode {
    private String name;
    private TreeNode leftChild;
    private TreeNode rightChild;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public TreeNode getLeftChild() {
        return leftChild;
    }
    
    public void setLeftChild(TreeNode leftChild) {
        this.leftChild = leftChild;
    }
    
    public TreeNode getRightChild() {
        return rightChild;
    }
    
    public void setRightChild(TreeNode rightChild) {
        this.rightChild = rightChild;
    }
    
    @Override
    public String toString() {
        return "TreeNode{name='" + name + "', leftChild=" + leftChild + ", rightChild=" + rightChild + "}";
    }
}
