package launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.time.LocalTime;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import nfOntology.NFOntology;
import normalizers.OntologyNormalizer;

public class OntologyNormalizerLauncher {

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, OWLOntologyCreationException {
		String inputOntologyPath = args[0];
		String outputOntologyPath = args[1];

		System.out.println("Normalizing Ontology (" + LocalTime.now() + ")");
		System.out.println(" * Input path:" + " " + inputOntologyPath);

		OWLOntology ontology = loadOntology(inputOntologyPath);
		if (ontology != null) {
			System.out.println("  - Input ontology size:" + " " + ontology.getAxiomCount());
			NFOntology nfOntology = OntologyNormalizer.normalizeOnt(ontology);

			System.out.println(" * Output path:" + " " + outputOntologyPath);
			nfOntology.toFile(outputOntologyPath);
			System.out.println("  - Normalized ontology sizet:" + " " + nfOntology.getAxioms().size());
			System.out.println("   + TBox size:" + " " + nfOntology.getTBoxAxioms().size());
			System.out.println("   + ABox size:" + " " + nfOntology.getABoxAxioms().size());
		}
	}

	public static OWLOntology loadOntology(String ontologyFilePath) {
		OWLOntology ontology = null;
		try {
			ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(ontologyFilePath));
		} catch (OWLOntologyCreationException e) {
			System.out.println(e);
			System.out.println("WARNING!!! Error loading ontology.");
			System.out.println(" -> " + ontologyFilePath + "\n");
		}
		return ontology;
	}
}
