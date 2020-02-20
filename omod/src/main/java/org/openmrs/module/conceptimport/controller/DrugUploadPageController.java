package org.openmrs.module.conceptimport.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class DrugUploadPageController {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	public void get(@SpringBean PageModel pageModel,
	        @RequestParam(value = "breadcrumbOverride", required = false) String breadcrumbOverride) {
		pageModel.put("breadcrumbOverride", breadcrumbOverride);
	}
	
	public void post(@SpringBean PageModel pageModel, UiUtils ui,
	        @RequestParam(value = "breadcrumbOverride", required = false) String breadcrumbOverride,
	        @RequestParam(value = "returnUrl", required = false) String returnUrl,
	        @RequestParam(value = "file", required = false) MultipartFile file) {
		
		readCSVFile(file);
		pageModel.put("breadcrumbOverride", breadcrumbOverride);
	}
	
	private void readCSVFile(MultipartFile csvFile) {
        String cvsSplitBy = ",";
        InputStream is = null;
        try {
            is = csvFile.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            Object[] objects = br.lines().toArray();

            List<String> affectedRows = new ArrayList<>();
            if (objects.length >= 1) {

                for (int i = 0; i < objects.length; i++) {
                    // use comma as separator
                    String[] vlResult = objects[i].toString().split(cvsSplitBy);
                    //When on headers of the CSV File
                    if (vlResult[0].contentEquals("MEDICINE")) continue;

                    try {
                        String isCombination = vlResult[4].replaceAll("\"", "");
                        String medicine = vlResult[0].replaceAll("\"", "");
                        String drugForm = vlResult[1].replaceAll("\"", "");
                        String strength = vlResult[2].replaceAll("\"", "");
                        String rxcui = vlResult[6].replaceAll("\"", "");
                        String rxname = "";
                        String rxmaptype = "";

                        if (!rxcui.equals("Specifics Not Found") && !rxcui.equals("")) {
                            rxname = vlResult[7].replaceAll("\"", "");
                            rxmaptype = vlResult[8].replaceAll("\"", "");
                        }

                        Drug drug = createDrug(medicine, drugForm, strength, rxcui, rxname, rxmaptype, isCombination);
                        if (drug == null) {
                            affectedRows.add((i + 1) + " Reason Null");
                        }

                    } catch (Exception e) {
                        log.error("Exception at row " + (i + 1), e);
                        affectedRows.add((i + 1) + "Reason " + e);
                    }
                }
            }

            log.info("Rows that did not import " + affectedRows.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public Drug createDrug(String conceptName, String drugForm, String strength, String rxcui, String rxName,
	        String rxMapType, String isCombination) {
		ConceptService conceptService = Context.getConceptService();
		String drugName = conceptName;
		if (!drugForm.equals("N/A") && !strength.equals("N/A")) {
			drugName = conceptName + " " + drugForm + " " + strength;
		}
		
		Drug drug = conceptService.getDrug(drugName);
		try {
			if (drug == null) {
				drug = new Drug();
				drug.setName(drugName.replace("null", "").trim());
				drug.setConcept(createNewConcept(conceptName, rxcui, rxName, rxMapType,
				    "8d490dfc-c2cc-11de-8d13-0010c6dffd0f", isCombination));
				if (!drugForm.equals("N/A")) {
					drug.setDosageForm(getDoseForm(drugForm));
				}
				
				if (!strength.equals("N/A")) {
					drug.setStrength(strength);
				}
				
				conceptService.saveDrug(drug);
			}
		}
		catch (Exception e) {
			log.error(e);
			drug = null;
		}
		
		return drug;
	}
	
	public Concept createNewConcept(String name, String rxcui, String rxName, String rxMapType, String conceptClassUUID,
	        String isCombination) {
		
		ConceptService conceptService = Context.getConceptService();
		
		Concept concept = conceptService.getConceptByName(name.trim());
		List<Concept> conceptList = conceptService.getConceptsByName(name.trim());
		
		if (concept == null && !conceptList.isEmpty()) {
			for (Concept concept1 : conceptList) {
				if (concept1.getName().getName().toLowerCase().equals(name.toLowerCase())) {
					concept = concept1;
					break;
				}
			}
		}
		
		if (concept == null) {
			
			concept = new Concept();
			ConceptName conceptName = null;
			if (isCombination.equals("TRUE")) {
				
				String[] strings = name.split("/");
				for (String s : strings) {
					Concept members = null;
					
					members = createNewConcept(s.trim(), null, null, null, conceptClassUUID, "FALSE");
					if (members != null) {
						concept.addSetMember(members);
					}
					
				}
				
				conceptName = (new ConceptName(name.replaceAll(" / ", "/"), new Locale("en", "US")));
				
			} else {
				conceptName = (new ConceptName(name, new Locale("en", "US")));
			}
			ConceptClass conceptClass = conceptService.getConceptClassByUuid(conceptClassUUID);
			ConceptDatatype conceptDatatype = conceptService
			        .getConceptDatatypeByUuid("8d4a4c94-c2cc-11de-8d13-0010c6dffd0f");
			concept.setConceptClass(conceptClass);
			concept.setDatatype(conceptDatatype);
			concept.setFullySpecifiedName(conceptName);
			conceptName.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
			conceptName.setLocalePreferred(true);
			concept.setPreferredName(conceptName);
			concept.addDescription(new ConceptDescription(concept.getName().getName()
			        + " is an essential medication for Uganda as of the essential medication list published 2016", null));
		}
		
		if (rxcui != null && !rxcui.equals("") && !rxcui.equals("Not Found") && !rxcui.equals("Specifics Not Found")) {
			ConceptSource conceptSource = conceptService.getConceptSourceByUuid("4ADDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
			
			ConceptReferenceTerm conceptReferenceTerm = conceptService.getConceptReferenceTermByCode(rxcui, conceptSource);
			
			if (conceptReferenceTerm == null) {
				conceptReferenceTerm = new ConceptReferenceTerm(conceptSource, rxcui, rxName);
				conceptService.saveConceptReferenceTerm(conceptReferenceTerm);
			}
			
			ConceptMap conceptMap = new ConceptMap();
			conceptMap.setConcept(concept);
			conceptMap.setConceptReferenceTerm(conceptReferenceTerm);
			conceptMap.setConceptMapType(conceptService.getConceptMapTypeByName(rxMapType));
			concept.addConceptMapping(conceptMap);
		}
		try {
			conceptService.saveConcept(concept);
			
		}
		catch (Exception e) {
			log.error(e);
			return concept;
		}
		
		return concept;
	}
	
	public Concept getDoseForm(String formName) {
		Concept concept = Context.getConceptService().getConceptByName(formName);
		
		if (concept == null) {
			concept = createNewConcept(formName, null, null, null, "de359f23-2bfc-4e8d-96d8-25b7526d6070", "FALSE");
		}
		return concept;
	}
}
