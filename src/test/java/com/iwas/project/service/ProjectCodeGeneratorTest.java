package com.iwas.project.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectCodeGeneratorTest {

    private final ProjectCodeGenerator gen = new ProjectCodeGenerator();

    @Test
    void threeWords_returnsInitials() {
        assertEquals("HRM", gen.generate("Human Resource Management"));
    }

    @Test
    void fourWords_returnsInitials() {
        assertEquals("PMOS", gen.generate("Project Management Office System"));
    }

    @Test
    void stopWordsFiltered_twoSignificantWords() {
        // "The", "of" filtered → ["Office", "Records"] → 2 words → initials
        assertEquals("OR", gen.generate("The Office of Records"));
    }

    @Test
    void twoWords_returnsInitials() {
        assertEquals("PM", gen.generate("Project Manager"));
    }

    @Test
    void oneWord_returnsFirstThreeChars() {
        assertEquals("ENG", gen.generate("Engineering"));
    }

    @Test
    void oneWord_shortWord_returnsAllChars() {
        assertEquals("IT", gen.generate("IT"));
    }

    @Test
    void specialCharsStripped() {
        assertEquals("HRM", gen.generate("Human & Resource Management!"));
    }

    @Test
    void uppercaseOutput() {
        String result = gen.generate("software development team");
        assertEquals(result, result.toUpperCase());
    }

    @Test
    void allStopWords_fallbackToRawInput() {
        // "a and the" → all filtered → fallback uses stripped raw input "AANDTHE"
        String result = gen.generate("a and the");
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    void manyWords_initialsCapAt7() {
        // 10 significant words → initials capped at 7
        String result = gen.generate("Alpha Beta Charlie Delta Echo Foxtrot Golf Hotel India Juliet");
        assertTrue(result.length() <= 7);
    }

    @Test
    void leadingTrailingSpaces_handled() {
        assertEquals("HRM", gen.generate("  Human Resource Management  "));
    }

    @Test
    void mixedCase_outputAlwaysUppercase() {
        assertEquals("HRM", gen.generate("human resource management"));
    }
}
