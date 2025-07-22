import com.google.gson.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class Demo {
    public static void main(String[] args) {
        JsonToObjectGenerator.processJsonFile("e:\\Office Rough\\JsonToObjectGenCode\\sample_data.json", A.class);
    }
}
