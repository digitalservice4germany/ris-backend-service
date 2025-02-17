package de.bund.digitalservice.ris.caselaw.domain;

/** Enum representing different source values for decisions. */
public enum SourceValue {
  O, // Unaufgefordert eingesandtes Original
  A, // Angefordertes Original
  Z, // Zeitschriftenveröffentlichung
  E, // Ohne Vorlage des Originals E-Mail
  L, // Ländergerichte, EuG- und EuGH-Entscheidungen über jDV-Verfahren
  S // Sonstige
}
