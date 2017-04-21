package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleFacet;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

@Component
@Order(6)
@Profile({"default", "rest"})
public class RestExternalReferenceRunner implements ApplicationRunner, ExitCodeGenerator {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IntegrationProperties integrationProperties;

	@Autowired
	private RestOperations restTemplate;
	
	@Autowired
	private BioSamplesClient client;
	
	private int exitCode = 1;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		log.info("Starting RestFacetRunner");
		
		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {
			Sample sample = getSampleTest1();
			
			client.persist(sample);
			
			//check /samples/{id}/externalreferences
			testSamplesExternalReference(sample);
			
			//check /externalreferences
			testExternalReferences();
			
			
		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {
		}
		
		//if we got here without throwing, then we finished successfully
		exitCode = 0;
		log.info("Finished RestSearchRunner");
	}
	
	private void testSamplesExternalReference(Sample sample) {
		
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples")
			.pathSegment(sample.getAccession()).pathSegment("externalreferences").build().toUri();

		log.info("GETting from "+uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<ExternalReference>>> response = restTemplate.exchange(request, 
				new ParameterizedTypeReference<PagedResources<Resource<ExternalReference>>>(){});
		response.getBody().forEach( new Consumer<Resource<ExternalReference>>() {
			@Override
			public void accept(Resource<ExternalReference> t) {
				if (t.getLink("self") == null) throw new IllegalArgumentException("Must have self link");					
			}
		});
	}
	
	private void testExternalReferences() {
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("externalreferences").build().toUri();

		log.info("GETting from "+uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<ExternalReference>>> response = restTemplate.exchange(request, 
				new ParameterizedTypeReference<PagedResources<Resource<ExternalReference>>>(){});
		response.getBody().forEach( new Consumer<Resource<ExternalReference>>() {
			@Override
			public void accept(Resource<ExternalReference> t) {
				if (t.getLink("self") == null) throw new IllegalArgumentException("Must have self link");					
			}
		});
	}
	
	private Sample getSampleTest1() throws URISyntaxException {
		String name = "Test Sample";
		String accession = "TESTExRef1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();

		SortedSet<Relationship> relationships = new TreeSet<>();
		
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("http://www.test.com/1"));
		externalReferences.add(ExternalReference.build("http://www.test.com/2"));
		externalReferences.add(ExternalReference.build("http://www.test.com/3"));
		externalReferences.add(ExternalReference.build("http://www.test.com/4"));
		externalReferences.add(ExternalReference.build("http://www.test.com/5"));
		externalReferences.add(ExternalReference.build("http://www.test.com/6"));
		externalReferences.add(ExternalReference.build("http://www.test.com/7"));
		externalReferences.add(ExternalReference.build("http://www.test.com/8"));
		externalReferences.add(ExternalReference.build("http://www.test.com/9"));
		externalReferences.add(ExternalReference.build("http://www.test.com/0"));

		return Sample.build(name, accession, release, update, attributes, relationships, externalReferences);
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}