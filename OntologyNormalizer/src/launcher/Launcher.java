package launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalTime;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

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
			Set<OWLAxiom> normalizedAxs = MainNormalizer.filterAndNormalizeAxioms(ontology);
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
			ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(documentSource, config);
		} catch (OWLOntologyCreationException e) {
			System.out.println(e);
			System.out.println("WARNING!!! Error loading ontology.");
			System.out.println(" -> " + ontologyFilePath + "\n");
		}
		return ontology;
	}

	public static void saveAxsAsOWLOntology(Set<OWLAxiom> normalizedAxs, String filePath) {
		try {
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology owlAPIOntology = manager.createOntology();
			for (OWLAxiom axiom : normalizedAxs)
				manager.addAxiom(owlAPIOntology, axiom);
			manager.saveOntology(owlAPIOntology, new FileOutputStream(filePath));
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("WARNING!!! Exception at toFile() at NFOntology.java." + "\n");
			e.printStackTrace();
		}
	}
}
