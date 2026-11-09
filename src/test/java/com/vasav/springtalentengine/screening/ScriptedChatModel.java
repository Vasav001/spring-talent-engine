package com.vasav.springtalentengine.screening;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

class ScriptedChatModel implements ChatModel {

    private final Deque<String> replies;
    private final List<Prompt> prompts = new ArrayList<>();

    ScriptedChatModel(String... replies) {
        this.replies = new ArrayDeque<>(List.of(replies));
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        if (replies.isEmpty()) {
            throw new IllegalStateException("No scripted reply left");
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage(replies.removeFirst()))));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    List<Prompt> prompts() {
        return prompts;
    }
}
