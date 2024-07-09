package org.healthnlp.deepphe.omop;

import org.apache.ctakes.core.pipeline.PiperFileRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a 1:1 facade for the cTAKES PiperFileRunner.
 * The only thing that this class will do is add the default piper file if one is not specified.
 *
 * @author SPF , chip-nlp
 * @since {6/25/2024}
 */
final public class DpheOmopDocRunner {

   static private final String DEFAULT_PIPER = "pipeline/OmopDocRunner.piper";

   public static void main( final String... args ) {
      // Create modifiable list of args.  List.of creates an UnmodifiableList, so wrap it.
      final List<String> parms = new ArrayList<>( List.of( args ) );
      // Add the default piper file if one is not specified.
      if ( !parms.contains( "-p" ) ) {
         parms.add( "-p" );
         parms.add( DEFAULT_PIPER );
      }
      // Does nothing but call PiperFileRunner.
      if ( !PiperFileRunner.run( parms.toArray( new String[ 0 ] ) ) ) {
         System.exit( 1 );
      }
   }


}
