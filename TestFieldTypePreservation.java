import java.util.*;

public class TestFieldTypePreservation {
    
    public static void main(String[] args) {
        // Test JSON with collections and strings containing special characters
        String json = "{\n" +
                "  \"employees\": [\n" +
                "    {\"name\": \"Alice\\nDoe\", \"age\": 28},\n" +
                "    {\"name\": \"Bob\\\"Smith\", \"age\": 32}\n" +
                "  ],\n" +
                "  \"skills\": [\"Java\", \"Python\\tAdvanced\", \"JavaScript\"],\n" +
                "  \"tasks\": [\"Code Review\", \"Bug\\\\Fixing\", \"Testing\"],\n" +
                "  \"priorities\": [\"High\", \"Medium\", \"Low\"]\n" +
                "}";
        
        System.out.println("Testing Field Type Preservation and String Escaping:");
        System.out.println("=" .repeat(60));
        
        String generatedCode = JsonToObjectGenerator.generateObjectCode(TestDepartment.class, json);
        System.out.println(generatedCode);
    }
}

class TestDepartment {
    private List<TestEmployee> employees;      // Should generate List<TestEmployee>, not Collection<TestEmployee>
    private Set<String> skills;               // Should generate Set<String>
    private Collection<String> tasks;         // Should generate Collection<String>
    private Deque<String> priorities;         // Should generate Deque<String>
    
    // Getters and setters
    public List<TestEmployee> getEmployees() { return employees; }
    public void setEmployees(List<TestEmployee> employees) { this.employees = employees; }
    
    public Set<String> getSkills() { return skills; }
    public void setSkills(Set<String> skills) { this.skills = skills; }
    
    public Collection<String> getTasks() { return tasks; }
    public void setTasks(Collection<String> tasks) { this.tasks = tasks; }
    
    public Deque<String> getPriorities() { return priorities; }
    public void setPriorities(Deque<String> priorities) { this.priorities = priorities; }
}

class TestEmployee {
    private String name;
    private int age;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}
