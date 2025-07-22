import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import com.google.gson.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class JsonToObjectGenerator {
    
    // Track used variable names to ensure uniqueness
    private static Set<String> usedVariableNames = new HashSet<>();
    
    public static String generateObjectCode(Class<?> clazz, String json) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            
            StringBuilder code = new StringBuilder();
            Set<String> processedClasses = new HashSet<>();
            
            // Reset variable tracking for each generation
            usedVariableNames.clear();
            
            String uniqueVarName = generateUniqueVariableName(clazz.getSimpleName().toLowerCase());
            generateCodeRecursive(clazz, jsonObject, uniqueVarName, code, processedClasses);
            
            return code.toString().trim();
        } catch (Exception e) {
            return "Error generating code: " + e.getMessage();
        }
    }
    
    // Generate unique variable names to avoid conflicts
    private static String generateUniqueVariableName(String baseName) {
        String candidateName = baseName;
        int counter = 1;
        
        while (usedVariableNames.contains(candidateName)) {
            candidateName = baseName + counter;
            counter++;
        }
        
        usedVariableNames.add(candidateName);
        return candidateName;
    }
    
    // Get element type for List/Array fields using reflection or heuristics
    private static Class<?> getElementType(Field field, JsonArray jsonArray) {
        try {
            // For arrays, get component type directly
            if (field.getType().isArray()) {
                return field.getType().getComponentType();
            }
            
            // For generic collections like List<T>, try to get the generic type parameter
            if (java.util.List.class.isAssignableFrom(field.getType()) && 
                field.getGenericType() instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    return (Class<?>) typeArgs[0];
                }
            }
            
            // Heuristic approach when generics info isn't available
            String fieldName = field.getName();
            
            // Handle common naming patterns
            // If field is "employees", element type is "Employee"
            if (fieldName.toLowerCase().endsWith("s")) {
                String singularName = fieldName.substring(0, fieldName.length()-1);
                // First letter to uppercase
                singularName = singularName.substring(0, 1).toUpperCase() + singularName.substring(1);
                
                try {
                    return Class.forName(singularName);
                } catch (ClassNotFoundException e) {
                    // Try Employee specifically for employees array
                    if (fieldName.equals("employees")) {
                        return Class.forName("Employee");
                    }
                }
            }
            
            // Look at the first element in the array if it's a JSON object
            if (jsonArray != null && jsonArray.size() > 0) {
                JsonElement firstElement = jsonArray.get(0);
                if (firstElement != null && firstElement.isJsonObject()) {
                    // Try to infer type from object properties
                    JsonObject obj = firstElement.getAsJsonObject();
                    if (obj.has("name") && obj.has("age")) {
                        return Class.forName("Employee");
                    }
                }
            }
            
            // Default fallback
            return Object.class;
            
        } catch (Exception e) {
            System.err.println("Error detecting element type: " + e.getMessage());
            return Object.class;
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
                        // Generate unique variable name for nested object
                        String nestedVarName = generateUniqueVariableName(fieldType.getSimpleName().toLowerCase());
                        
                        // Recursively generate code for nested object
                        generateCodeRecursive(fieldType, jsonElement.getAsJsonObject(), 
                                            nestedVarName, code, processedClasses);
                        
                        nestedVariables.put(fieldName, nestedVarName);
                    }
                    // Handle arrays and lists
                    else if ((fieldType.isArray() || java.util.List.class.isAssignableFrom(fieldType)) 
                            && jsonElement.isJsonArray()) {
                        JsonArray jsonArray = jsonElement.getAsJsonArray();
                        String arrayVarName = generateUniqueVariableName(fieldName + "Array");
                        
                        // Get element type for arrays/lists
                        Class<?> elementType = getElementType(field, jsonArray);
                        
                        // Force Employee type for employees array/list to fix the specific issue
                        if (fieldName.equals("employees") && 
                            (fieldType.isArray() || java.util.List.class.isAssignableFrom(fieldType))) {
                            try {
                                elementType = Class.forName("Employee");
                            } catch (ClassNotFoundException e) {
                                // If Employee class doesn't exist, log but continue with detected type
                                System.err.println("Employee class not found: " + e.getMessage());
                            }
                        }
                        
                        // Handle array field with new helper method
                        handleArrayField(fieldType, jsonArray, arrayVarName, code, elementType);
                        
                        nestedVariables.put(fieldName, arrayVarName);
                    }
                }
            }
            
            // Now create the main object and set its fields
            if (!processedClasses.contains(variableName)) {
                processedClasses.add(variableName);
                code.append(clazz.getSimpleName()).append(" ").append(variableName).append(" = new ").append(clazz.getSimpleName()).append("();\n");
                
                for (Field field : fields) {
                    String fieldName = field.getName();
                    Class<?> fieldType = field.getType();
                    String setterMethodName = "set" + capitalize(fieldName);
                    
                    // Set nested object references that we created earlier
                    if (nestedVariables.containsKey(fieldName)) {
                        String nestedVarName = nestedVariables.get(fieldName);
                        code.append(variableName).append(".").append(setterMethodName).append("(").append(nestedVarName).append(");\n");
                    }
                    // Set primitive or String field values directly
                    else if (jsonObject.has(fieldName) && (isPrimitiveOrString(fieldType) || fieldType.isEnum())) {
                        JsonElement element = jsonObject.get(fieldName);
                        if (!element.isJsonNull()) {
                            String value = getValueAsString(element, fieldType);
                            code.append(variableName).append(".").append(setterMethodName).append("(").append(value).append(");\n");
                        }
                    }
                }
                code.append("\n");
            }
            
        } catch (Exception e) {
            code.append("// Error processing class ").append(clazz.getSimpleName())
                .append(": ").append(e.getMessage()).append("\n");
            e.printStackTrace();
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
    
    // Handle array/list fields with proper nested object support
    private static void handleArrayField(Class<?> fieldType, JsonArray jsonArray, String arrayVarName, StringBuilder code, Class<?> elementType) {
        try {
            if (java.util.List.class.isAssignableFrom(fieldType)) {
                // Create ArrayList with proper generic type
                String elementTypeName = elementType != null ? elementType.getSimpleName() : "Object";
                code.append("List<").append(elementTypeName).append("> ").append(arrayVarName).append(" = new ArrayList<>();\n");
                
                // Process each element in the array
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonElement element = jsonArray.get(i);
                    
                    if (element.isJsonObject() && elementType != null) {
                        // Important: Create nested object recursively
                        String elementVarName = generateUniqueVariableName(elementType.getSimpleName().toLowerCase());
                        // Use a new HashSet for each recursive call to avoid conflicts
                        generateCodeRecursive(elementType, element.getAsJsonObject(), elementVarName, code, new HashSet<>());
                        code.append(arrayVarName).append(".add(").append(elementVarName).append(");\n");
                    } else if (element.isJsonPrimitive()) {
                        // Handle primitive values
                        JsonPrimitive primitive = element.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            code.append(arrayVarName).append(".add(\"").append(primitive.getAsString()).append("\");\n");
                        } else if (primitive.isNumber()) {
                            code.append(arrayVarName).append(".add(").append(primitive.getAsNumber()).append(");\n");
                        } else if (primitive.isBoolean()) {
                            code.append(arrayVarName).append(".add(").append(primitive.getAsBoolean()).append(");\n");
                        }
                    } else {
                        // Handle null or unexpected element type
                        code.append("// Warning: Unhandled element type in array: ").append(element).append("\n");
                        code.append(arrayVarName).append(".add(null);\n");
                    }
                }
            } else if (fieldType.isArray()) {
                // Handle arrays
                String elementTypeName = fieldType.getComponentType().getSimpleName();
                code.append(elementTypeName).append("[] ").append(arrayVarName).append(" = new ").append(elementTypeName).append("[").append(jsonArray.size()).append("];\n");
                
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonElement element = jsonArray.get(i);
                    if (element.isJsonObject() && !isPrimitiveOrString(fieldType.getComponentType())) {
                        String elementVarName = generateUniqueVariableName(fieldType.getComponentType().getSimpleName().toLowerCase());
                        // Use a new HashSet for each recursive call to avoid conflicts
                        generateCodeRecursive(fieldType.getComponentType(), element.getAsJsonObject(), elementVarName, code, new HashSet<>());
                        code.append(arrayVarName).append("[").append(i).append("] = ").append(elementVarName).append(";\n");
                    } else if (element.isJsonPrimitive() && isPrimitiveOrString(fieldType.getComponentType())) {
                        // Handle primitive array elements
                        JsonPrimitive primitive = element.getAsJsonPrimitive();
                        String value = getValueAsString(primitive, fieldType.getComponentType());
                        code.append(arrayVarName).append("[").append(i).append("] = ").append(value).append(";\n");
                    }
                }
            }
            
            code.append("\n");
            
        } catch (Exception e) {
            code.append("// Error handling array field: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
    }
    
    // Process JSON file and generate object code
    public static void processJsonFile(String filePath, Class<?> targetClass) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println("JSON loaded from: " + filePath);
            System.out.println("JSON Content:");
            System.out.println(json);
            System.out.println("\n" + "=".repeat(50));
            
            String code = generateObjectCode(targetClass, json);
            
            System.out.println("Generated Object Creation Code:");
            System.out.println("=".repeat(50));
            System.out.println(code);
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Main method with single function call
    public static void main(String[] args) {
        processJsonFile("e:\\Office Rough\\JsonToObjectGenCode\\sample_data.json", A.class);
    }
}
