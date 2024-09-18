package org.healthnlp.deepphe.nlp.ae.attribute;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PipeBitInfo.Role;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds Tumor Size of the form 1.2 cm, 1.2 x 1.2 cm, 1.2 x 1.2 cm, etc.,
 * Length x Width x Height takes first priority, then Length x Width, then Length.
 * Author: Nishanth P, DBMI-Columbia
 */

@PipeBitInfo(
  name = "SizeFinderAe",
  description = "Finds Tumor Size.",
  role = Role.ANNOTATOR
)
public class SizeFinderAe extends JCasAnnotator_ImplBase {
  private static final Logger LOGGER = Logger.getLogger("SizeFinderAe");

  public SizeFinderAe() {
  }

  public void process(JCas jcas) throws AnalysisEngineProcessException {
    LOGGER.info("Finding Sizes ...");
    SizeFinder.addSizes(jcas);
  }

  public enum SizeFinder {
    INSTANCE;

    static public SizeFinder getInstance() {
      return INSTANCE;
    }

    static private final Logger LOGGER = Logger.getLogger("SizeFinder");

    static private final String SIZE_1_REGEX = "\\b\\d+(?:\\.\\d+)?\\s*(?:cm|mm)\\b";
    static private final String SIZE_2_REGEX = "\\b\\d+(?:\\.\\d+)?\\s*(?:cm|mm)?\\s*(?:x|by|to|and|&)?\\s*\\d+(?:\\.\\d+)?\\s*(?:cm|mm)\\b";
    static private final String SIZE_3_REGEX = "\\b\\d+(?:\\.\\d+)?\\s*(?:cm|mm)?\\s*(?:x|by|to|and|&)?\\s*\\d+(?:\\.\\d+)?\\s*(?:cm|mm)?\\s*(?:x|by|to|and|&)?\\s*\\d+(?:\\.\\d+)?\\s*(?:cm|mm)\\b";
    // Use them in reverse so they capture the longer matches first
    static private final String FULL_REGEX = "(?:\\b\\d+(?:\\.\\d+)?\\s*(?:cm|mm)?\\s*(?:x|by|to|and|&)?\\s*\\d+(?:\\.\\d+)?\\s*(?:cm|mm)?\\s*(?:x|by|to|and|&)?\\s*\\d+(?:\\.\\d+)?\\s*(?:cm|mm)\\b)|(?:\\b\\d+(?:\\.\\d+)?\\s*(?:cm|mm)?\\s*(?:x|by|to|and|&)?\\s*\\d+(?:\\.\\d+)?\\s*(?:cm|mm)\\b)|(?:\\b\\d+(?:\\.\\d+)?\\s*(?:cm|mm)\\b)";

    static private final Pattern FULL_PATTERN = Pattern.compile(FULL_REGEX, Pattern.CASE_INSENSITIVE);

    private final Object LOCK = new Object();

    SizeFinder() {
    }

    static private final class SimpleSize {
      private final int _begin;
      private final int _end;
      private final String _sizeText;

      private SimpleSize(final int begin, final int end, String sizeText) {
        _begin = begin;
        _end = end;
        _sizeText = sizeText;
      }

      public String getSizeText() {
        return _sizeText;
      }
    }

    static public List<IdentifiedAnnotation> addSizes(final JCas jcas) {
      final String docText = jcas.getDocumentText();
      final List<SimpleSize> sizes = getSizes(docText);
      if (sizes.isEmpty()) {
        return Collections.emptyList();
      }
      final List<IdentifiedAnnotation> sizeAnnotations = new ArrayList<>();
      for (SimpleSize size : sizes) {
        final IdentifiedAnnotation annotation = AnnotationFactory.createAnnotation(jcas,
          size._begin, size._end, DpheGroup.SIZE, "SizeMeasurement", "", size.getSizeText());
        sizeAnnotations.add(annotation);
      }
      return sizeAnnotations;
    }

    static public List<IdentifiedAnnotation> addSizes(final JCas jcas, final AnnotationFS lookupWindow) {
      final String windowText = lookupWindow.getCoveredText();
      final List<SimpleSize> sizes = getSizes(windowText);
      if (sizes.isEmpty()) {
        return Collections.emptyList();
      }
      final int windowStartOffset = lookupWindow.getBegin();
      final List<IdentifiedAnnotation> sizeAnnotations = new ArrayList<>();
      for (SimpleSize size : sizes) {
        final IdentifiedAnnotation annotation = AnnotationFactory.createAnnotation(jcas,
          windowStartOffset + size._begin, windowStartOffset + size._end,
          DpheGroup.SIZE, "SizeMeasurement", "", size.getSizeText());
        sizeAnnotations.add(annotation);
      }
      return sizeAnnotations;
    }

    static List<SimpleSize> getSizes(final String lookupWindow) {
      if (lookupWindow.length() < 2) {
        return new ArrayList<>();
      }
      final List<SimpleSize> sizes = new ArrayList<>();
      final Matcher fullMatcher = FULL_PATTERN.matcher(lookupWindow);
      while (fullMatcher.find()) {
        int fullMatchStart = fullMatcher.start();
        String size = lookupWindow.substring(fullMatchStart, fullMatcher.end());
        sizes.add(new SimpleSize(fullMatchStart, fullMatcher.end(), size));
      }
      return sizes;
    }

  }
}

