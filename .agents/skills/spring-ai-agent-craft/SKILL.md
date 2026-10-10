---
name: spring-ai-agent-craft
description: >-
  Best practices and implementation patterns for building deterministic, resilient AI Agents
  and Copilots using Spring Boot 3.3+, Spring AI 1.x, and LLMs (Gemini/OpenAI). Covers @Tool /
  Function Calling registration, record DTO serialization, MessageChatMemoryAdvisor, conversationId
  session management, native query multi-column array mapping, and retry/circuit-breaker resilience.
---

# Spring AI Agent & Function Calling Craft

This skill provides architecture guidelines and implementation patterns for building production-grade AI agents using **Spring AI 1.x** and **Spring Boot 3.3+**.

---

## 1. ChatClient Fluent Architecture & Tool Binding

```java
@Service
public class TacticalAgentService {

    private final ChatClient chatClient;

    public TacticalAgentService(
        ChatClient.Builder chatClientBuilder,
        @Autowired(required = false) ChatMemory chatMemory,
        RadarEmissionTools radarTools,
        ComintEmissionTools comintTools
    ) {
        ChatClient.Builder builder = chatClientBuilder
            .defaultSystem(SYSTEM_DIRECTIVE)
            .defaultTools(radarTools, comintTools);

        if (chatMemory != null) {
            builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            );
        }

        this.chatClient = builder.build();
    }
}
```

---

## 2. Multi-Turn Session Memory (ChatMemory)

* Always isolate sessions by passing `chat_memory_conversation_id`:
```java
String briefing = chatClient.prompt()
    .advisors(spec -> spec.param("chat_memory_conversation_id", conversationId))
    .user(userPrompt)
    .call()
    .content();
```
* **Critical Rule:** In Spring AI request DTOs, use matching parameter names (e.g. `userPrompt` in Java record must match `userPrompt` in JSON payload, not `prompt`).

---

## 3. Spring AI @Tool Function Calling Guidelines

1. **Clear Descriptions:** `@Tool(description = "...")` must detail both what the tool does AND when the LLM should invoke it.
2. **Primitive Wrapper Types:** Always use `Integer`, `Double`, `Boolean` instead of primitive types (`int`, `double`) so arguments can be optional/nullable.
3. **ThreadLocal Call Logging:** Maintain auditability by capturing tool calls in a `ThreadLocal<List<String>>`:
```java
public class RadarEmissionTools {
    private static final ThreadLocal<List<String>> CALL_LOGS = ThreadLocal.withInitial(ArrayList::new);

    public static List<String> drainCallLogs() {
        List<String> logs = new ArrayList<>(CALL_LOGS.get());
        CALL_LOGS.get().clear();
        return logs;
    }
}
```

---

## 4. JPA Native Query Multi-Column Gotcha in Spring Data

When calling native queries with multiple columns without an entity mapping:
* **The Gotcha:** Spring Data JPA returns `Object[]` or `List<Object[]>`. In single-row queries, `counts[0]` is often an `Object[]` array itself (`[Ljava.lang.Object;`), causing `ClassCastException` if cast directly to `Number`.
* **The Bulletproof Solution:**
```java
Object[] raw = repo.getHierarchyCounts(start, end);
Object[] row = raw;
if (raw != null && raw.length > 0 && raw[0] instanceof Object[]) {
    row = (Object[]) raw[0];
}

long total = extractLong(row, 0);

private long extractLong(Object[] arr, int index) {
    if (arr == null || index >= arr.length || arr[index] == null) return 0L;
    if (arr[index] instanceof Number num) return num.longValue();
    try {
        return Long.parseLong(arr[index].toString());
    } catch (Exception e) {
        return 0L;
    }
}
```

---

## 5. Resilience: Exponential Backoff Retry Pattern

LLM APIs can suffer temporary rate limits (HTTP 429) or timeouts:
```java
int maxRetries = 3;
for (int attempt = 1; attempt <= maxRetries; attempt++) {
    try {
        return chatClient.prompt().user(prompt).call().content();
    } catch (Exception ex) {
        if (attempt == maxRetries) throw ex;
        Thread.sleep(1200L * attempt);
    }
}
```
