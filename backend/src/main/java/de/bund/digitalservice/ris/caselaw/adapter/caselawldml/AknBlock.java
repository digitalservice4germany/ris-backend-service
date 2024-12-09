package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlSeeAlso;
import lombok.NoArgsConstructor;
import org.eclipse.persistence.oxm.annotations.XmlDiscriminatorNode;

@NoArgsConstructor
@XmlDiscriminatorNode("@name")
@XmlSeeAlso({
  Opinions.class,
  AknEmbeddedStructureInBlock.HeadNote.class,
  AknEmbeddedStructureInBlock.OtherHeadNote.class,
  AknEmbeddedStructureInBlock.Outline.class,
  AknEmbeddedStructureInBlock.Tenor.class,
  AknEmbeddedStructureInBlock.DecisionReasons.class,
  AknEmbeddedStructureInBlock.Reasons.class,
  AknEmbeddedStructureInBlock.OtherLongText.class
})
public abstract class AknBlock {
  public abstract String getName();
}
