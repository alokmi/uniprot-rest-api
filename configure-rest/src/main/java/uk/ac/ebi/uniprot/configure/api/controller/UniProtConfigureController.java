package uk.ac.ebi.uniprot.configure.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.uniprot.configure.api.service.UniProtConfigureService;
import uk.ac.ebi.uniprot.configure.uniprot.domain.DatabaseGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.FieldGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItem;

@RestController
@RequestMapping("/configure")
public class UniProtConfigureController {
	UniProtConfigureService service;

	public UniProtConfigureController(UniProtConfigureService service) {
		this.service = service;
	}

	@GetMapping("/uniprot/search_terms")
	public List<SearchItem> getUniProtSearchTerms() {
		return service.getUniProtSearchItems();
	}

	@GetMapping("/uniprot/annotation_evidences")
	public  List<EvidenceGroup>  getUniProtAnnotationEvidences() {
		return service.getAnnotationEvidences();
	}
	
	@GetMapping("/uniprot/go_evidences")
	public  List<EvidenceGroup>  getUniProtGoEvidences() {
		return service.getGoEvidences();
	}
	@GetMapping("/uniprot/databases")
	public  List<DatabaseGroup>  getUniProtDatabase() {
		return service.getDatabases();
	}
	
	@GetMapping("/uniprot/databasefields")
	public  List<FieldGroup>  getDatabaseFields(){
		return service.getDatabaseFields();
	}
	@GetMapping("/uniprot/resultfields")
	public  List<FieldGroup>  getResultFields(){
		return service.getResultFields();
	}
}
