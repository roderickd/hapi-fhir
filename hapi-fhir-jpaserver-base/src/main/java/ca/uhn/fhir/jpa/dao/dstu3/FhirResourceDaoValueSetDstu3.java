package ca.uhn.fhir.jpa.dao.dstu3;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2016 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.nio.file.FileVisitOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.StringUtils;
import org.hl7.fhir.dstu3.exceptions.TerminologyServiceException;
import org.hl7.fhir.dstu3.hapi.validation.HapiWorkerContext;
import org.hl7.fhir.dstu3.hapi.validation.IValidationSupport;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetFilterComponent;
import org.hl7.fhir.dstu3.model.ValueSet.FilterOperator;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.dstu3.terminologies.ValueSetExpander.ValueSetExpansionOutcome;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ca.uhn.fhir.jpa.dao.IFhirResourceDaoCodeSystem;
import ca.uhn.fhir.jpa.dao.IFhirResourceDaoCodeSystem.LookupCodeResult;
import ca.uhn.fhir.jpa.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.util.LogicUtil;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.ElementUtil;

public class FhirResourceDaoValueSetDstu3 extends FhirResourceDaoDstu3<ValueSet> implements IFhirResourceDaoValueSet<ValueSet, Coding, CodeableConcept> {

	@Autowired
	@Qualifier("myJpaValidationSupportChainDstu3")
	private IValidationSupport myValidationSupport;

	@Autowired
	private IFhirResourceDaoCodeSystem<CodeSystem, CodeableConcept, Coding> myCodeSystemDao;

	@Override
	public ValueSet expand(IIdType theId, String theFilter) {
		ValueSet source = myValidationSupport.fetchResource(getContext(), ValueSet.class, theId.getValue());
		return expand(source, theFilter);
	}

	private ValueSet doExpand(ValueSet theSource) {

		validateIncludes("include", theSource.getCompose().getInclude());
		validateIncludes("exclude", theSource.getCompose().getExclude());

		HapiWorkerContext workerContext = new HapiWorkerContext(getContext(), myValidationSupport);

		ValueSetExpansionOutcome outcome = workerContext.expand(theSource);
		ValueSetExpansionComponent expansion = outcome.getValueset().getExpansion();

		ValueSet retVal = new ValueSet();
		retVal.setExpansion(expansion);
		return retVal;
	}

	private void validateIncludes(String name, List<ConceptSetComponent> listToValidate) {
		for (ConceptSetComponent nextExclude : listToValidate) {
			if (isBlank(nextExclude.getSystem()) && !ElementUtil.isEmpty(nextExclude.getConcept(), nextExclude.getFilter())) {
				throw new InvalidRequestException("ValueSet contains " + name + " criteria with no system defined");
			}
		}
	}

	@Override
	public ValueSet expandByIdentifier(String theUri, String theFilter) {
		if (isBlank(theUri)) {
			throw new InvalidRequestException("URI must not be blank or missing");
		}

		ValueSet source = new ValueSet();
		
		source.getCompose().addImport(theUri);

		if (isNotBlank(theFilter)) {
			ConceptSetComponent include = source.getCompose().addInclude();
			ConceptSetFilterComponent filter = include.addFilter();
			filter.setProperty("display");
			filter.setOp(FilterOperator.EQUAL);
			filter.setValue(theFilter);
		}

		ValueSet retVal = doExpand(source);
		return retVal;

		// if (defaultValueSet != null) {
		// source = getContext().newJsonParser().parseResource(ValueSet.class, getContext().newJsonParser().encodeResourceToString(defaultValueSet));
		// } else {
		// IBundleProvider ids = search(ValueSet.SP_URL, new UriParam(theUri));
		// if (ids.size() == 0) {
		// throw new InvalidRequestException("Unknown ValueSet URI: " + theUri);
		// }
		// source = (ValueSet) ids.getResources(0, 1).get(0);
		// }
		//
		// return expand(defaultValueSet, theFilter);

	}

	@Override
	public ValueSet expand(ValueSet source, String theFilter) {		
		ValueSet toExpand = new ValueSet();
		for (UriType next : source.getCompose().getImport()) {
			ConceptSetComponent include = toExpand.getCompose().addInclude();
			include.setSystem(next.getValue());
			addFilterIfPresent(theFilter, include);
		}
		
		for (ConceptSetComponent next : source.getCompose().getInclude()) {
			toExpand.getCompose().addInclude(next);
			addFilterIfPresent(theFilter, next);
		}
		
		if (toExpand.getCompose().isEmpty()) {
			throw new InvalidRequestException("ValueSet does not have any compose.include or compose.import values, can not expand");
		}

		toExpand.getCompose().getExclude().addAll(source.getCompose().getExclude());
		
		ValueSet retVal = doExpand(toExpand);
		return retVal;

	}

	private void addFilterIfPresent(String theFilter, ConceptSetComponent include) {
		if (isNotBlank(theFilter)) {
			include.addFilter().setProperty("display").setOp(FilterOperator.EQUAL).setValue(theFilter);
		}
	}

	@Override
	public ca.uhn.fhir.jpa.dao.IFhirResourceDaoValueSet.ValidateCodeResult validateCode(IPrimitiveType<String> theValueSetIdentifier, IIdType theId, IPrimitiveType<String> theCode,
			IPrimitiveType<String> theSystem, IPrimitiveType<String> theDisplay, Coding theCoding, CodeableConcept theCodeableConcept) {

		List<IIdType> valueSetIds = Collections.emptyList();
		List<IIdType> codeSystemIds = Collections.emptyList();

		boolean haveCodeableConcept = theCodeableConcept != null && theCodeableConcept.getCoding().size() > 0;
		boolean haveCoding = theCoding != null && theCoding.isEmpty() == false;
		boolean haveCode = theCode != null && theCode.isEmpty() == false;

		if (!haveCodeableConcept && !haveCoding && !haveCode) {
			throw new InvalidRequestException("No code, coding, or codeableConcept provided to validate");
		}
		if (!LogicUtil.multiXor(haveCodeableConcept, haveCoding, haveCode)) {
			throw new InvalidRequestException("$validate-code can only validate (system AND code) OR (coding) OR (codeableConcept)");
		}

		boolean haveIdentifierParam = theValueSetIdentifier != null && theValueSetIdentifier.isEmpty() == false;
		if (theId != null) {
			valueSetIds = Collections.singletonList(theId);
		} else if (haveIdentifierParam) {
			Set<Long> ids = searchForIds(ValueSet.SP_IDENTIFIER, new TokenParam(null, theValueSetIdentifier.getValue()));
			valueSetIds = new ArrayList<IIdType>();
			for (Long next : ids) {
				valueSetIds.add(new IdType("ValueSet", next));
			}
		} else {
			if (theCode == null || theCode.isEmpty()) {
				throw new InvalidRequestException("Either ValueSet ID or ValueSet identifier or system and code must be provided. Unable to validate.");
			}
			// String code = theCode.getValue();
			// String system = toStringOrNull(theSystem);
			LookupCodeResult result = myCodeSystemDao.lookupCode(theCode, theSystem, null, null);
			if (result.isFound()) {
				ca.uhn.fhir.jpa.dao.IFhirResourceDaoValueSet.ValidateCodeResult retVal = new ValidateCodeResult(true, "Found code", result.getCodeDisplay());
				return retVal;
			}
		}

		for (IIdType nextId : valueSetIds) {
			ValueSet expansion = expand(nextId, null);
			List<ValueSetExpansionContainsComponent> contains = expansion.getExpansion().getContains();
			ValidateCodeResult result = validateCodeIsInContains(contains, toStringOrNull(theSystem), toStringOrNull(theCode), theCoding, theCodeableConcept);
			if (result != null) {
				if (theDisplay != null && isNotBlank(theDisplay.getValue()) && isNotBlank(result.getDisplay())) {
					if (!theDisplay.getValue().equals(result.getDisplay())) {
						return new ValidateCodeResult(false, "Display for code does not match", result.getDisplay());
					}
				}
				return result;
			}
		}

		for (IIdType nextId : codeSystemIds) {
			ValueSet expansion = expand(nextId, null);
			List<ValueSetExpansionContainsComponent> contains = expansion.getExpansion().getContains();
			ValidateCodeResult result = validateCodeIsInContains(contains, toStringOrNull(theSystem), toStringOrNull(theCode), theCoding, theCodeableConcept);
			if (result != null) {
				if (theDisplay != null && isNotBlank(theDisplay.getValue()) && isNotBlank(result.getDisplay())) {
					if (!theDisplay.getValue().equals(result.getDisplay())) {
						return new ValidateCodeResult(false, "Display for code does not match", result.getDisplay());
					}
				}
				return result;
			}
		}

		return new ValidateCodeResult(false, "Code not found", null);
	}

	private String toStringOrNull(IPrimitiveType<String> thePrimitive) {
		return thePrimitive != null ? thePrimitive.getValue() : null;
	}

	private ca.uhn.fhir.jpa.dao.IFhirResourceDaoValueSet.ValidateCodeResult validateCodeIsInContains(List<ValueSetExpansionContainsComponent> contains, String theSystem, String theCode,
			Coding theCoding, CodeableConcept theCodeableConcept) {
		for (ValueSetExpansionContainsComponent nextCode : contains) {
			ca.uhn.fhir.jpa.dao.IFhirResourceDaoValueSet.ValidateCodeResult result = validateCodeIsInContains(nextCode.getContains(), theSystem, theCode, theCoding, theCodeableConcept);
			if (result != null) {
				return result;
			}

			String system = nextCode.getSystem();
			String code = nextCode.getCode();

			if (isNotBlank(theCode)) {
				if (theCode.equals(code) && (isBlank(theSystem) || theSystem.equals(system))) {
					return new ValidateCodeResult(true, "Validation succeeded", nextCode.getDisplay());
				}
			} else if (theCoding != null) {
				if (StringUtils.equals(system, theCoding.getSystem()) && StringUtils.equals(code, theCoding.getCode())) {
					return new ValidateCodeResult(true, "Validation succeeded", nextCode.getDisplay());
				}
			} else {
				for (Coding next : theCodeableConcept.getCoding()) {
					if (StringUtils.equals(system, next.getSystem()) && StringUtils.equals(code, next.getCode())) {
						return new ValidateCodeResult(true, "Validation succeeded", nextCode.getDisplay());
					}
				}
			}

		}

		return null;
	}

	@Override
	public void purgeCaches() {
		// nothing
	}

}
