import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * Enhanced JSON to Java Object Code Generator
 * 
 * Generates Java object creation code from JSON data with support for:
 * - Nested objects and composite patterns
 * - All Java Collection types (List, Set, Queue, Deque, etc.)
 * - Arrays and primitive types
 * - Recursive data structures
 * - Unique variable naming
 */
public class JsonToObjectGenerator {
    
    // ========================================================================================
    // CONSTANTS AND CONFIGURATION
    // ========================================================================================
    
    private static final String DEFAULT_ELEMENT_TYPE = "Object";
    private static final String ARRAY_SUFFIX = "Collection";
    private static final Map<Class<?>, String> COLLECTION_IMPLEMENTATIONS = initCollectionImplementations();
    private static final Map<Class<?>, String> COLLECTION_INTERFACES = initCollectionInterfaces();
    
    // ========================================================================================
    // STATE MANAGEMENT
    // ========================================================================================
    
    private static Set<String> usedVariableNames = new HashSet<>();
    
    // ========================================================================================
    // PUBLIC API
    // ========================================================================================
    
    /**
     * Main entry point for generating Java object creation code from JSON
     * 
     * @param clazz Target class to generate code for
     * @param json JSON string to parse
     * @return Generated Java code as string
     */
    public static String generateObjectCode(Class<?> clazz, String json) {
        try {
            JsonObject jsonObject = parseJson(json);
            StringBuilder code = new StringBuilder();
            
            resetState();
            String variableName = VariableNameManager.generateUnique(clazz.getSimpleName().toLowerCase());
            
            CodeGenerator.generateRecursive(clazz, jsonObject, variableName, code, new HashSet<>());
            
            return code.toString().trim();
        } catch (Exception e) {
            return "Error generating code: " + e.getMessage();
        }
    }
    
    /**
     * Process JSON file and generate object code with console output
     */
    public static void processJsonFile(String filePath, Class<?> targetClass) {
        try {
            String json = FileUtils.readJsonFile(filePath);
            ConsoleUtils.printJsonContent(filePath, json);
            
            String code = generateObjectCode(targetClass, json);
            ConsoleUtils.printGeneratedCode(code);
            
        } catch (Exception e) {
            System.err.println("Error processing JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ========================================================================================
    // INITIALIZATION AND CONFIGURATION
    // ========================================================================================
    
    private static Map<Class<?>, String> initCollectionImplementations() {
        Map<Class<?>, String> implementations = new HashMap<>();
        implementations.put(List.class, "ArrayList");
        implementations.put(Set.class, "HashSet");
        implementations.put(SortedSet.class, "TreeSet");
        implementations.put(NavigableSet.class, "TreeSet");
        implementations.put(LinkedHashSet.class, "LinkedHashSet");
        implementations.put(Queue.class, "LinkedList");
        implementations.put(Deque.class, "ArrayDeque");
        implementations.put(BlockingQueue.class, "LinkedBlockingQueue");
        implementations.put(Collection.class, "ArrayList");
        return implementations;
    }
    
    private static Map<Class<?>, String> initCollectionInterfaces() {
        Map<Class<?>, String> interfaces = new HashMap<>();
        interfaces.put(List.class, "List");
        interfaces.put(Set.class, "Set");
        interfaces.put(Queue.class, "Queue");
        interfaces.put(Deque.class, "Deque");
        interfaces.put(Collection.class, "Collection");
        return interfaces;
    }
    
    private static JsonObject parseJson(String json) {
        return new Gson().fromJson(json, JsonObject.class);
    }
    
    private static void resetState() {
        usedVariableNames.clear();
    }
    
    // ========================================================================================
    // VARIABLE NAME MANAGEMENT
    // ========================================================================================
    
    private static class VariableNameManager {
        
        public static String generateUnique(String baseName) {
            String candidateName = baseName;
            int counter = 1;
            
            while (usedVariableNames.contains(candidateName)) {
                candidateName = baseName + counter;
                counter++;
            }
            
            usedVariableNames.add(candidateName);
            return candidateName;
        }
    }
    
    // ========================================================================================
    // TYPE DETECTION AND ANALYSIS
    // ========================================================================================
    
    private static class TypeAnalyzer {
        
        public static boolean isCollectionType(Class<?> fieldType) {
            return fieldType.isArray() || Collection.class.isAssignableFrom(fieldType);
        }
        
        public static boolean isPrimitiveOrString(Class<?> type) {
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
        
        public static Class<?> getElementType(Field field, JsonArray jsonArray) {
            try {
                // Arrays: get component type directly
                if (field.getType().isArray()) {
                    return field.getType().getComponentType();
                }
                
                // Generic collections: use reflection
                Class<?> typeFromReflection = extractGenericType(field);
                if (typeFromReflection != null) {
                    return typeFromReflection;
                }
                
                // Heuristic approaches
                Class<?> typeFromNaming = inferTypeFromFieldName(field.getName());
                if (typeFromNaming != null) {
                    return typeFromNaming;
                }
                
                Class<?> typeFromJson = inferTypeFromJsonContent(jsonArray);
                if (typeFromJson != null) {
                    return typeFromJson;
                }
                
                return Object.class;
                
            } catch (Exception e) {
                System.err.println("Error detecting element type: " + e.getMessage());
                return Object.class;
            }
        }
        
        private static Class<?> extractGenericType(Field field) {
            if (Collection.class.isAssignableFrom(field.getType()) && 
                field.getGenericType() instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    return (Class<?>) typeArgs[0];
                }
            }
            return null;
        }
        
        private static Class<?> inferTypeFromFieldName(String fieldName) {
            try {
                if (fieldName.toLowerCase().endsWith("s")) {
                    String singularName = fieldName.substring(0, fieldName.length()-1);
                    singularName = StringUtils.capitalize(singularName);
                    return Class.forName(singularName);
                }
                
                if (fieldName.equals("employees")) {
                    return Class.forName("Employee");
                }
            } catch (ClassNotFoundException e) {
                // Ignore and try other methods
            }
            return null;
        }
        
        private static Class<?> inferTypeFromJsonContent(JsonArray jsonArray) {
            try {
                if (jsonArray != null && jsonArray.size() > 0) {
                    JsonElement firstElement = jsonArray.get(0);
                    if (firstElement != null && firstElement.isJsonObject()) {
                        JsonObject obj = firstElement.getAsJsonObject();
                        if (obj.has("name") && obj.has("age")) {
                            return Class.forName("Employee");
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                // Ignore and return null
            }
            return null;
        }
    }
    
    // ========================================================================================
    // COLLECTION HANDLING
    // ========================================================================================
    
    private static class CollectionHandler {
        
        public static String getImplementation(Class<?> collectionType) {
            // Check specific types first
            for (Map.Entry<Class<?>, String> entry : COLLECTION_IMPLEMENTATIONS.entrySet()) {
                if (entry.getKey().isAssignableFrom(collectionType)) {
                    return entry.getValue();
                }
            }
            return "ArrayList"; // Default fallback
        }
        
        public static String getInterfaceName(Class<?> collectionType) {
            // Handle Deque specially (extends Queue)
            if (Deque.class.isAssignableFrom(collectionType)) {
                return "Deque";
            }
            
            // Check other interfaces
            for (Map.Entry<Class<?>, String> entry : COLLECTION_INTERFACES.entrySet()) {
                if (entry.getKey().isAssignableFrom(collectionType)) {
                    return entry.getValue();
                }
            }
            return "Collection"; // Default fallback
        }
        
        public static void addElement(Class<?> collectionType, String collectionVarName, 
                                    String elementValue, StringBuilder code) {
            if (Queue.class.isAssignableFrom(collectionType) && 
                !Deque.class.isAssignableFrom(collectionType)) {
                // Use offer() for Queue (but not Deque)
                code.append(collectionVarName).append(".offer(").append(elementValue).append(");\n");
            } else {
                // Use add() for List, Set, Deque, and generic Collection
                code.append(collectionVarName).append(".add(").append(elementValue).append(");\n");
            }
        }
    }
    
    // ========================================================================================
    // CODE GENERATION ENGINE
    // ========================================================================================
    
    private static class CodeGenerator {
        
        public static void generateRecursive(Class<?> clazz, JsonObject jsonObject, 
                                           String variableName, StringBuilder code, 
                                           Set<String> processedClasses) {
            try {
                Field[] fields = clazz.getDeclaredFields();
                Map<String, String> nestedVariables = new HashMap<>();
                
                // Phase 1: Generate nested objects and collections
                generateNestedElements(clazz, jsonObject, fields, code, nestedVariables);
                
                // Phase 2: Create main object and set fields
                createMainObject(clazz, jsonObject, variableName, fields, code, 
                               nestedVariables, processedClasses);
                
            } catch (Exception e) {
                code.append("// Error processing class ").append(clazz.getSimpleName())
                    .append(": ").append(e.getMessage()).append("\n");
                e.printStackTrace();
            }
        }
        
        private static void generateNestedElements(Class<?> clazz, JsonObject jsonObject, 
                                                 Field[] fields, StringBuilder code, 
                                                 Map<String, String> nestedVariables) {
            for (Field field : fields) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                
                if (!jsonObject.has(fieldName)) continue;
                
                JsonElement jsonElement = jsonObject.get(fieldName);
                
                if (handleNestedObject(field, fieldType, jsonElement, code, nestedVariables) ||
                    handleCollection(field, fieldType, jsonElement, code, nestedVariables)) {
                    // Element handled
                }
            }
        }
        
        private static boolean handleNestedObject(Field field, Class<?> fieldType, 
                                                JsonElement jsonElement, StringBuilder code, 
                                                Map<String, String> nestedVariables) {
            if (!TypeAnalyzer.isPrimitiveOrString(fieldType) && jsonElement.isJsonObject()) {
                String nestedVarName = VariableNameManager.generateUnique(
                    fieldType.getSimpleName().toLowerCase());
                
                generateRecursive(fieldType, jsonElement.getAsJsonObject(), 
                                nestedVarName, code, new HashSet<>());
                
                nestedVariables.put(field.getName(), nestedVarName);
                return true;
            }
            return false;
        }
        
        private static boolean handleCollection(Field field, Class<?> fieldType, 
                                              JsonElement jsonElement, StringBuilder code, 
                                              Map<String, String> nestedVariables) {
            if (TypeAnalyzer.isCollectionType(fieldType) && jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                String collectionVarName = VariableNameManager.generateUnique(
                    field.getName() + ARRAY_SUFFIX);
                
                Class<?> elementType = TypeAnalyzer.getElementType(field, jsonArray);
                elementType = handleSpecialCases(field.getName(), fieldType, elementType);
                
                CollectionCodeGenerator.generate(fieldType, jsonArray, collectionVarName, 
                                               code, elementType, field);
                
                nestedVariables.put(field.getName(), collectionVarName);
                return true;
            }
            return false;
        }
        
        private static Class<?> handleSpecialCases(String fieldName, Class<?> fieldType, 
                                                 Class<?> elementType) {
            // Handle specific cases like "employees" field
            if (fieldName.equals("employees") && TypeAnalyzer.isCollectionType(fieldType)) {
                try {
                    return Class.forName("Employee");
                } catch (ClassNotFoundException e) {
                    System.err.println("Employee class not found: " + e.getMessage());
                }
            }
            return elementType;
        }
        
        private static void createMainObject(Class<?> clazz, JsonObject jsonObject, 
                                           String variableName, Field[] fields, 
                                           StringBuilder code, Map<String, String> nestedVariables, 
                                           Set<String> processedClasses) {
            if (processedClasses.contains(variableName)) return;
            
            processedClasses.add(variableName);
            
            // Create object instance
            code.append(clazz.getSimpleName()).append(" ").append(variableName)
                .append(" = new ").append(clazz.getSimpleName()).append("();\n");
            
            // Set all fields
            for (Field field : fields) {
                setFieldValue(field, jsonObject, variableName, code, nestedVariables);
            }
            
            code.append("\n");
        }
        
        private static void setFieldValue(Field field, JsonObject jsonObject, 
                                        String variableName, StringBuilder code, 
                                        Map<String, String> nestedVariables) {
            String fieldName = field.getName();
            String setterName = "set" + StringUtils.capitalize(fieldName);
            
            // Set nested object or collection references
            if (nestedVariables.containsKey(fieldName)) {
                String nestedVarName = nestedVariables.get(fieldName);
                code.append(variableName).append(".").append(setterName)
                    .append("(").append(nestedVarName).append(");\n");
            }
            // Set primitive or String field values
            else if (jsonObject.has(fieldName) && 
                     (TypeAnalyzer.isPrimitiveOrString(field.getType()) || field.getType().isEnum())) {
                JsonElement element = jsonObject.get(fieldName);
                if (!element.isJsonNull()) {
                    String value = ValueConverter.getValueAsString(element, field.getType());
                    code.append(variableName).append(".").append(setterName)
                        .append("(").append(value).append(");\n");
                }
            }
        }
    }
    
    // ========================================================================================
    // COLLECTION CODE GENERATION
    // ========================================================================================
    
    private static class CollectionCodeGenerator {
        
        public static void generate(Class<?> fieldType, JsonArray jsonArray, 
                                  String collectionVarName, StringBuilder code, 
                                  Class<?> elementType, Field originalField) {
            try {
                if (fieldType.isArray()) {
                    generateArrayCode(fieldType, jsonArray, collectionVarName, code, elementType);
                } else if (Collection.class.isAssignableFrom(fieldType)) {
                    generateCollectionCode(fieldType, jsonArray, collectionVarName, code, elementType, originalField);
                }
                code.append("\n");
            } catch (Exception e) {
                code.append("// Error handling collection field: ").append(e.getMessage()).append("\n");
                e.printStackTrace();
            }
        }
        
        private static void generateArrayCode(Class<?> fieldType, JsonArray jsonArray, 
                                            String arrayVarName, StringBuilder code, 
                                            Class<?> elementType) {
            String elementTypeName = fieldType.getComponentType().getSimpleName();
            code.append(elementTypeName).append("[] ").append(arrayVarName)
                .append(" = new ").append(elementTypeName).append("[")
                .append(jsonArray.size()).append("];\n");
            
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonElement element = jsonArray.get(i);
                processArrayElement(fieldType, element, arrayVarName, i, code);
            }
        }
        
        private static void processArrayElement(Class<?> fieldType, JsonElement element, 
                                              String arrayVarName, int index, StringBuilder code) {
            if (element.isJsonObject() && !TypeAnalyzer.isPrimitiveOrString(fieldType.getComponentType())) {
                String elementVarName = VariableNameManager.generateUnique(
                    fieldType.getComponentType().getSimpleName().toLowerCase());
                CodeGenerator.generateRecursive(fieldType.getComponentType(), 
                                              element.getAsJsonObject(), elementVarName, code, new HashSet<>());
                code.append(arrayVarName).append("[").append(index).append("] = ")
                    .append(elementVarName).append(";\n");
            } else if (element.isJsonPrimitive() && TypeAnalyzer.isPrimitiveOrString(fieldType.getComponentType())) {
                String value = ValueConverter.getValueAsString(element, fieldType.getComponentType());
                code.append(arrayVarName).append("[").append(index).append("] = ")
                    .append(value).append(";\n");
            }
        }
        
        private static void generateCollectionCode(Class<?> fieldType, JsonArray jsonArray, 
                                                 String collectionVarName, StringBuilder code, 
                                                 Class<?> elementType, Field originalField) {
            String elementTypeName = elementType != null ? elementType.getSimpleName() : DEFAULT_ELEMENT_TYPE;
            String implementation = CollectionHandler.getImplementation(fieldType);
            
            // Use the original field type name instead of generic interface name
            String fieldTypeName = getOriginalFieldTypeName(originalField, fieldType);
            
            // Create collection declaration with original field type
            code.append(fieldTypeName).append("<").append(elementTypeName).append("> ")
                .append(collectionVarName).append(" = new ").append(implementation).append("<>();\n");
            
            // Process each element
            for (JsonElement element : jsonArray) {
                processCollectionElement(fieldType, element, collectionVarName, code, elementType);
            }
        }
        
        private static String getOriginalFieldTypeName(Field originalField, Class<?> fieldType) {
            if (originalField != null) {
                // Get the actual declared type from the field
                Class<?> declaredType = originalField.getType();
                return declaredType.getSimpleName();
            }
            // Fallback to interface name if field is not available
            return CollectionHandler.getInterfaceName(fieldType);
        }
        
        private static void processCollectionElement(Class<?> fieldType, JsonElement element, 
                                                   String collectionVarName, StringBuilder code, 
                                                   Class<?> elementType) {
            if (element.isJsonObject() && elementType != null) {
                // Create nested object recursively
                String elementVarName = VariableNameManager.generateUnique(
                    elementType.getSimpleName().toLowerCase());
                CodeGenerator.generateRecursive(elementType, element.getAsJsonObject(), 
                                              elementVarName, code, new HashSet<>());
                CollectionHandler.addElement(fieldType, collectionVarName, elementVarName, code);
            } else if (element.isJsonPrimitive()) {
                // Handle primitive values
                String value = ValueConverter.getPrimitiveValue(element.getAsJsonPrimitive());
                CollectionHandler.addElement(fieldType, collectionVarName, value, code);
            } else {
                // Handle null or unexpected element type
                code.append("// Warning: Unhandled element type in collection: ").append(element).append("\n");
                CollectionHandler.addElement(fieldType, collectionVarName, "null", code);
            }
        }
    }
    
    // ========================================================================================
    // VALUE CONVERSION UTILITIES
    // ========================================================================================
    
    private static class ValueConverter {
        
        public static String getValueAsString(JsonElement element, Class<?> type) {
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                
                if (type == String.class && primitive.isString()) {
                    return "\"" + escapeString(primitive.getAsString()) + "\"";
                } else if ((type == int.class || type == Integer.class) && primitive.isNumber()) {
                    return String.valueOf(primitive.getAsInt());
                } else if ((type == long.class || type == Long.class) && primitive.isNumber()) {
                    return primitive.getAsLong() + "L";
                } else if ((type == double.class || type == Double.class) && primitive.isNumber()) {
                    return String.valueOf(primitive.getAsDouble());
                } else if ((type == float.class || type == Float.class) && primitive.isNumber()) {
                    return primitive.getAsFloat() + "f";
                } else if ((type == boolean.class || type == Boolean.class) && primitive.isBoolean()) {
                    return String.valueOf(primitive.getAsBoolean());
                } else if ((type == char.class || type == Character.class) && primitive.isString()) {
                    return "'" + escapeChar(primitive.getAsString().charAt(0)) + "'";
                } else if ((type == byte.class || type == Byte.class) && primitive.isNumber()) {
                    return "(byte)" + primitive.getAsByte();
                } else if ((type == short.class || type == Short.class) && primitive.isNumber()) {
                    return "(short)" + primitive.getAsShort();
                }
            }
            return escapeString(element.getAsString());
        }
        
        public static String getPrimitiveValue(JsonPrimitive primitive) {
            if (primitive.isString()) {
                return "\"" + escapeString(primitive.getAsString()) + "\"";
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber().toString();
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? "true" : "false";
            }
            return "null";
        }
        
        /**
         * Escape special characters in strings for Java code generation
         */
        private static String escapeString(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t")
                     .replace("\b", "\\b")
                     .replace("\f", "\\f");
        }
        
        /**
         * Escape special characters in char literals for Java code generation
         */
        private static String escapeChar(char c) {
            switch (c) {
                case '\\': return "\\\\";
                case '\'': return "\\\'";
                case '\n': return "\\n";
                case '\r': return "\\r";
                case '\t': return "\\t";
                case '\b': return "\\b";
                case '\f': return "\\f";
                default: return String.valueOf(c);
            }
        }
    }
    
    // ========================================================================================
    // UTILITY CLASSES
    // ========================================================================================
    
    private static class StringUtils {
        public static String capitalize(String str) {
            if (str == null || str.isEmpty()) {
                return str;
            }
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
    }
    
    private static class FileUtils {
        public static String readJsonFile(String filePath) throws IOException {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        }
    }
    
    private static class ConsoleUtils {
        public static void printJsonContent(String filePath, String json) {
            System.out.println("JSON loaded from: " + filePath);
            System.out.println("JSON Content:");
            System.out.println(json);
            System.out.println("\n" + "=".repeat(50));
        }
        
        public static void printGeneratedCode(String code) {
            System.out.println("Generated Object Creation Code:");
            System.out.println("=".repeat(50));
            System.out.println(code);
        }
    }
    
    // ========================================================================================
    // MAIN METHOD
    // ========================================================================================
    
    public static void main(String[] args) {
        processJsonFile("e:\\Office Rough\\JsonToObjectGenCode\\sample_data.json", A.class);
    }
}
