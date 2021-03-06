package uk.ac.ebi.biosamples.controller;

import java.util.Collection;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleReadService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Primary controller for REST operations both in JSON and XML and both read and
 * write.
 * 
 * See {@link SampleHtmlController} for the HTML equivalent controller.
 * 
 * @author faulcon
 *
 */
@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples")
public class SamplesRestController {

	private final SampleReadService sampleService;
	private final SamplePageService samplePageService;
	private final FilterService filterService;
	private final BioSamplesAapService bioSamplesAapService;


	private final SampleResourceAssembler sampleResourceAssembler;

	private final EntityLinks entityLinks;

	private Logger log = LoggerFactory.getLogger(getClass());

	public SamplesRestController(SampleReadService sampleService,
			SamplePageService samplePageService,FilterService filterService,
			BioSamplesAapService bioSamplesAapService,
			SampleResourceAssembler sampleResourceAssembler, EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.samplePageService = samplePageService;
		this.filterService = filterService;
		this.bioSamplesAapService = bioSamplesAapService;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.entityLinks = entityLinks;
	}

	@CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> searchHal(
			@RequestParam(name = "text", required = false) String text,
			@RequestParam(name = "updatedafter", required = false) String updatedAfter,
			@RequestParam(name = "updatedbefore", required = false) String updatedBefore,
			@RequestParam(name = "filter", required = false) String[] filter, Pageable page,
			PagedResourcesAssembler<Sample> pageAssembler) {

		
		//Need to decode the %20 and similar from the parameters
		//this is *not* needed for the html controller
		if (text != null) {
			try {
				text = URLDecoder.decode(text, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}		
		if (filter != null) {
			for (int i = 0; i < filter.length; i++) {
				try {
					filter[i] = URLDecoder.decode(filter[i], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		
		MultiValueMap<String, String> filtersMap = filterService.getFilters(filter);

		Collection<String> domains = bioSamplesAapService.getDomains();
		
		Instant updatedAfterDate = null;
		if (updatedAfter != null) {
			try {
				updatedAfterDate = Instant.parse(updatedAfter);
			} catch (DateTimeParseException e) {
				//TODO make an exception
				return ResponseEntity.badRequest().build();
			}
		}
		Instant updatedBeforeDate = null;
		if (updatedBefore != null) {
			try {
				updatedBeforeDate = Instant.parse(updatedBefore);
			} catch (DateTimeParseException e) {
				//TODO make an exception
				return ResponseEntity.badRequest().build();
			}
		}
		
		Page<Sample> pageSample = samplePageService.getSamplesByText(text, filtersMap, domains, updatedAfterDate, updatedBeforeDate, page);
		// add the links to each individual sample on the page
		// also adds links to first/last/next/prev at the same time
		PagedResources<Resource<Sample>> pagedResources = pageAssembler.toResource(pageSample, sampleResourceAssembler,
				entityLinks.linkToCollectionResource(Sample.class));

		// to generate the HAL template correctly, the parameter name must match
		// the requestparam name
		pagedResources
				.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleAutocompleteRestController.class)
						.getAutocompleteHal(text, filter, null)).withRel("autocomplete"));
		pagedResources.add(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleFacetRestController.class).getFacetsHal(text, filter))
				.withRel("facet"));
		pagedResources.add(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).getSampleHal(null))
				.withRel("sample"));

		return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic().getHeaderValue())
				.body(pagedResources);
	}

}
