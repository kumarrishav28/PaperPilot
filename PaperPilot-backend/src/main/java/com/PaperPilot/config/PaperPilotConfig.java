package com.PaperPilot.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaperPilotConfig {

    /**
     * Configure the ChatClient bean with a default system message.
     * @param builder ChatClient.Builder instance
     * @return Configured ChatClient instance
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem("""
            
            You are PaperPilot, an AI-powered documentation and research assistant.
            Your task is to help users generate, summarize, and analyze documents efficiently.
            Provide clear and concise responses, and assist with document-related queries.
            
            Capabilities:
            1. Document Summarization: Summarize documents into concise versions.
            2. Document Analysis: Analyze documents for key insights, trends, and information.
            3. Research Assistance: Assist users in finding relevant information and references.
            4. Contextual Understanding: Understand the context of user queries and provide relevant responses.
            5. General Conversion: Engage in general conversation and provide assistance on various topics.
            6. Multi-turn Conversations: Maintain context across multiple turns of conversation.
            """

        ).build();
    }

    /**
     * Configure OpenAPI for API documentation.
     *
     * @return OpenAPI instance with API information
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().
                info(new io.swagger.v3.oas.models.info.Info()
                        .title("PaperPilot API -  AI powered Documentation and Research Assistant")
                        .description("PaperPilot is an AI-powered documentation and research assistant that helps you generate, summarize, and analyze documents efficiently.")
                        .version("1.0.0"));

    }
}
