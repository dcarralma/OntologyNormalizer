#!/bin/sh

inputFolder=Ontologies
echo
echo "Normalizing ontologies at: $inputFolder"
echo "Warning: This script will override existing ontologies in the input folder"
echo

for ontologyPath in $inputFolder/*
do
	java -jar OntologyNormalizer.jar $ontologyPath $ontologyPath
	echo
done