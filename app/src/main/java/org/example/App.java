package org.example;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.output.Response;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

class App {

  //private static final String RESET = "\033[0m"; // Text Reset
  //private static final String RED = "\033[0;31m"; // RED
  //private static final String GREEN = "\033[0;32m"; // GREEN
  //private static final String YELLOW = "\033[0;33m"; // YELLOW
  //private static final String BLUE = "\033[0;34m"; // BLUE

  private static final String MODEL = "mistral";
  private static final String BASE_URL = "http://localhost:11434";
  private static Duration timeout = Duration.ofSeconds(120);

  public static void main(String[] args) {
    beginChatWithChatMemory();
  }

  static void beginChatWithChatMemory() {

    String output = "";

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        FileWriter writer =
            new FileWriter("/tmp/output.log", false)) { // 'false' to overwrite the file
      String input = "";
      while ((input = reader.readLine()) != null) {
        // System.out.println(input);
        output = output + input + System.lineSeparator();
        writer.write(input + System.lineSeparator());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (output.isEmpty()) {
      System.out.println("No input provided. Exiting.");
      System.exit(1);
    }

    ChatMemory memory = MessageWindowChatMemory.withMaxMessages(3);

    StreamingChatLanguageModel model =
        OllamaStreamingChatModel.builder()
            .baseUrl(BASE_URL)
            .modelName(MODEL)
            .timeout(timeout)
            .temperature(0.2)
            .responseFormat(ResponseFormat.JSON)
            .build();

    memory.add(
        SystemMessage.from(
            """
The user is going to provide the output of a command, it's usually a compiler, linter, formatter or the execution of programming tests. You should provide a summary of 3 lines, no blank lines between them or numbers in the beginning.
"""));

    memory.add(UserMessage.from(output));
    CompletableFuture<Response<AiMessage>> futureResponse = new CompletableFuture<>();
    model.generate(
        memory.messages(),
        new StreamingResponseHandler<AiMessage>() {

          @Override
          public void onNext(String token) {
            System.out.print(token);
          }

          @Override
          public void onComplete(Response<AiMessage> response) {
            memory.add(response.content());
            futureResponse.complete(response);
            System.out.println();
          }

          @Override
          public void onError(Throwable error) {
            futureResponse.completeExceptionally(error);
          }
        });

    futureResponse.join();
    System.exit(0);
  }
}