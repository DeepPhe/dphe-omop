package org.healthnlp.deepphe.omop.writer;

import org.apache.ctakes.core.cc.AbstractTableFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.apache.ctakes.ner.group.dphe.DpheGroupAccessor;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {6/25/2024}
 */
@PipeBitInfo (
      name = "Omop Mention Table Writer",
      description = "Writes a table of Document discovery information to file.",
      role = PipeBitInfo.Role.WRITER
)
public class OmopMentionTableWriter extends AbstractTableFileWriter {

   static private final Logger LOGGER = Logger.getLogger( "OmopMentionTableWriter" );


   /**
    * To print a decimal precise to only 2 digits.
    */
   static private final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#.00");


   /**
    * This header is made to fit both Element (Patient Concept) and Mention (Document Annotation) information.
    * The table header is constant, it is not based upon any values.
    */
   static private final List<String> HEADER = Arrays.asList( " Semantic ", " URI ", " CUI ", " TUI ", " Pref. Text ",
         " Negated ", " Uncertain ", " Historic ", " Confidence ", " Text ", " Window ");


   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow(final JCas jCas) {
      return HEADER;
   }


   /**
    * After every document is processed (at this point in the pipeline) the document cas is sent here.
    * If the patient processing is complete, return a series of rows pertaining to elements and mentions.
    * Element URI , Element CUI , Element Group , Element PrefText, Negated, Uncertain, Historic, Element Confidence
    * *   Mention URI , Mention CUI , Mention TUI , Mention PrefText, -N , -U , -H , Mention Confidence , DocId , Mention Text , Window
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      final Collection<IdentifiedAnnotation> mentions = JCasUtil.select( jCas, IdentifiedAnnotation.class );
      return mentions.stream()
              .map( MentionInfoHolder::new )
              .sorted( MENTION_COMPARATOR )
              .map( MentionInfoHolder::getRow )
              .collect( Collectors.toList() );
   }


   private static class MentionInfoHolder {
      static private final int WINDOW_EDGE = 40;
      private final DpheGroup _dpheGroup;
      private final String _uri;
      private final String _cui;
      private final SemanticTui _tui;
      private final String _prefText;
      private final String _negated;
      private final String _uncertain;
      private final String _historic;
      private final String _text;
      private String _textWindow = "";
      private final String _confidence;
      private MentionInfoHolder( final IdentifiedAnnotation annotation ) {
         _uri = Neo4jOntologyConceptUtil.getUris( annotation ).stream().findFirst().orElse( "" );
         _cui = IdentifiedAnnotationUtil.getCuis( annotation ).stream().findFirst().orElse( "" );
         _dpheGroup = DpheGroupAccessor.getInstance()
                                       .getBestGroup( DpheGroupAccessor.getInstance()
                                                                       .getAnnotationGroups( annotation ) );
         _tui = SemanticTui.getTuis( annotation ).stream().findFirst().orElse( SemanticTui.UNKNOWN );
         _negated =  IdentifiedAnnotationUtil.isNegated( annotation ) ? "True" : "False";
         _uncertain = IdentifiedAnnotationUtil.isUncertain( annotation ) ? "True" : "False";
         _historic = IdentifiedAnnotationUtil.isHistoric( annotation ) ? "True" : "False";
         // Replace newlines and carriage returns with spaces to keep the CSV rows clean
         _prefText = IdentifiedAnnotationUtil.getPreferredTexts( annotation ).stream().findFirst().orElse( "" )
           .replace('\n', ' ').replace('\r', ' ').replace('|', ' ');
        _text = annotation.getCoveredText().replace('\n', ' ').replace('\r', ' ')
          .replace('|', ' ');
         try {
            //////////////////////////////////////////////////////////////
            //   This is kind of strange, but you must use getView() on an annotation, not getCAS().
            //   getCAS() seems to always return the _InitialView, or the Patient cas containing the view.
            //////////////////////////////////////////////////////////////
//                final JCas annotationCas = annotation.getCAS().getJCas();
            final JCas annotationCas = annotation.getView().getJCas();
            final String docText = annotationCas.getDocumentText();
            _textWindow = docText
              .substring(
                  Math.max( 0, annotation.getBegin()-WINDOW_EDGE ),
                  Math.min( docText.length(), annotation.getEnd() + WINDOW_EDGE ) )
              // Replace newlines and carriage returns with spaces to keep the CSV rows clean
              .replace('\n', ' ').replace('\r', ' ').replace('|', ' ');
         } catch ( CASException casE ) {
            LOGGER.error( "Could not find JCas for annotation " + annotation.getCoveredText() );
         }
         _confidence = DECIMAL_FORMATTER.format( annotation.getConfidence() );
      }
      private List<String> getRow() {
         return Arrays.asList( _dpheGroup.getName(), _uri, _cui, _tui.name(), _prefText,
               _negated, _uncertain, _historic,
               _confidence, _text, _textWindow );
      }
   }

   /**
    * Compares mentions by group, uri, and confidence.  Used to sort the element mention rows.
    */
   static private final Comparator<MentionInfoHolder> MENTION_COMPARATOR = ( m1, m2 ) -> {
      final int group = String.CASE_INSENSITIVE_ORDER.compare( m1._dpheGroup.getName(), m2._dpheGroup.getName() );
      if ( group != 0 ) {
         return group;
      }
      final int uri = String.CASE_INSENSITIVE_ORDER.compare( m1._uri, m2._uri );
      if ( uri != 0 ) {
         return uri;
      }
      // Reverse confidence
      return String.CASE_INSENSITIVE_ORDER.compare( m2._confidence, m1._confidence );
   };



}
