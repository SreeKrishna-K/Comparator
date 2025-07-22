public class A {
    private String a;
    private B b;
    
    public A() {}
    
    public String getA() {
        return a;
    }
    
    public void setA(String a) {
        this.a = a;
    }
    
    public B getB() {
        return b;
    }
    
    public void setB(B b) {
        this.b = b;
    }
    
    @Override
    public String toString() {
        return "A{a='" + a + "', b=" + b + "}";
    }
}
