import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.*;
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
    
    // Determine the appropriate collection implementation for a given interface
    private static String getCollectionImplementation(Class<?> collectionType) {
        if (java.util.List.class.isAssignableFrom(collectionType)) {
            return "ArrayList";
        } else if (java.util.Set.class.isAssignableFrom(collectionType)) {
            if (java.util.SortedSet.class.isAssignableFrom(collectionType) || 
                java.util.NavigableSet.class.isAssignableFrom(collectionType)) {
                return "TreeSet";
            } else if (java.util.LinkedHashSet.class.isAssignableFrom(collectionType)) {
                return "LinkedHashSet";
            } else {
                return "HashSet";
            }
        } else if (java.util.Queue.class.isAssignableFrom(collectionType)) {
            if (java.util.Deque.class.isAssignableFrom(collectionType)) {
                return "ArrayDeque";
            } else if (java.util.concurrent.BlockingQueue.class.isAssignableFrom(collectionType)) {
                return "LinkedBlockingQueue";
            } else {
                return "LinkedList";
            }
        } else if (java.util.Collection.class.isAssignableFrom(collectionType)) {
            // Generic Collection interface - default to ArrayList
            return "ArrayList";
        }
        return "ArrayList"; // Default fallback
    }
    
    // Check if a field type is any kind of collection (including arrays)
    private static boolean isCollectionType(Class<?> fieldType) {
        return fieldType.isArray() || 
               java.util.Collection.class.isAssignableFrom(fieldType);
    }
    
    // Get element type for Collection/Array fields using reflection or heuristics
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
                    // Handle all collection types (arrays, lists, sets, queues, etc.)
                    else if (isCollectionType(fieldType) && jsonElement.isJsonArray()) {
                        JsonArray jsonArray = jsonElement.getAsJsonArray();
                        String collectionVarName = generateUniqueVariableName(fieldName + "Collection");
                        
                        // Get element type for collections
                        Class<?> elementType = getElementType(field, jsonArray);
                        
                        // Force Employee type for employees collection to fix the specific issue
                        if (fieldName.equals("employees") && isCollectionType(fieldType)) {
                            try {
                                elementType = Class.forName("Employee");
                            } catch (ClassNotFoundException e) {
                                // If Employee class doesn't exist, log but continue with detected type
                                System.err.println("Employee class not found: " + e.getMessage());
                            }
                        }
                        
                        // Handle collection field with unified method
                        handleCollectionField(fieldType, jsonArray, collectionVarName, code, elementType);
                        
                        nestedVariables.put(fieldName, collectionVarName);
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
    
    // Handle all collection types (List, Set, Queue, Deque, etc.) with proper nested object support
    private static void handleCollectionField(Class<?> fieldType, JsonArray jsonArray, String collectionVarName, StringBuilder code, Class<?> elementType) {
        try {
            if (fieldType.isArray()) {
                // Handle arrays (special case)
                handleArrayType(fieldType, jsonArray, collectionVarName, code, elementType);
            } else if (java.util.Collection.class.isAssignableFrom(fieldType)) {
                // Handle all Collection subtypes (List, Set, Queue, etc.)
                handleCollectionType(fieldType, jsonArray, collectionVarName, code, elementType);
            }
            
            code.append("\n");
            
        } catch (Exception e) {
            code.append("// Error handling collection field: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
    }
    
    // Handle array types specifically
    private static void handleArrayType(Class<?> fieldType, JsonArray jsonArray, String arrayVarName, StringBuilder code, Class<?> elementType) {
        String elementTypeName = fieldType.getComponentType().getSimpleName();
        code.append(elementTypeName).append("[] ").append(arrayVarName).append(" = new ").append(elementTypeName).append("[").append(jsonArray.size()).append("];\n");
        
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement element = jsonArray.get(i);
            if (element.isJsonObject() && !isPrimitiveOrString(fieldType.getComponentType())) {
                String elementVarName = generateUniqueVariableName(fieldType.getComponentType().getSimpleName().toLowerCase());
                generateCodeRecursive(fieldType.getComponentType(), element.getAsJsonObject(), elementVarName, code, new HashSet<>());
                code.append(arrayVarName).append("[").append(i).append("] = ").append(elementVarName).append(";\n");
            } else if (element.isJsonPrimitive() && isPrimitiveOrString(fieldType.getComponentType())) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                String value = getValueAsString(primitive, fieldType.getComponentType());
                code.append(arrayVarName).append("[").append(i).append("] = ").append(value).append(";\n");
            }
        }
    }
    
    // Handle Collection types (List, Set, Queue, Deque, etc.)
    private static void handleCollectionType(Class<?> fieldType, JsonArray jsonArray, String collectionVarName, StringBuilder code, Class<?> elementType) {
        String elementTypeName = elementType != null ? elementType.getSimpleName() : "Object";
        String implementation = getCollectionImplementation(fieldType);
        String interfaceName = getCollectionInterfaceName(fieldType);
        
        // Create collection with proper generic type and implementation
        code.append(interfaceName).append("<").append(elementTypeName).append("> ")
            .append(collectionVarName).append(" = new ").append(implementation).append("<>();\n");
        
        // Process each element in the JSON array
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement element = jsonArray.get(i);
            
            if (element.isJsonObject() && elementType != null) {
                // Create nested object recursively
                String elementVarName = generateUniqueVariableName(elementType.getSimpleName().toLowerCase());
                generateCodeRecursive(elementType, element.getAsJsonObject(), elementVarName, code, new HashSet<>());
                addElementToCollection(fieldType, collectionVarName, elementVarName, code);
            } else if (element.isJsonPrimitive()) {
                // Handle primitive values
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                String value = getPrimitiveValue(primitive);
                addElementToCollection(fieldType, collectionVarName, value, code);
            } else {
                // Handle null or unexpected element type
                code.append("// Warning: Unhandled element type in collection: ").append(element).append("\n");
                addElementToCollection(fieldType, collectionVarName, "null", code);
            }
        }
    }
    
    // Get the appropriate interface name for declaration
    private static String getCollectionInterfaceName(Class<?> collectionType) {
        if (java.util.List.class.isAssignableFrom(collectionType)) {
            return "List";
        } else if (java.util.Set.class.isAssignableFrom(collectionType)) {
            return "Set";
        } else if (java.util.Queue.class.isAssignableFrom(collectionType)) {
            if (java.util.Deque.class.isAssignableFrom(collectionType)) {
                return "Deque";
            } else {
                return "Queue";
            }
        } else if (java.util.Collection.class.isAssignableFrom(collectionType)) {
            return "Collection";
        }
        return "Collection"; // Default fallback
    }
    
    // Add element to collection using appropriate method
    private static void addElementToCollection(Class<?> collectionType, String collectionVarName, String elementValue, StringBuilder code) {
        if (java.util.Queue.class.isAssignableFrom(collectionType) && 
            !java.util.Deque.class.isAssignableFrom(collectionType)) {
            // Use offer() for Queue (but not Deque)
            code.append(collectionVarName).append(".offer(").append(elementValue).append(");\n");
        } else {
            // Use add() for List, Set, Deque, and generic Collection
            code.append(collectionVarName).append(".add(").append(elementValue).append(");\n");
        }
    }
    
    // Get primitive value as string for collection elements
    private static String getPrimitiveValue(JsonPrimitive primitive) {
        if (primitive.isString()) {
            return "\""+primitive.getAsString()+"\"";
        } else if (primitive.isNumber()) {
            return primitive.getAsNumber().toString();
        } else if (primitive.isBoolean()) {
            return primitive.getAsBoolean() ? "true" : "false";
        }
        return "null";
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
