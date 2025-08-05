package org.healthnlp.deepphe.omop;

import org.apache.commons.cli.*;
import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.omop.writer.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Java equivalent of the DeepPhe piper file for processing medical documents.
 * This pipeline runs DeepPhe processing on documents without patient information.
 */
public class DpheOmopPipelineBuilder {
    /**
     * Main method to demonstrate pipeline creation
     * @throws IOException
     * @throws UIMAException
     */
    private final PipelineBuilder builder;
    private AnalysisEngine analysisEngine;
    private JCas jcas;

    public DpheOmopPipelineBuilder(String configFile) throws UIMAException {
        final PiperFileReader reader = new PiperFileReader(configFile);
        this.builder = reader.getBuilder();
    }

    public void initialize() throws UIMAException, IOException {
        if (this.analysisEngine != null) {
            return;
        }
        System.out.println("Initializing DpheOmopPipelineBuilder: Creating live AnalysisEngine and JCas...");
        AnalysisEngineDescription aed = builder.getAnalysisEngineDesc();
        this.analysisEngine = UIMAFramework.produceAnalysisEngine(aed);
        this.jcas = JCasFactory.createJCas();
        System.out.println("Initialization complete.");
    }

    public List<Mention> run(String text) throws UIMAException {
        if (this.analysisEngine == null || this.jcas == null) {
            throw new IllegalStateException("The builder has not been initialized. Please call initialize() first.");
        }

        jcas.reset();
        jcas.setDocumentText(text);

        analysisEngine.process(jcas);

        OmopMentionTableWriter writer = new OmopMentionTableWriter();
        List<Mention> rows = writer.createDataFields(jcas);
        String patientId = SourceMetadataUtil.getPatientIdentifier( jcas );
        if (PatientDocCounter.getInstance().isPatientFull(patientId)) {
            UriInfoCache.getInstance().clear();
        }
        return rows;
    }

    public void close() {
        if (this.analysisEngine != null) {
            System.out.println("Closing DpheOmopPipelineBuilder: Destroying AnalysisEngine...");
            try {
                this.analysisEngine.destroy();
            } catch (Exception e) {
                System.err.println("Error during AnalysisEngine destruction: " + e.getMessage());
            } finally {
                this.analysisEngine = null;
            }
        }
    }

    public static void main(String[] args) throws ParseException, UIMAException, IOException {
        Options options = new Options();
        options.addOption("r", "resources_path", true, "Get full path to resources directory");

        CommandLineParser parser = new BasicParser();
        String resources_path = "";
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("r")) {
                resources_path = cmd.getOptionValue("r");
                System.out.println("Using resources path - " + resources_path);
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DeepPheOmop", options);
                return;
            }
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("DeepPheOmop", options);
            throw e;
        }

        Path tmp_resources_path = Files.createTempDirectory("resources");
        Path resourcesPath = Paths.get(resources_path);

        Files.walk(resourcesPath)
            .forEach(source -> {
                try {
                    Files.copy(source,
                      tmp_resources_path.resolve(resourcesPath.relativize(source)),
                      StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        String tmp_resources_dir = tmp_resources_path.toString();
        String configFile = resources_path + File.separator + "pipeline" + File.separator + "OmopDocRunner.piper";
        Path configPath = Paths.get(configFile);

        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(configPath), charset);
        content = content.replaceAll("/app/resources", tmp_resources_dir);
        Files.write(configPath, content.getBytes(charset));

        DpheOmopPipelineBuilder pipeline = new DpheOmopPipelineBuilder(configFile);
        pipeline.initialize();
        try {
            String text = "===================================================================\n" + //
                "Report ID.....................1,doc1\n" + //
                "Patient ID....................pt123123123\n" + //
                "Patient Name..................Fake Patient1\n" + //
                "Principal Date................20100123 1045\n" + //
                "Record Type...................RAD\n" + //
                "Patient DOB...................04/01/1960\n" + //
                "\n" + //
                "CLINICAL HISTORY:\n" + //
                "This is a 50 year old peri-menopausal female who underwent mammogram on 1/28/09 for a palpable lump in the right breast. Ultrasonography revealed a 1.2x3.4x5.6 cm hypoechoic mass in the upper inner quadrant at the 1 0’clock position. Ultrasonography also revealed an abnormally thickened lymph node in the right axilla which had a thickened cortex of 7 mm.  She now presents for U/S guided core biopsy of the mass and the abnormal lymph node.\n" + //
                "PROCEDURE: \n" + //
                "Ultrasound guided core biopsy of right breast 1 o’clock abnormality with clip placement\n" + //
                "Ultrasound guided core biopsy of right axilla abnormal lymph node with clip placement\n" + //
                "\n" + //
                "FINDINGS:\n" + //
                "The right breast and axilla were sterilely prepped and draped in the usual standard fashion.  First the right 1 o’clock position 5 cm from the nipple was targeted.  Local anesthesia was obtained with 2% xylocaine.  A small skin incision was made.  Under ultrasound guidance from a medial approach, 2 passes with a 14 gauge biopsy device were performed and sent to pathology.  A clip was placed. \n" + //
                "Then attention was turned to the right abnormal axillary lymph node.  Local anesthesia was obtained with 2% xylocaine.  A small skin incision was made.  Under ultrasound guidance from an inferomedial approach, 2 passes with a 14 gauge biopsy device were performed and sent to pathology.  A  clip was placed at the site of the biopsy.\n" + //
                "The wounds were cleaned and dressed.  The patient tolerated the procedure well, and there were no complications.\n" + //
                "Post procedure mammogram of the right breast demonstrated adequate clip placement.\n" + //
                "IMPRESSION:\n" + //
                "Uncomplicated ultrasound guided core biopsies of the right breast at the 1 o’clock position and abnormal right axillary lymph node.\n" + //
                "\n";
            List<Mention> results = pipeline.run(text);
            results.forEach(mention -> System.out.println(mention.toString()));
        } catch (ResourceInitializationException e) {
            System.err.println("Failed to create pipeline: " + e.getMessage());
            e.printStackTrace();
        } finally {
            pipeline.close();
        }
    }
}
