/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.zimwiki;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ZimWikiFileTests {

    public static class GeneratorTests {
        @SuppressWarnings("SpellCheckingInspection")
        @Test
        public void createsCorrectContentsForNewZimWikiFiles() {
            Locale.setDefault(Locale.ENGLISH);
            String expected = "Content-Type: text/x-zim-wiki\n" +
                    "Wiki-Format: zim 0.6\n" +
                    "Creation-Date: 2020-12-24T18:00:30+01:00\n" +
                    "\n" +
                    "====== My new wiki page ======\n" +
                    "Created Thursday 24 December 2020\n";
            String actual = ZimWikiTextActions.createZimWikiHeaderAndTitleContents("My_new_wiki_page", new Date(), "Created");
            assertThat(actual.replaceAll("Creat.*\n", "Creat\n")).isEqualTo(expected.replaceAll("Creat.*\n", "Creat\n"));
        }
    }

    public static class ZimFileHeaderTest {

        private Pattern pattern;

        @Test
        public void zimHeaderAtBeginningOfTheFileShouldMatch() {
            pattern = ZimWikiHighlighter.Patterns.ZIMHEADER.pattern;
            Matcher matcher = pattern.matcher("Content-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00\nOther content...");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("Content-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00");
        }

        @Test
        public void zimHeaderNotAtBeginningOfTheFileShouldNotMatch() {
            pattern = ZimWikiHighlighter.Patterns.ZIMHEADER.pattern;
            Matcher matcher = pattern.matcher("Blabla\nContent-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00");
            assertThat(matcher.find()).isFalse();
        }
    }

}
