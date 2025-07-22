# JSON to Object Generator

This Java application generates object initialization code from JSON input and class definitions.

## Features

- Generates Java code to create objects and set all fields using setter methods
- Handles nested objects recursively
- Supports all primitive types and String
- Uses reflection to analyze class structure

## Usage

```java
String json = "{ \"a\":\"Krishna\", \"b\": { \"x\":10 } }";
String code = JsonToObjectGenerator.generateObjectCode(A.class, json);
System.out.println(code);
```

## Output Example

For the input JSON and class A, it generates:
```java
B b = new B();
b.setX(10);
A a = new A();
a.setA("Krishna");
a.setB(b);
```

## Running the Demo

1. Compile: `mvn compile`
2. Run: `mvn exec:java -Dexec.mainClass="Demo"`

## Requirements

- Java 11+
- Maven
- Gson library (included in pom.xml)
