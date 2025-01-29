package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.languagetool.LanguageToolResponse;
import de.bund.digitalservice.ris.caselaw.adapter.languagetool.Match;
import de.bund.digitalservice.ris.caselaw.domain.textcheck.Category;
import de.bund.digitalservice.ris.caselaw.domain.textcheck.Context;
import de.bund.digitalservice.ris.caselaw.domain.textcheck.Match.MatchBuilder;
import de.bund.digitalservice.ris.caselaw.domain.textcheck.Replacement;
import de.bund.digitalservice.ris.caselaw.domain.textcheck.Rule;
import de.bund.digitalservice.ris.caselaw.domain.textcheck.Suggestion;
import de.bund.digitalservice.ris.caselaw.domain.textcheck.TextCheckResponse;
import de.bund.digitalservice.ris.caselaw.domain.textcheck.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TextCheckResponseTransformer {
  private TextCheckResponseTransformer() {}

  public static TextCheckResponse transformToDomain(
      List<de.bund.digitalservice.ris.caselaw.domain.textcheck.Match> matches) {
    List<Suggestion> suggestions = new ArrayList<>();
    matches.forEach(
        match -> {
          String word =
              match
                  .context()
                  .text()
                  .substring(
                      match.context().offset(),
                      match.context().offset() + match.context().length());

          if (suggestions.stream().noneMatch(suggestion -> suggestion.word().equals(word))) {
            suggestions.add(new Suggestion(word, new ArrayList<>()));
          }

          Optional<Suggestion> suggestionOptional =
              suggestions.stream().filter(suggestion -> suggestion.word().equals(word)).findFirst();

          suggestionOptional.ifPresent(suggestion -> suggestion.matches().add(match));
        });

    return TextCheckResponse.builder().suggestions(suggestions).build();
  }

  public static List<de.bund.digitalservice.ris.caselaw.domain.textcheck.Match>
      transformToListOfDomainMatches(LanguageToolResponse response) {
    List<de.bund.digitalservice.ris.caselaw.domain.textcheck.Match> matches = new ArrayList<>();

    for (Match match : response.getMatches()) {
      MatchBuilder matchBuilder =
          de.bund.digitalservice.ris.caselaw.domain.textcheck.Match.builder()
              .message(match.getMessage())
              .shortMessage(match.getShortMessage())
              .ignoreForIncompleteSentence(match.isIgnoreForIncompleteSentence());

      List<Replacement> replacements = new ArrayList<>();
      for (de.bund.digitalservice.ris.caselaw.adapter.languagetool.Replacement replacement :
          match.getReplacements()) {
        replacements.add(new Replacement(replacement.getValue()));
      }

      matchBuilder
          .replacements(replacements)
          .offset(match.getOffset())
          .length(match.getLength())
          .context(
              new Context(
                  match.getContext().getText(),
                  match.getContext().getOffset(),
                  match.getContext().getLength()))
          .sentence(match.getSentence())
          .type(new Type(match.getType().getTypeName()));

      Rule rule =
          Rule.builder()
              .id(match.getRule().getId())
              .description(match.getRule().getDescription())
              .issueType(match.getRule().getIssueType())
              .category(
                  new Category(
                      match.getRule().getCategory().getId(),
                      match.getRule().getCategory().getName()))
              .build();

      matchBuilder.rule(rule);

      matches.add(matchBuilder.build());
    }

    return matches;
  }
}
