package launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalTime;
import java.util.Set;

import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;

import normalizers.MainNormalizer;

public class Launcher {

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, OWLOntologyCreationException {
		String inputOntologyPath = args[0];
		String outputOntologyPath = args[1];

		System.out.println("Normalizing Ontology (" + LocalTime.now() + ")");
		System.out.println(" * Input path:" + " " + inputOntologyPath);

		OWLOntology ontology = loadOntology(inputOntologyPath);
		if (ontology != null) {
			System.out.println("  - Input ontology size:" + " " + ontology.getAxiomCount());
			MainNormalizer mainNormalizer = new MainNormalizer();
			Set<OWLLogicalAxiom> normalizedAxs = mainNormalizer.filterAndNormalizeAxioms(ontology);
			System.out.println(" * Output path:" + " " + outputOntologyPath);
			saveAxsAsOWLOntology(normalizedAxs, outputOntologyPath);
			System.out.println("  - Normalized axioms count:" + " " + normalizedAxs.size());
		}
	}

	public static OWLOntology loadOntology(String ontologyFilePath) {
		OWLOntology ontology = null;
		try {
			// ignore missing imports
			OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
			OWLOntologyDocumentSource documentSource = new FileDocumentSource(new File(ontologyFilePath));
			ontology = Srd.owlOntologyManager.loadOntologyFromOntologyDocument(documentSource, config);
		} catch (OWLOntologyCreationException e) {
			System.out.println(e);
			System.out.println("WARNING!!! Error loading ontology.");
			System.out.println(" -> " + ontologyFilePath + "\n");
		}
		return ontology;
	}

	public static void saveOWLOntologyToFile(OWLOntology owlAPIOntology, String filePath) {
		try {
			Srd.owlOntologyManager.saveOntology(owlAPIOntology, new FileOutputStream(filePath));
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("WARNING!!! Exception at toFile() at NFOntology.java." + "\n");
			e.printStackTrace();
		}
	}

	public static void saveAxsAsOWLOntology(Set<OWLLogicalAxiom> normalizedAxs, String filePath) {
		try {
			OWLOntology owlAPIOntology = Srd.owlOntologyManager.createOntology();
			for (OWLAxiom axiom : normalizedAxs)
				Srd.owlOntologyManager.addAxiom(owlAPIOntology, axiom);
			Srd.owlOntologyManager.saveOntology(owlAPIOntology, new FileOutputStream(filePath));
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("WARNING!!! Exception at toFile() at NFOntology.java." + "\n");
			e.printStackTrace();
		}
	}
}
