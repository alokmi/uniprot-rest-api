package org.uniprot.api.support_data.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.configure.service.ProteomeConfigureService;
import org.uniprot.store.search.domain.FieldGroup;

import java.util.List;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
*/
@RestController
@RequestMapping("/configure/proteome")
public class ProteomeConfigureController {
	private ProteomeConfigureService service;

	public ProteomeConfigureController(ProteomeConfigureService service) {
		this.service = service;
	}
	@GetMapping("/resultfields")
	public  List<FieldGroup>  getResultFields(){
		List<FieldGroup> resultFields = service.getResultFields();
		return resultFields;
	}
}
