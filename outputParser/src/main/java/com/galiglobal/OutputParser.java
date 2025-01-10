package com.galiglobal;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.output.Response;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class OutputParser {

  private static final String RESET = "\033[0m"; // Text Reset
  private static final String RED = "\033[0;31m"; // RED
  private static final String GREEN = "\033[0;32m"; // GREEN
  private static final String SUCCESS = GREEN + "SUCCESS" + RESET;
  private static final String ERROR = RED + "ERROR" + RESET;
  // private static final String YELLOW = "\033[0;33m"; // YELLOW
  // private static final String BLUE = "\033[0;34m"; // BLUE

  private static final String MODEL = "mistral";
  private static final String BASE_URL = "http://localhost:11434";
  private static Duration timeout = Duration.ofSeconds(120);

  private static String terminalOutput = "";

  public static void main(String[] args) {

    String output = "";

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        FileWriter writer = new FileWriter("/tmp/output.log", false)) {
      String input = "";
      while ((input = reader.readLine()) != null) {
        output = output + input + System.lineSeparator();
        writer.write(input + System.lineSeparator());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (output.isEmpty()) {
      System.out.println("No input provided. Exiting.");
      output = "Error: no output generated.";
    }

    ChatMemory memory = MessageWindowChatMemory.withMaxMessages(3);

    StreamingChatLanguageModel model =
        OllamaStreamingChatModel.builder()
            .baseUrl(BASE_URL)
            .modelName(MODEL)
            .timeout(timeout)
            .temperature(0.2)
            .responseFormat(
                ResponseFormat.builder()
                    .type(ResponseFormatType.JSON)
                    .jsonSchema(
                        JsonSchema.builder()
                            .rootElement(
                                JsonObjectSchema.builder()
                                    .addEnumProperty("result", List.of("SUCCESS", "ERROR"))
                                    .addStringProperty("summary", "Short summary of the output")
                                    .addStringProperty(
                                        "file",
                                        "Name of the class that has the error. Optional if there's"
                                            + " no errors or failed tests")
                                    .addIntegerProperty(
                                        "line",
                                        "Number of the line where the error happened. It's usually"
                                            + " the number after the name of the class and a color."
                                            + " Optional if there's no errors or failed tests")
                                    .required("result", "summary")
                                    .build())
                            .build())
                    .build())
            .build();

    memory.add(
        SystemMessage.from(
            """
The user is going to provide the output of a command, it's usually a compiler,
linter, formatter or the execution of programming tests. You should provide a
summary of 3 lines, no blank lines between them or numbers in the beginning.
"""));

    memory.add(UserMessage.from(output));
    CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();

    model.generate(
        memory.messages(),
        new StreamingResponseHandler<AiMessage>() {

          @Override
          public void onNext(String token) {
            terminalOutput = terminalOutput + token;
          }

          @Override
          public void onComplete(Response<AiMessage> response) {
            memory.add(response.content());
            futureResponse.complete(response);
          }

          @Override
          public void onError(Throwable error) {
            futureResponse.completeExceptionally(error);
          }
        });

    futureResponse.join();

    String[] lines = terminalOutput.split("\n");
    for (int i = 1; i < lines.length - 1; i++) {
      String trimmedLine = lines[i].trim();
      System.out.println(trimmedLine.replace("ERROR", ERROR).replace("SUCCESS", SUCCESS));
    }

    try (FileWriter file = new FileWriter("/tmp/output.json")) {
      file.write(terminalOutput);
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.exit(0);
  }
}
