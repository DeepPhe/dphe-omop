package org.healthnlp.deepphe.omop;

public class Mention {
  public final String semantic;
  public final String uri;
  public final String cui;
  public final String tui;
  public final String prefText;
  public final String negated;
  public final String uncertain;
  public final String historic;
  public final String confidence;
  public final String term;
  public final String window;

  public Mention(String semantic, String uri, String cui, String tui, String prefText,
                 String negated, String uncertain, String historic, String confidence,
                 String term, String window) {
    this.semantic = semantic;
    this.uri = uri;
    this.cui = cui;
    this.tui = tui;
    this.prefText = prefText;
    this.negated = negated;
    this.uncertain = uncertain;
    this.historic = historic;
    this.confidence = confidence;
    this.term = term;
    this.window = window;
  }

  public String toString() {
    return String.join( ", ", semantic, uri, cui, tui, prefText,
      negated, uncertain, historic, confidence, term, window );
  }
}