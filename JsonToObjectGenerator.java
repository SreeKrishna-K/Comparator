import java.lang.reflect.Field;
import java.util.*;
import com.google.gson.*;

public class JsonToObjectGenerator {
    
    public static String generateObjectCode(Class<?> clazz, String json) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            
            StringBuilder code = new StringBuilder();
            Set<String> processedClasses = new HashSet<>();
            
            generateCodeRecursive(clazz, jsonObject, clazz.getSimpleName().toLowerCase(), code, processedClasses);
            
            return code.toString().trim();
        } catch (Exception e) {
            return "Error generating code: " + e.getMessage();
        }
    }
    
    private static void generateCodeRecursive(Class<?> clazz, JsonObject jsonObject, 
                                            String variableName, StringBuilder code, 
                                            Set<String> processedClasses) {
        try {
            // First, create nested objects
            Field[] fields = clazz.getDeclaredFields();
            Map<String, String> nestedVariables = new HashMap<>();
            
            for (Field field : fields) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                
                if (jsonObject.has(fieldName)) {
                    JsonElement jsonElement = jsonObject.get(fieldName);
                    
                    // Handle nested objects
                    if (!isPrimitiveOrString(fieldType) && jsonElement.isJsonObject()) {
                        String nestedVarName = fieldName;
                        String nestedClassName = fieldType.getSimpleName();
                        
                        // Create nested object first
                        code.append(nestedClassName).append(" ").append(nestedVarName)
                            .append(" = new ").append(nestedClassName).append("();\n");
                        
                        // Recursively generate code for nested object
                        generateCodeRecursive(fieldType, jsonElement.getAsJsonObject(), 
                                            nestedVarName, code, processedClasses);
                        
                        nestedVariables.put(fieldName, nestedVarName);
                        code.append("\n");
                    }
                }
            }
            
            // Create main object
            String className = clazz.getSimpleName();
            code.append(className).append(" ").append(variableName)
                .append(" = new ").append(className).append("();\n");
            
            // Set primitive/String fields and nested objects
            for (Field field : fields) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                
                if (jsonObject.has(fieldName)) {
                    JsonElement jsonElement = jsonObject.get(fieldName);
                    String setterName = "set" + capitalize(fieldName);
                    
                    if (isPrimitiveOrString(fieldType)) {
                        // Handle primitive types and strings
                        String value = getValueAsString(jsonElement, fieldType);
                        code.append(variableName).append(".").append(setterName)
                            .append("(").append(value).append(");\n");
                    } else if (nestedVariables.containsKey(fieldName)) {
                        // Handle nested objects
                        code.append(variableName).append(".").append(setterName)
                            .append("(").append(nestedVariables.get(fieldName)).append(");\n");
                    }
                }
            }
            
        } catch (Exception e) {
            code.append("// Error processing class ").append(clazz.getSimpleName())
                .append(": ").append(e.getMessage()).append("\n");
        }
    }
    
    private static boolean isPrimitiveOrString(Class<?> type) {
        return type.isPrimitive() || 
               type == String.class || 
               type == Integer.class || 
               type == Long.class || 
               type == Double.class || 
               type == Float.class || 
               type == Boolean.class || 
               type == Character.class ||
               type == Byte.class ||
               type == Short.class;
    }
    
    private static String getValueAsString(JsonElement element, Class<?> type) {
        if (type == String.class) {
            return "\"" + element.getAsString() + "\"";
        } else if (type == int.class || type == Integer.class) {
            return String.valueOf(element.getAsInt());
        } else if (type == long.class || type == Long.class) {
            return element.getAsLong() + "L";
        } else if (type == double.class || type == Double.class) {
            return String.valueOf(element.getAsDouble());
        } else if (type == float.class || type == Float.class) {
            return element.getAsFloat() + "f";
        } else if (type == boolean.class || type == Boolean.class) {
            return String.valueOf(element.getAsBoolean());
        } else if (type == char.class || type == Character.class) {
            return "'" + element.getAsString().charAt(0) + "'";
        } else if (type == byte.class || type == Byte.class) {
            return "(byte)" + element.getAsByte();
        } else if (type == short.class || type == Short.class) {
            return "(short)" + element.getAsShort();
        }
        return element.getAsString();
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    // Test method
    public static void main(String[] args) {
        // Example usage - you would replace these with your actual classes
        try {
            // For demonstration, we'll simulate with the example you provided
            String json = "{\n" +
                         "  \"a\":\"Krishna\",\n" +
                         "  \"b\": {\n" +
                         "     \"x\":10\n" +
                         "  }\n" +
                         "}";
            
            System.out.println("Generated Code:");
            System.out.println("===============");
            
            // You would call it like this with your actual class:
            // String code = generateObjectCode(A.class, json);
            // System.out.println(code);
            
            System.out.println("// To use this generator:");
            System.out.println("// String code = JsonToObjectGenerator.generateObjectCode(YourClass.class, jsonString);");
            System.out.println("// System.out.println(code);");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
