# dphe-omop
This repository contains code that can be integrated into the OMOP ETL process for the DeepPhe project.
Although the output does not strictly conform to the OMOP NoteNLP schema, it provides the output of DeepPhe in a tabular
format that can be easily integrated into the OMOP ETL process by selecting the appropriate fields.

Everything will be built and enclosed in a single uber-jar, as is necessary for our initial runs using Apache Beam.
