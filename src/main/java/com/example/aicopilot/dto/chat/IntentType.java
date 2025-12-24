package com.example.aicopilot.dto.chat;

/**
 * Intent classification categories for the AI Architect.
 * Each type represents a specific agent responsibility.
 */
public enum IntentType {
    DESIGN,   // Create a brand-new process map
    MODIFY,   // Update or edit an existing structure
    ANALYZE,  // Perform structural audit and optimization checks
    GUIDE,    // Functional guidance on how to use the application
    CHAT      // General business conversation within the domain
}