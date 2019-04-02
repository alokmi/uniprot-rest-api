package uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.api.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.SearchEvidences;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;



public enum AnnotationEvidences implements SearchEvidences {
	INSTANCE;
	private static final String FILENAME ="uniprot/annotation_evidence.json";
	private  List< EvidenceGroup> evidences = new ArrayList<>();  
 
	 AnnotationEvidences(){
		init();
		 }
	
	 void init() {
		 ObjectMapper objectMapper = JsonConfig.getJsonMapper();
		 try ( InputStream is = AnnotationEvidences.class.getClassLoader()
				 .getResourceAsStream(FILENAME)){
			 List<EvidenceGroupImpl> evidences = objectMapper.readValue(is,  new TypeReference<List<EvidenceGroupImpl>>(){});
			 this.evidences.addAll(evidences);
		 }catch(Exception e) {
			 throw new RuntimeException (e);
		 }
		 
	
	}
	public List<EvidenceGroup> getEvidences() {
		return evidences;
	}
			 
}