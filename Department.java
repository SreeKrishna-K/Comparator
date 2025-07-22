import java.util.*;

public class Department {
    private String name;
    private List<Employee> employees;           // List test
    private Set<String> skills;               // Set test
    private Queue<String> tasks;              // Queue test
    private Deque<String> priorities;         // Deque test
    private Collection<String> resources;     // Generic Collection test
    
    // Constructors
    public Department() {}
    
    public Department(String name) {
        this.name = name;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<Employee> getEmployees() {
        return employees;
    }
    
    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }
    
    public Set<String> getSkills() {
        return skills;
    }
    
    public void setSkills(Set<String> skills) {
        this.skills = skills;
    }
    
    public Queue<String> getTasks() {
        return tasks;
    }
    
    public void setTasks(Queue<String> tasks) {
        this.tasks = tasks;
    }
    
    public Deque<String> getPriorities() {
        return priorities;
    }
    
    public void setPriorities(Deque<String> priorities) {
        this.priorities = priorities;
    }
    
    public Collection<String> getResources() {
        return resources;
    }
    
    public void setResources(Collection<String> resources) {
        this.resources = resources;
    }
    
    @Override
    public String toString() {
        return "Department{" +
                "name='" + name + '\'' +
                ", employees=" + employees +
                ", skills=" + skills +
                ", tasks=" + tasks +
                ", priorities=" + priorities +
                ", resources=" + resources +
                '}';
    }
}
