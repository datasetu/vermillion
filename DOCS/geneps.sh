#!/bin/bash

JAVA="/home/arun/jre1.8.0_191/bin/java"

for d in $(ls) 
do
	if [[ -d $d ]]
	then
		cd $d
		if [[ ! -d eps ]]
		then
			mkdir eps
		fi

		for f in $(ls *.plantuml)
		do
			ff=$(echo $f | cut -f1 -d'.')
			echo $f
			$JAVA -jar ~/plantuml.jar -pipe -Teps < $f > eps/$ff.eps
		done

		echo "ok " $d

		cd ..
	fi	
done
