package org.healthnlp.deepphe.nlp.ae.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PipeBitInfo.Role;
import org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;

/**
 * Finds Nuclear Grade of the form "Grade 1", "Nuclear grade 2" etc.
 * Author: Nishanth P, DBMI-Columbia
 */

@PipeBitInfo(
  name = "NuclearGradeFinder",
  description = "For deepphe.",
  products = {TypeProduct.IDENTIFIED_ANNOTATION, TypeProduct.GENERIC_RELATION},
  role = Role.ANNOTATOR
)
public final class NuclearGradeFinder extends JCasAnnotator_ImplBase {
  private static final Logger LOGGER = Logger.getLogger("NuclearGradeFinder");
  private static final String GRADE_URI = "GradingSystem";
  private static final Pattern FULL_PATTERN = Pattern.compile("(?:nuclear|tumor)?\\s*grade\\s*(?<GRADE>[1-9]|10)(?:[^0-9]|$)", Pattern.CASE_INSENSITIVE);

  public NuclearGradeFinder() {
  }

  public void process(JCas jCas) throws AnalysisEngineProcessException {
    LOGGER.info("Finding Grade Score Values ...");
    Collection<IdentifiedAnnotation> grades = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch(jCas, "GradingSystem");
    grades.forEach(TOP::removeFromIndexes);
    for ( Segment section : JCasUtil.select( jCas, Segment.class ) ) {
      findNuclearGrades( jCas, section );
    }
  }

  public static List<IdentifiedAnnotation> findNuclearGrades(JCas jcas, Annotation lookupWindow) {
    String lookupText = lookupWindow.getCoveredText();
    int grouping = lookupText.indexOf("Prognostic Nuclear Grade Group");
    if (grouping >= 0) {
      lookupText = lookupText.substring(0, grouping);
    }

    List<SimpleGrade> grades = getNuclearGrades(lookupText);
    if (grades.isEmpty()) {
      return Collections.emptyList();
    } else {
      Collection<IdentifiedAnnotation> plainGrades = Neo4jOntologyConceptUtil.getAnnotationsByUriBranch(jcas, lookupWindow, "CTCAE_Grade_Finding");
      int windowStartOffset = lookupWindow.getBegin();
      List<IdentifiedAnnotation> annotations = new ArrayList(grades.size());
      for ( SimpleGrade grade : grades ) {
        IdentifiedAnnotation annotation = AnnotationFactory.createAnnotation(jcas, windowStartOffset + grade._begin, windowStartOffset + grade._end, DpheGroup.DISEASE_GRADE_QUALIFIER, grade._uri, "", grade._uri);
        annotations.add(annotation);
        plainGrades.stream()
          .filter((a) -> a.getBegin() >= windowStartOffset + grade._matchBegin)
          .filter((a) -> a.getEnd() <= windowStartOffset + grade._end)
          .forEach(IdentifiedAnnotation::removeFromIndexes);
      }

      return annotations;
    }
  }

  private static List<SimpleGrade> getNuclearGrades(String lookupWindow) {
    if (lookupWindow.length() < 3) {
      return new ArrayList();
    } else {
      int comments = lookupWindow.lastIndexOf("COMMENTS");
      if (comments < 0) {
        comments = Integer.MAX_VALUE;
      }

      List<SimpleGrade> grades = new ArrayList();
      Matcher fullMatcher = FULL_PATTERN.matcher(lookupWindow);

      while(fullMatcher.find()) {
        if (fullMatcher.start() <= comments) {
          String gradeText = fullMatcher.group("GRADE");
          int gradeStart = fullMatcher.start("GRADE");
          int gradeEnd = fullMatcher.end("GRADE");
          grades.add(new SimpleGrade(fullMatcher.start(), gradeStart, fullMatcher.end(), getNuclearGradeUri(gradeText)));
        }
      }

      return grades;
    }
  }

  private static String getNuclearGradeUri(String gradeText) {
    return "Nuclear_Grade_Score_" + gradeText;
  }

  private static final class SimpleGrade {
    private final int _matchBegin;
    private final int _begin;
    private final int _end;
    private final String _uri;

    private SimpleGrade(int matchBegin, int begin, int end, String uri) {
      this._matchBegin = matchBegin;
      this._begin = begin;
      this._end = end;
      this._uri = uri;
    }
  }
}
