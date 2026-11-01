package org.yuktisetu.drive.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailListTest {

    @Test
    @DisplayName("reads emails out of any shape of list: lines, commas, names, sheet rows")
    void anyShape() {
        EmailList.Parsed p = EmailList.parse("""
                Asha Rao <Asha.Rao@PCCOE.org>, ravi@pccoe.org; meera+test@pccoe.org
                2023070001\tKaran Patil\tkaran.patil@pccoe.org
                "neha@pccoe.org".
                """);
        assertEquals(List.of("asha.rao@pccoe.org", "ravi@pccoe.org", "meera+test@pccoe.org",
                "karan.patil@pccoe.org", "neha@pccoe.org"), p.emails());
        assertTrue(p.duplicates().isEmpty());
        assertTrue(p.unreadable().isEmpty());
    }

    @Test
    @DisplayName("the same student twice is counted once, and reported")
    void duplicates() {
        EmailList.Parsed p = EmailList.parse("a@x.org\nA@X.org\na@x.org\nb@x.org");
        assertEquals(List.of("a@x.org", "b@x.org"), p.emails());
        assertEquals(Map.of("a@x.org", 2), p.duplicates());
    }

    @Test
    @DisplayName("broken addresses are reported, never guessed at")
    void unreadable() {
        EmailList.Parsed p = EmailList.parse("good@x.org, broken@pccoe, also @x.org");
        assertEquals(List.of("good@x.org"), p.emails());
        assertEquals(List.of("broken@pccoe,", "@x.org"), p.unreadable());
    }

    @Test
    @DisplayName("blank input is an empty list, not an error")
    void blank() {
        assertTrue(EmailList.parse("  \n ").emails().isEmpty());
        assertTrue(EmailList.parse(null).emails().isEmpty());
    }
}
